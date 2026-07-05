import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { logout } from '@/services/authService';
import { getAllClientsFromView, suspendClient, reactivateClient } from '@/services/clientService';
import { getAllTransactionsFromView } from '@/services/transactionService';
import { getAllAccountsFromView } from '@/services/accountService';
import { logSensitiveDataReveal } from '@/services/adminAuditService';

import {
  LogOut,
  Users,
  TrendingUp,
  Shield,
  Loader2,
  Wallet,
  LayoutDashboard,
  ShieldAlert,
  FileText,
  Menu,
  X,
} from 'lucide-react';
import ClientsTab from './ClientsTab';
import AccountsTab from './AccountsTab';
import TransactionsTab from './TransactionsTab';
import ClientDetailsModal from './ClientDetailsModal';
import SuspendClientModal from './SuspendClientModal';
import AccountStatementModal from './AccountStatementModal';
import FreezeAccountModal from './FreezeAccountModal';
import RevealSensitiveDataModal from './RevealSensitiveDataModal';

import DashboardOverview from './DashboardOverview';
import FraudAlertsTab from './FraudAlertsTab';
import FraudCommandCenter from './FraudCommandCenter';
import RevealAuditTab from './RevealAuditTab';
import TransactionDetailsModal from '@/components/TransactionDetailsModal';
import ConfirmDialog from '@/components/ConfirmDialog';
import { getFraudAlertsForCharts } from '@/services/fraudService';

