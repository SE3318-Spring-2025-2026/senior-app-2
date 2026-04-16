import { useState } from 'react';
import { addOrRemoveGroupMember } from '../services/api';
import './GroupMembers.css';

function GroupMembers({ groupId, members = [], isTeamLeader, onMemberRemoved }) {
  const [loadingMemberId, setLoadingMemberId] = useState(null);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const handleRemoveMember = async (studentId) => {
    if (!window.confirm('Are you sure you want to remove this member?')) {
      return;
    }

    setLoadingMemberId(studentId);
    setError(null);
    setSuccess(null);

    try {
      await addOrRemoveGroupMember(groupId, studentId, 'remove');
      setSuccess(`Student ${studentId} removed successfully`);
      onMemberRemoved(studentId);
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      setError(err.message || 'Failed to remove member');
    } finally {
      setLoadingMemberId(null);
    }
  };

  return (
    <div className="group-members-container">
      <h3>Group Members</h3>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      {members.length === 0 ? (
        <p className="no-members">No members in this group yet</p>
      ) : (
        <ul className="members-list">
          {members.map((memberId) => (
            <li key={memberId} className="member-item">
              <span className="member-id">{memberId}</span>
              {isTeamLeader && (
                <button
                  className="btn-remove"
                  onClick={() => handleRemoveMember(memberId)}
                  disabled={loadingMemberId === memberId}
                >
                  {loadingMemberId === memberId ? 'Removing...' : 'Remove'}
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default GroupMembers;
