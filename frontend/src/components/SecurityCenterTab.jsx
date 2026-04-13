import { useMemo } from 'react';
import { AlertTriangle, CheckCircle2, RefreshCw, ShieldAlert, ShieldOff } from 'lucide-react';

function statusLabel(alert) {
  if (alert.userResolution && alert.userResolution !== 'PENDING') {
    return alert.userResolution === 'LEGITIMATE' ? 'Resolved as legitimate' : 'Reported as fraud';
  }
  return alert.status || 'PENDING';
}

function statusClass(alert) {
  if (alert.userResolution && alert.userResolution !== 'PENDING') {
    return alert.userResolution === 'LEGITIMATE'
      ? 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/25'
      : 'bg-red-500/15 text-red-300 border border-red-500/25';
  }
  if (alert.status === 'BLOCK') return 'bg-red-500/15 text-red-300 border border-red-500/25';
  if (alert.status === 'STEP_UP_REQUIRED') return 'bg-amber-500/15 text-amber-300 border border-amber-500/25';
  return 'bg-sky-500/15 text-sky-300 border border-sky-500/25';
}

export default function SecurityCenterTab({ alerts, loading, error, onRefresh, onResolve }) {
  const pendingAlerts = useMemo(
    () => alerts.filter((alert) => !alert.userResolution || alert.userResolution === 'PENDING'),
    [alerts]
  );

  return (
    <div className="space-y-6">
      <div className="glass rounded-2xl p-6 border border-amber-500/10">
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
          <div>
            <h2 className="text-2xl font-bold flex items-center gap-2">
              <ShieldAlert className="w-6 h-6 text-amber-400" />
              Security Center
            </h2>
            <p className="text-zinc-500 text-sm mt-1">
              Review suspicious activity, confirm legitimate actions, or keep accounts protected.
            </p>
          </div>
          <button onClick={onRefresh} className="btn-secondary flex items-center gap-2 self-start lg:self-auto">
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            Refresh alerts
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mt-5">
          <div className="rounded-xl border border-white/5 bg-zinc-900/60 p-4">
            <p className="text-xs uppercase tracking-wide text-zinc-500">Pending alerts</p>
            <p className="text-2xl font-bold mt-2 text-amber-300">{pendingAlerts.length}</p>
          </div>
          <div className="rounded-xl border border-white/5 bg-zinc-900/60 p-4">
            <p className="text-xs uppercase tracking-wide text-zinc-500">Resolved legitimate</p>
            <p className="text-2xl font-bold mt-2 text-emerald-300">
              {alerts.filter((alert) => alert.userResolution === 'LEGITIMATE').length}
            </p>
          </div>
          <div className="rounded-xl border border-white/5 bg-zinc-900/60 p-4">
            <p className="text-xs uppercase tracking-wide text-zinc-500">Reported as fraud</p>
            <p className="text-2xl font-bold mt-2 text-red-300">
              {alerts.filter((alert) => alert.userResolution === 'FRAUD_REPORTED').length}
            </p>
          </div>
        </div>
      </div>

      {error ? (
        <div className="glass rounded-2xl p-6 border border-red-500/20 text-center text-red-300">
          {error}
        </div>
      ) : null}

      {loading ? (
        <div className="glass rounded-2xl p-6">
          <div className="space-y-3">
            {Array.from({ length: 3 }).map((_, index) => (
              <div key={index} className="skeleton h-24 w-full" />
            ))}
          </div>
        </div>
      ) : alerts.length === 0 ? (
        <div className="glass rounded-2xl p-12 text-center">
          <CheckCircle2 className="w-12 h-12 text-emerald-400 mx-auto mb-3" />
          <p className="text-zinc-300 font-medium">No security alerts right now.</p>
          <p className="text-zinc-500 text-sm mt-1">Any suspicious activity will show up here for confirmation.</p>
        </div>
      ) : (
        <div className="grid gap-4">
          {alerts.map((alert) => {
            const isPending = !alert.userResolution || alert.userResolution === 'PENDING';
            return (
              <div key={alert.id} className="glass rounded-2xl p-5 border border-white/5">
                <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4">
                  <div className="space-y-3">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${statusClass(alert)}`}>
                        {alert.status === 'STEP_UP_REQUIRED' ? <ShieldOff className="w-3.5 h-3.5" /> : <AlertTriangle className="w-3.5 h-3.5" />}
                        {statusLabel(alert)}
                      </span>
                      <span className="px-2.5 py-1 rounded-full text-xs font-medium bg-zinc-800 text-zinc-300 border border-white/5">
                        Risk {Number(alert.riskScore || 0).toFixed(1)}
                      </span>
                      <span className="px-2.5 py-1 rounded-full text-xs font-medium bg-zinc-800 text-zinc-300 border border-white/5">
                        {alert.decidedByTier || '—'}
                      </span>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 text-sm">
                      <div>
                        <p className="text-xs text-zinc-500 uppercase tracking-wide">Decision ID</p>
                        <p className="font-mono text-zinc-200">{alert.id}</p>
                      </div>
                      <div>
                        <p className="text-xs text-zinc-500 uppercase tracking-wide">Account ID</p>
                        <p className="font-mono text-zinc-200">{alert.accountId ?? '—'}</p>
                      </div>
                      <div>
                        <p className="text-xs text-zinc-500 uppercase tracking-wide">Created</p>
                        <p className="text-zinc-200">
                          {alert.createdAt ? new Date(alert.createdAt).toLocaleString() : '—'}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-zinc-500 uppercase tracking-wide">Amount</p>
                        <p className="text-zinc-200 font-medium">
                          {alert.amount != null ? `${Number(alert.amount).toFixed(2)} ${alert.currencyCode || 'EUR'}` : '—'}
                        </p>
                      </div>
                    </div>

                    {alert.explanation ? (
                      <div className="rounded-xl bg-zinc-900/60 border border-white/5 p-3">
                        <p className="text-xs text-zinc-500 uppercase tracking-wide mb-1">Alert summary</p>
                        <p className="text-sm text-zinc-300">{alert.explanation}</p>
                      </div>
                    ) : null}
                  </div>

                  {isPending ? (
                    <div className="flex flex-col gap-2 min-w-[220px] lg:items-stretch">
                      <button
                        type="button"
                        onClick={() => onResolve(alert, 'LEGITIMATE')}
                        className="btn-primary flex items-center justify-center gap-2"
                      >
                        <CheckCircle2 className="w-4 h-4" />
                        Yes, it was me
                      </button>
                      <button
                        type="button"
                        onClick={() => onResolve(alert, 'FRAUD_REPORTED')}
                        className="btn-secondary flex items-center justify-center gap-2 border-red-500/30 text-red-300"
                      >
                        <AlertTriangle className="w-4 h-4" />
                        No, report fraud
                      </button>
                    </div>
                  ) : (
                    <div className="min-w-[220px] rounded-xl border border-white/5 bg-zinc-900/60 p-4 text-sm text-zinc-300">
                      <p className="font-medium text-zinc-200 mb-1">Already resolved</p>
                      <p>{alert.userResolutionNotes || 'No additional note provided.'}</p>
                      {alert.userResolvedAt ? (
                        <p className="text-xs text-zinc-500 mt-3">
                          {new Date(alert.userResolvedAt).toLocaleString()}
                        </p>
                      ) : null}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}