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
