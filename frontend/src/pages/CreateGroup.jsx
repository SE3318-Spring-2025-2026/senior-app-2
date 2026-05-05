import React, { useEffect, useState } from 'react';
import { getProjectTemplates } from '../services/api';
import './CreateGroup.css';

const CreateGroup = () => {
  const [groupName, setGroupName] = useState('');
  const [projectId, setProjectId] = useState('');
  const [error, setError] = useState('');
  const [templates, setTemplates] = useState([]);
  const [loadingTemplates, setLoadingTemplates] = useState(true);

  useEffect(() => {
    async function loadTemplates() {
      try {
        const data = await getProjectTemplates();
        setTemplates(data);
      } catch (err) {
        setError(err.message || 'Failed to load project templates.');
      } finally {
        setLoadingTemplates(false);
      }
    }

    loadTemplates();
  }, []);

  const handleSubmit = (e) => {
    e.preventDefault();

    if (!groupName.trim()) {
      setError('Please enter a group name.');
      return;
    }
    if (!projectId) {
      setError('Please select a project template.');
      return;
    }

    setError('');
    console.log('Submitting Group Data:', { groupName, projectId });
    alert(`Group "${groupName}" has been created successfully!`);
  };

  return (
    <div className="create-group-page">
      <div className="create-group-layout">
        <div className="create-group-card">
          <h2>Group Creation Wizard</h2>

          {error && <div className="create-group-error">{error}</div>}

          <form onSubmit={handleSubmit} className="create-group-form">
            <div>
              <label>Group Name</label>
              <input
                type="text"
                value={groupName}
                onChange={(e) => setGroupName(e.target.value)}
                placeholder="Enter group name..."
                required
              />
            </div>

            <div>
              <label>Project Template</label>
              <select
                value={projectId}
                onChange={(e) => setProjectId(e.target.value)}
                required
                disabled={loadingTemplates || templates.length === 0}
              >
                <option value="">Select a template...</option>
                {templates.map((template) => (
                  <option key={template.projectId} value={template.projectId}>
                    {template.name}
                  </option>
                ))}
              </select>
            </div>

            <button type="submit" className="create-group-button">
              Create Group
            </button>
          </form>
        </div>

        <section className="template-browser" aria-label="Available project templates">
          <div className="template-browser-header">
            <h3>Available Templates</h3>
            <p>Review sprint count and deliverables before creating a group.</p>
          </div>

          {loadingTemplates && (
            <div className="template-loading">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
                style={{ animation: 'spin 1s linear infinite', verticalAlign: 'middle', marginRight: 8 }}>
                <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/>
              </svg>
              Loading templates…
            </div>
          )}

          {!loadingTemplates && templates.length === 0 && (
            <div className="template-empty">No project templates are currently available.</div>
          )}

          {!loadingTemplates && templates.length > 0 && (
            <div className="template-grid">
              {templates.map((template) => (
                <article
                  key={template.projectId}
                  className={`template-card${projectId === String(template.projectId) ? ' selected' : ''}`}
                  onClick={() => setProjectId(String(template.projectId))}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => e.key === 'Enter' && setProjectId(String(template.projectId))}
                  aria-pressed={projectId === String(template.projectId)}
                >
                  {projectId === String(template.projectId) && (
                    <div className="template-selected-badge">✓ Selected</div>
                  )}
                  <h4>{template.name}</h4>
                  <p className="template-meta">Sprint count: {template.sprintCount}</p>
                  <div className="template-deliverables-title">Deliverables</div>
                  <ul>
                    {template.deliverables.map((deliverable) => (
                      <li key={deliverable}>{deliverable}</li>
                    ))}
                  </ul>
                </article>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  );
};

export default CreateGroup;