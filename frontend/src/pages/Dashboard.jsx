import { useAuth } from '../context/AuthContext';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyTeams, getProjectTemplates, getStudentDashboard, respondGroupInvite } from '../services/api';
import InviteCard from '../components/InviteCard';
import './Dashboard.css';

function Dashboard() {
  const { user } = useAuth();
  const [data, setData] = useState({ activeProjects: [], pendingDeliverables: [], invitations: [] });
  const [staffData, setStaffData] = useState({ templates: [], teams: [] });
  const [loading, setLoading] = useState(false);
  const [inviteProcessing, setInviteProcessing] = useState({});
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
    Promise.all([getProjectTemplates(), getMyTeams()])
      .then(([templatesRes, teamsRes]) => {
        setStaffData({
          templates: templatesRes?.data || [],
          teams: teamsRes?.data || [],
        });
      })
      .finally(() => setLoading(false));
  }, [isCoordinator, isProfessor]);

  const onInviteAction = async (inviteId, action) => {
    setInviteProcessing((prev) => ({ ...prev, [inviteId]: action }));
    try {
      await respondGroupInvite(inviteId, action);
      // If accepted, remove from list after a brief delay to show success state
      if (action === 'ACCEPT') {
        setTimeout(() => {
          setData((prev) => ({
            ...prev,
            invitations: prev.invitations.filter((invite) => invite.inviteId !== inviteId),
          }));
        }, 1500);
      } else {
        // If declined, remove immediately
        setData((prev) => ({
          ...prev,
          invitations: prev.invitations.filter((invite) => invite.inviteId !== inviteId),
        }));
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
                    onRespond={onInviteAction}
                  />
                ))}
              </div>
            )}
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

          <section className="dashboard-panel">
            <h3>Quick Stats</h3>
            <div className="dashboard-row-card">
              <strong>Total Projects</strong>
              <span>{staffData.templates.length}</span>
            </div>
            <div className="dashboard-row-card">
              <strong>Total Teams</strong>
              <span>{staffData.teams.length}</span>
            </div>
            <div className="dashboard-row-card">
              <strong>Linked Projects</strong>
              <span>{staffData.teams.filter((t) => t.project).length}</span>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}

export default Dashboard;
