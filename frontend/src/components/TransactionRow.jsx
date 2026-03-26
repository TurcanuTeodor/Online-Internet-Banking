const TYPE_LABELS = {
  WITHDRAWAL: 'Withdrawal',
  DEPOSIT: 'Deposit',
  TRANSFER_INTERNAL: 'Internal Transfer',
  TRANSFER_EXTERNAL: 'External Transfer',
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

export function TransactionCompactRow({ tx, formatDate, formatCurrency, onViewDetails, showAccount = true }) {
  const label = getTransactionLabel(tx);
  const accountText = getTransactionAccountText(tx);
  const amountClass = tx?.sign === '+' ? 'text-emerald-400' : 'text-red-400';
  const amountPrefix = tx?.sign === '+' ? '+' : '-';

  return (
    <div className="glass rounded-xl p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm text-zinc-300">{formatDate(tx?.transactionDate)}</p>
          <p className="text-xs text-zinc-500 mt-1">{label}</p>
        </div>
        <p className={`text-sm font-bold ${amountClass}`}>
          {amountPrefix}
          {formatCurrency(tx?.amount || 0, tx?.currencyCode || tx?.originalCurrencyCode)}
        </p>
      </div>
      {showAccount ? <p className="text-xs font-mono text-zinc-500 mt-2 break-all">{accountText}</p> : null}
      {onViewDetails ? (
        <button
          type="button"
          className="btn-secondary text-xs py-1.5 px-3 mt-3"
          onClick={() => onViewDetails(tx?.id)}
        >
          View details
        </button>
      ) : null}
    </div>
  );
}

