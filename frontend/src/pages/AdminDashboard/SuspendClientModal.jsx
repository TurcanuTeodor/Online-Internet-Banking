import { X } from 'lucide-react';

export default function SuspendClientModal({ client, onClose, onConfirm }) {
  if (!client) return null;

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="glass rounded-2xl p-4 max-w-md w-full">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-red-400">Suspend client</h2>
          <button onClick={onClose} className="p-2 rounded-lg hover:bg-zinc-800 transition-colors" type="button">
            <X className="w-5 h-5" />
          </button>
        </div>

        <p className="text-zinc-300 mb-4 text-sm">
          Suspend client <strong className="text-white font-mono">#{client.clientId}</strong>? This is irreversible for
          the client&apos;s access until re-enabled from the backend.
        </p>

        <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-3 mb-4">
          <p className="text-sm text-red-400">
            This action deactivates the client profile and blocks banking access.
          </p>
        </div>

        <div className="flex items-center justify-end gap-2">
          <button type="button" onClick={onClose} className="btn-secondary text-sm">
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-xl font-medium transition-colors text-sm"
          >
            Confirm suspend
          </button>
        </div>
      </div>
    </div>
  );
}
