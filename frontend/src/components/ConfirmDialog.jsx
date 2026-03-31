import { AlertTriangle, Loader2 } from 'lucide-react';
import ModalShell from './ModalShell';

export default function ConfirmDialog({
  open,
  title = 'Are you sure?',
  description,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  danger = false,
  loading = false,
  onConfirm,
  onCancel,
}) {
  if (!open) return null;

  return (
    <ModalShell title={title} onClose={onCancel} maxWidth="max-w-md">
      {danger && (
        <div className="w-12 h-12 rounded-xl bg-red-500/15 flex items-center justify-center mb-4">
          <AlertTriangle className="w-6 h-6 text-red-400" />
        </div>
      )}
      {description && <p className="text-sm text-zinc-400 mb-6">{description}</p>}
      <div className="flex gap-3">
        <button type="button" onClick={onCancel} disabled={loading} className="btn-secondary flex-1">
          {cancelLabel}
        </button>
        <button
          type="button"
          onClick={onConfirm}
          disabled={loading}
          className={`flex-1 flex items-center justify-center gap-2 ${danger ? 'btn-danger' : 'btn-primary'}`}
        >
          {loading && <Loader2 className="w-4 h-4 animate-spin" />}
          {confirmLabel}
        </button>
      </div>
    </ModalShell>
  );
}
