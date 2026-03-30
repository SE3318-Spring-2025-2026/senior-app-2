import { Routes, Route } from 'react-router-dom';
import Login from './pages/Login';
import GitHubCallback from './pages/GitHubCallback';
import Dashboard from './pages/Dashboard';
import Users from './pages/Users';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
  return (
    <Routes>
      <Route path="/" element={<Login />} />
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
        <Route path="users" element={<Users />} />
      </Route>
    </Routes>
  );
}

export default App;
