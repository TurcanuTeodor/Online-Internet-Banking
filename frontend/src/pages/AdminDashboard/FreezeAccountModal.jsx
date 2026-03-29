import { X, AlertTriangle } from 'lucide-react';

function formatBalance(acc) {
  const raw = acc.accountBalance ?? acc.balance;
  if (raw === null || raw === undefined || raw === '') return '—';
  const n = parseFloat(raw);
  return Number.isFinite(n) ? n.toFixed(2) : '—';
}

export default function FreezeAccountModal({ account, onClose, onConfirm }) {
  if (!account) return null;

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="glass rounded-2xl p-4 max-w-md w-full">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-amber-400 flex items-center gap-2">
            <AlertTriangle className="w-6 h-6" />
            Freeze account
          </h2>
          <button onClick={onClose} className="p-2 rounded-lg hover:bg-zinc-800 transition-colors" type="button">
            <X className="w-5 h-5" />
          </button>
        </div>

        <p className="text-zinc-300 mb-3 text-sm">
          Freeze account <strong className="text-white font-mono">{account.accountIban}</strong> (client ID{' '}
          <span className="font-mono">{account.clientId ?? '—'}</span>)?
        </p>

        <div className="glass rounded-xl p-3 mb-4 space-y-2">
          <div>
            <label className="text-xs text-zinc-400">Balance</label>
            <p className="text-sm font-medium">
              {formatBalance(account)} {account.currencyCode}
            </p>
          </div>
        </div>

        <div className="bg-amber-500/10 border border-amber-500/30 rounded-lg p-3 mb-4">
          <p className="text-sm text-amber-400">
            This suspends the account and blocks transactions until reactivated.
          </p>
        </div>

        <div className="flex items-center justify-end gap-2">
          <button type="button" onClick={onClose} className="btn-secondary text-sm">
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            className="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-white rounded-xl font-medium transition-colors text-sm"
          >
            Confirm freeze
          </button>
        </div>
      </div>
    </div>
  );
}
