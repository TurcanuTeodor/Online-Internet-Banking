import { useNavigate } from 'react-router-dom';
import { logout } from '../../services/authService';
import { LogOut, Wallet, Plus, Send, Eye, EyeOff, Shield, Filter, ChevronLeft, ChevronRight, CreditCard, Receipt } from 'lucide-react';
import { useState, useEffect } from 'react';
import { jwtDecode } from 'jwt-decode';
import QRCode from 'qrcode';
import { getAccountsByClient, openAccount, transfer, getBalanceByIban } from '../../services/accountService';
import { getTransactionsByClient } from '../../services/transactionService';
import { setup2FA, confirm2FA } from '../../services/authService';
import TopUpModal from '../components/TopUpModal';
import CardsPaymentsTab from '../components/CardsPaymentsTab';

export default function Dashboard() {
  const navigate = useNavigate();
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
  const [mainTab, setMainTab] = useState('home');

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

  // Reset to page 1 when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [filters, itemsPerPage, selectedAccountFilter]);

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

      <div className="max-w-5xl mx-auto px-6 py-8">
        {/* Error/Success Messages */}
        {error && (
          <div className="mb-4 p-4 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 animate-fade-in">
            {error}
            <button onClick={() => setError('')} className="ml-4 underline">Dismiss</button>
          </div>
        )}
        {success && (
          <div className="mb-4 p-4 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-emerald-400 animate-fade-in">
            {success}
            <button onClick={() => setSuccess('')} className="ml-4 underline">Dismiss</button>
          </div>
        )}

        {clientId && !loading && (
          <div className="border-b border-gray-700 mb-6">
            <div className="flex gap-6 sm:gap-8">
              <button
                type="button"
                onClick={() => setMainTab('home')}
                className={`pb-3 px-1 -mb-px text-sm sm:text-base transition-colors ${
                  mainTab === 'home'
                    ? 'border-b-2 border-green-500 text-white font-medium'
                    : 'border-b-2 border-transparent text-zinc-400 hover:text-zinc-200'
                }`}
              >
                Accounts & Transactions
              </button>
              <button
                type="button"
                onClick={() => setMainTab('payments')}
                className={`pb-3 px-1 -mb-px text-sm sm:text-base transition-colors ${
                  mainTab === 'payments'
                    ? 'border-b-2 border-green-500 text-white font-medium'
                    : 'border-b-2 border-transparent text-zinc-400 hover:text-zinc-200'
                }`}
              >
                Cards & Payments
              </button>
            </div>
          </div>
        )}

        {loading ? (
          <div className="glass rounded-2xl p-12 text-center">
            <div className="w-12 h-12 border-4 border-emerald-500/20 border-t-emerald-500 rounded-full animate-spin mx-auto mb-4"></div>
            <p className="text-zinc-400">Loading your accounts...</p>
          </div>
        ) : mainTab === 'payments' && clientId ? (
          <CardsPaymentsTab
            clientId={clientId}
            accounts={accounts}
            transactions={transactions}
            onRefresh={fetchData}
            onOpenTopUp={setTopUpAccount}
          />
        ) : (
          <div className="space-y-10">
            {/* Accounts Section */}
            <div>
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-4">
                <h2 className="text-2xl font-bold">Your Accounts</h2>
                <div className="flex flex-wrap gap-3">
                  <button
                    onClick={() => setShowBalances(!showBalances)}
                    className="btn-secondary flex items-center gap-2"
                  >
                    {showBalances ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    {showBalances ? 'Hide' : 'Show'} Balances
                  </button>
                  <button
                    onClick={() => setActiveModal('openAccount')}
                    className="btn-primary flex items-center gap-2"
                  >
                    <Plus className="w-4 h-4" />
                    Open Account
                  </button>
                </div>
              </div>

              {accounts.length === 0 ? (
                <div className="glass rounded-2xl p-12 text-center">
                  <Wallet className="w-16 h-16 text-zinc-600 mx-auto mb-4" />
                  <p className="text-zinc-400 mb-4">No accounts yet</p>
                  <button onClick={() => setActiveModal('openAccount')} className="btn-primary">
                    Open Your First Account
                  </button>
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {accounts.map((account) => {
                    const canTopUp =
                      account.status === 'ACTIVE' &&
                      (account.currencyCode === 'EUR' || account.currencyCode === 'RON');
                    return (
                      <div key={account.id} className="glass rounded-2xl p-6 hover:border-emerald-500/20 transition-all flex flex-col">
                        <div className="flex items-center justify-between gap-3 mb-3">
                          <div className="flex items-center gap-2 min-w-0">
                            <div className="w-12 h-12 bg-emerald-500/20 rounded-xl flex items-center justify-center shrink-0">
                              <Wallet className="w-6 h-6 text-emerald-400" />
                            </div>
                            <span className="px-2.5 py-1 rounded-lg text-xs font-semibold bg-zinc-800 text-zinc-200 border border-zinc-600/50">
                              {account.currencyCode}
                            </span>
                          </div>
                          <span
                            className={`shrink-0 px-3 py-1 rounded-lg text-xs font-medium ${
                              account.status === 'ACTIVE'
                                ? 'bg-emerald-500/20 text-emerald-400'
                                : 'bg-zinc-700 text-zinc-400'
                            }`}
                          >
                            {account.status}
                          </span>
                        </div>
                        <p className="text-sm font-mono text-gray-400 break-all leading-snug">{account.iban}</p>
                        <p className="text-3xl font-bold mt-4 mb-6">
                          {showBalances ? formatCurrency(account.balance, account.currencyCode) : '••••••'}
                        </p>
                        <div
                          className={`mt-auto grid gap-3 ${canTopUp ? 'grid-cols-2' : 'grid-cols-1'}`}
                        >
                          <button
                            onClick={() => {
                              setSelectedAccount(account);
                              setActiveModal('transfer');
                            }}
                            className="btn-primary w-full flex items-center justify-center gap-2"
                          >
                            <Send className="w-4 h-4" />
                            Transfer
                          </button>
                          {canTopUp && (
                            <button
                              type="button"
                              onClick={() => setTopUpAccount(account)}
                              className="btn-secondary w-full flex items-center justify-center gap-2 border-emerald-500/30 text-emerald-300"
                            >
                              <CreditCard className="w-4 h-4" />
                              Top up
                            </button>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Recent Transactions */}
            <div>
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-4">
                <h2 className="text-2xl font-bold">Recent Transactions</h2>
                <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
                  <select
                    value={selectedAccountFilter}
                    onChange={(e) => setSelectedAccountFilter(e.target.value)}
                    className="input-field min-w-[200px]"
                  >
                    <option value="all">All Accounts</option>
                    {accounts.map((account) => (
                      <option key={account.id} value={account.iban}>
                        {account.iban} ({account.currencyCode})
                      </option>
                    ))}
                  </select>
                  <button
                    onClick={() => setShowFilters(!showFilters)}
                    className={`btn-secondary flex items-center justify-center gap-2 whitespace-nowrap ${showFilters ? 'bg-emerald-500/20 border-emerald-500/30' : ''}`}
                  >
                    <Filter className="w-4 h-4" />
                    {showFilters ? 'Hide' : 'Show'} Filters
                  </button>
                </div>
              </div>

              {/* Filter Panel */}
              {showFilters && (
                <div className="glass rounded-2xl p-6 mb-4 animate-fade-in">
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-zinc-400 mb-2">Transaction Type</label>
                      <select
                        value={filters.type}
                        onChange={(e) => handleFilterChange('type', e.target.value)}
                        className="input-field"
                      >
                        <option value="all">All Types</option>
                        {getTransactionTypes().map(type => (
                          <option key={type} value={type}>{type}</option>
                        ))}
                      </select>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-zinc-400 mb-2">Transaction Sign</label>
                      <select
                        value={filters.sign}
                        onChange={(e) => handleFilterChange('sign', e.target.value)}
                        className="input-field"
                      >
                        <option value="all">All</option>
                        <option value="+">Credit (+)</option>
                        <option value="-">Debit (-)</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-zinc-400 mb-2">Date From</label>
                      <input
                        type="date"
                        value={filters.dateFrom}
                        onChange={(e) => handleFilterChange('dateFrom', e.target.value)}
                        className="input-field"
                      />
                      {filters.dateFrom && !filters.dateTo && (
                        <p className="text-xs text-amber-400 mt-1">⚠️ Please select "Date To" to complete the range</p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-zinc-400 mb-2">Date To</label>
                      <input
                        type="date"
                        value={filters.dateTo}
                        onChange={(e) => handleFilterChange('dateTo', e.target.value)}
                        className="input-field"
                      />
                      {!filters.dateFrom && filters.dateTo && (
                        <p className="text-xs text-amber-400 mt-1">⚠️ Please select "Date From" to complete the range</p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-zinc-400 mb-2">Min Amount</label>
                      <input
                        type="number"
                        value={filters.minAmount}
                        onChange={(e) => handleFilterChange('minAmount', e.target.value)}
                        className="input-field"
                        placeholder="0.00"
                        step="0.01"
                        min="0"
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-zinc-400 mb-2">Max Amount</label>
                      <input
                        type="number"
                        value={filters.maxAmount}
                        onChange={(e) => handleFilterChange('maxAmount', e.target.value)}
                        className="input-field"
                        placeholder="0.00"
                        step="0.01"
                        min="0"
                      />
                    </div>
                  </div>

                  <div className="flex items-center justify-between mt-4 pt-4 border-t border-zinc-700">
                    <p className="text-sm text-zinc-400">
                      Found {filteredTransactions.length} transaction{filteredTransactions.length !== 1 ? 's' : ''}
                    </p>
                    <button onClick={resetFilters} className="btn-secondary text-sm">
                      Reset Filters
                    </button>
                  </div>
                </div>
              )}

              {transactions.length === 0 ? (
                <div className="glass rounded-2xl">
                  <div className="flex flex-col items-center justify-center py-16 gap-3">
                    <Receipt className="w-12 h-12 text-zinc-600" aria-hidden />
                    <p className="text-gray-400 text-sm">No transactions yet</p>
                    <p className="text-gray-500 text-xs text-center px-4">
                      Make a transfer or top up to get started
                    </p>
                  </div>
                </div>
              ) : filteredTransactions.length === 0 ? (
                <div className="glass rounded-2xl p-12 text-center">
                  <p className="text-zinc-400">No transactions match your filters</p>
                  <button onClick={resetFilters} className="btn-secondary mt-4">
                    Reset Filters
                  </button>
                </div>
              ) : (
                <>
                  <div className="glass rounded-2xl overflow-hidden">
                    <table className="w-full">
                      <thead className="bg-zinc-800/50">
                        <tr>
                          <th className="px-6 py-4 text-left text-xs font-medium text-zinc-400 uppercase">Date</th>
                          <th className="px-6 py-4 text-left text-xs font-medium text-zinc-400 uppercase">Type</th>
                          <th className="px-6 py-4 text-left text-xs font-medium text-zinc-400 uppercase">Account</th>
                          <th className="px-6 py-4 text-right text-xs font-medium text-zinc-400 uppercase">Amount</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-zinc-800">
                        {paginatedTransactions.map((tx) => (
                          <tr key={tx.id} className="hover:bg-zinc-800/30">
                            <td className="px-6 py-4 text-sm text-zinc-300">{formatDate(tx.transactionDate)}</td>
                            <td className="px-6 py-4">
                              <span className="px-2 py-1 rounded text-xs font-medium bg-blue-500/20 text-blue-400">
                                {tx.transactionTypeName}
                              </span>
                            </td>
                            <td className="px-6 py-4 text-sm font-mono text-zinc-400">{tx.accountIban}</td>
                            <td className={`px-6 py-4 text-sm font-bold text-right ${
                              tx.sign === '+' ? 'text-emerald-400' : 'text-red-400'
                            }`}>
                              {tx.sign === '+' ? '+' : '-'}
                              {formatCurrency(tx.amount, tx.currencyCode || tx.originalCurrencyCode)}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  {/* Pagination Controls */}
                  {totalPages > 1 && (
                    <div className="flex items-center justify-between mt-4">
                      <div className="flex items-center gap-2">
                        <label className="text-sm text-zinc-400">Items per page:</label>
                        <select
                          value={itemsPerPage}
                          onChange={(e) => setItemsPerPage(Number(e.target.value))}
                          className="input-field !py-1 !px-2 text-sm w-20"
                        >
                          <option value={10}>10</option>
                          <option value={25}>25</option>
                          <option value={50}>50</option>
                        </select>
                      </div>

                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => goToPage(currentPage - 1)}
                          disabled={currentPage === 1}
                          className="btn-secondary !p-2 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          <ChevronLeft className="w-4 h-4" />
                        </button>

                        <div className="flex items-center gap-1">
                          {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                            let pageNum;
                            if (totalPages <= 5) {
                              pageNum = i + 1;
                            } else if (currentPage <= 3) {
                              pageNum = i + 1;
                            } else if (currentPage >= totalPages - 2) {
                              pageNum = totalPages - 4 + i;
                            } else {
                              pageNum = currentPage - 2 + i;
                            }

                            return (
                              <button
                                key={pageNum}
                                onClick={() => goToPage(pageNum)}
                                className={`px-3 py-1 rounded-lg text-sm font-medium transition-all ${
                                  currentPage === pageNum
                                    ? 'bg-emerald-500 text-white'
                                    : 'bg-zinc-800 text-zinc-400 hover:bg-zinc-700'
                                }`}
                              >
                                {pageNum}
                              </button>
                            );
                          })}
                        </div>

                        <button
                          onClick={() => goToPage(currentPage + 1)}
                          disabled={currentPage === totalPages}
                          className="btn-secondary !p-2 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          <ChevronRight className="w-4 h-4" />
                        </button>

                        <span className="text-sm text-zinc-400 ml-2">
                          Page {currentPage} of {totalPages}
                        </span>
                      </div>
                    </div>
                  )}
                </>
              )}
            </div>
          </div>
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
    </div>
  );
}
