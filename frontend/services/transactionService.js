import apiClient from './apiClient';

/**
 * Get all transactions from view (read-only)
 * @returns {Promise} List of all transactions
 */
export const getAllTransactionsFromView = async () => {
  const response = await apiClient.get('/transactions/view-all');
  return response.data;
};

/**
 * Get transactions by account IBAN
 * @param {string} iban - Account IBAN
 * @returns {Promise} List of transactions
 */
export const getTransactionsByIban = async (iban) => {
  const response = await apiClient.get(`/transactions/by-iban/${iban}`);
  return response.data;
};

/**
 * Get transactions by client ID
 * @param {number} clientId - Client ID
 * @returns {Promise} List of transactions
 */
export const getTransactionsByClient = async (clientId) => {
  const response = await apiClient.get(`/transactions/by-client/${clientId}`);
  return response.data;
};

/**
 * Get transactions between two dates
 * @param {string} from - Start date (ISO format: YYYY-MM-DD)
 * @param {string} to - End date (ISO format: YYYY-MM-DD)
 * @returns {Promise} List of transactions
 */
export const getTransactionsBetweenDates = async (from, to) => {
  const response = await apiClient.get('/transactions/between', {
    params: { from, to },
  });
  return response.data;
};

/**
 * Get transactions by type code
 * @param {string} code - Transaction type code (e.g., 'DEP', 'RET', 'TRF')
 * @returns {Promise} List of transactions
 */
export const getTransactionsByType = async (code) => {
  const response = await apiClient.get(`/transactions/by-type/${code}`);
  return response.data;
};

/**
 * Get daily transaction totals (aggregated report)
 * @returns {Promise} Map of dates to total amounts
 */
export const getDailyTotals = async () => {
  const response = await apiClient.get('/transactions/daily-totals');
  return response.data;
};
