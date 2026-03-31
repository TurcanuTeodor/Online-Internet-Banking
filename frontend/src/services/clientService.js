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
 * Public sign-up: create client profile (no admin JWT). Returns client with id — use id as clientId for auth register.
 * @param {{ firstName: string, lastName: string, sexCode: string, clientTypeCode: string }} profile
 */
export const signUpClientProfile = async (profile) => {
  const response = await apiClient.post('/clients/sign-up', {
    firstName: profile.firstName,
    lastName: profile.lastName,
    sexCode: profile.sexCode,
    clientTypeCode: profile.clientTypeCode,
    active: true,
  });
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

/**
 * Get authenticated client's profile (read-only view).
 * Gateway endpoint: GET /api/clients/view/me
 * @returns {Promise<Object>} Client profile for the logged-in user
 */
export const getClientProfile = async () => {
  try {
    const response = await apiClient.get('/clients/view/me');
    return response.data;
  } catch (err) {
    const message =
      err?.response?.data?.message ||
      err?.message ||
      'Failed to load client profile';
    throw new Error(message);
  }
};

