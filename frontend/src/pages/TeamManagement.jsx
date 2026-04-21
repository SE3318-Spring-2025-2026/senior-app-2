import { useEffect, useMemo, useState } from 'react';
import {
  createProjectFromTemplateForTeam,
  createTeam,
  getMyTeams,
  getProjectTemplates,
  inviteStudentToTeam,
  listAdvisorOptionsForTeam,
  listStudentsForInvite,
} from '../services/api';
import './TeamManagement.css';

function TeamManagement() {
  const [teams, setTeams] = useState([]);
  const [students, setStudents] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [groupName, setGroupName] = useState('');
  const [error, setError] = useState('');
  const [selectedStudentByTeam, setSelectedStudentByTeam] = useState({});
  const [selectedTemplateByTeam, setSelectedTemplateByTeam] = useState({});
  const [inviteModalGroup, setInviteModalGroup] = useState(null);
  const [studentSearch, setStudentSearch] = useState('');
  const [advisorModalGroup, setAdvisorModalGroup] = useState(null);
  const [advisorOptions, setAdvisorOptions] = useState([]);
  const [advisorSearch, setAdvisorSearch] = useState('');

  const load = async () => {
    try {
      const [teamsRes, studentsRes, templatesRes] = await Promise.all([
        getMyTeams(),
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
    try {
      const res = await listAdvisorOptionsForTeam(team.groupId);
      setAdvisorOptions(res?.data || []);
    } catch (err) {
      setError(err.message || 'Failed to load advisors.');
      setAdvisorOptions([]);
    }
  };

  return (
    <div className="team-management-page">
      <h1 className="team-title">Team Management</h1>
      {error && <div className="team-error">{error}</div>}

      <form className="team-create-form" onSubmit={onCreateTeam}>
        <input
          value={groupName}
          onChange={(e) => setGroupName(e.target.value)}
          placeholder="Enter new team name"
        />
        <button type="submit" className="primary-btn">Create Team</button>
      </form>

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
                }}
                disabled={!team.currentUserLeader}
              >
                Invite Student
              </button>
              <button
                className="ghost-btn"
                onClick={() => openAdvisorModal(team)}
                disabled={!team.currentUserLeader || !team.project}
              >
                Invite Advisor
              </button>
            </div>
          </div>
        ))}
      </div>

      {inviteModalGroup && (
        <div className="modal-backdrop" onClick={() => setInviteModalGroup(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Invite Student</h3>
              <button className="ghost-btn" onClick={() => setInviteModalGroup(null)}>Close</button>
            </div>
            <p className="modal-sub">Team: <strong>{inviteModalGroup.groupName}</strong></p>
            <input
              className="soft-input"
              placeholder="Search by name or email..."
              value={studentSearch}
              onChange={(e) => setStudentSearch(e.target.value)}
            />
            <div className="modal-list">
              {filteredStudentOptions.map((opt) => (
                <div key={opt.value} className="modal-list-item">
                  <span>{opt.label}</span>
                  <button
                    className="primary-btn"
                    onClick={async () => {
                      setSelectedStudentByTeam((prev) => ({ ...prev, [inviteModalGroup.groupId]: opt.value }));
                      await onInvite(inviteModalGroup.groupId);
                      setInviteModalGroup(null);
                    }}
                  >
                    Invite
                  </button>
                </div>
              ))}
              {filteredStudentOptions.length === 0 && <div className="empty-state">No matching student found.</div>}
            </div>
          </div>
        </div>
      )}

      {advisorModalGroup && (
        <div className="modal-backdrop" onClick={() => setAdvisorModalGroup(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Invite Advisor</h3>
              <button className="ghost-btn" onClick={() => setAdvisorModalGroup(null)}>Close</button>
            </div>
            <p className="modal-sub">Team: <strong>{advisorModalGroup.groupName}</strong></p>
            <input
              className="soft-input"
              placeholder="Search advisor..."
              value={advisorSearch}
              onChange={(e) => setAdvisorSearch(e.target.value)}
            />
            <div className="modal-list">
              {filteredAdvisorOptions.map((opt) => (
                <div key={opt.userId} className="modal-list-item">
                  <span>{opt.fullName} ({opt.email})</span>
                  <button
                    className="primary-btn"
                    onClick={async () => {
                      await inviteStudentToTeam(advisorModalGroup.groupId, opt.userId);
                      setAdvisorModalGroup(null);
                      await load();
                    }}
                  >
                    Invite
                  </button>
                </div>
              ))}
              {filteredAdvisorOptions.length === 0 && <div className="empty-state">No advisor found in committee scope.</div>}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default TeamManagement;
