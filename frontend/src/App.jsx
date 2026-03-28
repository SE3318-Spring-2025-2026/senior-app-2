import { Routes, Route } from 'react-router-dom';
import Login from './pages/Login';
import Panel from './pages/Panel';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
  return (
    <Routes>
      <Route path="/" element={<Login />} />
      <Route
        path="/panel"
        element={
          <ProtectedRoute>
            <Panel />
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}

export default App;
