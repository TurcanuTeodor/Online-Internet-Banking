import { X, AlertTriangle } from 'lucide-react';

export default function FreezeAccountModal({ account, onClose, onConfirm }) {
  if (!account) return null;

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="glass rounded-2xl p-6 max-w-md w-full">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-2xl font-bold text-amber-400 flex items-center gap-2">
            <AlertTriangle className="w-6 h-6" />
            Freeze Account
          </h2>
          <button
            onClick={onClose}
            className="p-2 rounded-lg hover:bg-zinc-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <p className="text-zinc-300 mb-4">
          Are you sure you want to freeze account <strong className="text-white">{account.accountIban}</strong>?
        </p>

        <div className="glass rounded-xl p-4 mb-6">
          <div className="mb-2">
            <label className="text-xs text-zinc-400">Account Holder</label>
            <p className="text-sm font-medium">{account.clientFirstName} {account.clientLastName}</p>
          </div>
          <div>
            <label className="text-xs text-zinc-400">Current Balance</label>
            <p className="text-sm font-medium">
              {parseFloat(account.balance || account.accountBalance).toFixed(2)} {account.currencyCode}
            </p>
          </div>
        </div>

        <div className="bg-amber-500/10 border border-amber-500/30 rounded-lg p-4 mb-6">
          <p className="text-sm text-amber-400">
            ⚠️ This will suspend the account and prevent any transactions until it is reactivated.
          </p>
        </div>

        <div className="flex items-center justify-end gap-3">
          <button onClick={onClose} className="btn-secondary">
            Cancel
          </button>
          <button
            onClick={onConfirm}
            className="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-white rounded-xl font-medium transition-colors"
          >
            Confirm Freeze
          </button>
        </div>
      </div>
    </div>
  );
}
