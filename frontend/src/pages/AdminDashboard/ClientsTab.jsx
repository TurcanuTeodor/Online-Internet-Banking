import { Filter, Search } from 'lucide-react';
import PaginationControls from './PaginationControls';
import AdminRiskBadge from './AdminRiskBadge';
import RowActionsMenu from './RowActionsMenu';

function clientTypeOf(c) {
  return c.clientType ?? c.clientTypeName ?? '—';
}

export default function ClientsTab({
  clients,
  filters,
  onFilterChange,
  onResetFilters,
  showFilters,
  onToggleFilters,
  onViewDetails,
  onViewAccounts,
  onSuspend,
}) {
  const getClientTypes = () => {
    const types = new Set(clients.map((c) => clientTypeOf(c)).filter(Boolean));
    return Array.from(types).sort();
  };

  const getFilteredClients = () => {
    let filtered = [...clients];

    if (filters.search?.trim()) {
      const q = filters.search.trim();
      filtered = filtered.filter((client) => String(client.clientId ?? '').includes(q));
    }

    if (filters.type !== 'all') {
      filtered = filtered.filter((client) => clientTypeOf(client) === filters.type);
    }

    if (filters.status !== 'all') {
      const isActive = filters.status === 'active';
      filtered = filtered.filter((client) => client.active === isActive);
    }

    return filtered;
  };

  const filteredClients = getFilteredClients();
  const totalPages = Math.ceil(filteredClients.length / filters.itemsPerPage);
  const paginatedClients = filteredClients.slice(
    (filters.page - 1) * filters.itemsPerPage,
    filters.page * filters.itemsPerPage
  );

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-4">
        <h2 className="text-xl font-bold">Clients ({filteredClients.length})</h2>
        <button
          onClick={onToggleFilters}
          className={`btn-secondary flex items-center justify-center gap-2 ${showFilters ? 'bg-emerald-500/20 border-emerald-500/30' : ''}`}
        >
          <Filter className="w-4 h-4" />
          {showFilters ? 'Hide' : 'Show'} Filters
        </button>
      </div>

      {showFilters && (
        <div className="glass rounded-2xl p-6 mb-4 animate-fade-in">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">Search by client ID</label>
              <div className="relative">
                <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
                <input
                  type="text"
                  inputMode="numeric"
                  value={filters.search}
                  onChange={(e) => onFilterChange('search', e.target.value)}
                  className="input-field input-with-icon"
                  placeholder="e.g. 12"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">Client type</label>
              <select
                value={filters.type}
                onChange={(e) => onFilterChange('type', e.target.value)}
                className="input-field"
              >
                <option value="all">All types</option>
                {getClientTypes().map((type) => (
                  <option key={type} value={type}>
                    {type}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">Status</label>
              <select
                value={filters.status}
                onChange={(e) => onFilterChange('status', e.target.value)}
                className="input-field"
              >
                <option value="all">All</option>
                <option value="active">Active</option>
                <option value="inactive">Inactive</option>
              </select>
            </div>
          </div>

          <div className="flex items-center justify-between mt-4 pt-4 border-t border-zinc-700">
            <p className="text-sm text-zinc-400">
              Found {filteredClients.length} client{filteredClients.length !== 1 ? 's' : ''}
            </p>
            <button onClick={onResetFilters} className="btn-secondary text-sm">
              Reset filters
            </button>
          </div>
        </div>
      )}

      {filteredClients.length === 0 ? (
        <div className="glass rounded-2xl p-12 text-center">
          <p className="text-zinc-400">No clients match your filters</p>
          {(filters.search || filters.type !== 'all' || filters.status !== 'all') && (
            <button onClick={onResetFilters} className="btn-secondary mt-4">
              Reset filters
            </button>
          )}
        </div>
      ) : (
        <>
          <div className="glass rounded-2xl overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-zinc-800/50">
                  <tr>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">ID</th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Type</th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Risk level</th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Status</th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Date</th>
                    <th className="px-6 py-4 text-right text-sm font-semibold text-zinc-300 w-px">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-800">
                  {paginatedClients.map((client) => (
                    <tr key={client.clientId} className="hover:bg-zinc-800/30 transition-colors">
                      <td className="px-6 py-4 text-sm font-mono text-zinc-300">{client.clientId}</td>
                      <td className="px-6 py-4 text-sm">
                        <span className="px-2 py-1 bg-blue-500/20 text-blue-400 rounded text-xs">
                          {clientTypeOf(client)}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-sm">
                        <AdminRiskBadge level={client.riskLevel || 'LOW'} />
                      </td>
                      <td className="px-6 py-4 text-sm">
                        <span
                          className={`px-2 py-1 rounded text-xs ${
                            client.active ? 'bg-emerald-500/20 text-emerald-400' : 'bg-red-500/20 text-red-400'
                          }`}
                        >
                          {client.active ? 'Active' : 'Inactive'}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-sm text-zinc-400">
                        {client.createdAt ? new Date(client.createdAt).toLocaleDateString() : '—'}
                      </td>
                      <td className="px-6 py-4 text-sm text-right">
                        <RowActionsMenu
                          actions={[
                            { label: 'View details', onClick: () => onViewDetails(client) },
                            {
                              label: 'View accounts',
                              onClick: () => onViewAccounts(client),
                            },
                            {
                              label: 'Suspend client',
                              onClick: () => onSuspend(client),
                              disabled: !client.active,
                              danger: true,
                            },
                          ]}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {totalPages > 1 && (
            <PaginationControls
              currentPage={filters.page}
              totalPages={totalPages}
              itemsPerPage={filters.itemsPerPage}
              onPageChange={(page) => onFilterChange('page', page)}
              onItemsPerPageChange={(itemsPerPage) => onFilterChange('itemsPerPage', itemsPerPage)}
            />
          )}
        </>
      )}
    </div>
  );
}
