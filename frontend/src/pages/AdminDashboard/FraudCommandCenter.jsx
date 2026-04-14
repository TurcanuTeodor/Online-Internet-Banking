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
  filterTransactionsByLastDays,
  prepareClientRiskDistributionData,
  prepareScatterAnomalyDataWithAlerts,
  prepareHighRiskOverTimeDataWithAlerts,
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

const RANGE_OPTIONS = [7, 30, 90];

function formatAmountWithSign(sign, amount) {
  const n = Number(amount);
  const safe = Number.isFinite(n) ? n : 0;
  const prefix = sign === '-' ? '-' : '+';
  return `${prefix}${safe.toFixed(2)}`;
}

function ScatterTooltip({ active, payload }) {
  if (!active || !Array.isArray(payload) || payload.length === 0) return null;
  const point = payload[0]?.payload;
  if (!point) return null;

  const title = `${point.type || 'Transaction'} • ${point.date ? formatDate(point.date) : '—'}`;
  const risk = Number.isFinite(Number(point.riskScore)) ? Number(point.riskScore).toFixed(1) : '—';
  const amount = formatAmountWithSign(point.sign, point.amount);

  return (
    <div style={{ ...tooltipStyle(), padding: 12, minWidth: 220 }}>
      <div style={{ fontWeight: 700, marginBottom: 6 }}>{title}</div>
      <div style={{ fontSize: 12, opacity: 0.9, lineHeight: 1.5 }}>
        <div><span style={{ opacity: 0.7 }}>Amount:</span> {amount}</div>
        <div><span style={{ opacity: 0.7 }}>Risk score:</span> {risk}%</div>
        <div><span style={{ opacity: 0.7 }}>Flagged:</span> {point.flagged ? 'Yes' : 'No'}</div>
        {point.accountId != null && <div><span style={{ opacity: 0.7 }}>Account:</span> {point.accountId}</div>}
        {point.destinationAccountId != null && <div><span style={{ opacity: 0.7 }}>Destination:</span> {point.destinationAccountId}</div>}
        {point.merchant && <div><span style={{ opacity: 0.7 }}>Merchant:</span> {String(point.merchant)}</div>}
        {point.id && <div><span style={{ opacity: 0.7 }}>Tx ID:</span> {String(point.id)}</div>}
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
        <div><span style={{ opacity: 0.7 }}>Clients:</span> {count}</div>
        <div><span style={{ opacity: 0.7 }}>Share:</span> {pct}%</div>
      </div>
    </div>
  );
}

