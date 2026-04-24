import { useMemo } from 'react';
import './JiraRequirementViewer.css';

/**
 * JiraRequirementViewer - Displays Jira requirements in a readable format
 *
 * Props:
 * - requirement: Object with { id, key, summary, description, acceptanceCriteria }
 * - loading: Boolean to show loading state
 * - error: String error message if any
 */
function JiraRequirementViewer({ requirement, loading = false, error = null }) {
  const criteria = useMemo(() => {
    if (!requirement?.acceptanceCriteria) return [];
    
    if (Array.isArray(requirement.acceptanceCriteria)) {
      return requirement.acceptanceCriteria;
    }
    
    // If string, split by common delimiters
    if (typeof requirement.acceptanceCriteria === 'string') {
      return requirement.acceptanceCriteria
        .split(/\n|;|-/)
        .map((c) => c.trim())
        .filter((c) => c.length > 0);
    }
    
    return [];
  }, [requirement]);

  if (loading) {
    return (
      <div className="jira-viewer jira-viewer-loading">
        <div className="jira-loading-spinner" />
        <p>Loading Jira requirement...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="jira-viewer jira-viewer-error">
        <div className="jira-error-icon">⚠️</div>
        <p>{error}</p>
      </div>
    );
  }

  if (!requirement) {
    return (
      <div className="jira-viewer jira-viewer-empty">
        <div className="jira-empty-icon">📋</div>
        <p>No requirement selected</p>
      </div>
    );
  }

  return (
    <div className="jira-viewer">
      {/* Header */}
      <div className="jira-header">
        <div className="jira-badge">{requirement.key || 'REQ'}</div>
        <h2 className="jira-title">{requirement.summary || 'Requirement'}</h2>
      </div>

      {/* Description */}
      {requirement.description && (
        <div className="jira-section">
          <h3 className="jira-section-title">Description</h3>
          <div className="jira-description">{requirement.description}</div>
        </div>
      )}

      {/* Acceptance Criteria */}
      {criteria.length > 0 && (
        <div className="jira-section">
          <h3 className="jira-section-title">Acceptance Criteria</h3>
          <ul className="jira-criteria-list">
            {criteria.map((criterion, idx) => (
              <li key={idx} className="jira-criterion">
                <span className="jira-criterion-icon">✓</span>
                {criterion}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Additional Info */}
      {(requirement.priority || requirement.status || requirement.assignee) && (
        <div className="jira-meta">
          {requirement.priority && (
            <div className="jira-meta-item">
              <span className="jira-meta-label">Priority:</span>
              <span className="jira-meta-value">{requirement.priority}</span>
            </div>
          )}
          {requirement.status && (
            <div className="jira-meta-item">
              <span className="jira-meta-label">Status:</span>
              <span className="jira-meta-value">{requirement.status}</span>
            </div>
          )}
          {requirement.assignee && (
            <div className="jira-meta-item">
              <span className="jira-meta-label">Assignee:</span>
              <span className="jira-meta-value">{requirement.assignee}</span>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default JiraRequirementViewer;
