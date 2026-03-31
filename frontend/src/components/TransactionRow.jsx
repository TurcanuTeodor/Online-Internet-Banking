import { getTransactionLabel, getTransactionAccountText } from './transactionUtils';
import { TransactionIcon } from './transactionCategoryConfig';

export function TransactionCompactRow({ tx, formatDate, formatCurrency, onViewDetails, showAccount = true }) {
  const label = getTransactionLabel(tx);
  const accountText = getTransactionAccountText(tx);
  const amountClass = tx?.sign === '+' ? 'text-emerald-400' : 'text-red-400';
  const amountPrefix = tx?.sign === '+' ? '+' : '-';

  return (
    <div className="glass rounded-xl p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <TransactionIcon transaction={tx} />
          <div>
            <p className="text-sm text-zinc-300">{formatDate(tx?.transactionDate)}</p>
            <p className="text-xs text-zinc-500 mt-1">{label}</p>
          </div>
        </div>
        <p className={`text-sm font-bold ${amountClass}`}>
          {amountPrefix}
          {formatCurrency(tx?.amount || 0, tx?.currencyCode || tx?.originalCurrencyCode)}
        </p>
      </div>
      {showAccount ? <p className="text-xs font-mono text-zinc-500 mt-2 break-all pl-11">{accountText}</p> : null}
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
