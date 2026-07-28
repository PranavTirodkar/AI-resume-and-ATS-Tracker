export interface AtsAnalysisResult {
  overallScore: number;
  keywordMatchPercentage: number;
  formattingScore: number;
  impactScore: number;
  quantifiableMetricsScore: number;
  matchedKeywords: string[];
  missingKeywords: string[];
  strengths: string[];
  weaknesses: string[];
  actionableRecommendations: string[];
  tailoredBulletPoints: string[];
  executiveSummary: string;
}

export interface InterviewQuestion {
  id: string;
  category: 'Technical' | 'Behavioral' | 'System Design' | 'STAR Method';
  question: string;
  expectedKeyPoints: string[];
  difficulty: 'Easy' | 'Medium' | 'Hard';
}

export interface InterviewMessage {
  id: string;
  sender: 'ai' | 'user';
  text: string;
  timestamp: string;
  score?: number;
  feedback?: string;
  improvedAnswer?: string;
}

export interface InterviewSession {
  id: string;
  roleTitle: string;
  date: string;
  questionsAnswered: number;
  averageScore: number;
  messages: InterviewMessage[];
}

export interface JobApplication {
  id: string;
  companyName: string;
  roleTitle: string;
  location: string;
  salaryRange?: string;
  status: 'Wishlist' | 'Applied' | 'Interview' | 'Offer' | 'Rejected';
  appliedDate: string;
  notes?: string;
  matchScore?: number;
}

export interface RoadmapGoal {
  id: string;
  week: number;
  title: string;
  description: string;
  skills: string[];
  completed: boolean;
  courses: { name: string; url: string }[];
}

export interface UserProfile {
  name: string;
  email: string;
  targetRole: string;
  experienceLevel: 'Entry Level' | 'Mid-Senior' | 'Lead / Executive';
  apiKey?: string;
  isDarkMode: boolean;
  plan: 'Free Trial' | 'Pro Member' | 'Enterprise';
}

export interface ResumeData {
  fullName: string;
  email: string;
  phone: string;
  linkedin: string;
  github: string;
  summary: string;
  experiences: {
    id: string;
    company: string;
    role: string;
    duration: string;
    bulletPoints: string[];
  }[];
  education: {
    id: string;
    institution: string;
    degree: string;
    year: string;
  }[];
  skills: string[];
}
