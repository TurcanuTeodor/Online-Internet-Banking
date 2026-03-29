import { Wallet, Plus, Send, Eye, EyeOff, CreditCard } from 'lucide-react';
import UserAnalyticsSection from './UserAnalyticsSection';
import { TransactionCompactRow } from './TransactionRow';

export default function UserAccountsTab({
  accounts,
  transactions,
  latestActivity,
  showBalances,
  setShowBalances,
  setActiveModal,
  setSelectedAccount,
  setTopUpAccount,
  onViewAllActivity,
  formatCurrency,
  totalBalance,
  activeAccountsCount,
  monthlyOutgoing,
  onExpenseTypeSelect,
}) {
  return (
    <div className="space-y-8">
      <div className="glass rounded-2xl p-6">
        <h2 className="text-2xl font-bold">Accounts</h2>
        <p className="text-zinc-500 text-sm mt-1">
          Manage balances, open new accounts, and initiate transfers.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="glass rounded-2xl p-5">
          <p className="text-xs uppercase tracking-wide text-zinc-500">Total balances</p>
          <p className="text-2xl font-bold mt-2">{formatCurrency(totalBalance, 'EUR')}</p>
        </div>
        <div className="glass rounded-2xl p-5">
          <p className="text-xs uppercase tracking-wide text-zinc-500">Active accounts</p>
          <p className="text-2xl font-bold mt-2">{activeAccountsCount}</p>
        </div>
        <div className="glass rounded-2xl p-5">
          <p className="text-xs uppercase tracking-wide text-zinc-500">This month outflow</p>
          <p className="text-2xl font-bold mt-2 text-red-400">-{formatCurrency(monthlyOutgoing, 'EUR')}</p>
        </div>
      </div>

      <div>
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-4">
          <h2 className="text-2xl font-bold">Your Accounts</h2>
          <div className="flex flex-wrap gap-3">
            <button
              onClick={() => setShowBalances(!showBalances)}
              className="btn-secondary flex items-center gap-2"
            >
              {showBalances ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              {showBalances ? 'Hide' : 'Show'} Balances
            </button>
            <button
              onClick={() => setActiveModal('openAccount')}
              className="btn-primary flex items-center gap-2"
            >
              <Plus className="w-4 h-4" />
              Open Account
            </button>
          </div>
        </div>

        {accounts.length === 0 ? (
          <div className="glass rounded-2xl p-12 text-center">
            <Wallet className="w-16 h-16 text-zinc-600 mx-auto mb-4" />
            <p className="text-zinc-400 mb-4">No accounts yet</p>
            <button onClick={() => setActiveModal('openAccount')} className="btn-primary">
              Open Your First Account
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {accounts.map((account) => {
              const canTopUp =
                account.status === 'ACTIVE' &&
                (account.currencyCode === 'EUR' || account.currencyCode === 'RON');
              
              // Determine currency gradient
              let accentFrom = 'from-emerald-500/20';
              let accentTo = 'to-emerald-600/10';
              let accentIcon = 'text-emerald-400';
              let accentBorder = 'hover:border-emerald-500/30';
              
              if (account.currencyCode === 'EUR') {
                accentFrom = 'from-blue-500/20'; accentTo = 'to-blue-600/10'; accentIcon = 'text-blue-400'; accentBorder = 'hover:border-blue-500/30';
              } else if (account.currencyCode === 'USD') {
                accentFrom = 'from-emerald-500/20'; accentTo = 'to-emerald-600/10'; accentIcon = 'text-emerald-400'; accentBorder = 'hover:border-emerald-500/30';
              } else if (account.currencyCode === 'GBP') {
                accentFrom = 'from-violet-500/20'; accentTo = 'to-violet-600/10'; accentIcon = 'text-violet-400'; accentBorder = 'hover:border-violet-500/30';
              } else if (account.currencyCode === 'RON') {
                accentFrom = 'from-orange-500/20'; accentTo = 'to-orange-600/10'; accentIcon = 'text-orange-400'; accentBorder = 'hover:border-orange-500/30';
              }

              return (
                <div key={account.id} className={`glass rounded-2xl p-6 ${accentBorder} transition-all flex flex-col stat-card`}>
                  <div className="flex items-center justify-between gap-3 mb-3">
                    <div className="flex items-center gap-2 min-w-0">
                      <div className={`w-12 h-12 bg-gradient-to-br ${accentFrom} ${accentTo} rounded-xl flex items-center justify-center shrink-0`}>
                        <Wallet className={`w-6 h-6 ${accentIcon}`} />
                      </div>
                      <span className="px-2.5 py-1 rounded-lg text-xs font-semibold bg-zinc-800 text-zinc-200 border border-zinc-600/50">
                        {account.currencyCode}
                      </span>
                    </div>
                    <span
                      className={`shrink-0 px-3 py-1 rounded-lg text-xs font-medium ${
                        account.status === 'ACTIVE'
                          ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/20'
                          : 'bg-zinc-800 text-zinc-400 border border-zinc-700'
                      }`}
                    >
                      {account.status}
                    </span>
                  </div>
                  
                  <div 
                    className="mt-3 group relative cursor-pointer"
                    onClick={() => {
                      navigator.clipboard.writeText(account.iban);
                      // Optional: You could fire a toast here
                    }}
                    title="Click to copy IBAN"
                  >
                    <p className="text-xs text-zinc-500 mb-0.5">IBAN</p>
                    <p className="text-sm font-mono text-zinc-300 break-all leading-snug group-hover:text-white transition-colors">
                      {account.iban}
                    </p>
                    <div className="absolute top-1/2 -translate-y-1/2 right-0 opacity-0 group-hover:opacity-100 transition-opacity bg-zinc-800 text-xs px-2 py-1 rounded shadow-lg text-zinc-300">
                      Copy
                    </div>
                  </div>

                  <p className="text-3xl font-bold mt-4 mb-6">
                    {showBalances ? formatCurrency(account.balance, account.currencyCode) : '••••••'}
                  </p>
                  
                  <div className={`mt-auto grid gap-3 ${canTopUp ? 'grid-cols-2' : 'grid-cols-1'}`}>
                    <button
                      onClick={() => {
                        setSelectedAccount(account);
                        setActiveModal('transfer');
                      }}
                      className="btn-primary w-full flex items-center justify-center gap-2"
                    >
                      <Send className="w-4 h-4" />
                      Transfer
                    </button>
                    {canTopUp && (
                      <button
                        type="button"
                        onClick={() => setTopUpAccount(account)}
                        className="btn-secondary w-full flex items-center justify-center gap-2 border-emerald-500/30 text-emerald-300"
                      >
                        <CreditCard className="w-4 h-4" />
                        Top up
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      <UserAnalyticsSection transactions={transactions} onExpenseTypeSelect={onExpenseTypeSelect} />

      <section className="glass rounded-2xl p-6">
        <div className="flex items-center justify-between gap-3 mb-4">
          <h3 className="text-xl font-bold">Latest Activity</h3>
          <button type="button" className="btn-secondary text-sm" onClick={onViewAllActivity}>
            View all
          </button>
        </div>
        {!latestActivity || latestActivity.length === 0 ? (
          <p className="text-zinc-500 text-sm">No recent activity.</p>
        ) : (
          <div className="space-y-3">
            {latestActivity.map((tx) => (
              <TransactionCompactRow
                key={tx.id}
                tx={tx}
                formatDate={(d) => (d ? new Date(d).toLocaleString() : '—')}
                formatCurrency={formatCurrency}
                showAccount={false}
              />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

