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
