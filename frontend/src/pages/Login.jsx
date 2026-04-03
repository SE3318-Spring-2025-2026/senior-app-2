import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { login as loginApi } from '../services/api';
import './Login.css';

function Login() {
  const [role, setRole] = useState('staff');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { loginUser } = useAuth();

  const handleStaffSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const data = await loginApi(email, password);
      loginUser(data.token, data.user);
      navigate('/panel');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleGitHubLogin = () => {
    window.location.href = 'http://localhost:8080/api/auth/github';
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <h1>Sign In</h1>

        <div className="role-toggle">
          <button
            className={`role-option ${role === 'staff' ? 'active' : ''}`}
            onClick={() => { setRole('staff'); setError(''); }}
          >
            Staff
          </button>
          <button
            className={`role-option ${role === 'student' ? 'active' : ''}`}
            onClick={() => { setRole('student'); setError(''); }}
          >
            Student
          </button>
        </div>

        {error && <div className="error-message">{error}</div>}

        {role === 'staff' ? (
          <form onSubmit={handleStaffSubmit}>
            <div className="form-group">
              <label htmlFor="email">Email</label>
              <input
                id="email"
                type="text"
                placeholder="Enter your email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            <div className="form-group">
              <label htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            <button type="submit" className="login-button" disabled={loading}>
              {loading ? 'Signing in...' : 'Log In'}
            </button>
          </form>
        ) : (
          <div className="github-section">
            <p className="github-hint">Sign in with your GitHub account linked to your student ID.</p>
            <button className="github-button" onClick={handleGitHubLogin}>
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/>
              </svg>
              Sign in with GitHub
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default Login;