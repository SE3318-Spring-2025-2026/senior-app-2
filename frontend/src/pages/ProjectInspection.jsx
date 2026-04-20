import React, { useState, useEffect } from 'react';
import { getProjectsList, getProjectDeliverablesStatus } from '../services/api';
import './ProjectInspection.css';

function ProjectInspection() {
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [selectedProject, setSelectedProject] = useState(null);
  const [deliverables, setDeliverables] = useState([]);
  const [loadingDetails, setLoadingDetails] = useState(false);

  useEffect(() => {
    fetchProjects();
  }, []);

  const fetchProjects = async () => {
    try {
      setLoading(true);
      const data = await getProjectsList();
      setProjects(data.content || []);
    } catch (err) {
      setError(err.message || 'Failed to fetch projects');
    } finally {
      setLoading(false);
    }
  };

  const handleProjectSelect = async (project) => {
    setSelectedProject(project);
    try {
      setLoadingDetails(true);
      const data = await getProjectDeliverablesStatus(project.id);
      setDeliverables(data);
    } catch (err) {
      console.error(err);
      setDeliverables([]);
    } finally {
      setLoadingDetails(false);
    }
  };

  return (
    <div className="inspection-dashboard">
      {/* Sidebar: Projects List */}
      <div className="inspection-sidebar">
        <div className="sidebar-header">
          <h1>Projects</h1>
        </div>
        
        {loading && <p className="loading-text">Loading projects...</p>}
        {error && <p style={{ color: '#d93025', padding: '15px' }}>{error}</p>}
        {!loading && projects.length === 0 && !error && <p className="loading-text">No projects found.</p>}

        <ul className="project-list">
          {projects.map(p => (
            <li 
              key={p.id} 
              className={`project-item ${selectedProject?.id === p.id ? 'active' : ''}`}
              onClick={() => handleProjectSelect(p)}
            >
              <span className="project-name">{p.name}</span>
              <span className="project-term">{p.term}</span>
            </li>
          ))}
        </ul>
      </div>

      {/* Main Content: Inspection Details */}
      <div className="inspection-main">
        {!selectedProject && (
          <div className="empty-state">
            <h2>Select a project to inspect</h2>
            <p>Project details and deliverable statuses will appear here.</p>
          </div>
        )}

        {selectedProject && (
          <>
            <div className="details-header">
              <h1>{selectedProject.name}</h1>
              <p><strong>Term:</strong> {selectedProject.term}</p>
            </div>

            <div className="details-content">
              <h3>Deliverable Statuses</h3>
              
              {loadingDetails && <p className="loading-text">Loading deliverable details...</p>}
              {!loadingDetails && deliverables.length === 0 && (
                <p className="loading-text">No deliverables tracked for this project yet.</p>
              )}

              {!loadingDetails && deliverables.length > 0 && (
                <table className="deliverables-table">
                  <thead>
                    <tr>
                      <th>Deliverable ID</th>
                      <th>Status</th>
                      <th>System Score</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {deliverables.map(d => (
                      <tr key={d.deliverableId}>
                        <td>#{d.deliverableId}</td>
                        <td>
                          <span className={`status-badge ${d.status === 'GRADED' ? 'graded' : 'pending'}`}>
                            {d.status}
                          </span>
                        </td>
                        <td>{d.score !== null ? <strong>{d.score}</strong> : <span style={{ color: '#a0a0a0' }}>—</span>}</td>
                        <td>
                          <button 
                            className="action-btn"
                            onClick={() => alert(`Navigating to Grade Interface for Deliverable ${d.deliverableId}`)}
                          >
                            Inspect / Grade
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
}

export default ProjectInspection;
