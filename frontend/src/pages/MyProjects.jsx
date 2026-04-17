import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getProjectTemplates } from '../services/api';
import './MyProjects.css';

function formatDateTime(value) {
  if (!value) return '-';
  try {
    return new Date(value).toLocaleString();
  } catch {
    return value;
  }
}

function MyProjects() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    async function load() {
      setLoading(true);
      setError('');
      try {
        const response = await getProjectTemplates();
        if (!active) return;
        setTemplates(response?.data || []);
      } catch (err) {
        if (!active) return;
        setError(err.message || 'Failed to load templates.');
      } finally {
        if (active) setLoading(false);
      }
    }
    load();
    return () => {
      active = false;
    };
  }, []);

  return (
    <div className="my-projects-page">
      <div className="my-projects-header">
        <h1>My Projects</h1>
        <p>Project templates are listed below.</p>
      </div>

      {loading && <div className="my-projects-state">Loading templates...</div>}
      {error && !loading && <div className="my-projects-state error">{error}</div>}

      {!loading && !error && (
        <div className="my-projects-grid">
          {templates.map((template) => (
            <article
              key={template.templateId}
              className="my-project-card"
              onClick={() => navigate(`/panel/templates/${template.templateId}`)}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  navigate(`/panel/templates/${template.templateId}`);
                }
              }}
            >
              <h3>{template.name}</h3>
              <div className="meta-row">
                <span className="label">Term</span>
                <span>{template.term || '-'}</span>
              </div>
              <div className="meta-row">
                <span className="label">Version</span>
                <span>{template.version ?? '-'}</span>
              </div>
              <div className="meta-row">
                <span className="label">Template ID</span>
                <span>{template.templateId}</span>
              </div>
              <div className="meta-row">
                <span className="label">Active</span>
                <span>{template.active ? 'Yes' : 'No'}</span>
              </div>
              <div className="meta-row">
                <span className="label">Created At</span>
                <span>{formatDateTime(template.createdAt)}</span>
              </div>
              {user?.role === 'COORDINATOR' && (
                <div className="card-actions">
                  <button
                    type="button"
                    className="manage-comitees-btn"
                    onClick={(e) => {
                      e.stopPropagation();
                      navigate(`/panel/templates/${template.templateId}/manage-comitees`);
                    }}
                  >
                    Manage Comitees
                  </button>
                </div>
              )}
            </article>
          ))}
        </div>
      )}

      {!loading && !error && templates.length === 0 && (
        <div className="my-projects-state">No templates yet.</div>
      )}
    </div>
  );
}

export default MyProjects;
