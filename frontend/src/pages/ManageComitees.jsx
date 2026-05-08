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
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // Track selected member for each committee
  const [selectedMembers, setSelectedMembers] = useState({});

  async function loadData() {
    setLoading(true);
    setError('');
    try {
      const committeeRes = await getTemplateCommittees(templateId);
      setCommittees(committeeRes?.data || []);

      // Members list is only needed for coordinator/admin add flow.
      if (isCoordinator) {
        const memberRes = await getTemplateProfessors();
        setMembers(memberRes?.data || []);
      } else {
        setMembers([]);
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

  const handleCreateCommittee = async () => {
    try {
      const res = await createTemplateCommittee(templateId);
      setCommittees((prev) => [...prev, res.data]);
    } catch (err) {
      setError(err.message || 'Failed to create committee.');
    }
  };

  const handleAddMember = async (committeeId) => {
    const memberUserId = selectedMembers[committeeId];
    if (!memberUserId) return;
    
    try {
      const res = await addProfessorToTemplateCommittee(templateId, committeeId, memberUserId);
      setCommittees((prev) =>
        prev.map((committee) =>
          committee.committeeId === committeeId ? res.data : committee
        )
      );
      // Reset selected member for this committee
      setSelectedMembers((prev) => ({ ...prev, [committeeId]: '' }));
    } catch (err) {
      setError(err.message || 'Failed to add member. ' + (err.response?.data?.message || ''));
    }
  };

  const handleDeleteCommittee = async (committeeId) => {
    try {
      await deleteTemplateCommittee(templateId, committeeId);
      setCommittees((prev) => prev.filter((committee) => committee.committeeId !== committeeId));
    } catch (err) {
      setError(err.message || 'Failed to delete committee.');
    }
  };

  const handleRemoveMember = async (committeeId, memberUserId) => {
    try {
      const res = await removeProfessorFromTemplateCommittee(templateId, committeeId, memberUserId);
      setCommittees((prev) =>
        prev.map((committee) =>
          committee.committeeId === committeeId ? res.data : committee
        )
      );
    } catch (err) {
      setError(err.message || 'Failed to remove member.');
    }
  };

  return (
    <div className="manage-comitees-page">
      <div className="manage-comitees-header">
        <h1>Manage Committees</h1>
        {isCoordinator && (
          <button className="add-comitees-btn" onClick={handleCreateCommittee}>
            + Add Committee
          </button>
        )}
      </div>

      {loading && <div className="state-box">Loading...</div>}
      {error && !loading && <div className="state-box error">{error}</div>}

      {!loading && !error && (
        <div className="committee-grid">
          {committees.map((committee) => {
            const currentMembers = committee.professors || [];
            const atCapacity = currentMembers.length >= 5;
            
            return (
              <article key={committee.committeeId} className="committee-card">
                <div className="committee-card-header">
                  <h3>{committee.name}</h3>
                  <div className="committee-card-actions">
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
                  {currentMembers.map((member) => (
                    <div key={member.userId} className="committee-member-item">
                      <div>
                        <span>{member.fullName || member.email}</span>
                        <small>{member.email}</small>
                      </div>
                      {isCoordinator && (
                        <button
                          type="button"
                          className="remove-prof-inline-btn"
                          onClick={() => handleRemoveMember(committee.committeeId, member.userId)}
                          title="Remove member"
                        >
                          ✕
                        </button>
                      )}
                    </div>
                  ))}
                  {currentMembers.length === 0 && (
                    <div className="committee-empty">No member assigned.</div>
                  )}
                  
                  {isCoordinator && (
                    <div className="committee-add-member-section" style={{ marginTop: '1rem', borderTop: '1px solid #eee', paddingTop: '1rem' }}>
                      {atCapacity ? (
                        <div className="capacity-reached" style={{ fontSize: '0.85rem', color: '#666' }}>Committee has reached max capacity (5 members).</div>
                      ) : (
                        <div style={{ display: 'flex', gap: '8px' }}>
                          <select 
                            className="member-select"
                            value={selectedMembers[committee.committeeId] || ''}
                            onChange={(e) => setSelectedMembers({...selectedMembers, [committee.committeeId]: e.target.value})}
                            style={{ flex: 1, padding: '4px 8px', borderRadius: '4px', border: '1px solid #ccc' }}
                          >
                            <option value="">Select a member to add...</option>
                            {members.map(member => {
                              const alreadyAdded = currentMembers.some(m => m.userId === member.userId);
                              return (
                                <option key={member.userId} value={member.userId} disabled={alreadyAdded}>
                                  {member.fullName || member.email} {alreadyAdded ? '(Added)' : ''}
                                </option>
                              );
                            })}
                          </select>
                          <button 
                            type="button"
                            className="add-member-btn"
                            onClick={() => handleAddMember(committee.committeeId)}
                            disabled={!selectedMembers[committee.committeeId]}
                            style={{ padding: '4px 12px', background: '#007bff', color: 'white', border: 'none', borderRadius: '4px', cursor: selectedMembers[committee.committeeId] ? 'pointer' : 'not-allowed', opacity: selectedMembers[committee.committeeId] ? 1 : 0.6 }}
                          >
                            Add
                          </button>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </article>
            );
          })}
          {committees.length === 0 && (
            <div className="state-box">No committee yet. Click Add Committee.</div>
          )}
        </div>
      )}
    </div>
  );
}

export default ManageComitees;

