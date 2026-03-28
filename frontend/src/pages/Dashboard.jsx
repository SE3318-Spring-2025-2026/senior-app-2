import { useAuth } from '../context/AuthContext';
import './Dashboard.css';

function Dashboard() {
  const { user } = useAuth();

  return (
    <div className="dashboard">
      <h1>Welcome, {user?.fullName || 'User'}</h1>
      <p className="dashboard-subtitle">You are logged in as <strong>{user?.role}</strong></p>

      <div className="dashboard-cards">
        <div className="dashboard-card">
          <div className="card-label">Email</div>
          <div className="card-value">{user?.email}</div>
        </div>
        <div className="dashboard-card">
          <div className="card-label">Role</div>
          <div className="card-value role-badge">{user?.role}</div>
        </div>
      </div>
    </div>
  );
}

export default Dashboard;
