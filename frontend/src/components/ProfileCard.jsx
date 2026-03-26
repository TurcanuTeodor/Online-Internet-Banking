import { useEffect, useMemo, useState } from 'react';
import { Loader2, User } from 'lucide-react';
import { getClientProfile } from '../../services/clientService';

function pick(...values) {
  return values.find((v) => v !== null && v !== undefined && v !== '');
}

function Field({ label, value, mono = false, span2 = false }) {
  return (
    <div className={`${span2 ? 'md:col-span-2' : ''}`}>
      <p className="text-xs text-zinc-500 mb-1">{label}</p>
      <p className={`text-zinc-200 break-words ${mono ? 'font-mono' : ''}`}>{value ?? '—'}</p>
    </div>
  );
}

export default function ProfileCard({ title = 'Profile', subtitle = null }) {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let mounted = true;
    (async () => {
      setLoading(true);
      setError('');
      try {
        const data = await getClientProfile();
        if (mounted) setProfile(data);
      } catch (e) {
        if (mounted) setError(e?.message || 'Could not load profile');
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    return () => {
      mounted = false;
    };
  }, []);

  const fullName = useMemo(() => {
    if (!profile) return null;
    const first = pick(profile.firstName, profile.clientFirstName);
    const last = pick(profile.lastName, profile.clientLastName);
    return [first, last].filter(Boolean).join(' ') || pick(profile.name, profile.fullName);
  }, [profile]);

  return (
    <div className="space-y-4">
      {error && (
        <div className="p-4 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm">
          {error}
          <button type="button" onClick={() => setError('')} className="ml-3 underline">
            Dismiss
          </button>
        </div>
      )}

      <section className="glass rounded-2xl p-6">
        <div className="flex items-start justify-between gap-4 mb-6">
          <div>
            <h2 className="text-xl font-bold flex items-center gap-2">
              <User className="w-5 h-5 text-emerald-400" />
              {title}
            </h2>
            <p className="text-sm text-zinc-500 mt-1">{subtitle || fullName || 'Your profile details'}</p>
          </div>
          {loading ? (
            <div className="flex items-center gap-2 text-zinc-400 text-sm">
              <Loader2 className="w-4 h-4 animate-spin" /> Loading...
            </div>
          ) : null}
        </div>

        {!loading && !profile ? (
          <p className="text-zinc-400">No profile data.</p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Field label="Client type" value={pick(profile?.clientType, profile?.clientTypeName, profile?.clientTypeCode)} />
            <Field label="Full name" value={fullName} />
            <Field
              label="Status"
              value={
                typeof profile?.active === 'boolean'
                  ? profile.active
                    ? 'Active'
                    : 'Inactive'
                  : pick(profile?.status, profile?.enabled === true ? 'Active' : profile?.enabled === false ? 'Inactive' : null)
              }
            />
            <Field label="Email" value={pick(profile?.email, profile?.mail)} />
            <Field label="Phone" value={pick(profile?.phone, profile?.phoneNumber)} />
            <Field
              label="Address"
              span2
              value={pick(profile?.address, profile?.fullAddress, [profile?.street, profile?.city, profile?.postalCode, profile?.country].filter(Boolean).join(', '))}
            />
          </div>
        )}
      </section>
    </div>
  );
}

