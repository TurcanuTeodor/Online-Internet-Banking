import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { logout } from '../../../services/authService';
import { getAllClientsFromView, suspendClient } from '../../../services/clientService';
import { getAllTransactionsFromView } from '../../../services/transactionService';
import { getAllAccountsFromView } from '../../../services/accountService';
import { LogOut, Users, TrendingUp, Shield, Loader2, Wallet } from 'lucide-react';
import ClientsTab from './ClientsTab';
import AccountsTab from './AccountsTab';
import TransactionsTab from './TransactionsTab';
import ClientDetailsModal from './ClientDetailsModal';
import SuspendClientModal from './SuspendClientModal';
import AccountStatementModal from './AccountStatementModal';
import FreezeAccountModal from './FreezeAccountModal';

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState('clients');
  const [clients, setClients] = useState([]);
  const [accounts, setAccounts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  // Modals
  const [showClientDetailsModal, setShowClientDetailsModal] = useState(false);
  const [selectedClient, setSelectedClient] = useState(null);
  const [showSuspendModal, setShowSuspendModal] = useState(false);
  const [clientToSuspend, setClientToSuspend] = useState(null);
  const [showAccountStatementModal, setShowAccountStatementModal] = useState(false);
  const [selectedAccountForStatement, setSelectedAccountForStatement] = useState(null);
  const [showFreezeAccountModal, setShowFreezeAccountModal] = useState(false);
  const [accountToFreeze, setAccountToFreeze] = useState(null);

  // Client state
  const [showClientFilters, setShowClientFilters] = useState(false);
  const [clientFilters, setClientFilters] = useState({
    search: '',
    type: 'all',
    status: 'all',
    page: 1,
    itemsPerPage: 10,
  });

  // Account state
  const [showAccountFilters, setShowAccountFilters] = useState(false);
  const [accountFilters, setAccountFilters] = useState({
    search: '',
    status: 'all',
    currency: 'all',
    page: 1,
    itemsPerPage: 10,
  });

  // Transaction state
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
    fetchData();
  }, [activeTab]);

  // Reset to page 1 when filters change
  useEffect(() => {
    setClientFilters(prev => ({ ...prev, page: 1 }));
  }, [clientFilters.search, clientFilters.type, clientFilters.status, clientFilters.itemsPerPage]);

  useEffect(() => {
    setAccountFilters(prev => ({ ...prev, page: 1 }));
  }, [accountFilters.search, accountFilters.status, accountFilters.currency, accountFilters.itemsPerPage]);

  useEffect(() => {
    setTransactionFilters(prev => ({ ...prev, page: 1 }));
  }, [transactionFilters.type, transactionFilters.sign, transactionFilters.dateFrom, 
      transactionFilters.dateTo, transactionFilters.minAmount, transactionFilters.maxAmount, 
      transactionFilters.itemsPerPage]);

  const fetchData = async () => {
    setLoading(true);
    try {
      if (activeTab === 'clients') {
        const data = await getAllClientsFromView();
        setClients(data);
      } else if (activeTab === 'accounts') {
        const data = await getAllAccountsFromView();
        setAccounts(data);
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

  // Client handlers
  const handleViewClientDetails = (client) => {
    setSelectedClient(client);
    setShowClientDetailsModal(true);
  };

  const handleViewClientAccounts = (client) => {
    setActiveTab('accounts');
    setShowAccountFilters(true);
    setAccountFilters(prev => ({
      ...prev,
      search: `${client.clientFirstName} ${client.clientLastName}`.trim(),
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
      fetchData();
    } catch (err) {
      console.error('Error suspending client:', err);
    }
  };

  // Account handlers
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
      fetchData();
    } catch (err) {
      console.error('Error freezing account:', err);
    }
  };

  const handleClientFilterChange = (key, value) => {
    setClientFilters(prev => ({ ...prev, [key]: value }));
  };

  const handleAccountFilterChange = (key, value) => {
    setAccountFilters(prev => ({ ...prev, [key]: value }));
  };

  const handleTransactionFilterChange = (key, value) => {
    setTransactionFilters(prev => ({ ...prev, [key]: value }));
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
            onClick={() => setActiveTab('accounts')}
            className={`flex items-center gap-2 px-6 py-3 rounded-xl font-medium transition-all ${
              activeTab === 'accounts'
                ? 'bg-emerald-500 text-white'
                : 'glass text-zinc-400 hover:text-white'
            }`}
          >
            <Wallet className="w-5 h-5" />
            Accounts
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
          <>
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

        {/* Modals */}
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
    </div>
  );
}
