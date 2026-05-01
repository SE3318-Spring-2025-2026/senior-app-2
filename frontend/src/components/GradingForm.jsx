import React, { useEffect, useState } from 'react';
import { submitGrade } from '../services/api';

/**
 * @param {object} [myRubricGrades] — rubricId (string) -> grade you already submitted for this submission (read-only rows)
 */
const GradingForm = ({ submissionId, rubricItems, onGraded, canGrade = true, myRubricGrades = {} }) => {
  const [grades, setGrades] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    setGrades({});
    setError('');
    setSuccess('');
  }, [submissionId]);

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
      for (const rubricId of Object.keys(grades)) {
        if (myRubricGrades[rubricId] != null) continue;
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

  const hasNewGradeToSubmit = Object.keys(grades).some((k) => {
    if (myRubricGrades[k] != null) return false;
    const gradeValue = parseFloat(grades[k]);
    return !isNaN(gradeValue);
  });

  return (
    <div style={{ padding: '20px', border: '1px solid #e0e0e0', borderRadius: '8px', maxWidth: '600px' }}>
      <h3>Deliverable Grading</h3>
      {!canGrade && <div style={{ color: '#b45309', marginBottom: '10px' }}>You can view this submission but cannot grade this group.</div>}
      {error && <div style={{ color: 'red', marginBottom: '10px' }}>{error}</div>}
      {success && <div style={{ color: 'green', marginBottom: '10px' }}>{success}</div>}
      
      <form onSubmit={handleSubmit}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
          {rubricItems.map((item) => {
            const idKey = String(item.id);
            const already = myRubricGrades[idKey];
            const label = item.title || item.criteria || `Rubric ${item.id}`;
            const max = item.maxScore || 100;
            const locked = already != null && already !== '';
            return (
              <div key={item.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <label>
                  <strong>{label}</strong>
                  {max != null && <span style={{ marginLeft: '10px', fontSize: '0.9em', color: '#666' }}>(Max: {max})</span>}
                  {locked && <span style={{ marginLeft: '8px', fontSize: '0.85em', color: '#059669' }}>— your score saved</span>}
                </label>
                <input
                  type="number"
                  min="0"
                  max={max}
                  step="0.5"
                  value={locked ? String(already) : (grades[idKey] ?? '')}
                  onChange={(e) => handleGradeChange(idKey, e.target.value)}
                  placeholder="Score"
                  disabled={!canGrade || locked}
                  style={{ padding: '8px', width: '100px', borderRadius: '4px', border: '1px solid #ccc' }}
                />
              </div>
            );
          })}
        </div>
        <button
          type="submit"
          disabled={!canGrade || loading || !hasNewGradeToSubmit}
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
