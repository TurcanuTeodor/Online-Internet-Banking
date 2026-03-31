export default function TableSkeleton({ rows = 8, cols = 5 }) {
  return (
    <div className="glass rounded-2xl overflow-hidden">
      <div className="bg-zinc-800/50 px-4 py-3 flex gap-4">
        {Array.from({ length: cols }).map((_, i) => (
          <div key={i} className="skeleton h-3 flex-1" style={{ maxWidth: i === 0 ? 80 : 'none' }} />
        ))}
      </div>
      <div className="divide-y divide-zinc-800">
        {Array.from({ length: rows }).map((_, r) => (
          <div key={r} className="px-4 py-3 flex gap-4" style={{ animationDelay: `${r * 0.05}s` }}>
            {Array.from({ length: cols }).map((_, c) => (
              <div key={c} className="skeleton h-4 flex-1" style={{ maxWidth: c === 0 ? 80 : 'none' }} />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}
