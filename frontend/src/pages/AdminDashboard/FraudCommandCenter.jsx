import { useMemo, useState } from 'react';
import { AlertTriangle } from 'lucide-react';
import {
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Scatter,
  ScatterChart,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import {
  prepareClientRiskDistributionData,
  prepareAlertScatterData,
  prepareAlertRiskOverTime,
  prepareAlertStatusDistributionData,
} from '@/lib/analyticsTransforms';

const RISK_COLORS = {
  LOW: '#22c55e',
  MEDIUM: '#eab308',
  HIGH: '#f97316',
  CRITICAL: '#ef4444',
};

function tooltipStyle() {
  return {
    backgroundColor: '#09090b',
    border: '1px solid #27272a',
    borderRadius: 12,
    color: '#e4e4e7',
  };
}

function formatDate(value) {
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleString('en-GB');
}

function formatAmountXAxis(value) {
  if (value >= 1000) {
    return `${(value / 1000).toFixed(value % 1000 === 0 ? 0 : 1)}K`;
  }
  return value;
}

const RANGE_OPTIONS = [7, 30, 90];

const STATUS_COLORS = {
  BLOCK: '#ef4444',
  FLAG: '#f97316',
  STEP_UP_REQUIRED: '#eab308',
  MANUAL_REVIEW: '#60a5fa',
  ALLOW: '#22c55e',
};

function ScatterTooltip({ active, payload }) {
  if (!active || !Array.isArray(payload) || payload.length === 0) return null;
  const point = payload[0]?.payload;
  if (!point) return null;

  const title = `${point.status || 'Alert'} • ${point.date ? formatDate(point.date) : '—'}`;
  const risk = Number.isFinite(Number(point.riskScore)) ? Number(point.riskScore).toFixed(1) : '—';
  const amount = Number.isFinite(Number(point.amount)) ? `${Number(point.amount).toFixed(2)} ${point.currencyCode || 'EUR'}` : '—';

  return (
    <div style={{ ...tooltipStyle(), padding: 12, minWidth: 220 }}>
      <div style={{ fontWeight: 700, marginBottom: 6 }}>{title}</div>
      <div style={{ fontSize: 12, opacity: 0.9, lineHeight: 1.5 }}>
        <div><span style={{ opacity: 0.7 }}>Amount:</span> {amount}</div>
        <div><span style={{ opacity: 0.7 }}>Risk score:</span> {risk}%</div>
        {point.accountId != null && <div><span style={{ opacity: 0.7 }}>Account:</span> {point.accountId}</div>}
        {point.id != null && <div><span style={{ opacity: 0.7 }}>Alert ID:</span> {String(point.id)}</div>}
      </div>
    </div>
  );
}

function PieTooltip({ active, payload, total }) {
  if (!active || !Array.isArray(payload) || payload.length === 0) return null;
  const item = payload[0]?.payload;
  if (!item) return null;
  const level = item.level || '—';
  const count = Number.isFinite(Number(item.value)) ? Number(item.value) : 0;
  const safeTotal = Number.isFinite(Number(total)) ? Number(total) : 0;
  const pct = safeTotal > 0 ? ((count / safeTotal) * 100).toFixed(1) : '0.0';

  return (
    <div style={{ ...tooltipStyle(), padding: 12 }}>
      <div style={{ fontWeight: 700, marginBottom: 6 }}>{level}</div>
      <div style={{ fontSize: 12, opacity: 0.9, lineHeight: 1.5 }}>
        <div><span style={{ opacity: 0.7 }}>Alerts:</span> {count}</div>
        <div><span style={{ opacity: 0.7 }}>Share:</span> {pct}%</div>
      </div>
    </div>
  );
}

export default function FraudCommandCenter({ clients = [], fraudAlerts = [] }) {
  const [rangeDays, setRangeDays] = useState(30);

  // Filter alerts to the selected time range
  const rangedAlerts = useMemo(() => {
    const safeDays = Number.isInteger(rangeDays) && rangeDays > 0 ? rangeDays : 30;
    const start = new Date();
    start.setHours(0, 0, 0, 0);
    start.setDate(start.getDate() - (safeDays - 1));
    return (Array.isArray(fraudAlerts) ? fraudAlerts : []).filter((a) => {
      const d = a?.createdAt ? new Date(a.createdAt) : null;
      return d && d >= start;
    });
  }, [fraudAlerts, rangeDays]);

  // Scatter chart: all alert points
  const scatterData = useMemo(() => prepareAlertScatterData(rangedAlerts), [rangedAlerts]);
  const highRiskPoints = useMemo(() => scatterData.filter((x) => x.highRisk), [scatterData]);
  const normalPoints = useMemo(() => scatterData.filter((x) => !x.highRisk), [scatterData]);

  // Line chart: alerts per day
  const riskTimeData = useMemo(
    () => prepareAlertRiskOverTime(rangedAlerts, rangeDays),
    [rangedAlerts, rangeDays]
  );

  // Pie chart: alert status distribution
  const alertStatusDistribution = useMemo(() => prepareAlertStatusDistributionData(rangedAlerts), [rangedAlerts]);
  const alertStatusDistributionTotal = useMemo(
    () => (Array.isArray(alertStatusDistribution) ? alertStatusDistribution.reduce((acc, x) => acc + (Number(x?.value) || 0), 0) : 0),
    [alertStatusDistribution]
  );

  // Summary stats for the header
  const totalAlerts = rangedAlerts.length;
  const blockedCount = rangedAlerts.filter((a) => a.status === 'BLOCK').length;
  const flaggedCount = rangedAlerts.filter((a) => a.status === 'FLAG').length;
  const fraudReportedCount = rangedAlerts.filter((a) => a.userResolution === 'FRAUD_REPORTED').length;

  return (
    <div className="space-y-4">
      <div className="glass rounded-2xl p-5 border border-red-500/10">
        <div className="flex items-center gap-3">
          <AlertTriangle className="w-5 h-5 text-red-400" />
          <div>
            <h2 className="text-2xl font-bold">AI Fraud Command Center</h2>
            <p className="text-sm text-zinc-500">Monitor anomalies, attack waves, and client risk concentration.</p>
          </div>
        </div>
        <div className="flex items-center gap-2 mt-3">
          {RANGE_OPTIONS.map((d) => (
            <button
              key={d}
              type="button"
              onClick={() => setRangeDays(d)}
              className={`px-3 py-1.5 rounded-lg text-xs border transition-colors ${
                rangeDays === d
                  ? 'bg-red-500/20 border-red-500/40 text-red-300'
                  : 'bg-zinc-900/70 border-zinc-700 text-zinc-400 hover:text-zinc-200'
              }`}
            >
              {d}d
            </button>
          ))}
        </div>

        {/* Summary stats */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-4">
          {[
            { label: 'Total Alerts', value: totalAlerts, color: 'text-zinc-200' },
            { label: 'Blocked', value: blockedCount, color: 'text-red-400' },
            { label: 'Flagged', value: flaggedCount, color: 'text-orange-400' },
            { label: 'Fraud Reported', value: fraudReportedCount, color: 'text-amber-400' },
          ].map((stat) => (
            <div key={stat.label} className="bg-zinc-900/60 rounded-xl p-3 border border-white/5">
              <p className="text-xs text-zinc-500 uppercase tracking-wide">{stat.label}</p>
              <p className={`text-2xl font-bold mt-1 ${stat.color}`}>{stat.value}</p>
            </div>
          ))}
        </div>
      </div>

      <section className="glass rounded-2xl p-4">
        <h2 className="text-sm font-semibold text-zinc-300 mb-2">
          Anomaly Detection — Amount vs Risk Score (last {rangeDays} days)
        </h2>
        {scatterData.length === 0 ? (
          <div className="h-[380px] flex items-center justify-center text-zinc-600 text-sm">
            No fraud alerts in the last {rangeDays} days
          </div>
        ) : (
          <div className="h-[380px]">
            <ResponsiveContainer width="100%" height="100%">
              <ScatterChart margin={{ top: 12, right: 24, left: 8, bottom: 8 }}>
                <CartesianGrid stroke="#27272a" strokeDasharray="3 3" />
                <XAxis type="number" dataKey="amount" name="Amount" domain={[0, (dataMax) => Math.max(dataMax || 0, 4000)]} tickFormatter={formatAmountXAxis} tick={{ fill: '#a1a1aa', fontSize: 12 }} axisLine={{ stroke: '#3f3f46' }} label={{ value: 'Amount (EUR)', position: 'insideBottom', offset: -4, fill: '#71717a', fontSize: 11 }} />
                <YAxis type="number" dataKey="riskScore" name="Risk score" domain={[0, 100]} tick={{ fill: '#a1a1aa', fontSize: 12 }} axisLine={{ stroke: '#3f3f46' }} label={{ value: 'Risk %', angle: -90, position: 'insideLeft', fill: '#71717a', fontSize: 11 }} />
                <Tooltip
                  cursor={{ strokeDasharray: '3 3' }}
                  content={<ScatterTooltip />}
                />
                <Legend verticalAlign="top" align="left" wrapperStyle={{ paddingBottom: '15px' }} />
                <Scatter name="Normal" data={normalPoints} fill="#60a5fa" />
                <Scatter name="Flagged / High Risk" data={highRiskPoints} fill="#f97316" />
              </ScatterChart>
            </ResponsiveContainer>
          </div>
        )}
      </section>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <section className="glass rounded-2xl p-4">
          <h2 className="text-sm font-semibold text-zinc-300 mb-3">
            High-Risk Alerts Per Day (last {rangeDays} days)
          </h2>
          <div className="h-[300px]">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={riskTimeData}>
                <CartesianGrid stroke="#27272a" strokeDasharray="3 3" />
                <XAxis dataKey="label" minTickGap={30} tick={{ fill: '#a1a1aa', fontSize: 12 }} axisLine={{ stroke: '#3f3f46' }} />
                <YAxis allowDecimals={false} domain={[0, (dataMax) => Math.max(dataMax, 5)]} tick={{ fill: '#a1a1aa', fontSize: 12 }} axisLine={{ stroke: '#3f3f46' }} label={{ value: 'Alerts', angle: -90, position: 'insideLeft', fill: '#71717a', fontSize: 11 }} />
                <Tooltip contentStyle={tooltipStyle()} />
                <Line type="monotone" dataKey="highRiskCount" name="High-risk alerts" stroke="#ef4444" strokeWidth={2.5} dot={{ r: 3 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </section>

        <section className="glass rounded-2xl p-4">
          <h2 className="text-sm font-semibold text-zinc-300 mb-3">Alert Status Distribution</h2>
          <div className="h-[300px]">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={alertStatusDistribution} dataKey="value" nameKey="level" outerRadius={105} label>
                  {alertStatusDistribution.map((entry) => (
                    <Cell key={entry.level} fill={STATUS_COLORS[entry.level] || '#a1a1aa'} />
                  ))}
                </Pie>
                <Legend />
                <Tooltip content={<PieTooltip total={alertStatusDistributionTotal} />} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </section>
      </div>

      <div className="glass rounded-2xl p-4">
        <h3 className="text-sm font-semibold text-zinc-300 mb-2">Analyst hints</h3>
        <ul className="text-xs text-zinc-400 space-y-1">
          <li>Orange points indicate fraud alerts with risk score above 70% or BLOCK/FLAG status.</li>
          <li>Spikes in the red line indicate days with high alert volume — possible coordinated fraud activity.</li>
          <li>Risk distribution helps prioritize KYC and account monitoring workflows.</li>
          <li>Charts are based on fraud engine decisions (not transaction records) for accuracy.</li>
        </ul>
      </div>
    </div>
  );
}
