import React, { useState } from 'react';
import { submitGrade } from '../services/api';

const GradingForm = ({ submissionId, rubricItems, onGraded, canGrade = true }) => {
  const [grades, setGrades] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleGradeChange = (rubricId, value) => {
    setGrades((prev) => ({
      ...prev,
      [rubricId]: value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      // Create independent grade request for each rubric evaluated
      for (const rubricId of Object.keys(grades)) {
        const gradeValue = parseFloat(grades[rubricId]);
        if (!isNaN(gradeValue)) {
          await submitGrade(submissionId, parseInt(rubricId, 10), gradeValue);
        }
      }
      setSuccess('Grades submitted successfully!');
      if (onGraded) {
        onGraded();
      }
    } catch (err) {
      setError(err.message || 'Failed to submit grades');
    } finally {
      setLoading(false);
    }
  };

  if (!rubricItems || rubricItems.length === 0) {
    return <div>No rubric available for grading.</div>;
  }

  return (
    <div style={{ padding: '20px', border: '1px solid #e0e0e0', borderRadius: '8px', maxWidth: '600px' }}>
      <h3>Deliverable Grading</h3>
      {!canGrade && <div style={{ color: '#b45309', marginBottom: '10px' }}>You can view this submission but cannot grade this group.</div>}
      {error && <div style={{ color: 'red', marginBottom: '10px' }}>{error}</div>}
      {success && <div style={{ color: 'green', marginBottom: '10px' }}>{success}</div>}
      
      <form onSubmit={handleSubmit}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
          {rubricItems.map((item) => (
            <div key={item.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <label>
                <strong>{item.criteria || `Rubric ${item.id}`}</strong>
                {item.maxScore && <span style={{ marginLeft: '10px', fontSize: '0.9em', color: '#666' }}>(Max: {item.maxScore})</span>}
              </label>
              <input
                type="number"
                min="0"
                max={item.maxScore || 100}
                step="0.5"
                value={grades[item.id] || ''}
                onChange={(e) => handleGradeChange(item.id, e.target.value)}
                placeholder="Score"
                required
                disabled={!canGrade}
                style={{ padding: '8px', width: '100px', borderRadius: '4px', border: '1px solid #ccc' }}
              />
            </div>
          ))}
        </div>
        <button
          type="submit"
          disabled={!canGrade || loading || Object.keys(grades).length === 0}
          style={{
            marginTop: '20px',
            padding: '10px 15px',
            backgroundColor: '#007bff',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: loading ? 'not-allowed' : 'pointer'
          }}
        >
          {loading ? 'Submitting...' : 'Submit Grades'}
        </button>
      </form>
    </div>
  );
};

export default GradingForm;
