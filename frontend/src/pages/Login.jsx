import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  login as loginApi,
  getGithubLoginUrl,
  checkStudentIdValidity,
} from '../services/api';
import './Login.css';

function Login() {
  const [role, setRole] = useState('staff');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [studentId, setStudentId] = useState('');
  const [studentCheck, setStudentCheck] = useState(null);
  const [error, setError] = useState('');
  const [forgotSuccess, setForgotSuccess] = useState(false);
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

  const handleVerifyStudentId = async (e) => {
    e.preventDefault();
    setError('');
    setStudentCheck(null);
    setLoading(true);
    try {
      const result = await checkStudentIdValidity(studentId.trim());
      if (!result.valid) {
        setError('This student ID is not on the approved list. Contact your coordinator.');
        return;
      }
      setStudentCheck(result);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const startGithub = async (flow) => {
    setError('');
    setLoading(true);
    try {
      const authUrl = await getGithubLoginUrl(studentId.trim(), flow);
      window.location.href = authUrl;
    } catch (err) {
      setError(err.message);
      setLoading(false);
    }
  };

  const handleForgotPassword = async () => {
    if (!email) {
      setError('Please enter your email address in the field above to reset your password.');
      return;
    }
    setError('');
    setForgotSuccess(false);
    setLoading(true);
    try {
      await fetch('http://localhost:8080/auth/reset-password/forgot', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email }),
      });
      // Always show the same message regardless of whether the email exists
      // to prevent email enumeration.
      setForgotSuccess(true);
    } catch {
      setError('Failed to send reset link. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  const resetStudentFlow = () => {
    setStudentCheck(null);
    setStudentId('');
    setError('');
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <h1>Sign In</h1>

        <div className="role-toggle">
          <button
            type="button"
            className={`role-option ${role === 'staff' ? 'active' : ''}`}
            onClick={() => {
              setRole('staff');
              setError('');
            }}
          >
            Staff
          </button>
          <button
            type="button"
            className={`role-option ${role === 'student' ? 'active' : ''}`}
            onClick={() => {
              setRole('student');
              setError('');
              setStudentCheck(null);
            }}
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
            <div className="form-group" style={{ marginBottom: '8px' }}>
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

            {forgotSuccess && (
              <div style={{ background: '#dcfce7', color: '#166534', padding: '10px 12px', borderRadius: '8px', marginBottom: '12px', fontSize: '13px', border: '1px solid #bbf7d0' }}>
                If that email exists in our system, a reset link has been sent to it.
              </div>
            )}
            <div style={{ textAlign: 'right', marginBottom: '20px' }}>
              <button
                type="button"
                onClick={handleForgotPassword}
                disabled={loading}
                style={{
                  background: 'none',
                  border: 'none',
                  color: '#4f46e5',
                  fontSize: '13px',
                  cursor: loading ? 'not-allowed' : 'pointer',
                  padding: 0,
                  textDecoration: 'underline',
                  opacity: loading ? 0.6 : 1,
                }}
              >
                {loading ? 'Sending...' : 'Forgot Password?'}
              </button>
            </div>

            <button type="submit" className="login-button" disabled={loading}>
              {loading ? 'Signing in...' : 'Log In'}
            </button>
          </form>
        ) : !studentCheck ? (
          <form onSubmit={handleVerifyStudentId}>
            <p className="github-hint">
              Enter your student ID as provided by your coordinator. We will verify it before GitHub sign-in.
            </p>
            <div className="form-group">
              <label htmlFor="studentId">Student ID</label>
              <input
                id="studentId"
                type="text"
                placeholder="e.g. your university student number"
                value={studentId}
                onChange={(e) => setStudentId(e.target.value)}
                required
              />
            </div>
            <button type="submit" className="login-button" disabled={loading}>
              {loading ? 'Checking...' : 'Continue'}
            </button>
          </form>
        ) : (
          <div className="github-section">
            {studentCheck.linked ? (
              <>
                <p className="github-hint">
                  Your student ID is verified and linked to an account. Sign in with the same GitHub account you used
                  when you registered.
                </p>
                <button
                  type="button"
                  className="github-button"
                  onClick={() => startGithub('login')}
                  disabled={loading}
                >
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                    <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z" />
                  </svg>
                  Sign in with GitHub
                </button>
              </>
            ) : (
              <>
                <p className="github-hint">
                  Your student ID is approved. Match your GitHub account to create your SeniorApp student profile (one
                  time).
                </p>
                <button
                  type="button"
                  className="github-button"
                  onClick={() => startGithub('link')}
                  disabled={loading}
                >
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                    <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z" />
                  </svg>
                  Continue with GitHub
                </button>
              </>
            )}
            <button type="button" className="login-button secondary-outline" onClick={resetStudentFlow} style={{ marginTop: '12px' }}>
              Use a different student ID
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default Login;
