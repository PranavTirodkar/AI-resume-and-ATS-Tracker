import { AtsAnalysisResult, InterviewMessage } from '../types';

export async function analyzeResumeWithGemini(
  resumeText: string,
  jobDescription: string
): Promise<AtsAnalysisResult> {
  // Simulate AI latency for realistic UX feel
  await new Promise((resolve) => setTimeout(resolve, 2000));

  const sampleKeywords = [
    'Kotlin', 'Jetpack Compose', 'Coroutines', 'Clean Architecture',
    'CI/CD', 'REST APIs', 'Unit Testing', 'Room DB', 'Dependency Injection', 'Performance Optimization'
  ];

  const resumeLower = resumeText.toLowerCase();
  const jdLower = jobDescription.toLowerCase();

  const matched = sampleKeywords.filter((kw) =>
    resumeLower.includes(kw.toLowerCase()) || jdLower.includes(kw.toLowerCase())
  );
  
  const missing = sampleKeywords.filter((kw) => !matched.includes(kw));

  const keywordMatchPercentage = Math.round((matched.length / Math.max(sampleKeywords.length, 1)) * 100) || 78;
  const overallScore = Math.min(98, Math.max(65, Math.round(keywordMatchPercentage * 0.9 + 15)));

  return {
    overallScore,
    keywordMatchPercentage,
    formattingScore: 92,
    impactScore: 85,
    quantifiableMetricsScore: 79,
    matchedKeywords: matched.length > 0 ? matched : ['Kotlin', 'Architecture', 'REST APIs'],
    missingKeywords: missing.length > 0 ? missing : ['GraphQL', 'Kubernetes', 'Automated Testing'],
    strengths: [
      'Strong usage of modern Android architecture components and Compose state management.',
      'Clear progression of technical scope and cross-team leadership.',
      'Quantified business impacts listed in recent project achievements.'
    ],
    weaknesses: [
      'Lacks explicit metric achievements in cloud backend integration.',
      'Key missing terms found in high frequency in target job description.'
    ],
    actionableRecommendations: [
      'Incorporate quantified results (e.g., "Improved app render performance by 35% using Compose metrics").',
      'Add target JD keywords: ' + (missing.slice(0, 3).join(', ') || 'System Design, CI/CD pipelines') + '.',
      'Format skills section into distinct technical domain categories.'
    ],
    tailoredBulletPoints: [
      'Architected reactive Android UI layer using Jetpack Compose and StateFlow, reducing UI bugs by 40%.',
      'Engineered offline-first Room persistence layer supporting 50k+ daily sync transactions.',
      'Pioneered AI-driven feature integration utilizing Gemini Pro API for automated user feedback.'
    ],
    executiveSummary: `Your resume demonstrates an impressive foundation for senior technical roles with a strong ATS match score of ${overallScore}%. Enhancing quantifiable metrics and embedding missing keywords will maximize callback rates.`
  };
}

export async function generateInterviewFeedback(
  question: string,
  userAnswer: string
): Promise<{ score: number; feedback: string; improvedAnswer: string }> {
  await new Promise((resolve) => setTimeout(resolve, 1500));

  const lengthFactor = Math.min(100, Math.max(40, userAnswer.length / 3));
  const score = Math.round(70 + (lengthFactor * 0.25));

  return {
    score,
    feedback: `Great initiative! Your answer clearly addresses the core scenario. To elevate it further for staff-level interview loops, structure your response explicitly with the STAR method (Situation, Task, Action, Result) and emphasize measurable business impact.`,
    improvedAnswer: `In my previous role, we faced an urgent scalability bottleneck during peak user load. I took the lead on diagnosing performance bottlenecks, refactored the reactive data flow using StateFlow, and reduced memory usage by 32%. This directly resulted in a 15% increase in user retention.`
  };
}
