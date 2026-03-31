import { useState, useEffect, useRef, useCallback } from 'react';
import { Bell, X, CheckCheck, AlertTriangle, ArrowDownToLine, ArrowUpFromLine, Info } from 'lucide-react';

function getNotificationIcon(type) {
  switch (type) {
    case 'success': return <ArrowDownToLine className="w-4 h-4 text-emerald-400" />;
    case 'warning': return <AlertTriangle className="w-4 h-4 text-amber-400" />;
    case 'error': return <ArrowUpFromLine className="w-4 h-4 text-red-400" />;
    default: return <Info className="w-4 h-4 text-blue-400" />;
  }
}

function timeAgo(date) {
  const seconds = Math.floor((Date.now() - new Date(date).getTime()) / 1000);
  if (seconds < 60) return 'just now';
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
  return `${Math.floor(seconds / 86400)}d ago`;
}

let globalId = 0;

export function useNotifications() {
  const [notifications, setNotifications] = useState([]);

  const addNotification = useCallback((message, type = 'info') => {
    const id = ++globalId;
    setNotifications((prev) => [{ id, message, type, timestamp: new Date().toISOString(), read: false }, ...prev].slice(0, 50));
    return id;
  }, []);

  const markAllRead = useCallback(() => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  }, []);

  const clearAll = useCallback(() => setNotifications([]), []);

  const unreadCount = notifications.filter((n) => !n.read).length;

  return { notifications, addNotification, markAllRead, clearAll, unreadCount };
}

export default function NotificationCenter({ notifications, unreadCount, onMarkAllRead, onClearAll }) {
  const [open, setOpen] = useState(false);
  const panelRef = useRef(null);

  useEffect(() => {
    const handler = (e) => {
      if (panelRef.current && !panelRef.current.contains(e.target)) setOpen(false);
    };
    if (open) document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [open]);

  return (
    <div className="relative" ref={panelRef}>
      <button
        onClick={() => { setOpen(!open); if (!open && unreadCount > 0) onMarkAllRead?.(); }}
        className="relative p-2 text-zinc-400 hover:text-white rounded-lg hover:bg-zinc-800 transition-colors"
        title="Notifications"
      >
        <Bell className="w-5 h-5" />
        {unreadCount > 0 && (
          <span className="absolute -top-0.5 -right-0.5 w-4 h-4 bg-red-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 top-full mt-2 w-80 glass rounded-2xl border border-white/10 shadow-2xl z-50 animate-fade-in overflow-hidden">
          <div className="flex items-center justify-between px-4 py-3 border-b border-white/10">
            <h3 className="text-sm font-semibold">Notifications</h3>
            <div className="flex items-center gap-2">
              {notifications.length > 0 && (
                <button onClick={onClearAll} className="text-xs text-zinc-500 hover:text-zinc-300 transition-colors flex items-center gap-1">
                  <CheckCheck className="w-3 h-3" />
                  Clear
                </button>
              )}
              <button onClick={() => setOpen(false)} className="text-zinc-500 hover:text-zinc-300 p-0.5">
                <X className="w-4 h-4" />
              </button>
            </div>
          </div>

          <div className="max-h-80 overflow-y-auto">
            {notifications.length === 0 ? (
              <p className="text-center text-zinc-500 text-sm py-8">No notifications yet.</p>
            ) : (
              notifications.map((n) => (
                <div key={n.id} className={`px-4 py-3 border-b border-white/5 ${n.read ? '' : 'bg-emerald-500/5'}`}>
                  <div className="flex items-start gap-2.5">
                    <div className="mt-0.5">{getNotificationIcon(n.type)}</div>
                    <div className="min-w-0 flex-1">
                      <p className="text-sm text-zinc-200">{n.message}</p>
                      <p className="text-xs text-zinc-500 mt-0.5">{timeAgo(n.timestamp)}</p>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
