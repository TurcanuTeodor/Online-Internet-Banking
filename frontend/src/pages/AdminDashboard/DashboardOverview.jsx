import { useMemo, useState } from 'react';
import { Users, Wallet, ArrowLeftRight, ShieldAlert } from 'lucide-react';
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
} from '../../lib/analyticsTransforms';

const RANGE_OPTIONS = [7, 30, 90];
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

function SmartTooltip({ active, payload, label, formatter, labelFormatter }) {
  if (!active || !Array.isArray(payload) || payload.length === 0) return null;
  const validPayload = payload.filter((p) => p && p.value !== null && p.value !== undefined);
  if (validPayload.length === 0) return null;

  const renderedLabel = typeof labelFormatter === 'function' ? labelFormatter(label, validPayload) : label;

  return (
    <div style={tooltipStyle()}>
      {renderedLabel ? <p className="text-xs text-zinc-400 mb-1">{renderedLabel}</p> : null}
      <div className="space-y-0.5">
        {validPayload.map((entry, idx) => {
          const formatted = typeof formatter === 'function' ? formatter(entry.value, entry.name, entry, idx) : entry.value;
          const [valueText, nameText] = Array.isArray(formatted) ? formatted : [formatted, entry.name];
          return (
            <p key={`${entry.name}-${idx}`} className="text-xs text-zinc-200">
              <span className="text-zinc-400">{nameText || entry.name}:</span> {String(valueText)}
            </p>
          );
        })}
      </div>
    </div>
  );
}

function formatDate(value) {
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleString('en-GB');
}

export default function DashboardOverview({ clients, accounts, transactions }) {
  const [rangeDays, setRangeDays] = useState(30);
  const activeClients = (clients || []).filter((c) => c.active === true).length;
  const elevatedRisk = (clients || []).filter((c) => {
    const r = (c.riskLevel || '').toString().toUpperCase();
    return r === 'HIGH' || r === 'CRITICAL';
  }).length;
  const rangedTransactions = useMemo(
    () => filterTransactionsByLastDays(transactions, rangeDays),
    [transactions, rangeDays]
  );
  const scatterData = useMemo(() => prepareScatterAnomalyData(rangedTransactions), [rangedTransactions]);
  const highRiskPoints = useMemo(() => scatterData.filter((x) => x.highRisk), [scatterData]);
  const normalPoints = useMemo(() => scatterData.filter((x) => !x.highRisk), [scatterData]);
  const riskTimeData = useMemo(() => prepareHighRiskOverTimeData(rangedTransactions), [rangedTransactions]);
  const riskDistribution = useMemo(() => prepareClientRiskDistributionData(clients), [clients]);

  const stats = [
    {
      label: 'Total clients',
      value: (clients || []).length,
      icon: Users,
      accent: 'text-emerald-400',
      bg: 'bg-emerald-500/10',
    },
    {
      label: 'Total accounts',
      value: (accounts || []).length,
      icon: Wallet,
      accent: 'text-blue-400',
      bg: 'bg-blue-500/10',
    },
    {
      label: 'Total transactions',
      value: (transactions || []).length,
      icon: ArrowLeftRight,
      accent: 'text-violet-400',
      bg: 'bg-violet-500/10',
    },
    {
      label: 'Elevated risk (HIGH / CRITICAL)',
      value: elevatedRisk,
      sub: `${activeClients} active clients`,
      icon: ShieldAlert,
      accent: 'text-amber-400',
      bg: 'bg-amber-500/10',
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-xl font-bold">Overview</h2>
        <div className="flex items-center gap-2">
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
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        {stats.map((s) => {
          const Icon = s.icon;
          return (
            <div
              key={s.label}
              className="glass rounded-2xl p-5 border border-white/5 flex flex-col gap-3"
            >
              <div className="flex items-center justify-between gap-2">
                <span className="text-sm text-zinc-400 font-medium">{s.label}</span>
                <div className={`w-10 h-10 rounded-xl ${s.bg} flex items-center justify-center`}>
                  <Icon className={`w-5 h-5 ${s.accent}`} />
                </div>
              </div>
              <p className="text-3xl font-bold tracking-tight">{s.value}</p>
              {s.sub && <p className="text-xs text-zinc-500">{s.sub}</p>}
            </div>
          );
        })}
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-5 gap-4">
        <section className="glass rounded-2xl p-4 xl:col-span-3">
          <h3 className="text-sm font-semibold text-zinc-300 mb-3">Anomaly / Alerting Detection (last {rangeDays} days)</h3>
          <div className="h-[640px] xl:h-[680px]">
            <ResponsiveContainer width="100%" height="100%">
              <ScatterChart margin={{ top: 12, right: 24, left: 8, bottom: 8 }}>
                <CartesianGrid stroke="#27272a" strokeDasharray="3 3" />
                <XAxis type="number" dataKey="amount" name="Amount" tick={{ fill: '#a1a1aa', fontSize: 12 }} axisLine={{ stroke: '#3f3f46' }} />
                <YAxis type="number" dataKey="riskScore" name="Risk score" domain={[0, 100]} tick={{ fill: '#a1a1aa', fontSize: 12 }} axisLine={{ stroke: '#3f3f46' }} />
                <Tooltip
                  cursor={{ strokeDasharray: '3 3' }}
                  content={(props) => (
                    <SmartTooltip
                      {...props}
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
                  )}
                />
                <Legend />
                <Scatter name="Normal" data={normalPoints} fill="#60a5fa" />
                <Scatter name="High Risk (>70)" data={highRiskPoints} fill="#f97316" />
              </ScatterChart>
            </ResponsiveContainer>
          </div>
        </section>

        <div className="xl:col-span-2 grid grid-cols-1 gap-4">
          <section className="glass rounded-2xl p-4">
            <h3 className="text-sm font-semibold text-zinc-300 mb-3">Risk Over Time (High Risk count/day)</h3>
            <div className="h-[320px]">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={riskTimeData}>
                  <CartesianGrid stroke="#27272a" strokeDasharray="3 3" />
                  <XAxis dataKey="label" tick={{ fill: '#a1a1aa', fontSize: 12 }} axisLine={{ stroke: '#3f3f46' }} />
                  <YAxis allowDecimals={false} tick={{ fill: '#a1a1aa', fontSize: 12 }} axisLine={{ stroke: '#3f3f46' }} />
                  <Tooltip content={(props) => <SmartTooltip {...props} />} />
                  <Line type="monotone" dataKey="highRiskCount" stroke="#ef4444" strokeWidth={2.5} dot={{ r: 3 }} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </section>

          <section className="glass rounded-2xl p-4">
            <h3 className="text-sm font-semibold text-zinc-300 mb-3">Client Risk Distribution</h3>
            <div className="h-[320px]">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={riskDistribution} dataKey="value" nameKey="level" outerRadius={105} label>
                    {riskDistribution.map((entry) => (
                      <Cell key={entry.level} fill={RISK_COLORS[entry.level] || '#a1a1aa'} />
                    ))}
                  </Pie>
                  <Legend />
                  <Tooltip content={(props) => <SmartTooltip {...props} />} />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
