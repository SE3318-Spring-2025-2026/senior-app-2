import { useAuth } from '../context/AuthContext';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAdvisorLiveGrades, getMyGroupInvites, getMyTeams, getProjectTemplates, getStudentDashboard, respondGroupInvite } from '../services/api';
import InviteCard from '../components/InviteCard';
import './Dashboard.css';

function Dashboard() {
  const { user } = useAuth();
  const [data, setData] = useState({ activeProjects: [], pendingDeliverables: [], invitations: [] });
  const [staffData, setStaffData] = useState({ templates: [], teams: [] });
  const [staffInvites, setStaffInvites] = useState([]);
  const [advisorLiveGrades, setAdvisorLiveGrades] = useState([]);
  const [loading, setLoading] = useState(false);
  const [inviteProcessing, setInviteProcessing] = useState({});
  const [committeeSelection, setCommitteeSelection] = useState({
    inviteId: null,
    options: [],
    selectedCommitteeId: null,
    loading: false,
    error: '',
  });
  const navigate = useNavigate();
  const isStudent = user?.role === 'STUDENT';
  const isCoordinator = user?.role === 'COORDINATOR';
  const isProfessor = user?.role === 'PROFESSOR';

  useEffect(() => {
    if (!isStudent) return;
    setLoading(true);
    getStudentDashboard()
      .then((res) => {
        setData(res?.data || { activeProjects: [], pendingDeliverables: [], invitations: [] });
      })
      .finally(() => setLoading(false));
  }, [isStudent]);

  useEffect(() => {
    if (!isCoordinator && !isProfessor) return;
    setLoading(true);
    Promise.all([getProjectTemplates(), getMyTeams(), getMyGroupInvites()])
      .then(([templatesRes, teamsRes, invitesRes]) => {
        setStaffData({
          templates: templatesRes?.data || [],
          teams: teamsRes?.data || [],
        });
        setStaffInvites(invitesRes?.data || []);
      })
      .finally(() => setLoading(false));
  }, [isCoordinator, isProfessor]);

  useEffect(() => {
    if (!isCoordinator && !isProfessor) return;
    getAdvisorLiveGrades()
      .then((res) => setAdvisorLiveGrades(Array.isArray(res?.data) ? res.data : []))
      .catch(() => setAdvisorLiveGrades([]));
  }, [isCoordinator, isProfessor]);

  const onInviteAction = async (inviteId, action, scope, committeeId = null) => {
    setInviteProcessing((prev) => ({ ...prev, [inviteId]: action }));
    try {
      const res = await respondGroupInvite(inviteId, action, committeeId);
      const payload = res?.data || {};
      if (action === 'ACCEPT' && payload.selectionRequired) {
        setCommitteeSelection({
          inviteId,
          options: Array.isArray(payload.committeeOptions) ? payload.committeeOptions : [],
          selectedCommitteeId: null,
          loading: false,
          error: '',
        });
        return Promise.resolve(payload);
      }
      const removeInvite = () => {
        if (scope === 'staff') {
          setStaffInvites((prev) => prev.filter((invite) => invite.inviteId !== inviteId));
        } else {
          setData((prev) => ({
            ...prev,
            invitations: prev.invitations.filter((invite) => invite.inviteId !== inviteId),
          }));
        }
      };

      if (action === 'ACCEPT') {
        setTimeout(async () => {
          removeInvite();
          if (scope === 'staff') {
            const teamsRes = await getMyTeams();
            setStaffData((prev) => ({ ...prev, teams: teamsRes?.data || [] }));
          }
        }, 1500);
      } else {
        removeInvite();
      }
      return Promise.resolve();
    } catch (error) {
      return Promise.reject(error);
    } finally {
      setInviteProcessing((prev) => {
        const updated = { ...prev };
        delete updated[inviteId];
        return updated;
      });
    }
  };

  const submitCommitteeSelection = async (scope) => {
    if (!committeeSelection.inviteId || !committeeSelection.selectedCommitteeId) return;
    setCommitteeSelection((prev) => ({ ...prev, loading: true, error: '' }));
    try {
      await onInviteAction(
        committeeSelection.inviteId,
        'ACCEPT',
        scope,
        Number(committeeSelection.selectedCommitteeId),
      );
      setCommitteeSelection({
        inviteId: null,
        options: [],
        selectedCommitteeId: null,
        loading: false,
        error: '',
      });
    } catch (err) {
      setCommitteeSelection((prev) => ({
        ...prev,
        loading: false,
        error: err?.message || 'Failed to assign committee.',
      }));
    }
  };
  return (
    <div className="dashboard">
      <h1>Welcome, {user?.fullName || 'User'}</h1>
      <p className="dashboard-subtitle">You are logged in as <strong>{user?.role}</strong></p>

      {!isStudent && <div className="dashboard-cards">
        <div className="dashboard-card">
          <div className="card-label">Email</div>
          <div className="card-value">{user?.email}</div>
        </div>
        <div className="dashboard-card">
          <div className="card-label">Role</div>
          <div className="card-value role-badge">{user?.role}</div>
        </div>
      </div>}

      {isStudent && (
        <div className="student-dashboard-grid">
          <section className="dashboard-panel">
            <h3>My Projects</h3>
            {loading && <p>Loading...</p>}
            {!loading && data.activeProjects.length === 0 && <p>No active projects yet.</p>}
            {data.activeProjects.map((project) => (
              <div
                className="dashboard-row-card"
                key={`${project.projectId}-${project.groupId}`}
                onClick={() => navigate(`/panel/student-projects/${project.projectId}`)}
                style={{ cursor: 'pointer' }}
              >
                <strong>{project.projectTitle}</strong>
                <span>Group #{project.groupId}</span>
                <span>{project.term}</span>
              </div>
            ))}
          </section>

          <section className="dashboard-panel">
            <h3>Pending Deliverables</h3>
            {loading && <p>Loading...</p>}
            {!loading && data.pendingDeliverables.length === 0 && <p>No pending deliverables.</p>}
            {data.pendingDeliverables.map((item, idx) => (
              <div className="dashboard-row-card" key={`${item.projectId}-${item.deliverableTitle}-${idx}`}>
                <strong>{item.deliverableTitle}</strong>
                <span>{item.projectTitle} - {item.sprintTitle}</span>
                <span>Due: {item.dueDate || 'TBD'}</span>
              </div>
            ))}
          </section>

          <section className="dashboard-panel invitations-panel">
            <div className="panel-header">
              <h3>Invitations</h3>
              {data.invitations.length > 0 && (
                <span className="badge-count">{data.invitations.length}</span>
              )}
            </div>
            {loading && (
              <div className="invitations-loading">
                <div className="loading-spinner" />
                <p>Loading invitations...</p>
              </div>
            )}
            {!loading && data.invitations.length === 0 && (
              <div className="invitations-empty">
                <div className="empty-icon">&#128236;</div>
                <p>No pending invitations</p>
                <span>When someone invites you to a team, it will appear here</span>
              </div>
            )}
            {!loading && data.invitations.length > 0 && (
              <div className="invitations-list">
                {data.invitations.map((invite) => (
                  <InviteCard
                    key={invite.inviteId}
                    invite={invite}
                    onRespond={(id, action) => onInviteAction(id, action, 'student')}
                  />
                ))}
              </div>
            )}
          </section>
          <section className="dashboard-panel">
            <h3>Live Grade Summary</h3>
            {advisorLiveGrades.length === 0 && <p>No live grade data yet.</p>}
            {advisorLiveGrades.map((row) => (
              <div className="dashboard-row-card" key={`${row.projectId}-${row.groupId}`}>
                <strong>{row.projectTitle}</strong>
                <span>Group #{row.groupId}</span>
                <span>Team: {row.cumulativeTeamGrade ?? '-'} | Individual: {row.adjustedIndividualGrade ?? '-'}</span>
              </div>
            ))}
          </section>
        </div>
      )}

      {(isCoordinator || isProfessor) && (
        <div className="student-dashboard-grid">
          <section className="dashboard-panel">
            <h3>My Projects</h3>
            {loading && <p>Loading...</p>}
            {!loading && staffData.templates.length === 0 && <p>No projects found.</p>}
            {staffData.templates.map((template) => (
              <div
                className="dashboard-row-card"
                key={template.templateId}
                onClick={() => navigate(`/panel/templates/${template.templateId}`)}
                style={{ cursor: 'pointer' }}
              >
                <strong>{template.name}</strong>
                <span>Template #{template.templateId}</span>
                <span>{template.term}</span>
              </div>
            ))}
          </section>

          <section className="dashboard-panel">
            <h3>My Teams</h3>
            {loading && <p>Loading...</p>}
            {!loading && staffData.teams.length === 0 && <p>No team relation found.</p>}
            {staffData.teams.map((team) => (
              <div className="dashboard-row-card" key={team.groupId}>
                <strong>{team.groupName}</strong>
                <span>Group #{team.groupId}</span>
                <span>{team.project ? `Project: ${team.project.title}` : 'No linked project yet'}</span>
              </div>
            ))}
          </section>

          <section className="dashboard-panel invitations-panel">
            <div className="panel-header">
              <h3>Invitations</h3>
              {staffInvites.length > 0 && (
                <span className="badge-count">{staffInvites.length}</span>
              )}
            </div>
            {loading && (
              <div className="invitations-loading">
                <div className="loading-spinner" />
                <p>Loading invitations...</p>
              </div>
            )}
            {!loading && staffInvites.length === 0 && (
              <div className="invitations-empty">
                <div className="empty-icon">&#128236;</div>
                <p>No pending invitations</p>
                <span>When someone invites you to a team, it will appear here</span>
              </div>
            )}
            {!loading && staffInvites.length > 0 && (
              <div className="invitations-list">
                {staffInvites.map((invite) => (
                  <InviteCard
                    key={invite.inviteId}
                    invite={invite}
                    onRespond={(id, action) => onInviteAction(id, action, 'staff')}
                  />
                ))}
              </div>
            )}
          </section>
        </div>
      )}
      {committeeSelection.inviteId && (
        <div className="modal-backdrop">
          <div className="modal-card" style={{ maxWidth: 560 }}>
            <div className="modal-header">
              <div className="modal-title-section">
                <h3>Select Committee</h3>
                <p className="modal-sub">You are in multiple committees for this project. Choose one.</p>
              </div>
              <button
                className="close-icon-btn"
                onClick={() =>
                  setCommitteeSelection({
                    inviteId: null,
                    options: [],
                    selectedCommitteeId: null,
                    loading: false,
                    error: '',
                  })
                }
              >
                <span aria-hidden="true">&times;</span>
              </button>
            </div>
            <div className="modal-list">
              {committeeSelection.options.map((opt) => (
                <label
                  key={opt.committeeId}
                  className="modal-list-item"
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}
                >
                  <div>
                    <strong>{opt.committeeName}</strong>
                    <div style={{ fontSize: 12, opacity: 0.8 }}>
                      Assigned groups: {opt.assignedGroupCount}
                    </div>
                  </div>
                  <input
                    type="radio"
                    name="committee-selection"
                    value={opt.committeeId}
                    checked={String(committeeSelection.selectedCommitteeId) === String(opt.committeeId)}
                    onChange={() =>
                      setCommitteeSelection((prev) => ({
                        ...prev,
                        selectedCommitteeId: opt.committeeId,
                      }))
                    }
                  />
                </label>
              ))}
            </div>
            {committeeSelection.error && (
              <div className="invite-status-banner invite-status-error">
                <span className="status-icon">&#9888;</span>
                <span>{committeeSelection.error}</span>
              </div>
            )}
            <div className="delete-actions">
              <button
                className="cancel-btn"
                onClick={() =>
                  setCommitteeSelection({
                    inviteId: null,
                    options: [],
                    selectedCommitteeId: null,
                    loading: false,
                    error: '',
                  })
                }
                disabled={committeeSelection.loading}
              >
                Cancel
              </button>
              <button
                className="primary-btn"
                onClick={() => submitCommitteeSelection('staff')}
                disabled={committeeSelection.loading || !committeeSelection.selectedCommitteeId}
              >
                {committeeSelection.loading ? 'Assigning...' : 'Assign & Accept'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Dashboard;
