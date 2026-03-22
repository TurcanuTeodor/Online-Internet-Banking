/**
 * Risk level badge for admin tables.
 * LOW=green, MEDIUM=yellow, HIGH=orange, CRITICAL=red+pulse
 */
export default function AdminRiskBadge({ level }) {
  const key = (level || 'LOW').toString().trim().toUpperCase();
  const styles = {
    LOW: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30',
    MEDIUM: 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30',
    HIGH: 'bg-orange-500/20 text-orange-400 border-orange-500/30',
    CRITICAL: 'bg-red-500/25 text-red-400 border-red-500/40 animate-pulse',
  };
  const cls = styles[key] || 'bg-zinc-700/50 text-zinc-300 border-zinc-600';
  return (
    <span
      className={`inline-flex items-center px-2 py-0.5 rounded-md text-xs font-semibold border ${cls}`}
    >
      {key}
    </span>
  );
}
