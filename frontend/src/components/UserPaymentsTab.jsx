import { useState } from 'react';
import { Send } from 'lucide-react';
import { transfer } from '@/services/accountService';
import TransferConfirmation from './TransferConfirmation';

export default function UserPaymentsTab({ accounts, onSuccess, onError, onRefresh }) {
  const [fromAccountId, setFromAccountId] = useState('');
  const [toIban, setToIban] = useState('');
  const [amount, setAmount] = useState('');
  const [busy, setBusy] = useState(false);
  const [step, setStep] = useState('form');

  const activeAccounts = (accounts || []).filter((a) => a.status === 'ACTIVE');
  const selectedFrom = activeAccounts.find((a) => String(a.id) === fromAccountId);

  const handleReview = (e) => {
    e.preventDefault();
    if (!selectedFrom) { onError?.('Please select a source account.'); return; }
    if (!toIban.trim()) { onError?.('Please enter a destination IBAN.'); return; }
    const parsedAmount = Number.parseFloat(amount);
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) { onError?.('Please enter a valid transfer amount.'); return; }
    setStep('confirm');
  };

  const submitTransfer = async () => {
    setBusy(true);
    try {
      await transfer(selectedFrom.iban, toIban.trim(), Number.parseFloat(amount));
      setToIban('');
      setAmount('');
      setStep('form');
      onSuccess?.(`Transfer sent successfully from ${selectedFrom.iban}.`);
      onRefresh?.();
    } catch (err) {
      onError?.(err?.response?.data?.message || err?.message || 'Transfer failed');
      setStep('form');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="glass rounded-2xl p-6">
        <h2 className="text-2xl font-bold">Payments & Transfers</h2>
        <p className="text-zinc-500 text-sm mt-1">Send money quickly using your active accounts.</p>
      </div>

      <section className="glass rounded-2xl p-6">
        <h3 className="text-lg font-semibold mb-4">Send money</h3>
        {step === 'confirm' ? (
          <TransferConfirmation
            fromAccount={selectedFrom}
            toIban={toIban}
            amount={Number.parseFloat(amount)}
            loading={busy}
            onConfirm={submitTransfer}
            onBack={() => setStep('form')}
          />
        ) : (
          <form onSubmit={handleReview} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">Source account</label>
              <select className="input-field" value={fromAccountId} onChange={(e) => setFromAccountId(e.target.value)} required>
                <option value="">Select source account</option>
                {activeAccounts.map((a) => (
                  <option key={a.id} value={String(a.id)}>{a.iban} ({a.currencyCode}) - {a.balance}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">Destination IBAN</label>
              <input type="text" className="input-field" placeholder="RO49BANK0000000002EUR" value={toIban} onChange={(e) => setToIban(e.target.value)} required />
            </div>
            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">Amount</label>
              <input type="number" className="input-field" placeholder="0.00" step="0.01" min="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} required />
            </div>
            <button type="submit" className="btn-primary flex items-center gap-2" disabled={busy}>
              <Send className="w-4 h-4" />
              Review Transfer
            </button>
          </form>
        )}
      </section>
    </div>
  );
}
