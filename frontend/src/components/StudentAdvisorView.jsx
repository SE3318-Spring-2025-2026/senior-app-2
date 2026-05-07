import { useState, useEffect } from 'react';

function StudentAdvisorView({ groupId, projectId }) {
  const [selectedProf, setSelectedProf] = useState('');
  const [committeeProfs, setCommitteeProfs] = useState([]);

  // Sadece proje seçiliyse o projenin komitesini getir
  useEffect(() => {
    if (!projectId) return; 
    
    const fetchCommitteeProfs = () => {
      // API'den komite hocalarının geldiğini simüle ediyoruz
      const mockCommittee = [
        { id: 'prof-404', name: 'Dr. Ahmet (Committee Member)' },
        { id: 'prof-707', name: 'Dr. Zeynep (Committee Member)' }
      ];
      setCommitteeProfs(mockCommittee);
    };
    fetchCommitteeProfs();
  }, [projectId]);

  const sendRequest = async () => {
    if (!selectedProf) {
      alert("Please select a professor from the committee!");
      return;
    }
    try {
      const response = await fetch(`http://localhost:3000/api/groups/${groupId}/advisor-requests`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ professorId: selectedProf })
      });
      
      if (response.status === 201) alert("Success! Request sent to the professor.");
      else if (response.status === 403) alert("Error: This professor is not in your project's committee!");
      else if (response.status === 409) alert("Error: Your group is already in the assignment process.");
      else alert("Something went wrong.");
    } catch (error) {
      alert("Backend is unreachable, but filtered frontend logic works!");
    }
  };

  // KRİTİK GÜNCELLEME: Proje seçilmemişse ekranı kilitle!
  if (!projectId) {
    return (
      <div style={{ backgroundColor: '#fee2e2', padding: '20px', borderRadius: '12px', border: '1px solid #f87171', marginBottom: '30px' }}>
        <h3 style={{ marginTop: '0', color: '#991b1b', fontSize: '18px' }}>⚠️ Action Required</h3>
        <p style={{ color: '#7f1d1d', margin: 0, fontSize: '15px' }}>
          You must create a group and select a <strong>Project Template</strong> before you can send an advisor request.
        </p>
      </div>
    );
  }

  return (
    <div style={{ backgroundColor: '#ffffff', padding: '30px', borderRadius: '16px', boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1)', marginBottom: '30px' }}>
      <h3 style={{ marginTop: '0', color: '#111827', borderBottom: '2px solid #e5e7eb', paddingBottom: '10px' }}>
        🎓 Advisor Selection
      </h3>
      <div style={{ backgroundColor: '#e0e7ff', padding: '12px', borderRadius: '8px', marginBottom: '20px' }}>
        <p style={{ fontSize: '13px', color: '#3730a3', margin: 0, fontWeight: '600' }}>
          📌 Selected Project: {projectId}
        </p>
        <p style={{ fontSize: '12px', color: '#4338ca', margin: '4px 0 0 0' }}>
          Showing only the available professors for this project's committee.
        </p>
      </div>
      
      <div style={{ marginTop: '20px' }}>
        <label style={{ display: 'block', fontSize: '14px', color: '#4b5563', marginBottom: '8px', fontWeight: '500' }}>Select Committee Professor</label>
        <select 
          value={selectedProf} 
          onChange={(e) => setSelectedProf(e.target.value)}
          style={{ width: '100%', padding: '12px', borderRadius: '8px', border: '1px solid #d1d5db', outline: 'none', fontSize: '15px', backgroundColor: '#f9fafb' }}
        >
          <option value="">Choose from the list...</option>
          {committeeProfs.map((prof) => (
            <option key={prof.id} value={prof.id}>{prof.name}</option>
          ))}
        </select>
      </div>

      <button 
        onClick={sendRequest} 
        style={{ width: '100%', padding: '12px', marginTop: '20px', backgroundColor: '#4f46e5', color: 'white', border: 'none', borderRadius: '8px', fontSize: '16px', fontWeight: '600', cursor: 'pointer', transition: 'background-color 0.2s' }}
      >
        Send Advisee Request
      </button>
    </div>
  );
}

export default StudentAdvisorView;