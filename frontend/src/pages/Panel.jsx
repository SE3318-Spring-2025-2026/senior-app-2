import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Panel.css';

function Panel() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div className="panel-container">
      <header className="panel-header">
        <h1>Dashboard</h1>
        <div className="header-right">
          <span className="user-info">
            {user?.fullName || user?.email}
            <span className="user-role">{user?.role}</span>
          </span>
          <button className="logout-button" onClick={handleLogout}>Log Out</button>
        </div>
      </header>
      <main className="panel-content">
        <p>Welcome, {user?.fullName || 'User'}! This panel is empty for now.</p>
      </main>
    </div>
  );
}

export default Panel;
