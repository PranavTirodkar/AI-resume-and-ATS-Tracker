import React from 'react';
import { FileText, Download, Sparkles, Plus, Trash2 } from 'lucide-react';
import { useApp } from '../context/AppContext';

export const ResumeBuilderPage: React.FC = () => {
  const { resumeData, updateResumeData } = useApp();

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 text-emerald-400 text-xs font-semibold uppercase tracking-wider mb-2">
            <FileText className="w-4 h-4" /> Interactive Resume Builder
          </div>
          <h1 className="text-3xl font-extrabold text-white">Live Resume Editor & Template Studio</h1>
        </div>

        <button
          onClick={handlePrint}
          className="px-6 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-sm flex items-center gap-2 shadow-lg shadow-indigo-500/25 transition-all"
        >
          <Download className="w-4 h-4" /> Download / Print PDF
        </button>
      </div>

      {/* Editor & Preview Split View */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Editor Side */}
        <div className="p-6 rounded-2xl bg-slate-900/50 border border-slate-800 space-y-6">
          <h2 className="text-lg font-bold text-white">Candidate Details</h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="text-xs text-slate-400 font-semibold block mb-1">Full Name</label>
              <input
                type="text"
                value={resumeData.fullName}
                onChange={(e) => updateResumeData({ fullName: e.target.value })}
                className="w-full p-3 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white"
              />
            </div>
            <div>
              <label className="text-xs text-slate-400 font-semibold block mb-1">Email</label>
              <input
                type="text"
                value={resumeData.email}
                onChange={(e) => updateResumeData({ email: e.target.value })}
                className="w-full p-3 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white"
              />
            </div>
          </div>

          <div>
            <label className="text-xs text-slate-400 font-semibold block mb-1">Professional Summary</label>
            <textarea
              rows={4}
              value={resumeData.summary}
              onChange={(e) => updateResumeData({ summary: e.target.value })}
              className="w-full p-3 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white resize-none"
            />
          </div>

          <div>
            <label className="text-xs text-slate-400 font-semibold block mb-1">Technical Skills (Comma Separated)</label>
            <input
              type="text"
              value={resumeData.skills.join(', ')}
              onChange={(e) => updateResumeData({ skills: e.target.value.split(',').map(s => s.trim()) })}
              className="w-full p-3 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white"
            />
          </div>
        </div>

        {/* Live Preview Document */}
        <div id="resume-document" className="p-8 rounded-2xl bg-white text-slate-900 space-y-6 shadow-2xl min-h-[600px] font-sans">
          <div className="border-b border-slate-200 pb-4 text-center">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">{resumeData.fullName}</h1>
            <div className="text-xs text-slate-600 mt-1 space-x-2">
              <span>{resumeData.email}</span> • <span>{resumeData.phone}</span> • <span>{resumeData.linkedin}</span>
            </div>
          </div>

          <div>
            <h3 className="text-xs font-bold uppercase tracking-wider text-indigo-700 mb-1 border-b border-slate-200 pb-1">
              Executive Summary
            </h3>
            <p className="text-xs text-slate-700 leading-relaxed">{resumeData.summary}</p>
          </div>

          <div>
            <h3 className="text-xs font-bold uppercase tracking-wider text-indigo-700 mb-2 border-b border-slate-200 pb-1">
              Work Experience
            </h3>
            <div className="space-y-4">
              {resumeData.experiences.map((exp) => (
                <div key={exp.id} className="space-y-1">
                  <div className="flex justify-between items-center text-xs font-bold text-slate-900">
                    <span>{exp.role} — {exp.company}</span>
                    <span className="text-slate-500 font-normal">{exp.duration}</span>
                  </div>
                  <ul className="list-disc list-inside text-[11px] text-slate-700 space-y-0.5">
                    {exp.bulletPoints.map((bp, i) => (
                      <li key={i}>{bp}</li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>
          </div>

          <div>
            <h3 className="text-xs font-bold uppercase tracking-wider text-indigo-700 mb-2 border-b border-slate-200 pb-1">
              Core Competencies
            </h3>
            <p className="text-xs text-slate-700">{resumeData.skills.join(' • ')}</p>
          </div>
        </div>
      </div>
    </div>
  );
};
