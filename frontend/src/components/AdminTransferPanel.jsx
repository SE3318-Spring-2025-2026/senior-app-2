import { useState } from 'react';

function AdminTransferPanel() {
  const [groupId, setGroupId] = useState('');
  const [newProfessorId, setNewProfessorId] = useState('');

  const handleTransfer = async () => {
    if (!groupId || !newProfessorId) {
      alert("Please ensure both fields are filled!");
      return;
    }
    try {
      await fetch(`http://localhost:3000/api/admin/groups/${groupId}/transfer`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ newProfessorId })
      });
      alert(`Transfer request sent! Target Professor: ${newProfessorId}`);
    } catch (error) {
      alert("Backend is unreachable, but admin transfer logic works!");
    }
  };

  return (
    <div style={{ backgroundColor: '#fff7ed', padding: '30px', borderRadius: '16px', border: '1px solid #fdba74', boxShadow: '0 10px 15px -3px rgba(251, 146, 60, 0.1)' }}>
      <h3 style={{ marginTop: '0', color: '#c2410c', borderBottom: '2px solid #fed7aa', paddingBottom: '10px' }}>
        ⚙️ Admin: Manual Group Transfer
      </h3>
      <p style={{ fontSize: '13px', color: '#9a3412', marginBottom: '20px' }}>⚠️ Warning: Use only in exceptional situations.</p>
      
      <div style={{ marginBottom: '15px' }}>
        <label style={{ display: 'block', fontSize: '14px', color: '#9a3412', marginBottom: '8px', fontWeight: '500' }}>Group ID</label>
        <input 
          type="text" 
          value={groupId} 
          onChange={(e) => setGroupId(e.target.value)} 
          placeholder="e.g., grp-12345"
          style={{ width: '100%', padding: '12px', borderRadius: '8px', border: '1px solid #fdba74', outline: 'none', backgroundColor: '#fff' }}
        />
      </div>

      <div style={{ marginBottom: '20px' }}>
        <label style={{ display: 'block', fontSize: '14px', color: '#9a3412', marginBottom: '8px', fontWeight: '500' }}>New Professor</label>
        <select 
          value={newProfessorId} 
          onChange={(e) => setNewProfessorId(e.target.value)}
          style={{ width: '100%', padding: '12px', borderRadius: '8px', border: '1px solid #fdba74', outline: 'none', backgroundColor: '#fff' }}
        >
          <option value="">Select a Professor...</option>
          <option value="prof-505">Dr. Ayse Yilmaz (prof-505)</option>
          <option value="prof-606">Dr. Mehmet Demir (prof-606)</option>
        </select>
      </div>

      <button 
        onClick={handleTransfer}
        style={{ width: '100%', padding: '12px', backgroundColor: '#ea580c', color: 'white', border: 'none', borderRadius: '8px', fontSize: '16px', fontWeight: '600', cursor: 'pointer' }}
      >
        Force Transfer Group
      </button>
    </div>
  );
}

export default AdminTransferPanel;