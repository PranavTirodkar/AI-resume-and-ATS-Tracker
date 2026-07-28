import React, { useState } from 'react';
import { Kanban, Plus, Trash2, Building2, MapPin, DollarSign } from 'lucide-react';
import { useApp } from '../context/AppContext';
import { JobApplication } from '../types';

export const JobTrackerPage: React.FC = () => {
  const { jobApplications, addJobApplication, updateJobStatus, deleteJobApplication } = useApp();

  const columns: JobApplication['status'][] = ['Wishlist', 'Applied', 'Interview', 'Offer', 'Rejected'];

  const [showAddModal, setShowAddModal] = useState(false);
  const [newCompany, setNewCompany] = useState('');
  const [newRole, setNewRole] = useState('');
  const [newLocation, setNewLocation] = useState('Remote');
  const [newSalary, setNewSalary] = useState('');

  const handleCreate = () => {
    if (!newCompany.trim() || !newRole.trim()) return;
    addJobApplication({
      companyName: newCompany,
      roleTitle: newRole,
      location: newLocation,
      salaryRange: newSalary || undefined,
      status: 'Wishlist',
      appliedDate: new Date().toISOString().split('T')[0],
      matchScore: 90
    });
    setNewCompany('');
    setNewRole('');
    setNewSalary('');
    setShowAddModal(false);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider mb-2">
            <Kanban className="w-4 h-4" /> Application Pipeline
          </div>
          <h1 className="text-3xl font-extrabold text-white">Kanban Job Application Tracker</h1>
        </div>

        <button
          onClick={() => setShowAddModal(true)}
          className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-sm flex items-center gap-2 shadow-lg shadow-indigo-500/25 transition-all"
        >
          <Plus className="w-4 h-4" /> Add Application
        </button>
      </div>

      {/* Kanban Board Columns */}
      <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-4 overflow-x-auto pb-4">
        {columns.map((col) => {
          const appsInCol = jobApplications.filter((a) => a.status === col);
          return (
            <div key={col} className="p-4 rounded-2xl bg-slate-900/40 border border-slate-800 space-y-4 min-w-[220px]">
              <div className="flex items-center justify-between">
                <span className="font-bold text-xs uppercase tracking-wider text-slate-300">{col}</span>
                <span className="text-xs font-bold text-slate-500 bg-slate-950 px-2 py-0.5 rounded-md">
                  {appsInCol.length}
                </span>
              </div>

              <div className="space-y-3">
                {appsInCol.map((app) => (
                  <div
                    key={app.id}
                    className="p-4 rounded-xl bg-slate-950/90 border border-slate-800 space-y-3 hover:border-indigo-500/40 transition-all group"
                  >
                    <div className="flex justify-between items-start">
                      <div>
                        <h4 className="font-bold text-white text-xs">{app.roleTitle}</h4>
                        <div className="text-[11px] font-medium text-slate-400 mt-0.5 flex items-center gap-1">
                          <Building2 className="w-3 h-3 text-slate-500" /> {app.companyName}
                        </div>
                      </div>
                      <button
                        onClick={() => deleteJobApplication(app.id)}
                        className="text-slate-600 hover:text-rose-400 transition-colors opacity-0 group-hover:opacity-100"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>

                    <div className="flex items-center justify-between text-[10px] text-slate-400 pt-1">
                      <span className="flex items-center gap-1">
                        <MapPin className="w-3 h-3 text-slate-500" /> {app.location}
                      </span>
                      {app.matchScore && (
                        <span className="font-semibold text-indigo-400">{app.matchScore}% Match</span>
                      )}
                    </div>

                    {/* Move Status Selector */}
                    <select
                      value={app.status}
                      onChange={(e) => updateJobStatus(app.id, e.target.value as JobApplication['status'])}
                      className="w-full text-[10px] font-semibold bg-slate-900 border border-slate-800 text-slate-300 rounded-md p-1 focus:outline-none"
                    >
                      {columns.map((c) => (
                        <option key={c} value={c}>{c}</option>
                      ))}
                    </select>
                  </div>
                ))}
              </div>
            </div>
          );
        })}
      </div>

      {/* Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-md p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-4">
            <h3 className="font-bold text-white text-lg">Add New Application</h3>

            <input
              type="text"
              placeholder="Company Name (e.g. Stripe)"
              value={newCompany}
              onChange={(e) => setNewCompany(e.target.value)}
              className="w-full p-3 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white"
            />
            <input
              type="text"
              placeholder="Role Title (e.g. Staff Engineer)"
              value={newRole}
              onChange={(e) => setNewRole(e.target.value)}
              className="w-full p-3 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white"
            />
            <input
              type="text"
              placeholder="Location (e.g. Remote / SF)"
              value={newLocation}
              onChange={(e) => setNewLocation(e.target.value)}
              className="w-full p-3 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white"
            />

            <div className="flex gap-3 pt-2">
              <button
                onClick={() => setShowAddModal(false)}
                className="flex-1 py-2.5 rounded-xl bg-slate-800 text-slate-300 text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                onClick={handleCreate}
                className="flex-1 py-2.5 rounded-xl bg-indigo-600 text-white text-xs font-semibold"
              >
                Save
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
