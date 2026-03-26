import { Filter, ChevronLeft, ChevronRight, Receipt } from 'lucide-react';
import { TransactionCompactRow, getTransactionAccountText, getTransactionLabel } from './TransactionRow';

export default function UserTransactionsTab({
  accounts,
  selectedAccountFilter,
  setSelectedAccountFilter,
  filters,
  handleFilterChange,
  getTransactionTypes,
  showFilters,
  setShowFilters,
  filteredTransactions,
  resetFilters,
  transactions,
  paginatedTransactions,
  formatDate,
  formatCurrency,
  setSelectedTransactionId,
  totalPages,
  itemsPerPage,
  setItemsPerPage,
  currentPage,
  goToPage,
}) {
  return (
    <div className="space-y-8">
      <div>
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-4">
          <h2 className="text-2xl font-bold">Recent Transactions</h2>
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
            <select
              value={selectedAccountFilter}
              onChange={(e) => setSelectedAccountFilter(e.target.value)}
              className="input-field min-w-[200px]"
            >
              <option value="all">All Accounts</option>
              {accounts.map((account) => (
                <option key={account.id} value={account.iban}>
                  {account.iban} ({account.currencyCode})
                </option>
              ))}
            </select>
            <select
              value={filters.type}
              onChange={(e) => handleFilterChange('type', e.target.value)}
              className="input-field min-w-[170px]"
            >
              <option value="all">All types</option>
              {getTransactionTypes().map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
            <input
              type="date"
              value={filters.dateFrom}
              onChange={(e) => handleFilterChange('dateFrom', e.target.value)}
              className="input-field"
              title="Date from"
            />
            <input
              type="date"
              value={filters.dateTo}
              onChange={(e) => handleFilterChange('dateTo', e.target.value)}
              className="input-field"
              title="Date to"
            />
            <button
              onClick={() => setShowFilters(!showFilters)}
              className={`btn-secondary flex items-center justify-center gap-2 whitespace-nowrap ${showFilters ? 'bg-emerald-500/20 border-emerald-500/30' : ''}`}
            >
              <Filter className="w-4 h-4" />
              {showFilters ? 'Hide' : 'More'} Filters
            </button>
          </div>
        </div>

        {showFilters && (
          <div className="glass rounded-2xl p-6 mb-4 animate-fade-in">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-zinc-400 mb-2">Transaction Sign</label>
                <select
                  value={filters.sign}
                  onChange={(e) => handleFilterChange('sign', e.target.value)}
                  className="input-field"
                >
                  <option value="all">All</option>
                  <option value="+">Credit (+)</option>
                  <option value="-">Debit (-)</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-zinc-400 mb-2">Min Amount</label>
                <input
                  type="number"
                  value={filters.minAmount}
                  onChange={(e) => handleFilterChange('minAmount', e.target.value)}
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
                  onChange={(e) => handleFilterChange('maxAmount', e.target.value)}
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
              <button onClick={resetFilters} className="btn-secondary text-sm">
                Reset Filters
              </button>
            </div>
          </div>
        )}

        {transactions.length === 0 ? (
          <div className="glass rounded-2xl">
            <div className="flex flex-col items-center justify-center py-16 gap-3">
              <Receipt className="w-12 h-12 text-zinc-600" aria-hidden />
              <p className="text-gray-400 text-sm">No transactions yet</p>
              <p className="text-gray-500 text-xs text-center px-4">
                Make a transfer or top up to get started
              </p>
            </div>
          </div>
        ) : filteredTransactions.length === 0 ? (
          <div className="glass rounded-2xl p-12 text-center">
            <p className="text-zinc-400">No transactions match your filters</p>
            <button onClick={resetFilters} className="btn-secondary mt-4">
              Reset Filters
            </button>
          </div>
        ) : (
          <>
            <div className="md:hidden space-y-3">
              {paginatedTransactions.map((tx) => (
                <TransactionCompactRow
                  key={tx.id}
                  tx={tx}
                  formatDate={formatDate}
                  formatCurrency={formatCurrency}
                  onViewDetails={setSelectedTransactionId}
                />
              ))}
            </div>

            <div className="hidden md:block glass rounded-2xl overflow-hidden">
              <table className="w-full">
                <thead className="bg-zinc-800/90">
                  <tr>
                    <th className="sticky top-0 z-10 px-6 py-4 text-left text-xs font-medium text-zinc-400 uppercase bg-zinc-800/95">Date</th>
                    <th className="sticky top-0 z-10 px-6 py-4 text-left text-xs font-medium text-zinc-400 uppercase bg-zinc-800/95">Type</th>
                    <th className="sticky top-0 z-10 px-6 py-4 text-left text-xs font-medium text-zinc-400 uppercase bg-zinc-800/95">Account</th>
                    <th className="sticky top-0 z-10 px-6 py-4 text-right text-xs font-medium text-zinc-400 uppercase bg-zinc-800/95">Amount</th>
                    <th className="sticky top-0 z-10 px-6 py-4 text-right text-xs font-medium text-zinc-400 uppercase bg-zinc-800/95">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-800">
                  {paginatedTransactions.map((tx) => (
                    <tr key={tx.id} className="hover:bg-zinc-800/30">
                      <td className="px-6 py-4 text-sm text-zinc-300">{formatDate(tx.transactionDate)}</td>
                      <td className="px-6 py-4">
                        <span className="px-2 py-1 rounded text-xs font-medium bg-blue-500/20 text-blue-400">
                          {getTransactionLabel(tx)}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-sm font-mono text-zinc-400">{getTransactionAccountText(tx)}</td>
                      <td className={`px-6 py-4 text-sm font-bold text-right ${
                        tx.sign === '+' ? 'text-emerald-400' : 'text-red-400'
                      }`}>
                        {tx.sign === '+' ? '+' : '-'}
                        {formatCurrency(tx.amount, tx.currencyCode || tx.originalCurrencyCode)}
                      </td>
                      <td className="px-6 py-4 text-right">
                        <button
                          type="button"
                          className="btn-secondary text-xs py-1.5 px-3"
                          onClick={() => setSelectedTransactionId(tx.id)}
                        >
                          View details
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {totalPages > 1 && (
              <div className="flex items-center justify-between mt-4">
                <div className="flex items-center gap-2">
                  <label className="text-sm text-zinc-400">Items per page:</label>
                  <select
                    value={itemsPerPage}
                    onChange={(e) => setItemsPerPage(Number(e.target.value))}
                    className="input-field !py-1 !px-2 text-sm w-20"
                  >
                    <option value={10}>10</option>
                    <option value={25}>25</option>
                    <option value={50}>50</option>
                  </select>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => goToPage(currentPage - 1)}
                    disabled={currentPage === 1}
                    className="btn-secondary !p-2 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <ChevronLeft className="w-4 h-4" />
                  </button>

                  <div className="flex items-center gap-1">
                    {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                      let pageNum;
                      if (totalPages <= 5) pageNum = i + 1;
                      else if (currentPage <= 3) pageNum = i + 1;
                      else if (currentPage >= totalPages - 2) pageNum = totalPages - 4 + i;
                      else pageNum = currentPage - 2 + i;
                      return (
                        <button
                          key={pageNum}
                          onClick={() => goToPage(pageNum)}
                          className={`px-3 py-1 rounded-lg text-sm font-medium transition-all ${
                            currentPage === pageNum
                              ? 'bg-emerald-500 text-white'
                              : 'bg-zinc-800 text-zinc-400 hover:bg-zinc-700'
                          }`}
                        >
                          {pageNum}
                        </button>
                      );
                    })}
                  </div>

                  <button
                    onClick={() => goToPage(currentPage + 1)}
                    disabled={currentPage === totalPages}
                    className="btn-secondary !p-2 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <ChevronRight className="w-4 h-4" />
                  </button>

                  <span className="text-sm text-zinc-400 ml-2">
                    Page {currentPage} of {totalPages}
                  </span>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

