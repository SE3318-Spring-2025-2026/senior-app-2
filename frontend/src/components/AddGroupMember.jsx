import { useState } from 'react';
import { addOrRemoveGroupMember } from '../services/api';
import './GroupMembers.css';

function AddGroupMember({ groupId, isTeamLeader, onMemberAdded }) {
  const [studentId, setStudentId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const handleAddMember = async (e) => {
    e.preventDefault();

    if (!studentId.trim()) {
      setError('Please enter a student ID');
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const response = await addOrRemoveGroupMember(groupId, studentId.trim(), 'add');
      setSuccess(`Student ${studentId} added successfully`);
      setStudentId('');
      onMemberAdded(studentId.trim());
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      setError(err.message || 'Failed to add member');
    } finally {
      setLoading(false);
    }
  };

  if (!isTeamLeader) {
    return null;
  }

  return (
    <div className="add-member-container">
      <h3>Add Group Member</h3>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <form onSubmit={handleAddMember} className="add-member-form">
        <div className="form-group">
          <label htmlFor="studentId">Student ID</label>
          <input
            type="text"
            id="studentId"
            value={studentId}
            onChange={(e) => setStudentId(e.target.value)}
            placeholder="Enter student ID"
            disabled={loading}
          />
        </div>
        <button
          type="submit"
          className="btn-add"
          disabled={loading}
        >
          {loading ? 'Adding...' : 'Add Member'}
        </button>
      </form>
    </div>
  );
}

export default AddGroupMember;
