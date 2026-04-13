import apiClient from './apiClient';
import { jwtDecode } from 'jwt-decode';

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
  
  // Only persist token when 2FA is complete (or not required)
  if (response.data.token && !response.data.twoFactorRequired) {
    sessionStorage.setItem('jwt_token', response.data.token);
  } else {
    sessionStorage.removeItem('jwt_token');
  }
  
  // Store refresh token if provided
  if (response.data.refreshToken) {
    sessionStorage.setItem('refresh_token', response.data.refreshToken);
  }
  
  return response.data;
};

/**
 * Change password — also triggers re-encryption of personal data server-side.
 * After success, all refresh tokens are invalidated → user must log in again.
 * @param {string} oldPassword
 * @param {string} newPassword
 */
export const changePassword = async (oldPassword, newPassword) => {
  const response = await apiClient.post('/auth/change-password', { oldPassword, newPassword });
  return response.data;
};

/**
 * Refresh the access token using refresh token
 * @param {string} refreshToken - Refresh token from login
 * @returns {Promise} RefreshTokenResponse with new token and optional new refresh token
 */
export const refreshAccessToken = async (refreshToken = null) => {
  const token = refreshToken || sessionStorage.getItem('refresh_token');
  
  if (!token) {
    throw new Error('No refresh token available');
  }
  
  try {
    const previousAccess = typeof sessionStorage !== 'undefined' ? sessionStorage.getItem('jwt_token') : null;
    const response = await apiClient.post('/auth/refresh-token', {
      refreshToken: token,
      ...(previousAccess ? { accessToken: previousAccess } : {}),
    });
    
    // Update stored tokens
    sessionStorage.setItem('jwt_token', response.data.token);
    
    // If new refresh token provided (rotation), update it
    if (response.data.refreshToken) {
      sessionStorage.setItem('refresh_token', response.data.refreshToken);
    }
    
    return response.data;
  } catch (error) {
    // Refresh token is invalid/expired - force logout
    logout();
    throw error;
  }
};

/**
 * Logout user
 * Revokes refresh token and clears local storage
 */
export const logout = async () => {
  const refreshToken = sessionStorage.getItem('refresh_token');
  
  try {
    if (refreshToken) {
      // Notify server to revoke refresh token
      await apiClient.post('/auth/logout', { refreshToken });
    }
  } catch (error) {
    // Continue logout even if API call fails
    console.warn('Failed to revoke refresh token:', error);
  }
  
  sessionStorage.removeItem('jwt_token');
  sessionStorage.removeItem('refresh_token');
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
    sessionStorage.setItem('jwt_token', response.data.token);
  }
  
  // Store refresh token if provided
  if (response.data.refreshToken) {
    sessionStorage.setItem('refresh_token', response.data.refreshToken);
  }
  
  return response.data;
};


/**
 * Check if user is authenticated
 * @returns {boolean} Authentication status
 */
export const isAuthenticated = () => {
  const token = sessionStorage.getItem('jwt_token');
  if (!token) {
    return false;
  }

  try {
    const decoded = jwtDecode(token);
    const isExpired = typeof decoded.exp === 'number' && Date.now() >= decoded.exp * 1000;
    const hasValidated2fa = decoded['2fa'] === 'ok';
    return !isExpired && hasValidated2fa;
  } catch {
    return false;
  }
};

/**
 * Check if current authenticated user has ADMIN role.
 * @returns {boolean}
 */
export const isAdmin = () => {
  const token = sessionStorage.getItem('jwt_token');
  if (!token) return false;

  try {
    const decoded = jwtDecode(token);
    const role = String(decoded.role || decoded.authority || '').toUpperCase();
    if (role === 'ADMIN' || role === 'ROLE_ADMIN') return true;

    const roles = Array.isArray(decoded.roles) ? decoded.roles : [];
    return roles.some((r) => {
      const normalized = String(r).toUpperCase();
      return normalized === 'ADMIN' || normalized === 'ROLE_ADMIN';
    });
  } catch {
    return false;
  }
};
