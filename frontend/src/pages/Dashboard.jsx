import { useNavigate, useParams } from 'react-router-dom';
import { logout, setup2FA } from '@/services/authService';
import { LogOut, Wallet, Shield, ShieldAlert, CreditCard, LayoutDashboard, ArrowLeftRight, UserCircle, Search } from 'lucide-react';
import { useState, useEffect, useMemo } from 'react';
import { toast } from 'sonner';
import useDashboardData from '@/hooks/useDashboardData';
import useKeyboardShortcuts from '@/hooks/useKeyboardShortcuts';
import { getMyFraudAlerts, resolveMyFraudAlert } from '@/services/fraudService';

import TopUpModal from '../components/TopUpModal';
import CardsPaymentsTab from '../components/CardsPaymentsTab';
import ProfileTab from '../components/ProfileTab';
import TransactionDetailsModal from '../components/TransactionDetailsModal';
import UserAccountsTab from '../components/UserAccountsTab';
import UserTransactionsTab from '../components/UserTransactionsTab';
import UserPaymentsTab from '../components/UserPaymentsTab';
import OnboardingCard from '../components/OnboardingCard';
import SearchOverlay from '../components/SearchOverlay';
import NotificationCenter, { useNotifications } from '../components/NotificationCenter';
import SecurityCenterTab from '../components/SecurityCenterTab';
import TableSkeleton from '../components/TableSkeleton';

import OpenAccountModal from '../components/Modals/OpenAccountModal';
import TransferModal from '../components/Modals/TransferModal';
import SetupTwoFaModal from '../components/Modals/SetupTwoFaModal';

const USER_SECTIONS = ['accounts', 'transactions', 'payments', 'cards', 'security', 'profile'];

