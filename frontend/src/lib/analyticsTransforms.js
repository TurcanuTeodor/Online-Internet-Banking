function toNumber(value) {
  const n = Number.parseFloat(value);
  return Number.isFinite(n) ? n : 0;
}

function toDate(value) {
  if (!value) return null;
  if (Array.isArray(value)) {
    const [y, m, d, h = 0, min = 0, s = 0] = value;
    const date = new Date(y, m - 1, d, h, min, s);
    return Number.isNaN(date.getTime()) ? null : date;
  }
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? null : d;
}

function dayKey(dateValue) {
  const d = toDate(dateValue);
  if (!d) return '';
  return d.toISOString().slice(0, 10);
}

function normalizeTransactionType(tx) {
  const raw = tx?.transactionTypeName || tx?.transactionTypeCode || tx?.transactionType || tx?.type;
  if (!raw) return 'Other';
  return String(raw)
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

function txAmount(tx) {
  return toNumber(tx?.amount ?? tx?.transactionAmount);
}

function txSign(tx) {
  const explicit = String(tx?.sign ?? tx?.transactionSign ?? '').trim();
  if (explicit === '+' || explicit === '-') return explicit;
  return txAmount(tx) < 0 ? '-' : '+';
}

function normalizeRiskScore(raw) {
  const n = toNumber(raw);
  if (n <= 0) return 0;
  const pct = n > 1 ? n : n * 100;
  if (pct < 0) return 0;
  if (pct > 100) return 100;
  return pct;
}

function formatDayLabel(isoDay) {
  const d = toDate(isoDay);
  if (!d) return isoDay;
  return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short' });
}

export function filterTransactionsByLastDays(transactions, days = 30) {
  const safeDays = Number.isInteger(days) && days > 0 ? days : 30;
  const start = new Date();
  start.setHours(0, 0, 0, 0);
  start.setDate(start.getDate() - (safeDays - 1));

  return (Array.isArray(transactions) ? transactions : []).filter((tx) => {
    const d = toDate(tx?.transactionDate);
    return d && d >= start;
  });
}

export function prepareCashflowData(transactions, days = 30) {
  const safeDays = Number.isInteger(days) && days > 0 ? days : 30;
  const now = new Date();
  const dayList = Array.from({ length: safeDays }, (_, idx) => {
    const d = new Date(now);
    d.setDate(now.getDate() - (safeDays - 1 - idx));
    const iso = d.toISOString().slice(0, 10);
    return {
      date: iso,
      label: formatDayLabel(iso),
      inflow: 0,
      outflow: 0,
    };
  });

  const byDay = new Map(dayList.map((x) => [x.date, x]));
  (Array.isArray(transactions) ? transactions : []).forEach((tx) => {
    const key = dayKey(tx?.transactionDate);
    if (!byDay.has(key)) return;
    const amount = Math.abs(txAmount(tx));
    if (amount <= 0) return;
    const sign = txSign(tx);
    const bucket = byDay.get(key);
    if (sign === '-') bucket.outflow -= amount;
    else bucket.inflow += amount;
  });

  return dayList;
}

export function prepareExpenseCompositionData(transactions) {
  const totals = new Map();
  let totalSpent = 0;

  (Array.isArray(transactions) ? transactions : []).forEach((tx) => {
    const amount = Math.abs(txAmount(tx));
    if (amount <= 0 || txSign(tx) !== '-') return;
    const type = normalizeTransactionType(tx);
    totals.set(type, (totals.get(type) || 0) + amount);
    totalSpent += amount;
  });

  const segments = Array.from(totals.entries())
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value);

  return { segments, totalSpent };
}

export function prepareScatterAnomalyData(transactions) {
  // Kept for backward compatibility (older imports). Prefer passing alerted ids via the new signature.
  return prepareScatterAnomalyDataWithAlerts(transactions, new Set());
}

function txIdForJoin(tx) {
  return tx?.transactionId ?? tx?.id ?? null;
}

export function prepareScatterAnomalyDataWithAlerts(transactions, alertedTransactionIds) {
  const alertedIds = alertedTransactionIds instanceof Set ? alertedTransactionIds : new Set();
  return (Array.isArray(transactions) ? transactions : []).map((tx) => {
    const amount = Math.abs(txAmount(tx));
    const riskScore = normalizeRiskScore(tx?.riskScore);
    const flagged = Boolean(tx?.flagged);
    const txId = txIdForJoin(tx);
    const alerted = txId != null && alertedIds.has(txId);
    return {
      id: txId ?? `${tx?.transactionDate || 'tx'}-${amount}-${riskScore}`,
      amount,
      riskScore,
      flagged,
      alerted,
      highRisk: riskScore > 70 || flagged || alerted,
      type: normalizeTransactionType(tx),
      date: tx?.transactionDate || null,
      sign: txSign(tx),
      accountId: tx?.accountId ?? null,
      destinationAccountId: tx?.destinationAccountId ?? null,
      merchant: tx?.merchant ?? null,
    };
  });
}

