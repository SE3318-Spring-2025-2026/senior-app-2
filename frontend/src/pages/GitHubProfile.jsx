import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import './GitHubProfile.css';

const GitHubProfile = () => {
  const { userId } = useParams();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('profile');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [profileData, setProfileData] = useState(null);
  const [repositories, setRepositories] = useState(null);
  const [activity, setActivity] = useState(null);

  const fetchGitHubProfile = async () => {
    try {
      setLoading(true);
      setError(null);

      const url = userId 
        ? `http://localhost:8080/api/github/profile?userId=${userId}`
        : 'http://localhost:8080/api/github/profile';

      const token = localStorage.getItem('token');
      const response = await fetch(url, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        if (response.status === 403) {
          throw new Error('You can only view your own GitHub profile');
        }
        throw new Error('Failed to fetch GitHub profile');
      }

      const data = await response.json();
      setProfileData(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const fetchRepositories = async () => {
    try {
      setLoading(true);
      setError(null);

      const url = userId 
        ? `http://localhost:8080/api/github/repositories?userId=${userId}`
        : 'http://localhost:8080/api/github/repositories';

      const token = localStorage.getItem('token');
      const response = await fetch(url, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error('Failed to fetch repositories');
      }

      const data = await response.json();
      setRepositories(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const fetchActivity = async () => {
    try {
      setLoading(true);
      setError(null);

      const url = userId 
        ? `http://localhost:8080/api/github/activity?userId=${userId}`
        : 'http://localhost:8080/api/github/activity';

      const token = localStorage.getItem('token');
      const response = await fetch(url, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error('Failed to fetch activity');
      }

      const data = await response.json();
      setActivity(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (activeTab === 'profile') {
      fetchGitHubProfile();
    } else if (activeTab === 'repositories') {
      fetchRepositories();
    } else if (activeTab === 'activity') {
      fetchActivity();
    }
  }, [activeTab, userId]);

  const renderProfile = () => {
    if (!profileData) return null;

    if (profileData.status === 'not_linked') {
      return (
        <div className="warning-message">
          <h3>GitHub Account Not Linked</h3>
          <p>{profileData.message}</p>
        </div>
      );
    }

    if (profileData.status === 'error') {
      return (
        <div className="error-message">
          <h3>Error Loading Profile</h3>
          <p>{profileData.message}</p>
        </div>
      );
    }

    try {
      let profile;
      if (typeof profileData.profileData === 'string') {
        profile = JSON.parse(profileData.profileData);
      } else {
        profile = profileData.profileData;
      }
      
      if (!profile) {
        return (
          <div className="warning-message">
            <h3>No Profile Data</h3>
            <p>Could not load GitHub profile data</p>
          </div>
        );
      }

      return (
        <div className="github-profile-header">
          <div className="profile-info">
            <img 
              src={profile.avatar_url} 
              alt={profile.login} 
              className="profile-avatar"
            />
            <div className="profile-details">
              <h2>{profile.name || profile.login}</h2>
              <p>@{profile.login}</p>
              {profile.bio && <p>{profile.bio}</p>}
              {profile.location && <p>📍 {profile.location}</p>}
              {profile.company && <p>🏢 {profile.company}</p>}
              <div className="profile-stats">
                <div className="stat-item">
                  <div className="stat-value">{profile.public_repos}</div>
                  <div className="stat-label">Repositories</div>
                </div>
                <div className="stat-item">
                  <div className="stat-value">{profile.followers}</div>
                  <div className="stat-label">Followers</div>
                </div>
                <div className="stat-item">
                  <div className="stat-value">{profile.following}</div>
                  <div className="stat-label">Following</div>
                </div>
              </div>
              <a 
                href={profile.html_url} 
                target="_blank" 
                rel="noopener noreferrer"
                style={{ color: '#667eea', marginTop: '10px', display: 'inline-block' }}
              >
                View on GitHub →
              </a>
            </div>
          </div>
        </div>
      );
    } catch (e) {
      return (
        <div className="error-message">
          <h3>Error Parsing Profile Data</h3>
          <p>{e.message}</p>
          <details style={{ marginTop: '10px' }}>
            <summary>Raw response</summary>
            <pre style={{ fontSize: '10px', maxHeight: '200px', overflow: 'auto' }}>
              {profileData.profileData}
            </pre>
          </details>
        </div>
      );
    }
  };

  const renderRepositories = () => {
    if (!repositories) return null;

    if (repositories.status === 'not_linked') {
      return (
        <div className="warning-message">
          <h3>GitHub Account Not Linked</h3>
          <p>{repositories.message}</p>
        </div>
      );
    }

    if (repositories.status === 'error') {
      return (
        <div className="error-message">
          <h3>Error Loading Repositories</h3>
          <p>{repositories.message}</p>
          {repositories.repositories && (
            <details style={{ marginTop: '10px' }}>
              <summary>Raw response</summary>
              <pre style={{ fontSize: '10px', maxHeight: '200px', overflow: 'auto' }}>
                {repositories.repositories}
              </pre>
            </details>
          )}
        </div>
      );
    }

    try {
      let repos;
      if (typeof repositories.repositories === 'string') {
        repos = JSON.parse(repositories.repositories);
      } else {
        repos = repositories.repositories;
      }
      
      if (!repos || repos.length === 0) {
        return (
          <div className="warning-message">
            <h3>No Repositories Found</h3>
            <p>This user has no public repositories</p>
          </div>
        );
      }

      return (
        <div className="repo-list">
          {repos.map((repo) => (
            <div key={repo.id} className="repo-item">
              <div className="repo-header">
                <a 
                  href={repo.html_url} 
                  target="_blank" 
                  rel="noopener noreferrer"
                  className="repo-name"
                >
                  {repo.name}
                </a>
                <span className="repo-meta-item">⭐ {repo.stargazers_count}</span>
              </div>
              {repo.description && (
                <div className="repo-description">{repo.description}</div>
              )}
              <div className="repo-meta">
                {repo.language && (
                  <div className="repo-meta-item">
                    <span>●</span> {repo.language}
                  </div>
                )}
                <div className="repo-meta-item">
                  <span>⑂</span> {repo.forks_count}
                </div>
                <div className="repo-meta-item">
                  <span>🕒</span> {new Date(repo.updated_at).toLocaleDateString()}
                </div>
              </div>
            </div>
          ))}
        </div>
      );
    } catch (e) {
      return (
        <div className="error-message">
          <h3>Error Parsing Repository Data</h3>
          <p>{e.message}</p>
          <details style={{ marginTop: '10px' }}>
            <summary>Raw response</summary>
            <pre style={{ fontSize: '10px', maxHeight: '200px', overflow: 'auto' }}>
              {repositories.repositories}
            </pre>
          </details>
        </div>
      );
    }
  };

  const renderActivity = () => {
    if (!activity) return null;

    if (activity.status === 'not_linked') {
      return (
        <div className="warning-message">
          <h3>GitHub Account Not Linked</h3>
          <p>{activity.message}</p>
        </div>
      );
    }

    if (activity.status === 'error') {
      return (
        <div className="error-message">
          <h3>Error Loading Activity</h3>
          <p>{activity.message}</p>
          {activity.activity && (
            <details style={{ marginTop: '10px' }}>
              <summary>Raw response</summary>
              <pre style={{ fontSize: '10px', maxHeight: '200px', overflow: 'auto' }}>
                {activity.activity}
              </pre>
            </details>
          )}
        </div>
      );
    }

    try {
      let events;
      if (typeof activity.activity === 'string') {
        events = JSON.parse(activity.activity);
      } else {
        events = activity.activity;
      }
      
      if (!events || events.length === 0) {
        return (
          <div className="warning-message">
            <h3>No Recent Activity</h3>
            <p>No recent public activity found</p>
          </div>
        );
      }

      return (
        <div className="activity-list">
          {events.map((event, index) => (
            <div key={index} className="activity-item">
              <div className="activity-type">{event.type}</div>
              <div className="activity-time">
                {new Date(event.created_at).toLocaleString()}
              </div>
            </div>
          ))}
        </div>
      );
    } catch (e) {
      return (
        <div className="error-message">
          <h3>Error Parsing Activity Data</h3>
          <p>{e.message}</p>
          <details style={{ marginTop: '10px' }}>
            <summary>Raw response</summary>
            <pre style={{ fontSize: '10px', maxHeight: '200px', overflow: 'auto' }}>
              {activity.activity}
            </pre>
          </details>
        </div>
      );
    }
  };

  if (loading) {
    return (
      <div className="github-profile-container">
        <div className="loading-spinner">
          <div>Loading...</div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="github-profile-container">
        <div className="error-message">
          <h3>Error</h3>
          <p>{error}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="github-profile-container">
      <h1>GitHub Profile</h1>
      
      <div className="tab-buttons">
        <button 
          className={`tab-button ${activeTab === 'profile' ? 'active' : ''}`}
          onClick={() => setActiveTab('profile')}
        >
          Profile
        </button>
        <button 
          className={`tab-button ${activeTab === 'repositories' ? 'active' : ''}`}
          onClick={() => setActiveTab('repositories')}
        >
          Repositories
        </button>
        <button 
          className={`tab-button ${activeTab === 'activity' ? 'active' : ''}`}
          onClick={() => setActiveTab('activity')}
        >
          Activity
        </button>
      </div>

      {activeTab === 'profile' && (
        <div className="github-section">
          <h3>👤 Profile Information</h3>
          {renderProfile()}
        </div>
      )}

      {activeTab === 'repositories' && (
        <div className="github-section">
          <h3>📚 Repositories</h3>
          {renderRepositories()}
        </div>
      )}

      {activeTab === 'activity' && (
        <div className="github-section">
          <h3>⚡ Recent Activity</h3>
          {renderActivity()}
        </div>
      )}
    </div>
  );
};

export default GitHubProfile;
