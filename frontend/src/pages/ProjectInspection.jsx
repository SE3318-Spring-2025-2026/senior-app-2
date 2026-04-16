import React, { useState, useEffect } from 'react';
import { getProjectsList, getProjectDeliverablesStatus } from '../services/api';

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
      setProjects(data.content || []); // assuming paginated response uses 'content'
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
      // Fallback or show error
      setDeliverables([]);
    } finally {
      setLoadingDetails(false);
    }
  };

  return (
    <div style={{ padding: '28px', display: 'flex', gap: '20px' }}>
      {/* Sidebar: Projects List */}
      <div style={{ flex: '1', borderRight: '1px solid #ccc', paddingRight: '20px' }}>
        <h1>Projects</h1>
        {loading && <p>Loading projects...</p>}
        {error && <p style={{ color: 'red' }}>{error}</p>}
        {!loading && projects.length === 0 && <p>No projects found.</p>}

        <ul style={{ listStyle: 'none', padding: 0 }}>
          {projects.map(p => (
            <li 
              key={p.id} 
              style={{
                padding: '10px', 
                borderBottom: '1px solid #eee', 
                cursor: 'pointer',
                backgroundColor: selectedProject?.id === p.id ? '#f0f0f0' : 'transparent'
              }}
              onClick={() => handleProjectSelect(p)}
            >
              <strong>{p.name}</strong> <br/>
              <small>{p.term}</small>
            </li>
          ))}
        </ul>
      </div>

      {/* Main Content: Inspection Details */}
      <div style={{ flex: '2', paddingLeft: '20px' }}>
        {!selectedProject && (
          <div style={{ marginTop: '50px', textAlign: 'center', color: '#666' }}>
            <h2>Select a project to inspect</h2>
            <p>Project details and deliverable statuses will appear here.</p>
          </div>
        )}

        {selectedProject && (
          <div>
            <h1>{selectedProject.name} Inspection</h1>
            <p><strong>Term:</strong> {selectedProject.term}</p>
            <hr />

            <h3>Deliverable Statuses</h3>
            {loadingDetails && <p>Loading details...</p>}
            {!loadingDetails && deliverables.length === 0 && (
              <p>No deliverables tracked for this project yet.</p>
            )}

            {!loadingDetails && deliverables.length > 0 && (
              <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '20px' }}>
                <thead>
                  <tr style={{ borderBottom: '2px solid #ccc', textAlign: 'left' }}>
                    <th style={{ padding: '10px' }}>Deliverable ID</th>
                    <th style={{ padding: '10px' }}>Status</th>
                    <th style={{ padding: '10px' }}>System Score</th>
                    <th style={{ padding: '10px' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {deliverables.map(d => (
                    <tr key={d.deliverableId} style={{ borderBottom: '1px solid #eee' }}>
                      <td style={{ padding: '10px' }}>{d.deliverableId}</td>
                      <td style={{ padding: '10px' }}>
                        <span style={{
                          padding: '4px 8px',
                          borderRadius: '12px',
                          backgroundColor: d.status === 'GRADED' ? '#d4edda' : '#fff3cd',
                          color: d.status === 'GRADED' ? '#155724' : '#856404',
                          fontWeight: 'bold',
                          fontSize: '0.85em'
                        }}>
                          {d.status}
                        </span>
                      </td>
                      <td style={{ padding: '10px' }}>{d.score !== null ? d.score : '-'}</td>
                      <td style={{ padding: '10px' }}>
                        <button 
                          style={{ padding: '5px 10px', backgroundColor: '#007bff', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
                          onClick={() => alert('Grading feature will be hooked here (Issue 93 integration)')}
                        >
                          Grade / Inspect
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

export default ProjectInspection;
