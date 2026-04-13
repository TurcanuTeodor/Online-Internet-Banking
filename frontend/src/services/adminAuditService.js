import apiClient from './apiClient';

/**
 * Log a sensitive-data reveal action for audit purposes.
 * @param {{ scope: string, targetType: string, targetId: string|number, reasonCode: string, reasonDetails?: string }} payload
 */
export const logSensitiveDataReveal = async (payload) => {
  const response = await apiClient.post('/accounts/audit/reveal', {
    scope: payload.scope,
    targetType: payload.targetType,
    targetId: String(payload.targetId),
    reasonCode: payload.reasonCode,
    reasonDetails: payload.reasonDetails || '',
  });
  return response.data;
};

/**
 * Retrieve admin reveal-audit report rows.
 * @param {{ page?: number, size?: number, reasonCode?: string }} params
 */
export const getSensitiveRevealAuditEvents = async ({ page = 0, size = 10, reasonCode = '' } = {}) => {
  const response = await apiClient.get('/accounts/audit/reveal-events', {
    params: {
      page,
      size,
      ...(reasonCode ? { reasonCode } : {}),
    },
  });
  return response.data;
};