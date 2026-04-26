import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import SplitPaneComparison from '../components/comparison/SplitPaneComparison';
import { getComparisonData, getAIFeedback } from '../services/api';
import { useAuth } from '../context/AuthContext';
import './CodeReviewComparison.css';

/**
 * CodeReviewComparison - Page for comparing Jira requirements with GitHub code changes
 *
 * Features:
 * - Loads comparison data from backend
 * - Displays Jira requirements vs. GitHub diff
 * - Shows AI-generated feedback with severity levels
 * - Synchronizes highlighting between sections
 * - Responsive design for all screen sizes
 */
function CodeReviewComparison() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [requirement, setRequirement] = useState(null);
  const [diff, setDiff] = useState('');
  const [feedback, setFeedback] = useState([]);
  const [highlightedLines, setHighlightedLines] = useState([]);

  const [loading, setLoading] = useState({
    requirement: true,
    diff: true,
    feedback: true,
  });

  const [error, setError] = useState({
    requirement: null,
    diff: null,
    feedback: null,
  });

  // Load comparison data on component mount
  useEffect(() => {
    if (!projectId) return;

    const loadData = async () => {
      try {
        // Load main comparison data
        const comparisonData = await getComparisonData(projectId);

        if (comparisonData) {
          setRequirement(comparisonData.requirement || null);
          setDiff(comparisonData.diff || '');
          setFeedback(comparisonData.feedback || []);
          setHighlightedLines(comparisonData.highlightedLines || []);

          setLoading((prev) => ({
            ...prev,
            requirement: false,
            diff: false,
            feedback: false,
          }));
        }
      } catch (err) {
        const errorMsg = err?.message || 'Failed to load comparison data';
        setError({
          requirement: errorMsg,
          diff: errorMsg,
          feedback: errorMsg,
        });

        setLoading({
          requirement: false,
          diff: false,
          feedback: false,
        });
      }
    };

    loadData();
  }, [projectId]);

  const handleLineClick = (lineNumber, lineContent) => {
    console.log(`Line ${lineNumber} clicked:`, lineContent);

    // Scroll feedback panel to show related feedback
    const feedbackItem = feedback.find((f) => f.lineNumber === lineNumber);
    if (feedbackItem) {
      const feedbackEl = document.querySelector(
        `[data-feedback-id="${feedbackItem.id}"]`
      );
      if (feedbackEl) {
        feedbackEl.scrollIntoView({ behavior: 'smooth' });
      }
    }
  };

  const handleFeedbackClick = (feedbackItem) => {
    console.log('Feedback item clicked:', feedbackItem);

    // In a real app, you could open a dialog or highlight the related code line
    // For now, we just log it
  };

  // Determine file name from project or use default
  const fileName = projectId ? `project-${projectId}.diff` : 'select-project.diff';

  // Show project selection if no projectId
  if (!projectId) {
    return (
      <div className="code-review-comparison-page">
        <header className="review-header">
          <div className="review-header-content">
            <h1 className="review-title">Code Review</h1>
            <p className="review-subtitle">
              Compare Jira requirements with GitHub code changes
            </p>
          </div>
        </header>
        <div className="review-container">
          <div className="empty-state">
            <div className="empty-state-icon">📋</div>
            <h2>Select a Project</h2>
            <p>Please select a project from My Projects to start code review.</p>
            <button className="btn btn-primary" onClick={() => navigate('/panel/my-projects')}>
              Go to My Projects
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="code-review-comparison-page">
      {/* Header */}
      <header className="review-header">
        <div className="review-header-content">
          <h1 className="review-title">Code Review</h1>
          <p className="review-subtitle">
            Compare Jira requirements with GitHub code changes
          </p>
        </div>
        <div className="review-meta">
          <span className="review-user">{user?.fullName || 'User'}</span>
        </div>
      </header>

      {/* Comparison UI */}
      <div className="review-container">
        <SplitPaneComparison
          requirement={requirement}
          diff={diff}
          feedback={feedback}
          highlightedLines={highlightedLines}
          loading={loading}
          error={error}
          fileName={fileName}
          onLineClick={handleLineClick}
          onFeedbackClick={handleFeedbackClick}
        />
      </div>

      {/* Footer with actions */}
      <footer className="review-footer">
        <button className="btn btn-secondary">← Back</button>
        <div className="review-actions">
          <button className="btn btn-outline">Save Progress</button>
          <button className="btn btn-primary">Approve & Close</button>
        </div>
      </footer>
    </div>
  );
}

export default CodeReviewComparison;
