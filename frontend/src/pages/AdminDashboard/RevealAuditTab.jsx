import { useEffect, useMemo, useState } from 'react';
import { getSensitiveRevealAuditEvents } from '@/services/adminAuditService';
import PaginationControls from './PaginationControls';

const REASON_OPTIONS = [
  { value: 'all', label: 'All reason codes' },
  { value: 'DISPUTE_INVESTIGATION', label: 'Customer dispute investigation' },
  { value: 'FRAUD_REVIEW', label: 'Fraud / suspicious activity review' },
  { value: 'REGULATORY_AUDIT', label: 'Regulatory or compliance audit' },
  { value: 'SUPPORT_REQUEST', label: 'Customer support request handling' },
  { value: 'RECONCILIATION', label: 'Ledger / transaction reconciliation' },
  { value: 'OTHER', label: 'Other' },
];

export default function RevealAuditTab() {
  const [reasonCode, setReasonCode] = useState('all');
  const [itemsPerPage, setItemsPerPage] = useState(10);
  const [currentPage, setCurrentPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [auditPage, setAuditPage] = useState({ content: [], totalPages: 1, totalElements: 0, number: 0, size: 10 });

  const fetchAuditEvents = async () => {
    setLoading(true);
    setError('');
    try {
      const result = await getSensitiveRevealAuditEvents({
        page: currentPage - 1,
        size: itemsPerPage,
        reasonCode: reasonCode === 'all' ? '' : reasonCode,
      });
      setAuditPage(result || { content: [], totalPages: 1, totalElements: 0, number: 0, size: itemsPerPage });
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'Failed to load audit report');
      setAuditPage({ content: [], totalPages: 1, totalElements: 0, number: 0, size: itemsPerPage });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAuditEvents();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentPage, itemsPerPage, reasonCode]);

  useEffect(() => {
    setCurrentPage(1);
  }, [reasonCode, itemsPerPage]);

  const rows = useMemo(() => (Array.isArray(auditPage?.content) ? auditPage.content : []), [auditPage]);
  const totalPages = Math.max(1, Number(auditPage?.totalPages || 1));

  return (
    <div className="space-y-4">
      <div className="glass rounded-2xl p-5">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-3">
          <div>
            <h2 className="text-lg font-semibold">Sensitive Data Reveal Audit</h2>
            <p className="text-sm text-zinc-500">Filter reveal events by reason code for compliance and review.</p>
          </div>
          <div className="w-full sm:w-72">
            <label className="text-xs text-zinc-500">Reason code</label>
            <select
              value={reasonCode}
              onChange={(e) => setReasonCode(e.target.value)}
              className="input-field mt-1"
            >
              {REASON_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </div>
        </div>

        {error ? (
          <div className="rounded-xl border border-red-500/20 bg-red-500/10 text-red-300 text-sm p-3">{error}</div>
        ) : null}

        <div className="overflow-x-auto rounded-xl border border-white/10">
          <table className="w-full min-w-[980px]">
            <thead>
              <tr className="border-b border-white/10 text-zinc-400 text-xs uppercase tracking-wide">
                <th className="text-left px-4 py-3">Time</th>
                <th className="text-left px-4 py-3">Admin</th>
                <th className="text-left px-4 py-3">Scope</th>
                <th className="text-left px-4 py-3">Target</th>
                <th className="text-left px-4 py-3">Reason Code</th>
                <th className="text-left px-4 py-3">Details</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                Array.from({ length: 6 }).map((_, idx) => (
                  <tr key={idx} className="border-b border-white/5">
                    <td colSpan={6} className="px-4 py-3"><div className="skeleton h-4 w-full" /></td>
                  </tr>
                ))
              ) : rows.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-10 text-center text-sm text-zinc-500">No audit events found for this filter.</td>
                </tr>
              ) : (
                rows.map((row) => (
                  <tr key={row.id} className="border-b border-white/5 hover:bg-zinc-800/40">
                    <td className="px-4 py-3 text-sm text-zinc-300">{row.createdAt ? new Date(row.createdAt).toLocaleString() : '—'}</td>
                    <td className="px-4 py-3 text-sm">
                      <div className="font-medium text-zinc-200">{row.actorUsername || 'unknown'}</div>
                      <div className="text-xs text-zinc-500">{row.actorRole || 'UNKNOWN'} {row.actorClientId != null ? `• #${row.actorClientId}` : ''}</div>
                    </td>
                    <td className="px-4 py-3 text-sm text-zinc-300">{row.scope || '—'}</td>
                    <td className="px-4 py-3 text-sm text-zinc-300">
                      <div>{row.targetType || '—'}</div>
                      <div className="text-xs text-zinc-500">{row.targetId || '—'}</div>
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <span className="px-2 py-1 rounded text-xs bg-zinc-800 border border-white/10 text-zinc-200">{row.reasonCode || '—'}</span>
                    </td>
                    <td className="px-4 py-3 text-sm text-zinc-300 max-w-[360px]">{row.reasonDetails || '—'}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <PaginationControls
          currentPage={currentPage}
          totalPages={totalPages}
          itemsPerPage={itemsPerPage}
          onPageChange={setCurrentPage}
          onItemsPerPageChange={setItemsPerPage}
        />
      </div>
    </div>
  );
}
