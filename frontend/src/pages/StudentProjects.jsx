import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyStudentProjects } from '../services/api';
import './StudentProjects.css';

function StudentProjects() {
  const [projects, setProjects] = useState([]);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    getMyStudentProjects()
      .then((res) => setProjects(Array.isArray(res) ? res : []))
      .catch((e) => setError(e.message || 'Failed to load projects.'));
  }, []);

  return (
    <div className="student-projects-page">
      <h1>Projelerim</h1>
      {error && <div className="student-projects-error">{error}</div>}
      <div className="student-projects-grid">
        {projects.map((project) => (
          <button
            key={`${project.projectId}-${project.groupId}`}
            className="student-project-card"
            onClick={() => navigate(`/panel/student-projects/${project.projectId}`)}
          >
            <h3>{project.projectTitle}</h3>
            <p>Project #{project.projectId}</p>
            <p>Group #{project.groupId}</p>
            <p>{project.term}</p>
          </button>
        ))}
      </div>
    </div>
  );
}

export default StudentProjects;
