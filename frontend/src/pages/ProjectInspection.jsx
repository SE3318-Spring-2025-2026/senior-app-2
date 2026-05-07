import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getProjectDetail, getProjects } from '../services/api';

function ProjectInspection() {
  const { templateId } = useParams();
  const [projects, setProjects] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState(null);
  const [projectDetail, setProjectDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    setError('');
    getProjects({ templateId: Number(templateId) })
      .then((res) => {
        const list = res?.data || [];
        setProjects(list);
        if (list.length > 0) {
          setSelectedProjectId(list[0].projectId);
        }
      })
      .catch((e) => setError(e.message || 'Failed to load project list.'))
      .finally(() => setLoading(false));
  }, [templateId]);

  useEffect(() => {
    if (!selectedProjectId) {
      setProjectDetail(null);
      return;
    }
    setLoadingDetail(true);
    getProjectDetail(selectedProjectId)
      .then((res) => setProjectDetail(res?.data || null))
      .catch((e) => setError(e.message || 'Failed to load project detail.'))
      .finally(() => setLoadingDetail(false));
  }, [selectedProjectId]);

  return (
    <div style={{ padding: '24px', display: 'grid', gridTemplateColumns: '320px 1fr', gap: '16px' }}>
      <aside style={{ border: '1px solid #e5e7eb', borderRadius: '12px', padding: '14px', background: '#fff' }}>
        <h2 style={{ marginTop: 0 }}>Projects</h2>
        {loading && <p>Loading...</p>}
        {error && <p style={{ color: '#b91c1c' }}>{error}</p>}
        {!loading && projects.length === 0 && <p>No project created from this template yet.</p>}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {projects.map((p) => (
            <button
              key={p.projectId}
              onClick={() => setSelectedProjectId(p.projectId)}
              style={{
                textAlign: 'left',
                border: '1px solid #e5e7eb',
                borderRadius: '10px',
                padding: '10px',
                background: selectedProjectId === p.projectId ? '#eef2ff' : '#fff',
                cursor: 'pointer',
              }}
            >
              <strong>{p.title}</strong>
              <div style={{ color: '#6b7280', fontSize: '13px' }}>{p.term}</div>
            </button>
          ))}
        </div>
      </aside>

      <main style={{ border: '1px solid #e5e7eb', borderRadius: '12px', padding: '14px', background: '#fff' }}>
        <h2 style={{ marginTop: 0 }}>Project Inspection</h2>
        {!selectedProjectId && <p>Select a project.</p>}
        {loadingDetail && <p>Loading detail...</p>}
        {projectDetail && !loadingDetail && (
          <div>
            <p><strong>Title:</strong> {projectDetail.title}</p>
            <p><strong>Status:</strong> {projectDetail.status}</p>
            <p><strong>Term:</strong> {projectDetail.term}</p>
            <hr />
            <h3>Sprints</h3>
            {projectDetail.sprints?.length ? projectDetail.sprints.map((sprint) => (
              <div key={sprint.sprintNo} style={{ marginBottom: '12px', border: '1px solid #f1f5f9', borderRadius: '8px', padding: '10px' }}>
                <strong>{sprint.title}</strong>
                <div style={{ fontSize: '13px', color: '#6b7280' }}>
                  {sprint.startDate || '-'} to {sprint.endDate || '-'}
                </div>
                <div style={{ marginTop: '6px' }}>
                  <div><strong>Deliverables:</strong> {sprint.deliverables?.length || 0}</div>
                  <div><strong>Evaluations:</strong> {sprint.evaluations?.length || 0}</div>
                </div>
              </div>
            )) : <p>No sprint data.</p>}
          </div>
        )}
      </main>
    </div>
  );
}

export default ProjectInspection;
