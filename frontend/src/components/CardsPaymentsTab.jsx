import { useState, useEffect } from 'react';
import { loadStripe } from '@stripe/stripe-js';
import { Elements, CardElement, useStripe, useElements } from '@stripe/react-stripe-js';
import { CreditCard, Loader2, Trash2, Star, Wallet, ShieldCheck } from 'lucide-react';
import { stripeElementsAppearance } from '@/lib/stripeAppearance';
import {
  getPaymentMethodsByClient,
  attachPaymentMethod,
  deletePaymentMethod,
  setDefaultPaymentMethod,
} from '@/services/paymentService';

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
export default function CardsPaymentsTab({ clientId, accounts, onRefresh, onOpenTopUp }) {
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
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

  const activeAccounts = (accounts || []).filter((a) => a.status === 'ACTIVE');
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

    </div>
  );
}
