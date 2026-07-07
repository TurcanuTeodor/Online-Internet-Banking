import { useState, useEffect } from 'react';
import { jwtDecode } from 'jwt-decode';
import { getAccountsByClient } from '@/services/accountService';
import { getTransactionsByClient } from '@/services/transactionService';
import { getPaymentHistory } from '@/services/paymentService';

export default function useDashboardData() {
  const [clientId, setClientId] = useState(null);
  const [sub, setSub] = useState('');
  const [twoFaEnabled, setTwoFaEnabled] = useState(false);
  const [accounts, setAccounts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const token = sessionStorage.getItem('jwt_token');
    if (token) {
      try {
        const decoded = jwtDecode(token);
        setClientId(decoded.clientId);
        setTwoFaEnabled(decoded['2fa_verified'] === true);
        setSub(decoded.sub || '');
      } catch (e) {
        console.error('Failed to decode token:', e);
      }
    }
  }, []);

  const fetchData = async () => {
    if (!clientId) return;
    setLoading(true);
    try {
      const results = await Promise.allSettled([
        getAccountsByClient(clientId),
        getTransactionsByClient(clientId),
        getPaymentHistory(clientId),
      ]);

      const accountsData = results[0].status === 'fulfilled' ? results[0].value : [];
      const transactionsData = results[1].status === 'fulfilled' ? results[1].value : [];
      const paymentsData = results[2].status === 'fulfilled' ? results[2].value : [];

      if (results.some(r => r.status === 'rejected')) {
        setError('Failed to load some dashboard data');
      }

      const accountMap = new Map();
      (accountsData || []).forEach(acc => accountMap.set(String(acc.id), acc));

      const enrichedTransactions = (transactionsData || []).map(tx => {
        const acc = accountMap.get(String(tx.accountId));
        if (acc) {
          return { ...tx, iban: acc.iban, currencyCode: acc.currencyCode };
        }
        return tx;
      });

      console.log("Enriched Transactions:", enrichedTransactions);
      setAccounts(accountsData);
      setTransactions(enrichedTransactions);
      setPayments(Array.isArray(paymentsData) ? paymentsData : []);
    } catch (err) {
      console.error('Error fetching data:', err);
      setError('Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (clientId) fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clientId]);

  const ledgerTransactions = [...(transactions || [])]
    .sort((a, b) => new Date(b.transactionDate || 0).getTime() - new Date(a.transactionDate || 0).getTime());

  const totalBalance = accounts.reduce((sum, account) => sum + Number(account.balance || 0), 0);
  const activeAccountsCount = accounts.filter((account) => account.status === 'ACTIVE').length;
  const monthKey = new Date().toISOString().slice(0, 7);
  const monthlyOutgoing = ledgerTransactions
    .filter((tx) => tx.sign === '-' && String(tx.transactionDate || '').startsWith(monthKey))
    .reduce((sum, tx) => sum + Number(tx.amount || 0), 0);

  return {
    clientId,
    sub,
    twoFaEnabled,
    setTwoFaEnabled,
    accounts,
    transactions,
    payments,
    ledgerTransactions,
    loading,
    error,
    setError,
    fetchData,
    totalBalance,
    activeAccountsCount,
    monthlyOutgoing,
  };
}
