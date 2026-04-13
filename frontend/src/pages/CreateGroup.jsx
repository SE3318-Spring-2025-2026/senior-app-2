import React, { useState } from 'react';

const CreateGroup = () => {
  const [groupName, setGroupName] = useState('');
  const [projectId, setProjectId] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    
    if (!groupName || !projectId) {
      setError("Please fill in all fields.");
      return;
    }

    setError('');
    console.log("Submitting Group Data:", { groupName, projectId });
    alert(`Group "${groupName}" has been created successfully!`);
  };

  return (
    <div style={{ 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      minHeight: '80vh',
      backgroundColor: '#f4f7fe' 
    }}>
      <div style={{ 
        backgroundColor: 'white', 
        padding: '30px', 
        borderRadius: '12px', 
        boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
        width: '100%',
        maxWidth: '400px'
      }}>
        <h2 style={{ color: '#4318FF', marginBottom: '20px', textAlign: 'center' }}>
          Group Creation Wizard
        </h2>

        {error && (
          <div style={{ color: 'red', marginBottom: '10px', fontSize: '14px', textAlign: 'center' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
          
          <div>
            <label style={{ fontWeight: '600', display: 'block', marginBottom: '5px' }}>Group Name</label>
            <input 
              type="text" 
              value={groupName} 
              onChange={(e) => setGroupName(e.target.value)} 
              placeholder="Enter group name..."
              required 
              style={{ 
                width: '100%', 
                padding: '12px', 
                borderRadius: '8px', 
                border: '1px solid #E0E5F2',
                outline: 'none'
              }}
            />
          </div>

          <div>
            <label style={{ fontWeight: '600', display: 'block', marginBottom: '5px' }}>Project Template</label>
            <select 
              value={projectId} 
              onChange={(e) => setProjectId(e.target.value)} 
              required
              style={{ 
                width: '100%', 
                padding: '12px', 
                borderRadius: '8px', 
                border: '1px solid #E0E5F2',
                backgroundColor: 'white'
              }}
            >
              <option value="">Select a template...</option>
              <option value="1">Software Engineering Project</option>
              <option value="2">Data Analysis Template</option>
              <option value="3">Mobile App Framework</option>
            </select>
          </div>

          <button 
            type="submit" 
            style={{ 
              padding: '12px', 
              cursor: 'pointer', 
              backgroundColor: '#4318FF', 
              color: 'white', 
              border: 'none', 
              borderRadius: '8px',
              fontWeight: 'bold',
              marginTop: '10px',
              transition: 'background 0.3s'
            }}
            onMouseOver={(e) => e.target.style.backgroundColor = '#3311CC'}
            onMouseOut={(e) => e.target.style.backgroundColor = '#4318FF'}
          >
            Create Group
          </button>
        </form>
      </div>
    </div>
  );
};

export default CreateGroup;