export default function Dashboard() {
  const navigate = useNavigate();
  const { section } = useParams();
  const data = useDashboardData();
  const {
    clientId, sub, twoFaEnabled, setTwoFaEnabled,
    accounts, transactions, ledgerTransactions,
    loading, fetchData, totalBalance, activeAccountsCount, monthlyOutgoing,
  } = data;

  const [showBalances, setShowBalances] = useState(true);
  const [showSearch, setShowSearch] = useState(false);
  const { notifications, addNotification, markAllRead, clearAll, unreadCount } = useNotifications();
  
  const [activeModal, setActiveModal] = useState(null);
  const [selectedAccount, setSelectedAccount] = useState(null);
  const [twoFaSetup, setTwoFaSetup] = useState(null);
  const [topUpAccount, setTopUpAccount] = useState(null);
  const [selectedTransactionId, setSelectedTransactionId] = useState(null);
  const [fraudAlerts, setFraudAlerts] = useState([]);
  const [fraudAlertsLoading, setFraudAlertsLoading] = useState(false);
  const [fraudAlertsError, setFraudAlertsError] = useState('');

  // Transaction filtering
  const [showFilters, setShowFilters] = useState(false);
  const [selectedAccountFilter, setSelectedAccountFilter] = useState('all');
  const [filters, setFilters] = useState({ type: 'all', sign: 'all', dateFrom: '', dateTo: '', minAmount: '', maxAmount: '' });
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(10);

  const mainTab = USER_SECTIONS.includes(section) ? section : 'accounts';

  const handleSetup2FA = async () => {
    try {
      const data = await setup2FA();
      setTwoFaSetup(data);
      setActiveModal('2fa');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to setup 2FA');
    }
  };

  const handleLogout = () => { logout(); navigate('/login'); };

  const formatCurrency = (amount, currency) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: currency || 'USD' }).format(amount);

  const formatDate = (dateString) =>
    new Date(dateString).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });

  const fetchFraudAlerts = async () => {
    if (!clientId) return;
    setFraudAlertsLoading(true);
    setFraudAlertsError('');
    try {
      const result = await getMyFraudAlerts(0, 20);
      setFraudAlerts(Array.isArray(result?.content) ? result.content : []);
    } catch (err) {
      setFraudAlerts([]);
      setFraudAlertsError(err?.response?.data?.message || err?.message || 'Failed to load security alerts');
    } finally {
      setFraudAlertsLoading(false);
    }
  };

  // Filtering logic
  const getFilteredTransactions = () => {
    let filtered = [...ledgerTransactions];
    if (selectedAccountFilter !== 'all') {
      const selectedAccount = accounts.find((account) => account.iban === selectedAccountFilter);
      const selectedAccountId = selectedAccount?.id;
      const selectedCurrency = selectedAccount?.currencyCode;

      filtered = filtered.filter((tx) => {
        const txIban = tx.accountIban || tx.iban || tx.senderIban || tx.receiverIban || tx.fromIban || tx.toIban;
        if (txIban) return txIban === selectedAccountFilter;

        const txAccountId = tx.accountId ?? tx.sourceAccountId ?? tx.originAccountId;
        if (selectedAccountId != null && txAccountId != null) {
          return String(txAccountId) === String(selectedAccountId);
        }

        const txCurrency = tx.currencyCode || tx.originalCurrencyCode;
        if (selectedCurrency && txCurrency) {
          return String(txCurrency).toUpperCase() === String(selectedCurrency).toUpperCase();
        }

        return false;
      });
    }
    if (filters.type !== 'all') filtered = filtered.filter(tx => tx.transactionTypeName === filters.type);
    if (filters.sign !== 'all') filtered = filtered.filter(tx => tx.sign === filters.sign);
    if (filters.dateFrom) { const d = new Date(filters.dateFrom); d.setHours(0,0,0,0); filtered = filtered.filter(tx => new Date(tx.transactionDate) >= d); }
    if (filters.dateTo) { const d = new Date(filters.dateTo); d.setHours(23,59,59,999); filtered = filtered.filter(tx => new Date(tx.transactionDate) <= d); }
    if (filters.minAmount !== '') filtered = filtered.filter(tx => tx.amount >= parseFloat(filters.minAmount));
    if (filters.maxAmount !== '') filtered = filtered.filter(tx => tx.amount <= parseFloat(filters.maxAmount));
    return filtered;
  };

  const getTransactionTypes = () => Array.from(new Set(ledgerTransactions.map(tx => tx.transactionTypeName))).sort();

  const filteredTransactions = getFilteredTransactions();
  const latestActivity = ledgerTransactions.slice(0, 5);
  const totalPages = Math.ceil(filteredTransactions.length / itemsPerPage);
  const paginatedTransactions = filteredTransactions.slice((currentPage - 1) * itemsPerPage, currentPage * itemsPerPage);

  useEffect(() => { setCurrentPage(1); }, [filters, itemsPerPage, selectedAccountFilter]);

  const handleFilterChange = (key, value) => setFilters(prev => ({ ...prev, [key]: value }));
  const resetFilters = () => { setSelectedAccountFilter('all'); setFilters({ type: 'all', sign: 'all', dateFrom: '', dateTo: '', minAmount: '', maxAmount: '' }); };
  const goToPage = (page) => { if (page >= 1 && page <= totalPages) setCurrentPage(page); };

  const handleAnalyticsExpenseTypeSelect = (typeLabel) => {
    if (!typeLabel) return;
    const matchingType = getTransactionTypes().find((t) => String(t).toLowerCase() === String(typeLabel).toLowerCase());
    setFilters((prev) => ({ ...prev, type: matchingType || prev.type, sign: '-' }));
    navigate('/dashboard/transactions');
  };

  useEffect(() => { if (!section || !USER_SECTIONS.includes(section)) navigate('/dashboard/accounts', { replace: true }); }, [section, navigate]);

  useEffect(() => {
    if (clientId) fetchFraudAlerts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clientId]);

  const kbShortcuts = useMemo(() => [
    { key: 'k', ctrl: true, action: () => setShowSearch(true) },
    { key: '1', ctrl: false, action: () => navigate('/dashboard/accounts') },
    { key: '2', ctrl: false, action: () => navigate('/dashboard/transactions') },
    { key: '3', ctrl: false, action: () => navigate('/dashboard/payments') },
    { key: '4', ctrl: false, action: () => navigate('/dashboard/cards') },
    { key: '5', ctrl: false, action: () => navigate('/dashboard/security') },
    { key: '6', ctrl: false, action: () => navigate('/dashboard/profile') },
  ], [navigate, setShowSearch]);
  useKeyboardShortcuts(kbShortcuts);

  const contentWidthClass = mainTab === 'accounts' || mainTab === 'transactions' ? 'max-w-6xl' : 'max-w-5xl';
  const USER_NAV = [
    { id: 'accounts', label: 'Accounts', icon: LayoutDashboard },
    { id: 'transactions', label: 'Transactions', icon: ArrowLeftRight },
    { id: 'payments', label: 'Payments', icon: CreditCard },
    { id: 'cards', label: 'Cards', icon: Wallet },
    { id: 'security', label: 'Security', icon: ShieldAlert },
    { id: 'profile', label: 'Profile', icon: UserCircle },
  ];

  const pendingFraudAlerts = fraudAlerts.filter((alert) => !alert.userResolution || alert.userResolution === 'PENDING');

  const handleResolveFraudAlert = async (alert, resolution) => {
    try {
      await resolveMyFraudAlert(alert.id, resolution, resolution === 'LEGITIMATE' ? 'Confirmed by client' : 'Client reported fraud');
      toast.success(
        resolution === 'LEGITIMATE'
          ? 'Alert marked legitimate. Security review updated.'
          : 'Alert reported as fraud. Protective action sent.'
      );
      await Promise.all([fetchFraudAlerts(), fetchData()]);
    } catch (err) {
      toast.error(err?.response?.data?.message || err?.message || 'Failed to resolve alert');
    }
  };

  return (
    <div className="min-h-screen bg-slate-950">
      <nav className="glass border-b border-white/10">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 py-4 flex items-center justify-between gap-3">
          <div className="flex items-center gap-3 min-w-0">
            <div className="w-10 h-10 bg-emerald-500/20 rounded-xl flex items-center justify-center shrink-0">
              <Wallet className="w-5 h-5 text-emerald-400" />
            </div>
            <h1 className="text-lg sm:text-xl font-bold truncate">CashTactics Dashboard</h1>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <button onClick={() => setShowSearch(true)} className="p-2 text-zinc-400 hover:text-white rounded-lg hover:bg-zinc-800 transition-colors" title="Search (Ctrl+K)">
              <Search className="w-5 h-5" />
            </button>
            <NotificationCenter notifications={notifications} unreadCount={unreadCount} onMarkAllRead={markAllRead} onClearAll={clearAll} />
            {sub && (
              <div className="hidden sm:flex items-center justify-center w-8 h-8 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 font-bold text-sm" title={sub}>
                {sub.substring(0, 2).toUpperCase()}
              </div>
            )}
            {!twoFaEnabled && (
              <button onClick={handleSetup2FA} className="btn-secondary flex items-center gap-2 text-sm px-3 py-2 md:text-base md:px-4">
                <Shield className="w-4 h-4 shrink-0" />
                <span className="hidden sm:inline">Enable 2FA</span>
              </button>
            )}
            <button onClick={handleLogout} className="btn-secondary flex items-center gap-2 text-sm px-3 py-2 md:text-base md:px-4">
              <LogOut className="w-4 h-4 shrink-0" />
              <span className="hidden sm:inline">Logout</span>
            </button>
          </div>
        </div>
      </nav>

      <div className={`${contentWidthClass} mx-auto px-6 py-8`}>
        {clientId && !loading && (
          <div className="border-b border-gray-700 mb-6">
            <div className="flex gap-2 sm:gap-3 overflow-x-auto pb-2">
              {USER_NAV.map((item) => {
                const Icon = item.icon;
                const active = mainTab === item.id;
                return (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => navigate(`/dashboard/${item.id}`)}
                    className={`flex items-center gap-2 px-3 py-2 rounded-xl text-sm whitespace-nowrap border transition-colors ${
                      active ? 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30' : 'text-zinc-400 border-transparent hover:text-zinc-200 hover:bg-zinc-800/70'
                    }`}
                  >
                    <Icon className="w-4 h-4" />
                    {item.label}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {mainTab !== 'security' && pendingFraudAlerts.length > 0 && (
          <div className="mb-6 glass rounded-2xl border border-amber-500/15 bg-amber-500/8 p-4">
            <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-3">
              <div>
                <p className="text-amber-300 font-semibold">Security attention needed</p>
                <p className="text-sm text-zinc-400">
                  You have {pendingFraudAlerts.length} unresolved fraud alert{pendingFraudAlerts.length !== 1 ? 's' : ''}. Review them in Security Center.
                </p>
              </div>
              <button type="button" onClick={() => navigate('/dashboard/security')} className="btn-primary flex items-center gap-2 self-start md:self-auto">
                <ShieldAlert className="w-4 h-4" />
                Review alerts
              </button>
            </div>
          </div>
        )}

        {loading ? (
          <div className="space-y-6 animate-fade-in">
            <div className="glass rounded-2xl p-6"><div className="skeleton h-8 w-48 mb-2" /><div className="skeleton h-4 w-72" /></div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {[1, 2, 3].map((i) => (<div key={i} className="glass rounded-2xl p-5"><div className="skeleton h-3 w-32 mb-4" /><div className="skeleton h-8 w-24" /></div>))}
            </div>
            <div className="mt-8">
              <TableSkeleton />
            </div>
          </div>
        ) : mainTab === 'payments' && clientId ? (
          <UserPaymentsTab accounts={accounts} onSuccess={(msg) => toast.success(msg)} onError={(msg) => toast.error(msg)} onRefresh={fetchData} />
        ) : mainTab === 'cards' && clientId ? (
          <div className="space-y-6">
            <div className="glass rounded-2xl p-6"><h2 className="text-2xl font-bold">Cards</h2><p className="text-zinc-500 text-sm mt-1">Manage funding methods and top up accounts.</p></div>
            <CardsPaymentsTab clientId={clientId} accounts={accounts} onRefresh={fetchData} onOpenTopUp={setTopUpAccount} />
          </div>
        ) : mainTab === 'profile' && clientId ? (
          <div className="space-y-6">
            <div className="glass rounded-2xl p-6"><h2 className="text-2xl font-bold">Profile</h2><p className="text-zinc-500 text-sm mt-1">Your personal details and account profile information.</p></div>
            <ProfileTab />
          </div>
        ) : mainTab === 'security' ? (
          <SecurityCenterTab
            alerts={fraudAlerts}
            loading={fraudAlertsLoading}
            error={fraudAlertsError}
            onRefresh={fetchFraudAlerts}
            onResolve={handleResolveFraudAlert}
          />
        ) : mainTab === 'transactions' ? (
          <UserTransactionsTab
            accounts={accounts} selectedAccountFilter={selectedAccountFilter} setSelectedAccountFilter={setSelectedAccountFilter}
            filters={filters} handleFilterChange={handleFilterChange} getTransactionTypes={getTransactionTypes}
            showFilters={showFilters} setShowFilters={setShowFilters} filteredTransactions={filteredTransactions}
            resetFilters={resetFilters} transactions={transactions} paginatedTransactions={paginatedTransactions}
            formatDate={formatDate} formatCurrency={formatCurrency} setSelectedTransactionId={setSelectedTransactionId}
            totalPages={totalPages} itemsPerPage={itemsPerPage} setItemsPerPage={setItemsPerPage}
            currentPage={currentPage} goToPage={goToPage}
          />
        ) : (
          <>
            <OnboardingCard accounts={accounts} onAction={setActiveModal} onNavigate={navigate} />
            <UserAccountsTab
              accounts={accounts} transactions={ledgerTransactions} latestActivity={latestActivity}
              showBalances={showBalances} setShowBalances={setShowBalances} setActiveModal={setActiveModal}
              setSelectedAccount={setSelectedAccount} setTopUpAccount={setTopUpAccount}
              onViewAllActivity={() => navigate('/dashboard/transactions')} formatCurrency={formatCurrency}
              totalBalance={totalBalance} activeAccountsCount={activeAccountsCount}
              monthlyOutgoing={monthlyOutgoing} onExpenseTypeSelect={handleAnalyticsExpenseTypeSelect}
            />
          </>
        )}
      </div>

      {topUpAccount && (
        <TopUpModal
          account={topUpAccount}
          onClose={() => setTopUpAccount(null)}
          onSuccess={() => { toast.success('Top-up completed successfully.'); fetchData(); }}
          onError={(msg) => toast.error(msg)}
        />
      )}

      {activeModal === 'openAccount' && (
        <OpenAccountModal clientId={clientId} onClose={() => setActiveModal(null)} onSuccess={fetchData} />
      )}

      {activeModal === 'transfer' && selectedAccount && (
        <TransferModal selectedAccount={selectedAccount} onClose={() => setActiveModal(null)} onSuccess={fetchData} />
      )}

      {activeModal === '2fa' && twoFaSetup && (
        <SetupTwoFaModal
          twoFaSetup={twoFaSetup}
          onClose={() => setActiveModal(null)}
          onSuccess={() => { setTwoFaEnabled(true); }}
        />
      )}

      {selectedTransactionId != null && (
        <TransactionDetailsModal id={selectedTransactionId} onClose={() => setSelectedTransactionId(null)} />
      )}

      {showSearch && (
        <SearchOverlay
          accounts={accounts}
          transactions={ledgerTransactions}
          onNavigate={(path) => { navigate(path); setShowSearch(false); }}
          onSelectTransaction={setSelectedTransactionId}
          onClose={() => setShowSearch(false)}
        />
      )}
    </div>
  );
}
