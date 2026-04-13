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
      const [accountsData, transactionsData, paymentsData] = await Promise.all([
        getAccountsByClient(clientId),
        getTransactionsByClient(clientId),
        getPaymentHistory(clientId),
      ]);
      setAccounts(accountsData);
      setTransactions(transactionsData);
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
