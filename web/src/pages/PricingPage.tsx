import React from 'react';
import { Check, Sparkles, Zap, Shield } from 'lucide-react';
import { Link } from 'react_router_dom';

export const PricingPage: React.FC = () => {
  const plans = [
    {
      name: 'Starter',
      price: '$0',
      period: 'Forever Free',
      description: 'Essential ATS keyword check and basic interview questions.',
      features: [
        '3 ATS Resume Scans / month',
        'Basic Keyword Gap Analysis',
        '1 Mock Interview Session',
        'Standard Resume Templates'
      ],
      cta: 'Current Plan',
      highlight: false
    },
    {
      name: 'Pro Candidate',
      price: '$19',
      period: 'per month',
      description: 'Unlimited Gemini AI dual-core scans, mock interviews & roadmaps.',
      features: [
        'Unlimited Enterprise ATS Scans',
        'AI Bullet Point Refactoring',
        'Unlimited Voice & Text Mock Interviews',
        'STAR Method Feedback Loop',
        'Interactive 4-Week Career Roadmap',
        'Kanban Application Tracker'
      ],
      cta: 'Upgrade to Pro',
      highlight: true
    },
    {
      name: 'Executive / Team',
      price: '$49',
      period: 'per month',
      description: '1-on-1 expert coaching review and custom AI fine-tuning.',
      features: [
        'Everything in Pro',
        'Staff Engineer Resume Review',
        'Priority Gemini 1.5 Pro AI Latency',
        'Custom System Design Practice',
        'Dedicated Career Strategist'
      ],
      cta: 'Contact Sales',
      highlight: false
    }
  ];

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 space-y-12">
      <div className="text-center space-y-4 max-w-2xl mx-auto">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider">
          <Sparkles className="w-4 h-4" /> Transparent Enterprise Pricing
        </div>
        <h1 className="text-4xl font-extrabold text-white">Invest in Your Next Career Leap</h1>
        <p className="text-slate-400 text-sm">
          Get unlimited access to Gemini-powered ATS scanners, real-time mock interviews, and tailored career roadmaps.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        {plans.map((p, i) => (
          <div
            key={i}
            className={`p-8 rounded-3xl space-y-6 flex flex-col justify-between transition-all ${
              p.highlight
                ? 'bg-slate-900 border-2 border-indigo-500 shadow-2xl shadow-indigo-500/20 scale-105'
                : 'bg-slate-900/40 border border-slate-800'
            }`}
          >
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <h3 className="font-extrabold text-white text-xl">{p.name}</h3>
                {p.highlight && (
                  <span className="text-[10px] font-bold text-indigo-300 bg-indigo-500/20 px-2.5 py-1 rounded-full border border-indigo-500/40">
                    MOST POPULAR
                  </span>
                )}
              </div>
              <p className="text-xs text-slate-400 leading-relaxed">{p.description}</p>
              <div className="flex items-baseline gap-1">
                <span className="text-4xl font-black text-white">{p.price}</span>
                <span className="text-xs text-slate-400">/{p.period}</span>
              </div>

              <ul className="space-y-3 pt-4 border-t border-slate-800">
                {p.features.map((f, idx) => (
                  <li key={idx} className="text-xs text-slate-300 flex items-center gap-2.5">
                    <Check className="w-4 h-4 text-emerald-400 shrink-0" />
                    {f}
                  </li>
                ))}
              </ul>
            </div>

            <Link
              to="/ats"
              className={`w-full py-3.5 rounded-xl font-bold text-xs text-center transition-all ${
                p.highlight
                  ? 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-lg shadow-indigo-500/30'
                  : 'bg-slate-800 hover:bg-slate-700 text-slate-200'
              }`}
            >
              {p.cta}
            </Link>
          </div>
        ))}
      </div>
    </div>
  );
};
