import ProfileCard from '../components/ProfileCard';

export default function Profile() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-zinc-900 to-slate-950">
      <div className="max-w-5xl mx-auto px-6 py-8">
        <ProfileCard title="Profile" />
      </div>
    </div>
  );
}

