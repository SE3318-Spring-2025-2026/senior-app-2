import { useState, useMemo } from 'react';
import MarkdownRenderer from './MarkdownRenderer';
import './AIFeedbackPanel.css';

/**
 * AIFeedbackPanel - Displays AI-generated review feedback
 *
 * Props:
 * - feedback: Array of { id, lineNumber, severity, title, message, suggestion }
 * - loading: Boolean to show loading state
 * - error: String error message if any
 * - onFeedbackClick: Callback when feedback item is clicked
 */
function AIFeedbackPanel({ feedback = [], loading = false, error = null, onFeedbackClick = null }) {
  const [expanded, setExpanded] = useState(false);
  const [selectedFeedbackId, setSelectedFeedbackId] = useState(null);

  const severityStats = useMemo(() => {
    const stats = { error: 0, warning: 0, info: 0 };
    feedback.forEach((item) => {
      const severity = item.severity || 'info';
      if (severity in stats) {
        stats[severity]++;
      }
    });
    return stats;
  }, [feedback]);

  const sortedFeedback = useMemo(() => {
    return [...feedback].sort((a, b) => {
      const severityOrder = { error: 0, warning: 1, info: 2 };
      const severityA = severityOrder[a.severity] ?? 3;
      const severityB = severityOrder[b.severity] ?? 3;
      return severityA - severityB;
    });
  }, [feedback]);

  const handleFeedbackClick = (item) => {
    setSelectedFeedbackId(item.id);
    if (onFeedbackClick) {
      onFeedbackClick(item);
    }
  };

  const toggleExpanded = () => {
    setExpanded(!expanded);
  };

  if (loading) {
    return (
      <div className="ai-feedback-panel ai-feedback-loading">
        <div className="ai-feedback-spinner" />
        <p>Loading AI feedback...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="ai-feedback-panel ai-feedback-error">
        <div className="ai-feedback-error-icon">⚠️</div>
        <p>{error}</p>
      </div>
    );
  }

  return (
    <div className={`ai-feedback-panel ${expanded ? 'expanded' : 'collapsed'}`}>
      {/* Header */}
      <div className="ai-feedback-header" onClick={toggleExpanded}>
        <div className="ai-feedback-icon">🤖</div>
        <div className="ai-feedback-title-section">
          <h3 className="ai-feedback-title">AI Review Feedback</h3>
          <p className="ai-feedback-subtitle">
            {feedback.length} issue{feedback.length !== 1 ? 's' : ''} found
          </p>
        </div>
        <div className="ai-feedback-stats">
          {severityStats.error > 0 && (
            <span className="ai-stat ai-stat-error">{severityStats.error}</span>
          )}
          {severityStats.warning > 0 && (
            <span className="ai-stat ai-stat-warning">{severityStats.warning}</span>
          )}
          {severityStats.info > 0 && (
            <span className="ai-stat ai-stat-info">{severityStats.info}</span>
          )}
        </div>
        <button className="ai-feedback-toggle">
          {expanded ? '▼' : '▶'}
        </button>
      </div>

      {/* Content */}
      {expanded && (
        <div className="ai-feedback-content">
          {feedback.length === 0 ? (
            <div className="ai-feedback-empty">
              <div className="ai-feedback-empty-icon">✅</div>
              <p>No issues found! Code looks good.</p>
            </div>
          ) : (
            <div className="ai-feedback-list">
              {sortedFeedback.map((item) => (
                <div
                  key={item.id}
                  className={`ai-feedback-item ai-feedback-${item.severity || 'info'} ${
                    selectedFeedbackId === item.id ? 'selected' : ''
                  }`}
                  onClick={() => handleFeedbackClick(item)}
                >
                  {/* Item header */}
                  <div className="ai-feedback-item-header">
                    <span className="ai-feedback-severity-badge">
                      {item.severity === 'error' && '🔴'}
                      {item.severity === 'warning' && '🟠'}
                      {item.severity === 'info' && '🔵'}
                    </span>
                    <div className="ai-feedback-item-title-section">
                      <h4 className="ai-feedback-item-title">{item.title || 'Issue'}</h4>
                      {item.lineNumber && (
                        <span className="ai-feedback-line-num">Line {item.lineNumber}</span>
                      )}
                    </div>
                  </div>

                  {/* Item message */}
                  <div className="ai-feedback-item-message">
                    <MarkdownRenderer content={item.message} />
                  </div>

                  {/* Suggestion */}
                  {item.suggestion && (
                    <div className="ai-feedback-suggestion">
                      <div className="ai-feedback-suggestion-title">💡 Suggestion:</div>
                      <div className="ai-feedback-suggestion-content">
                        <code>{item.suggestion}</code>
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default AIFeedbackPanel;
