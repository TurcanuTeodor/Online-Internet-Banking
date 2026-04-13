import { useState, useMemo } from 'react';
import { Download, Trash2, Loader2, ShieldCheck, AlertTriangle } from 'lucide-react';
import { jwtDecode } from 'jwt-decode';
import apiClient from '@/services/apiClient';
import ConfirmDialog from './ConfirmDialog';

export default function GdprSection() {
  const [exportLoading, setExportLoading] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');

  const clientId = useMemo(() => {
    try {
      const token = sessionStorage.getItem('jwt_token');
      if (token) return jwtDecode(token).clientId;
    } catch { /* ignore */ }
    return null;
  }, []);

  const handleExport = async () => {
    if (!clientId) return;
    setExportLoading(true);
    setError('');
    try {
      const res = await apiClient.get(`/gdpr/clients/${clientId}/export`);
      const blob = new Blob([JSON.stringify(res.data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `cashtactics-data-export-${clientId}.json`;
      a.click();
      URL.revokeObjectURL(url);
      setSuccess('Your data has been downloaded.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to export data.');
    } finally {
      setExportLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!clientId) return;
    setDeleteLoading(true);
    setError('');
    try {
      await apiClient.delete(`/gdpr/clients/${clientId}/delete`);
      setSuccess('Account deletion requested. You will be logged out.');
      setShowDeleteConfirm(false);
      setTimeout(() => {
        sessionStorage.removeItem('jwt_token');
        sessionStorage.removeItem('refresh_token');
        window.location.href = '/login';
      }, 3000);
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to request account deletion.');
    } finally {
      setDeleteLoading(false);
    }
  };

  return (
    <section className="glass rounded-2xl p-6 animate-fade-in">
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 bg-blue-500/15 rounded-xl flex items-center justify-center">
          <ShieldCheck className="w-5 h-5 text-blue-400" />
        </div>
        <div>
          <h2 className="text-lg font-bold">Privacy & Data (GDPR)</h2>
          <p className="text-sm text-zinc-500">Export your data or request account deletion.</p>
        </div>
      </div>

      {success && (
        <div className="mb-4 p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-emerald-400 text-sm">
          {success}
        </div>
      )}
      {error && (
        <div className="mb-4 p-3 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm">
          {error}
        </div>
      )}

      <div className="space-y-4">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 p-4 rounded-xl border border-white/5 bg-zinc-900/40">
          <div>
            <p className="text-sm font-medium text-zinc-200">Download my data</p>
            <p className="text-xs text-zinc-500 mt-0.5">Export all your personal data in JSON format.</p>
          </div>
          <button onClick={handleExport} disabled={exportLoading} className="btn-secondary flex items-center gap-2 text-sm shrink-0">
            {exportLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />}
            Export data
          </button>
        </div>

        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 p-4 rounded-xl border border-red-500/10 bg-red-500/5">
          <div>
            <p className="text-sm font-medium text-zinc-200">Delete my account</p>
            <p className="text-xs text-zinc-500 mt-0.5">Permanently delete all your data. This cannot be undone.</p>
          </div>
          <button onClick={() => setShowDeleteConfirm(true)} className="btn-danger flex items-center gap-2 text-sm shrink-0">
            <Trash2 className="w-4 h-4" />
            Request deletion
          </button>
        </div>
      </div>

      <ConfirmDialog
        open={showDeleteConfirm}
        title="Delete your account?"
        description="This will permanently erase all your personal data, close your accounts, and anonymize your transaction history. This action cannot be undone."
        confirmLabel="Delete everything"
        danger
        loading={deleteLoading}
        onConfirm={handleDelete}
        onCancel={() => setShowDeleteConfirm(false)}
      />
    </section>
  );
}
