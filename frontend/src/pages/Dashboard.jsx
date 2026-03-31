import { useNavigate, useParams } from 'react-router-dom';
import { logout, setup2FA, confirm2FA } from '@/services/authService';
import { openAccount, transfer } from '@/services/accountService';
import { LogOut, Wallet, Shield, CreditCard, LayoutDashboard, ArrowLeftRight, UserCircle, Search } from 'lucide-react';
import { useState, useEffect, useMemo } from 'react';
import QRCode from 'qrcode';
import useDashboardData from '@/hooks/useDashboardData';
import useKeyboardShortcuts from '@/hooks/useKeyboardShortcuts';
import TopUpModal from '../components/TopUpModal';
import CardsPaymentsTab from '../components/CardsPaymentsTab';
import ProfileTab from '../components/ProfileTab';
import TransactionDetailsModal from '../components/TransactionDetailsModal';
import UserAccountsTab from '../components/UserAccountsTab';
import UserTransactionsTab from '../components/UserTransactionsTab';
import UserPaymentsTab from '../components/UserPaymentsTab';
import OnboardingCard from '../components/OnboardingCard';
import TransferConfirmation from '../components/TransferConfirmation';
import SearchOverlay from '../components/SearchOverlay';
import NotificationCenter, { useNotifications } from '../components/NotificationCenter';
import ThemeToggle from '../components/ThemeToggle';

