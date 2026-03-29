import { useCallback, useEffect, useState } from 'react';
import {
  ShieldAlert, Loader2, RefreshCw, AlertTriangle, CheckCircle,
  ChevronLeft, ChevronRight, X, Save, Clock, User, CreditCard,
} from 'lucide-react';
import { getFraudAlerts, reviewFraudDecision } from '../../../services/fraudService';

const STATUS_CONFIG = {
  BLOCK:         { label: 'BLOCK',          cls: 'badge-block',  icon: ShieldAlert },
  FLAG:          { label: 'FLAG',           cls: 'badge-flag',   icon: AlertTriangle },
  MANUAL_REVIEW: { label: 'MANUAL REVIEW',  cls: 'badge-review', icon: Clock },
  ALLOW:         { label: 'ALLOW',          cls: 'badge-allow',  icon: CheckCircle },
};

const TIER_CONFIG = {
  TIER1_RULES:      { label: 'Tier 1',      cls: 'badge-tier1' },
  TIER2_BEHAVIORAL: { label: 'Tier 2',      cls: 'badge-tier2' },
  TIER3_LLM:        { label: 'Tier 3 LLM',  cls: 'badge-flag' },
};

function StatusBadge({ status }) {
  const cfg = STATUS_CONFIG[status] || { label: status, cls: 'badge-inactive', icon: null };
  const Icon = cfg.icon;
  return (
    <span className={`badge ${cfg.cls} gap-1`}>
      {Icon && <Icon className="w-3 h-3" />}
      {cfg.label}
    </span>
  );
}

function TierBadge({ tier }) {
  const cfg = TIER_CONFIG[tier] || { label: tier, cls: 'badge-inactive' };
  return <span className={`badge ${cfg.cls}`}>{cfg.label}</span>;
}

function RiskBar({ score }) {
  const pct = Math.min(100, Math.max(0, Number(score) || 0));
  const color = pct >= 70 ? '#ef4444' : pct >= 40 ? '#f97316' : '#22c55e';
  return (
    <div className="flex items-center gap-2 min-w-[80px]">
      <div className="risk-bar-track flex-1">
        <div className="risk-bar-fill" style={{ width: `${pct}%`, backgroundColor: color }} />
      </div>
      <span className="text-xs text-zinc-400 w-7 text-right">{pct.toFixed(0)}</span>
    </div>
  );
}

