import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './StudentList.css';

function StudentList() {
  const navigate = useNavigate();
  const [students, setStudents] = useState([]);
  const [filtered, setFiltered] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');

  useEffect(() => {
    fetchStudents();
  }, []);

  useEffect(() => {
    const q = search.toLowerCase().trim();
    if (!q) {
      setFiltered(students);
    } else {
      setFiltered(students.filter(s =>
        (s.fullName || '').toLowerCase().includes(q) ||
        (s.email || '').toLowerCase().includes(q) ||
        (s.githubUsername || '').toLowerCase().includes(q) ||
        String(s.id).includes(q)
      ));
    }
  }, [search, students]);

  async function fetchStudents() {
    try {
      const token = localStorage.getItem('token');
      const res = await fetch('http://localhost:8080/api/students', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (!res.ok) throw new Error('Failed to fetch students');
      const data = await res.json();
      setStudents(data);
      setFiltered(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  if (loading) {
    return (
      <div className="sl-page">
        <div className="sl-loading">
          <div className="sl-spinner" />
          <p>Loading students...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="sl-page">
        <div className="sl-error">{error}</div>
      </div>
    );
  }

  return (
    <div className="sl-page">
      <div className="sl-header">
        <div>
          <h1>Students</h1>
          <p className="sl-subtitle">{students.length} students registered</p>
        </div>
        <div className="sl-search-box">
          <svg className="sl-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8" />
            <path d="m21 21-4.35-4.35" />
          </svg>
          <input
            type="text"
            placeholder="Search by name, email, or GitHub..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="sl-search-input"
          />
        </div>
      </div>

      {filtered.length === 0 ? (
        <div className="sl-empty">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" width="48" height="48">
            <path d="M15 19.128a9.38 9.38 0 0 0 2.625.372 9.337 9.337 0 0 0 4.121-.952 4.125 4.125 0 0 0-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 0 1 8.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0 1 11.964-3.07M12 6.375a3.375 3.375 0 1 1-6.75 0 3.375 3.375 0 0 1 6.75 0Zm8.25 2.25a2.625 2.625 0 1 1-5.25 0 2.625 2.625 0 0 1 5.25 0Z" />
          </svg>
          <h3>No students found</h3>
          <p>{search ? 'Try a different search term' : 'No students registered yet'}</p>
        </div>
      ) : (
        <div className="sl-grid">
          {filtered.map((s) => (
            <div key={s.id} className="sl-card">
              <div className="sl-card-top">
                <div className="sl-avatar">
                  {s.githubUsername ? (
                    <img
                      src={`https://github.com/${s.githubUsername}.png?size=80`}
                      alt={s.fullName}
                      onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'flex'; }}
                    />
                  ) : null}
                  <div className="sl-avatar-fallback" style={{ display: s.githubUsername ? 'none' : 'flex' }}>
                    {(s.fullName || 'S').charAt(0).toUpperCase()}
                  </div>
                </div>
                <div className="sl-card-info">
                  <h3 className="sl-card-name">{s.fullName}</h3>
                  <p className="sl-card-email">{s.email}</p>
                  {s.githubUsername && (
                    <a
                      href={`https://github.com/${s.githubUsername}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="sl-card-github"
                    >
                      <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                        <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z" />
                      </svg>
                      @{s.githubUsername}
                    </a>
                  )}
                </div>
              </div>
              <div className="sl-card-actions">
                {s.githubUsername && (
                  <button
                    className="sl-btn sl-btn-github"
                    onClick={() => navigate(`/panel/github-profile/${s.id}`)}
                  >
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                      <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z" />
                    </svg>
                    View Profile
                  </button>
                )}
                {!s.githubUsername && (
                  <span className="sl-btn sl-btn-disabled">No GitHub linked</span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default StudentList;
