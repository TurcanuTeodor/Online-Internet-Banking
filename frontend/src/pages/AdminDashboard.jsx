import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { logout } from '../../services/authService';
import { getAllClientsFromView } from '../../services/clientService';
import { getAllTransactionsFromView } from '../../services/transactionService';
import { LogOut, Users, TrendingUp, Shield, Loader2 } from 'lucide-react';

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState('clients');
  const [clients, setClients] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    fetchData();
  }, [activeTab]);

  const fetchData = async () => {
    setLoading(true);
    try {
      if (activeTab === 'clients') {
        const data = await getAllClientsFromView();
        setClients(data);
      } else if (activeTab === 'transactions') {
        const data = await getAllTransactionsFromView();
        setTransactions(data);
      }
    } catch (err) {
      console.error('Error fetching data:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-slate-950">
      {/* Nav */}
      <nav className="glass border-b border-white/10">
        <div className="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-emerald-500/20 rounded-xl flex items-center justify-center">
              <Shield className="w-5 h-5 text-emerald-400" />
            </div>
            <div>
              <h1 className="text-xl font-bold">CashTactics Admin</h1>
              <p className="text-xs text-zinc-500">Administrator Panel</p>
            </div>
          </div>
          <button onClick={handleLogout} className="btn-secondary flex items-center gap-2">
            <LogOut className="w-4 h-4" />
            Logout
          </button>
        </div>
      </nav>

      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Tabs */}
        <div className="flex gap-4 mb-8">
          <button
            onClick={() => setActiveTab('clients')}
            className={`flex items-center gap-2 px-6 py-3 rounded-xl font-medium transition-all ${
              activeTab === 'clients'
                ? 'bg-emerald-500 text-white'
                : 'glass text-zinc-400 hover:text-white'
            }`}
          >
            <Users className="w-5 h-5" />
            Clients
          </button>
          <button
            onClick={() => setActiveTab('transactions')}
            className={`flex items-center gap-2 px-6 py-3 rounded-xl font-medium transition-all ${
              activeTab === 'transactions'
                ? 'bg-emerald-500 text-white'
                : 'glass text-zinc-400 hover:text-white'
            }`}
          >
            <TrendingUp className="w-5 h-5" />
            Transactions
          </button>
        </div>

        {/* Content */}
        {loading ? (
          <div className="glass rounded-2xl p-12 flex flex-col items-center justify-center">
            <Loader2 className="w-12 h-12 text-emerald-400 animate-spin mb-4" />
            <p className="text-zinc-400">Loading data...</p>
          </div>
        ) : (
          <div className="glass rounded-2xl overflow-hidden">
            {activeTab === 'clients' && (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-zinc-800/50">
                    <tr>
                      <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">ID</th>
                      <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Name</th>
                      <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Email</th>
                      <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Type</th>
                      <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-800">
                    {clients.map((client) => (
                      <tr key={client.clientId} className="hover:bg-zinc-800/30 transition-colors">
                        <td className="px-6 py-4 text-sm text-zinc-400">{client.clientId}</td>
                        <td className="px-6 py-4 text-sm font-medium">{client.firstName} {client.lastName}</td>
                        <td className="px-6 py-4 text-sm text-zinc-400">{client.email || 'N/A'}</td>
                        <td className="px-6 py-4 text-sm">
                          <span className="px-2 py-1 bg-blue-500/20 text-blue-400 rounded text-xs">
                            {client.clientTypeCode}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-sm">
                          <span className={`px-2 py-1 rounded text-xs ${
                            client.active ? 'bg-emerald-500/20 text-emerald-400' : 'bg-red-500/20 text-red-400'
                          }`}>
                            {client.active ? 'Active' : 'Inactive'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {clients.length === 0 && (
                  <div className="p-12 text-center text-zinc-500">
                    No clients found
                  </div>
                )}
              </div>
            )}

            {activeTab === 'transactions' && (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-zinc-800/50">
                    <tr>
                      <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">ID</th>
                      <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">IBAN</th>
                      <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Type</th>
                      <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Amount</th>
                      <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Date</th>
                      <th className="px-6 py-4 text-left text-sm font-semibold text-zinc-300">Details</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-800">
                    {transactions.map((tx) => (
                      <tr key={tx.transactionId} className="hover:bg-zinc-800/30 transition-colors">
                        <td className="px-6 py-4 text-sm text-zinc-400">{tx.transactionId}</td>
                        <td className="px-6 py-4 text-sm font-mono text-xs">{tx.iban}</td>
                        <td className="px-6 py-4 text-sm">
                          <span className="px-2 py-1 bg-purple-500/20 text-purple-400 rounded text-xs">
                            {tx.transactionTypeCode}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-sm font-medium">
                          <span className={tx.sign === 'CREDIT' ? 'text-emerald-400' : 'text-red-400'}>
                            {tx.sign === 'CREDIT' ? '+' : '-'}{tx.amount} {tx.currencyCode}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-sm text-zinc-400">
                          {new Date(tx.transactionDate).toLocaleDateString()}
                        </td>
                        <td className="px-6 py-4 text-sm text-zinc-400">{tx.details}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {transactions.length === 0 && (
                  <div className="p-12 text-center text-zinc-500">
                    No transactions found
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
