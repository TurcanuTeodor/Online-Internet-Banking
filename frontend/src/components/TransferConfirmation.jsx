import { ArrowRight, Loader2, AlertTriangle } from 'lucide-react';

export default function TransferConfirmation({
  fromAccount,
  toIban,
  amount,
  loading,
  onConfirm,
  onBack,
}) {
  const formatCurrency = (val, cur) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: cur || 'EUR' }).format(val);

  return (
    <div className="space-y-5">
      <div className="flex items-center gap-2 text-amber-400 text-sm">
        <AlertTriangle className="w-4 h-4 shrink-0" />
        Please review the details before confirming.
      </div>

      <div className="glass rounded-xl p-4 space-y-3">
        <div>
          <p className="text-xs text-zinc-500">From</p>
          <p className="text-sm font-mono text-zinc-200">{fromAccount?.iban}</p>
          <p className="text-xs text-zinc-500 mt-0.5">{fromAccount?.currencyCode} account</p>
        </div>
        <div className="flex justify-center">
          <ArrowRight className="w-5 h-5 text-emerald-400" />
        </div>
        <div>
          <p className="text-xs text-zinc-500">To</p>
          <p className="text-sm font-mono text-zinc-200">{toIban}</p>
        </div>
      </div>

      <div className="glass rounded-xl p-4 text-center">
        <p className="text-xs text-zinc-500 mb-1">Amount</p>
        <p className="text-3xl font-bold text-white">
          {formatCurrency(amount, fromAccount?.currencyCode)}
        </p>
      </div>

      <div className="flex gap-3">
        <button type="button" onClick={onBack} disabled={loading} className="btn-secondary flex-1">
          Go Back
        </button>
        <button type="button" onClick={onConfirm} disabled={loading} className="btn-primary flex-1 flex items-center justify-center gap-2">
          {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
          Confirm Transfer
        </button>
      </div>
    </div>
  );
}
