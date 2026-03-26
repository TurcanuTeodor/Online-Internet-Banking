import { useEffect, useState } from 'react';
import { loadStripe } from '@stripe/stripe-js';
import { Elements, CardElement, useStripe, useElements } from '@stripe/react-stripe-js';
import { X, CreditCard, Loader2 } from 'lucide-react';
import { createTopUpIntent, getPaymentMethodsByClient } from '../../services/paymentService';
import { stripeElementsAppearance } from '../lib/stripeAppearance';
import { jwtDecode } from 'jwt-decode';

const publishableKey = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY;
const stripePromise = publishableKey ? loadStripe(publishableKey) : null;

function TopUpPaymentForm({ clientSecret, currencyCode, amountLabel, selectedSavedMethodId, onSuccess, onError }) {
  const stripe = useStripe();
  const elements = useElements();
  const [processing, setProcessing] = useState(false);

  const handlePay = async (e) => {
    e.preventDefault();
    if (!stripe || !elements) return;
    const card = elements.getElement(CardElement);
    if (!card) return;

    setProcessing(true);
    try {
      const confirmPayload = selectedSavedMethodId
        ? { payment_method: selectedSavedMethodId }
        : { payment_method: { card } };
      const { error, paymentIntent } = await stripe.confirmCardPayment(clientSecret, confirmPayload);
      if (error) {
        onError(error.message || 'Payment failed');
        return;
      }
      if (paymentIntent?.status === 'succeeded') {
        onSuccess();
      } else {
        onError(`Payment status: ${paymentIntent?.status || 'unknown'}`);
      }
    } catch (err) {
      onError(err.message || 'Payment failed');
    } finally {
      setProcessing(false);
    }
  };

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

  return (
    <form onSubmit={handlePay} className="space-y-4">
      <p className="text-sm text-zinc-400">
        Amount: <span className="text-white font-semibold">{amountLabel}</span> ({currencyCode})
      </p>
      {!selectedSavedMethodId ? (
        <div className="p-4 rounded-xl border border-gray-700 bg-gray-800/50">
          <CardElement options={cardStyle} />
        </div>
      ) : (
        <div className="p-3 rounded-xl border border-emerald-500/20 bg-emerald-500/10 text-emerald-300 text-sm">
          Using selected saved card.
        </div>
      )}
      <p className="text-xs text-zinc-500">
        Card details are sent directly to Stripe — they never touch CashTactics servers.
      </p>
      <button
        type="submit"
        disabled={!stripe || processing}
        className="btn-primary w-full flex items-center justify-center gap-2"
      >
        {processing ? <Loader2 className="w-4 h-4 animate-spin" /> : <CreditCard className="w-4 h-4" />}
        Pay now
      </button>
    </form>
  );
}

/**
 * @param {{ account: object, onClose: () => void, onSuccess: () => void }} props
 */
