import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getUsers, changeUserRole, registerStaff } from '../services/api';
import './Users.css';

const ROLES = ['ADMIN', 'COORDINATOR', 'PROFESSOR', 'STUDENT'];

function Users() {
  const navigate = useNavigate();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [newEmail, setNewEmail] = useState('');
  const [newName, setNewName] = useState('');
  const [newRole, setNewRole] = useState('PROFESSOR');
  const [addError, setAddError] = useState('');
  const [addLoading, setAddLoading] = useState(false);

  useEffect(() => {
    fetchUsers();
  }, []);

  async function fetchUsers() {
    try {
      const data = await getUsers();
      setUsers(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleRoleChange(userId, role) {
    try {
      const updated = await changeUserRole(userId, role);
      setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleAddUser(e) {
    e.preventDefault();
    setAddError('');
    setAddLoading(true);
    try {
      await registerStaff(newEmail, newName, newRole);
      setNewEmail('');
      setNewName('');
      setNewRole('PROFESSOR');
      await fetchUsers();
    } catch (err) {
      setAddError(err.message);
    } finally {
      setAddLoading(false);
    }
  }

  // Sends a password reset link from the admin panel
  async function handleSendResetLink(accountId) {
    try {
      const response = await fetch('http://localhost:8080/auth/reset-password/admin', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ accountId }),
      });

      if (!response.ok) {
        throw new Error('Failed to send reset link');
      }
      alert('Password reset link sent successfully!');
    } catch (err) {
      alert(err.message);
    }
  }

  if (loading) return <div className="users-loading">Loading users...</div>;

  return (
    <div className="users-page">
      <h1>User Management</h1>

      {error && <div className="users-error">{error}</div>}

      <div className="add-user-section">
        <h2>Add Staff Member</h2>
        {addError && <div className="users-error">{addError}</div>}
        <form className="add-user-form" onSubmit={handleAddUser}>
          <input
            type="email"
            placeholder="Email"
            value={newEmail}
            onChange={(e) => setNewEmail(e.target.value)}
            required
          />
          <input
            type="text"
            placeholder="Full Name"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            required
          />
          <select value={newRole} onChange={(e) => setNewRole(e.target.value)}>
            <option value="PROFESSOR">Professor</option>
            <option value="COORDINATOR">Coordinator</option>
            <option value="ADMIN">Admin</option>
          </select>
          <button type="submit" disabled={addLoading}>
            {addLoading ? 'Adding...' : 'Add User'}
          </button>
        </form>
      </div>

      <div className="users-table-wrapper">
        <table className="users-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Actions</th>
              <th style={{ width: 80 }} />
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td>{u.id}</td>
                <td>{u.fullName || '—'}</td>
                <td>{u.email}</td>
                <td>
                  <select
                    value={u.role}
                    onChange={(e) => handleRoleChange(u.id, e.target.value)}
                  >
                    {ROLES.map((r) => (
                      <option key={r} value={r}>{r}</option>
                    ))}
                  </select>
                </td>
                <td>
                  {/* Show the reset button only for staff members since students use GitHub OAuth */}
                  {u.role !== 'STUDENT' && (
                    <button 
                      onClick={() => handleSendResetLink(u.id)}
                      style={{
                        padding: '6px 12px',
                        fontSize: '12px',
                        backgroundColor: '#4f46e5',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer'
                      }}
                    >
                      Send Reset Link
                    </button>
                  )}
                </td>
                <td>
                  {u.role === 'STUDENT' && u.githubUsername && (
                    <button
                      onClick={() => navigate(`/panel/github-profile/${u.id}`)}
                      style={{
                        padding: '6px 12px',
                        fontSize: '12px',
                        backgroundColor: '#667eea',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer'
                      }}
                    >
                      GitHub
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default Users;