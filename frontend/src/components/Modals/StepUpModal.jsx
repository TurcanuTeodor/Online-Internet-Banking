import React, { useState } from 'react';
import { ShieldAlert, X } from 'lucide-react';

export default function StepUpModal({ onVerify, onCancel, error }) {
  const [code, setCode] = useState(['', '', '', '', '', '']);

  const handleChange = (index, value) => {
    if (!/^\d*$/.test(value)) return;
    const newCode = [...code];
    newCode[index] = value;
    setCode(newCode);

    // Auto-focus next input
    if (value && index < 5) {
      const nextInput = document.getElementById(`otp-input-${index + 1}`);
      if (nextInput) nextInput.focus();
    }
  };

  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !code[index] && index > 0) {
      const prevInput = document.getElementById(`otp-input-${index - 1}`);
      if (prevInput) prevInput.focus();
    }
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pastedData = e.clipboardData.getData('text').slice(0, 6).split('');
    if (pastedData.some(char => !/^\d$/.test(char))) return;
    
    const newCode = [...code];
    for (let i = 0; i < pastedData.length; i++) {
      newCode[i] = pastedData[i];
    }
    setCode(newCode);
    
    const nextIndex = Math.min(pastedData.length, 5);
    const nextInput = document.getElementById(`otp-input-${nextIndex}`);
    if (nextInput) nextInput.focus();
  };

  const submit = () => {
    const fullCode = code.join('');
    if (fullCode.length === 6) {
      onVerify(fullCode);
    }
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-in fade-in duration-200">
      <div className="glass max-w-sm w-full rounded-3xl p-8 relative shadow-2xl border border-white/10 text-center animate-in zoom-in-95 duration-200">
        
        <button onClick={onCancel} className="absolute top-4 right-4 text-zinc-400 hover:text-white transition-colors">
          <X className="w-5 h-5" />
        </button>

        <div className="mx-auto w-16 h-16 rounded-full bg-indigo-500/20 border border-indigo-500/30 flex items-center justify-center mb-6">
          <ShieldAlert className="w-8 h-8 text-indigo-400" />
        </div>

        <h2 className="text-2xl font-bold mb-2 tracking-tight text-white">Security Check</h2>
        <p className="text-sm text-zinc-400 mb-8">
          For your protection, please confirm this transfer with the 6-digit code from your authenticator app.
        </p>

        {error && (
          <div className="mb-6 p-3 rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 text-sm">
            {error}
          </div>
        )}

        <div className="flex justify-center gap-2 mb-8" onPaste={handlePaste}>
          {code.map((digit, idx) => (
            <input
              key={idx}
              id={`otp-input-${idx}`}
              type="text"
              maxLength={1}
              value={digit}
              onChange={(e) => handleChange(idx, e.target.value)}
              onKeyDown={(e) => handleKeyDown(idx, e)}
              className="w-12 h-14 text-center text-xl font-bold bg-white/5 border border-white/10 rounded-xl focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all outline-none"
              autoComplete="off"
            />
          ))}
        </div>

        <button
          onClick={submit}
          disabled={code.join('').length !== 6}
          className="w-full py-3.5 bg-indigo-500 hover:bg-indigo-600 disabled:bg-white/10 disabled:text-white/30 text-white rounded-xl font-semibold transition-all shadow-lg shadow-indigo-500/20 disabled:shadow-none"
        >
          Verify & Transfer
        </button>
      </div>
    </div>
  );
}
