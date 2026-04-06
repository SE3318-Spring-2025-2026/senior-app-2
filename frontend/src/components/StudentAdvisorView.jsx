import { useState } from 'react';

function StudentAdvisorView({ groupId }) {
  const [selectedProf, setSelectedProf] = useState('');

  const professors = [
    { id: 'prof-404', name: 'Dr. Ahmet (Software Eng.)' },
    { id: 'prof-505', name: 'Dr. Ayse (Computer Eng.)' }
  ];

  const sendRequest = async () => {
    if (!selectedProf) {
      alert("Please select a professor first!");
      return;
    }
    try {
      const response = await fetch(`http://localhost:3000/api/groups/${groupId}/advisor-requests`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ professorId: selectedProf })
      });
      if (response.status === 201) alert("Success! Request sent to the professor.");
      else alert("Something went wrong or request already pending.");
    } catch (error) {
      alert("Backend is unreachable, but frontend logic works!");
    }
  };

  return (
    <div style={{ backgroundColor: '#ffffff', padding: '30px', borderRadius: '16px', boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1)', marginBottom: '30px' }}>
      <h3 style={{ marginTop: '0', color: '#111827', borderBottom: '2px solid #e5e7eb', paddingBottom: '10px' }}>
        🎓 Advisor Selection
      </h3>
      
      <div style={{ marginTop: '20px' }}>
        <label style={{ display: 'block', fontSize: '14px', color: '#4b5563', marginBottom: '8px', fontWeight: '500' }}>Select Professor</label>
        <select 
          value={selectedProf} 
          onChange={(e) => setSelectedProf(e.target.value)}
          style={{ width: '100%', padding: '12px', borderRadius: '8px', border: '1px solid #d1d5db', outline: 'none', fontSize: '15px', backgroundColor: '#f9fafb' }}
        >
          <option value="">Choose from the list...</option>
          {professors.map((prof) => (
            <option key={prof.id} value={prof.id}>{prof.name}</option>
          ))}
        </select>
      </div>

      <button 
        onClick={sendRequest} 
        style={{ width: '100%', padding: '12px', marginTop: '20px', backgroundColor: '#4f46e5', color: 'white', border: 'none', borderRadius: '8px', fontSize: '16px', fontWeight: '600', cursor: 'pointer', transition: 'background-color 0.2s' }}
      >
        Send Request
      </button>
    </div>
  );
}

export default StudentAdvisorView;