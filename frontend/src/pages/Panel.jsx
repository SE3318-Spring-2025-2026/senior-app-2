import { useNavigate } from 'react-router-dom';
import './Panel.css';

function Panel() {
  const navigate = useNavigate();

  const handleLogout = () => {
    navigate('/');
  };

  return (
    <div className="panel-container">
      <header className="panel-header">
        <h1>Dashboard</h1>
        <button className="logout-button" onClick={handleLogout}>Log Out</button>
      </header>
      <main className="panel-content">
        <p>Welcome! This panel is empty for now.</p>
      </main>
    </div>
  );
}

export default Panel;
