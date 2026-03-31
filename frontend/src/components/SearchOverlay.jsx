import { useState, useEffect, useRef, useMemo } from 'react';
import { Search, X, ArrowRight } from 'lucide-react';

export default function SearchOverlay({ accounts = [], transactions = [], onNavigate, onSelectTransaction, onClose }) {
  const [query, setQuery] = useState('');
  const inputRef = useRef(null);

  useEffect(() => { inputRef.current?.focus(); }, []);
  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [onClose]);

  const results = useMemo(() => {
    if (!query.trim()) return [];
    const q = query.toLowerCase();
    const items = [];

    for (const acc of accounts) {
      if ((acc.iban || '').toLowerCase().includes(q) || (acc.currencyCode || '').toLowerCase().includes(q)) {
        items.push({ type: 'account', label: `${acc.iban} (${acc.currencyCode})`, sub: `Balance: ${acc.balance}`, action: () => onNavigate?.('/dashboard/accounts') });
      }
    }

    for (const tx of transactions.slice(0, 200)) {
      const txText = `${tx.transactionTypeName || ''} ${tx.accountIban || ''} ${tx.amount || ''} ${tx.details || ''}`.toLowerCase();
      if (txText.includes(q)) {
        items.push({
          type: 'transaction',
          label: `${tx.transactionTypeName || 'Transaction'} — ${tx.amount} ${tx.currencyCode || ''}`,
          sub: tx.accountIban || '',
          action: () => { onSelectTransaction?.(tx.transactionId); onClose(); },
        });
      }
      if (items.length > 15) break;
    }

    const pages = [
      { name: 'Accounts', path: '/dashboard/accounts' },
      { name: 'Transactions', path: '/dashboard/transactions' },
      { name: 'Payments', path: '/dashboard/payments' },
      { name: 'Cards', path: '/dashboard/cards' },
      { name: 'Profile & Settings', path: '/dashboard/profile' },
    ];
    for (const p of pages) {
      if (p.name.toLowerCase().includes(q)) {
        items.push({ type: 'page', label: p.name, sub: 'Page', action: () => { onNavigate?.(p.path); onClose(); } });
      }
    }

    return items.slice(0, 20);
  }, [query, accounts, transactions, onNavigate, onSelectTransaction, onClose]);

  return (
    <div className="fixed inset-0 z-[70] flex items-start justify-center pt-[15vh]" onClick={onClose}>
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" />
      <div className="relative w-full max-w-lg glass rounded-2xl border border-white/10 shadow-2xl overflow-hidden animate-fade-in" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center gap-3 px-4 py-3 border-b border-white/10">
          <Search className="w-5 h-5 text-zinc-500 shrink-0" />
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search accounts, transactions, pages…"
            className="flex-1 bg-transparent text-white placeholder-zinc-500 outline-none text-sm"
          />
          <kbd className="hidden sm:inline text-xs text-zinc-500 bg-zinc-800 px-1.5 py-0.5 rounded">ESC</kbd>
          <button onClick={onClose} className="text-zinc-500 hover:text-zinc-300 p-1"><X className="w-4 h-4" /></button>
        </div>
        {query.trim() && (
          <div className="max-h-[50vh] overflow-y-auto p-2">
            {results.length === 0 ? (
              <p className="text-center text-zinc-500 text-sm py-8">No results found.</p>
            ) : (
              results.map((r, i) => (
                <button
                  key={i}
                  type="button"
                  onClick={r.action}
                  className="w-full flex items-center justify-between gap-3 px-3 py-2.5 rounded-xl text-left hover:bg-zinc-800/70 transition-colors group"
                >
                  <div className="min-w-0">
                    <p className="text-sm text-zinc-200 truncate">{r.label}</p>
                    <p className="text-xs text-zinc-500 truncate">{r.sub}</p>
                  </div>
                  <ArrowRight className="w-4 h-4 text-zinc-600 group-hover:text-emerald-400 shrink-0 transition-colors" />
                </button>
              ))
            )}
          </div>
        )}
      </div>
    </div>
  );
}