export default function TopUpModal({ account, onClose, onSuccess }) {
  const [amount, setAmount] = useState('');
  const [step, setStep] = useState('amount');
  const [intent, setIntent] = useState(null);
  const [loading, setLoading] = useState(false);
  const [localError, setLocalError] = useState('');
  const [savedMethods, setSavedMethods] = useState([]);
  const [selectedSavedMethodId, setSelectedSavedMethodId] = useState('');

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const token = localStorage.getItem('jwt_token');
        if (!token) return;
        const payload = jwtDecode(token);
        const cid = payload?.clientId;
        if (!cid) return;
        const methods = await getPaymentMethodsByClient(cid);
        if (mounted) {
          const arr = Array.isArray(methods) ? methods : [];
          setSavedMethods(arr);
          const def = arr.find((m) => m.isDefault);
          if (def?.stripePaymentMethodId || def?.paymentMethodId || def?.id) {
            setSelectedSavedMethodId(def.stripePaymentMethodId || def.paymentMethodId || def.id);
          }
        }
      } catch {
        // keep silent; fallback to manual card entry still works
      }
    })();
    return () => {
      mounted = false;
    };
  }, []);

  if (!publishableKey || !stripePromise) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
        <div className="glass max-w-md w-full rounded-2xl p-6 relative">
          <button type="button" onClick={onClose} className="absolute top-4 right-4 text-zinc-400 hover:text-white">
            <X className="w-5 h-5" />
          </button>
          <h2 className="text-xl font-bold mb-2">Top up</h2>
          <p className="text-red-400 text-sm">
            Missing <code className="text-zinc-300">VITE_STRIPE_PUBLISHABLE_KEY</code> in{' '}
            <code className="text-zinc-300">frontend/.env.local</code>. Add your Stripe publishable key and restart{' '}
            <code className="text-zinc-300">npm run dev</code>.
          </p>
        </div>
      </div>
    );
  }

  const formatMoney = (amt, cur) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: cur || 'EUR' }).format(amt);

  const handleStartIntent = async (e) => {
    e.preventDefault();
    setLocalError('');
    const parsed = parseFloat(amount, 10);
    if (Number.isNaN(parsed) || parsed < 0.5) {
      setLocalError('Enter an amount of at least 0.50');
      return;
    }
    setLoading(true);
    try {
      const data = await createTopUpIntent(account.id, parsed);
      setIntent(data);
      setStep('pay');
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || err.message;
      setLocalError(typeof msg === 'string' ? msg : 'Could not start payment');
    } finally {
      setLoading(false);
    }
  };

  const amountLabel = intent ? formatMoney(intent.amount, intent.currencyCode) : '';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
      <div className="glass max-w-md w-full rounded-2xl p-6 relative max-h-[90vh] overflow-y-auto">
        <button type="button" onClick={onClose} className="absolute top-4 right-4 text-zinc-400 hover:text-white">
          <X className="w-5 h-5" />
        </button>
        <h2 className="text-2xl font-bold mb-1 flex items-center gap-2">
          <CreditCard className="w-6 h-6 text-emerald-400" />
          Top up account
        </h2>
        <p className="text-sm text-zinc-500 mb-4 font-mono truncate">{account.iban}</p>

        {localError && (
          <div className="mb-4 p-3 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-sm">{localError}</div>
        )}

        {step === 'amount' && (
          <form onSubmit={handleStartIntent} className="space-y-4">
            <div>
              <label className="block text-sm text-zinc-400 mb-2">Amount ({account.currencyCode})</label>
              <input
                type="number"
                min="0.5"
                step="0.01"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="input-field w-full"
                placeholder="e.g. 25.00"
                required
              />
              <p className="text-xs text-zinc-500 mt-1">Minimum 0.50. Currency matches this account ({account.currencyCode}).</p>
            </div>
            <button type="submit" disabled={loading} className="btn-primary w-full flex items-center justify-center gap-2">
              {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
              Continue to card
            </button>
          </form>
        )}

        {step === 'pay' && intent?.clientSecret && (
          <Elements
            key={intent.clientSecret}
            stripe={stripePromise}
            options={{
              clientSecret: intent.clientSecret,
              appearance: stripeElementsAppearance,
            }}
          >
            <TopUpPaymentForm
              clientSecret={intent.clientSecret}
              currencyCode={intent.currencyCode}
              amountLabel={amountLabel}
              selectedSavedMethodId={selectedSavedMethodId}
              onSuccess={() => {
                onSuccess();
                onClose();
              }}
              onError={(msg) => setLocalError(msg)}
            />
            {savedMethods.length > 0 && (
              <div className="mt-4">
                <label className="block text-sm text-zinc-400 mb-2">Quick fund with saved card</label>
                <select
                  className="input-field"
                  value={selectedSavedMethodId}
                  onChange={(e) => setSelectedSavedMethodId(e.target.value)}
                >
                  <option value="">Use manual card entry</option>
                  {savedMethods.map((m) => {
                    const methodId = m.stripePaymentMethodId || m.paymentMethodId || m.id;
                    return (
                      <option key={m.id} value={methodId}>
                        {(m.cardBrand || 'Card').toUpperCase()} •••• {m.cardLast4}
                        {m.isDefault ? ' (Default)' : ''}
                      </option>
                    );
                  })}
                </select>
              </div>
            )}
          </Elements>
        )}
      </div>
    </div>
  );
}
