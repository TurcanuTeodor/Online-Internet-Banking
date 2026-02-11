import apiClient from './apiClient';

/**
 * Register a new user
 * @param {Object} userData - User registration data
 * @returns {Promise} API response
 */
export const register = async (userData) => {
  const response = await apiClient.post('/auth/register', userData);
  return response.data;
};

/**
 * Login user
 * @param {string} username - Username
 * @param {string} password - User password
 * @returns {Promise} LoginResponse with token (or tempToken if 2FA enabled)
 */
export const login = async (usernameOrEmail, password) => {
  const response = await apiClient.post('/auth/login', { usernameOrEmail, password });
  
  // If 2FA is required, tempToken is returned instead of token
  if (response.data.token) {
    localStorage.setItem('jwt_token', response.data.token);
  }
  
  return response.data;
};

/**
 * Logout user
 * Clears token from localStorage and redirects to login
 */
export const logout = () => {
  localStorage.removeItem('jwt_token');
  window.location.href = '/login';
};

/**
 * Setup 2FA for the authenticated user
 * @returns {Promise} TwoFaSetupResponse with QR code and secret
 */
export const setup2FA = async () => {
  const response = await apiClient.post('/auth/2fa/setup');
  return response.data;
};

/**
 * Confirm 2FA setup with verification code
 * @param {string} code - 6-digit verification code
 * @returns {Promise} API response
 */
export const confirm2FA = async (code) => {
  const response = await apiClient.post('/auth/2fa/confirm', { code });
  return response.data;
};

/**
 * Verify 2FA code during login
 * @param {string} tempToken - Temporary token from login response
 * @param {string} code - 6-digit verification code
 * @returns {Promise} LoginResponse with actual JWT token
 */
export const verify2FA = async (tempToken, code) => {
  const response = await apiClient.post('/auth/2fa/verify', {
    tempToken,
    code,
  });
  
  if (response.data.token) {
    localStorage.setItem('jwt_token', response.data.token);
  }
  
  return response.data;
};

/**
 * Check if user is authenticated
 * @returns {boolean} Authentication status
 */
export const isAuthenticated = () => {
  return !!localStorage.getItem('jwt_token');
};
