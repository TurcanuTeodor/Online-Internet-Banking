import { useEffect, useMemo, useState } from 'react';
import { Loader2, User, Mail, Phone, MapPin, Pencil, Check, X, Building2 } from 'lucide-react';
import { getClientProfile, updateClientContact } from '../../services/clientService';
import { jwtDecode } from 'jwt-decode';

function pick(...values) {
  return values.find((v) => v !== null && v !== undefined && v !== '');
}

function Avatar({ name }) {
  const initials = (name || '?')
    .split(' ')
    .map((w) => w[0])
    .filter(Boolean)
    .slice(0, 2)
    .join('')
    .toUpperCase();

  return (
    <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-emerald-500/30 to-emerald-600/20 border border-emerald-500/20 flex items-center justify-center text-2xl font-bold text-emerald-300 shrink-0">
      {initials}
    </div>
  );
}

function InfoRow(props) {
  const { label, value, accent = 'text-zinc-300' } = props;
  const Icon = props.icon;
  if (!value) return null;
  return (
    <div className="flex items-start gap-3 py-3 border-b border-white/5 last:border-0">
      <Icon className="w-4 h-4 text-zinc-500 mt-0.5 shrink-0" />
      <div className="min-w-0">
        <p className="text-xs text-zinc-600 mb-0.5">{label}</p>
        <p className={`text-sm break-words ${accent}`}>{value}</p>
      </div>
    </div>
  );
}

function EditContactModal({ profile, onClose, onSaved }) {
  const clientId = useMemo(() => {
    try {
      const token = localStorage.getItem('jwt_token');
      if (token) return jwtDecode(token).clientId;
    } catch { /* token decode failed */ }
    return null;
  }, []);

  const [form, setForm] = useState({
    email: pick(profile?.email, profile?.mail) || '',
    phone: pick(profile?.phone, profile?.phoneNumber) || '',
    address: pick(profile?.address) || '',
    city: pick(profile?.city) || '',
    postalCode: pick(profile?.postalCode) || '',
    contactPerson: pick(profile?.contactPerson) || '',
    website: pick(profile?.website) || '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!clientId) { setError('Client ID not found. Please re-login.'); return; }
    setLoading(true);
    setError('');
    try {
      await updateClientContact(clientId, form);
      onSaved();
      onClose();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'Failed to update contact info.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-fade-in" onClick={onClose}>
      <div className="glass rounded-2xl p-6 w-full max-w-md animate-slide-up" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-5">
          <h3 className="text-lg font-bold">Edit Contact Info</h3>
          <button onClick={onClose} className="text-zinc-500 hover:text-white transition-colors"><X className="w-5 h-5" /></button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-3">
          {[
            { label: 'Email', field: 'email', type: 'email', placeholder: 'your@email.com' },
            { label: 'Phone', field: 'phone', type: 'tel', placeholder: '+40 700 000 000' },
            { label: 'Address', field: 'address', type: 'text', placeholder: 'Street address' },
            { label: 'City', field: 'city', type: 'text', placeholder: 'City' },
            { label: 'Postal Code', field: 'postalCode', type: 'text', placeholder: '000000' },
          ].map(({ label, field, type, placeholder }) => (
            <div key={field}>
              <label className="block text-xs font-medium text-zinc-500 mb-1">{label}</label>
              <input
                type={type}
                value={form[field]}
                onChange={handleChange(field)}
                className="input-field"
                placeholder={placeholder}
              />
            </div>
          ))}

          {error && (
            <p className="text-red-400 text-sm p-3 bg-red-500/10 rounded-xl border border-red-500/20">{error}</p>
          )}

          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary flex-1 flex items-center justify-center gap-2 py-2.5">
              <X className="w-4 h-4" /> Cancel
            </button>
            <button type="submit" disabled={loading} className="btn-primary flex-1 flex items-center justify-center gap-2 py-2.5">
              {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />}
              Save
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function ProfileCard() {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showEditModal, setShowEditModal] = useState(false);

  const fetchProfile = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await getClientProfile();
      setProfile(data);
    } catch (e) {
      setError(e?.message || 'Could not load profile');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchProfile(); }, []);

  const fullName = useMemo(() => {
    if (!profile) return null;
    const first = pick(profile.firstName, profile.clientFirstName);
    const last = pick(profile.lastName, profile.clientLastName);
    return [first, last].filter(Boolean).join(' ') || pick(profile.name, profile.fullName) || null;
  }, [profile]);

  const isActive = profile?.active !== false;

  return (
    <>
      <div className="space-y-0">
        {error && (
          <div className="p-4 mb-4 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm flex items-center gap-2">
            <span>{error}</span>
            <button className="ml-auto underline text-xs" onClick={() => setError('')}>Dismiss</button>
          </div>
        )}

        <section className="glass rounded-2xl p-6">
          {/* Header */}
          <div className="flex items-start gap-4 mb-6">
            <Avatar name={fullName || 'U'} />
            <div className="min-w-0 flex-1">
              {loading ? (
                <div className="space-y-2">
                  <div className="skeleton h-5 w-36" />
                  <div className="skeleton h-3 w-24" />
                </div>
              ) : (
                <>
                  <h2 className="text-xl font-bold truncate">{fullName || 'Your Profile'}</h2>
                  <div className="flex items-center gap-2 mt-1 flex-wrap">
                    <span className={`badge ${isActive ? 'badge-active' : 'badge-inactive'}`}>
                      {isActive ? 'Active' : 'Inactive'}
                    </span>
                    {profile?.clientType || profile?.clientTypeName ? (
                      <span className="badge badge-tier1">
                        {pick(profile?.clientType, profile?.clientTypeName, profile?.clientTypeCode)}
                      </span>
                    ) : null}
                  </div>
                </>
              )}
            </div>
          </div>

          {/* Data rows */}
          {loading ? (
            <div className="space-y-3">
              {[1, 2, 3, 4].map((i) => <div key={i} className="skeleton h-10 w-full" />)}
            </div>
          ) : profile ? (
            <div>
              <p className="text-xs font-semibold text-zinc-600 uppercase tracking-wider mb-3">Contact Information</p>
              <InfoRow icon={Mail} label="Email" value={pick(profile.email, profile.mail)} />
              <InfoRow icon={Phone} label="Phone" value={pick(profile.phone, profile.phoneNumber)} />
              <InfoRow icon={MapPin} label="Address"
                value={[pick(profile.address), pick(profile.city), pick(profile.postalCode)].filter(Boolean).join(', ')} />
              {pick(profile.contactPerson) && (
                <InfoRow icon={User} label="Contact Person" value={profile.contactPerson} />
              )}
              {pick(profile.website) && (
                <InfoRow icon={Building2} label="Website" value={profile.website} />
              )}

              <button
                onClick={() => setShowEditModal(true)}
                className="mt-4 flex items-center gap-2 text-sm text-emerald-400 hover:text-emerald-300 transition-colors"
              >
                <Pencil className="w-4 h-4" />
                Edit contact information
              </button>
            </div>
          ) : (
            <p className="text-zinc-500 text-sm">No profile data available.</p>
          )}
        </section>
      </div>

      {showEditModal && profile && (
        <EditContactModal
          profile={profile}
          onClose={() => setShowEditModal(false)}
          onSaved={fetchProfile}
        />
      )}
    </>
  );
}
