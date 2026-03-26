import { useState, useEffect } from 'react';
import { loadStripe } from '@stripe/stripe-js';
import { Elements, CardElement, useStripe, useElements } from '@stripe/react-stripe-js';
import { CreditCard, Loader2, Trash2, Star, Wallet, ShieldCheck } from 'lucide-react';
import { stripeElementsAppearance } from '../lib/stripeAppearance';
import ModalShell from './ModalShell';
import {
  getPaymentMethodsByClient,
  attachPaymentMethod,
  deletePaymentMethod,
  setDefaultPaymentMethod,
  getPaymentHistory,
  requestRefund,
} from '../../services/paymentService';

const publishableKey = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY;
const stripePromise = publishableKey ? loadStripe(publishableKey) : null;

const cardStyle = {
  style: {
    base: {
      color: '#e4e4e7',
      fontFamily: 'system-ui, sans-serif',
      fontSize: '16px',
      '::placeholder': { color: '#71717a' },
    },
    invalid: { color: '#f87171' },
  },
};

function AddCardForm({ clientId, onSuccess, onError }) {
  const stripe = useStripe();
  const elements = useElements();
  const [busy, setBusy] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!stripe || !elements) return;
    const card = elements.getElement(CardElement);
    if (!card) return;

    setBusy(true);
    try {
      const { error, paymentMethod } = await stripe.createPaymentMethod({
        type: 'card',
        card,
      });
      if (error) {
        onError(error.message || 'Could not read card');
        return;
      }
      if (!paymentMethod?.id) {
        onError('No payment method returned from Stripe');
        return;
      }
      await attachPaymentMethod(clientId, paymentMethod.id);
      onSuccess();
    } catch (err) {
      onError(err.response?.data?.message || err.message || 'Failed to save card');
    } finally {
      setBusy(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="p-4 rounded-xl border border-gray-700 bg-gray-800/50">
        <CardElement options={cardStyle} />
      </div>
      <div className="flex items-center gap-2 text-xs text-gray-500 mt-2">
        <ShieldCheck size={14} className="text-green-500 shrink-0" />
        <span>Card data goes to Stripe only — we store brand/last4 after attach.</span>
      </div>
      <button type="submit" disabled={!stripe || busy} className="btn-primary w-full flex items-center justify-center gap-2">
        {busy ? <Loader2 className="w-4 h-4 animate-spin" /> : <CreditCard className="w-4 h-4" />}
        Save card
      </button>
    </form>
  );
}

/**
 * @param {{
 *   clientId: number,
 *   accounts: Array,
 *   transactions: Array,
 *   onRefresh: () => void,
 *   onOpenTopUp: (account: object) => void,
 * }} props
 */
