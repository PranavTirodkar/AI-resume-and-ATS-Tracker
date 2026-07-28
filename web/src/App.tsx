import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react_router_dom';
import { AppProvider } from './context/AppContext';
import { Navbar } from './components/Navbar';
import { Footer } from './components/Footer';

import { LandingPage } from './pages/LandingPage';
import { DashboardPage } from './pages/DashboardPage';
import { ATSPage } from './pages/ATSPage';
import { MockInterviewPage } from './pages/MockInterviewPage';
import { RoadmapPage } from './pages/RoadmapPage';
import { JobTrackerPage } from './pages/JobTrackerPage';
import { ResumeBuilderPage } from './pages/ResumeBuilderPage';
import { PricingPage } from './pages/PricingPage';
import { ProfilePage } from './pages/ProfilePage';

export const App: React.FC = () => {
  return (
    <AppProvider>
      <Router>
        <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100 selection:bg-indigo-500 selection:text-white">
          <Navbar />
          <main className="flex-1">
            <Routes>
              <Route path="/" element={<LandingPage />} />
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/ats" element={<ATSPage />} />
              <Route path="/interview" element={<MockInterviewPage />} />
              <Route path="/roadmap" element={<RoadmapPage />} />
              <Route path="/tracker" element={<JobTrackerPage />} />
              <Route path="/builder" element={<ResumeBuilderPage />} />
              <Route path="/pricing" element={<PricingPage />} />
              <Route path="/profile" element={<ProfilePage />} />
            </Routes>
          </main>
          <Footer />
        </div>
      </Router>
    </AppProvider>
  );
};

export default App;
