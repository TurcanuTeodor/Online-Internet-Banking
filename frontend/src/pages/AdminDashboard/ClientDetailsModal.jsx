import { X, Wallet } from 'lucide-react';
import AdminRiskBadge from './AdminRiskBadge';

function firstNameOf(c) {
  return c.firstName ?? c.clientFirstName ?? '';
}

function lastNameOf(c) {
  return c.lastName ?? c.clientLastName ?? '';
}

function clientTypeOf(c) {
  return c.clientType ?? c.clientTypeName ?? '—';
}

export default function ClientDetailsModal({ client, onClose, onViewAccounts }) {
  if (!client) return null;

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="glass rounded-2xl p-4 max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold">Client details</h2>
          <button onClick={onClose} className="p-2 rounded-lg hover:bg-zinc-800 transition-colors" type="button">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          <div>
            <label className="text-xs text-zinc-400">Client ID</label>
            <p className="text-sm font-mono font-medium">{client.clientId}</p>
          </div>

          <div>
            <label className="text-xs text-zinc-400">Name</label>
            <p className="text-sm font-medium">
              {firstNameOf(client)} {lastNameOf(client)}
            </p>
          </div>

          <div>
            <label className="text-xs text-zinc-400">Email</label>
            <p className="text-sm font-medium">{client.email || '—'}</p>
          </div>

          <div>
            <label className="text-xs text-zinc-400">Phone</label>
            <p className="text-sm font-medium">{client.phone || '—'}</p>
          </div>

          <div>
            <label className="text-xs text-zinc-400">Client type</label>
            <p className="text-sm pt-1">
              <span className="px-2 py-0.5 bg-blue-500/20 text-blue-400 rounded text-xs">{clientTypeOf(client)}</span>
            </p>
          </div>

          <div>
            <label className="text-xs text-zinc-400">Risk level</label>
            <p className="text-sm pt-1">
              <AdminRiskBadge level={client.riskLevel || 'LOW'} />
            </p>
          </div>

          <div>
            <label className="text-xs text-zinc-400">Address</label>
            <p className="text-sm font-medium">{client.address || '—'}</p>
          </div>

          <div>
            <label className="text-xs text-zinc-400">City</label>
            <p className="text-sm font-medium">{client.city || '—'}</p>
          </div>

          <div>
            <label className="text-xs text-zinc-400">Postal code</label>
            <p className="text-sm font-medium">{client.postalCode || '—'}</p>
          </div>

          <div>
            <label className="text-xs text-zinc-400">Status</label>
            <p className="text-sm pt-1">
              <span
                className={`px-2 py-1 rounded text-sm ${
                  client.active ? 'bg-emerald-500/20 text-emerald-400' : 'bg-red-500/20 text-red-400'
                }`}
              >
                {client.active ? 'Active' : 'Inactive'}
              </span>
            </p>
          </div>

          <div>
            <label className="text-xs text-zinc-400">Registered</label>
            <p className="text-sm font-medium">
              {client.createdAt ? new Date(client.createdAt).toLocaleDateString() : '—'}
            </p>
          </div>
        </div>

        <div className="flex items-center justify-end gap-2 mt-4 pt-4 border-t border-zinc-700">
          <button
            type="button"
            onClick={() => {
              onClose();
              onViewAccounts(client);
            }}
            className="btn-secondary flex items-center gap-2 text-sm"
          >
            <Wallet className="w-4 h-4" />
            View accounts
          </button>
          <button type="button" onClick={onClose} className="btn-primary">
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
