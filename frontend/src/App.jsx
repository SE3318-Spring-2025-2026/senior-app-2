import { Routes, Route } from 'react-router-dom';
import Login from './pages/Login';
import GitHubCallback from './pages/GitHubCallback';
import Dashboard from './pages/Dashboard';
import Users from './pages/Users';
import AuditLogs from './pages/AuditLogs';
import AccessDenied from './pages/AccessDenied';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import AdminRoute from './components/AdminRoute';
import ResetPassword from './pages/ResetPassword'; 

function App() {
  return (
    <Routes>
      <Route path="/" element={<Login />} />
      <Route path="/access-denied" element={<AccessDenied />} />
      <Route path="/reset-password" element={<ResetPassword />} />

      <Route path="/auth/github/callback" element={<GitHubCallback />} />
      <Route
        path="/panel"
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Dashboard />} />
        <Route
          path="users"
          element={
            <AdminRoute>
              <Users />
            </AdminRoute>
          }
        />
        <Route
          path="logs"
          element={
            <AdminRoute>
              <AuditLogs />
            </AdminRoute>
          }
        />
      </Route>
    </Routes>
  );
}

export default App;