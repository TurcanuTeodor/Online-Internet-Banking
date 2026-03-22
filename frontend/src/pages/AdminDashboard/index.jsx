import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { logout } from '../../../services/authService';
import { getAllClientsFromView, suspendClient } from '../../../services/clientService';
import { getAllTransactionsFromView } from '../../../services/transactionService';
import { getAllAccountsFromView } from '../../../services/accountService';
import {
  LogOut,
  Users,
  TrendingUp,
  Shield,
  Loader2,
  Wallet,
  LayoutDashboard,
} from 'lucide-react';
import ClientsTab from './ClientsTab';
import AccountsTab from './AccountsTab';
import TransactionsTab from './TransactionsTab';
import ClientDetailsModal from './ClientDetailsModal';
import SuspendClientModal from './SuspendClientModal';
import AccountStatementModal from './AccountStatementModal';
import FreezeAccountModal from './FreezeAccountModal';
import DashboardOverview from './DashboardOverview';

const NAV = [
  { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { id: 'clients', label: 'Clients', icon: Users },
  { id: 'accounts', label: 'Accounts', icon: Wallet },
  { id: 'transactions', label: 'Transactions', icon: TrendingUp },
];

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [clients, setClients] = useState([]);
  const [accounts, setAccounts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const [showClientDetailsModal, setShowClientDetailsModal] = useState(false);
  const [selectedClient, setSelectedClient] = useState(null);
  const [showSuspendModal, setShowSuspendModal] = useState(false);
  const [clientToSuspend, setClientToSuspend] = useState(null);
  const [showAccountStatementModal, setShowAccountStatementModal] = useState(false);
  const [selectedAccountForStatement, setSelectedAccountForStatement] = useState(null);
  const [showFreezeAccountModal, setShowFreezeAccountModal] = useState(false);
  const [accountToFreeze, setAccountToFreeze] = useState(null);

  const [showClientFilters, setShowClientFilters] = useState(false);
  const [clientFilters, setClientFilters] = useState({
    search: '',
    type: 'all',
    status: 'all',
    page: 1,
    itemsPerPage: 10,
  });

  const [showAccountFilters, setShowAccountFilters] = useState(false);
  const [accountFilters, setAccountFilters] = useState({
    search: '',
    status: 'all',
    currency: 'all',
    page: 1,
    itemsPerPage: 10,
  });

  const [showTransactionFilters, setShowTransactionFilters] = useState(false);
  const [transactionFilters, setTransactionFilters] = useState({
    type: 'all',
    sign: 'all',
    dateFrom: '',
    dateTo: '',
    minAmount: '',
    maxAmount: '',
    page: 1,
    itemsPerPage: 10,
  });

  useEffect(() => {
    fetchAllData();
  }, []);

  useEffect(() => {
    setClientFilters((prev) => ({ ...prev, page: 1 }));
  }, [clientFilters.search, clientFilters.type, clientFilters.status, clientFilters.itemsPerPage]);

  useEffect(() => {
    setAccountFilters((prev) => ({ ...prev, page: 1 }));
  }, [accountFilters.search, accountFilters.status, accountFilters.currency, accountFilters.itemsPerPage]);

  useEffect(() => {
    setTransactionFilters((prev) => ({ ...prev, page: 1 }));
  }, [
    transactionFilters.type,
    transactionFilters.sign,
    transactionFilters.dateFrom,
    transactionFilters.dateTo,
    transactionFilters.minAmount,
    transactionFilters.maxAmount,
    transactionFilters.itemsPerPage,
  ]);

  const fetchAllData = async () => {
    setLoading(true);
    try {
      const [c, a, t] = await Promise.all([
        getAllClientsFromView(),
        getAllAccountsFromView(),
        getAllTransactionsFromView(),
      ]);
      setClients(Array.isArray(c) ? c : []);
      setAccounts(Array.isArray(a) ? a : []);
      setTransactions(Array.isArray(t) ? t : []);
    } catch (err) {
      console.error('Error fetching admin data:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const handleViewClientDetails = (client) => {
    setSelectedClient(client);
    setShowClientDetailsModal(true);
  };

  const handleViewClientAccounts = (client) => {
    setActiveTab('accounts');
    setShowAccountFilters(true);
    setAccountFilters((prev) => ({
      ...prev,
      search: String(client.clientId ?? ''),
      page: 1,
    }));
  };

  const handleSuspendClient = (client) => {
    setClientToSuspend(client);
    setShowSuspendModal(true);
  };

  const confirmSuspend = async () => {
    if (!clientToSuspend?.clientId) return;
    try {
      await suspendClient(clientToSuspend.clientId);
      setShowSuspendModal(false);
      setClientToSuspend(null);
      fetchAllData();
    } catch (err) {
      console.error('Error suspending client:', err);
    }
  };

  const handleViewAccountStatement = (account) => {
    setSelectedAccountForStatement(account);
    setShowAccountStatementModal(true);
  };

  const handleFreezeAccount = (account) => {
    setAccountToFreeze(account);
    setShowFreezeAccountModal(true);
  };

  const confirmFreezeAccount = async () => {
    try {
      const { freezeAccount } = await import('../../../services/accountService');
      await freezeAccount(accountToFreeze.accountId);
      setShowFreezeAccountModal(false);
      setAccountToFreeze(null);
      fetchAllData();
    } catch (err) {
      console.error('Error freezing account:', err);
    }
  };

  const handleClientFilterChange = (key, value) => {
    setClientFilters((prev) => ({ ...prev, [key]: value }));
  };

  const handleAccountFilterChange = (key, value) => {
    setAccountFilters((prev) => ({ ...prev, [key]: value }));
  };

  const handleTransactionFilterChange = (key, value) => {
    setTransactionFilters((prev) => ({ ...prev, [key]: value }));
  };

  const resetClientFilters = () => {
    setClientFilters({
      search: '',
      type: 'all',
      status: 'all',
      page: 1,
      itemsPerPage: 10,
    });
  };

  const resetAccountFilters = () => {
    setAccountFilters({
      search: '',
      status: 'all',
      currency: 'all',
      page: 1,
      itemsPerPage: 10,
    });
  };

  const resetTransactionFilters = () => {
    setTransactionFilters({
      type: 'all',
      sign: 'all',
      dateFrom: '',
      dateTo: '',
      minAmount: '',
      maxAmount: '',
      page: 1,
      itemsPerPage: 10,
    });
  };

  return (
    <div className="min-h-screen bg-slate-950 flex">
      {/* Sidebar */}
      <aside className="hidden md:flex w-56 shrink-0 flex-col border-r border-white/10 bg-zinc-900/90 backdrop-blur-sm">
        <div className="p-4 border-b border-white/10 flex items-center gap-3">
          <div className="w-10 h-10 bg-emerald-500/20 rounded-xl flex items-center justify-center shrink-0">
            <Shield className="w-5 h-5 text-emerald-400" />
          </div>
          <div className="min-w-0">
            <p className="text-sm font-bold truncate">CashTactics</p>
            <p className="text-xs text-zinc-500">Admin</p>
          </div>
        </div>
        <nav className="p-3 flex flex-col gap-1">
          {NAV.map((item) => {
            const Icon = item.icon;
            const active = activeTab === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => setActiveTab(item.id)}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors text-left w-full ${
                  active
                    ? 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/30'
                    : 'text-zinc-400 hover:text-white hover:bg-zinc-800/80 border border-transparent'
                }`}
              >
                <Icon className="w-5 h-5 shrink-0" />
                {item.label}
              </button>
            );
          })}
        </nav>
      </aside>

      <div className="flex-1 flex flex-col min-w-0">
        <nav className="glass border-b border-white/10 shrink-0">
          <div className="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between gap-3">
            <div className="md:hidden flex items-center gap-2 text-sm font-bold text-zinc-200">
              <Shield className="w-5 h-5 text-emerald-400" />
              Admin
            </div>
            <div className="hidden md:block" />
            <button onClick={handleLogout} className="btn-secondary flex items-center gap-2 text-sm px-3 py-2">
              <LogOut className="w-4 h-4" />
              Logout
            </button>
          </div>
        </nav>

        {/* Mobile nav */}
        <div className="md:hidden border-b border-white/10 bg-zinc-900/80 px-2 py-2 flex gap-1 overflow-x-auto">
          {NAV.map((item) => {
            const Icon = item.icon;
            const active = activeTab === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => setActiveTab(item.id)}
                className={`flex items-center gap-2 px-3 py-2 rounded-lg text-xs font-medium whitespace-nowrap shrink-0 ${
                  active ? 'bg-emerald-500/20 text-emerald-300' : 'text-zinc-400'
                }`}
              >
                <Icon className="w-4 h-4" />
                {item.label}
              </button>
            );
          })}
        </div>

        <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-8">
          {loading ? (
            <div className="glass rounded-2xl p-12 flex flex-col items-center justify-center">
              <Loader2 className="w-12 h-12 text-emerald-400 animate-spin mb-4" />
              <p className="text-zinc-400">Loading data...</p>
            </div>
          ) : (
            <>
              {activeTab === 'dashboard' && (
                <DashboardOverview clients={clients} accounts={accounts} transactions={transactions} />
              )}

              {activeTab === 'clients' && (
                <ClientsTab
                  clients={clients}
                  filters={clientFilters}
                  onFilterChange={handleClientFilterChange}
                  onResetFilters={resetClientFilters}
                  showFilters={showClientFilters}
                  onToggleFilters={() => setShowClientFilters(!showClientFilters)}
                  onViewDetails={handleViewClientDetails}
                  onViewAccounts={handleViewClientAccounts}
                  onSuspend={handleSuspendClient}
                />
              )}

              {activeTab === 'accounts' && (
                <AccountsTab
                  accounts={accounts}
                  filters={accountFilters}
                  onFilterChange={handleAccountFilterChange}
                  onResetFilters={resetAccountFilters}
                  showFilters={showAccountFilters}
                  onToggleFilters={() => setShowAccountFilters(!showAccountFilters)}
                  onViewStatement={handleViewAccountStatement}
                  onFreezeAccount={handleFreezeAccount}
                />
              )}

              {activeTab === 'transactions' && (
                <TransactionsTab
                  transactions={transactions}
                  filters={transactionFilters}
                  onFilterChange={handleTransactionFilterChange}
                  onResetFilters={resetTransactionFilters}
                  showFilters={showTransactionFilters}
                  onToggleFilters={() => setShowTransactionFilters(!showTransactionFilters)}
                />
              )}
            </>
          )}
        </main>
      </div>

      {showClientDetailsModal && (
        <ClientDetailsModal
          client={selectedClient}
          onClose={() => setShowClientDetailsModal(false)}
          onViewAccounts={handleViewClientAccounts}
        />
      )}

      {showSuspendModal && (
        <SuspendClientModal
          client={clientToSuspend}
          onClose={() => {
            setShowSuspendModal(false);
            setClientToSuspend(null);
          }}
          onConfirm={confirmSuspend}
        />
      )}

      {showAccountStatementModal && (
        <AccountStatementModal
          account={selectedAccountForStatement}
          onClose={() => {
            setShowAccountStatementModal(false);
            setSelectedAccountForStatement(null);
          }}
        />
      )}

      {showFreezeAccountModal && (
        <FreezeAccountModal
          account={accountToFreeze}
          onClose={() => {
            setShowFreezeAccountModal(false);
            setAccountToFreeze(null);
          }}
          onConfirm={confirmFreezeAccount}
        />
      )}
    </div>
  );
}
