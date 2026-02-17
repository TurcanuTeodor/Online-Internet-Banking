import apiClient from './apiClient';

/**
 * Open a new account
 * @param {number} clientId - Client ID
 * @param {string} currencyCode - Currency code (e.g., 'USD', 'EUR')
 * @returns {Promise} Created account
 */
export const openAccount = async (clientId, currencyCode) => {
  const response = await apiClient.post('/accounts/open', {
    clientId,
    currencyCode,
  });
  return response.data;
};

/**
 * Close an account
 * @param {number} accountId - Account ID
 * @returns {Promise} Closed account details
 */
export const closeAccount = async (accountId) => {
  const response = await apiClient.post(`/accounts/${accountId}/close`);
  return response.data;
};

/**
 * Get accounts by client ID
 * @param {number} clientId - Client ID
 * @returns {Promise} List of accounts
 */
export const getAccountsByClient = async (clientId) => {
  const response = await apiClient.get(`/accounts/by-client/${clientId}`);
  return response.data;
};

/**
 * Get account balance by IBAN
 * @param {string} iban - Account IBAN
 * @returns {Promise} Account balance
 */
export const getBalanceByIban = async (iban) => {
  const response = await apiClient.get(`/accounts/${iban}/balance`);
  return response.data;
};

/**
 * Transfer money between accounts
 * @param {string} fromIban - Source account IBAN
 * @param {string} toIban - Destination account IBAN
 * @param {number} amount - Transfer amount
 * @returns {Promise} API response
 */
export const transfer = async (fromIban, toIban, amount) => {
  const response = await apiClient.post('/accounts/transfer', {
    fromIban,
    toIban,
    amount,
  });
  return response.data;
};
