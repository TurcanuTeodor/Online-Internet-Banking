import { useState } from 'react';
import { Lock, Eye, EyeOff, Loader2, CheckCircle, AlertCircle, LogOut } from 'lucide-react';
import { changePassword, logout } from '@/services/authService';

function StrengthBar({ password }) {
  const checks = [
    password.length >= 8,
    /[A-Z]/.test(password),
    /[0-9]/.test(password),
    /[!@#$%^&*]/.test(password),
  ];
  const score = checks.filter(Boolean).length;
  const labels = ['', 'Weak', 'Fair', 'Good', 'Strong'];
  const colors = ['', '#ef4444', '#f97316', '#eab308', '#22c55e'];

  if (!password) return null;
  return (
    <div className="mt-2">
      <div className="flex gap-1 mb-1">
        {[1, 2, 3, 4].map((i) => (
          <div
            key={i}
            className="h-1.5 flex-1 rounded-full transition-all duration-300"
            style={{ backgroundColor: i <= score ? colors[score] : 'rgba(63,63,70,0.5)' }}
          />
        ))}
      </div>
      <p className="text-xs" style={{ color: colors[score] }}>
        {labels[score]}
        {score < 4 && (
          <span className="text-zinc-600 ml-1">
            — add {!checks[0] ? '8+ chars' : !checks[1] ? 'uppercase' : !checks[2] ? 'number' : 'special char (!@#$)'}
          </span>
        )}
      </p>
    </div>
  );
}

function PasswordInput({ value, onChange, placeholder, id }) {
  const [show, setShow] = useState(false);
  return (
    <div className="relative">
      <input
        id={id}
        type={show ? 'text' : 'password'}
        value={value}
        onChange={onChange}
        className="input-field pr-11"
        placeholder={placeholder}
        autoComplete="new-password"
      />
      <button
        type="button"
        onClick={() => setShow((s) => !s)}
        className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-500 hover:text-zinc-300 transition-colors"
        tabIndex={-1}
        aria-label={show ? 'Hide password' : 'Show password'}
      >
        {show ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
      </button>
    </div>
  );
}

export default function ChangePasswordCard() {
  const [form, setForm] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [countdown, setCountdown] = useState(5);

  const handleChange = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const validate = () => {
    if (!form.oldPassword) return 'Current password is required.';
    if (form.newPassword.length < 8) return 'New password must be at least 8 characters.';
    if (!/[A-Z]/.test(form.newPassword)) return 'New password must contain an uppercase letter.';
    if (!/[0-9]/.test(form.newPassword)) return 'New password must contain a number.';
    if (!/[!@#$%^&*]/.test(form.newPassword)) return 'Must contain a special character (!@#$%^&*).';
    if (form.newPassword !== form.confirmPassword) return 'Passwords do not match.';
    if (form.oldPassword === form.newPassword) return 'New password must be different from current.';
    return null;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const err = validate();
    if (err) { setError(err); return; }

    setLoading(true);
    setError('');
    try {
      await changePassword(form.oldPassword, form.newPassword);
      setSuccess(true);
      let count = 5;
      setCountdown(count);
      const iv = setInterval(() => {
        count -= 1;
        setCountdown(count);
        if (count <= 0) {
          clearInterval(iv);
          logout();
        }
      }, 1000);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'Failed to change password. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="glass rounded-2xl p-6 animate-fade-in">
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 bg-amber-500/15 rounded-xl flex items-center justify-center">
          <Lock className="w-5 h-5 text-amber-400" />
        </div>
        <div>
          <h2 className="text-lg font-bold">Change Password</h2>
          <p className="text-sm text-zinc-500">Your data will be automatically re-encrypted.</p>
        </div>
      </div>

      {success ? (
        <div className="rounded-xl p-5 bg-emerald-500/10 border border-emerald-500/20 text-center animate-fade-in">
          <CheckCircle className="w-10 h-10 text-emerald-400 mx-auto mb-3" />
          <p className="font-semibold text-emerald-300 mb-1">Password changed successfully!</p>
          <p className="text-sm text-zinc-400 mb-3">
            All your data has been re-encrypted with the new key.
          </p>
          <div className="flex items-center justify-center gap-2 text-zinc-500 text-sm">
            <LogOut className="w-4 h-4" />
            Logging out in <span className="text-emerald-400 font-bold ml-1">{countdown}s</span>
          </div>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="oldPassword" className="block text-sm font-medium text-zinc-400 mb-2">
              Current Password
            </label>
            <PasswordInput
              id="oldPassword"
              value={form.oldPassword}
              onChange={handleChange('oldPassword')}
              placeholder="Enter current password"
            />
          </div>

          <div>
            <label htmlFor="newPassword" className="block text-sm font-medium text-zinc-400 mb-2">
              New Password
            </label>
            <PasswordInput
              id="newPassword"
              value={form.newPassword}
              onChange={handleChange('newPassword')}
              placeholder="Enter new password"
            />
            <StrengthBar password={form.newPassword} />
          </div>

          <div>
            <label htmlFor="confirmPassword" className="block text-sm font-medium text-zinc-400 mb-2">
              Confirm New Password
            </label>
            <PasswordInput
              id="confirmPassword"
              value={form.confirmPassword}
              onChange={handleChange('confirmPassword')}
              placeholder="Repeat new password"
            />
            {form.confirmPassword && form.newPassword !== form.confirmPassword && (
              <p className="text-xs text-red-400 mt-1 flex items-center gap-1">
                <AlertCircle className="w-3 h-3" /> Passwords do not match
              </p>
            )}
          </div>

          {error && (
            <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm flex items-start gap-2 animate-fade-in">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              {error}
            </div>
          )}

          <button type="submit" disabled={loading} className="w-full btn-primary flex items-center justify-center gap-2">
            {loading ? (
              <><Loader2 className="w-4 h-4 animate-spin" /> Changing password &amp; re-encrypting&hellip;</>
            ) : (
              <><Lock className="w-4 h-4" /> Change Password</>
            )}
          </button>

          <p className="text-xs text-zinc-600 text-center">
            After changing, you will be logged out automatically for security.
          </p>
        </form>
      )}
    </section>
  );
}
