import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Layout.css';

function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar-logo">SeniorApp</div>

        <nav className="sidebar-nav">
          <NavLink 
            to="/panel" 
            end 
            className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
          >
            Dashboard
          </NavLink>

          {user?.role === 'COORDINATOR' && (
            <NavLink 
              to="/panel/whitelist" 
              className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
            >
              Student Whitelist
            </NavLink>
          )}

          {user?.role === 'ADMIN' && (
            <NavLink 
              to="/panel/users" 
              className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
            >
              Users
            </NavLink>
          )}
        </nav>

        <div className="sidebar-footer">
          <div className="sidebar-user">
            <span className="sidebar-user-name">{user?.fullName || user?.email}</span>
            <span className="sidebar-user-role">{user?.role}</span>
          </div>
          <button className="sidebar-logout" onClick={handleLogout}>Log Out</button>
        </div>
      </aside>

      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}

export default Layout;