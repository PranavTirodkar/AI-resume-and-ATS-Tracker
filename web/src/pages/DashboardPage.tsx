import React from 'react';
import { Link } from 'react-router-dom';
import {
  FileCheck,
  MessageSquare,
  Compass,
  Kanban,
  FileText,
  TrendingUp,
  CheckCircle2,
  Clock,
  ArrowRight,
  Plus
} from 'lucide-react';
import { useApp } from '../context/AppContext';

export const DashboardPage: React.FC = () => {
  const { user, atsResult, jobApplications, interviewSessions, roadmapGoals } = useApp();

  const completedGoals = roadmapGoals.filter((g) => g.completed).length;
  const roadmapProgress = Math.round((completedGoals / Math.max(roadmapGoals.length, 1)) * 100);

  const stats = [
    {
      title: 'ATS Match Score',
      value: atsResult ? `${atsResult.overallScore}%` : '88%',
      subtitle: atsResult ? 'Based on recent scan' : 'Sample analysis',
      icon: FileCheck,
      color: 'text-indigo-400',
      bg: 'bg-indigo-500/10'
    },
    {
      title: 'Active Applications',
      value: jobApplications.length.toString(),
      subtitle: `${jobApplications.filter((a) => a.status === 'Interview').length} in interview stage`,
      icon: Kanban,
      color: 'text-purple-400',
      bg: 'bg-purple-500/10'
    },
    {
      title: 'Interview Readiness',
      value: '92%',
      subtitle: `${interviewSessions.length} sessions completed`,
      icon: MessageSquare,
      color: 'text-emerald-400',
      bg: 'bg-emerald-500/10'
    },
    {
      title: 'Roadmap Completion',
      value: `${roadmapProgress}%`,
      subtitle: `${completedGoals} of ${roadmapGoals.length} milestones done`,
      icon: Compass,
      color: 'text-amber-400',
      bg: 'bg-amber-500/10'
    }
  ];

  return (
    <div className="space-y-8 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Welcome Banner */}
      <div className="p-8 rounded-3xl bg-gradient-to-r from-indigo-900/60 via-purple-900/40 to-slate-900 border border-indigo-500/30 flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
        <div>
          <h1 className="text-3xl font-extrabold text-white">Welcome back, {user.name} 👋</h1>
          <p className="text-slate-300 text-sm mt-1">
            Target Role: <span className="font-semibold text-indigo-300">{user.targetRole}</span> • Senior Career Track
          </p>
        </div>
        <div className="flex gap-3">
          <Link
            to="/ats"
            className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-sm flex items-center gap-2 shadow-lg shadow-indigo-500/25 transition-all"
          >
            <Plus className="w-4 h-4" /> New ATS Analysis
          </Link>
          <Link
            to="/interview"
            className="px-5 py-2.5 rounded-xl bg-slate-900 border border-slate-700 hover:bg-slate-800 text-slate-200 font-semibold text-sm flex items-center gap-2"
          >
            Start Mock Interview
          </Link>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        {stats.map((s, i) => {
          const Icon = s.icon;
          return (
            <div key={i} className="p-6 rounded-2xl bg-slate-900/50 border border-slate-800 space-y-3">
              <div className="flex justify-between items-center">
                <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">{s.title}</span>
                <div className={`p-2 rounded-xl ${s.bg}`}>
                  <Icon className={`w-5 h-5 ${s.color}`} />
                </div>
              </div>
              <div className="text-3xl font-black text-white">{s.value}</div>
              <div className="text-xs text-slate-400">{s.subtitle}</div>
            </div>
          );
        })}
      </div>

      {/* Main Grid: Application Overview & Quick Navigation */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left 2 Cols: Active Job Pipeline */}
        <div className="lg:col-span-2 p-6 rounded-2xl bg-slate-900/40 border border-slate-800 space-y-6">
          <div className="flex justify-between items-center">
            <div>
              <h2 className="text-lg font-bold text-white">Active Applications</h2>
              <p className="text-xs text-slate-400">Tracked candidates pipeline</p>
            </div>
            <Link to="/tracker" className="text-xs font-semibold text-indigo-400 hover:text-indigo-300 flex items-center gap-1">
              View Kanban Board <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          <div className="space-y-3">
            {jobApplications.slice(0, 4).map((app) => (
              <div
                key={app.id}
                className="p-4 rounded-xl bg-slate-950/60 border border-slate-800/80 flex items-center justify-between hover:border-slate-700 transition-colors"
              >
                <div>
                  <div className="font-semibold text-white text-sm">{app.roleTitle}</div>
                  <div className="text-xs text-slate-400">{app.companyName} • {app.location}</div>
                </div>
                <div className="flex items-center gap-4">
                  <span className="text-xs font-bold text-indigo-400 bg-indigo-500/10 px-2.5 py-1 rounded-md border border-indigo-500/20">
                    Match {app.matchScore}%
                  </span>
                  <span
                    className={`text-xs px-3 py-1 rounded-full font-semibold ${
                      app.status === 'Interview'
                        ? 'bg-purple-500/20 text-purple-300 border border-purple-500/30'
                        : app.status === 'Offer'
                        ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                        : 'bg-slate-800 text-slate-300'
                    }`}
                  >
                    {app.status}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Right Col: Quick Modules */}
        <div className="p-6 rounded-2xl bg-slate-900/40 border border-slate-800 space-y-6">
          <h2 className="text-lg font-bold text-white">Recommended Actions</h2>

          <div className="space-y-4">
            <Link
              to="/ats"
              className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 hover:border-indigo-500/40 block space-y-2 group transition-all"
            >
              <div className="flex items-center gap-3">
                <FileCheck className="w-5 h-5 text-indigo-400" />
                <span className="font-semibold text-white text-sm group-hover:text-indigo-400">Scan Resume against JD</span>
              </div>
              <p className="text-xs text-slate-400">Get instant keyword gaps and ATS bullet point rewrites.</p>
            </Link>

            <Link
              to="/interview"
              className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 hover:border-indigo-500/40 block space-y-2 group transition-all"
            >
              <div className="flex items-center gap-3">
                <MessageSquare className="w-5 h-5 text-purple-400" />
                <span className="font-semibold text-white text-sm group-hover:text-purple-400">Practice STAR Questions</span>
              </div>
              <p className="text-xs text-slate-400">Interactive voice & text AI mock interview feedback.</p>
            </Link>

            <Link
              to="/builder"
              className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 hover:border-indigo-500/40 block space-y-2 group transition-all"
            >
              <div className="flex items-center gap-3">
                <FileText className="w-5 h-5 text-emerald-400" />
                <span className="font-semibold text-white text-sm group-hover:text-emerald-400">Edit Live Resume PDF</span>
              </div>
              <p className="text-xs text-slate-400">Update achievements with real-time AI bullet point refactoring.</p>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
