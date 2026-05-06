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
                {/* Top Row */}
                <div className="sp-card-top">
                  <div className="sp-card-info">
                    <span className={`sp-status-badge ${getStatusClass(project.status)}`}>
                      {getStatusLabel(project.status)}
                    </span>
                    <h3 className="sp-card-title">{project.projectTitle}</h3>
                    <p className="sp-card-desc">
                      {project.description || `Project #${project.projectId} • Group #${project.groupId}`}
                    </p>
                  </div>
                  <div className="sp-card-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                      <rect x="3" y="3" width="7" height="7" rx="1" />
                      <rect x="14" y="3" width="7" height="7" rx="1" />
                      <rect x="3" y="14" width="7" height="7" rx="1" />
                      <rect x="14" y="14" width="7" height="7" rx="1" />
                    </svg>
                  </div>
                </div>

                {/* Stats */}
                <div className="sp-stats-row">
                  <div className="sp-stat">
                    <span className="sp-stat-label">GPA</span>
                    <span className="sp-stat-value gpa">{gpa}</span>
                  </div>
                  <div className="sp-stat">
                    <span className="sp-stat-label">Contribution</span>
                    <span className="sp-stat-value contribution">{contribution}</span>
                  </div>
                </div>

                {/* Progress */}
                <div className="sp-progress-row">
                  <span className="sp-progress-label">Progress</span>
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
