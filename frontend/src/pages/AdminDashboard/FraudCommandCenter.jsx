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
  prepareHighRiskOverTimeData,
  prepareScatterAnomalyData,
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

export default function FraudCommandCenter({ transactions = [], clients = [] }) {
  const [rangeDays, setRangeDays] = useState(30);

  const rangedTransactions = useMemo(
    () => filterTransactionsByLastDays(transactions, rangeDays),
    [transactions, rangeDays]
  );
  const scatterData = useMemo(() => prepareScatterAnomalyData(rangedTransactions), [rangedTransactions]);
  const highRiskPoints = useMemo(() => scatterData.filter((x) => x.highRisk), [scatterData]);
  const normalPoints = useMemo(() => scatterData.filter((x) => !x.highRisk), [scatterData]);
  const riskTimeData = useMemo(() => prepareHighRiskOverTimeData(rangedTransactions), [rangedTransactions]);
  const riskDistribution = useMemo(() => prepareClientRiskDistributionData(clients), [clients]);

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
                contentStyle={tooltipStyle()}
                formatter={(value, name) => {
                  if (name === 'Amount') return [`${Number(value).toFixed(2)}`, 'Amount'];
                  return [`${Number(value).toFixed(1)}%`, 'Risk score'];
                }}
                labelFormatter={(_, payload) => {
                  const point = Array.isArray(payload) && payload[0]?.payload ? payload[0].payload : null;
                  if (!point) return 'Transaction';
                  const type = point.type || 'Transaction';
                  const dateText = point.date ? formatDate(point.date) : 'Unknown date';
                  return `${type} | ${dateText}`;
                }}
              />
              <Legend />
              <Scatter name="Normal" data={normalPoints} fill="#60a5fa" />
              <Scatter name="High Risk (>70)" data={highRiskPoints} fill="#f97316" />
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
                <Tooltip contentStyle={tooltipStyle()} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </section>
      </div>

      <div className="glass rounded-2xl p-4">
        <h3 className="text-sm font-semibold text-zinc-300 mb-2">Analyst hints</h3>
        <ul className="text-xs text-zinc-400 space-y-1">
          <li>Orange points in the scatter chart indicate transactions with risk score above 70%.</li>
          <li>Sharp spikes in the red line can indicate coordinated fraud activity.</li>
          <li>Risk distribution helps prioritize KYC and account monitoring workflows.</li>
        </ul>
        <p className="text-xs text-zinc-500 mt-2">
          Sample timestamp from dataset: {rangedTransactions[0]?.transactionDate ? formatDate(rangedTransactions[0].transactionDate) : '—'}
        </p>
      </div>
    </div>
  );
}
