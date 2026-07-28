import React, { createContext, useContext, useState, useEffect } from 'react';
import {
  AtsAnalysisResult,
  InterviewSession,
  JobApplication,
  RoadmapGoal,
  UserProfile,
  ResumeData
} from '../types';

interface AppContextType {
  user: UserProfile;
  updateUser: (fields: Partial<UserProfile>) => void;
  atsResult: AtsAnalysisResult | null;
  setAtsResult: (res: AtsAnalysisResult | null) => void;
  jobApplications: JobApplication[];
  addJobApplication: (app: Omit<JobApplication, 'id'>) => void;
  updateJobStatus: (id: string, status: JobApplication['status']) => void;
  deleteJobApplication: (id: string) => void;
  interviewSessions: InterviewSession[];
  addInterviewSession: (session: InterviewSession) => void;
  roadmapGoals: RoadmapGoal[];
  toggleRoadmapGoal: (id: string) => void;
  resumeData: ResumeData;
  updateResumeData: (data: Partial<ResumeData>) => void;
}

const defaultResume: ResumeData = {
  fullName: 'Alex Morgan',
  email: 'alex.morgan@tech.dev',
  phone: '+1 (555) 234-5678',
  linkedin: 'linkedin.com/in/alexm-dev',
  github: 'github.com/alexm-code',
  summary: 'Senior Full-Stack & Mobile Software Engineer with 6+ years of experience building scalable distributed web applications, reactive Compose UIs, and integrating AI models into user workflows.',
  experiences: [
    {
      id: 'exp-1',
      company: 'Apex Tech Solutions',
      role: 'Senior Staff Engineer',
      duration: '2022 - Present',
      bulletPoints: [
        'Led team of 8 engineers in migrating legacy Android app to Jetpack Compose, improving render frames by 40%.',
        'Engineered cloud backend services handling over 1.2M daily active API requests with 99.99% uptime.'
      ]
    },
    {
      id: 'exp-2',
      company: 'Nexus Software Inc.',
      role: 'Full Stack Engineer',
      duration: '2019 - 2022',
      bulletPoints: [
        'Built real-time collaboration tools using React, TypeScript, and WebSocket architectures.',
        'Integrated automated CI/CD pipelines reducing deployment friction by 60%.'
      ]
    }
  ],
  education: [
    {
      id: 'edu-1',
      institution: 'University of California, Berkeley',
      degree: 'B.S. in Computer Science',
      year: '2019'
    }
  ],
  skills: ['Kotlin', 'TypeScript', 'React', 'Jetpack Compose', 'Node.js', 'System Design', 'Gemini AI', 'Tailwind CSS', 'Docker', 'GraphQL']
};

const initialApplications: JobApplication[] = [
  { id: '1', companyName: 'Google', roleTitle: 'Senior Software Engineer', location: 'Mountain View, CA', salaryRange: '$180k - $240k', status: 'Interview', appliedDate: '2026-07-15', matchScore: 92 },
  { id: '2', companyName: 'Stripe', roleTitle: 'Staff Frontend Developer', location: 'Remote', salaryRange: '$190k - $250k', status: 'Applied', appliedDate: '2026-07-20', matchScore: 88 },
  { id: '3', companyName: 'Linear', roleTitle: 'Product Engineer', location: 'San Francisco, CA', salaryRange: '$170k - $220k', status: 'Wishlist', appliedDate: '2026-07-22', matchScore: 95 },
  { id: '4', companyName: 'Netflix', roleTitle: 'Lead UI/UX Architect', location: 'Los Gatos, CA', salaryRange: '$210k - $280k', status: 'Offer', appliedDate: '2026-06-30', matchScore: 96 }
];

const initialRoadmap: RoadmapGoal[] = [
  { id: 'r1', week: 1, title: 'Master STAR Interview Framework', description: 'Practice structuring behavioral stories with measurable quantitative outcomes.', skills: ['Behavioral Prep', 'STAR Method'], completed: true, courses: [{ name: 'Behavioral Mastery', url: '#' }] },
  { id: 'r2', week: 2, title: 'Deep Dive into System Design Patterns', description: 'Review distributed caching, microservice scaling, and offline sync mechanics.', skills: ['System Design', 'Cache Invalidation'], completed: true, courses: [{ name: 'High Scale Systems', url: '#' }] },
  { id: 'r3', week: 3, title: 'ATS Keyword Alignment & Portfolio Polish', description: 'Optimize bullet points for ATS crawlers and update open-source repositories.', skills: ['ATS Optimization', 'Git Clean Code'], completed: false, courses: [{ name: 'ATS Formula Guide', url: '#' }] },
  { id: 'r4', week: 4, title: 'Executive Mock Interviews & Salary Negotiation', description: 'Conduct timed live AI mock interview loops with real-time feedback.', skills: ['Live Prep', 'Negotiation Strategies'], completed: false, courses: [{ name: 'Offer Negotiation Blueprint', url: '#' }] }
];

const AppContext = createContext<AppContextType | undefined>(undefined);

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserProfile>({
    name: 'Alex Morgan',
    email: 'alex.morgan@tech.dev',
    targetRole: 'Senior Full Stack Engineer',
    experienceLevel: 'Mid-Senior',
    isDarkMode: true,
    plan: 'Pro Member'
  });

  const [atsResult, setAtsResult] = useState<AtsAnalysisResult | null>(null);
  const [jobApplications, setJobApplications] = useState<JobApplication[]>(initialApplications);
  const [interviewSessions, setInterviewSessions] = useState<InterviewSession[]>([]);
  const [roadmapGoals, setRoadmapGoals] = useState<RoadmapGoal[]>(initialRoadmap);
  const [resumeData, setResumeData] = useState<ResumeData>(defaultResume);

  useEffect(() => {
    if (user.isDarkMode) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [user.isDarkMode]);

  const updateUser = (fields: Partial<UserProfile>) => {
    setUser((prev) => ({ ...prev, ...fields }));
  };

  const addJobApplication = (app: Omit<JobApplication, 'id'>) => {
    const newApp: JobApplication = {
      ...app,
      id: Date.now().toString()
    };
    setJobApplications((prev) => [newApp, ...prev]);
  };

  const updateJobStatus = (id: string, status: JobApplication['status']) => {
    setJobApplications((prev) =>
      prev.map((app) => (app.id === id ? { ...app, status } : app))
    );
  };

  const deleteJobApplication = (id: string) => {
    setJobApplications((prev) => prev.filter((app) => app.id !== id));
  };

  const addInterviewSession = (session: InterviewSession) => {
    setInterviewSessions((prev) => [session, ...prev]);
  };

  const toggleRoadmapGoal = (id: string) => {
    setRoadmapGoals((prev) =>
      prev.map((goal) => (goal.id === id ? { ...goal, completed: !goal.completed } : goal))
    );
  };

  const updateResumeData = (data: Partial<ResumeData>) => {
    setResumeData((prev) => ({ ...prev, ...data }));
  };

  return (
    <AppContext.Provider
      value={{
        user,
        updateUser,
        atsResult,
        setAtsResult,
        jobApplications,
        addJobApplication,
        updateJobStatus,
        deleteJobApplication,
        interviewSessions,
        addInterviewSession,
        roadmapGoals,
        toggleRoadmapGoal,
        resumeData,
        updateResumeData
      }}
    >
      {children}
    </AppContext.Provider>
  );
};

export const useApp = () => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
};
