function escapeCsvField(value) {
  if (value === null || value === undefined) return '';
  const str = String(value);
  if (/[",\r\n]/.test(str)) return `"${str.replace(/"/g, '""')}"`;
  return str;
}

function downloadBlob(filename, content, mimeType = 'text/csv;charset=utf-8;') {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename.toLowerCase().endsWith('.csv') ? filename : `${filename}.csv`;
  a.rel = 'noopener';
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

export function exportToCsv(filename, headers, rows) {
  const lines = [headers.map(escapeCsvField).join(',')];
  for (const row of rows) lines.push(row.map(escapeCsvField).join(','));
  downloadBlob(filename, lines.join('\r\n'));
}

function directionLabel(sign) {
  if (sign === '+' || sign === 'CREDIT') return 'Credit';
  if (sign === '-' || sign === 'DEBIT') return 'Debit';
  return String(sign ?? '');
}

export function exportTransactionsToCsv(transactions, filename = 'transactions.csv') {
  const headers = ['Date', 'Type', 'Account', 'Amount', 'Currency', 'Direction', 'Details'];
  const rows = (transactions || []).map((t) => [
    t.transactionDate ?? '', t.transactionTypeName ?? '', t.accountIban ?? '',
    t.amount ?? '', t.currencyCode ?? '', directionLabel(t.sign), t.details ?? '',
  ]);
  exportToCsv(filename, headers, rows);
}

export function exportAccountStatementToCsv(account, transactions) {
  const acc = account || {};
  const lines = [
    ['Field', 'Value'].map(escapeCsvField).join(','),
    ['Account IBAN', acc.iban ?? ''].map(escapeCsvField).join(','),
    ['Currency', acc.currencyCode ?? ''].map(escapeCsvField).join(','),
    ['Current balance', acc.balance ?? ''].map(escapeCsvField).join(','),
    '',
    ['Date', 'Type', 'Account', 'Amount', 'Currency', 'Direction', 'Details'].map(escapeCsvField).join(','),
  ];
  for (const t of (transactions || [])) {
    lines.push([
      t.transactionDate ?? '', t.transactionTypeName ?? '', t.accountIban ?? '',
      t.amount ?? '', t.currencyCode ?? '', directionLabel(t.sign), t.details ?? '',
    ].map(escapeCsvField).join(','));
  }
  const safeIban = String(acc.iban ?? 'statement').replace(/[^a-zA-Z0-9_-]+/g, '_').slice(0, 40);
  downloadBlob(`account-statement_${safeIban}.csv`, lines.join('\r\n'));
}
