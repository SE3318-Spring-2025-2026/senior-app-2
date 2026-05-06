import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import {
  createProjectFromTemplateForTeam,
  createTeam,
  deleteGroup,
  getAllTeams,
  getMyTeams,
  getProjectTemplates,
  inviteStudentToTeam,
  listAdvisorOptionsForTeam,
  listStudentsForInvite,
} from '../services/api';
import './TeamManagement.css';

function TeamManagement() {
  const { user } = useAuth();
  const [teams, setTeams] = useState([]);
  const [students, setStudents] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [groupName, setGroupName] = useState('');
  const [error, setError] = useState('');
  const [selectedStudentByTeam, setSelectedStudentByTeam] = useState({});
  const [selectedTemplateByTeam, setSelectedTemplateByTeam] = useState({});
  const [inviteModalGroup, setInviteModalGroup] = useState(null);
  const [studentSearch, setStudentSearch] = useState('');
  const [studentInviteStatus, setStudentInviteStatus] = useState({ loading: false, success: null, error: null });
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [advisorModalGroup, setAdvisorModalGroup] = useState(null);
  const [advisorOptions, setAdvisorOptions] = useState([]);
  const [advisorSearch, setAdvisorSearch] = useState('');
  const [inviteStatus, setInviteStatus] = useState({ loading: false, success: null, error: null });
  const [selectedAdvisor, setSelectedAdvisor] = useState(null);
  const [deleteModalGroup, setDeleteModalGroup] = useState(null);
  const [deleteStatus, setDeleteStatus] = useState({ loading: false, error: null });

  const load = async () => {
    try {
      const teamsReq = user?.role === 'PROFESSOR' ? getAllTeams() : getMyTeams();
      const [teamsRes, studentsRes, templatesRes] = await Promise.all([
        teamsReq,
        listStudentsForInvite(),
        getProjectTemplates(),
      ]);
      setTeams(teamsRes?.data || []);
      setStudents(studentsRes?.data || []);
      setTemplates(templatesRes?.data || []);
    } catch (e) {
      setError(e.message || 'Failed to load team data.');
    }
  };

  useEffect(() => {
    load();
  }, []);

  const studentOptions = useMemo(
    () => students.map((s) => ({ value: String(s.userId), label: `${s.fullName} (${s.email})` })),
    [students]
  );
  const filteredStudentOptions = useMemo(() => {
    const q = studentSearch.trim().toLowerCase();
    if (!q) return studentOptions;
    return studentOptions.filter((opt) => opt.label.toLowerCase().includes(q));
  }, [studentOptions, studentSearch]);
  const filteredAdvisorOptions = useMemo(() => {
    const q = advisorSearch.trim().toLowerCase();
    if (!q) return advisorOptions;
    return advisorOptions.filter((opt) => `${opt.fullName} ${opt.email}`.toLowerCase().includes(q));
  }, [advisorOptions, advisorSearch]);

  const onCreateTeam = async (e) => {
    e.preventDefault();
    setError('');
    if (!groupName.trim()) return;
    try {
      await createTeam(groupName.trim());
      setGroupName('');
      await load();
    } catch (err) {
      setError(err.message || 'Failed to create team.');
    }
  };

  const onInvite = async (groupId) => {
    const studentUserId = selectedStudentByTeam[groupId];
    if (!studentUserId) return;
    try {
      await inviteStudentToTeam(groupId, Number(studentUserId));
      await load();
    } catch (err) {
      setError(err.message || 'Failed to send invite.');
    }
  };

  const handleStudentInvite = async (student) => {
    setSelectedStudent(student.value);
    setStudentInviteStatus({ loading: true, success: null, error: null });
    try {
      setSelectedStudentByTeam((prev) => ({ ...prev, [inviteModalGroup.groupId]: student.value }));
      await inviteStudentToTeam(inviteModalGroup.groupId, Number(student.value));
      setStudentInviteStatus({ loading: false, success: `Successfully invited ${student.label.split('(')[0].trim()}!`, error: null });
      setTimeout(async () => {
        setInviteModalGroup(null);
        await load();
      }, 1500);
    } catch (err) {
      setStudentInviteStatus({ loading: false, success: null, error: err.message || 'Failed to send invitation. Please try again.' });
      setSelectedStudent(null);
    }
  };

  const onCreateProject = async (groupId) => {
    const templateId = selectedTemplateByTeam[groupId];
    if (!templateId) return;
    try {
      await createProjectFromTemplateForTeam(groupId, Number(templateId));
      await load();
    } catch (err) {
      setError(err.message || 'Failed to create project.');
    }
  };
  const openAdvisorModal = async (team) => {
    setAdvisorModalGroup(team);
    setAdvisorSearch('');
    setInviteStatus({ loading: false, success: null, error: null });
    setSelectedAdvisor(null);
    try {
      const res = await listAdvisorOptionsForTeam(team.groupId);
      setAdvisorOptions(res?.data || []);
    } catch (err) {
      setError(err.message || 'Failed to load advisors.');
      setAdvisorOptions([]);
    }
  };

  const handleAdvisorInvite = async (advisor) => {
    setSelectedAdvisor(advisor.userId);
    setInviteStatus({ loading: true, success: null, error: null });
    try {
      await inviteStudentToTeam(advisorModalGroup.groupId, advisor.userId);
      setInviteStatus({ loading: false, success: `Successfully invited ${advisor.fullName}!`, error: null });
      setTimeout(async () => {
        setAdvisorModalGroup(null);
        await load();
      }, 1500);
    } catch (err) {
      setInviteStatus({ loading: false, success: null, error: err.message || 'Failed to send invitation. Please try again.' });
      setSelectedAdvisor(null);
    }
  };

  const handleDeleteGroup = async () => {
    if (!deleteModalGroup) return;
    setDeleteStatus({ loading: true, error: null });
    try {
      await deleteGroup(deleteModalGroup.groupId);
      setDeleteModalGroup(null);
      setDeleteStatus({ loading: false, error: null });
      await load();
    } catch (err) {
      setDeleteStatus({ loading: false, error: err.message || 'Failed to delete group. Please try again.' });
    }
  };

  return (
    <div className="team-management-page">
      <h1 className="team-title">Team Management</h1>
      {error && <div className="team-error">{error}</div>}

      {(user?.role === 'STUDENT' || user?.role === 'COORDINATOR') && (
        <form className="team-create-form" onSubmit={onCreateTeam}>
          <input
            value={groupName}
            onChange={(e) => setGroupName(e.target.value)}
            placeholder="Enter new team name"
          />
          <button type="submit" className="primary-btn">Create Team</button>
        </form>
      )}

      <div className="team-grid">
        {teams.map((team) => (
          <div key={team.groupId} className="team-card">
            <div className="team-card-header">
              <h3>{team.groupName}</h3>
              <span className="group-chip">Group #{team.groupId}</span>
            </div>
            <p className="team-sub">Group #{team.groupId}</p>

            {team.project ? (
              <div className="team-project-linked">
                Linked Project: <strong>{team.project.title}</strong>
              </div>
            ) : (
              <div className="team-row">
                <select
                  value={selectedTemplateByTeam[team.groupId] || ''}
                  onChange={(e) =>
                    setSelectedTemplateByTeam((prev) => ({ ...prev, [team.groupId]: e.target.value }))
                  }
                  className="soft-input"
                >
                  <option value="">Select template</option>
                  {templates.map((t) => (
                    <option key={t.templateId} value={t.templateId}>
                      {t.name}
                    </option>
                  ))}
                </select>
                <button className="primary-btn" onClick={() => onCreateProject(team.groupId)} disabled={!team.currentUserLeader}>
                  Select Project
                </button>
              </div>
            )}

            <h4>Members</h4>
            <div className="team-members">
              {team.members.map((member) => (
                <div key={`${team.groupId}-${member.userId}-${member.inviteStatus}`} className="team-member-item">
                  <span>{member.fullName}</span>
                  <span className={`badge ${member.inviteStatus.toLowerCase()}`}>{member.inviteStatus}</span>
                </div>
              ))}
            </div>

            <div className="team-row">
              <button
                className="primary-btn"
                onClick={() => {
                  setInviteModalGroup(team);
                  setStudentSearch('');
                  setStudentInviteStatus({ loading: false, success: null, error: null });
                  setSelectedStudent(null);
                }}
                disabled={!team.currentUserLeader && user?.role !== 'PROFESSOR'}
              >
                Invite Student
              </button>
              <button
                className="ghost-btn"
                onClick={() => openAdvisorModal(team)}
              >
                Invite Advisor
              </button>
              {team.currentUserLeader && (
                <button
                  className="delete-btn"
                  onClick={() => setDeleteModalGroup(team)}
                >
                  Delete
                </button>
              )}
            </div>
          </div>
        ))}
      </div>

      {inviteModalGroup && (
        <div className="modal-backdrop" onClick={() => setInviteModalGroup(null)}>
          <div className="modal-card student-invite-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title-section">
                <h3>Invite Student</h3>
                <p className="modal-sub">Send an invitation to join your team</p>
              </div>
              <button className="close-icon-btn" onClick={() => setInviteModalGroup(null)}>
                <span aria-hidden="true">&times;</span>
              </button>
            </div>
            
            <div className="modal-team-info">
              <div className="team-info-badge">
                <span className="team-info-label">Team</span>
                <span className="team-info-value">{inviteModalGroup.groupName}</span>
              </div>
            </div>

            {studentInviteStatus.success && (
              <div className="invite-status-banner invite-status-success">
                <span className="status-icon">&#10003;</span>
                <span>{studentInviteStatus.success}</span>
              </div>
            )}

            {studentInviteStatus.error && (
              <div className="invite-status-banner invite-status-error">
                <span className="status-icon">&#9888;</span>
                <span>{studentInviteStatus.error}</span>
              </div>
            )}

            <div className="search-section">
              <div className="search-input-wrapper">
                <span className="search-icon">&#128269;</span>
                <input
                  className="search-input"
                  placeholder="Search by name or email..."
                  value={studentSearch}
                  onChange={(e) => setStudentSearch(e.target.value)}
                />
              </div>
            </div>

            <div className="modal-list student-list">
              {filteredStudentOptions.length === 0 ? (
                <div className="empty-state">
                  <div className="empty-state-icon">&#128269;</div>
                  <p>No students found matching your search.</p>
                  <p className="empty-state-hint">Try adjusting your search criteria.</p>
                </div>
              ) : (
                filteredStudentOptions.map((opt) => (
                  <div key={opt.value} className="modal-list-item student-item">
                    <div className="student-info">
                      <div className="student-avatar">
                        {opt.label?.charAt(0)?.toUpperCase() || 'S'}
                      </div>
                      <div className="student-details">
                        <span className="student-name">{opt.label.split('(')[0].trim()}</span>
                        <span className="student-email">{opt.label.match(/\(([^)]+)\)/)?.[1] || ''}</span>
                      </div>
                    </div>
                    <button
                      className={`invite-student-btn ${selectedStudent === opt.value ? 'inviting' : ''}`}
                      onClick={() => handleStudentInvite(opt)}
                      disabled={studentInviteStatus.loading}
                    >
                      {studentInviteStatus.loading && selectedStudent === opt.value ? (
                        <span className="btn-spinner"></span>
                      ) : (
                        'Send Invite'
                      )}
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}

      {deleteModalGroup && (
        <div className="modal-backdrop" onClick={() => setDeleteModalGroup(null)}>
          <div className="modal-card delete-confirm-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title-section">
                <h3>Delete Group</h3>
                <p className="modal-sub">Are you sure you want to delete this group?</p>
              </div>
              <button className="close-icon-btn" onClick={() => setDeleteModalGroup(null)}>
                <span aria-hidden="true">&times;</span>
              </button>
            </div>

            <div className="delete-warning">
              <div className="warning-icon">&#9888;</div>
              <div className="warning-content">
                <p className="warning-title">This action cannot be undone</p>
                <p className="warning-text">
                  You are about to delete <strong>{deleteModalGroup.groupName}</strong>. All team members will be removed and this team will no longer be available.
                </p>
              </div>
            </div>

            {deleteStatus.error && (
              <div className="invite-status-banner invite-status-error">
                <span className="status-icon">&#9888;</span>
                <span>{deleteStatus.error}</span>
              </div>
            )}

            <div className="delete-actions">
              <button
                className="cancel-btn"
                onClick={() => setDeleteModalGroup(null)}
                disabled={deleteStatus.loading}
              >
                Cancel
              </button>
              <button
                className="confirm-delete-btn"
                onClick={handleDeleteGroup}
                disabled={deleteStatus.loading}
              >
                {deleteStatus.loading ? (
                  <span className="btn-spinner"></span>
                ) : (
                  'Delete Group'
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {advisorModalGroup && (
        <div className="modal-backdrop" onClick={() => setAdvisorModalGroup(null)}>
          <div className="modal-card advisor-invite-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title-section">
                <h3>Invite Professor</h3>
                <p className="modal-sub">Send an invitation to join your team as an advisor</p>
              </div>
              <button className="close-icon-btn" onClick={() => setAdvisorModalGroup(null)}>
                <span aria-hidden="true">&times;</span>
              </button>
            </div>
            
            <div className="modal-team-info">
              <div className="team-info-badge">
                <span className="team-info-label">Team</span>
                <span className="team-info-value">{advisorModalGroup.groupName}</span>
              </div>
            </div>

            {inviteStatus.success && (
              <div className="invite-status-banner invite-status-success">
                <span className="status-icon">&#10003;</span>
                <span>{inviteStatus.success}</span>
              </div>
            )}

            {inviteStatus.error && (
              <div className="invite-status-banner invite-status-error">
                <span className="status-icon">&#9888;</span>
                <span>{inviteStatus.error}</span>
              </div>
            )}

            <div className="search-section">
              <div className="search-input-wrapper">
                <span className="search-icon">&#128269;</span>
                <input
                  className="search-input"
                  placeholder="Search by name or email..."
                  value={advisorSearch}
                  onChange={(e) => setAdvisorSearch(e.target.value)}
                />
              </div>
            </div>

            <div className="modal-list advisor-list">
              {filteredAdvisorOptions.length === 0 ? (
                <div className="empty-state">
                  <div className="empty-state-icon">&#128269;</div>
                  <p>No advisors found matching your search.</p>
                  <p className="empty-state-hint">Try adjusting your search criteria.</p>
                </div>
              ) : (
                filteredAdvisorOptions.map((opt) => (
                  <div key={opt.userId} className="modal-list-item advisor-item">
                    <div className="advisor-info">
                      <div className="advisor-avatar">
                        {opt.fullName?.charAt(0)?.toUpperCase() || 'P'}
                      </div>
                      <div className="advisor-details">
                        <span className="advisor-name">{opt.fullName}</span>
                        <span className="advisor-email">{opt.email}</span>
                      </div>
                    </div>
                    <button
                      className={`invite-advisor-btn ${selectedAdvisor === opt.userId ? 'inviting' : ''}`}
                      onClick={() => handleAdvisorInvite(opt)}
                      disabled={inviteStatus.loading}
                    >
                      {inviteStatus.loading && selectedAdvisor === opt.userId ? (
                        <span className="btn-spinner"></span>
                      ) : (
                        'Send Invite'
                      )}
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default TeamManagement;
