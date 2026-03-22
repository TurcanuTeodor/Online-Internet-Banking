import apiClient from './apiClient';

/**
 * Create a new client (Personal or Business)
 * @param {Object} clientData - Client data (includes clientType: 'PF' or 'PJ')
 * @returns {Promise} Created client details
 */
export const createClient = async (clientData) => {
  const response = await apiClient.post('/clients', clientData);
  return response.data;
};

/**
 * Search clients by name
 * @param {string} name - Search query for client name
 * @returns {Promise} List of matching clients
 */
export const searchClientsByName = async (name) => {
  const response = await apiClient.get('/clients/search', {
    params: { name },
  });
  return response.data;
};

/**
 * Update client contact information
 * @param {number} clientId - Client ID
 * @param {Object} contactInfo - Contact information (email, phone, address, etc.)
 * @returns {Promise} Updated contact info
 */
export const updateClientContact = async (clientId, contactInfo) => {
  const response = await apiClient.put(`/clients/${clientId}/contact`, contactInfo);
  return response.data;
};

/**
 * Get client summary (includes accounts, balances, etc.)
 * @param {number} clientId - Client ID
 * @returns {Promise} Client summary data
 */
export const getClientSummary = async (clientId) => {
  const response = await apiClient.get(`/clients/${clientId}/summary`);
  return response.data;
};

/**
 * Delete (soft delete) a client
 * @param {number} clientId - Client ID
 * @returns {Promise} API response
 */
export const deleteClient = async (clientId) => {
  const response = await apiClient.delete(`/clients/${clientId}`);
  return response.data;
};

/**
 * Admin: suspend client (sets active=false). Idempotent if already inactive.
 * @param {number} clientId
 */
export const suspendClient = async (clientId) => {
  await apiClient.put(`/clients/${clientId}/suspend`);
};

/**
 * Get all clients from view (read-only)
 * @returns {Promise} List of all clients
 */
export const getAllClientsFromView = async () => {
  const response = await apiClient.get('/clients/view');
  return response.data;
};
