import { createElement } from 'react';
import clsx from 'clsx';
import {
  ArrowDownToLine, ArrowLeftRight, ArrowUpFromLine, Banknote, Car, CircleDot,
  CreditCard, Film, GraduationCap, HeartPulse, Receipt, Send, ShoppingBag,
  ShoppingCart, UtensilsCrossed, Zap,
} from 'lucide-react';

const ICON_MAP = {
  ArrowDownToLine, ArrowLeftRight, ArrowUpFromLine, Banknote, Car, CircleDot,
  CreditCard, Film, GraduationCap, HeartPulse, Receipt, Send, ShoppingBag,
  ShoppingCart, UtensilsCrossed, Zap,
};

export const TRANSACTION_TYPE_CONFIG = {
  DEPOSIT: { iconName: 'ArrowDownToLine', color: 'text-emerald-400', label: 'Deposit' },
  WITHDRAWAL: { iconName: 'ArrowUpFromLine', color: 'text-rose-400', label: 'Withdrawal' },
  TRANSFER_INTERNAL: { iconName: 'ArrowLeftRight', color: 'text-sky-400', label: 'Internal transfer' },
  TRANSFER_EXTERNAL: { iconName: 'Send', color: 'text-blue-400', label: 'External transfer' },
  PAYMENT: { iconName: 'Receipt', color: 'text-rose-300', label: 'Payment' },
  TOP_UP: { iconName: 'CreditCard', color: 'text-emerald-300', label: 'Card top-up' },
};

export const CATEGORY_CONFIG = {
  groceries: { iconName: 'ShoppingCart', color: 'text-lime-400', label: 'Groceries' },
  transport: { iconName: 'Car', color: 'text-cyan-400', label: 'Transport' },
  salary: { iconName: 'Banknote', color: 'text-emerald-400', label: 'Salary' },
  entertainment: { iconName: 'Film', color: 'text-fuchsia-400', label: 'Entertainment' },
  utilities: { iconName: 'Zap', color: 'text-amber-400', label: 'Utilities' },
  dining: { iconName: 'UtensilsCrossed', color: 'text-orange-400', label: 'Dining' },
  shopping: { iconName: 'ShoppingBag', color: 'text-violet-400', label: 'Shopping' },
  health: { iconName: 'HeartPulse', color: 'text-pink-400', label: 'Health' },
  education: { iconName: 'GraduationCap', color: 'text-indigo-400', label: 'Education' },
  other: { iconName: 'CircleDot', color: 'text-zinc-400', label: 'Other' },
};

const DEFAULT_TYPE = { iconName: 'CircleDot', color: 'text-zinc-400', label: 'Transaction' };

export function resolveTransactionTypeKey(tx) {
  const raw = tx?.transactionType ?? tx?.transactionTypeName ?? tx?.type ?? '';
  let s = String(raw).trim().toUpperCase().replace(/\s+/g, '_');
  if ((s.includes('TOP') && s.includes('UP')) || s.includes('TOPUP') || s === 'CARD_TOP_UP') return 'TOP_UP';
  if (TRANSACTION_TYPE_CONFIG[s]) return s;
  if (s.includes('TRANSFER')) return s.includes('INTERNAL') ? 'TRANSFER_INTERNAL' : 'TRANSFER_EXTERNAL';
  if (s.includes('DEPOSIT')) return 'DEPOSIT';
  if (s.includes('WITHDRAW')) return 'WITHDRAWAL';
  if (s.includes('PAYMENT')) return 'PAYMENT';
  return 'OTHER';
}

export function getTransactionIcon(tx) {
  const cat = String(tx?.category ?? tx?.transactionCategory ?? '').trim().toLowerCase();
  if (cat && CATEGORY_CONFIG[cat]) {
    const cfg = CATEGORY_CONFIG[cat];
    return { Icon: ICON_MAP[cfg.iconName] ?? CircleDot, color: cfg.color, label: cfg.label };
  }
  const typeKey = resolveTransactionTypeKey(tx);
  const cfg = TRANSACTION_TYPE_CONFIG[typeKey] || DEFAULT_TYPE;
  return { Icon: ICON_MAP[cfg.iconName] ?? CircleDot, color: cfg.color, label: cfg.label };
}

export function TransactionIcon({ transaction, className, size = 18 }) {
  const { Icon, color } = getTransactionIcon(transaction);
  return createElement(
    'span',
    { className: clsx('inline-flex shrink-0 items-center justify-center rounded-xl bg-zinc-800/90 p-2 ring-1 ring-white/5', className) },
    createElement(Icon, { className: color, size, strokeWidth: 2 })
  );
}
