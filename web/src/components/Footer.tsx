import React from 'react';
import { Sparkles, Github, Twitter, Linkedin, Heart } from 'lucide-react';
import { Link } from 'react-router-dom';

export const Footer: React.FC = () => {
  return (
    <footer className="border-t border-slate-800/80 bg-slate-950 text-slate-400 text-sm py-12 mt-20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 grid grid-cols-1 md:grid-cols-4 gap-8">
        <div className="space-y-4">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-indigo-600 flex items-center justify-center">
              <Sparkles className="w-4 h-4 text-white" />
            </div>
            <span className="font-bold text-white tracking-tight text-lg">AI Resume Coach</span>
          </div>
          <p className="text-slate-400 text-xs leading-relaxed">
            Enterprise-grade AI resume optimizer, ATS scanner, and real-time mock interview trainer powered by Gemini AI architecture.
          </p>
        </div>

        <div>
          <h4 className="font-semibold text-white mb-3">Product</h4>
          <ul className="space-y-2 text-xs">
            <li><Link to="/ats" className="hover:text-indigo-400">ATS Keyword Analyzer</Link></li>
            <li><Link to="/interview" className="hover:text-indigo-400">AI Mock Interviewer</Link></li>
            <li><Link to="/roadmap" className="hover:text-indigo-400">Career Roadmap</Link></li>
            <li><Link to="/builder" className="hover:text-indigo-400">Interactive Resume Builder</Link></li>
            <li><Link to="/tracker" className="hover:text-indigo-400">Kanban Job Tracker</Link></li>
          </ul>
        </div>

        <div>
          <h4 className="font-semibold text-white mb-3">Company</h4>
          <ul className="space-y-2 text-xs">
            <li><Link to="/pricing" className="hover:text-indigo-400">Pricing Plans</Link></li>
            <li><a href="#" className="hover:text-indigo-400">Enterprise Solution</a></li>
            <li><a href="#" className="hover:text-indigo-400">Customer Success Stories</a></li>
            <li><a href="#" className="hover:text-indigo-400">Security & Privacy</a></li>
          </ul>
        </div>

        <div>
          <h4 className="font-semibold text-white mb-3">Connect</h4>
          <div className="flex gap-3 text-slate-400 mb-4">
            <a href="https://github.com" target="_blank" rel="noreferrer" className="hover:text-white"><Github className="w-5 h-5" /></a>
            <a href="https://twitter.com" target="_blank" rel="noreferrer" className="hover:text-white"><Twitter className="w-5 h-5" /></a>
            <a href="https://linkedin.com" target="_blank" rel="noreferrer" className="hover:text-white"><Linkedin className="w-5 h-5" /></a>
          </div>
          <p className="text-xs text-slate-500">
            &copy; {new Date().getFullYear()} AI Resume Coach Inc. All rights reserved.
          </p>
        </div>
      </div>
    </footer>
  );
};
