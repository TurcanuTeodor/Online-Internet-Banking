import { useNavigate, useParams } from 'react-router-dom';
import { logout } from '../../services/authService';
import { LogOut, Wallet, Shield, CreditCard, LayoutDashboard, ArrowLeftRight, UserCircle } from 'lucide-react';
import { useState, useEffect } from 'react';
import { jwtDecode } from 'jwt-decode';
import QRCode from 'qrcode';
import { getAccountsByClient, openAccount, transfer } from '../../services/accountService';
import { getTransactionsByClient } from '../../services/transactionService';
import { getPaymentHistory } from '../../services/paymentService';
import { setup2FA, confirm2FA } from '../../services/authService';
import TopUpModal from '../components/TopUpModal';
import CardsPaymentsTab from '../components/CardsPaymentsTab';
import ProfileTab from '../components/ProfileTab';
import TransactionDetailsModal from '../components/TransactionDetailsModal';
import UserAccountsTab from '../components/UserAccountsTab';
import UserTransactionsTab from '../components/UserTransactionsTab';
import UserPaymentsTab from '../components/UserPaymentsTab';

const USER_SECTIONS = ['accounts', 'transactions', 'payments', 'cards', 'profile'];

export default function Dashboard() {
  const navigate = useNavigate();
  const { section } = useParams();
  const [clientId, setClientId] = useState(null);
  const [accounts, setAccounts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showBalances, setShowBalances] = useState(true);
  const [activeModal, setActiveModal] = useState(null); // 'openAccount', 'transfer', '2fa'
  const [selectedAccount, setSelectedAccount] = useState(null);
  const [twoFaSetup, setTwoFaSetup] = useState(null);
  const [qrCodeDataUrl, setQrCodeDataUrl] = useState(null);
  const [twoFaEnabled, setTwoFaEnabled] = useState(false);
  const [sub, setSub] = useState('');
  
  // Form states
  const [newAccountCurrency, setNewAccountCurrency] = useState('EUR');
  const [transferForm, setTransferForm] = useState({ toIban: '', amount: '' });
  const [twoFaCode, setTwoFaCode] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Filter and Pagination states
  const [showFilters, setShowFilters] = useState(false);
  const [selectedAccountFilter, setSelectedAccountFilter] = useState('all');
  const [filters, setFilters] = useState({
    type: 'all',
    sign: 'all',
    dateFrom: '',
    dateTo: '',
    minAmount: '',
    maxAmount: ''
  });
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(10);
  const [topUpAccount, setTopUpAccount] = useState(null);
  const [selectedTransactionId, setSelectedTransactionId] = useState(null);

  const mainTab = USER_SECTIONS.includes(section) ? section : 'accounts';

  useEffect(() => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      try {
        const decoded = jwtDecode(token);
        setClientId(decoded.clientId);
        setTwoFaEnabled(decoded['2fa_verified'] === true);
        setSub(decoded.sub || '');
      } catch (error) {
        console.error('Failed to decode token:', error);
      }
    }
  }, []);

  useEffect(() => {
    if (clientId) {
      fetchData();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clientId]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [accountsData, transactionsData, paymentsData] = await Promise.all([
        getAccountsByClient(clientId),
        getTransactionsByClient(clientId),
        getPaymentHistory(clientId),
      ]);
      setAccounts(accountsData);
      setTransactions(transactionsData); // Show all transactions, filter and paginate client-side
      setPayments(Array.isArray(paymentsData) ? paymentsData : []);
    } catch (err) {
      console.error('Error fetching data:', err);
      setError('Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  // Generate QR code when 2FA setup data is received
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

  const handleTransfer = async () => {
    try {
      await transfer(selectedAccount.iban, transferForm.toIban, parseFloat(transferForm.amount));
      setSuccess(`Transferred ${transferForm.amount} ${selectedAccount.currencyCode} successfully!`);
      setActiveModal(null);
      setTransferForm({ toIban: '', amount: '' });
      fetchData();
    } catch (err) {
      setError(err.response?.data?.message || 'Transfer failed');
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

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const formatCurrency = (amount, currency) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency || 'USD'
    }).format(amount);
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const normalizedPaymentEntries = (payments || [])
    .filter((p) => {
      const status = String(p?.status || '').toUpperCase();
      const type = String(p?.paymentType || p?.type || '').toUpperCase();
      const successLike = ['SUCCEEDED', 'SUCCESS', 'COMPLETED', 'PAID'].includes(status);
      const topUpLike = type.includes('TOP') || type.includes('DEPOSIT') || type.includes('FUND');
      return successLike && topUpLike;
    })
    .map((p) => {
    const rawType = p.paymentType || p.type || '';
    const typeText = String(rawType).toUpperCase();
    const isTopUp = typeText.includes('TOP') || typeText.includes('DEPOSIT') || typeText.includes('FUND');
    return {
      id: `payment-${p.id}`,
      transactionDate: p.createdAt || p.updatedAt,
      transactionTypeName: rawType || 'PAYMENT',
      transactionTypeCode: rawType || 'PAYMENT',
      displayLabel: p.merchant || p.provider || (isTopUp ? 'Card Top-up' : 'Payment'),
      accountIban: p.accountIban || null,
      currencyCode: p.currencyCode || 'EUR',
      amount: Number(p.amount || 0),
      sign: isTopUp ? '+' : '-',
      paymentId: p.id,
      source: 'payment',
      status: p.status,
    };
  });

  const ledgerTransactions = [...(transactions || []), ...normalizedPaymentEntries]
    .sort((a, b) => new Date(b.transactionDate || 0).getTime() - new Date(a.transactionDate || 0).getTime());

  // Filter transactions based on selected filters
  const getFilteredTransactions = () => {
    let filtered = [...ledgerTransactions];

    // Filter by selected account
    if (selectedAccountFilter !== 'all') {
      filtered = filtered.filter(tx => tx.accountIban === selectedAccountFilter);
    }

    // Filter by type
    if (filters.type !== 'all') {
      filtered = filtered.filter(tx => tx.transactionTypeName === filters.type);
    }

    // Filter by sign (credit/debit)
    if (filters.sign !== 'all') {
      filtered = filtered.filter(tx => tx.sign === filters.sign);
    }

    // Filter by date range
    if (filters.dateFrom) {
      const fromDate = new Date(filters.dateFrom);
      fromDate.setHours(0, 0, 0, 0);
      filtered = filtered.filter(tx => new Date(tx.transactionDate) >= fromDate);
    }
    if (filters.dateTo) {
      const toDate = new Date(filters.dateTo);
      toDate.setHours(23, 59, 59, 999); // Include entire day
      filtered = filtered.filter(tx => new Date(tx.transactionDate) <= toDate);
    }

    // Filter by amount range
    if (filters.minAmount !== '') {
      filtered = filtered.filter(tx => tx.amount >= parseFloat(filters.minAmount));
    }
    if (filters.maxAmount !== '') {
      filtered = filtered.filter(tx => tx.amount <= parseFloat(filters.maxAmount));
    }

    return filtered;
  };

  // Get unique transaction types for filter dropdown
  const getTransactionTypes = () => {
    const types = new Set(ledgerTransactions.map(tx => tx.transactionTypeName));
    return Array.from(types).sort();
  };

  // Apply filtering and pagination
  const filteredTransactions = getFilteredTransactions();
  const latestActivity = ledgerTransactions.slice(0, 5);
  const totalPages = Math.ceil(filteredTransactions.length / itemsPerPage);
  const paginatedTransactions = filteredTransactions.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );
  const totalBalance = accounts.reduce((sum, account) => sum + Number(account.balance || 0), 0);
  const activeAccountsCount = accounts.filter((account) => account.status === 'ACTIVE').length;
  const monthKey = new Date().toISOString().slice(0, 7);
  const monthlyOutgoing = ledgerTransactions
    .filter((tx) => tx.sign === '-' && String(tx.transactionDate || '').startsWith(monthKey))
    .reduce((sum, tx) => sum + Number(tx.amount || 0), 0);

  // Reset to page 1 when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [filters, itemsPerPage, selectedAccountFilter]);

  useEffect(() => {
    if (!error) return;
    const t = setTimeout(() => setError(''), 4500);
    return () => clearTimeout(t);
  }, [error]);

  useEffect(() => {
    if (!success) return;
    const t = setTimeout(() => setSuccess(''), 3500);
    return () => clearTimeout(t);
  }, [success]);

  const handleFilterChange = (key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }));
  };

  const resetFilters = () => {
    setSelectedAccountFilter('all');
    setFilters({
      type: 'all',
      sign: 'all',
      dateFrom: '',
      dateTo: '',
      minAmount: '',
      maxAmount: ''
    });
  };

  const goToPage = (page) => {
    if (page >= 1 && page <= totalPages) {
      setCurrentPage(page);
    }
  };

  const handleAnalyticsExpenseTypeSelect = (typeLabel) => {
    if (!typeLabel) return;
    const matchingType = getTransactionTypes().find((t) => String(t).toLowerCase() === String(typeLabel).toLowerCase());
    setFilters((prev) => ({
      ...prev,
      type: matchingType || prev.type,
      sign: '-',
    }));
    navigate('/dashboard/transactions');
  };

  const contentWidthClass = mainTab === 'accounts' || mainTab === 'transactions' ? 'max-w-6xl' : 'max-w-5xl';
  const USER_NAV = [
    { id: 'accounts', label: 'Accounts', icon: LayoutDashboard },
    { id: 'transactions', label: 'Transactions', icon: ArrowLeftRight },
    { id: 'payments', label: 'Payments', icon: CreditCard },
    { id: 'cards', label: 'Cards', icon: Wallet },
    { id: 'profile', label: 'Profile', icon: UserCircle },
  ];

  useEffect(() => {
    if (!section || !USER_SECTIONS.includes(section)) {
      navigate('/dashboard/accounts', { replace: true });
    }
  }, [section, navigate]);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-zinc-900 to-slate-950">
      {/* Nav */}
      <nav className="glass border-b border-white/10">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 py-4 flex items-center justify-between gap-3">
          <div className="flex items-center gap-3 min-w-0">
            <div className="w-10 h-10 bg-emerald-500/20 rounded-xl flex items-center justify-center shrink-0">
              <Wallet className="w-5 h-5 text-emerald-400" />
            </div>
            <h1 className="text-lg sm:text-xl font-bold truncate">CashTactics Dashboard</h1>
          </div>
          <div className="flex items-center gap-3 shrink-0">
            {sub && (
              <div className="hidden sm:flex items-center justify-center w-8 h-8 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 font-bold text-sm" title={sub}>
                {sub.substring(0, 2).toUpperCase()}
              </div>
            )}
            {!twoFaEnabled && (
              <button
                onClick={handleSetup2FA}
                className="btn-secondary flex items-center gap-2 text-sm px-3 py-2 md:text-base md:px-4"
              >
                <Shield className="w-4 h-4 shrink-0" />
                <span className="hidden sm:inline">Enable 2FA</span>
              </button>
            )}
            <button
              onClick={handleLogout}
              className="btn-secondary flex items-center gap-2 text-sm px-3 py-2 md:text-base md:px-4"
            >
              <LogOut className="w-4 h-4 shrink-0" />
              <span className="hidden sm:inline">Logout</span>
            </button>
          </div>
        </div>
      </nav>

      <div className={`${contentWidthClass} mx-auto px-6 py-8`}>
        {(error || success) && (
          <div className="fixed top-4 right-4 z-[60] space-y-2 w-[min(92vw,360px)]">
            {error ? (
              <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-xl text-red-300 text-sm shadow-lg animate-slide-in-right flex items-start justify-between gap-2">
                <span>{error}</span>
                <button onClick={() => setError('')} className="text-red-400 hover:text-red-200 mt-0.5 shrink-0">&times;</button>
              </div>
            ) : null}
            {success ? (
              <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-emerald-300 text-sm shadow-lg animate-slide-in-right flex items-start justify-between gap-2">
                <span>{success}</span>
                <button onClick={() => setSuccess('')} className="text-emerald-400 hover:text-emerald-200 mt-0.5 shrink-0">&times;</button>
              </div>
            ) : null}
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
                      active
                        ? 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30'
                        : 'text-zinc-400 border-transparent hover:text-zinc-200 hover:bg-zinc-800/70'
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
            <div className="glass rounded-2xl p-6">
              <div className="skeleton h-8 w-48 mb-2" />
              <div className="skeleton h-4 w-72" />
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {[1, 2, 3].map((i) => (
                <div key={i} className="glass rounded-2xl p-5">
                  <div className="skeleton h-3 w-32 mb-4" />
                  <div className="skeleton h-8 w-24" />
                </div>
              ))}
            </div>
            <div className="flex gap-3 mb-4 mt-8">
              <div className="skeleton h-10 w-32" />
              <div className="skeleton h-10 w-40" />
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {[1, 2, 3].map((i) => <div key={i} className="skeleton h-48 w-full" />)}
            </div>
          </div>
        ) : mainTab === 'payments' && clientId ? (
          <div className="space-y-6">
            <UserPaymentsTab
              accounts={accounts}
              onSuccess={setSuccess}
              onError={setError}
              onRefresh={fetchData}
            />
          </div>
        ) : mainTab === 'cards' && clientId ? (
          <div className="space-y-6">
            <div className="glass rounded-2xl p-6">
              <h2 className="text-2xl font-bold">Cards</h2>
              <p className="text-zinc-500 text-sm mt-1">
                Manage funding methods and top up accounts.
              </p>
            </div>
            <CardsPaymentsTab
              clientId={clientId}
              accounts={accounts}
              onRefresh={fetchData}
              onOpenTopUp={setTopUpAccount}
            />
          </div>
        ) : mainTab === 'profile' && clientId ? (
          <div className="space-y-6">
            <div className="glass rounded-2xl p-6">
              <h2 className="text-2xl font-bold">Profile</h2>
              <p className="text-zinc-500 text-sm mt-1">
                Your personal details and account profile information.
              </p>
            </div>
            <ProfileTab />
          </div>
        ) : mainTab === 'transactions' ? (
          <UserTransactionsTab
            accounts={accounts}
            selectedAccountFilter={selectedAccountFilter}
            setSelectedAccountFilter={setSelectedAccountFilter}
            filters={filters}
            handleFilterChange={handleFilterChange}
            getTransactionTypes={getTransactionTypes}
            showFilters={showFilters}
            setShowFilters={setShowFilters}
            filteredTransactions={filteredTransactions}
            resetFilters={resetFilters}
            transactions={transactions}
            paginatedTransactions={paginatedTransactions}
            formatDate={formatDate}
            formatCurrency={formatCurrency}
            setSelectedTransactionId={setSelectedTransactionId}
            totalPages={totalPages}
            itemsPerPage={itemsPerPage}
            setItemsPerPage={setItemsPerPage}
            currentPage={currentPage}
            goToPage={goToPage}
          />
        ) : (
          <UserAccountsTab
            accounts={accounts}
            transactions={ledgerTransactions}
            latestActivity={latestActivity}
            showBalances={showBalances}
            setShowBalances={setShowBalances}
            setActiveModal={setActiveModal}
            setSelectedAccount={setSelectedAccount}
            setTopUpAccount={setTopUpAccount}
            onViewAllActivity={() => navigate('/dashboard/transactions')}
            formatCurrency={formatCurrency}
            totalBalance={totalBalance}
            activeAccountsCount={activeAccountsCount}
            monthlyOutgoing={monthlyOutgoing}
            onExpenseTypeSelect={handleAnalyticsExpenseTypeSelect}
          />
        )}
      </div>

      {/* Modals */}
      {topUpAccount && (
        <TopUpModal
          account={topUpAccount}
          onClose={() => setTopUpAccount(null)}
          onSuccess={() => {
            setSuccess('Top-up submitted. Balance updates when Stripe confirms the payment (usually a few seconds).');
            fetchData();
          }}
        />
      )}

      {activeModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50" onClick={() => setActiveModal(null)}>
          <div className="glass rounded-2xl p-6 max-w-md w-full animate-fade-in" onClick={(e) => e.stopPropagation()}>
            {activeModal === 'openAccount' && (
              <>
                <h3 className="text-xl font-bold mb-4">Open New Account</h3>
                <label className="block text-sm font-medium text-zinc-400 mb-2">Currency</label>
                <select
                  value={newAccountCurrency}
                  onChange={(e) => setNewAccountCurrency(e.target.value)}
                  className="input-field mb-4"
                >
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
              <>
                <h3 className="text-xl font-bold mb-4">Transfer from {selectedAccount.iban}</h3>
                <label className="block text-sm font-medium text-zinc-400 mb-2">To IBAN</label>
                <input
                  type="text"
                  value={transferForm.toIban}
                  onChange={(e) => setTransferForm({...transferForm, toIban: e.target.value})}
                  className="input-field mb-4"
                  placeholder="RO49BANK0000000002EUR"
                />
                <label className="block text-sm font-medium text-zinc-400 mb-2">Amount ({selectedAccount.currencyCode})</label>
                <input
                  type="number"
                  value={transferForm.amount}
                  onChange={(e) => setTransferForm({...transferForm, amount: e.target.value})}
                  className="input-field mb-4"
                  placeholder="0.00"
                  step="0.01"
                  min="0"
                />
                <div className="flex gap-3">
                  <button onClick={() => setActiveModal(null)} className="btn-secondary flex-1">Cancel</button>
                  <button onClick={handleTransfer} className="btn-primary flex-1">Transfer</button>
                </div>
              </>
            )}

            {activeModal === '2fa' && twoFaSetup && (
              <>
                <h3 className="text-xl font-bold mb-4">Setup Two-Factor Authentication</h3>
                <p className="text-zinc-400 text-sm mb-4">
                  Scan this QR code with your authenticator app (Google Authenticator, Authy, etc.)
                </p>
                <div className="bg-white p-4 rounded-xl mb-4 flex justify-center">
                  {qrCodeDataUrl && <img src={qrCodeDataUrl} alt="2FA QR Code" className="w-48 h-48" />}
                </div>
                <p className="text-zinc-400 text-xs mb-2">Or enter this secret manually:</p>
                <p className="font-mono text-sm bg-zinc-800 p-2 rounded mb-4">{twoFaSetup.secret}</p>
                <label className="block text-sm font-medium text-zinc-400 mb-2">Enter verification code</label>
                <input
                  type="text"
                  value={twoFaCode}
                  onChange={(e) => setTwoFaCode(e.target.value)}
                  className="input-field mb-4"
                  placeholder="000000"
                  maxLength={6}
                />
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
    </div>
  );
}
