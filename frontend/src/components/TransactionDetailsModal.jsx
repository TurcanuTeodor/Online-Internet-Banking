import { useEffect, useMemo, useState } from 'react';
import { Loader2, Receipt } from 'lucide-react';
import { getTransactionDetails } from '../../services/transactionService';
import ModalShell from './ModalShell';

const LABELS = {
  transactionId: 'Transaction ID',
  id: 'Transaction ID',
  transactionDate: 'Date',
  createdAt: 'Created at',
  updatedAt: 'Updated at',
  transactionTypeName: 'Type',
  transactionTypeCode: 'Type code',
  transactionType: 'Type',
  type: 'Type',
  status: 'Status',
  sign: 'Direction',
  amount: 'Amount',
  currencyCode: 'Currency',
  originalAmount: 'Original amount',
  originalCurrencyCode: 'Original currency',
  exchangeRate: 'Exchange rate',
  accountIban: 'Account IBAN',
  senderIban: 'Sender IBAN',
  receiverIban: 'Receiver IBAN',
  fromIban: 'From IBAN',
  toIban: 'To IBAN',
  destinationIban: 'Destination IBAN',
  destinationAccountIban: 'Destination account IBAN',
  accountId: 'Account ID',
  destinationAccountId: 'Destination account ID',
  clientId: 'Client ID',
  merchant: 'Merchant',
  reference: 'Reference',
  description: 'Description',
  details: 'Details',
};

function isEmpty(v) {
  return v === null || v === undefined || v === '';
}

function humanizeKey(key) {
  const normalized = String(key || '')
    .replace(/_/g, ' ')
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .trim();
  if (!normalized) return '—';
  return normalized.charAt(0).toUpperCase() + normalized.slice(1);
}

function formatMaybeDate(v) {
  if (isEmpty(v)) return null;
  const d = new Date(v);
  if (Number.isNaN(d.getTime())) return null;
  return d.toLocaleString();
}

function formatMoney(amount, currency) {
  if (isEmpty(amount)) return null;
  const n = typeof amount === 'number' ? amount : Number.parseFloat(amount);
  if (!Number.isFinite(n)) return null;
  try {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: currency || 'EUR' }).format(n);
  } catch {
    return `${n.toFixed(2)} ${currency || ''}`.trim();
  }
}

function formatValue(key, value, all) {
  if (isEmpty(value)) return null;

  if (['transactionDate', 'createdAt', 'updatedAt'].includes(key)) {
    return formatMaybeDate(value) ?? String(value);
  }

  if (key === 'amount') {
    return formatMoney(value, all?.currencyCode || all?.originalCurrencyCode) ?? String(value);
  }

  if (key === 'originalAmount') {
    return formatMoney(value, all?.originalCurrencyCode || all?.currencyCode) ?? String(value);
  }

  if (key === 'sign') {
    if (value === '+') return 'Credit (+)';
    if (value === '-') return 'Debit (-)';
  }

  if (typeof value === 'object') {
    try {
      return JSON.stringify(value);
    } catch {
      return String(value);
    }
  }

  return String(value);
}

function ReceiptField({ label, value, mono = false }) {
  return (
    <div className="glass rounded-xl p-3">
      <p className="text-xs text-zinc-500 mb-1">{label}</p>
      <p className={`text-sm text-zinc-200 break-words ${mono ? 'font-mono' : ''}`}>{value}</p>
    </div>
  );
}

export default function TransactionDetailsModal({ id, onClose }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let mounted = true;
    (async () => {
      if (id == null) return;
      setLoading(true);
      setError('');
      try {
        const d = await getTransactionDetails(id);
        if (mounted) setData(d);
      } catch (e) {
        if (mounted) setError(e.message || 'Could not load transaction');
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    return () => {
      mounted = false;
    };
  }, [id]);

  const receipt = useMemo(() => {
    if (!data || typeof data !== 'object') return { rows: [], headerAmount: null, sign: null };

    const preferred = [
      'transactionId',
      'id',
      'transactionDate',
      'transactionTypeName',
      'transactionTypeCode',
      'status',
      'sign',
      'amount',
      'currencyCode',
      'originalAmount',
      'originalCurrencyCode',
      'exchangeRate',
      'senderIban',
      'receiverIban',
      'fromIban',
      'toIban',
      'accountIban',
      'destinationIban',
      'destinationAccountIban',
      'accountId',
      'destinationAccountId',
      'clientId',
      'merchant',
      'reference',
      'description',
      'details',
      'createdAt',
      'updatedAt',
    ];

    const used = new Set();
    const rows = [];

    const addRow = (key) => {
      if (used.has(key) || !(key in data)) return;
      const formatted = formatValue(key, data[key], data);
      if (formatted === null) return;
      used.add(key);
      rows.push({
        key,
        label: LABELS[key] || humanizeKey(key),
        value: formatted,
        mono: /(id|iban|reference|token|intent)/i.test(key),
      });
    };

    preferred.forEach(addRow);
    Object.keys(data)
      .filter((k) => !used.has(k))
      .filter((k) => !k.startsWith('_'))
      .sort()
      .forEach(addRow);

    const amount = formatMoney(data.amount, data.currencyCode || data.originalCurrencyCode);
    const sign = data.sign === '+' ? '+' : data.sign === '-' ? '-' : '';

    return {
      rows,
      sign,
      headerAmount: amount ? `${sign}${amount}` : null,
    };
  }, [data]);

  return (
    <ModalShell
      title="Transaction receipt"
      subtitle={`Transaction ID: ${id}`}
      onClose={onClose}
      maxWidth="max-w-2xl"
    >
      <div className="flex items-center justify-between gap-4 mb-4">
        <div className="flex items-center gap-2 text-zinc-300">
          <Receipt className="w-5 h-5 text-emerald-400" />
          <span className="text-sm">Receipt details</span>
        </div>
        {receipt.headerAmount ? (
          <p className={`text-lg font-bold ${receipt.sign === '+' ? 'text-emerald-400' : receipt.sign === '-' ? 'text-red-400' : 'text-zinc-200'}`}>
            {receipt.headerAmount}
          </p>
        ) : null}
      </div>

      {error && (
        <div className="mb-4 p-3 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-sm">{error}</div>
      )}

      {loading ? (
        <div className="glass rounded-xl p-8 text-center">
          <div className="flex items-center justify-center gap-2 text-zinc-400">
            <Loader2 className="w-4 h-4 animate-spin" />
            Loading receipt...
          </div>
        </div>
      ) : !data || receipt.rows.length === 0 ? (
        <div className="glass rounded-xl p-8 text-center">
          <p className="text-zinc-400">No receipt data.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {receipt.rows.map((row) => (
            <ReceiptField key={row.key} label={row.label} value={row.value} mono={row.mono} />
          ))}
        </div>
      )}
    </ModalShell>
  );
}

