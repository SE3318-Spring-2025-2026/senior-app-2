/**
 * @deprecated Prefer InspectorRubricGradePanel with project detail rubrics (includes id).
 * Thin adapter: rubricItems should include id; grader is taken from JWT on the server.
 */
import React from 'react';
import InspectorRubricGradePanel from './InspectorRubricGradePanel';
import './GradingForm.css';

export default function GradingForm({ submissionId, rubricItems, onGraded }) {
  const rubrics = (rubricItems || []).map((item) => ({
    id: item.id,
    title: item.criteria || item.title || (item.id != null ? `Rubric ${item.id}` : ''),
    criteriaType: item.maxScore != null ? `En fazla ${item.maxScore} puan` : undefined,
  }));

  return (
    <div className="grading-container">
      <InspectorRubricGradePanel
        mode="submission"
        submissionId={submissionId}
        rubrics={rubrics}
        onSaved={onGraded}
      />
    </div>
  );
}
