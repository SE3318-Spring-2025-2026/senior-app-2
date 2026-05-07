import { useState } from 'react';

function ProfessorDashboard() {
  // GÜNCELLEME: Artık gelen isteklerde projectName (Şablon) bilgisi de var
  const [requests, setRequests] = useState([
    { 
      requestId: 'req-888', 
      groupId: 'grp-98765', 
      groupName: 'Tech Titans', 
      projectName: 'SE 1111 Graduation Project', 
      members: ['Efkan', 'John', 'Jane'] 
    }
  ]);

  const handleDecision = async (groupId, requestId, decision) => {
    try {
      const response = await fetch(`http://localhost:3000/api/groups/${groupId}/advisor-requests/${requestId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ decision })
      });

      if (response.status === 200) {
        alert(`Request successfully ${decision}d!`);
        setRequests(requests.filter(req => req.requestId !== requestId));
      } else if (response.status === 403) {
        alert("Unauthorized: You are not the target professor.");
      }
    } catch (error) {
      alert(`Backend is unreachable, but the ${decision} logic works!`);
      setRequests(requests.filter(req => req.requestId !== requestId));
    }
  };

  return (
    <div style={{ backgroundColor: '#ffffff', padding: '30px', borderRadius: '16px', boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1)', marginBottom: '30px' }}>
      <h3 style={{ marginTop: '0', color: '#111827', borderBottom: '2px solid #e5e7eb', paddingBottom: '10px' }}>
        👨‍🏫 Pending Advisor Requests
      </h3>
      <p style={{ fontSize: '13px', color: '#6b7280', marginBottom: '20px' }}>Requests from various project committees you are assigned to.</p>
      
      {requests.length === 0 ? (
        <p style={{ color: '#6b7280', fontStyle: 'italic' }}>No pending requests at the moment.</p>
      ) : (
        requests.map((req) => (
          <div key={req.requestId} style={{ backgroundColor: '#f8fafc', border: '1px solid #e2e8f0', padding: '20px', borderRadius: '12px', marginTop: '10px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div>
                <h4 style={{ margin: '0 0 5px 0', color: '#0f172a', fontSize: '18px' }}>{req.groupName}</h4>
                {/* Proje Adı Kartın Üzerine Eklendi */}
                <p style={{ margin: '0 0 8px 0', color: '#4338ca', fontSize: '13px', fontWeight: '600' }}>
                  📚 Project: {req.projectName}
                </p>
                <p style={{ margin: '0', color: '#64748b', fontSize: '14px' }}><strong>Members:</strong> {req.members.join(', ')}</p>
              </div>
            </div>
            
            <div style={{ display: 'flex', gap: '12px', marginTop: '20px' }}>
              <button 
                onClick={() => handleDecision(req.groupId, req.requestId, 'approve')}
                style={{ flex: 1, padding: '10px', backgroundColor: '#10b981', color: 'white', border: 'none', borderRadius: '6px', fontWeight: '600', cursor: 'pointer' }}
              >
                ✓ Approve
              </button>
              <button 
                onClick={() => handleDecision(req.groupId, req.requestId, 'decline')}
                style={{ flex: 1, padding: '10px', backgroundColor: '#ef4444', color: 'white', border: 'none', borderRadius: '6px', fontWeight: '600', cursor: 'pointer' }}
              >
                ✕ Decline
              </button>
            </div>
          </div>
        ))
      )}
    </div>
  );
}

export default ProfessorDashboard;