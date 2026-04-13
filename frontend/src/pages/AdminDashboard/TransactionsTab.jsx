import { Eye, Filter } from 'lucide-react';
import PaginationControls from './PaginationControls';
import { maskMoneyValue } from '@/lib/maskingUtils';

const MASK_TEXT = 'Hidden';

function transactionTypeOf(tx) {
  return tx.transactionType ?? tx.transactionTypeName ?? '—';
}

function signOf(tx) {
  return tx.sign ?? tx.transactionSign ?? '';
}

function amountOf(tx) {
  const v = tx.amount ?? tx.transactionAmount;
  if (v === null || v === undefined || v === '') return null;
  const n = parseFloat(v);
  return Number.isFinite(n) ? n : null;
}

function currencyOf(tx) {
  return tx.originalCurrency ?? tx.currencyCode ?? 'EUR';
}

function formatRiskScore(tx) {
  const raw = tx.riskScore;
  if (raw === null || raw === undefined || raw === '') return '—';
  const n = parseFloat(raw);
  if (!Number.isFinite(n)) return '—';
  const pct = n > 1 ? n : n * 100;
  return `${pct.toFixed(1)}%`;
}

function riskScoreColor(tx) {
  const raw = tx.riskScore;
  if (raw === null || raw === undefined || raw === '') return 'text-zinc-500';
  const n = parseFloat(raw);
  if (!Number.isFinite(n)) return 'text-zinc-500';
  const pct = n > 1 ? n : n * 100;
  if (pct > 70) return 'text-red-400';
  if (pct > 40) return 'text-amber-400';
  return 'text-emerald-400';
}

function transactionTypeBadgeClass(typeName) {
  const t = (typeName || '').toLowerCase();
  if (t.includes('transfer')) return 'bg-sky-500/15 text-sky-300 border border-sky-500/30';
  if (t.includes('deposit') || t.includes('top') || t.includes('credit'))
    return 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/30';
  if (t.includes('withdraw') || t.includes('debit') || t.includes('payment'))
    return 'bg-rose-500/15 text-rose-300 border border-rose-500/30';
  if (t.includes('fee')) return 'bg-orange-500/15 text-orange-300 border border-orange-500/30';
  return 'bg-violet-500/15 text-violet-300 border border-violet-500/30';
}

