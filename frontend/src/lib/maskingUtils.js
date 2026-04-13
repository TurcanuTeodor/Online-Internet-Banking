export function maskIban(iban) {
  if (!iban) return '—';
  const str = String(iban).trim();
  if (str.length <= 8) return `**** ${str.slice(-4)}`;
  const head = str.substring(0, 8);
  const tail = str.slice(-4);
  return `${head} **** **** **** ${tail}`;
}

export function maskEmail(email) {
  if (!email) return '—';
  const str = String(email).trim();
  const parts = str.split('@');
  if (parts.length !== 2) return str;
  const [user, domain] = parts;
  if (!user || !domain) return str;
  if (user.length <= 2) return `${user.charAt(0)}***@${domain}`;
  return `${user.substring(0, 2)}***@${domain}`;
}

export function maskMoneyValue() {
  return '***.**';
}
