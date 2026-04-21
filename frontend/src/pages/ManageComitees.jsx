import { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  addProfessorToTemplateCommittee,
  createTemplateCommittee,
  deleteTemplateCommittee,
  getTemplateCommittees,
  getTemplateProfessors,
  removeProfessorFromTemplateCommittee,
} from '../services/api';
import './ManageComitees.css';

function ManageComitees() {
  const { templateId } = useParams();
  const { user } = useAuth();
  const isCoordinator = user?.role === 'COORDINATOR' || user?.role === 'ADMIN';
  const [committees, setCommittees] = useState([]);
  const [professors, setProfessors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [popupCommitteeId, setPopupCommitteeId] = useState(null);
  const [query, setQuery] = useState('');

  async function loadData() {
    setLoading(true);
    setError('');
    try {
      const committeeRes = await getTemplateCommittees(templateId);
      setCommittees(committeeRes?.data || []);

      // Professors list is only needed for coordinator/admin add flow.
      if (isCoordinator) {
        const professorRes = await getTemplateProfessors();
        setProfessors(professorRes?.data || []);
      } else {
        setProfessors([]);
      }
    } catch (err) {
      setError(err.message || 'Failed to load committees.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, [templateId, isCoordinator]);

  const popupCommittee = committees.find((c) => c.committeeId === popupCommitteeId) || null;

  const filteredProfessors = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    return professors.filter((prof) => {
      if (!normalized) return true;
      return `${prof.fullName || ''} ${prof.email || ''}`.toLowerCase().includes(normalized);
    });
  }, [professors, query]);

  const handleCreateCommittee = async () => {
    try {
      const res = await createTemplateCommittee(templateId);
      setCommittees((prev) => [...prev, res.data]);
    } catch (err) {
      setError(err.message || 'Failed to create committee.');
    }
  };

  const handleAddProfessor = async (committeeId, professorUserId) => {
    try {
      const res = await addProfessorToTemplateCommittee(templateId, committeeId, professorUserId);
      setCommittees((prev) =>
        prev.map((committee) =>
          committee.committeeId === committeeId ? res.data : committee
        )
      );
    } catch (err) {
      setError(err.message || 'Failed to add professor.');
    }
  };

  const handleDeleteCommittee = async (committeeId) => {
    try {
      await deleteTemplateCommittee(templateId, committeeId);
      setCommittees((prev) => prev.filter((committee) => committee.committeeId !== committeeId));
      if (popupCommitteeId === committeeId) setPopupCommitteeId(null);
    } catch (err) {
      setError(err.message || 'Failed to delete committee.');
    }
  };

  const handleRemoveProfessor = async (committeeId, professorUserId) => {
    try {
      const res = await removeProfessorFromTemplateCommittee(templateId, committeeId, professorUserId);
      setCommittees((prev) =>
        prev.map((committee) =>
          committee.committeeId === committeeId ? res.data : committee
        )
      );
    } catch (err) {
      setError(err.message || 'Failed to remove professor.');
    }
  };

  return (
    <div className="manage-comitees-page">
      <div className="manage-comitees-header">
        <h1>Manage Comitees</h1>
        {isCoordinator && (
          <button className="add-comitees-btn" onClick={handleCreateCommittee}>
            + Add Comitees
          </button>
        )}
      </div>

      {loading && <div className="state-box">Loading...</div>}
      {error && !loading && <div className="state-box error">{error}</div>}

      {!loading && !error && (
        <div className="committee-grid">
          {committees.map((committee) => (
            <article key={committee.committeeId} className="committee-card">
              <div className="committee-card-header">
                <h3>{committee.name}</h3>
                <div className="committee-card-actions">
                  {isCoordinator && (
                    <button
                      type="button"
                      className="settings-btn"
                      onClick={() => {
                        setPopupCommitteeId(committee.committeeId);
                        setQuery('');
                      }}
                      title="Settings"
                    >
                      ⚙
                    </button>
                  )}
                  {isCoordinator && (
                    <button
                      type="button"
                      className="delete-committee-btn"
                      onClick={() => handleDeleteCommittee(committee.committeeId)}
                      title="Delete committee"
                    >
                      ✕
                    </button>
                  )}
                </div>
              </div>
              <div className="committee-member-list">
                {(committee.professors || []).map((prof) => (
                  <div key={prof.userId} className="committee-member-item">
                    <div>
                      <span>{prof.fullName || prof.email}</span>
                      <small>{prof.email}</small>
                    </div>
                    {isCoordinator && (
                      <button
                        type="button"
                        className="remove-prof-inline-btn"
                        onClick={() => handleRemoveProfessor(committee.committeeId, prof.userId)}
                        title="Remove professor"
                      >
                        ✕
                      </button>
                    )}
                  </div>
                ))}
                {(committee.professors || []).length === 0 && (
                  <div className="committee-empty">No professor assigned.</div>
                )}
              </div>
            </article>
          ))}
          {committees.length === 0 && (
            <div className="state-box">No committee yet. Click Add Comitees.</div>
          )}
        </div>
      )}

      {isCoordinator && popupCommittee && (
        <div className="popup-backdrop" onClick={() => setPopupCommitteeId(null)}>
          <div className="popup-panel" onClick={(e) => e.stopPropagation()}>
            <div className="popup-head">
              <h3>{popupCommittee.name} - Professors</h3>
              <button type="button" className="close-btn" onClick={() => setPopupCommitteeId(null)}>✕</button>
            </div>
            <input
              className="popup-search"
              placeholder="Search professor..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
            <div className="popup-list">
              {filteredProfessors.map((prof) => {
                const alreadyAdded = (popupCommittee.professors || []).some((item) => item.userId === prof.userId);
                return (
                  <div key={prof.userId} className="popup-row">
                    <div>
                      <div>{prof.fullName || '-'}</div>
                      <small>{prof.email}</small>
                    </div>
                    <button
                      type="button"
                      className="add-prof-btn"
                      onClick={() => handleAddProfessor(popupCommittee.committeeId, prof.userId)}
                      disabled={alreadyAdded}
                    >
                      {alreadyAdded ? 'Added' : '+'}
                    </button>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default ManageComitees;
