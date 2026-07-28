import React from 'react';
import { User, Key, Shield, Sun, Moon, Save } from 'lucide-react';
import { useApp } from '../context/AppContext';

export const ProfilePage: React.FC = () => {
  const { user, updateUser } = useApp();

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Header */}
      <div>
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider mb-2">
          <User className="w-4 h-4" /> Account & Settings
        </div>
        <h1 className="text-3xl font-extrabold text-white">Profile & Preferences</h1>
      </div>

      {/* Profile Form */}
      <div className="p-8 rounded-3xl bg-slate-900/50 border border-slate-800 space-y-6">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
          <div>
            <label className="text-xs font-semibold text-slate-400 block mb-1">Full Name</label>
            <input
              type="text"
              value={user.name}
              onChange={(e) => updateUser({ name: e.target.value })}
              className="w-full p-3 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white"
            />
          </div>

          <div>
            <label className="text-xs font-semibold text-slate-400 block mb-1">Email Address</label>
            <input
              type="email"
              value={user.email}
              onChange={(e) => updateUser({ email: e.target.value })}
              className="w-full p-3 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white"
            />
          </div>

          <div>
            <label className="text-xs font-semibold text-slate-400 block mb-1">Target Role Title</label>
            <input
              type="text"
              value={user.targetRole}
              onChange={(e) => updateUser({ targetRole: e.target.value })}
              className="w-full p-3 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white"
            />
          </div>

          <div>
            <label className="text-xs font-semibold text-slate-400 block mb-1">Target Experience Level</label>
            <select
              value={user.experienceLevel}
              onChange={(e) => updateUser({ experienceLevel: e.target.value as any })}
              className="w-full p-3 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white"
            >
              <option value="Entry Level">Entry Level</option>
              <option value="Mid-Senior">Mid-Senior</option>
              <option value="Lead / Executive">Lead / Executive</option>
            </select>
          </div>
        </div>

        {/* API Key */}
        <div className="pt-4 border-t border-slate-800 space-y-2">
          <label className="text-xs font-semibold text-slate-400 flex items-center gap-2">
            <Key className="w-4 h-4 text-amber-400" /> Custom Gemini API Key (Optional Override)
          </label>
          <input
            type="password"
            placeholder="AIzaSy..."
            value={user.apiKey || ''}
            onChange={(e) => updateUser({ apiKey: e.target.value })}
            className="w-full p-3 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white font-mono"
          />
          <p className="text-[11px] text-slate-500">
            If provided, your key will be stored securely in local browser state and used for direct Gemini API calls.
          </p>
        </div>

        {/* Theme Settings */}
        <div className="pt-4 border-t border-slate-800 flex justify-between items-center">
          <div>
            <div className="text-xs font-bold text-white">Interface Appearance</div>
            <div className="text-[11px] text-slate-400">Toggle dark mode or light canvas</div>
          </div>
          <button
            onClick={() => updateUser({ isDarkMode: !user.isDarkMode })}
            className="px-4 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-300 text-xs font-semibold flex items-center gap-2 hover:text-white"
          >
            {user.isDarkMode ? <Sun className="w-4 h-4 text-amber-400" /> : <Moon className="w-4 h-4 text-indigo-400" />}
            {user.isDarkMode ? 'Dark Mode' : 'Light Mode'}
          </button>
        </div>
      </div>
    </div>
  );
};
