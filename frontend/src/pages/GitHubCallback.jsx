import { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

function GitHubCallback() {
  const navigate = useNavigate();
  const location = useLocation();
  const { loginUser } = useAuth();

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const token = params.get('token');
    const userJson = params.get('user');

    if (token && userJson) {
      try {
        const user = JSON.parse(userJson);
        loginUser(token, user);
        navigate('/panel');
      } catch (err) {
        navigate('/?error=invalid_user_data');
      }
    } else {
      navigate('/?error=github_auth_failed');
    }
  }, [location, navigate, loginUser]);

  return (
    <div className="loading" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh' }}>
      Connecting to SeniorApp via GitHub...
    </div>
  );
}

export default GitHubCallback;