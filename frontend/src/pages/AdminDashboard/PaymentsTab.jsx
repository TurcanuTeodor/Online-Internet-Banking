import { useState, useEffect, useMemo } from 'react';
import { Filter, Search, Loader2, RotateCcw, CreditCard } from 'lucide-react';
import PaginationControls from './PaginationControls';
import { getPaymentHistory, requestRefund } from '@/services/paymentService';

function paymentIdOf(p) {
  return p?.id ?? p?.paymentId ?? '—';
}

function formatAmount(p) {
  const v = p?.amount;
  if (v === null || v === undefined || v === '') return '—';
  const n = typeof v === 'number' ? v : parseFloat(v);
  return Number.isFinite(n) ? n.toFixed(2) : '—';
}

function formatCreated(p) {
  const raw = p?.createdAt ?? p?.created_at;
  if (!raw) return '—';
  try { return new Date(raw).toLocaleString(); } catch { return '—'; }
}

function statusBadgeClass(status) {
  const s = String(status || '').toUpperCase();
  if (s === 'SUCCEEDED') return 'badge badge-active';
  if (s === 'FAILED') return 'badge badge-block';
  if (s === 'PENDING') return 'badge badge-review';
  return 'badge bg-zinc-500/15 text-zinc-300 border border-zinc-500/25';
}

