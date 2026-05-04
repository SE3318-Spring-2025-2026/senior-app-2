import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyStudentProjects } from '../services/api';
import './StudentProjects.css';

function StudentProjects() {
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    setLoading(true);
    getMyStudentProjects()
      .then((res) => setProjects(Array.isArray(res) ? res : []))
      .catch((e) => setError(e.message || 'Failed to load projects.'))
      .finally(() => setLoading(false));
  }, []);

  const getStatusClass = (status) => {
    if (!status) return 'sp-status-default';
    const s = status.toUpperCase();
    if (s.includes('ACTIVE')) return 'sp-status-active';
    if (s.includes('RESEARCH')) return 'sp-status-research';
    if (s.includes('PLANNING')) return 'sp-status-planning';
    if (s.includes('COMPLETED') || s.includes('DONE')) return 'sp-status-completed';
    return 'sp-status-default';
  };

  const getStatusLabel = (status) => {
    if (!status) return 'Active';
    const s = status.toUpperCase();
    if (s.includes('ACTIVE')) return 'Active Phase';
    if (s.includes('RESEARCH')) return 'Research';
    if (s.includes('PLANNING')) return 'Planning';
    if (s.includes('COMPLETED') || s.includes('DONE')) return 'Completed';
    return status;
  };

  if (loading) {
    return (
      <div className="student-projects-page">
        <div className="sp-header">
          <h1>My Projects</h1>
          <p>Select a workspace to track your academic progress.</p>
        </div>
        <div className="sp-loading">
          <div className="sp-spinner" />
          <span className="sp-loading-text">Loading projects...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="student-projects-page">
      <div className="sp-header">
        <h1>My Projects</h1>
        <p>Select a workspace to track your academic progress.</p>
      </div>

      {error && <div className="sp-error">{error}</div>}

      {!error && projects.length === 0 && (
        <div className="sp-empty">
          <div className="sp-empty-icon">📂</div>
          <p>No projects assigned yet.</p>
        </div>
      )}

      {!error && projects.length > 0 && (
        <div className="sp-grid">
          {projects.map((project) => {
            const progress = project.progress ?? Math.floor(Math.random() * 80 + 10);
            const gpa = project.gpa ?? '-';
            const contribution = project.contribution ?? '-';

            return (
              <div
                key={`${project.projectId}-${project.groupId}`}
                className="sp-card"
                onClick={() => navigate(`/panel/student-projects/${project.projectId}`)}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    navigate(`/panel/student-projects/${project.projectId}`);
                  }
                }}
              >
                {/* Background Watermark Icon */}
                <div className="sp-card-bg-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M20.25 14.15v4.25c0 1.094-.787 2.036-1.872 2.18-2.087.277-4.216.42-6.378.42s-4.291-.143-6.378-.42c-1.085-.144-1.872-1.086-1.872-2.18v-4.25m16.5 0a2.18 2.18 0 0 0 .75-1.661V8.706c0-1.081-.768-2.015-1.837-2.175a48.114 48.114 0 0 0-3.413-.387m4.5 8.006c-.194.165-.42.295-.673.38A23.978 23.978 0 0 1 12 15.75c-2.648 0-5.195-.429-7.577-1.22a2.016 2.016 0 0 1-.673-.38m0 0A2.18 2.18 0 0 1 3 12.489V8.706c0-1.081.768-2.015 1.837-2.175a48.111 48.111 0 0 1 3.413-.387m7.5 0V5.25A2.25 2.25 0 0 0 13.5 3h-3a2.25 2.25 0 0 0-2.25 2.25v.894m7.5 0a48.667 48.667 0 0 0-7.5 0M12 12.75h.008v.008H12v-.008Z" />
                  </svg>
                </div>

                {/* Top Row */}
                <div className="sp-card-top">
                  <span className={`sp-status-badge ${getStatusClass(project.status)}`}>
                    {getStatusLabel(project.status)}
                  </span>
                </div>
                
                <h3 className="sp-card-title">{project.projectTitle}</h3>
                <p className="sp-card-desc">
                  {project.description || `Project #${project.projectId} • Group #${project.groupId}`}
                </p>

                {/* Stats */}
                <div className="sp-stats-row">
                  <div className="sp-stat">
                    <span className="sp-stat-label">CPA</span>
                    <span className="sp-stat-value gpa">{gpa}</span>
                  </div>
                  <div className="sp-stat">
                    <span className="sp-stat-label">CONTRIBUTION</span>
                    <span className="sp-stat-value contribution">{contribution}%</span>
                  </div>
                </div>

                {/* Progress */}
                <div className="sp-progress-row">
                  <span className="sp-progress-label">PROGRESS</span>
                  <div className="sp-progress-bar">
                    <div
                      className="sp-progress-fill"
                      style={{ width: `${progress}%` }}
                    />
                  </div>
                  <span className="sp-progress-pct">{progress}%</span>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default StudentProjects;
