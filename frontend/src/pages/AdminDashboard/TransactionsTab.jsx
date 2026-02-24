import { Filter } from 'lucide-react';
import PaginationControls from './PaginationControls';

export default function TransactionsTab({
  transactions,
  filters,
  onFilterChange,
  onResetFilters,
  showFilters,
  onToggleFilters,
}) {
  const getTransactionTypes = () => {
    const types = new Set(transactions.map(tx => tx.transactionTypeName));
    return Array.from(types).sort();
  };

  const getFilteredTransactions = () => {
    let filtered = [...transactions];

    if (filters.type !== 'all') {
      filtered = filtered.filter(tx => tx.transactionTypeName === filters.type);
    }

    if (filters.sign !== 'all') {
      filtered = filtered.filter(tx => tx.transactionSign === filters.sign);
    }

    if (filters.dateFrom) {
      const fromDate = new Date(filters.dateFrom);
      fromDate.setHours(0, 0, 0, 0);
      filtered = filtered.filter(tx => new Date(tx.transactionDate) >= fromDate);
    }
    if (filters.dateTo) {
      const toDate = new Date(filters.dateTo);
      toDate.setHours(23, 59, 59, 999);
      filtered = filtered.filter(tx => new Date(tx.transactionDate) <= toDate);
    }

    if (filters.minAmount !== '') {
      filtered = filtered.filter(tx => tx.transactionAmount >= parseFloat(filters.minAmount));
    }
    if (filters.maxAmount !== '') {
      filtered = filtered.filter(tx => tx.transactionAmount <= parseFloat(filters.maxAmount));
    }

    return filtered;
  };

  const filteredTransactions = getFilteredTransactions();
  const totalPages = Math.ceil(filteredTransactions.length / filters.itemsPerPage);
  const paginatedTransactions = filteredTransactions.slice(
    (filters.page - 1) * filters.itemsPerPage,
    filters.page * filters.itemsPerPage
  );

  return (
    <div>
      {/* Filter Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-4">
        <h2 className="text-xl font-bold">
          Transactions ({filteredTransactions.length})
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
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">Transaction Type</label>
              <select
                value={filters.type}
                onChange={(e) => onFilterChange('type', e.target.value)}
                className="input-field"
              >
                <option value="all">All Types</option>
                {getTransactionTypes().map(type => (
                  <option key={type} value={type}>{type}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">Transaction Sign</label>
              <select
                value={filters.sign}
                onChange={(e) => onFilterChange('sign', e.target.value)}
                className="input-field"
              >
                <option value="all">All</option>
                <option value="+">Credit (+)</option>
                <option value="-">Debit (-)</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">Date From</label>
              <input
                type="date"
                value={filters.dateFrom}
                onChange={(e) => onFilterChange('dateFrom', e.target.value)}
                className="input-field"
              />
              {filters.dateFrom && !filters.dateTo && (
                <p className="text-xs text-amber-400 mt-1">⚠️ Please select "Date To"</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">Date To</label>
              <input
                type="date"
                value={filters.dateTo}
                onChange={(e) => onFilterChange('dateTo', e.target.value)}
                className="input-field"
              />
              {!filters.dateFrom && filters.dateTo && (
                <p className="text-xs text-amber-400 mt-1">⚠️ Please select "Date From"</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">Min Amount</label>
              <input
                type="number"
                value={filters.minAmount}
                onChange={(e) => onFilterChange('minAmount', e.target.value)}
                className="input-field"
                placeholder="0.00"
                step="0.01"
                min="0"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">Max Amount</label>
              <input
                type="number"
                value={filters.maxAmount}
                onChange={(e) => onFilterChange('maxAmount', e.target.value)}
                className="input-field"
                placeholder="0.00"
                step="0.01"
                min="0"
              />
            </div>
          </div>

          <div className="flex items-center justify-between mt-4 pt-4 border-t border-zinc-700">
            <p className="text-sm text-zinc-400">
              Found {filteredTransactions.length} transaction{filteredTransactions.length !== 1 ? 's' : ''}
            </p>
            <button onClick={onResetFilters} className="btn-secondary text-sm">
              Reset Filters
            </button>
          </div>
        </div>
      )}

      {/* Table */}
      {filteredTransactions.length === 0 ? (
        <div className="glass rounded-2xl p-12 text-center">
          <p className="text-zinc-400">No transactions match your filters</p>
          {(filters.type !== 'all' || filters.sign !== 'all' || 
            filters.dateFrom || filters.dateTo || 
            filters.minAmount || filters.maxAmount) && (
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
                    <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">ID</th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Date & Time</th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Sender</th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Receiver</th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Type</th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Amount</th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Fraud Score</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-800">
                  {paginatedTransactions.map((tx) => {
                    const fraudScoreValue = tx.fraudScore === null || tx.fraudScore === undefined
                      ? null
                      : parseFloat(tx.fraudScore);
                    const fraudPercent = fraudScoreValue === null
                      ? null
                      : (fraudScoreValue > 1 ? fraudScoreValue : fraudScoreValue * 100);
                    const fraudColor = fraudPercent === null
                      ? 'text-zinc-500'
                      : fraudPercent > 70
                        ? 'text-red-400'
                        : fraudPercent > 40
                          ? 'text-amber-400'
                          : 'text-emerald-400';

                    const typeLower = (tx.transactionTypeName || '').toLowerCase();
                    const isTransfer = typeLower.includes('transfer');
                    const receiverName = (tx.destFirstName && tx.destLastName)
                      ? `${tx.destFirstName} ${tx.destLastName}`
                      : tx.destIban
                        ? 'External'
                        : `(${tx.transactionTypeName || 'N/A'})`;
                    const receiverIban = isTransfer ? (tx.destIban || 'External') : '';
                    
                    return (
                      <tr key={tx.transactionId} className="hover:bg-zinc-800/30 transition-colors">
                        <td className="px-6 py-4 text-sm text-zinc-400">{tx.transactionId}</td>
                        <td className="px-6 py-4 text-sm text-zinc-400">
                          {new Date(tx.transactionDate).toLocaleString('en-GB', {
                            day: '2-digit',
                            month: '2-digit',
                            year: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit'
                          })}
                        </td>
                        <td className="px-6 py-4 text-sm">
                          <div>
                            <p className="font-medium">
                              {tx.sourceFirstName && tx.sourceLastName 
                                ? `${tx.sourceFirstName} ${tx.sourceLastName}` 
                                : 'N/A'}
                            </p>
                            <p className="text-xs text-zinc-500 font-mono">{tx.sourceIban}</p>
                          </div>
                        </td>
                        <td className="px-6 py-4 text-sm">
                          <div>
                            <p className="font-medium">
                              {receiverName}
                            </p>
                            <p className="text-xs text-zinc-500 font-mono">{receiverIban}</p>
                          </div>
                        </td>
                        <td className="px-6 py-4 text-sm">
                          <span className="px-2 py-1 bg-purple-500/20 text-purple-400 rounded text-xs">
                            {tx.transactionTypeName}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-sm font-medium">
                          <span className={tx.transactionSign === '+' ? 'text-emerald-400' : 'text-red-400'}>
                            {tx.transactionSign === '+' ? '+' : '-'}{tx.transactionAmount} {tx.currencyCode}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-sm">
                          <span className={`font-semibold ${fraudColor}`}>
                            {fraudPercent === null ? 'N/A' : `${fraudPercent.toFixed(1)}%`}
                          </span>
                        </td>
                      </tr>
                    );
                  })}
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
