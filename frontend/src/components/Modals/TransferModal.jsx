import { useState } from 'react';
import { transfer } from '@/services/accountService';
import TransferConfirmation from '../TransferConfirmation';
import { toast } from 'sonner';

export default function TransferModal({ selectedAccount, onClose, onSuccess }) {
  const [transferForm, setTransferForm] = useState({ toIban: '', amount: '' });
  const [transferStep, setTransferStep] = useState('form');
  const [loading, setLoading] = useState(false);

  const handleTransferSubmit = () => {
    if (!selectedAccount || !transferForm.toIban.trim() || !transferForm.amount) {
      toast.error('Please fill all transfer fields.');
      return;
    }
    setTransferStep('confirm');
  };

  const handleTransferConfirm = async () => {
    setLoading(true);
    try {
      await transfer(selectedAccount.iban, transferForm.toIban, parseFloat(transferForm.amount));
      toast.success(`Transferred ${transferForm.amount} ${selectedAccount.currencyCode} successfully!`);
      onSuccess();
      onClose();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Transfer failed');
      setTransferStep('form');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50" onClick={onClose}>
      <div className="glass rounded-2xl p-6 max-w-md w-full animate-fade-in" onClick={(e) => e.stopPropagation()}>
        {transferStep === 'confirm' ? (
          <TransferConfirmation
            fromAccount={selectedAccount}
            toIban={transferForm.toIban}
            amount={parseFloat(transferForm.amount)}
            loading={loading}
            onConfirm={handleTransferConfirm}
            onBack={() => setTransferStep('form')}
          />
        ) : (
          <>
            <h3 className="text-xl font-bold mb-4">Transfer from {selectedAccount.iban}</h3>
            <label className="block text-sm font-medium text-zinc-400 mb-2">To IBAN</label>
            <input type="text" value={transferForm.toIban} onChange={(e) => setTransferForm({...transferForm, toIban: e.target.value})} className="input-field mb-4" placeholder="RO49BANK0000000002EUR" />
            <label className="block text-sm font-medium text-zinc-400 mb-2">Amount ({selectedAccount.currencyCode})</label>
            <input type="number" value={transferForm.amount} onChange={(e) => setTransferForm({...transferForm, amount: e.target.value})} className="input-field mb-4" placeholder="0.00" step="0.01" min="0" />
            <div className="flex gap-3">
              <button onClick={onClose} className="btn-secondary flex-1">Cancel</button>
              <button onClick={handleTransferSubmit} className="btn-primary flex-1">Review Transfer</button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