function ReviewModal({ decision, onClose, onReviewed }) {
  const [status, setStatus] = useState(decision.status);
  const [notes, setNotes] = useState(decision.adminNotes || '');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await reviewFraudDecision(decision.id, status, notes);
      onReviewed();
      onClose();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'Review failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-fade-in" onClick={onClose}>
      <div className="glass rounded-2xl p-6 w-full max-w-lg animate-slide-up" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-5">
          <h3 className="text-lg font-bold flex items-center gap-2">
            <ShieldAlert className="w-5 h-5 text-amber-400" />
            Review Decision #{decision.id}
          </h3>
          <button onClick={onClose} className="text-zinc-500 hover:text-white transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Context */}
        <div className="grid grid-cols-3 gap-3 mb-5">
          {[
            { icon: User, label: 'Client ID', value: decision.clientId },
            { icon: CreditCard, label: 'Account ID', value: decision.accountId },
            { icon: AlertTriangle, label: 'Risk Score', value: `${(decision.riskScore || 0).toFixed(1)}` },
          ].map((item) => {
            const Icon = item.icon;
            return (
            <div key={item.label} className="bg-zinc-900/60 rounded-xl p-3 border border-white/5">
              <div className="flex items-center gap-1.5 text-zinc-500 mb-1">
                <Icon className="w-3 h-3" />
                <span className="text-xs">{item.label}</span>
              </div>
              <p className="font-semibold text-sm">{item.value ?? '—'}</p>
            </div>
            );
          })}
        </div>

        {decision.ruleHits && (
          <div className="mb-4 p-3 bg-zinc-900/60 rounded-xl border border-white/5">
            <p className="text-xs text-zinc-500 mb-1">Rule Hits / Summary</p>
            <p className="text-sm text-zinc-300 break-words">{decision.ruleHits}</p>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-zinc-400 mb-2">Change Status</label>
            <select value={status} onChange={(e) => setStatus(e.target.value)} className="input-field">
              <option value="ALLOW">ALLOW — Transaction is legitimate</option>
              <option value="FLAG">FLAG — Suspicious, monitor closely</option>
              <option value="BLOCK">BLOCK — Fraudulent, block permanently</option>
              <option value="MANUAL_REVIEW">MANUAL REVIEW — Needs more investigation</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-zinc-400 mb-2">Admin Notes</label>
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="input-field resize-none"
              rows={3}
              placeholder="Add investigation notes..."
            />
          </div>

          {error && (
            <p className="text-red-400 text-sm p-3 bg-red-500/10 rounded-xl border border-red-500/20">{error}</p>
          )}

          <div className="flex gap-3">
            <button type="button" onClick={onClose} className="btn-secondary flex-1 flex items-center justify-center gap-2 py-2.5">
              <X className="w-4 h-4" /> Cancel
            </button>
            <button type="submit" disabled={loading} className="btn-primary flex-1 flex items-center justify-center gap-2 py-2.5">
              {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
              Save Review
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function EmptyState({ message }) {
  return (
    <div className="py-16 text-center">
      <ShieldAlert className="w-12 h-12 text-zinc-700 mx-auto mb-3" />
      <p className="text-zinc-500">{message}</p>
    </div>
  );
}

export default function FraudAlertsTab() {
  const [data, setData] = useState(null);         // paginated response
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [serviceDown, setServiceDown] = useState(false);
  const [reviewTarget, setReviewTarget] = useState(null);

  const PAGE_SIZE = 15;

  const fetchAlerts = useCallback(async () => {
    setLoading(true);
    setError('');
    setServiceDown(false);
    try {
      const result = await getFraudAlerts(page, PAGE_SIZE);
      setData(result);
    } catch (err) {
      const status = err?.response?.status;
      if (!status || status === 503 || status === 0) {
        setServiceDown(true);
      } else {
        setError(err?.response?.data?.message || err?.message || 'Failed to load fraud alerts.');
      }
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => { fetchAlerts(); }, [fetchAlerts]);

  const alerts = data?.content || [];
  const totalPages = data?.totalPages || 0;
  const totalElements = data?.totalElements || 0;

  return (
    <>
      <div className="space-y-4 animate-fade-in">
        {/* Header */}
        <div className="glass rounded-2xl p-5 border border-red-500/10">
          <div className="flex items-center justify-between gap-4 flex-wrap">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-red-500/15 rounded-xl flex items-center justify-center">
                <ShieldAlert className="w-5 h-5 text-red-400" />
              </div>
              <div>
                <h2 className="text-xl font-bold">Fraud Alerts</h2>
                <p className="text-sm text-zinc-500">Real-time decisions from the AI Fraud Engine</p>
              </div>
            </div>
            <button
              onClick={fetchAlerts}
              disabled={loading}
              className="btn-secondary flex items-center gap-2 text-sm px-3 py-2"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
              Refresh
            </button>
          </div>

          {!serviceDown && !loading && (
            <div className="flex gap-4 mt-4 text-sm text-zinc-400">
              <span>{totalElements} total alerts</span>
              {alerts.filter(a => a.status === 'BLOCK').length > 0 && (
                <span className="text-red-400">{alerts.filter(a => a.status === 'BLOCK').length} BLOCKED this page</span>
              )}
              {alerts.filter(a => a.status === 'FLAG').length > 0 && (
                <span className="text-orange-400">{alerts.filter(a => a.status === 'FLAG').length} FLAGGED this page</span>
              )}
            </div>
          )}
        </div>

        {/* Content */}
        {serviceDown ? (
          <div className="glass rounded-2xl p-8 text-center border border-amber-500/10">
            <AlertTriangle className="w-12 h-12 text-amber-500/60 mx-auto mb-3" />
            <p className="text-amber-400 font-semibold mb-2">Fraud Service Unavailable</p>
            <p className="text-sm text-zinc-500 mb-4">The fraud detection engine is currently offline. Alerts cannot be loaded.</p>
            <button onClick={fetchAlerts} className="btn-secondary text-sm px-4 py-2 flex items-center gap-2 mx-auto">
              <RefreshCw className="w-4 h-4" /> Retry
            </button>
          </div>
        ) : loading ? (
          <div className="glass rounded-2xl p-4">
            <div className="space-y-3">
              {Array.from({ length: 6 }).map((_, i) => (
                <div key={i} className="skeleton h-12 w-full" style={{ animationDelay: `${i * 0.08}s` }} />
              ))}
            </div>
          </div>
        ) : error ? (
          <div className="glass rounded-2xl p-6 border border-red-500/20 text-center">
            <p className="text-red-400">{error}</p>
          </div>
        ) : alerts.length === 0 ? (
          <div className="glass rounded-2xl">
            <EmptyState message="No fraud alerts — the system is clean." />
          </div>
        ) : (
          <div className="glass rounded-2xl overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-white/10">
                    {['ID', 'Client', 'Account', 'Status', 'Risk Score', 'Tier', 'Rule Hits', 'Created At', 'Actions'].map((h) => (
                      <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-zinc-500 uppercase tracking-wider whitespace-nowrap">
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {alerts.map((alert) => (
                    <tr key={alert.id} className="border-b border-white/5 table-row-hover">
                      <td className="px-4 py-3 font-mono text-zinc-400 text-xs">{alert.id}</td>
                      <td className="px-4 py-3 text-zinc-300">{alert.clientId ?? '—'}</td>
                      <td className="px-4 py-3 text-zinc-400 text-xs font-mono">{alert.accountId ?? '—'}</td>
                      <td className="px-4 py-3">
                        <StatusBadge status={alert.status} />
                      </td>
                      <td className="px-4 py-3">
                        <RiskBar score={alert.riskScore} />
                      </td>
                      <td className="px-4 py-3">
                        <TierBadge tier={alert.decidedByTier} />
                      </td>
                      <td className="px-4 py-3 max-w-[200px]">
                        <p className="text-xs text-zinc-500 truncate" title={alert.ruleHits}>
                          {alert.ruleHits || '—'}
                        </p>
                      </td>
                      <td className="px-4 py-3 text-xs text-zinc-500 whitespace-nowrap">
                        {alert.createdAt ? new Date(alert.createdAt).toLocaleString('en-GB') : '—'}
                      </td>
                      <td className="px-4 py-3">
                        <button
                          onClick={() => setReviewTarget(alert)}
                          className="text-xs text-emerald-400 hover:text-emerald-300 transition-colors font-medium whitespace-nowrap"
                        >
                          Review →
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between px-4 py-3 border-t border-white/5">
                <p className="text-xs text-zinc-500">Page {page + 1} of {totalPages}</p>
                <div className="flex gap-2">
                  <button
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="btn-secondary flex items-center gap-1 text-xs px-3 py-1.5"
                  >
                    <ChevronLeft className="w-3.5 h-3.5" /> Prev
                  </button>
                  <button
                    onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                    disabled={page >= totalPages - 1}
                    className="btn-secondary flex items-center gap-1 text-xs px-3 py-1.5"
                  >
                    Next <ChevronRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {reviewTarget && (
        <ReviewModal
          decision={reviewTarget}
          onClose={() => setReviewTarget(null)}
          onReviewed={fetchAlerts}
        />
      )}
    </>
  );
}
