import apiClient from './apiClient';

/**
 * Get paginated fraud alerts (FLAG, BLOCK, MANUAL_REVIEW statuses).
 * @param {number} page  - 0-indexed page number
 * @param {number} size  - items per page
 * @returns {Promise<{ content: Array, totalElements: number, totalPages: number }>}
 */
export const getFraudAlerts = async (page = 0, size = 20) => {
  const response = await apiClient.get('/fraud/alerts', { params: { page, size } });
  return response.data;
};

/**
 * Convenience for charts: fetch a larger slice of alerts.
 * Note: still uses the paginated endpoint; increase size if needed.
 */
export const getFraudAlertsForCharts = async (size = 500) => {
  const safeSize = Number.isFinite(Number(size)) ? Math.max(1, Math.min(5000, Number(size))) : 500;
  const response = await apiClient.get('/fraud/alerts', { params: { page: 0, size: safeSize } });
  return response.data;
};

/**
 * Get a single fraud decision by ID.
 * @param {number|string} id
 */
export const getFraudDecision = async (id) => {
  const response = await apiClient.get(`/fraud/decisions/${id}`);
  return response.data;
};

/**
 * Admin: review a fraud decision — change status and add notes.
 * @param {number|string} id
 * @param {string} newStatus  - e.g. 'ALLOW', 'BLOCK', 'FLAG'
 * @param {string} notes      - Admin review notes
 */
export const reviewFraudDecision = async (id, newStatus, notes) => {
  const response = await apiClient.put(`/fraud/decisions/${id}/review`, {
    status: newStatus,
    notes,
  });
  return response.data;
};

/**
 * Get the current user's fraud/security alerts.
 * @param {number} page
 * @param {number} size
 */
export const getMyFraudAlerts = async (page = 0, size = 20) => {
  const response = await apiClient.get('/fraud/user/alerts', { params: { page, size } });
  return response.data;
};

/**
 * Resolve one of the current user's fraud alerts.
 * @param {number|string} id
 * @param {'LEGITIMATE'|'FRAUD_REPORTED'} resolution
 * @param {string} notes
 */
export const resolveMyFraudAlert = async (id, resolution, notes = '') => {
  const response = await apiClient.post(`/fraud/user/alerts/${id}/resolve`, {
    resolution,
    notes,
  });
  return response.data;
};
