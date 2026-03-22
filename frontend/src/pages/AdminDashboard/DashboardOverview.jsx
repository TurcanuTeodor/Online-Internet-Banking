import { Users, Wallet, ArrowLeftRight, ShieldAlert } from 'lucide-react';

export default function DashboardOverview({ clients, accounts, transactions }) {
  const activeClients = (clients || []).filter((c) => c.active === true).length;
  const elevatedRisk = (clients || []).filter((c) => {
    const r = (c.riskLevel || '').toString().toUpperCase();
    return r === 'HIGH' || r === 'CRITICAL';
  }).length;

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
    <div>
      <h2 className="text-xl font-bold mb-6">Overview</h2>
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
    </div>
  );
}
