import { useState } from 'react';
import { uploadStudentWhitelist } from '../services/api';
import './Users.css';

function StudentManagement() {
  const [ids, setIds] = useState('');
  const [status, setStatus] = useState({ type: '', msg: '' });

  const handleUpload = async (e) => {
    e.preventDefault();
    const idArray = ids.split(/[\n,]+/).map(s => s.trim()).filter(s => s !== "");
    
    try {
      await uploadStudentWhitelist(idArray);
      setStatus({ type: 'success', msg: `${idArray.length} IDs uploaded successfully!` });
      setIds('');
    } catch (err) {
      setStatus({ type: 'error', msg: err.message || 'Upload failed.' });
    }
  };

  return (
    <div className="users-page">
      <h1>Coordinator: Student Whitelist</h1>
      <div className="add-user-section">
        <form onSubmit={handleUpload}>
          <textarea 
            style={{ width: '100%', minHeight: '150px', padding: '12px', borderRadius: '8px', border: '1px solid #ddd', marginBottom: '15px', display: 'block', fontFamily: 'inherit' }}
            placeholder="Enter Student IDs (e.g. 210101001, 210101002)..."
            value={ids}
            onChange={(e) => setIds(e.target.value)}
          />
          <button type="submit" className="login-button" style={{width: 'auto'}}>Save Valid IDs</button>
        </form>
        {status.msg && (
          <div className={status.type === 'success' ? 'role-badge' : 'users-error'} style={{marginTop: '15px'}}>
            {status.msg}
          </div>
        )}
      </div>
    </div>
  );
}

export default StudentManagement;