import { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import './Login.css'; // Reusing the styles (card structure) from the Login page

function ResetPassword() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();

  const [isValidating, setIsValidating] = useState(true);
  const [isTokenValid, setIsTokenValid] = useState(false);
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  // Check the token's validity when the page loads 
  useEffect(() => {
    if (!token) {
      setIsValidating(false);
      return;
    }

    const checkToken = async () => {
      try {
        const response = await fetch(`http://localhost:8080/auth/reset-password/check-token-validity?token=${token}`);
        if (!response.ok) throw new Error('Network response was not ok');
        
        const data = await response.json();
        setIsTokenValid(data.valid);
      } catch (err) {
        setIsTokenValid(false);
      } finally {
        setIsValidating(false);
      }
    };

    checkToken();
  }, [token]);

  // Send the new password to the backend 
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');

    if (password !== confirmPassword) {
      setError('Passwords do not match!');
      return;
    }

    if (password.length < 6) {
      setError('Password must be at least 6 characters long.');
      return;
    }

    setLoading(true);

    try {
      const response = await fetch('http://localhost:8080/auth/reset-password/new-password', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ token, newPassword: password }),
      });

      if (!response.ok) {
        throw new Error('Failed to update password. Token might be expired.');
      }

      setMessage('Password updated successfully! Redirecting to login...');
      
      // Redirect to the Login page after 3 seconds upon success
      setTimeout(() => {
        navigate('/');
      }, 3000);

    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  if (isValidating) {
    return (
      <div className="login-container">
        <div className="login-card" style={{ textAlign: 'center' }}>
          <h2>Checking Link...</h2>
          <p>Please wait while we verify your password reset link.</p>
        </div>
      </div>
    );
  }

  // Show an error if the token is invalid or expired
  if (!isTokenValid) {
    return (
      <div className="login-container">
        <div className="login-card" style={{ textAlign: 'center' }}>
          <h2 style={{ color: '#dc2626' }}>Invalid or Expired Link</h2>
          <p style={{ marginTop: '10px', marginBottom: '20px' }}>
            This password reset link is no longer valid. It may have expired or already been used.
          </p>
          <button className="login-button" onClick={() => navigate('/login')}>
            Return to Login
          </button>
        </div>
      </div>
    );
  }

  // Show the password reset form if the token is valid
  return (
    <div className="login-container">
      <div className="login-card">
        <h1>Create New Password</h1>
        
        {error && <div className="error-message">{error}</div>}
        {message && (
          <div style={{ background: '#dcfce3', color: '#166534', padding: '10px', borderRadius: '8px', marginBottom: '16px', fontSize: '14px', border: '1px solid #bbf7d0' }}>
            {message}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="new-password">New Password</label>
            <input
              id="new-password"
              type="password"
              placeholder="Enter new password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          <div className="form-group" style={{ marginBottom: '24px' }}>
            <label htmlFor="confirm-password">Confirm Password</label>
            <input
              id="confirm-password"
              type="password"
              placeholder="Confirm new password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
            />
          </div>

          <button type="submit" className="login-button" disabled={loading || message !== ''}>
            {loading ? 'Updating...' : 'Reset Password'}
          </button>
        </form>
      </div>
    </div>
  );
}

export default ResetPassword;