export default function TransactionsTab({
  transactions,
  filters,
  onFilterChange,
  onResetFilters,
  showFilters,
  onToggleFilters,
  onViewDetails,
  showSensitiveData,
  onRequestSensitiveReveal,
}) {
  const getTransactionTypes = () => {
    const types = new Set(transactions.map((tx) => transactionTypeOf(tx)).filter((x) => x && x !== '—'));
    return Array.from(types).sort();
  };

  const getFilteredTransactions = () => {
    let filtered = [...transactions];

    if (filters.type !== 'all') {
      filtered = filtered.filter((tx) => transactionTypeOf(tx) === filters.type);
    }

    if (filters.sign !== 'all') {
      filtered = filtered.filter((tx) => signOf(tx) === filters.sign);
    }

    if (filters.dateFrom) {
      const fromDate = new Date(filters.dateFrom);
      fromDate.setHours(0, 0, 0, 0);
      filtered = filtered.filter((tx) => new Date(tx.transactionDate) >= fromDate);
    }
    if (filters.dateTo) {
      const toDate = new Date(filters.dateTo);
      toDate.setHours(23, 59, 59, 999);
      filtered = filtered.filter((tx) => new Date(tx.transactionDate) <= toDate);
    }

    if (filters.minAmount !== '') {
      const min = parseFloat(filters.minAmount);
      filtered = filtered.filter((tx) => {
        const a = amountOf(tx);
        return a !== null && a >= min;
      });
    }
    if (filters.maxAmount !== '') {
      const max = parseFloat(filters.maxAmount);
      filtered = filtered.filter((tx) => {
        const a = amountOf(tx);
        return a !== null && a <= max;
      });
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
    <div className="space-y-3">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-4">
        <h2 className="text-lg font-semibold">Transactions ({filteredTransactions.length})</h2>
        <div className="flex flex-wrap items-center gap-2">
          {!showSensitiveData && (
            <button
              type="button"
              onClick={onRequestSensitiveReveal}
              className="btn-secondary flex items-center justify-center gap-2 border-amber-500/30 text-amber-300"
            >
              <Eye className="w-4 h-4" />
              Reveal data
            </button>
          )}
          <button
            onClick={onToggleFilters}
            className={`btn-secondary flex items-center justify-center gap-2 ${showFilters ? 'bg-emerald-500/20 border-emerald-500/30' : ''}`}
          >
            <Filter className="w-4 h-4" />
            {showFilters ? 'Hide' : 'Show'} Filters
          </button>
        </div>
      </div>

      {showFilters && (
        <div className="glass rounded-2xl p-4 mb-3 animate-fade-in border border-white/5">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">Transaction type</label>
              <select
                value={filters.type}
                onChange={(e) => onFilterChange('type', e.target.value)}
                className="input-field"
              >
                <option value="all">All types</option>
                {getTransactionTypes().map((type) => (
                  <option key={type} value={type}>
                    {type}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">Sign</label>
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
              <label className="block text-sm font-medium text-zinc-400 mb-2">Date from</label>
              <input
                type="date"
                value={filters.dateFrom}
                onChange={(e) => onFilterChange('dateFrom', e.target.value)}
                className="input-field"
              />
              {filters.dateFrom && !filters.dateTo && (
                <p className="text-xs text-amber-400 mt-1">Select &quot;Date to&quot; to complete the range</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">Date to</label>
              <input
                type="date"
                value={filters.dateTo}
                onChange={(e) => onFilterChange('dateTo', e.target.value)}
                className="input-field"
              />
              {!filters.dateFrom && filters.dateTo && (
                <p className="text-xs text-amber-400 mt-1">Select &quot;Date from&quot; to complete the range</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">Min amount</label>
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
              <label className="block text-sm font-medium text-zinc-400 mb-2">Max amount</label>
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

          <div className="flex items-center justify-between mt-3 pt-3 border-t border-zinc-700">
            <p className="text-xs text-zinc-400">
              Found {filteredTransactions.length} transaction{filteredTransactions.length !== 1 ? 's' : ''}
            </p>
            <button onClick={onResetFilters} className="btn-secondary text-xs py-1.5 px-3">
              Reset filters
            </button>
          </div>
        </div>
      )}

      {filteredTransactions.length === 0 ? (
        <div className="glass rounded-2xl p-8 text-center">
          <p className="text-zinc-400">No transactions match your filters</p>
          {(filters.type !== 'all' ||
            filters.sign !== 'all' ||
            filters.dateFrom ||
            filters.dateTo ||
            filters.minAmount ||
            filters.maxAmount) && (
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
                    <th className="px-4 py-3 text-left text-xs font-semibold text-zinc-300 uppercase tracking-wide">ID</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-zinc-300 uppercase tracking-wide">Date &amp; time</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-zinc-300 uppercase tracking-wide">Account ID</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-zinc-300 uppercase tracking-wide">Destination account ID</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-zinc-300 uppercase tracking-wide">Type</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-zinc-300 uppercase tracking-wide">Amount</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-zinc-300 uppercase tracking-wide">Risk score</th>
                    <th className="px-4 py-3 text-right text-xs font-semibold text-zinc-300 uppercase tracking-wide w-px">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-800">
                  {paginatedTransactions.map((tx) => {
                    const typeLabel = transactionTypeOf(tx);
                    const amt = amountOf(tx);
                    const cur = currencyOf(tx);
                    const sg = signOf(tx);
                    const destId = tx.destinationAccountId;

                    return (
                      <tr key={tx.transactionId} className="hover:bg-zinc-800/30 transition-colors">
                        <td className="px-4 py-3 text-sm font-mono text-zinc-400">{tx.transactionId}</td>
                        <td className="px-4 py-3 text-sm text-zinc-400">
                          {tx.transactionDate
                            ? new Date(tx.transactionDate).toLocaleString('en-GB', {
                                day: '2-digit',
                                month: '2-digit',
                                year: 'numeric',
                                hour: '2-digit',
                                minute: '2-digit',
                              })
                            : '—'}
                        </td>
                        <td className="px-4 py-3 text-sm font-mono text-zinc-300">
                          {tx.accountId != null ? tx.accountId : '—'}
                        </td>
                        <td className="px-4 py-3 text-sm font-mono text-zinc-400">
                          {showSensitiveData ? (destId != null ? destId : '—') : MASK_TEXT}
                        </td>
                        <td className="px-4 py-3 text-sm">
                          {showSensitiveData ? (
                            <span
                              className={`inline-flex px-2 py-1 rounded-md text-xs font-medium ${transactionTypeBadgeClass(typeLabel)}`}
                            >
                              {typeLabel}
                            </span>
                          ) : (
                            <span className="inline-flex px-2 py-1 rounded-md text-xs font-medium bg-zinc-700/50 text-zinc-400 border border-zinc-600/60">
                              {MASK_TEXT}
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-3 text-sm font-medium">
                          <span className={sg === '+' ? 'text-emerald-400' : 'text-red-400'}>
                            {sg === '+' ? '+' : sg === '-' ? '-' : ''}
                            {showSensitiveData ? (amt !== null ? amt.toFixed(2) : '—') : maskMoneyValue()} {cur}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-sm">
                          <span className={`font-semibold ${riskScoreColor(tx)}`}>{formatRiskScore(tx)}</span>
                        </td>
                        <td className="px-4 py-3 text-sm text-right">
                          <button
                            type="button"
                            className="text-xs text-emerald-400 hover:text-emerald-300 transition-colors font-medium"
                            onClick={() => onViewDetails?.(tx.transactionId)}
                          >
                            Details
                          </button>
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
