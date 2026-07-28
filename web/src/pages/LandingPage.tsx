import React from 'react';
import { motion } from 'framer-motion';
import { Link } from 'react_router_dom';
import {
  Sparkles,
  FileCheck,
  MessageSquare,
  Compass,
  ArrowRight,
  CheckCircle2,
  TrendingUp,
  ShieldCheck,
  Building2,
  Users,
  Star,
  Zap
} from 'lucide-react';

export const LandingPage: React.FC = () => {
  const companies = ['Google', 'Amazon', 'Microsoft', 'Adobe', 'Netflix', 'Meta'];

  const stats = [
    { label: 'Resumes Analyzed', value: '100K+' },
    { label: 'ATS Match Accuracy', value: '98.4%' },
    { label: 'Mock Interviews Conducted', value: '50K+' },
    { label: 'Average Salary Increase', value: '+35%' },
  ];

  const features = [
    {
      icon: FileCheck,
      title: 'Precision ATS Analysis',
      description: 'Instant parsing against target Job Descriptions with real-time keyword matching, score gauges, and formatting recommendations.',
      color: 'from-indigo-500 to-blue-500'
    },
    {
      icon: MessageSquare,
      title: 'Real-Time AI Mock Interview',
      description: 'Interactive voice & chat mock interviews with tailored question loops, STAR framework feedback, and score evaluation.',
      color: 'from-purple-500 to-pink-500'
    },
    {
      icon: Compass,
      title: 'AI Career Roadmap',
      description: 'Dynamic 4-week step-by-step career timeline, skill gap tree, and recommended project milestones to land staff-level roles.',
      color: 'from-emerald-500 to-teal-500'
    },
    {
      icon: Zap,
      title: 'Live Resume Editor',
      description: 'Modern glassmorphism templates, live AI bullet point rewrites, and instant PDF download capabilities.',
      color: 'from-amber-500 to-orange-500'
    }
  ];

  const testimonials = [
    {
      quote: "AI Resume Coach increased my ATS score from 62% to 94%. I landed 4 senior interviews at Google and Meta within two weeks!",
      name: "Sarah Jenkins",
      role: "Senior Staff Engineer @ Google",
      avatar: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150"
    },
    {
      quote: "The mock interview coach gave me bulletproof confidence using the STAR method. The feedback felt like a real hiring manager.",
      name: "David Chen",
      role: "Lead Product Manager @ Stripe",
      avatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150"
    }
  ];

  return (
    <div className="space-y-24 pb-20">
      {/* Hero Section */}
      <section className="relative pt-12 pb-20 overflow-hidden">
        {/* Glow Effects */}
        <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-indigo-600/20 rounded-full blur-[120px] pointer-events-none" />
        <div className="absolute top-1/3 left-1/3 w-[400px] h-[400px] bg-purple-600/15 rounded-full blur-[100px] pointer-events-none" />

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center relative z-10">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 text-xs font-semibold uppercase tracking-wider mb-6"
          >
            <Sparkles className="w-4 h-4" /> Powered by Gemini AI Dual-Core Engine
          </motion.div>

          <motion.h1
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.1 }}
            className="text-4xl sm:text-6xl lg:text-7xl font-extrabold tracking-tight text-white max-w-4xl mx-auto leading-[1.1]"
          >
            Land Your Dream Job with{' '}
            <span className="bg-gradient-to-r from-indigo-400 via-purple-400 to-pink-400 bg-clip-text text-transparent">
              Enterprise AI Intelligence
            </span>
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.2 }}
            className="mt-6 text-lg sm:text-xl text-slate-400 max-w-2xl mx-auto leading-relaxed"
          >
            Bypass ATS filters, master high-stakes technical & behavioral mock interviews, and follow personalized career roadmaps built by staff engineers.
          </motion.p>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.3 }}
            className="mt-8 flex flex-col sm:flex-row gap-4 justify-center items-center"
          >
            <Link
              to="/ats"
              className="w-full sm:w-auto px-8 py-4 rounded-xl bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white font-semibold flex items-center justify-center gap-3 shadow-lg shadow-indigo-500/25 transition-all hover:scale-[1.02]"
            >
              Upload Resume for ATS Scan <ArrowRight className="w-5 h-5" />
            </Link>
            <Link
              to="/interview"
              className="w-full sm:w-auto px-8 py-4 rounded-xl bg-slate-900 border border-slate-800 hover:bg-slate-800 text-slate-200 font-semibold flex items-center justify-center gap-2 transition-all"
            >
              Try AI Mock Interview
            </Link>
          </motion.div>

          {/* Stat Bar */}
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.4 }}
            className="mt-16 grid grid-cols-2 lg:grid-cols-4 gap-4 p-6 rounded-2xl bg-slate-900/60 border border-slate-800/80 backdrop-blur-md"
          >
            {stats.map((st, i) => (
              <div key={i} className="text-center p-3">
                <div className="text-2xl sm:text-3xl font-black text-white bg-gradient-to-r from-indigo-400 to-purple-400 bg-clip-text text-transparent">
                  {st.value}
                </div>
                <div className="text-xs text-slate-400 font-medium mt-1">{st.label}</div>
              </div>
            ))}
          </motion.div>
        </div>
      </section>

      {/* Trusted Companies */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <p className="text-xs font-semibold text-slate-500 uppercase tracking-widest mb-6">
          Candidates Placed at Leading Enterprise Tech Companies
        </p>
        <div className="flex flex-wrap items-center justify-center gap-8 sm:gap-12 opacity-60">
          {companies.map((company, i) => (
            <span key={i} className="text-xl sm:text-2xl font-bold tracking-tight text-slate-400 hover:text-white transition-colors">
              {company}
            </span>
          ))}
        </div>
      </section>

      {/* Core Features */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-16">
          <h2 className="text-3xl font-extrabold text-white sm:text-4xl">
            Everything You Need to Secure Your Next Role
          </h2>
          <p className="mt-3 text-slate-400 text-sm max-w-xl mx-auto">
            A complete suite of AI-driven tools designed to transform your resume, boost interview readiness, and track applications seamlessly.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {features.map((feat, i) => {
            const Icon = feat.icon;
            return (
              <div
                key={i}
                className="p-8 rounded-2xl bg-slate-900/40 border border-slate-800/80 hover:border-indigo-500/40 transition-all group"
              >
                <div className={`w-12 h-12 rounded-xl bg-gradient-to-tr ${feat.color} flex items-center justify-center mb-6 shadow-md`}>
                  <Icon className="w-6 h-6 text-white" />
                </div>
                <h3 className="text-xl font-bold text-white mb-2 group-hover:text-indigo-400 transition-colors">
                  {feat.title}
                </h3>
                <p className="text-slate-400 text-sm leading-relaxed">{feat.description}</p>
              </div>
            );
          })}
        </div>
      </section>

      {/* Testimonials */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="p-10 rounded-3xl bg-gradient-to-b from-indigo-950/40 to-slate-900/60 border border-indigo-500/20">
          <div className="text-center mb-10">
            <h2 className="text-2xl sm:text-3xl font-bold text-white">Trusted by Top Tech Professionals</h2>
            <div className="flex justify-center gap-1 mt-2 text-amber-400">
              {[...Array(5)].map((_, i) => <Star key={i} className="w-4 h-4 fill-amber-400" />)}
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            {testimonials.map((t, i) => (
              <div key={i} className="p-6 rounded-2xl bg-slate-950/80 border border-slate-800 space-y-4">
                <p className="text-slate-300 text-sm italic leading-relaxed">"{t.quote}"</p>
                <div className="flex items-center gap-3">
                  <img src={t.avatar} alt={t.name} className="w-10 h-10 rounded-full object-cover border border-indigo-500/40" />
                  <div>
                    <div className="font-semibold text-white text-sm">{t.name}</div>
                    <div className="text-xs text-indigo-400">{t.role}</div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
};
