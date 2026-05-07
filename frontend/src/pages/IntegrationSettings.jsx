import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { setupIntegrations, getGroupIntegrations } from '../services/api';
import './IntegrationSettings.css';

function IntegrationSettings() {
  const { user, loading: authLoading } = useAuth();
  const [githubPat, setGithubPat] = useState('');
  const [jiraSpaceUrl, setJiraSpaceUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [pageLoading, setPageLoading] = useState(true);

  // Get group ID from authenticated user context
  const groupId = user?.groupId;
  const isTeamLeader = user?.role === 'TEAM_LEADER';

  useEffect(() => {
    if (authLoading) {
      setPageLoading(true);
      return;
    }

    if (isTeamLeader && groupId) {
      loadIntegrations();
    } else {
      setPageLoading(false);
    }
  }, [isTeamLeader, groupId, authLoading]);

  async function loadIntegrations() {
    try {
      const data = await getGroupIntegrations(groupId);
      setGithubPat(data.githubPat || '');
      setJiraSpaceUrl(data.jiraSpaceUrl || '');
      setPageLoading(false);
    } catch (err) {
      console.error('Failed to load integrations:', err);
      setPageLoading(false);
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();

    if (!groupId) {
      setError('Group information not available. Please refresh the page.');
      return;
    }

    if (!githubPat.trim() || !jiraSpaceUrl.trim()) {
      setError('Both GitHub Token and JIRA Workspace URL are required');
      return;
    }

    // Validate GitHub token format
    if (!githubPat.trim().startsWith('ghp_') && !githubPat.trim().startsWith('github_pat_')) {
      setError('Invalid GitHub Personal Access Token format (should start with ghp_ or github_pat_)');
      return;
    }

    // Validate JIRA URL format
    if (!jiraSpaceUrl.trim().startsWith('http://') && !jiraSpaceUrl.trim().startsWith('https://')) {
      setError('JIRA Workspace URL must start with http:// or https://');
      return;
    }

    setLoading(true);
    setError('');
    setSuccess('');

    try {
      await setupIntegrations(groupId, githubPat.trim(), jiraSpaceUrl.trim());
      setSuccess('Integration settings saved successfully! GitHub and JIRA credentials have been configured.');
      setTimeout(() => setSuccess(''), 5000);
    } catch (err) {
      setError(err.message || 'Failed to save integration settings');
    } finally {
      setLoading(false);
    }
  }

  // Show loading while authenticating
  if (authLoading) {
    return <div className="integration-settings-loading">Loading authentication...</div>;
  }

  if (!isTeamLeader) {
    return (
      <div className="integration-settings-page">
        <div className="alert alert-error">
          You don't have permission to access this page. Only Team Leaders can manage integrations.
        </div>
      </div>
    );
  }

  if (!groupId) {
    return (
      <div className="integration-settings-page">
        <div className="alert alert-error">
          Unable to load group information. Please ensure you are assigned to a group. Contact support if the issue persists.
        </div>
      </div>
    );
  }

  if (pageLoading) {
    return <div className="integration-settings-loading">Loading integration settings...</div>;
  }

  return (
    <div className="integration-settings-page">
      <div className="integration-settings-container">
        <h1>Integration Settings</h1>
        <p className="settings-subtitle">Configure GitHub and JIRA credentials for your team</p>

        {error && <div className="alert alert-error">{error}</div>}
        {success && <div className="alert alert-success">{success}</div>}

        <form onSubmit={handleSubmit} className="integration-form">
          {/* GitHub Token Section */}
          <div className="form-section">
            <h2>GitHub Configuration</h2>
            <div className="form-group">
              <label htmlFor="githubPat">GitHub Personal Access Token</label>
              <div className="input-wrapper">
                <input
                  type="password"
                  id="githubPat"
                  value={githubPat}
                  onChange={(e) => setGithubPat(e.target.value)}
                  placeholder="ghp_xxxxxxxxxxxxxxxxxxxx"
                  disabled={loading}
                  required
                />
                <div className="helper-text">
                  Create a <a href="https://github.com/settings/tokens" target="_blank" rel="noopener noreferrer">
                    Personal Access Token
                  </a> with repo and read:org scopes
                </div>
              </div>
            </div>
          </div>

          {/* JIRA Section */}
          <div className="form-section">
            <h2>JIRA Configuration</h2>
            <div className="form-group">
              <label htmlFor="jiraSpaceUrl">JIRA Workspace URL</label>
              <div className="input-wrapper">
                <input
                  type="url"
                  id="jiraSpaceUrl"
                  value={jiraSpaceUrl}
                  onChange={(e) => setJiraSpaceUrl(e.target.value)}
                  placeholder="https://yourcompany.atlassian.net/wiki/spaces/TEAM"
                  disabled={loading}
                  required
                />
                <div className="helper-text">
                  Example: https://myuni.atlassian.net/wiki/spaces/PROJECTKEY
                </div>
              </div>
            </div>
          </div>

          <div className="form-actions">
            <button
              type="submit"
              className="btn-save"
              disabled={loading}
            >
              {loading ? 'Saving...' : 'Save Settings'}
            </button>
          </div>
        </form>

        <div className="info-section">
          <h3>About These Settings</h3>
          <p>
            These credentials enable:
          </p>
          <ul>
            <li>Daily AI Sync between your team's GitHub and JIRA</li>
            <li>Automatic repository monitoring and updates</li>
            <li>Project management integration</li>
          </ul>
          <p className="security-note">
            ⚠️ Your credentials are securely stored and encrypted. Never share your Personal Access Token.
          </p>
        </div>
      </div>
    </div>
  );
}

export default IntegrationSettings;