export function prepareHighRiskOverTimeData(transactions, days = 30) {
  const safeDays = Number.isInteger(days) && days > 0 ? days : 30;
  const now = new Date();
  const dayList = Array.from({ length: safeDays }, (_, idx) => {
    const d = new Date(now);
    d.setDate(now.getDate() - (safeDays - 1 - idx));
    const iso = d.toISOString().slice(0, 10);
    return {
      date: iso,
      label: formatDayLabel(iso),
      highRiskCount: 0,
    };
  });

  const byDay = new Map(dayList.map((x) => [x.date, x]));
  (Array.isArray(transactions) ? transactions : []).forEach((tx) => {
    const risk = normalizeRiskScore(tx?.riskScore);
    const flagged = Boolean(tx?.flagged);
    if (risk <= 70 && !flagged) return;
    const key = dayKey(tx?.transactionDate);
    if (!byDay.has(key)) return;
    byDay.get(key).highRiskCount += 1;
  });

  return dayList;
}

export function prepareHighRiskOverTimeDataWithAlerts(transactions, days = 30, alertedTransactionIds) {
  const safeDays = Number.isInteger(days) && days > 0 ? days : 30;
  const alertedIds = alertedTransactionIds instanceof Set ? alertedTransactionIds : new Set();

  const now = new Date();
  const dayList = Array.from({ length: safeDays }, (_, idx) => {
    const d = new Date(now);
    d.setDate(now.getDate() - (safeDays - 1 - idx));
    const iso = d.toISOString().slice(0, 10);
    return {
      date: iso,
      label: formatDayLabel(iso),
      highRiskCount: 0,
    };
  });

  const byDay = new Map(dayList.map((x) => [x.date, x]));
  (Array.isArray(transactions) ? transactions : []).forEach((tx) => {
    const risk = normalizeRiskScore(tx?.riskScore);
    const flagged = Boolean(tx?.flagged);
    const txId = txIdForJoin(tx);
    const alerted = txId != null && alertedIds.has(txId);
    if (risk <= 70 && !flagged && !alerted) return;
    const key = dayKey(tx?.transactionDate);
    if (!byDay.has(key)) return;
    byDay.get(key).highRiskCount += 1;
  });

  return dayList;
}

export function prepareClientRiskDistributionData(clients) {
  const base = { LOW: 0, MEDIUM: 0, HIGH: 0, CRITICAL: 0 };
  (Array.isArray(clients) ? clients : []).forEach((client) => {
    const raw = (client?.riskLevel || 'LOW').toString().trim().toUpperCase();
    if (Object.prototype.hasOwnProperty.call(base, raw)) base[raw] += 1;
  });

  return Object.entries(base).map(([level, value]) => ({ level, value }));
}

export function prepareAlertStatusDistributionData(fraudAlerts) {
  const totals = {};
  (Array.isArray(fraudAlerts) ? fraudAlerts : []).forEach((alert) => {
    const raw = (alert?.status || 'UNKNOWN').toString().trim().toUpperCase();
    totals[raw] = (totals[raw] || 0) + 1;
  });

  return Object.entries(totals)
    .map(([level, value]) => ({ level, value }))
    .sort((a, b) => b.value - a.value);
}

// ----------------------------------------------------------------------------
// NEW FINTECH & BUSINESS KPIs (Admin + User)
// ----------------------------------------------------------------------------

const STATIC_RATES_TO_EUR = {
  EUR: 1,
  USD: 0.91,
  GBP: 1.17,
  RON: 0.20,
};

export function preparePlatformVolumeData(transactions, days = 30) {
  const safeDays = Number.isInteger(days) && days > 0 ? days : 30;
  const now = new Date();
  const dayList = Array.from({ length: safeDays }, (_, idx) => {
    const d = new Date(now);
    d.setDate(now.getDate() - (safeDays - 1 - idx));
    const iso = d.toISOString().slice(0, 10);
    return {
      date: iso,
      label: formatDayLabel(iso),
      volumeEUR: 0,
      txCount: 0,
    };
  });

  const byDay = new Map(dayList.map((x) => [x.date, x]));
  let totalTxCount = 0;

  (Array.isArray(transactions) ? transactions : []).forEach((tx) => {
    const key = dayKey(tx?.transactionDate);
    if (!byDay.has(key)) return;
    
    const amount = Math.abs(txAmount(tx));
    if (amount <= 0) return;

    // Best effort cross-currency conversion
    const currency = (tx?.currencyCode || tx?.currency || 'EUR').toUpperCase();
    const rate = STATIC_RATES_TO_EUR[currency] || 1;
    const volumeInEur = amount * rate;

    const bucket = byDay.get(key);
    bucket.volumeEUR += volumeInEur;
    bucket.txCount += 1;
    totalTxCount += 1;
  });

  return { volumeData: dayList, totalTxCount };
}

