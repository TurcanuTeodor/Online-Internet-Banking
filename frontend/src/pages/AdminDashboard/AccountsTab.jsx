import { Filter, Search, FileText, Snowflake } from 'lucide-react';
import PaginationControls from './PaginationControls';

export default function AccountsTab({
  accounts,
  filters,
  onFilterChange,
  onResetFilters,
  showFilters,
  onToggleFilters,
  onViewStatement,
  onFreezeAccount,
}) {
  const getAccountStatuses = () => {
    const statuses = new Set(accounts.map(acc => acc.accountStatusName));
    return Array.from(statuses).sort();
  };

  const getAccountCurrencies = () => {
    const currencies = new Set(accounts.map(acc => acc.currencyCode));
    return Array.from(currencies).sort();
  };

  const getFilteredAccounts = () => {
    let filtered = [...accounts];

    if (filters.search) {
      const searchLower = filters.search.toLowerCase();
      filtered = filtered.filter(acc =>
        acc.accountIban?.toLowerCase().includes(searchLower) ||
        acc.clientId?.toString().includes(searchLower) ||
        acc.clientFirstName?.toLowerCase().includes(searchLower) ||
        acc.clientLastName?.toLowerCase().includes(searchLower) ||
        `${acc.clientFirstName || ''} ${acc.clientLastName || ''}`.trim().toLowerCase().includes(searchLower)
      );
    }

    if (filters.status !== 'all') {
      filtered = filtered.filter(acc => acc.accountStatusName === filters.status);
    }

    if (filters.currency !== 'all') {
      filtered = filtered.filter(acc => acc.currencyCode === filters.currency);
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
    <div>
      {/* Filter Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-4">
        <h2 className="text-xl font-bold">
          Accounts ({filteredAccounts.length})
        </h2>
        <button
          onClick={onToggleFilters}
          className={`btn-secondary flex items-center justify-center gap-2 ${showFilters ? 'bg-emerald-500/20 border-emerald-500/30' : ''}`}
        >
          <Filter className="w-4 h-4" />
          {showFilters ? 'Hide' : 'Show'} Filters
        </button>
      </div>

      {/* Filters */}
      {showFilters && (
        <div className="glass rounded-2xl p-6 mb-4 animate-fade-in">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">Search</label>
              <div className="relative">
                <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
                <input
                  type="text"
                  value={filters.search}
                  onChange={(e) => onFilterChange('search', e.target.value)}
                  className="input-field input-with-icon"
                  placeholder="IBAN or client..."
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
                <option value="all">All Statuses</option>
                {getAccountStatuses().map(status => (
                  <option key={status} value={status}>{status}</option>
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
                <option value="all">All Currencies</option>
                {getAccountCurrencies().map(currency => (
                  <option key={currency} value={currency}>{currency}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="flex items-center justify-between mt-4 pt-4 border-t border-zinc-700">
            <p className="text-sm text-zinc-400">
              Found {filteredAccounts.length} account{filteredAccounts.length !== 1 ? 's' : ''}
            </p>
            <button onClick={onResetFilters} className="btn-secondary text-sm">
              Reset Filters
            </button>
          </div>
        </div>
      )}

      {/* Table */}
      {filteredAccounts.length === 0 ? (
        <div className="glass rounded-2xl p-12 text-center">
          <p className="text-zinc-400">No accounts match your filters</p>
          {(filters.search || filters.status !== 'all' || filters.currency !== 'all') && (
            <button onClick={onResetFilters} className="btn-secondary mt-4">
              Reset Filters
            </button>
          )}
        </div>
      ) : (
        <>
          <div className="glass rounded-2xl overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-zinc-800/50">
                  <tr>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">IBAN</th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Client Name</th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Currency</th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Current Balance</th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Account Status</th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Opening Date</th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-800">
                  {paginatedAccounts.map((acc) => (
                    <tr key={acc.accountId} className="hover:bg-zinc-800/30 transition-colors">
                      <td className="px-6 py-4 text-sm font-mono text-xs">{acc.accountIban}</td>
                      <td className="px-6 py-4 text-sm font-medium">
                        {acc.clientFirstName} {acc.clientLastName}
                      </td>
                      <td className="px-6 py-4 text-sm">
                        <span className="px-2 py-1 bg-yellow-500/20 text-yellow-400 rounded text-xs">
                          {acc.currencyCode}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-sm font-medium text-emerald-400">
                        {parseFloat(acc.accountBalance || acc.balance).toFixed(2)}
                      </td>
                      <td className="px-6 py-4 text-sm">
                        <span className={`px-2 py-1 rounded text-xs ${
                          (acc.accountStatusName || acc.status) === 'ACTIVE' 
                            ? 'bg-emerald-500/20 text-emerald-400' 
                            : (acc.accountStatusName || acc.status) === 'SUSPENDED' 
                            ? 'bg-amber-500/20 text-amber-400'
                            : 'bg-red-500/20 text-red-400'
                        }`}>
                          {acc.accountStatusName || acc.status}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-sm text-zinc-400">
                        {acc.createdAt ? new Date(acc.createdAt).toLocaleDateString() : 'N/A'}
                      </td>
                      <td className="px-6 py-4 text-sm">
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => onViewStatement(acc)}
                            className="p-1.5 rounded-lg bg-blue-500/20 text-blue-400 hover:bg-blue-500/30 transition-colors"
                            title="View Statement"
                          >
                            <FileText className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => onFreezeAccount(acc)}
                            className="p-1.5 rounded-lg bg-amber-500/20 text-amber-400 hover:bg-amber-500/30 transition-colors"
                            title="Freeze Account"
                            disabled={(acc.accountStatusName || acc.status) !== 'ACTIVE'}
                          >
                            <Snowflake className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Pagination */}
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
