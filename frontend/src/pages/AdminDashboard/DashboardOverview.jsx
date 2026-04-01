import { useMemo, useState } from 'react';
import { Users, Wallet, ArrowLeftRight, ShieldAlert, Activity } from 'lucide-react';
import {
  Area,
  AreaChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import {
  filterTransactionsByLastDays,
  preparePlatformVolumeData,
  prepareTransactionTypeDistribution,
} from '@/lib/analyticsTransforms';

const RANGE_OPTIONS = [7, 30, 90];

const TYPE_COLORS = ['#3b82f6', '#8b5cf6', '#22c55e', '#f59e0b', '#ef4444'];

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

function formatMoney(value) {
  const num = Number.isFinite(value) ? value : 0;
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'EUR',
    maximumFractionDigits: 0,
  }).format(num);
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
  
  const { volumeData, totalTxCount } = useMemo(
    () => preparePlatformVolumeData(rangedTransactions, rangeDays),
    [rangedTransactions, rangeDays]
  );

  const txTypes = useMemo(
    () => prepareTransactionTypeDistribution(rangedTransactions),
    [rangedTransactions]
  );

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
        <h2 className="text-xl font-bold">Platform Overview</h2>
        <div className="flex items-center gap-2">
          {RANGE_OPTIONS.map((d) => (
            <button
              key={d}
              type="button"
              onClick={() => setRangeDays(d)}
              className={`px-3 py-1.5 rounded-lg text-xs border transition-colors ${
                rangeDays === d
                  ? 'bg-blue-500/20 border-blue-500/40 text-blue-300'
                  : 'bg-zinc-900/70 border-zinc-700 text-zinc-400 hover:text-zinc-200'
              }`}
            >
              {d}d
            </button>
          ))}
        </div>
      </div>
      
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3">
        {stats.map((s) => {
          const Icon = s.icon;
          return (
            <div
              key={s.label}
              className="glass rounded-2xl p-4 border border-white/5 flex flex-col gap-2.5"
            >
              <div className="flex items-center justify-between gap-2">
                <span className="text-sm text-zinc-400 font-medium">{s.label}</span>
                <div className={`w-9 h-9 rounded-xl ${s.bg} flex items-center justify-center`}>
                  <Icon className={`w-5 h-5 ${s.accent}`} />
                </div>
              </div>
              <p className="text-2xl font-bold tracking-tight">{s.value}</p>
              {s.sub && <p className="text-xs text-zinc-500">{s.sub}</p>}
            </div>
          );
        })}
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-4">
        {/* Total Platform Volume */}
        <section className="glass rounded-2xl p-6 xl:col-span-2 relative border border-white/5">
          <div className="flex flex-wrap items-start justify-between mb-4 gap-4">
            <div>
              <h3 className="text-lg font-bold text-zinc-100 flex items-center gap-2">
                <Activity className="w-5 h-5 text-blue-400" />
                Capital Velocity & Platform Volume
              </h3>
              <p className="text-sm text-zinc-400">Total processed capital converted equivalent to EUR.</p>
            </div>
            <div className="text-right glass px-4 py-2 rounded-xl bg-zinc-900/80 border border-white/5 inline-flex items-center gap-4 shadow-lg shadow-black/20">
              <div>
                <p className="text-xs text-zinc-500 font-medium uppercase tracking-wider mb-0.5">Tx Volume</p>
                <p className="text-lg font-bold text-zinc-200 leading-none">{totalTxCount}</p>
              </div>
              <div className="w-px h-8 bg-zinc-800" />
              <div>
                <p className="text-xs text-zinc-500 font-medium uppercase tracking-wider mb-0.5">Sum EUR</p>
                <p className="text-lg font-bold text-emerald-400 leading-none">
                  {formatMoney(volumeData.reduce((acc, obj) => acc + obj.volumeEUR, 0))}
                </p>
              </div>
            </div>
          </div>
          
          <div className="h-[400px]">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={volumeData} margin={{ top: 12, right: 0, left: 16, bottom: 0 }}>
                <defs>
                  <linearGradient id="volumeGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0.05} />
                  </linearGradient>
                </defs>
                <CartesianGrid stroke="#27272a" strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="label" tick={{ fill: '#a1a1aa', fontSize: 12 }} axisLine={{ stroke: '#3f3f46' }} tickLine={false} />
                <YAxis 
                  tick={{ fill: '#a1a1aa', fontSize: 12 }} 
                  tickFormatter={(val) => `€${(val / 1000).toFixed(0)}k`} 
                  axisLine={{ stroke: '#3f3f46' }} 
                  tickLine={false} 
                />
                <Tooltip
                  cursor={{ stroke: '#52525b', strokeWidth: 1, strokeDasharray: '3 3' }}
                  content={(props) => (
                    <SmartTooltip
                      {...props}
                      formatter={(value, name) => {
                        if (name === 'Volume EUR') return [formatMoney(value), name];
                        return [value, name];
                      }}
                    />
                  )}
                />
                <Area 
                  type="monotone" 
                  dataKey="volumeEUR" 
                  name="Volume EUR" 
                  stroke="#3b82f6" 
                  fill="url(#volumeGradient)" 
                  strokeWidth={2} 
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </section>

        {/* Transaction Types Breakdown */}
        <section className="glass rounded-2xl p-6 border border-white/5">
          <h3 className="text-lg font-bold text-zinc-100 mb-1">Activity Distribution</h3>
          <p className="text-sm text-zinc-400 mb-6">Execution profile across your ecosystem.</p>
          <div className="h-[300px]">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie 
                  data={txTypes} 
                  dataKey="value" 
                  nameKey="name" 
                  cx="50%"
                  cy="45%"
                  outerRadius={90} 
                  innerRadius={55}
                  stroke="#18181b"
                  strokeWidth={2}
                  paddingAngle={2}
                >
                  {txTypes.map((entry, index) => (
                    <Cell key={entry.name} fill={TYPE_COLORS[index % TYPE_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip contentStyle={tooltipStyle()} />
                <Legend 
                  layout="horizontal" 
                  align="center" 
                  verticalAlign="bottom" 
                  iconType="circle"
                  formatter={(value, entry) => (
                    <span className="text-sm font-medium text-zinc-300 ml-1">{value} ({entry.payload.value})</span>
                  )}
                  wrapperStyle={{ paddingTop: '20px' }}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
          {txTypes.length === 0 && (
            <p className="text-zinc-500 text-sm py-4 text-center">No transactions mapped in this timeframe.</p>
          )}
        </section>
      </div>
    </div>
  );
}
