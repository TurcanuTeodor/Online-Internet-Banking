import { useState } from 'react';
import { transfer } from '@/services/accountService';
import TransferConfirmation from '../TransferConfirmation';
import StepUpModal from './StepUpModal';
import { toast } from 'sonner';

export default function TransferModal({ selectedAccount, onClose, onSuccess }) {
  const [transferForm, setTransferForm] = useState({ toIban: '', amount: '' });
  const [transferStep, setTransferStep] = useState('form');
  const [loading, setLoading] = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [showStepUp, setShowStepUp] = useState(false);
  const [stepUpError, setStepUpError] = useState('');

  const handleTransferSubmit = () => {
    setSubmitError('');
    if (!selectedAccount || !transferForm.toIban.trim() || !transferForm.amount) {
      toast.error('Please fill all transfer fields.');
      return;
    }
    setTransferStep('confirm');
  };

  const handleTransferConfirm = async (totpCode = null) => {
    if (!selectedAccount) {
      toast.error('Source account is no longer available.');
      setTransferStep('form');
      return;
    }

    setLoading(true);
    setSubmitError('');
    setStepUpError('');
    try {
      await transfer(selectedAccount.iban, transferForm.toIban.trim(), parseFloat(transferForm.amount), totpCode);
      toast.success(`Transferred ${transferForm.amount} ${selectedAccount.currencyCode} successfully!`);
      setShowStepUp(false);
      onSuccess();
      onClose();
    } catch (err) {
      const status = err?.response?.status;
      const message = err?.response?.data?.message || err?.message || 'Transfer failed';
      
      if (status === 428 || status === 401) {
        setStepUpError(message);
        setShowStepUp(true);
      } else {
        setSubmitError(message);
        toast.error(message);
        setShowStepUp(false);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      {showStepUp && (
        <StepUpModal
          error={stepUpError}
          onVerify={(code) => handleTransferConfirm(code)}
          onCancel={() => setShowStepUp(false)}
        />
      )}
      <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50" onClick={onClose}>
        <div className="glass rounded-2xl p-6 max-w-md w-full animate-fade-in" onClick={(e) => e.stopPropagation()}>
          {transferStep === 'confirm' ? (
          <TransferConfirmation
            fromAccount={selectedAccount}
            toIban={transferForm.toIban}
            amount={parseFloat(transferForm.amount)}
            loading={loading}
            errorMessage={submitError}
            onConfirm={handleTransferConfirm}
            onBack={() => {
              setSubmitError('');
              setTransferStep('form');
            }}
          />
        ) : (
          <>
            <h3 className="text-xl font-bold mb-4">Transfer from {selectedAccount.iban}</h3>
            <label className="block text-sm font-medium text-zinc-400 mb-2">To IBAN</label>
            <input
              type="text"
              value={transferForm.toIban}
              onChange={(e) => {
                setSubmitError('');
                setTransferForm({ ...transferForm, toIban: e.target.value });
              }}
              className="input-field mb-4"
              placeholder="RO49BANK0000000002EUR"
            />
            <label className="block text-sm font-medium text-zinc-400 mb-2">Amount ({selectedAccount.currencyCode})</label>
            <input
              type="number"
              value={transferForm.amount}
              onChange={(e) => {
                setSubmitError('');
                setTransferForm({ ...transferForm, amount: e.target.value });
              }}
              className="input-field mb-4"
              placeholder="0.00"
              step="0.01"
              min="0"
            />
            <div className="flex gap-3">
              <button onClick={onClose} className="btn-secondary flex-1">Cancel</button>
              <button onClick={handleTransferSubmit} className="btn-primary flex-1">Review Transfer</button>
            </div>
          </>
        )}
      </div>
    </div>
    </>
  );
}
