import { useState } from 'react';
import { openAccount } from '@/services/accountService';
import { toast } from 'sonner';

export default function OpenAccountModal({ clientId, onClose, onSuccess }) {
  const [newAccountCurrency, setNewAccountCurrency] = useState('EUR');
  const [loading, setLoading] = useState(false);

  const handleOpenAccount = async () => {
    setLoading(true);
    try {
      await openAccount(clientId, newAccountCurrency);
      toast.success('Account opened successfully!');
      onSuccess();
      onClose();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to open account');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50" onClick={onClose}>
      <div className="glass rounded-2xl p-6 max-w-md w-full animate-fade-in" onClick={(e) => e.stopPropagation()}>
        <h3 className="text-xl font-bold mb-4">Open New Account</h3>
        <label className="block text-sm font-medium text-zinc-400 mb-2">Currency</label>
        <select value={newAccountCurrency} onChange={(e) => setNewAccountCurrency(e.target.value)} className="input-field mb-4" disabled={loading}>
          <option value="EUR">EUR - Euro</option>
          <option value="USD">USD - US Dollar</option>
          <option value="RON">RON - Romanian Leu</option>
          <option value="GBP">GBP - British Pound</option>
        </select>
        <div className="flex gap-3">
          <button onClick={onClose} disabled={loading} className="btn-secondary flex-1">Cancel</button>
          <button onClick={handleOpenAccount} disabled={loading} className="btn-primary flex-1">
            {loading ? 'Opening...' : 'Open Account'}
          </button>
        </div>
      </div>
    </div>
  );
}
