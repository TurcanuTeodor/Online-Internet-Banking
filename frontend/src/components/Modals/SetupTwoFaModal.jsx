import { useState, useEffect } from 'react';
import QRCode from 'qrcode';
import { confirm2FA } from '@/services/authService';
import { toast } from 'sonner';

export default function SetupTwoFaModal({ twoFaSetup, onClose, onSuccess }) {
  const [qrCodeDataUrl, setQrCodeDataUrl] = useState(null);
  const [twoFaCode, setTwoFaCode] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (twoFaSetup?.otpauthUrl) {
      QRCode.toDataURL(twoFaSetup.otpauthUrl)
        .then(url => setQrCodeDataUrl(url))
        .catch(err => {
          console.error('Failed to generate QR code:', err);
          toast.error('Failed to generate QR code');
        });
    }
  }, [twoFaSetup]);

  const handleConfirm2FA = async () => {
    setLoading(true);
    try {
      await confirm2FA(twoFaCode);
      toast.success('2FA enabled successfully!');
      onSuccess();
      onClose();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Invalid code');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50" onClick={onClose}>
      <div className="glass rounded-2xl p-6 max-w-md w-full animate-fade-in" onClick={(e) => e.stopPropagation()}>
        <h3 className="text-xl font-bold mb-4">Setup Two-Factor Authentication</h3>
        <p className="text-zinc-400 text-sm mb-4">Scan this QR code with your authenticator app (Google Authenticator, Authy, etc.)</p>
        <div className="bg-white p-4 rounded-xl mb-4 flex justify-center">
          {qrCodeDataUrl ? <img src={qrCodeDataUrl} alt="2FA QR Code" className="w-48 h-48" /> : <div className="w-48 h-48 flex items-center justify-center bg-gray-100 text-gray-500 rounded text-sm">Loading QR...</div>}
        </div>
        <p className="text-zinc-400 text-xs mb-2">Or enter this secret manually:</p>
        <p className="font-mono text-sm bg-zinc-800 p-2 rounded mb-4 break-all">{twoFaSetup?.secret}</p>
        <label className="block text-sm font-medium text-zinc-400 mb-2">Enter verification code</label>
        <input type="text" value={twoFaCode} onChange={(e) => setTwoFaCode(e.target.value)} className="input-field mb-4" placeholder="000000" maxLength={6} disabled={loading} />
        <div className="flex gap-3">
          <button onClick={onClose} disabled={loading} className="btn-secondary flex-1">Cancel</button>
          <button onClick={handleConfirm2FA} disabled={loading || twoFaCode.length < 6} className="btn-primary flex-1">
            {loading ? 'Confirming...' : 'Confirm'}
          </button>
        </div>
      </div>
    </div>
  );
}