const USER_SECTIONS = ['accounts', 'transactions', 'payments', 'cards', 'profile'];

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
  const [qrCodeDataUrl, setQrCodeDataUrl] = useState(null);
  const [twoFaCode, setTwoFaCode] = useState('');
  const [newAccountCurrency, setNewAccountCurrency] = useState('EUR');
  const [transferForm, setTransferForm] = useState({ toIban: '', amount: '' });
  const [transferStep, setTransferStep] = useState('form');
  const [transferLoading, setTransferLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [topUpAccount, setTopUpAccount] = useState(null);
  const [selectedTransactionId, setSelectedTransactionId] = useState(null);

  // Transaction filtering (kept in this scope for cross-tab analytics linking)
  const [showFilters, setShowFilters] = useState(false);
  const [selectedAccountFilter, setSelectedAccountFilter] = useState('all');
  const [filters, setFilters] = useState({ type: 'all', sign: 'all', dateFrom: '', dateTo: '', minAmount: '', maxAmount: '' });
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(10);

  const mainTab = USER_SECTIONS.includes(section) ? section : 'accounts';

  useEffect(() => {
    if (twoFaSetup?.otpauthUrl) {
      QRCode.toDataURL(twoFaSetup.otpauthUrl)
        .then(url => setQrCodeDataUrl(url))
        .catch(err => console.error('Failed to generate QR code:', err));
    }
  }, [twoFaSetup]);

  const handleOpenAccount = async () => {
    try {
      await openAccount(clientId, newAccountCurrency);
      setSuccess('Account opened successfully!');
      setActiveModal(null);
      fetchData();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to open account');
    }
  };

  const handleTransferSubmit = () => {
    if (!selectedAccount || !transferForm.toIban.trim() || !transferForm.amount) {
      setError('Please fill all transfer fields.');
      return;
    }
    setTransferStep('confirm');
  };

  const handleTransferConfirm = async () => {
    setTransferLoading(true);
    try {
      await transfer(selectedAccount.iban, transferForm.toIban, parseFloat(transferForm.amount));
      setSuccess(`Transferred ${transferForm.amount} ${selectedAccount.currencyCode} successfully!`);
      setActiveModal(null);
      setTransferForm({ toIban: '', amount: '' });
      setTransferStep('form');
      fetchData();
    } catch (err) {
      setError(err.response?.data?.message || 'Transfer failed');
      setTransferStep('form');
    } finally {
      setTransferLoading(false);
    }
  };

  const handleSetup2FA = async () => {
    try {
      const data = await setup2FA();
      setTwoFaSetup(data);
      setActiveModal('2fa');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to setup 2FA');
    }
  };

  const handleConfirm2FA = async () => {
    try {
      await confirm2FA(twoFaCode);
      setSuccess('2FA enabled successfully!');
      setActiveModal(null);
      setTwoFaEnabled(true);
      setTwoFaCode('');
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid code');
    }
  };

  const handleLogout = () => { logout(); navigate('/login'); };

  const formatCurrency = (amount, currency) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: currency || 'USD' }).format(amount);

  const formatDate = (dateString) =>
    new Date(dateString).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });

  // Filtering logic
  const getFilteredTransactions = () => {
    let filtered = [...ledgerTransactions];
    if (selectedAccountFilter !== 'all') filtered = filtered.filter(tx => tx.accountIban === selectedAccountFilter);
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
  useEffect(() => { if (error) { addNotification(error, 'error'); const t = setTimeout(() => setError(''), 4500); return () => clearTimeout(t); } }, [error, addNotification]);
  useEffect(() => { if (success) { addNotification(success, 'success'); const t = setTimeout(() => setSuccess(''), 3500); return () => clearTimeout(t); } }, [success, addNotification]);

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

  const kbShortcuts = useMemo(() => [
    { key: 'k', ctrl: true, action: () => setShowSearch(true) },
    { key: '1', ctrl: false, action: () => navigate('/dashboard/accounts') },
    { key: '2', ctrl: false, action: () => navigate('/dashboard/transactions') },
    { key: '3', ctrl: false, action: () => navigate('/dashboard/payments') },
    { key: '4', ctrl: false, action: () => navigate('/dashboard/cards') },
    { key: '5', ctrl: false, action: () => navigate('/dashboard/profile') },
  ], [navigate]);
  useKeyboardShortcuts(kbShortcuts);

  const contentWidthClass = mainTab === 'accounts' || mainTab === 'transactions' ? 'max-w-6xl' : 'max-w-5xl';
  const USER_NAV = [
    { id: 'accounts', label: 'Accounts', icon: LayoutDashboard },
    { id: 'transactions', label: 'Transactions', icon: ArrowLeftRight },
    { id: 'payments', label: 'Payments', icon: CreditCard },
    { id: 'cards', label: 'Cards', icon: Wallet },
    { id: 'profile', label: 'Profile', icon: UserCircle },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-zinc-900 to-slate-950">
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
            <ThemeToggle />
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
        {(error || success) && (
          <div className="fixed top-4 right-4 z-[60] space-y-2 w-[min(92vw,360px)]">
            {error && (
              <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-xl text-red-300 text-sm shadow-lg animate-slide-in-right flex items-start justify-between gap-2">
                <span>{error}</span>
                <button onClick={() => setError('')} className="text-red-400 hover:text-red-200 mt-0.5 shrink-0">&times;</button>
              </div>
            )}
            {success && (
              <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-emerald-300 text-sm shadow-lg animate-slide-in-right flex items-start justify-between gap-2">
                <span>{success}</span>
                <button onClick={() => setSuccess('')} className="text-emerald-400 hover:text-emerald-200 mt-0.5 shrink-0">&times;</button>
              </div>
            )}
          </div>
        )}

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

        {loading ? (
          <div className="space-y-6 animate-fade-in">
            <div className="glass rounded-2xl p-6"><div className="skeleton h-8 w-48 mb-2" /><div className="skeleton h-4 w-72" /></div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {[1, 2, 3].map((i) => (<div key={i} className="glass rounded-2xl p-5"><div className="skeleton h-3 w-32 mb-4" /><div className="skeleton h-8 w-24" /></div>))}
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {[1, 2, 3].map((i) => <div key={i} className="skeleton h-48 w-full" />)}
            </div>
          </div>
        ) : mainTab === 'payments' && clientId ? (
          <UserPaymentsTab accounts={accounts} onSuccess={setSuccess} onError={setError} onRefresh={fetchData} />
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

      {/* Top-up modal */}
      {topUpAccount && (
        <TopUpModal
          account={topUpAccount}
          onClose={() => setTopUpAccount(null)}
          onSuccess={() => { setSuccess('Top-up submitted. Balance updates when Stripe confirms the payment.'); fetchData(); }}
        />
      )}

      {/* Action modals */}
      {activeModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50" onClick={() => { setActiveModal(null); setTransferStep('form'); }}>
          <div className="glass rounded-2xl p-6 max-w-md w-full animate-fade-in" onClick={(e) => e.stopPropagation()}>
            {activeModal === 'openAccount' && (
              <>
                <h3 className="text-xl font-bold mb-4">Open New Account</h3>
                <label className="block text-sm font-medium text-zinc-400 mb-2">Currency</label>
                <select value={newAccountCurrency} onChange={(e) => setNewAccountCurrency(e.target.value)} className="input-field mb-4">
                  <option value="EUR">EUR - Euro</option>
                  <option value="USD">USD - US Dollar</option>
                  <option value="RON">RON - Romanian Leu</option>
                  <option value="GBP">GBP - British Pound</option>
                </select>
                <div className="flex gap-3">
                  <button onClick={() => setActiveModal(null)} className="btn-secondary flex-1">Cancel</button>
                  <button onClick={handleOpenAccount} className="btn-primary flex-1">Open Account</button>
                </div>
              </>
            )}

            {activeModal === 'transfer' && selectedAccount && (
              transferStep === 'confirm' ? (
                <TransferConfirmation
                  fromAccount={selectedAccount}
                  toIban={transferForm.toIban}
                  amount={parseFloat(transferForm.amount)}
                  loading={transferLoading}
                  onConfirm={handleTransferConfirm}
                  onBack={() => setTransferStep('form')}
                />
              ) : (
                <>
                  <h3 className="text-xl font-bold mb-4">Transfer from {selectedAccount.iban}</h3>
                  <label className="block text-sm font-medium text-zinc-400 mb-2">To IBAN</label>
                  <input type="text" value={transferForm.toIban} onChange={(e) => setTransferForm({...transferForm, toIban: e.target.value})} className="input-field mb-4" placeholder="RO49BANK0000000002EUR" />
                  <label className="block text-sm font-medium text-zinc-400 mb-2">Amount ({selectedAccount.currencyCode})</label>
                  <input type="number" value={transferForm.amount} onChange={(e) => setTransferForm({...transferForm, amount: e.target.value})} className="input-field mb-4" placeholder="0.00" step="0.01" min="0" />
                  <div className="flex gap-3">
                    <button onClick={() => setActiveModal(null)} className="btn-secondary flex-1">Cancel</button>
                    <button onClick={handleTransferSubmit} className="btn-primary flex-1">Review Transfer</button>
                  </div>
                </>
              )
            )}

            {activeModal === '2fa' && twoFaSetup && (
              <>
                <h3 className="text-xl font-bold mb-4">Setup Two-Factor Authentication</h3>
                <p className="text-zinc-400 text-sm mb-4">Scan this QR code with your authenticator app (Google Authenticator, Authy, etc.)</p>
                <div className="bg-white p-4 rounded-xl mb-4 flex justify-center">
                  {qrCodeDataUrl && <img src={qrCodeDataUrl} alt="2FA QR Code" className="w-48 h-48" />}
                </div>
                <p className="text-zinc-400 text-xs mb-2">Or enter this secret manually:</p>
                <p className="font-mono text-sm bg-zinc-800 p-2 rounded mb-4">{twoFaSetup.secret}</p>
                <label className="block text-sm font-medium text-zinc-400 mb-2">Enter verification code</label>
                <input type="text" value={twoFaCode} onChange={(e) => setTwoFaCode(e.target.value)} className="input-field mb-4" placeholder="000000" maxLength={6} />
                <div className="flex gap-3">
                  <button onClick={() => setActiveModal(null)} className="btn-secondary flex-1">Cancel</button>
                  <button onClick={handleConfirm2FA} className="btn-primary flex-1">Confirm</button>
                </div>
              </>
            )}
          </div>
        </div>
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