export function prepareTopMerchants(transactions, limit = 3) {
  const merchantTotals = new Map();

  (Array.isArray(transactions) ? transactions : []).forEach((tx) => {
    // Only count outflows
    if (txSign(tx) !== '-') return;
    const amount = Math.abs(txAmount(tx));
    if (amount <= 0) return;

    // Use merchant field first; fall back to details (e.g. "Transfer to IBAN"
    // or "Groceries shopping"), which doubles as a recipient/description label.
    const merchant = 
      (tx?.merchant && tx.merchant.trim()) || 
      (tx?.details && tx.details.trim()) || 
      'Unknown Merchant';

    // Normalize slightly to group casing discrepancies
    const normalizedMerchant = merchant.toUpperCase();

    merchantTotals.set(normalizedMerchant, {
      name: merchant,
      total: (merchantTotals.get(normalizedMerchant)?.total || 0) + amount,
    });
  });

  const sortedList = Array.from(merchantTotals.values())
    .sort((a, b) => b.total - a.total)
    .slice(0, limit);

  return sortedList;
}

export function prepareTransactionTypeDistribution(transactions) {
  const totals = new Map();

  (Array.isArray(transactions) ? transactions : []).forEach((tx) => {
    const type = normalizeTransactionType(tx);
    // Grouping by raw count of transaction types
    totals.set(type, (totals.get(type) || 0) + 1);
  });

  return Array.from(totals.entries())
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value);
}

// ----------------------------------------------------------------------------
// FRAUD ALERT-BASED CHART TRANSFORMS
// Used by FraudCommandCenter — fraud alerts have amount/riskScore directly,
// so we don't need to join with transactions by transactionId.
// ----------------------------------------------------------------------------

/**
 * Converts an array of fraud alert DTOs into scatter plot data points.
 * Each point = { amount, riskScore, highRisk, status, accountId, date, currencyCode, id }
 */
export function prepareAlertScatterData(fraudAlerts) {
  return (Array.isArray(fraudAlerts) ? fraudAlerts : []).map((alert) => {
    const amount = toNumber(alert?.amount);
    const riskScore = normalizeRiskScore(alert?.riskScore);
    const status = String(alert?.status || '');
    const highRisk = riskScore > 70 || status === 'BLOCK' || status === 'FLAG' || status === 'STEP_UP_REQUIRED';
    return {
      id: alert?.id ?? `alert-${amount}-${riskScore}`,
      amount,
      riskScore,
      highRisk,
      status,
      accountId: alert?.accountId ?? null,
      currencyCode: alert?.currencyCode ?? 'EUR',
      date: alert?.createdAt ?? null,
      // For tooltip compatibility with ScatterTooltip
      type: 'Fraud Alert',
      sign: '-',
      flagged: highRisk,
    };
  });
}

/**
 * Builds a daily high-risk alert count for the line chart.
 * Counts fraud alerts (FLAG, BLOCK, STEP_UP_REQUIRED) per day over the last `days` days.
 */
export function prepareAlertRiskOverTime(fraudAlerts, days = 30) {
  const safeDays = Number.isInteger(days) && days > 0 ? days : 30;
  const now = new Date();
  const dayList = Array.from({ length: safeDays }, (_, idx) => {
    const d = new Date(now);
    d.setDate(now.getDate() - (safeDays - 1 - idx));
    const iso = d.toISOString().slice(0, 10);
    return {
      date: iso,
      label: formatDayLabel(iso),
      highRiskCount: 0,
    };
  });

  const byDay = new Map(dayList.map((x) => [x.date, x]));
  const alertStatuses = new Set(['FLAG', 'BLOCK', 'STEP_UP_REQUIRED', 'MANUAL_REVIEW']);

  (Array.isArray(fraudAlerts) ? fraudAlerts : []).forEach((alert) => {
    const status = String(alert?.status || '');
    if (!alertStatuses.has(status)) return;
    const key = dayKey(alert?.createdAt);
    if (!byDay.has(key)) return;
    byDay.get(key).highRiskCount += 1;
  });

  return dayList;
}

