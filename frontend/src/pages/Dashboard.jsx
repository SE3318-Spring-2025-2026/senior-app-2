import { useAuth } from '../context/AuthContext';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyTeams, getProjectTemplates, getStudentDashboard, respondGroupInvite } from '../services/api';
import './Dashboard.css';

function Dashboard() {
  const { user } = useAuth();
  const [data, setData] = useState({ activeProjects: [], pendingDeliverables: [], invitations: [] });
  const [staffData, setStaffData] = useState({ templates: [], teams: [] });
  const [loading, setLoading] = useState(false);
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
    await respondGroupInvite(inviteId, action);
    setData((prev) => ({
      ...prev,
      invitations: prev.invitations.filter((invite) => invite.inviteId !== inviteId),
    }));
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

          <section className="dashboard-panel">
            <h3>Invitations</h3>
            {loading && <p>Loading...</p>}
            {!loading && data.invitations.length === 0 && <p>No invitations.</p>}
            {data.invitations.map((invite) => (
              <div className="dashboard-row-card" key={invite.inviteId}>
                <strong>{invite.groupName}</strong>
                <span>Group #{invite.groupId}</span>
                <div className="invite-actions">
                  <button onClick={() => onInviteAction(invite.inviteId, 'ACCEPT')}>Accept</button>
                  <button className="danger" onClick={() => onInviteAction(invite.inviteId, 'DECLINE')}>Decline</button>
                </div>
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
