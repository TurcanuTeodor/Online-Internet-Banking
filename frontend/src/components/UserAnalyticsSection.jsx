import { useMemo, useState, useEffect } from 'react';
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
import { Store, Pencil, Check } from 'lucide-react';
import {
  filterTransactionsByLastDays,
  prepareCashflowData,
  prepareExpenseCompositionData,
  prepareTopMerchants,
} from '@/lib/analyticsTransforms';

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
  const [budget, setBudget] = useState(2000);
  const [isEditingBudget, setIsEditingBudget] = useState(false);
  const [budgetInput, setBudgetInput] = useState('');

  useEffect(() => {
    const saved = localStorage.getItem('monthlyBudget');
    if (saved) setBudget(Number(saved));
  }, []);

  const handleSaveBudget = () => {
    const val = Number(budgetInput);
    if (!isNaN(val) && val > 0) {
      setBudget(val);
      localStorage.setItem('monthlyBudget', val);
    }
    setIsEditingBudget(false);
  };

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

  const topMerchants = useMemo(
    () => prepareTopMerchants(rangedTransactions, 3),
    [rangedTransactions]
  );

  const budgetProgress = Math.min((totalSpent / budget) * 100, 100);
  const isOverBudget = totalSpent > budget;

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
              className={`px-3 py-1.5 rounded-lg text-xs border transition-colors ${rangeDays === d
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
        {/* Cashflow Chart */}
        <div className="glass rounded-2xl p-4 xl:col-span-2">
          <h4 className="text-sm font-semibold text-zinc-300 mb-3">Cashflow Evolution (last {rangeDays} days)</h4>
          <div className="h-80 w-full mt-2">
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

        {/* Spending Categories */}
        <div className="glass rounded-2xl p-4">
          <h4 className="text-sm font-semibold text-zinc-300 mb-3">Spending Categories</h4>
          <div className="h-80 w-full mt-2">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={segments}
                  dataKey="value"
                  nameKey="name"
                  cx="50%"
                  cy="40%"
                  innerRadius={55}
                  outerRadius={85}
                  paddingAngle={2}
                  onClick={(entry) => onExpenseTypeSelect?.(entry?.name)}
                  cursor="pointer"
                  stroke="#18181b"
                  strokeWidth={2}
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
                <Legend 
                  layout="horizontal" 
                  align="center" 
                  verticalAlign="bottom" 
                  iconType="circle"
                  wrapperStyle={{ paddingTop: '10px' }}
                  formatter={(value) => <span className="text-xs font-medium text-zinc-300 ml-1">{value}</span>}
                />
                <text x="50%" y="35%" textAnchor="middle" dominantBaseline="middle" fill="#d4d4d8" fontSize={12}>
                  Total spent
                </text>
                <text x="50%" y="45%" textAnchor="middle" dominantBaseline="middle" fill="#fafafa" fontSize={15} fontWeight={700}>
                  {formatMoney(totalSpent)}
                </text>
              </PieChart>
            </ResponsiveContainer>
          </div>
          {segments.length === 0 && (
            <p className="text-xs text-zinc-500 -mt-2 text-center">No outflow transactions available.</p>
          )}
        </div>
      </div>

      {/* Second Row: Budget Tracker & Top Merchants */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Budget Tracker */}
        <div className="glass rounded-2xl p-6 flex flex-col justify-center">
          <div className="flex items-center justify-between mb-4">
            <h4 className="text-sm font-semibold text-zinc-300">Target Budget ({rangeDays}d)</h4>
            {!isEditingBudget ? (
              <button
                onClick={() => { setBudgetInput(String(budget)); setIsEditingBudget(true); }}
                className="text-zinc-400 hover:text-white transition-colors p-1"
                title="Edit budget"
              >
                <Pencil className="w-4 h-4" />
              </button>
            ) : (
              <div className="flex items-center gap-2">
                <input
                  type="number"
                  value={budgetInput}
                  onChange={(e) => setBudgetInput(e.target.value)}
                  className="bg-zinc-800 border border-zinc-700 rounded px-2 py-1 text-sm w-24 text-white focus:outline-none focus:border-emerald-500"
                  autoFocus
                />
                <button onClick={handleSaveBudget} className="text-emerald-400 hover:text-emerald-300 p-1">
                  <Check className="w-4 h-4" />
                </button>
              </div>
            )}
          </div>

          <div className="flex items-end gap-2 mb-2">
            <span className={`text-3xl font-bold tracking-tight ${isOverBudget ? 'text-red-400' : 'text-emerald-400'}`}>
              {formatMoney(totalSpent)}
            </span>
            <span className="text-zinc-500 text-sm mb-1">/ {formatMoney(budget)}</span>
          </div>

          <div className="w-full bg-zinc-800/80 rounded-full h-3 overflow-hidden shadow-inner border border-zinc-700/50">
            <div
              className={`h-full rounded-full transition-all duration-1000 ${isOverBudget ? 'bg-red-500' : 'bg-emerald-500'}`}
              style={{ width: `${budgetProgress}%` }}
            />
          </div>
          {isOverBudget ? (
            <p className="text-xs text-red-500 mt-3 font-medium">You have exceeded your target budget by {formatMoney(totalSpent - budget)}.</p>
          ) : (
            <p className="text-xs text-zinc-400 mt-3">You have {formatMoney(budget - totalSpent)} left in your budget.</p>
          )}
        </div>

        {/* Top 3 Merchants */}
        <div className="glass rounded-2xl p-6 flex flex-col">
          <h4 className="text-sm font-semibold text-zinc-300 mb-4">Top Merchants ({rangeDays}d)</h4>
          {topMerchants.length > 0 ? (
            <div className="space-y-4 flex-1">
              {topMerchants.map((merchant, i) => (
                <div key={merchant.name} className="flex items-center justify-between">
                  <div className="flex items-center gap-3 min-w-0">
                    <div className="w-10 h-10 rounded-xl bg-zinc-800/50 border border-zinc-700/50 flex items-center justify-center shrink-0">
                      <Store className="w-5 h-5 text-zinc-400" />
                    </div>
                    <div className="truncate">
                      <p className="text-sm font-semibold text-zinc-200 truncate">{merchant.name}</p>
                      <p className="text-xs text-zinc-500">#{i + 1} frequently paid</p>
                    </div>
                  </div>
                  <div className="shrink-0 text-right">
                    <p className="text-sm font-mono text-zinc-300 tracking-tight">
                      {formatMoney(merchant.total)}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="flex-1 flex items-center justify-center py-6">
              <p className="text-zinc-500 text-sm text-center">No merchants found for this period.</p>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
