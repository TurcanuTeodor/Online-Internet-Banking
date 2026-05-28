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
 * @param {string} totpCode - Optional TOTP code for 2FA step-up
 * @returns {Promise} API response
 */
export const transfer = async (fromIban, toIban, amount, totpCode = null) => {
  const headers = {};
  if (totpCode) {
    headers['X-TOTP-Code'] = totpCode;
  }
  const response = await apiClient.post('/accounts/transfer', {
    fromIban,
    toIban,
    amount,
  }, { headers });
  return response.data;
};

/**
 * Get all accounts from view (read-only)
 * @returns {Promise} List of all accounts
 */
export const getAllAccountsFromView = async () => {
  const response = await apiClient.get('/accounts/view');
  return response.data;
};

/**
 * Freeze an account (set status to SUSPENDED)
 * @param {number} accountId - Account ID
 * @returns {Promise} Frozen account details
 */
export const freezeAccount = async (accountId) => {
  const response = await apiClient.post(`/accounts/${accountId}/freeze`);
  return response.data;
};

/**
 * Unfreeze / reactivate an account (set status back to ACTIVE)
 * @param {number} accountId - Account ID
 * @returns {Promise} Reactivated account details
 */
export const unfreezeAccount = async (accountId) => {
  const response = await apiClient.post(`/accounts/${accountId}/unfreeze`);
  return response.data;
};
