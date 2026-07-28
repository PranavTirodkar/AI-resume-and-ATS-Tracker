import React, { useState } from 'react';
import {
  MessageSquare,
  Mic,
  MicOff,
  Send,
  Sparkles,
  Award,
  Clock,
  RotateCcw,
  CheckCircle2
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { InterviewMessage } from '../types';
import { generateInterviewFeedback } from '../services/geminiService';

export const MockInterviewPage: React.FC = () => {
  const { addInterviewSession } = useApp();

  const [category, setCategory] = useState<'Behavioral' | 'Technical' | 'STAR Method'>('STAR Method');
  const [messages, setMessages] = useState<InterviewMessage[]>([
    {
      id: '1',
      sender: 'ai',
      text: "Hello! I'm your Gemini AI Senior Technical Interview Coach. Let's practice a high-stakes scenario. Tell me about a time you had to lead a critical system refactor under tight deadlines. How did you balance architectural quality with delivery speed?",
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    }
  ]);

  const [userInput, setUserInput] = useState('');
  const [isMicActive, setIsMicActive] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);

  const handleSend = async () => {
    if (!userInput.trim() || isProcessing) return;

    const userMsg: InterviewMessage = {
      id: Date.now().toString(),
      sender: 'user',
      text: userInput,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setMessages((prev) => [...prev, userMsg]);
    setUserInput('');
    setIsProcessing(true);

    try {
      const feedback = await generateInterviewFeedback(
        messages[messages.length - 1]?.text || 'System refactor scenario',
        userInput
      );

      const aiMsg: InterviewMessage = {
        id: (Date.now() + 1).toString(),
        sender: 'ai',
        text: `Feedback (Score: ${feedback.score}/100):\n${feedback.feedback}\n\nExemplary Answer Structuring:\n"${feedback.improvedAnswer}"\n\nNext Question: How do you handle disagreements on technical design trade-offs with cross-functional stakeholders?`,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        score: feedback.score,
        feedback: feedback.feedback,
        improvedAnswer: feedback.improvedAnswer
      };

      setMessages((prev) => [...prev, aiMsg]);
    } catch (e) {
      console.error(e);
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-purple-500/10 text-purple-400 text-xs font-semibold uppercase tracking-wider mb-2">
            <MessageSquare className="w-4 h-4" /> Real-Time Mock Coach
          </div>
          <h1 className="text-3xl font-extrabold text-white">Interactive AI Interview Session</h1>
        </div>

        {/* Category Switcher */}
        <div className="flex bg-slate-900 p-1 rounded-xl border border-slate-800 text-xs font-semibold">
          {(['STAR Method', 'Technical', 'Behavioral'] as const).map((cat) => (
            <button
              key={cat}
              onClick={() => setCategory(cat)}
              className={`px-3 py-1.5 rounded-lg transition-all ${
                category === cat ? 'bg-purple-600 text-white' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      {/* Chat Canvas */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-6 min-h-[500px] flex flex-col justify-between">
        {/* Messages Feed */}
        <div className="space-y-4 max-h-[500px] overflow-y-auto pr-2">
          {messages.map((m) => (
            <div
              key={m.id}
              className={`flex flex-col ${m.sender === 'user' ? 'items-end' : 'items-start'}`}
            >
              <div className="flex items-center gap-2 mb-1">
                <span className="text-[11px] font-semibold text-slate-400">
                  {m.sender === 'ai' ? 'Gemini Interview Coach' : 'You'}
                </span>
                <span className="text-[10px] text-slate-500">{m.timestamp}</span>
              </div>

              <div
                className={`max-w-2xl p-4 rounded-2xl text-xs sm:text-sm leading-relaxed ${
                  m.sender === 'user'
                    ? 'bg-indigo-600 text-white rounded-br-none'
                    : 'bg-slate-950 border border-slate-800 text-slate-200 rounded-bl-none whitespace-pre-line'
                }`}
              >
                {m.text}
              </div>
            </div>
          ))}

          {isProcessing && (
            <div className="flex items-center gap-2 text-xs text-purple-400 animate-pulse">
              <Sparkles className="w-4 h-4 animate-spin" /> Evaluating response and generating feedback...
            </div>
          )}
        </div>

        {/* Input Bar */}
        <div className="pt-4 border-t border-slate-800 flex items-center gap-3">
          <button
            onClick={() => setIsMicActive(!isMicActive)}
            className={`p-3 rounded-xl border transition-all ${
              isMicActive
                ? 'bg-rose-600/20 text-rose-400 border-rose-500/40 animate-pulse'
                : 'bg-slate-950 text-slate-400 border-slate-800 hover:text-white'
            }`}
            title="Toggle Voice Input Simulation"
          >
            {isMicActive ? <Mic className="w-5 h-5" /> : <MicOff className="w-5 h-5" />}
          </button>

          <input
            type="text"
            value={userInput}
            onChange={(e) => setUserInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSend()}
            placeholder={
              isMicActive
                ? 'Listening... Speak your interview response now'
                : 'Type your structured interview response (STAR method recommended)...'
            }
            className="flex-1 px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 text-xs sm:text-sm focus:outline-none focus:border-purple-500/80 transition-colors"
          />

          <button
            onClick={handleSend}
            disabled={!userInput.trim() || isProcessing}
            className="p-3 rounded-xl bg-purple-600 hover:bg-purple-500 text-white disabled:opacity-50 transition-all shadow-md shadow-purple-500/20"
          >
            <Send className="w-5 h-5" />
          </button>
        </div>
      </div>
    </div>
  );
};