const VALID_TABS = ['dashboard', 'clients', 'accounts', 'transactions', 'fraud', 'audit'];
const NAV = [
  { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { id: 'clients', label: 'Clients', icon: Users },
  { id: 'accounts', label: 'Accounts', icon: Wallet },
  { id: 'transactions', label: 'Transactions', icon: TrendingUp },
  { id: 'fraud', label: 'Fraud Center', icon: ShieldAlert },
  { id: 'audit', label: 'Audit Logs', icon: FileText },
];

export default function AdminDashboard() {
  const { tab } = useParams();
  const navigate = useNavigate();
  const activeTab = VALID_TABS.includes(tab) ? tab : 'dashboard';
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);
  const [clients, setClients] = useState([]);
  const [accounts, setAccounts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [fraudAlertsForCharts, setFraudAlertsForCharts] = useState([]);

  const [showClientDetailsModal, setShowClientDetailsModal] = useState(false);
  const [selectedClient, setSelectedClient] = useState(null);
  const [showSuspendModal, setShowSuspendModal] = useState(false);
  const [clientToSuspend, setClientToSuspend] = useState(null);
  const [showAccountStatementModal, setShowAccountStatementModal] = useState(false);
  const [selectedAccountForStatement, setSelectedAccountForStatement] = useState(null);
  const [showFreezeAccountModal, setShowFreezeAccountModal] = useState(false);
  const [accountToFreeze, setAccountToFreeze] = useState(null);
  const [showCloseAccountModal, setShowCloseAccountModal] = useState(false);
  const [accountToClose, setAccountToClose] = useState(null);
  const [showReactivateModal, setShowReactivateModal] = useState(false);
  const [clientToReactivate, setClientToReactivate] = useState(null);
  const [showUnfreezeAccountModal, setShowUnfreezeAccountModal] = useState(false);
  const [accountToUnfreeze, setAccountToUnfreeze] = useState(null);
  const [selectedTransactionId, setSelectedTransactionId] = useState(null);

  const [showAccountSensitiveData, setShowAccountSensitiveData] = useState(false);
  const [showClientSensitiveData, setShowClientSensitiveData] = useState(false);
  const [showRevealModal, setShowRevealModal] = useState(false);
  const [revealScope, setRevealScope] = useState(null);
  const [revealLoading, setRevealLoading] = useState(false);


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
    let mounted = true;
    (async () => {
      if (activeTab !== 'fraud') return;
      try {
        const page = await getFraudAlertsForCharts(1000);
        const arr = Array.isArray(page?.content) ? page.content : [];
        if (mounted) setFraudAlertsForCharts(arr);
      } catch {
        if (mounted) setFraudAlertsForCharts([]);
      }
    })();
    return () => { mounted = false; };
  }, [activeTab]);

  useEffect(() => {
    if (!tab || !VALID_TABS.includes(tab)) {
      navigate('/admin/dashboard', { replace: true });
    }
  }, [tab, navigate]);

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
    navigate('/admin/accounts');
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

  const handleReactivateClient = (client) => {
    setClientToReactivate(client);
    setShowReactivateModal(true);
  };

  const confirmReactivate = async () => {
    if (!clientToReactivate?.clientId) return;
    try {
      await reactivateClient(clientToReactivate.clientId);
      setShowReactivateModal(false);
      setClientToReactivate(null);
      fetchAllData();
    } catch (err) {
      console.error('Error reactivating client:', err);
    }
  };

  const handleViewAccountStatement = (account) => {
    setSelectedAccountForStatement(account);
    setShowAccountStatementModal(true);
  };

  const handleRequestSensitiveReveal = (scope) => {
    setRevealScope(scope);
    setShowRevealModal(true);
  };

  const confirmSensitiveReveal = async ({ reasonCode, reasonDetails }) => {
    if (!revealScope) return;
    setRevealLoading(true);
    try {
      await logSensitiveDataReveal({
        scope: revealScope.scope,
        targetType: revealScope.targetType,
        targetId: String(revealScope.targetId || 'dashboard'),
        reasonCode,
        reasonDetails,
      });
      if (revealScope.scope === 'ADMIN_ACCOUNTS') {
        setShowAccountSensitiveData(true);
      }
      if (revealScope.scope === 'ADMIN_CLIENTS') {
        setShowClientSensitiveData(true);
      }
      setShowRevealModal(false);
      setRevealScope(null);
    } catch (err) {
      console.error('Error logging sensitive data reveal:', err);
    } finally {
      setRevealLoading(false);
    }
  };

  const handleFreezeAccount = (account) => {
    setAccountToFreeze(account);
    setShowFreezeAccountModal(true);
  };

  const confirmFreezeAccount = async () => {
    try {
      const { freezeAccount } = await import('@/services/accountService');
      await freezeAccount(accountToFreeze.accountId);
      setShowFreezeAccountModal(false);
      setAccountToFreeze(null);
      fetchAllData();
    } catch (err) {
      console.error('Error freezing account:', err);
    }
  };

  const handleCloseAccount = (account) => {
    setAccountToClose(account);
    setShowCloseAccountModal(true);
  };

  const confirmCloseAccount = async () => {
    try {
      const { closeAccount } = await import('@/services/accountService');
      await closeAccount(accountToClose.accountId);
      setShowCloseAccountModal(false);
      setAccountToClose(null);
      fetchAllData();
    } catch (err) {
      console.error('Error closing account:', err);
    }
  };

  const handleUnfreezeAccount = (account) => {
    setAccountToUnfreeze(account);
    setShowUnfreezeAccountModal(true);
  };

  const confirmUnfreezeAccount = async () => {
    try {
      const { unfreezeAccount } = await import('@/services/accountService');
      await unfreezeAccount(accountToUnfreeze.accountId);
      setShowUnfreezeAccountModal(false);
      setAccountToUnfreeze(null);
      fetchAllData();
    } catch (err) {
      console.error('Error unfreezing account:', err);
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

  const sidebarContent = (
    <>
      <div className="p-3 border-b border-white/10 flex items-center gap-3">
        <div className="w-10 h-10 bg-emerald-500/20 rounded-xl flex items-center justify-center shrink-0">
          <Shield className="w-5 h-5 text-emerald-400" />
        </div>
        <div className="min-w-0">
          <p className="text-sm font-bold truncate">CashTactics</p>
          <p className="text-xs text-zinc-500">Admin</p>
        </div>
      </div>
      <nav className="p-2.5 flex flex-col gap-1">
        {NAV.map((item) => {
          const Icon = item.icon;
          const active = activeTab === item.id;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => { navigate(`/admin/${item.id}`); setMobileSidebarOpen(false); }}
              className={`flex items-center gap-2.5 px-2.5 py-2 rounded-xl text-sm font-medium transition-colors text-left w-full ${
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
    </>
  );

  return (
    <div className="min-h-screen bg-slate-950 flex">
      {/* Desktop Sidebar */}
      <aside className="hidden md:flex w-52 shrink-0 flex-col border-r border-white/10 bg-zinc-900/90 backdrop-blur-sm">
        {sidebarContent}
      </aside>

      {/* Mobile Sidebar Drawer */}
      {mobileSidebarOpen && (
        <div className="fixed inset-0 z-50 md:hidden">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setMobileSidebarOpen(false)} />
          <aside className="absolute left-0 top-0 bottom-0 w-64 bg-zinc-900 border-r border-white/10 flex flex-col animate-slide-in-left">
            <div className="flex items-center justify-end p-2">
              <button onClick={() => setMobileSidebarOpen(false)} className="p-2 text-zinc-400 hover:text-white"><X className="w-5 h-5" /></button>
            </div>
            {sidebarContent}
          </aside>
        </div>
      )}

      <div className="flex-1 flex flex-col min-w-0">
        <nav className="glass border-b border-white/10 shrink-0">
          <div className="max-w-7xl mx-auto px-3 py-3 flex items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <button onClick={() => setMobileSidebarOpen(true)} className="md:hidden p-2 text-zinc-400 hover:text-white rounded-lg hover:bg-zinc-800 transition-colors">
                <Menu className="w-5 h-5" />
              </button>
              <div className="md:hidden flex items-center gap-2 text-sm font-bold text-zinc-200">
                <Shield className="w-5 h-5 text-emerald-400" />
                Admin
              </div>
              <div className="hidden md:block" />
            </div>
            <button onClick={handleLogout} className="btn-secondary flex items-center gap-2 text-sm px-3 py-2">
              <LogOut className="w-4 h-4" />
              Logout
            </button>
          </div>
        </nav>

        <main className="flex-1 max-w-7xl w-full mx-auto px-3 py-6">
          {loading ? (
            <div className="glass rounded-2xl p-8 flex flex-col items-center justify-center">
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
                  onReactivate={handleReactivateClient}
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
                  onUnfreezeAccount={handleUnfreezeAccount}
                  onCloseAccount={handleCloseAccount}
                  showSensitiveData={showAccountSensitiveData}
                  onRequestSensitiveReveal={() => handleRequestSensitiveReveal({
                    scope: 'ADMIN_ACCOUNTS',
                    targetType: 'ACCOUNT_IBAN',
                    targetId: 'accounts-tab',
                  })}
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
                  onViewDetails={setSelectedTransactionId}
                />
              )}

              {activeTab === 'fraud' && (
                <div className="space-y-6">
                  <FraudAlertsTab />
                  <FraudCommandCenter clients={clients} fraudAlerts={fraudAlertsForCharts} />
                </div>
              )}

              {activeTab === 'audit' && (
                <RevealAuditTab />
              )}
            </>
          )}
        </main>
      </div>

      {showClientDetailsModal && selectedClient && (
        <ClientDetailsModal
          client={selectedClient}
          onClose={() => setShowClientDetailsModal(false)}
          onViewAccounts={handleViewClientAccounts}
          showSensitiveData={showClientSensitiveData}
          onRequestSensitiveReveal={() => handleRequestSensitiveReveal({
            scope: 'ADMIN_CLIENTS',
            targetType: 'CLIENT_CONTACT_INFO',
            targetId: selectedClient.clientId,
          })}
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
          showSensitiveData={showAccountSensitiveData}
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

      <ConfirmDialog
        open={showCloseAccountModal}
        title="Close this account?"
        description={`This will permanently close account ${accountToClose?.accountIban || ''}. Any remaining balance must be transferred first. This action cannot be undone.`}
        confirmLabel="Close Account"
        danger
        onConfirm={confirmCloseAccount}
        onCancel={() => { setShowCloseAccountModal(false); setAccountToClose(null); }}
      />

      <ConfirmDialog
        open={showReactivateModal}
        title="Reactivate this client?"
        description={`This will reactivate client ID ${clientToReactivate?.clientId || ''}. The client will regain full access to their accounts and services.`}
        confirmLabel="Reactivate Client"
        onConfirm={confirmReactivate}
        onCancel={() => { setShowReactivateModal(false); setClientToReactivate(null); }}
      />

      <ConfirmDialog
        open={showUnfreezeAccountModal}
        title="Reactivate this account?"
        description={`This will reactivate account ${accountToUnfreeze?.accountIban || ''}. The account status will change from SUSPENDED back to ACTIVE.`}
        confirmLabel="Reactivate Account"
        onConfirm={confirmUnfreezeAccount}
        onCancel={() => { setShowUnfreezeAccountModal(false); setAccountToUnfreeze(null); }}
      />

      {selectedTransactionId != null && (
        <TransactionDetailsModal
          id={selectedTransactionId}
          maskSensitiveData={false}
          onClose={() => setSelectedTransactionId(null)}
        />
      )}

      {showRevealModal && revealScope && (
        <RevealSensitiveDataModal
          scopeLabel={revealScope.scope === 'ADMIN_ACCOUNTS' ? 'Account details' : 'Client contact info'}
          loading={revealLoading}
          onClose={() => {
            setShowRevealModal(false);
            setRevealScope(null);
          }}
          onConfirm={confirmSensitiveReveal}
        />
      )}
    </div>
  );
}
