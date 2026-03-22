import { X, FileText } from 'lucide-react';
import { useState, useEffect } from 'react';
import { getTransactionsByIban } from '../../../services/transactionService';

export default function AccountStatementModal({ account, onClose }) {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchTransactions();
  }, [account]);

  const fetchTransactions = async () => {
    setLoading(true);
    try {
      const data = await getTransactionsByIban(account.accountIban);
      setTransactions(data);
    } catch (err) {
      console.error('Error fetching transactions:', err);
    } finally {
      setLoading(false);
    }
  };

  if (!account) return null;

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="glass rounded-2xl p-6 max-w-5xl w-full max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-2xl font-bold flex items-center gap-2">
              <FileText className="w-6 h-6 text-emerald-400" />
              Account Statement
            </h2>
            <p className="text-sm text-zinc-400 mt-1">
              IBAN: <span className="font-mono text-zinc-300">{account.accountIban}</span> • Client ID:{' '}
              <span className="font-mono">{account.clientId ?? '—'}</span>
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-lg hover:bg-zinc-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Account Summary */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
          <div className="glass rounded-xl p-4">
            <label className="text-sm text-zinc-400">Current Balance</label>
            <p className="text-2xl font-bold text-emerald-400">
              {(() => {
                const raw = account.accountBalance ?? account.balance;
                const n = parseFloat(raw);
                return Number.isFinite(n) ? n.toFixed(2) : '—';
              })()}{' '}
              {account.currencyCode}
            </p>
          </div>
          <div className="glass rounded-xl p-4">
            <label className="text-sm text-zinc-400">Total Transactions</label>
            <p className="text-2xl font-bold">{transactions.length}</p>
          </div>
          <div className="glass rounded-xl p-4">
            <label className="text-sm text-zinc-400">Account Status</label>
            <p className="text-lg">
              <span className={`px-2 py-1 rounded text-sm ${
                (account.status || account.accountStatusName) === 'ACTIVE' 
                  ? 'bg-emerald-500/20 text-emerald-400' 
                  : 'bg-red-500/20 text-red-400'
              }`}>
                {account.status || account.accountStatusName}
              </span>
            </p>
          </div>
        </div>

        {/* Transactions Table */}
        <div className="mb-4">
          <h3 className="text-lg font-semibold mb-3">Transaction History</h3>
          {loading ? (
            <div className="glass rounded-xl p-12 text-center">
              <p className="text-zinc-400">Loading transactions...</p>
            </div>
          ) : transactions.length === 0 ? (
            <div className="glass rounded-xl p-12 text-center">
              <p className="text-zinc-400">No transactions found</p>
            </div>
          ) : (
            <div className="glass rounded-xl overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-zinc-800/50">
                    <tr>
                      <th className="px-4 py-3 text-left text-sm font-semibold text-zinc-300">Date</th>
                      <th className="px-4 py-3 text-left text-sm font-semibold text-zinc-300">Type</th>
                      <th className="px-4 py-3 text-left text-sm font-semibold text-zinc-300">Amount</th>
                      <th className="px-4 py-3 text-left text-sm font-semibold text-zinc-300">Details</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-800">
                    {transactions.map((tx) => {
                      const tid = tx.transactionId ?? tx.id;
                      const typeLabel = tx.transactionTypeName ?? tx.transactionType ?? '—';
                      const amt = tx.amount ?? tx.transactionAmount;
                      const cur = tx.currencyCode ?? tx.originalCurrency ?? '';
                      const sg = tx.sign ?? tx.transactionSign ?? '';
                      return (
                        <tr key={tid} className="hover:bg-zinc-800/30 transition-colors">
                          <td className="px-4 py-3 text-sm text-zinc-400">
                            {tx.transactionDate ? new Date(tx.transactionDate).toLocaleString() : '—'}
                          </td>
                          <td className="px-4 py-3 text-sm">
                            <span className="px-2 py-1 bg-purple-500/20 text-purple-400 rounded text-xs">
                              {typeLabel}
                            </span>
                          </td>
                          <td className="px-4 py-3 text-sm font-medium">
                            <span className={sg === '+' ? 'text-emerald-400' : 'text-red-400'}>
                              {sg === '+' ? '+' : sg === '-' ? '-' : ''}
                              {amt != null ? parseFloat(amt).toFixed(2) : '—'} {cur}
                            </span>
                          </td>
                          <td className="px-4 py-3 text-sm text-zinc-400">{tx.details || '—'}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>

        <div className="flex items-center justify-end gap-3 pt-4 border-t border-zinc-700">
          <button onClick={onClose} className="btn-primary">
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
