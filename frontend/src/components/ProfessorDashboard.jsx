import { useState, useEffect } from 'react';
import { respondToAdvisorInvite } from '../services/api';

function ProfessorDashboard() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchRequests();
  }, []);

  const fetchRequests = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/advisor-requests/my-requests', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      const data = await response.json();
      setRequests(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error("Failed to fetch requests");
    } finally {
      setLoading(false);
    }
  };

  const handleDecision = async (requestId, decision) => {
    try {
      await respondToAdvisorInvite(requestId, decision);
      alert(`Request ${decision === 'approve' ? 'approved' : 'declined'} successfully!`);
      setRequests(requests.filter(req => req.id !== requestId));
    } catch (error) {
      alert(error.message || 'Action failed');
    }
  };

  if (loading) return <p>Loading requests...</p>;

  return (
    <div style={{ backgroundColor: '#ffffff', padding: '30px', borderRadius: '16px', boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1)', marginBottom: '30px' }}>
      <h3 style={{ marginTop: '0', color: '#111827', borderBottom: '2px solid #e5e7eb', paddingBottom: '10px' }}>
        👨‍🏫 Pending Advisor Requests
      </h3>
      <p style={{ fontSize: '13px', color: '#6b7280', marginBottom: '20px' }}>
        Requests from various project committees you are assigned to.
      </p>
      
      {requests.length === 0 ? (
        <p style={{ color: '#6b7280', fontStyle: 'italic' }}>No pending requests at the moment.</p>
      ) : (
        requests.map((req) => (
          <div key={req.id} style={{ backgroundColor: '#f8fafc', border: '1px solid #e2e8f0', padding: '20px', borderRadius: '12px', marginTop: '10px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div>
                <h4 style={{ margin: '0 0 5px 0', color: '#0f172a', fontSize: '18px' }}>{req.group?.groupName || 'Unknown Group'}</h4>
                <p style={{ margin: '0 0 8px 0', color: '#4338ca', fontSize: '13px', fontWeight: '600' }}>
                  📚 Project ID: {req.group?.id}
                </p>
                <p style={{ margin: '0', color: '#64748b', fontSize: '14px' }}>
                  <strong>Status:</strong> {req.status}
                </p>
              </div>
            </div>
            
            <div style={{ display: 'flex', gap: '12px', marginTop: '20px' }}>
              <button 
                onClick={() => handleDecision(req.id, 'approve')}
                style={{ flex: 1, padding: '10px', backgroundColor: '#10b981', color: 'white', border: 'none', borderRadius: '6px', fontWeight: '600', cursor: 'pointer' }}
              >
                ✓ Approve
              </button>
              <button 
                onClick={() => handleDecision(req.id, 'decline')}
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