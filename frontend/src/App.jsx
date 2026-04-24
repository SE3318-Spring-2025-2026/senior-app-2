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
import TemplateBuilder from './pages/TemplateBuilder';
import MyProjects from './pages/MyProjects';
import ProjectInspection from './pages/ProjectInspection';
import ManageComitees from './pages/ManageComitees';
import TeamManagement from './pages/TeamManagement';
import StudentProjects from './pages/StudentProjects';
import StudentProjectPage from './pages/StudentProjectPage';
import PerformanceAnalytics from './pages/PerformanceAnalytics';
import CodeReviewComparison from './pages/CodeReviewComparison';

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
        <Route path="template-builder" element={<TemplateBuilder />} />
        <Route path="create-group" element={<TeamManagement />} />
        <Route path="my-student-projects" element={<StudentProjects />} />
        <Route path="student-projects/:projectId" element={<StudentProjectPage />} />
        <Route path="my-projects" element={<MyProjects />} />
        <Route path="templates/:templateId" element={<ProjectInspection />} />
        <Route path="templates/:templateId/manage-comitees" element={<ManageComitees />} />
        
        {/* Analitik Rotası */}
        <Route path="analytics" element={<PerformanceAnalytics />} />
        <Route path="projects" element={<ProjectInspection />} />
        
        {/* Code Review Comparison */}
        <Route path="review/:projectId" element={<CodeReviewComparison />} />
        
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
