import React, { useState } from 'react';
import {
  FileCheck,
  Upload,
  Sparkles,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Download,
  Copy,
  RefreshCw,
  FileText
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { analyzeResumeWithGemini } from '../services/geminiService';

export const ATSPage: React.FC = () => {
  const { atsResult, setAtsResult, resumeData } = useApp();

  const [resumeText, setResumeText] = useState(
    `${resumeData.summary}\n\nExperience:\n${resumeData.experiences.map(e => `${e.role} at ${e.company}\n${e.bulletPoints.join('\n')}`).join('\n\n')}\n\nSkills: ${resumeData.skills.join(', ')}`
  );

  const [jobDescription, setJobDescription] = useState(
    'Seeking a Senior Staff Software Engineer skilled in Kotlin, Jetpack Compose, Clean Architecture, CI/CD pipelines, Room Database, REST APIs, and Gemini AI integrations.'
  );

  const [isAnalyzing, setIsAnalyzing] = useState(false);

  const handleAnalyze = async () => {
    if (!resumeText.trim() || !jobDescription.trim()) return;
    setIsAnalyzing(true);
    try {
      const res = await analyzeResumeWithGemini(resumeText, jobDescription);
      setAtsResult(res);
    } catch (e) {
      console.error(e);
    } finally {
      setIsAnalyzing(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Header */}
      <div>
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider mb-2">
          <FileCheck className="w-4 h-4" /> ATS Optimizer Engine
        </div>
        <h1 className="text-3xl font-extrabold text-white">ATS Keyword & Skill Gap Analyzer</h1>
        <p className="text-slate-400 text-sm mt-1">
          Scan your resume against target job postings to identify keyword gaps, formatting issues, and tailored bullet point rewrites.
        </p>
      </div>

      {/* Input Section: 2 Columns */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Resume Input Card */}
        <div className="p-6 rounded-2xl bg-slate-900/50 border border-slate-800 space-y-4">
          <div className="flex justify-between items-center">
            <label className="font-bold text-white text-sm flex items-center gap-2">
              <FileText className="w-4 h-4 text-indigo-400" /> Resume Content
            </label>
            <label className="text-xs text-indigo-400 hover:underline cursor-pointer flex items-center gap-1">
              <Upload className="w-3.5 h-3.5" /> Upload File (.txt / .pdf)
              <input
                type="file"
                accept=".txt,.pdf"
                className="hidden"
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file) {
                    const reader = new FileReader();
                    reader.onload = (evt) => {
                      if (evt.target?.result) {
                        setResumeText(evt.target.result as string);
                      }
                    };
                    reader.readAsText(file);
                  }
                }}
              />
            </label>
          </div>
          <textarea
            value={resumeText}
            onChange={(e) => setResumeText(e.target.value)}
            rows={10}
            placeholder="Paste raw resume text here..."
            className="w-full p-4 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 text-xs font-mono focus:outline-none focus:border-indigo-500/80 transition-colors resize-none"
          />
        </div>

        {/* JD Input Card */}
        <div className="p-6 rounded-2xl bg-slate-900/50 border border-slate-800 space-y-4">
          <div className="flex justify-between items-center">
            <label className="font-bold text-white text-sm flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-purple-400" /> Target Job Description (JD)
            </label>
          </div>
          <textarea
            value={jobDescription}
            onChange={(e) => setJobDescription(e.target.value)}
            rows={10}
            placeholder="Paste target job description or requirements here..."
            className="w-full p-4 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 text-xs font-mono focus:outline-none focus:border-purple-500/80 transition-colors resize-none"
          />
        </div>
      </div>

      {/* Action Button */}
      <div className="text-center">
        <button
          onClick={handleAnalyze}
          disabled={isAnalyzing || !resumeText.trim() || !jobDescription.trim()}
          className="px-10 py-4 rounded-2xl bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-600 hover:from-indigo-500 hover:to-pink-500 text-white font-bold shadow-xl shadow-indigo-500/25 transition-all hover:scale-[1.02] disabled:opacity-50 disabled:pointer-events-none inline-flex items-center gap-3"
        >
          {isAnalyzing ? (
            <>
              <RefreshCw className="w-5 h-5 animate-spin" /> Scanning with Gemini AI...
            </>
          ) : (
            <>
              <Sparkles className="w-5 h-5" /> Run Enterprise ATS Analysis
            </>
          )}
        </button>
      </div>

      {/* Analysis Results */}
      {atsResult && (
        <div className="space-y-8 pt-6 border-t border-slate-800">
          {/* Top Score Summary Banner */}
          <div className="p-8 rounded-3xl bg-slate-900/80 border border-slate-800 grid grid-cols-1 md:grid-cols-4 gap-6 items-center">
            <div className="text-center md:text-left space-y-1">
              <span className="text-xs font-bold text-slate-400 uppercase tracking-widest">Overall ATS Match</span>
              <div className="text-5xl font-black bg-gradient-to-r from-indigo-400 to-emerald-400 bg-clip-text text-transparent">
                {atsResult.overallScore}%
              </div>
              <p className="text-xs text-slate-400">High probability of passing automated screening</p>
            </div>

            <div className="md:col-span-3 grid grid-cols-3 gap-4">
              <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800/80 text-center">
                <div className="text-xs text-slate-400">Keyword Density</div>
                <div className="text-xl font-extrabold text-white mt-1">{atsResult.keywordMatchPercentage}%</div>
              </div>
              <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800/80 text-center">
                <div className="text-xs text-slate-400">Formatting Quality</div>
                <div className="text-xl font-extrabold text-white mt-1">{atsResult.formattingScore}%</div>
              </div>
              <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800/80 text-center">
                <div className="text-xs text-slate-400">Impact & Metrics</div>
                <div className="text-xl font-extrabold text-white mt-1">{atsResult.quantifiableMetricsScore}%</div>
              </div>
            </div>
          </div>

          {/* Keywords Breakdown */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="p-6 rounded-2xl bg-slate-900/40 border border-slate-800 space-y-4">
              <h3 className="font-bold text-emerald-400 text-sm flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4" /> Matched Keywords ({atsResult.matchedKeywords.length})
              </h3>
              <div className="flex flex-wrap gap-2">
                {atsResult.matchedKeywords.map((kw, i) => (
                  <span key={i} className="px-3 py-1 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs font-medium">
                    {kw}
                  </span>
                ))}
              </div>
            </div>

            <div className="p-6 rounded-2xl bg-slate-900/40 border border-slate-800 space-y-4">
              <h3 className="font-bold text-rose-400 text-sm flex items-center gap-2">
                <XCircle className="w-4 h-4" /> Missing Keywords ({atsResult.missingKeywords.length})
              </h3>
              <div className="flex flex-wrap gap-2">
                {atsResult.missingKeywords.map((kw, i) => (
                  <span key={i} className="px-3 py-1 rounded-lg bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs font-medium">
                    {kw}
                  </span>
                ))}
              </div>
            </div>
          </div>

          {/* Actionable Recommendations & Bullet Points */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="p-6 rounded-2xl bg-slate-900/40 border border-slate-800 space-y-4">
              <h3 className="font-bold text-amber-400 text-sm flex items-center gap-2">
                <AlertTriangle className="w-4 h-4" /> Recommended Enhancements
              </h3>
              <ul className="space-y-3">
                {atsResult.actionableRecommendations.map((rec, i) => (
                  <li key={i} className="text-xs text-slate-300 flex items-start gap-2 leading-relaxed">
                    <span className="w-1.5 h-1.5 rounded-full bg-amber-400 mt-1.5 shrink-0" />
                    {rec}
                  </li>
                ))}
              </ul>
            </div>

            <div className="p-6 rounded-2xl bg-slate-900/40 border border-slate-800 space-y-4">
              <h3 className="font-bold text-indigo-400 text-sm flex items-center gap-2">
                <Sparkles className="w-4 h-4" /> AI Tailored Bullet Point Rewrites
              </h3>
              <div className="space-y-3">
                {atsResult.tailoredBulletPoints.map((bp, i) => (
                  <div key={i} className="p-3 rounded-xl bg-slate-950/80 border border-slate-800 text-xs text-slate-200 leading-relaxed font-mono">
                    "{bp}"
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