export default function CardsPaymentsTab({ clientId, accounts, transactions, onRefresh, onOpenTopUp }) {
  const [methods, setMethods] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [topUpAccountId, setTopUpAccountId] = useState('');
  const [payments, setPayments] = useState([]);
  const [paymentsLoading, setPaymentsLoading] = useState(false);
  const [selectedPayment, setSelectedPayment] = useState(null);
  const [refundBusy, setRefundBusy] = useState(false);

  const loadMethods = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await getPaymentMethodsByClient(clientId);
      setMethods(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.response?.data?.message || 'Could not load saved cards');
      setMethods([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (clientId) loadMethods();
  }, [clientId]);

  const loadPayments = async () => {
    setPaymentsLoading(true);
    setError('');
    try {
      const data = await getPaymentHistory(clientId);
      setPayments(Array.isArray(data) ? data : []);
    } catch (err) {
      setPayments([]);
      setError(err.message || 'Could not load payment history');
    } finally {
      setPaymentsLoading(false);
    }
  };

  useEffect(() => {
    if (clientId) loadPayments();
  }, [clientId]);

  const handleDelete = async (id) => {
    if (!window.confirm('Remove this card from your profile?')) return;
    try {
      await deletePaymentMethod(id);
      setSuccess('Card removed');
      loadMethods();
      onRefresh?.();
    } catch (err) {
      setError(err.response?.data?.message || 'Could not delete card');
    }
  };

  const handleSetDefault = async (id) => {
    try {
      await setDefaultPaymentMethod(clientId, id);
      setSuccess('Default card updated');
      loadMethods();
    } catch (err) {
      setError(err.response?.data?.message || 'Could not set default');
    }
  };

  const stripePayments = (transactions || []).filter(
    (tx) =>
      tx?.merchant === 'Stripe' ||
      (typeof tx?.details === 'string' && tx.details.includes('Stripe')) ||
      (typeof tx?.details === 'string' && tx.details.includes('Card top-up'))
  );

  const formatMoney = (amt, cur) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: cur || 'EUR' }).format(amt);

  const formatDate = (d) =>
    d ? new Date(d).toLocaleString() : '—';

  const activeAccounts = (accounts || []).filter((a) => a.status === 'ACTIVE');
  const selectedPaymentStatus = String(selectedPayment?.status || '').toUpperCase();
  const refundBlockedStatuses = ['REFUNDED', 'REFUND_REQUESTED', 'FAILED', 'CANCELED', 'CANCELLED'];
  const canRequestRefund =
    selectedPayment &&
    selectedPayment.id != null &&
    !refundBlockedStatuses.includes(selectedPaymentStatus);

  const paymentDetailsRows = selectedPayment
    ? [
        ['Payment ID', selectedPayment.id],
        ['Status', selectedPayment.status],
        ['Amount', selectedPayment.amount != null ? formatMoney(selectedPayment.amount, selectedPayment.currencyCode) : null],
        ['Currency', selectedPayment.currencyCode],
        ['Type', selectedPayment.paymentType || selectedPayment.type],
        ['Provider', selectedPayment.provider || selectedPayment.gateway || selectedPayment.merchant],
        ['Payment Intent', selectedPayment.paymentIntentId || selectedPayment.stripePaymentIntentId],
        ['Method', selectedPayment.method || selectedPayment.paymentMethodType || selectedPayment.cardBrand],
        ['Card Last4', selectedPayment.cardLast4],
        ['Client ID', selectedPayment.clientId],
        ['Account ID', selectedPayment.accountId],
        ['Account IBAN', selectedPayment.accountIban],
        ['Created at', selectedPayment.createdAt ? new Date(selectedPayment.createdAt).toLocaleString() : null],
        ['Updated at', selectedPayment.updatedAt ? new Date(selectedPayment.updatedAt).toLocaleString() : null],
      ].filter(([, value]) => value !== null && value !== undefined && value !== '')
    : [];

  useEffect(() => {
    if (!error) return;
    const t = setTimeout(() => setError(''), 4500);
    return () => clearTimeout(t);
  }, [error]);

  useEffect(() => {
    if (!success) return;
    const t = setTimeout(() => setSuccess(''), 3500);
    return () => clearTimeout(t);
  }, [success]);

  return (
    <div className="space-y-8">
      {(error || success) && (
        <div className="fixed top-4 right-4 z-[60] space-y-2 w-[min(92vw,360px)]">
          {error ? (
            <div className="p-3 rounded-xl bg-red-500/10 border border-red-500/20 text-red-300 text-sm shadow-lg">
              {error}
            </div>
          ) : null}
          {success ? (
            <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-sm shadow-lg">
              {success}
            </div>
          ) : null}
        </div>
      )}

      {/* Top-up quick action */}
      <section className="glass rounded-2xl p-6">
        <h2 className="text-xl font-bold mb-2 flex items-center gap-2">
          <Wallet className="w-5 h-5 text-emerald-400" />
          Top up account (Stripe)
        </h2>
        <p className="text-sm text-zinc-500 mb-4">
          Choose an account and pay with your card — card numbers never go to our servers.
        </p>
        <div className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-end">
          <div className="flex-1">
            <label className="block text-sm text-zinc-400 mb-1">Account</label>
            <select
              className="input-field w-full"
              value={topUpAccountId}
              onChange={(e) => setTopUpAccountId(e.target.value)}
            >
              <option value="">— Select —</option>
              {activeAccounts
                .filter((a) => a.currencyCode === 'EUR' || a.currencyCode === 'RON')
                .map((a) => (
                  <option key={a.id} value={String(a.id)}>
                    {a.iban} ({a.currencyCode})
                  </option>
                ))}
            </select>
          </div>
          <button
            type="button"
            disabled={!topUpAccountId}
            className="btn-primary flex items-center justify-center gap-2"
            onClick={() => {
              const acc = activeAccounts.find((a) => String(a.id) === topUpAccountId);
              if (acc) onOpenTopUp(acc);
            }}
          >
            <CreditCard className="w-4 h-4" />
            Continue to payment
          </button>
        </div>
      </section>

      {/* Saved cards */}
      <section className="glass rounded-2xl p-6">
        <h2 className="text-xl font-bold mb-4">Saved cards</h2>
        {loading ? (
          <div className="flex items-center gap-2 text-zinc-400">
            <Loader2 className="w-5 h-5 animate-spin" /> Loading…
          </div>
        ) : methods.length === 0 ? (
          <p className="text-zinc-500 text-sm">No cards yet. Add one below.</p>
        ) : (
          <ul className="space-y-3">
            {methods.map((m) => (
              <li
                key={m.id}
                className="flex flex-wrap items-center justify-between gap-3 border border-white/10 rounded-xl p-4 bg-black/20"
              >
                <div>
                  <p className="font-mono text-zinc-200">
                    {(m.cardBrand || 'Card').toUpperCase()} •••• {m.cardLast4}
                  </p>
                  <p className="text-xs text-zinc-500">
                    exp {m.expiryMonth}/{m.expiryYear}
                    {m.isDefault ? (
                      <span className="ml-2 text-emerald-400">Default</span>
                    ) : null}
                  </p>
                </div>
                <div className="flex gap-2">
                  {!m.isDefault && (
                    <button type="button" className="btn-secondary text-sm py-1 px-3" onClick={() => handleSetDefault(m.id)}>
                      <Star className="w-4 h-4 inline mr-1" />
                      Default
                    </button>
                  )}
                  <button
                    type="button"
                    className="btn-secondary text-sm py-1 px-3 text-red-300 border-red-500/30"
                    onClick={() => handleDelete(m.id)}
                  >
                    <Trash2 className="w-4 h-4 inline mr-1" />
                    Remove
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* Add card */}
      <section className="glass rounded-2xl p-6">
        <h2 className="text-xl font-bold mb-4">Add new card</h2>
        {!publishableKey || !stripePromise ? (
          <p className="text-amber-400 text-sm">
            Set <code className="text-zinc-300">VITE_STRIPE_PUBLISHABLE_KEY</code> in <code>.env.local</code>.
          </p>
        ) : (
          <Elements stripe={stripePromise} options={{ appearance: stripeElementsAppearance }}>
            <AddCardForm
              clientId={clientId}
              onSuccess={() => {
                setSuccess('Card saved');
                loadMethods();
                onRefresh?.();
              }}
              onError={(msg) => setError(msg)}
            />
          </Elements>
        )}
      </section>

      {/* Payment history */}
      <section className="glass rounded-2xl p-6 overflow-hidden">
        <div className="flex items-center justify-between gap-3 mb-4">
          <div>
            <h2 className="text-xl font-bold">Payment history</h2>
            <p className="text-xs text-zinc-500 mt-1">Click any payment to open details and refund actions.</p>
          </div>
          <button type="button" className="btn-secondary text-sm" onClick={loadPayments} disabled={paymentsLoading}>
            Refresh
          </button>
        </div>
        {paymentsLoading ? (
          <div className="flex items-center gap-2 text-zinc-400">
            <Loader2 className="w-5 h-5 animate-spin" /> Loading…
          </div>
        ) : payments.length === 0 ? (
          <p className="text-zinc-500 text-sm">No payments yet.</p>
        ) : (
          <>
            <div className="md:hidden space-y-3">
              {payments.map((p) => (
                <div key={p.id} className="rounded-xl border border-zinc-800 bg-black/20 p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-xs text-zinc-500">Date</p>
                      <p className="text-sm text-zinc-300">{p.createdAt ? new Date(p.createdAt).toLocaleString() : '—'}</p>
                    </div>
                    <p className="text-sm font-semibold text-zinc-200">
                      {p.amount != null ? formatMoney(p.amount, p.currencyCode) : '—'}
                    </p>
                  </div>
                  <div className="mt-2 flex items-center justify-between text-sm">
                    <p className="text-zinc-400">{p.paymentType || p.type || '—'}</p>
                    <p className="text-zinc-500">{p.status || '—'}</p>
                  </div>
                  <button type="button" className="btn-secondary text-xs py-1.5 px-3 mt-3" onClick={() => setSelectedPayment(p)}>
                    View
                  </button>
                </div>
              ))}
            </div>
            <div className="hidden md:block overflow-x-auto">
              <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-zinc-800 text-left text-zinc-400">
                  <th className="sticky top-0 z-10 py-2 pr-4 bg-zinc-900/95">Date</th>
                  <th className="sticky top-0 z-10 py-2 pr-4 bg-zinc-900/95">Type</th>
                  <th className="sticky top-0 z-10 py-2 pr-4 bg-zinc-900/95">Status</th>
                  <th className="sticky top-0 z-10 py-2 text-right bg-zinc-900/95">Amount</th>
                  <th className="sticky top-0 z-10 py-2 pl-4 text-right bg-zinc-900/95">Action</th>
                </tr>
              </thead>
              <tbody>
                {payments.map((p) => (
                  <tr key={p.id} className="border-b border-zinc-800/50 hover:bg-zinc-800/30">
                    <td className="py-2 pr-4 text-zinc-300">
                      {p.createdAt ? new Date(p.createdAt).toLocaleString() : '—'}
                    </td>
                    <td className="py-2 pr-4 text-zinc-300">{p.paymentType || p.type || '—'}</td>
                    <td className="py-2 pr-4 text-zinc-500">{p.status || '—'}</td>
                    <td className="py-2 text-right font-medium text-zinc-200">
                      {p.amount != null ? formatMoney(p.amount, p.currencyCode) : '—'}
                    </td>
                    <td className="py-2 pl-4 text-right">
                      <button
                        type="button"
                        className="btn-secondary text-xs py-1.5 px-3"
                        onClick={() => setSelectedPayment(p)}
                      >
                        View
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
              </table>
            </div>
          </>
        )}
      </section>

      {/* Card / Stripe payment history */}
      <section className="glass rounded-2xl p-6 overflow-hidden">
        <h2 className="text-xl font-bold mb-4">Card payments (Stripe)</h2>
        {stripePayments.length === 0 ? (
          <p className="text-zinc-500 text-sm">No Stripe card payments yet.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-zinc-800 text-left text-zinc-400">
                  <th className="sticky top-0 z-10 py-2 pr-4 bg-zinc-900/95">Date</th>
                  <th className="sticky top-0 z-10 py-2 pr-4 bg-zinc-900/95">Type</th>
                  <th className="sticky top-0 z-10 py-2 pr-4 bg-zinc-900/95">Details</th>
                  <th className="sticky top-0 z-10 py-2 text-right bg-zinc-900/95">Amount</th>
                </tr>
              </thead>
              <tbody>
                {stripePayments.map((tx) => (
                  <tr key={tx.id} className="border-b border-zinc-800/50">
                    <td className="py-2 pr-4 text-zinc-300">{formatDate(tx.transactionDate)}</td>
                    <td className="py-2 pr-4">{tx.transactionTypeName}</td>
                    <td className="py-2 pr-4 text-zinc-500 max-w-xs truncate">{tx.details || '—'}</td>
                    <td className={`py-2 text-right font-medium ${tx.sign === '+' ? 'text-emerald-400' : 'text-red-400'}`}>
                      {tx.sign === '+' ? '+' : '-'}
                      {formatMoney(tx.amount, tx.originalCurrencyCode)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {selectedPayment && (
        <ModalShell
          title="Payment details"
          subtitle={`Payment ID: ${selectedPayment.id}`}
          onClose={() => setSelectedPayment(null)}
          maxWidth="max-w-lg"
          footer={
            <>
              <button type="button" className="btn-secondary" onClick={() => setSelectedPayment(null)}>
                Close
              </button>
              {canRequestRefund ? (
                <button
                  type="button"
                  className="btn-primary"
                  disabled={refundBusy}
                  onClick={async () => {
                    if (!window.confirm('Request a refund for this payment?')) return;
                    setRefundBusy(true);
                    try {
                      await requestRefund(selectedPayment.id);
                      setSuccess('Refund requested');
                      setSelectedPayment(null);
                      loadPayments();
                    } catch (e) {
                      setError(e.message || 'Refund request failed');
                    } finally {
                      setRefundBusy(false);
                    }
                  }}
                >
                  {refundBusy ? 'Requesting…' : 'Request refund'}
                </button>
              ) : (
                <span className="text-xs text-zinc-500">Refund unavailable for this payment status.</span>
              )}
            </>
          }
        >
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
              {paymentDetailsRows.map(([label, value]) => (
                <div key={label} className="glass rounded-xl p-3">
                  <p className="text-xs text-zinc-500">{label}</p>
                  <p className="text-zinc-200 break-all">{String(value)}</p>
                </div>
              ))}
            </div>
        </ModalShell>
      )}
    </div>
  );
}
