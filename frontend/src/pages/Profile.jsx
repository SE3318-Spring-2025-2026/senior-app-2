import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  getProfileGithubPatStatus,
  getProfileJiraAccount,
  getProfileJiraConnectionStatus,
  getJiraOAuthLoginUrl,
  getStaffGithubLinkUrl,
  saveProfileGithubPat,
  updateMyProfile,
} from '../services/api';
import { useAuth } from '../context/AuthContext';

function Profile() {
  const { user, refreshUser, setUser } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    fullName: user?.fullName || '',
    email: user?.email || '',
  });
  const [saving, setSaving] = useState(false);
  const [status, setStatus] = useState('');
  const [pat, setPat] = useState('');
  const [patConfigured, setPatConfigured] = useState(false);
  const [patSaving, setPatSaving] = useState(false);
  const [jiraConnectionConfigured, setJiraConnectionConfigured] = useState(false);
  const [jiraAccount, setJiraAccount] = useState({ jiraAccountId: '', jiraEmail: '', jiraDisplayName: '' });

  useEffect(() => {
    setForm({
      fullName: user?.fullName || '',
      email: user?.email || '',
    });
  }, [user]);

  useEffect(() => {
    getProfileGithubPatStatus()
      .then((res) => setPatConfigured(!!res?.configured))
      .catch(() => setPatConfigured(false));
  }, []);

  useEffect(() => {
    getProfileJiraConnectionStatus()
      .then((res) => {
        setJiraConnectionConfigured(!!res?.configured);
      })
      .catch(() => setJiraConnectionConfigured(false));
    getProfileJiraAccount()
      .then((res) => {
        setJiraAccount({
          jiraAccountId: res?.jiraAccountId || '',
          jiraEmail: res?.jiraEmail || '',
          jiraDisplayName: res?.jiraDisplayName || '',
        });
      })
      .catch(() => {});
  }, []);

  const saveProfile = async () => {
    setSaving(true);
    setStatus('');
    try {
      const updated = await updateMyProfile(form);
      setUser((prev) => (prev ? { ...prev, ...updated } : updated));
      await refreshUser();
      setStatus('Profile saved.');
    } catch (e) {
      setStatus(e.message || 'Failed to save profile.');
    } finally {
      setSaving(false);
    }
  };

  const connectGithub = async () => {
    try {
      const authUrl = await getStaffGithubLinkUrl();
      window.location.href = authUrl;
    } catch (e) {
      setStatus(e.message || 'GitHub link could not be started.');
    }
  };

  return (
    <div className="dashboard">
      <h1>My Profile</h1>
      <p className="dashboard-subtitle">Update your profile and connect GitHub.</p>

      <div className="dashboard-panel" style={{ maxWidth: 720 }}>
        <h3>Profile Info</h3>
        <div className="template-form">
          <label>
            Full Name
            <input
              value={form.fullName}
              onChange={(e) => setForm((p) => ({ ...p, fullName: e.target.value }))}
            />
          </label>
          <label>
            Email
            <input
              value={form.email}
              onChange={(e) => setForm((p) => ({ ...p, email: e.target.value }))}
            />
          </label>
          <button className="template-btn" onClick={saveProfile} disabled={saving}>
            {saving ? 'Saving...' : 'Save Profile'}
          </button>
        </div>
      </div>

      <div className="dashboard-panel" style={{ maxWidth: 720, marginTop: 16 }}>
        <h3>GitHub Link</h3>
        <p>Current GitHub: {user?.githubUsername ? `@${user.githubUsername}` : 'Not linked yet'}</p>
        <button className="template-btn" onClick={connectGithub}>
          {user?.githubUsername ? 'Reconnect GitHub' : 'Connect GitHub'}
        </button>
      </div>

      {(user?.role === 'PROFESSOR' || user?.role === 'COORDINATOR' || user?.role === 'ADMIN') && (
        <div className="dashboard-panel" style={{ maxWidth: 720, marginTop: 16 }}>
          <h3>GitHub PAT (for repo operations)</h3>
          <p>{patConfigured ? 'PAT configured.' : 'PAT not configured.'}</p>
          <input
            type="password"
            value={pat}
            onChange={(e) => setPat(e.target.value)}
            placeholder="ghp_... or github_pat_..."
            style={{ width: '100%', marginBottom: 8 }}
          />
          <button
            className="template-btn"
            onClick={async () => {
              setPatSaving(true);
              try {
                await saveProfileGithubPat(pat);
                setPat('');
                setPatConfigured(true);
                setStatus('GitHub PAT saved.');
              } catch (e) {
                setStatus(e.message || 'Failed to save PAT.');
              } finally {
                setPatSaving(false);
              }
            }}
            disabled={patSaving || !pat.trim()}
          >
            {patSaving ? 'Saving...' : 'Save PAT'}
          </button>
        </div>
      )}

      <div className="dashboard-panel" style={{ maxWidth: 720, marginTop: 16 }}>
        <h3>Jira Connection (OAuth)</h3>
        <p>{jiraConnectionConfigured ? 'Jira connection configured.' : 'Jira connection not configured.'}</p>
        {user?.role === 'STUDENT' && (
          <p style={{ marginTop: 8 }}>
            Student assignment for Jira uses your connected account.
            {jiraAccount?.jiraAccountId ? ` Current accountId: ${jiraAccount.jiraAccountId}` : ' Connect Jira to auto-fill accountId.'}
          </p>
        )}
        <button
          className="template-btn"
          onClick={async () => {
            try {
              const authUrl = await getJiraOAuthLoginUrl();
              window.location.href = authUrl;
            } catch (e) {
              setStatus(e.message || 'Failed to start Jira OAuth.');
            }
          }}
        >
          {jiraConnectionConfigured ? 'Reconnect Jira' : 'Connect Jira'}
        </button>
      </div>

      {status && <div className="dashboard-panel" style={{ maxWidth: 720, marginTop: 16 }}>{status}</div>}
      <button className="template-btn" style={{ marginTop: 16 }} onClick={() => navigate('/panel')}>Back</button>
    </div>
  );
}

export default Profile;
