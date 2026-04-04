
import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { githubCallback } from '../services/api';
import './Login.css';

function GitHubCallback() {
    const [searchParams] = useSearchParams();
    const [error, setError] = useState('');
    const navigate = useNavigate();
    const { loginUser } = useAuth();

    useEffect(() => {
        const code = searchParams.get('code');
        const state = searchParams.get('state');

        if (!code || !state) {
            setError('Missing authorization parameters from GitHub.');
            return;
        }

        githubCallback(code, state)
            .then((data) => {
                loginUser(data.token, data.user);
                navigate('/panel', { replace: true });
            })
            .catch((err) => {
                setError(err.message || 'GitHub authentication failed.');
            });
    }, [searchParams, loginUser, navigate]);

    if (error) {
        return (
            <div className="login-container">
                <div className="login-card">
                    <h1>Authentication Failed</h1>
                    <div className="error-message">{error}</div>
                    <Link to="/" className="login-button" style={{ textAlign: 'center', textDecoration: 'none', display: 'block' }}>
                        Back to Login
                    </Link>
                </div>
            </div>
        );
    }

    return (
        <div className="login-container">
            <div className="login-card" style={{ textAlign: 'center' }}>
                <h1>Signing you in...</h1>
                <p style={{ color: '#666' }}>Completing GitHub authentication</p>
            </div>
        </div>
    );
}

export default GitHubCallback;
