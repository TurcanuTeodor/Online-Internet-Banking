import { Filter, Search } from 'lucide-react';
import PaginationControls from './PaginationControls';
import RowActionsMenu from './RowActionsMenu';

function accountStatus(acc) {
  return acc.accountStatusName ?? acc.status ?? '—';
}

function formatBalance(acc) {
  const raw = acc.accountBalance ?? acc.balance;
  if (raw === null || raw === undefined || raw === '') return '—';
  const n = parseFloat(raw);
  return Number.isFinite(n) ? n.toFixed(2) : '—';
}

export default function AccountsTab({
  accounts,
  filters,
  onFilterChange,
  onResetFilters,
  showFilters,
  onToggleFilters,
  onViewStatement,
  onFreezeAccount,
  onCloseAccount,
}) {
  const getAccountStatuses = () => {
    const statuses = new Set(accounts.map((acc) => accountStatus(acc)).filter((s) => s && s !== '—'));
    return Array.from(statuses).sort();
  };

  const getAccountCurrencies = () => {
    const currencies = new Set(accounts.map((acc) => acc.currencyCode).filter(Boolean));
    return Array.from(currencies).sort();
  };

  const getFilteredAccounts = () => {
    let filtered = [...accounts];

    if (filters.search?.trim()) {
      const searchLower = filters.search.trim().toLowerCase();
      filtered = filtered.filter((acc) => {
        const iban = (acc.accountIban || '').toLowerCase();
        const id = String(acc.clientId ?? '');
        return iban.includes(searchLower) || id.includes(searchLower);
      });
    }

    if (filters.status !== 'all') {
      filtered = filtered.filter((acc) => accountStatus(acc) === filters.status);
    }

    if (filters.currency !== 'all') {
      filtered = filtered.filter((acc) => acc.currencyCode === filters.currency);
    }

    return filtered;
  };

  const filteredAccounts = getFilteredAccounts();
  const totalPages = Math.ceil(filteredAccounts.length / filters.itemsPerPage);
  const paginatedAccounts = filteredAccounts.slice(
    (filters.page - 1) * filters.itemsPerPage,
    filters.page * filters.itemsPerPage
  );

  return (
    <div className="space-y-3">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-4">
        <h2 className="text-lg font-semibold">Accounts ({filteredAccounts.length})</h2>
        <button
          onClick={onToggleFilters}
          className={`btn-secondary flex items-center justify-center gap-2 ${showFilters ? 'bg-emerald-500/20 border-emerald-500/30' : ''}`}
        >
          <Filter className="w-4 h-4" />
          {showFilters ? 'Hide' : 'Show'} Filters
        </button>
      </div>

      {showFilters && (
        <div className="glass rounded-2xl p-4 mb-3 animate-fade-in border border-white/5">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">Search (IBAN or client ID)</label>
              <div className="relative">
                <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
                <input
                  type="text"
                  value={filters.search}
                  onChange={(e) => onFilterChange('search', e.target.value)}
                  className="input-field input-with-icon"
                  placeholder="IBAN or numeric client ID"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">Status</label>
              <select
                value={filters.status}
                onChange={(e) => onFilterChange('status', e.target.value)}
                className="input-field"
              >
                <option value="all">All statuses</option>
                {getAccountStatuses().map((status) => (
                  <option key={status} value={status}>
                    {status}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">Currency</label>
              <select
                value={filters.currency}
                onChange={(e) => onFilterChange('currency', e.target.value)}
                className="input-field"
              >
                <option value="all">All currencies</option>
                {getAccountCurrencies().map((currency) => (
                  <option key={currency} value={currency}>
                    {currency}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="flex items-center justify-between mt-3 pt-3 border-t border-zinc-700">
            <p className="text-xs text-zinc-400">
              Found {filteredAccounts.length} account{filteredAccounts.length !== 1 ? 's' : ''}
            </p>
            <button onClick={onResetFilters} className="btn-secondary text-xs py-1.5 px-3">
              Reset filters
            </button>
          </div>
        </div>
      )}

      {filteredAccounts.length === 0 ? (
        <div className="glass rounded-2xl p-12 text-center">
          <p className="text-zinc-400">No accounts match your filters</p>
          {(filters.search || filters.status !== 'all' || filters.currency !== 'all') && (
            <button onClick={onResetFilters} className="btn-secondary mt-4">
              Reset filters
            </button>
          )}
        </div>
      ) : (
        <>
          <div className="glass rounded-2xl overflow-hidden border border-white/5">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-zinc-800/50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-zinc-300 uppercase tracking-wide">IBAN</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-zinc-300 uppercase tracking-wide">Client ID</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-zinc-300 uppercase tracking-wide">Currency</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-zinc-300 uppercase tracking-wide">Balance</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-zinc-300 uppercase tracking-wide">Status</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-zinc-300 uppercase tracking-wide">Opened</th>
                    <th className="px-4 py-3 text-right text-xs font-semibold text-zinc-300 uppercase tracking-wide w-px">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-800">
                  {paginatedAccounts.map((acc) => {
                    const st = accountStatus(acc);
                    return (
                      <tr key={acc.accountId} className="hover:bg-zinc-800/30 transition-colors">
                        <td className="px-4 py-3 text-sm font-mono text-zinc-300">{acc.accountIban}</td>
                        <td className="px-4 py-3 text-sm font-mono text-zinc-400">{acc.clientId ?? '—'}</td>
                        <td className="px-4 py-3 text-sm">
                          <span className="px-2 py-1 bg-yellow-500/20 text-yellow-400 rounded text-xs">
                            {acc.currencyCode}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-sm font-medium text-emerald-400">{formatBalance(acc)}</td>
                        <td className="px-4 py-3 text-sm">
                          <span
                            className={`px-2 py-1 rounded text-xs ${
                              st === 'ACTIVE'
                                ? 'bg-emerald-500/20 text-emerald-400'
                                : st === 'SUSPENDED'
                                  ? 'bg-amber-500/20 text-amber-400'
                                  : 'bg-red-500/20 text-red-400'
                            }`}
                          >
                            {st}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-sm text-zinc-400">
                          {acc.createdAt ? new Date(acc.createdAt).toLocaleDateString() : '—'}
                        </td>
                        <td className="px-4 py-3 text-sm text-right">
                          <RowActionsMenu
                            actions={[
                              { label: 'View statement', onClick: () => onViewStatement(acc) },
                              {
                                label: 'Freeze account',
                                onClick: () => onFreezeAccount(acc),
                                disabled: st !== 'ACTIVE',
                                danger: false,
                              },
                              {
                                label: 'Close account',
                                onClick: () => onCloseAccount?.(acc),
                                disabled: st === 'CLOSED',
                                danger: true,
                              },
                            ]}
                          />
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {totalPages > 1 && (
            <PaginationControls
              currentPage={filters.page}
              totalPages={totalPages}
              itemsPerPage={filters.itemsPerPage}
              onPageChange={(page) => onFilterChange('page', page)}
              onItemsPerPageChange={(itemsPerPage) => onFilterChange('itemsPerPage', itemsPerPage)}
            />
          )}
        </>
      )}
    </div>
  );
}
