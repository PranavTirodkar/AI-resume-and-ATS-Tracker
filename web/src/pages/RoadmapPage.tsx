import React from 'react';
import { Compass, CheckCircle2, Circle, BookOpen, ArrowUpRight, Trophy } from 'lucide-react';
import { useApp } from '../context/AppContext';

export const RoadmapPage: React.FC = () => {
  const { roadmapGoals, toggleRoadmapGoal, user } = useApp();

  const completedCount = roadmapGoals.filter((g) => g.completed).length;
  const progressPercent = Math.round((completedCount / Math.max(roadmapGoals.length, 1)) * 100);

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Header Banner */}
      <div className="p-8 rounded-3xl bg-gradient-to-r from-emerald-950/60 via-slate-900 to-indigo-950/60 border border-emerald-500/30 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-6">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 text-emerald-400 text-xs font-semibold uppercase tracking-wider mb-2">
            <Compass className="w-4 h-4" /> Career Growth & Roadmap Engine
          </div>
          <h1 className="text-3xl font-extrabold text-white">4-Week Staff Engineer Prep Plan</h1>
          <p className="text-slate-300 text-xs sm:text-sm mt-1">
            Tailored specifically for <span className="text-emerald-300 font-semibold">{user.targetRole}</span> candidates.
          </p>
        </div>

        <div className="p-4 rounded-2xl bg-slate-950/80 border border-slate-800 text-center min-w-[160px]">
          <div className="text-xs text-slate-400 font-semibold">Overall Progress</div>
          <div className="text-3xl font-black text-emerald-400 mt-1">{progressPercent}%</div>
          <div className="text-[10px] text-slate-500">{completedCount} of {roadmapGoals.length} completed</div>
        </div>
      </div>

      {/* Timeline Goals */}
      <div className="space-y-6">
        {roadmapGoals.map((goal, i) => (
          <div
            key={goal.id}
            className={`p-6 rounded-2xl border transition-all ${
              goal.completed
                ? 'bg-slate-900/30 border-emerald-500/30'
                : 'bg-slate-900/60 border-slate-800 hover:border-slate-700'
            }`}
          >
            <div className="flex items-start justify-between gap-4">
              <div className="flex items-start gap-4">
                <button
                  onClick={() => toggleRoadmapGoal(goal.id)}
                  className="mt-1 transition-transform hover:scale-110"
                >
                  {goal.completed ? (
                    <CheckCircle2 className="w-6 h-6 text-emerald-400" />
                  ) : (
                    <Circle className="w-6 h-6 text-slate-600 hover:text-slate-400" />
                  )}
                </button>

                <div className="space-y-2">
                  <div className="flex items-center gap-3">
                    <span className="text-xs font-extrabold text-emerald-400 bg-emerald-500/10 px-2.5 py-0.5 rounded-md border border-emerald-500/20">
                      WEEK {goal.week}
                    </span>
                    <h3 className={`text-base font-bold ${goal.completed ? 'text-slate-400 line-through' : 'text-white'}`}>
                      {goal.title}
                    </h3>
                  </div>

                  <p className="text-xs text-slate-400 leading-relaxed">{goal.description}</p>

                  <div className="flex flex-wrap gap-2 pt-2">
                    {goal.skills.map((s, idx) => (
                      <span key={idx} className="px-2.5 py-1 rounded-md bg-slate-950 border border-slate-800 text-slate-300 text-[11px]">
                        {s}
                      </span>
                    ))}
                  </div>
                </div>
              </div>

              {goal.courses.length > 0 && (
                <div className="hidden sm:block text-right">
                  <span className="text-[10px] font-semibold text-slate-500 uppercase tracking-wider block mb-1">
                    Recommended Resource
                  </span>
                  {goal.courses.map((c, idx) => (
                    <a
                      key={idx}
                      href={c.url}
                      className="text-xs text-indigo-400 hover:underline inline-flex items-center gap-1 font-semibold"
                    >
                      {c.name} <ArrowUpRight className="w-3 h-3" />
                    </a>
                  ))}
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
