import { useState, useRef, useEffect } from 'react';
import { MoreVertical } from 'lucide-react';

/**
 * @param {{ actions: Array<{ label: string, onClick: () => void, disabled?: boolean, danger?: boolean }> }} props
 */
export default function RowActionsMenu({ actions }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    const onDoc = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', onDoc);
    return () => document.removeEventListener('mousedown', onDoc);
  }, []);

  return (
    <div className="relative inline-block text-left" ref={ref}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="p-1.5 rounded-lg text-zinc-400 hover:text-white hover:bg-zinc-800 transition-colors"
        aria-expanded={open}
        aria-haspopup="true"
        title="Actions"
      >
        <MoreVertical className="w-4 h-4" />
      </button>
      {open && (
        <div
          className="absolute right-0 z-50 mt-1 min-w-[10rem] rounded-xl border border-zinc-700 bg-zinc-900 shadow-xl py-1"
          role="menu"
        >
          {actions.map((a, i) => (
            <button
              key={i}
              type="button"
              role="menuitem"
              disabled={a.disabled}
              onClick={() => {
                setOpen(false);
                a.onClick();
              }}
              className={`w-full text-left px-2 py-1.5 text-xs transition-colors disabled:opacity-40 disabled:pointer-events-none ${
                a.danger
                  ? 'text-red-400 hover:bg-red-500/10'
                  : 'text-zinc-200 hover:bg-zinc-800'
              }`}
            >
              {a.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
