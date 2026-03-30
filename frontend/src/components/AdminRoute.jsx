import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

function AdminRoute({ children }) {
  const { user, loading } = useAuth();

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  if (!user) {
    return <Navigate to="/" replace />;
  }

  if (user.role !== 'ADMIN') {
    return <Navigate to="/access-denied" replace />;
  }

  return children;
}

export default AdminRoute;
