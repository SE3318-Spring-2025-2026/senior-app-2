import React, { useState } from 'react';
import { submitGrade } from '../services/api';
import './GradingForm.css';

const GradingForm = ({ submissionId, rubricItems, graderId, onGraded }) => {
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
          await submitGrade(submissionId, graderId, parseInt(rubricId), gradeValue);
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
    return <div className="grading-container">No rubric available for grading.</div>;
  }

  return (
    <div className="grading-container">
      <h3 className="grading-header">Deliverable Grading</h3>
      
      {error && <div className="grading-alert error">{error}</div>}
      {success && <div className="grading-alert success">{success}</div>}
      
      <form onSubmit={handleSubmit}>
        <div className="rubric-list">
          {rubricItems.map((item) => (
            <div key={item.id} className="rubric-item">
              <div className="rubric-label">
                <span className="rubric-title">{item.criteria || `Rubric ${item.id}`}</span>
                {item.maxScore && <span className="rubric-max">Maximum Allowed: {item.maxScore} pts</span>}
              </div>
              <input
                type="number"
                min="0"
                max={item.maxScore || 100}
                step="0.5"
                value={grades[item.id] || ''}
                onChange={(e) => handleGradeChange(item.id, e.target.value)}
                placeholder="Score"
                required
                className="rubric-input"
              />
            </div>
          ))}
        </div>
        <button
          type="submit"
          disabled={loading || Object.keys(grades).length === 0}
          className="submit-btn"
        >
          {loading ? 'Submitting Grades...' : 'Submit Grades'}
        </button>
      </form>
    </div>
  );
};

export default GradingForm;
