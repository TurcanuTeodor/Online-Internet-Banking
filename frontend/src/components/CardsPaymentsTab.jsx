import { useState, useEffect } from 'react';
import { loadStripe } from '@stripe/stripe-js';
import { Elements, CardElement, useStripe, useElements } from '@stripe/react-stripe-js';
import { CreditCard, Loader2, Trash2, Star, Wallet } from 'lucide-react';
import {
  getPaymentMethodsByClient,
  attachPaymentMethod,
  deletePaymentMethod,
  setDefaultPaymentMethod,
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
      <div className="rounded-xl border border-white/10 bg-black/20 p-4">
        <CardElement options={cardStyle} />
      </div>
      <p className="text-xs text-zinc-500">
        Card data goes to Stripe only — we store brand/last4 after attach.
      </p>
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

  return (
    <div className="space-y-10">
      {error && (
        <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 text-sm">
          {error}
          <button type="button" onClick={() => setError('')} className="ml-3 underline">
            Dismiss
          </button>
        </div>
      )}
      {success && (
        <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-sm">
          {success}
          <button type="button" onClick={() => setSuccess('')} className="ml-3 underline">
            OK
          </button>
        </div>
      )}

      {/* Top-up quick action */}
      <section className="glass rounded-2xl p-6">
        <h2 className="text-xl font-bold mb-2 flex items-center gap-2">
          <Wallet className="w-5 h-5 text-emerald-400" />
          Alimentare cont (Stripe)
        </h2>
        <p className="text-sm text-zinc-500 mb-4">Alege contul și deschide plata cu cardul (nu trimitem numărul pe server).</p>
        <div className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-end">
          <div className="flex-1">
            <label className="block text-sm text-zinc-400 mb-1">Cont</label>
            <select
              className="input-field w-full"
              value={topUpAccountId}
              onChange={(e) => setTopUpAccountId(e.target.value)}
            >
              <option value="">— selectează —</option>
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
            Continuă la plată
          </button>
        </div>
      </section>

      {/* Saved cards */}
      <section className="glass rounded-2xl p-6">
        <h2 className="text-xl font-bold mb-4">Carduri salvate</h2>
        {loading ? (
          <div className="flex items-center gap-2 text-zinc-400">
            <Loader2 className="w-5 h-5 animate-spin" /> Se încarcă…
          </div>
        ) : methods.length === 0 ? (
          <p className="text-zinc-500 text-sm">Niciun card încă. Adaugă unul mai jos.</p>
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
                    Șterge
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* Add card */}
      <section className="glass rounded-2xl p-6">
        <h2 className="text-xl font-bold mb-4">Adaugă card nou</h2>
        {!publishableKey || !stripePromise ? (
          <p className="text-amber-400 text-sm">
            Setează <code className="text-zinc-300">VITE_STRIPE_PUBLISHABLE_KEY</code> în <code>.env.local</code>.
          </p>
        ) : (
          <Elements stripe={stripePromise}>
            <AddCardForm
              clientId={clientId}
              onSuccess={() => {
                setSuccess('Card salvat');
                loadMethods();
                onRefresh?.();
              }}
              onError={(msg) => setError(msg)}
            />
          </Elements>
        )}
      </section>

      {/* Card / Stripe payment history */}
      <section className="glass rounded-2xl p-6 overflow-hidden">
        <h2 className="text-xl font-bold mb-4">Istoric plăți card (Stripe)</h2>
        {stripePayments.length === 0 ? (
          <p className="text-zinc-500 text-sm">Nicio tranzacție Stripe încă.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-zinc-800 text-left text-zinc-400">
                  <th className="py-2 pr-4">Dată</th>
                  <th className="py-2 pr-4">Tip</th>
                  <th className="py-2 pr-4">Detalii</th>
                  <th className="py-2 text-right">Sumă</th>
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
    </div>
  );
}