export default function FraudCommandCenter({ transactions = [], clients = [], fraudAlerts = [] }) {
  const [rangeDays, setRangeDays] = useState(30);

  const rangedTransactions = useMemo(
    () => filterTransactionsByLastDays(transactions, rangeDays),
    [transactions, rangeDays]
  );
  // Fraud alerts/decisions are not necessarily reflected in the transaction view as `flagged`.
  // We overlay the latest alerts by transactionId so charts match the Fraud Alerts table.
  const alertedTransactionIds = useMemo(
    () => new Set((Array.isArray(fraudAlerts) ? fraudAlerts : []).map((a) => a?.transactionId).filter((x) => x != null)),
    [fraudAlerts]
  );

  const scatterData = useMemo(
    () => prepareScatterAnomalyDataWithAlerts(rangedTransactions, alertedTransactionIds),
    [rangedTransactions, alertedTransactionIds]
  );
  const highRiskPoints = useMemo(() => scatterData.filter((x) => x.highRisk), [scatterData]);
  const normalPoints = useMemo(() => scatterData.filter((x) => !x.highRisk), [scatterData]);
  const riskTimeData = useMemo(
    () => prepareHighRiskOverTimeDataWithAlerts(rangedTransactions, rangeDays, alertedTransactionIds),
    [rangedTransactions, rangeDays, alertedTransactionIds]
  );
  const riskDistribution = useMemo(() => prepareClientRiskDistributionData(clients), [clients]);
  const riskDistributionTotal = useMemo(
    () => (Array.isArray(riskDistribution) ? riskDistribution.reduce((acc, x) => acc + (Number(x?.value) || 0), 0) : 0),
    [riskDistribution]
  );

  return (
    <div className="space-y-4">
      <div className="glass rounded-2xl p-5 border border-red-500/10">
        <div className="flex items-center gap-3">
          <AlertTriangle className="w-5 h-5 text-red-400" />
          <div>
            <h1 className="text-2xl font-bold">AI Fraud Command Center</h1>
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
      </div>

      <section className="glass rounded-2xl p-4">
        <h2 className="text-sm font-semibold text-zinc-300 mb-3">Anomaly / Alerting Detection (last {rangeDays} days)</h2>
        <div className="h-[380px]">
          <ResponsiveContainer width="100%" height="100%">
            <ScatterChart margin={{ top: 12, right: 24, left: 8, bottom: 8 }}>
              <CartesianGrid stroke="#27272a" strokeDasharray="3 3" />
              <XAxis type="number" dataKey="amount" name="Amount" tick={{ fill: '#a1a1aa', fontSize: 12 }} axisLine={{ stroke: '#3f3f46' }} />
              <YAxis type="number" dataKey="riskScore" name="Risk score" domain={[0, 100]} tick={{ fill: '#a1a1aa', fontSize: 12 }} axisLine={{ stroke: '#3f3f46' }} />
              <Tooltip
                cursor={{ strokeDasharray: '3 3' }}
                content={<ScatterTooltip />}
              />
              <Legend />
              <Scatter name="Normal" data={normalPoints} fill="#60a5fa" />
              <Scatter name="Flagged / High Risk" data={highRiskPoints} fill="#f97316" />
            </ScatterChart>
          </ResponsiveContainer>
        </div>
      </section>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <section className="glass rounded-2xl p-4">
          <h2 className="text-sm font-semibold text-zinc-300 mb-3">Risk Over Time (High Risk count/day, last {rangeDays} days)</h2>
          <div className="h-[300px]">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={riskTimeData}>
                <CartesianGrid stroke="#27272a" strokeDasharray="3 3" />
                <XAxis dataKey="label" tick={{ fill: '#a1a1aa', fontSize: 12 }} axisLine={{ stroke: '#3f3f46' }} />
                <YAxis allowDecimals={false} tick={{ fill: '#a1a1aa', fontSize: 12 }} axisLine={{ stroke: '#3f3f46' }} />
                <Tooltip contentStyle={tooltipStyle()} />
                <Line type="monotone" dataKey="highRiskCount" stroke="#ef4444" strokeWidth={2.5} dot={{ r: 3 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </section>

        <section className="glass rounded-2xl p-4">
          <h2 className="text-sm font-semibold text-zinc-300 mb-3">Client Risk Distribution</h2>
          <div className="h-[300px]">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={riskDistribution} dataKey="value" nameKey="level" outerRadius={105} label>
                  {riskDistribution.map((entry) => (
                    <Cell key={entry.level} fill={RISK_COLORS[entry.level] || '#a1a1aa'} />
                  ))}
                </Pie>
                <Legend />
                <Tooltip content={<PieTooltip total={riskDistributionTotal} />} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </section>
      </div>

      <div className="glass rounded-2xl p-4">
        <h3 className="text-sm font-semibold text-zinc-300 mb-2">Analyst hints</h3>
        <ul className="text-xs text-zinc-400 space-y-1">
          <li>Orange points indicate transactions that are flagged by the fraud engine and/or have a risk score above 70%.</li>
          <li>Spikes in the red line can indicate coordinated fraud activity or a burst of flagged transactions.</li>
          <li>Risk distribution helps prioritize KYC and account monitoring workflows.</li>
        </ul>
        <p className="text-xs text-zinc-500 mt-2">
          Sample timestamp from dataset: {rangedTransactions[0]?.transactionDate ? formatDate(rangedTransactions[0].transactionDate) : '—'}
        </p>
      </div>
    </div>
  );
}
