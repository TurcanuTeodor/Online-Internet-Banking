import { useNavigate, useParams } from 'react-router-dom';
import { logout } from '../../services/authService';
import { LogOut, Wallet, Shield, CreditCard, LayoutDashboard, ArrowLeftRight, UserCircle } from 'lucide-react';
import { useState, useEffect } from 'react';
import { jwtDecode } from 'jwt-decode';
import QRCode from 'qrcode';
import { getAccountsByClient, openAccount, transfer, getBalanceByIban } from '../../services/accountService';
import { getTransactionsByClient } from '../../services/transactionService';
import { setup2FA, confirm2FA } from '../../services/authService';
import TopUpModal from '../components/TopUpModal';
import CardsPaymentsTab from '../components/CardsPaymentsTab';
import ProfileTab from '../components/ProfileTab';
import TransactionDetailsModal from '../components/TransactionDetailsModal';
import UserAccountsTab from '../components/UserAccountsTab';
import UserTransactionsTab from '../components/UserTransactionsTab';

const USER_SECTIONS = ['accounts', 'transactions', 'payments', 'profile'];

export default function Dashboard() {
  const navigate = useNavigate();
  const { section } = useParams();
  const [clientId, setClientId] = useState(null);
  const [accounts, setAccounts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showBalances, setShowBalances] = useState(true);
  const [activeModal, setActiveModal] = useState(null); // 'openAccount', 'transfer', '2fa'
  const [selectedAccount, setSelectedAccount] = useState(null);
  const [twoFaSetup, setTwoFaSetup] = useState(null);
  const [qrCodeDataUrl, setQrCodeDataUrl] = useState(null);
  const [twoFaEnabled, setTwoFaEnabled] = useState(false);
  
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
      } catch (error) {
        console.error('Failed to decode token:', error);
      }
    }
  }, []);

  useEffect(() => {
    if (clientId) {
      fetchData();
    }
  }, [clientId]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [accountsData, transactionsData] = await Promise.all([
        getAccountsByClient(clientId),
        getTransactionsByClient(clientId)
      ]);
      setAccounts(accountsData);
      setTransactions(transactionsData); // Show all transactions, filter and paginate client-side
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

  // Filter transactions based on selected filters
  const getFilteredTransactions = () => {
    let filtered = [...transactions];

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
    const types = new Set(transactions.map(tx => tx.transactionTypeName));
    return Array.from(types).sort();
  };

  // Apply filtering and pagination
  const filteredTransactions = getFilteredTransactions();
  const totalPages = Math.ceil(filteredTransactions.length / itemsPerPage);
  const paginatedTransactions = filteredTransactions.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );
  const totalBalance = accounts.reduce((sum, account) => sum + Number(account.balance || 0), 0);
  const activeAccountsCount = accounts.filter((account) => account.status === 'ACTIVE').length;
  const monthKey = new Date().toISOString().slice(0, 7);
  const monthlyOutgoing = transactions
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

  const contentWidthClass = mainTab === 'accounts' || mainTab === 'transactions' ? 'max-w-6xl' : 'max-w-5xl';
  const USER_NAV = [
    { id: 'accounts', label: 'Accounts', icon: LayoutDashboard },
    { id: 'transactions', label: 'Transactions', icon: ArrowLeftRight },
    { id: 'payments', label: 'Payments', icon: CreditCard },
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
          <div className="flex items-center gap-2 shrink-0">
            {!twoFaEnabled && (
              <button
                onClick={handleSetup2FA}
                className="btn-secondary flex items-center gap-2 text-sm px-3 py-2 md:text-base md:px-4"
              >
                <Shield className="w-4 h-4 shrink-0" />
                Enable 2FA
              </button>
            )}
            <button
              onClick={handleLogout}
              className="btn-secondary flex items-center gap-2 text-sm px-3 py-2 md:text-base md:px-4"
            >
              <LogOut className="w-4 h-4 shrink-0" />
              Logout
            </button>
          </div>
        </div>
      </nav>

      <div className={`${contentWidthClass} mx-auto px-6 py-8`}>
        {(error || success) && (
          <div className="fixed top-4 right-4 z-[60] space-y-2 w-[min(92vw,360px)]">
            {error ? (
              <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-xl text-red-300 text-sm shadow-lg">
                {error}
              </div>
            ) : null}
            {success ? (
              <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-emerald-300 text-sm shadow-lg">
                {success}
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
          <div className="glass rounded-2xl p-12 text-center">
            <div className="w-12 h-12 border-4 border-emerald-500/20 border-t-emerald-500 rounded-full animate-spin mx-auto mb-4"></div>
            <p className="text-zinc-400">Loading your accounts...</p>
          </div>
        ) : mainTab === 'payments' && clientId ? (
          <div className="space-y-6">
            <div className="glass rounded-2xl p-6">
              <h2 className="text-2xl font-bold">Cards & Payments</h2>
              <p className="text-zinc-500 text-sm mt-1">
                Manage saved cards, review payment activity, and request refunds.
              </p>
            </div>
            <CardsPaymentsTab
              clientId={clientId}
              accounts={accounts}
              transactions={transactions}
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
            showBalances={showBalances}
            setShowBalances={setShowBalances}
            setActiveModal={setActiveModal}
            setSelectedAccount={setSelectedAccount}
            setTopUpAccount={setTopUpAccount}
            formatCurrency={formatCurrency}
            totalBalance={totalBalance}
            activeAccountsCount={activeAccountsCount}
            monthlyOutgoing={monthlyOutgoing}
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
