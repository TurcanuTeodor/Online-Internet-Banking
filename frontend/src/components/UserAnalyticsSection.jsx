import { useMemo, useState } from 'react';
import {
  Area,
  AreaChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import {
  filterTransactionsByLastDays,
  prepareCashflowData,
  prepareExpenseCompositionData,
} from '../lib/analyticsTransforms';

const DONUT_COLORS = ['#22c55e', '#06b6d4', '#8b5cf6', '#f59e0b', '#ef4444', '#3b82f6', '#a855f7'];

function chartTooltipStyle() {
  return {
    backgroundColor: '#09090b',
    border: '1px solid #27272a',
    borderRadius: 12,
    color: '#e4e4e7',
  };
}

function SmartTooltip({ active, payload, label, formatter }) {
  if (!active || !Array.isArray(payload) || payload.length === 0) return null;
  const validPayload = payload.filter((p) => p && p.value !== null && p.value !== undefined);
  if (validPayload.length === 0) return null;

  return (
    <div style={chartTooltipStyle()}>
      {label ? <p className="text-xs text-zinc-400 mb-1">{label}</p> : null}
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

const RANGE_OPTIONS = [7, 30, 90];

export default function UserAnalyticsSection({ transactions, onExpenseTypeSelect }) {
  const [rangeDays, setRangeDays] = useState(30);
  const rangedTransactions = useMemo(
    () => filterTransactionsByLastDays(transactions, rangeDays),
    [transactions, rangeDays]
  );
  const cashflowData = useMemo(
    () => prepareCashflowData(rangedTransactions, rangeDays),
    [rangedTransactions, rangeDays]
  );
  const { segments, totalSpent } = useMemo(
    () => prepareExpenseCompositionData(rangedTransactions),
    [rangedTransactions]
  );

  return (
    <section className="space-y-4">
      <div>
        <h3 className="text-xl font-bold">Analytics</h3>
        <p className="text-sm text-zinc-500">Insights from your latest transactions and spending behavior.</p>
        <div className="flex items-center gap-2 mt-3">
          {RANGE_OPTIONS.map((d) => (
            <button
              key={d}
              type="button"
              onClick={() => setRangeDays(d)}
              className={`px-3 py-1.5 rounded-lg text-xs border transition-colors ${
                rangeDays === d
                  ? 'bg-emerald-500/20 border-emerald-500/40 text-emerald-300'
                  : 'bg-zinc-900/70 border-zinc-700 text-zinc-400 hover:text-zinc-200'
              }`}
            >
              {d}d
            </button>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-4">
        <div className="glass rounded-2xl p-4 xl:col-span-2">
          <h4 className="text-sm font-semibold text-zinc-300 mb-3">Cashflow Evolution (last {rangeDays} days)</h4>
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={cashflowData}>
                <defs>
                  <linearGradient id="inflowGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#22c55e" stopOpacity={0.55} />
                    <stop offset="95%" stopColor="#22c55e" stopOpacity={0.05} />
                  </linearGradient>
                  <linearGradient id="outflowGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#ef4444" stopOpacity={0.5} />
                    <stop offset="95%" stopColor="#ef4444" stopOpacity={0.07} />
                  </linearGradient>
                </defs>
                <CartesianGrid stroke="#27272a" strokeDasharray="3 3" />
                <XAxis dataKey="label" tick={{ fill: '#a1a1aa', fontSize: 12 }} tickLine={false} axisLine={{ stroke: '#3f3f46' }} />
                <YAxis tick={{ fill: '#a1a1aa', fontSize: 12 }} tickLine={false} axisLine={{ stroke: '#3f3f46' }} />
                <Tooltip
                  content={(props) => (
                    <SmartTooltip
                      {...props}
                      formatter={(value, name) => [formatMoney(Math.abs(Number(value) || 0)), name]}
                    />
                  )}
                />
                <Area type="monotone" dataKey="inflow" name="Inflow" stroke="#22c55e" fill="url(#inflowGradient)" strokeWidth={2} />
                <Area type="monotone" dataKey="outflow" name="Outflow" stroke="#ef4444" fill="url(#outflowGradient)" strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="glass rounded-2xl p-4">
          <h4 className="text-sm font-semibold text-zinc-300 mb-3">Expense Composition</h4>
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={segments}
                  dataKey="value"
                  nameKey="name"
                  innerRadius={70}
                  outerRadius={105}
                  paddingAngle={2}
                  onClick={(entry) => onExpenseTypeSelect?.(entry?.name)}
                  cursor="pointer"
                >
                  {segments.map((entry, idx) => (
                    <Cell key={`${entry.name}-${idx}`} fill={DONUT_COLORS[idx % DONUT_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip
                  content={(props) => (
                    <SmartTooltip
                      {...props}
                      formatter={(value, name) => [formatMoney(Number(value) || 0), name]}
                    />
                  )}
                />
                <text x="50%" y="48%" textAnchor="middle" dominantBaseline="middle" fill="#d4d4d8" fontSize={13}>
                  Total spent
                </text>
                <text x="50%" y="57%" textAnchor="middle" dominantBaseline="middle" fill="#fafafa" fontSize={16} fontWeight={700}>
                  {formatMoney(totalSpent)}
                </text>
              </PieChart>
            </ResponsiveContainer>
          </div>
          {segments.length === 0 && (
            <p className="text-xs text-zinc-500 -mt-2">No outflow transactions available to build composition.</p>
          )}
        </div>
      </div>
    </section>
  );
}
