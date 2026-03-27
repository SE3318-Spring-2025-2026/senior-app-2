import { Routes, Route } from 'react-router-dom';
import Login from './pages/Login';
import Panel from './pages/Panel';

function App() {
  return (
    <Routes>
      <Route path="/" element={<Login />} />
      <Route path="/panel" element={<Panel />} />
    </Routes>
  );
}

export default App;
