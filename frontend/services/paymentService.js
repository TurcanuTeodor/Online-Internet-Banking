import apiClient from './apiClient';

/**
 * Start a one-time card top-up: creates a Stripe PaymentIntent and returns clientSecret for Elements.
 * Currency is determined by the backend from the account — do not send currency from the client.
 */
export async function createTopUpIntent(accountId, amount) {
  const { data } = await apiClient.post('/payments/top-up/intent', {
    accountId,
    amount: typeof amount === 'string' ? Number.parseFloat(amount) : Number(amount),
  });
  return data;
}

export async function getPaymentMethodsByClient(clientId) {
  const { data } = await apiClient.get(`/payment-methods/by-client/${clientId}`);
  return data;
}

/**
 * @param {number} clientId
 * @param {string} stripePaymentMethodId - from stripe.createPaymentMethod (pm_...)
 */
export async function attachPaymentMethod(clientId, stripePaymentMethodId) {
  const { data } = await apiClient.post('/payment-methods', {
    clientId,
    stripePaymentMethodId,
  });
  return data;
}

export async function deletePaymentMethod(paymentMethodId) {
  await apiClient.delete(`/payment-methods/${paymentMethodId}`);
}

export async function setDefaultPaymentMethod(clientId, paymentMethodId) {
  const { data } = await apiClient.put(
    `/payment-methods/${paymentMethodId}/set-default`,
    null,
    { params: { clientId } }
  );
  return data;
}
