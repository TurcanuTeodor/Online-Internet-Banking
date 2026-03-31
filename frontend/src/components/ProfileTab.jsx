import ProfileCard from './ProfileCard';
import ChangePasswordCard from './ChangePasswordCard';
import GdprSection from './GdprSection';
import { ShieldCheck, UserCircle } from 'lucide-react';

export default function ProfileTab() {
  return (
    <div className="space-y-6 animate-fade-in">
      <div className="glass rounded-2xl p-5 border border-emerald-500/10">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-emerald-500/15 rounded-xl flex items-center justify-center">
            <UserCircle className="w-5 h-5 text-emerald-400" />
          </div>
          <div>
            <h2 className="text-lg font-bold">Profile &amp; Settings</h2>
            <p className="text-sm text-zinc-500">Manage your personal information and account security.</p>
          </div>
          <div className="ml-auto hidden sm:flex items-center gap-1.5 text-xs text-emerald-500">
            <ShieldCheck className="w-4 h-4" />
            End-to-end encrypted
          </div>
        </div>
      </div>

      <ProfileCard />
      <ChangePasswordCard />
      <GdprSection />
    </div>
  );
}
