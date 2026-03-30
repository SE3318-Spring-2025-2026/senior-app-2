import { useNavigate } from 'react-router-dom';
import './AccessDenied.css';

function AccessDenied() {
  const navigate = useNavigate();

  return (
    <div className="access-denied-container">
      <div className="access-denied-card">
        <div className="access-denied-icon">403</div>
        <h1>Access Denied</h1>
        <p>You do not have permission to view this page.</p>
        <button className="access-denied-button" onClick={() => navigate('/panel')}>
          Go to Homepage
        </button>
      </div>
    </div>
  );
}

export default AccessDenied;
