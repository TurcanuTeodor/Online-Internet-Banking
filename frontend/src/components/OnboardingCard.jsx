import { useState } from 'react';
import { Wallet, CreditCard, Send, X, Sparkles } from 'lucide-react';

const STEPS = [
  { icon: Wallet, label: 'Open your first account', description: 'Choose a currency and get an IBAN instantly.', action: 'openAccount' },
  { icon: CreditCard, label: 'Add a payment card', description: 'Save a card for easy top-ups via Stripe.', action: 'cards' },
  { icon: Send, label: 'Make your first transfer', description: 'Send money to any IBAN in seconds.', action: 'payments' },
];

export default function OnboardingCard({ accounts, onAction, onNavigate }) {
  const [dismissed, setDismissed] = useState(() => localStorage.getItem('onboarding_dismissed') === 'true');

  if (dismissed || (accounts && accounts.length > 0)) return null;

  const handleDismiss = () => {
    setDismissed(true);
    localStorage.setItem('onboarding_dismissed', 'true');
  };

  return (
    <div className="glass rounded-2xl p-6 border border-emerald-500/15 animate-fade-in">
      <div className="flex items-start justify-between gap-3 mb-5">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-emerald-500/20 rounded-xl flex items-center justify-center">
            <Sparkles className="w-5 h-5 text-emerald-400" />
          </div>
          <div>
            <h2 className="text-lg font-bold">Welcome to CashTactics!</h2>
            <p className="text-sm text-zinc-500">Get started in 3 easy steps.</p>
          </div>
        </div>
        <button onClick={handleDismiss} className="text-zinc-500 hover:text-zinc-300 transition-colors p-1">
          <X className="w-4 h-4" />
        </button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        {STEPS.map((step, idx) => {
          const Icon = step.icon;
          return (
            <button
              key={step.action}
              type="button"
              onClick={() => {
                if (step.action === 'openAccount') onAction?.('openAccount');
                else onNavigate?.(`/dashboard/${step.action}`);
              }}
              className="glass rounded-xl p-4 text-left hover:border-emerald-500/30 transition-all group"
            >
              <div className="flex items-center gap-2 mb-2">
                <span className="text-xs font-bold text-emerald-500">{idx + 1}</span>
                <Icon className="w-4 h-4 text-emerald-400" />
              </div>
              <p className="text-sm font-medium text-zinc-200 group-hover:text-white">{step.label}</p>
              <p className="text-xs text-zinc-500 mt-1">{step.description}</p>
            </button>
          );
        })}
      </div>
    </div>
  );
}
