import { useState } from 'react';
import { Eye, ShieldAlert } from 'lucide-react';
import ModalShell from '@/components/ModalShell';

const REASON_OPTIONS = [
  { value: 'DISPUTE_INVESTIGATION', label: 'Customer dispute investigation' },
  { value: 'FRAUD_REVIEW', label: 'Fraud / suspicious activity review' },
  { value: 'REGULATORY_AUDIT', label: 'Regulatory or compliance audit' },
  { value: 'SUPPORT_REQUEST', label: 'Customer support request handling' },
  { value: 'RECONCILIATION', label: 'Ledger / transaction reconciliation' },
  { value: 'OTHER', label: 'Other (provide details)' },
];

export default function RevealSensitiveDataModal({ scopeLabel, onClose, onConfirm, loading = false }) {
  const [reasonCode, setReasonCode] = useState('');
  const [reasonDetails, setReasonDetails] = useState('');

  const selectedReason = REASON_OPTIONS.find((opt) => opt.value === reasonCode);
  const requiresDetails = reasonCode === 'OTHER';
  const detailsTrimmed = reasonDetails.trim();
  const canSubmit = reasonCode && (!requiresDetails || detailsTrimmed.length >= 8);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!canSubmit || !selectedReason) return;

    await onConfirm({
      reasonCode,
      reasonDetails: detailsTrimmed,
    });
    setReasonCode('');
    setReasonDetails('');
  };

  return (
    <ModalShell
      title="Reveal sensitive financial data?"
      subtitle={`Scope: ${scopeLabel}`}
      onClose={onClose}
      maxWidth="max-w-lg"
      footer={(
        <>
          <button type="button" onClick={onClose} className="btn-secondary">
            Cancel
          </button>
          <button type="submit" form="reveal-sensitive-data-form" disabled={loading || !canSubmit} className="btn-primary flex items-center gap-2">
            <Eye className="w-4 h-4" />
            {loading ? 'Logging...' : 'Reveal data'}
          </button>
        </>
      )}
    >
      <div className="mb-4 flex items-start gap-3 rounded-xl border border-amber-500/20 bg-amber-500/10 p-4 text-sm text-amber-200">
        <ShieldAlert className="mt-0.5 w-5 h-5 shrink-0 text-amber-300" />
        <p>
          This action is audited. Select a reason category and add details when needed. The reveal event will be logged with your admin identity.
        </p>
      </div>

      <form id="reveal-sensitive-data-form" onSubmit={handleSubmit}>
        <label className="block text-sm font-medium text-zinc-400 mb-2">Reason category</label>
        <select
          value={reasonCode}
          onChange={(e) => setReasonCode(e.target.value)}
          className="input-field mb-4"
          required
        >
          <option value="" disabled>
            Select why you need to reveal sensitive data
          </option>
          {REASON_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>

        <label className="block text-sm font-medium text-zinc-400 mb-2">
          Additional details {requiresDetails ? '(required, min 8 chars)' : '(optional)'}
        </label>
        <textarea
          value={reasonDetails}
          onChange={(e) => setReasonDetails(e.target.value)}
          className="input-field resize-none"
          rows={4}
          minLength={requiresDetails ? 8 : 0}
          required={requiresDetails}
          placeholder="Example: Case #12345, chargeback review opened by support"
        />
      </form>
    </ModalShell>
  );
}