export default function PaymentsTab({ clients = [] }) {
  const [showFilters, setShowFilters] = useState(true);
  const [clientIdSearch, setClientIdSearch] = useState('');
  const [loadedClientId, setLoadedClientId] = useState(null);
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [page, setPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(10);
  const [refundingId, setRefundingId] = useState(null);

  const clientIdOptions = useMemo(() => {
    return (Array.isArray(clients) ? clients : [])
      .map((c) => c?.clientId)
      .filter((id) => id != null && String(id).trim() !== '');
  }, [clients]);

  useEffect(() => { setPage(1); }, [itemsPerPage, loadedClientId]);

  const handleLoadPayments = async () => {
    const raw = clientIdSearch.trim();
    if (!raw) { setError('Enter a client ID'); return; }
    setLoading(true);
    setError('');
    try {
      const data = await getPaymentHistory(raw);
      setPayments(Array.isArray(data) ? data : []);
      setLoadedClientId(raw);
    } catch (err) {
      setPayments([]);
      setLoadedClientId(null);
      setError(err?.message || 'Failed to load payments');
    } finally { setLoading(false); }
  };

  const handleResetFilters = () => {
    setClientIdSearch(''); setLoadedClientId(null); setPayments([]); setError(''); setPage(1);
  };

  const handleRefund = async (id) => {
    if (id == null) return;
    setRefundingId(id);
    setError('');
    try {
      await requestRefund(id);
      if (loadedClientId != null) {
        const data = await getPaymentHistory(loadedClientId);
        setPayments(Array.isArray(data) ? data : []);
      }
    } catch (err) {
      setError(err?.message || 'Refund failed');
    } finally { setRefundingId(null); }
  };

  const totalPages = Math.ceil(payments.length / itemsPerPage) || 0;
  const paginatedPayments = payments.slice((page - 1) * itemsPerPage, page * itemsPerPage);

  return (
    <div className="space-y-3">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-4">
        <h2 className="text-lg font-semibold flex items-center gap-2">
          <CreditCard className="w-5 h-5 text-emerald-400" />
          Payments
          {loadedClientId != null && <span className="text-sm font-normal text-zinc-400">({payments.length} for client {loadedClientId})</span>}
        </h2>
        <button type="button" onClick={() => setShowFilters(!showFilters)}
          className={`btn-secondary flex items-center justify-center gap-2 ${showFilters ? 'bg-emerald-500/20 border-emerald-500/30' : ''}`}>
          <Filter className="w-4 h-4" />
          {showFilters ? 'Hide' : 'Show'} Filters
        </button>
      </div>

      {showFilters && (
        <div className="glass rounded-2xl p-4 mb-3 animate-fade-in border border-white/5">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-zinc-400 mb-2">Search by client ID</label>
              <div className="relative flex flex-col sm:flex-row gap-2">
                <div className="relative flex-1">
                  <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
                  <input type="text" list="admin-payments-client-ids" value={clientIdSearch}
                    onChange={(e) => setClientIdSearch(e.target.value)}
                    onKeyDown={(e) => { if (e.key === 'Enter') handleLoadPayments(); }}
                    className="input-field input-with-icon w-full" placeholder="Numeric client ID" />
                  <datalist id="admin-payments-client-ids">
                    {clientIdOptions.map((id) => <option key={String(id)} value={String(id)} />)}
                  </datalist>
                </div>
                <button type="button" onClick={handleLoadPayments} disabled={loading}
                  className="btn-secondary flex items-center justify-center gap-2 shrink-0 px-4">
                  {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Search className="w-4 h-4" />}
                  Load history
                </button>
              </div>
            </div>
          </div>
          {error && <p className="text-sm text-red-400 mt-3">{error}</p>}
          <div className="flex items-center justify-between mt-3 pt-3 border-t border-zinc-700">
            <p className="text-xs text-zinc-400">{loadedClientId != null ? `${payments.length} payment${payments.length !== 1 ? 's' : ''} loaded` : 'No client selected'}</p>
            <button type="button" onClick={handleResetFilters} className="btn-secondary text-xs py-1.5 px-3">Reset filters</button>
          </div>
        </div>
      )}

      {loadedClientId == null ? (
        <div className="glass rounded-2xl p-12 text-center border border-white/5">
          <p className="text-zinc-400">Enter a client ID and load to view payment history.</p>
        </div>
      ) : payments.length === 0 && !loading ? (
        <div className="glass rounded-2xl p-12 text-center border border-white/5">
          <p className="text-zinc-400">No payments found for this client.</p>
        </div>
      ) : (
        <>
          <div className="glass rounded-2xl overflow-hidden border border-white/5">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-zinc-800/50">
                  <tr>
                    {['Payment ID', 'Client ID', 'Amount', 'Currency', 'Status', 'Description', 'Created', 'Actions'].map(h => (
                      <th key={h} className={`px-4 py-3 text-xs font-semibold text-zinc-300 uppercase tracking-wide ${h === 'Actions' ? 'text-right w-px' : 'text-left'}`}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-800">
                  {paginatedPayments.map((p) => {
                    const pid = paymentIdOf(p);
                    const canRefund = String(p?.status ?? '').toUpperCase() === 'SUCCEEDED';
                    return (
                      <tr key={String(pid)} className="hover:bg-zinc-800/30 transition-colors">
                        <td className="px-4 py-3 text-sm font-mono text-zinc-300">{pid}</td>
                        <td className="px-4 py-3 text-sm font-mono text-zinc-400">{p?.clientId ?? '—'}</td>
                        <td className="px-4 py-3 text-sm font-medium text-emerald-400">{formatAmount(p)}</td>
                        <td className="px-4 py-3 text-sm text-zinc-300">{p?.currencyCode ?? '—'}</td>
                        <td className="px-4 py-3 text-sm"><span className={statusBadgeClass(p?.status)}>{p?.status ?? '—'}</span></td>
                        <td className="px-4 py-3 text-sm text-zinc-400 max-w-[220px] truncate" title={p?.description || ''}>{p?.description?.trim() || '—'}</td>
                        <td className="px-4 py-3 text-sm text-zinc-400 whitespace-nowrap">{formatCreated(p)}</td>
                        <td className="px-4 py-3 text-sm text-right">
                          {canRefund ? (
                            <button type="button" onClick={() => handleRefund(p.id)} disabled={refundingId === p.id}
                              className="btn-secondary text-xs py-1.5 px-2 inline-flex items-center gap-1">
                              {refundingId === p.id ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <RotateCcw className="w-3.5 h-3.5" />}
                              Refund
                            </button>
                          ) : <span className="text-xs text-zinc-500">—</span>}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
          {totalPages > 1 && (
            <PaginationControls currentPage={page} totalPages={totalPages} itemsPerPage={itemsPerPage}
              onPageChange={setPage} onItemsPerPageChange={setItemsPerPage} />
          )}
        </>
      )}
    </div>
  );
}
