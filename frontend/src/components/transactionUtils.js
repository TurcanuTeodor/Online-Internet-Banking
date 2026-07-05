const TYPE_LABELS = {
  WDL: 'Withdrawal',
  DEP: 'Deposit',
  TR_INT: 'Internal Transfer',
  TR_EXT: 'External Transfer',
  TRANSFER: 'Transfer',
  TOP_UP: 'Card Top-up',
  PAYMENT: 'Payment',
  REFUND: 'Refund',
};

function prettyType(raw) {
  if (!raw) return 'Transaction';
  const normalized = String(raw).toUpperCase();
  if (TYPE_LABELS[normalized]) return TYPE_LABELS[normalized];
  return String(raw)
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

export function getTransactionLabel(tx) {
  return (
    tx?.merchantName ||
    tx?.merchant ||
    tx?.senderName ||
    tx?.receiverName ||
    tx?.counterpartyName ||
    tx?.displayLabel ||
    prettyType(tx?.transactionTypeName || tx?.transactionTypeCode || tx?.type)
  );
}

export function getTransactionAccountText(tx) {
  return tx?.accountIban || tx?.iban || tx?.currencyCode || tx?.originalCurrencyCode || '—';
}
