import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { register } from '../../services/authService';
import { signUpClientProfile } from '../../services/clientService';
import { UserPlus, Loader2, Lock, Eye, EyeOff, AlertCircle } from 'lucide-react';

function StrengthBar({ password }) {
  const checks = [
    password.length >= 8,
    /[A-Z]/.test(password),
    /[0-9]/.test(password),
    /[!@#$%^&*]/.test(password),
  ];
  const score = checks.filter(Boolean).length;
  const colors = ['', '#ef4444', '#f97316', '#eab308', '#22c55e'];
  const labels = ['', 'Weak', 'Fair', 'Good', 'Strong'];

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
      <p className="text-xs" style={{ color: colors[score] || '#71717a' }}>
        {labels[score] || 'Enter password'}
      </p>
    </div>
  );
}

export default function Register() {
  const [formData, setFormData] = useState({
    firstName: '', lastName: '',
    sexCode: 'M', clientTypeCode: 'PF',
    usernameOrEmail: '', password: '', confirmPassword: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => setFormData({ ...formData, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const client = await signUpClientProfile({
        firstName: formData.firstName,
        lastName: formData.lastName,
        sexCode: formData.sexCode,
        clientTypeCode: formData.clientTypeCode,
      });
      if (!client?.id) { setError('Could not create client profile. Try again.'); return; }

      await register({
        clientId: client.id,
        firstName: formData.firstName,
        lastName: formData.lastName,
        sexCode: formData.sexCode,
        clientTypeCode: formData.clientTypeCode,
        usernameOrEmail: formData.usernameOrEmail,
        password: formData.password,
      });

      navigate('/login', { state: { message: 'Account created! Please sign in.' } });
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-gradient-to-br from-slate-950 via-zinc-900 to-slate-950 relative overflow-hidden">
      <div className="absolute top-1/4 -left-48 w-96 h-96 bg-emerald-500/8 rounded-full blur-3xl animate-glow-pulse pointer-events-none" />
      <div className="absolute bottom-1/4 -right-48 w-96 h-96 bg-emerald-500/8 rounded-full blur-3xl animate-glow-pulse pointer-events-none" style={{ animationDelay: '2s' }} />

      <div className="w-full max-w-md relative z-10 animate-slide-up">
        <div className="text-center mb-8">
          <div className="w-16 h-16 bg-gradient-to-br from-emerald-500/30 to-emerald-600/20 border border-emerald-500/20 rounded-2xl flex items-center justify-center mx-auto mb-4">
            <UserPlus className="w-7 h-7 text-emerald-400" />
          </div>
          <h1 className="text-3xl font-bold tracking-tight">Create account</h1>
          <p className="text-zinc-500 mt-1.5 text-sm">Join CashTactics today</p>
        </div>

        <div className="glass rounded-2xl p-8 shadow-2xl">
          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Name row */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-zinc-500 mb-1.5">First Name</label>
                <input type="text" name="firstName" value={formData.firstName} onChange={handleChange}
                  className="input-field" placeholder="John" required />
              </div>
              <div>
                <label className="block text-xs font-medium text-zinc-500 mb-1.5">Last Name</label>
                <input type="text" name="lastName" value={formData.lastName} onChange={handleChange}
                  className="input-field" placeholder="Doe" required />
              </div>
            </div>

            {/* Gender + Type row */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-zinc-500 mb-1.5">Gender</label>
                <select name="sexCode" value={formData.sexCode} onChange={handleChange} className="input-field" required>
                  <option value="M">Male</option>
                  <option value="F">Female</option>
                  <option value="O">Other</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium text-zinc-500 mb-1.5">Account Type</label>
                <select name="clientTypeCode" value={formData.clientTypeCode} onChange={handleChange} className="input-field" required>
                  <option value="PF">Personal</option>
                  <option value="PJ">Business</option>
                </select>
              </div>
            </div>

            {/* Email */}
            <div>
              <label className="block text-xs font-medium text-zinc-500 mb-1.5">Email or Username</label>
              <input type="text" name="usernameOrEmail" value={formData.usernameOrEmail} onChange={handleChange}
                className="input-field" placeholder="your@email.com" required />
            </div>

            {/* Password */}
            <div>
              <label className="block text-xs font-medium text-zinc-500 mb-1.5">Password</label>
              <div className="relative">
                <input type={showPassword ? 'text' : 'password'} name="password"
                  value={formData.password} onChange={handleChange}
                  className="input-field pr-11" placeholder="Min. 8 characters" required minLength={8} />
                <button type="button" onClick={() => setShowPassword(s => !s)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-500 hover:text-zinc-300 transition-colors" tabIndex={-1}>
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              <StrengthBar password={formData.password} />
            </div>

            {/* Confirm Password */}
            <div>
              <label className="block text-xs font-medium text-zinc-500 mb-1.5">Confirm Password</label>
              <div className="relative">
                <input type={showConfirm ? 'text' : 'password'} name="confirmPassword"
                  value={formData.confirmPassword} onChange={handleChange}
                  className="input-field pr-11" placeholder="Repeat password" required />
                <button type="button" onClick={() => setShowConfirm(s => !s)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-500 hover:text-zinc-300 transition-colors" tabIndex={-1}>
                  {showConfirm ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {formData.confirmPassword && formData.password !== formData.confirmPassword && (
                <p className="text-xs text-red-400 mt-1 flex items-center gap-1">
                  <AlertCircle className="w-3 h-3" /> Passwords don&apos;t match
                </p>
              )}
            </div>

            {error && (
              <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm flex items-start gap-2 animate-fade-in">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                {error}
              </div>
            )}

            <button type="submit" disabled={loading} className="w-full btn-primary flex items-center justify-center gap-2 py-3 mt-1">
              {loading ? (
                <><Loader2 className="w-5 h-5 animate-spin" /> Creating account…</>
              ) : (
                <><UserPlus className="w-5 h-5" /> Create Account</>
              )}
            </button>
          </form>

          <div className="mt-5 pt-5 border-t border-white/5 text-center">
            <p className="text-zinc-500 text-sm">
              Already have an account?{' '}
              <Link to="/login" className="text-emerald-400 hover:text-emerald-300 transition-colors font-medium">
                Sign in
              </Link>
            </p>
          </div>
        </div>

        <p className="text-zinc-600 text-xs text-center mt-5 flex items-center justify-center gap-1.5">
          <Lock className="w-3 h-3" />
          CashTactics © 2026 — End-to-end encrypted
        </p>
      </div>
    </div>
  );
}
