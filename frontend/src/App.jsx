import { Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import GitHubCallback from './pages/GitHubCallback';
import Dashboard from './pages/Dashboard';
import Users from './pages/Users';
import AuditLogs from './pages/AuditLogs';
import AccessDenied from './pages/AccessDenied';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import AdminRoute from './components/AdminRoute';
import StudentManagement from './pages/StudentManagement';
import ResetPassword from './pages/ResetPassword';
import IntegrationSettings from './pages/IntegrationSettings'; 

function App() {
  return (
    <Routes>
      <Route path="/" element={<Login />} />
      <Route path="/auth/callback" element={<GitHubCallback />} />
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
        
        {/* Koordinatör rotası */}
        <Route path="whitelist" element={<StudentManagement />} />
        
        {/* Team Leader - Integration Settings */}
        <Route path="integrations" element={<IntegrationSettings />} />
        
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
      
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  );
}

export default App;
