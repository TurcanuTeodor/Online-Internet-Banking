import { X } from 'lucide-react';

export default function SuspendClientModal({ client, onClose, onConfirm }) {
  if (!client) return null;

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="glass rounded-2xl p-6 max-w-md w-full">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-2xl font-bold text-red-400">Suspend Client</h2>
          <button
            onClick={onClose}
            className="p-2 rounded-lg hover:bg-zinc-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <p className="text-zinc-300 mb-6">
          Are you sure you want to suspend client <strong>{client.clientFirstName} {client.clientLastName}</strong>?
        </p>

        <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-4 mb-6">
          <p className="text-sm text-red-400">
            ⚠️ This action will deactivate the client's account and prevent them from accessing banking services.
          </p>
        </div>

        <div className="flex items-center justify-end gap-3">
          <button
            onClick={onClose}
            className="btn-secondary"
          >
            Cancel
          </button>
          <button
            onClick={onConfirm}
            className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-xl font-medium transition-colors"
          >
            Confirm Suspend
          </button>
        </div>
      </div>
    </div>
  );
}
