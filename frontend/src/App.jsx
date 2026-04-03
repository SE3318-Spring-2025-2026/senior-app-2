import { Routes, Route } from 'react-router-dom';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Users from './pages/Users';
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
      </Route>
    </Routes>
  );
}

export default App;