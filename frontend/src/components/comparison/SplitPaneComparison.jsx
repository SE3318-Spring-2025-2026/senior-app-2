import { useState, useRef, useEffect } from 'react';
import JiraRequirementViewer from './JiraRequirementViewer';
import GitHubDiffViewer from './GitHubDiffViewer';
import AIFeedbackPanel from './AIFeedbackPanel';
import './SplitPaneComparison.css';

/**
 * SplitPaneComparison - Main comparison UI component
 *
 * Features:
 * - Split-pane layout (left: requirements, right: code diff)
 * - Draggable divider to resize panes
 * - AI feedback panel at the bottom
 * - Line highlighting and synchronization
 * - Responsive design for desktop/tablet
 *
 * Props:
 * - requirement: Jira requirement object
 * - diff: GitHub diff content
 * - highlightedLines: Array of lines with AI feedback
 * - feedback: Array of AI feedback items
 * - loading: Boolean or object with section loading states
 * - error: String or object with section error states
 * - fileName: Name of the file being compared
 * - onLineClick: Callback when a line is clicked
 * - onFeedbackClick: Callback when feedback is clicked
 */
function SplitPaneComparison({
  requirement = null,
  diff = '',
  highlightedLines = [],
  feedback = [],
  loading = {},
  error = {},
  fileName = 'code.diff',
  onLineClick = null,
  onFeedbackClick = null,
}) {
  const [dividerPos, setDividerPos] = useState(50);
  const [isDragging, setIsDragging] = useState(false);
  const containerRef = useRef(null);
  const dividerRef = useRef(null);

  // Normalize loading and error states
  const loadingState = typeof loading === 'boolean' ? { all: loading } : loading;
  const errorState = typeof error === 'string' ? { all: error } : error;

  // Mouse move handler for divider dragging
  useEffect(() => {
    if (!isDragging) return;

    const handleMouseMove = (e) => {
      if (!containerRef.current) return;

      const container = containerRef.current;
      const rect = container.getBoundingClientRect();
      const newPos = Math.max(20, Math.min(80, ((e.clientX - rect.left) / rect.width) * 100));

      setDividerPos(newPos);
    };

    const handleMouseUp = () => {
      setIsDragging(false);
    };

    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
    };
  }, [isDragging]);

  const handleDividerMouseDown = () => {
    setIsDragging(true);
  };

  const handleFeedbackClick = (item) => {
    if (onFeedbackClick) {
      onFeedbackClick(item);
    }
  };

  return (
    <div className="split-pane-comparison" ref={containerRef}>
      {/* Left pane - Jira Requirement */}
      <div className="split-pane-left" style={{ flex: `0 0 ${dividerPos}%` }}>
        <JiraRequirementViewer
          requirement={requirement}
          loading={loadingState.requirement || loadingState.all}
          error={errorState.requirement || errorState.all}
        />
      </div>

      {/* Divider */}
      <div
        className={`split-pane-divider ${isDragging ? 'dragging' : ''}`}
        ref={dividerRef}
        onMouseDown={handleDividerMouseDown}
        title="Drag to resize panes"
      >
        <div className="divider-handle" />
      </div>

      {/* Right pane - Code & Feedback */}
      <div className="split-pane-right" style={{ flex: `0 0 ${100 - dividerPos}%` }}>
        {/* Top section - GitHub Diff */}
        <div className="split-pane-top">
          <GitHubDiffViewer
            diff={diff}
            highlightedLines={highlightedLines}
            fileName={fileName}
            loading={loadingState.diff || loadingState.all}
            error={errorState.diff || errorState.all}
            onLineClick={onLineClick}
          />
        </div>

        {/* Bottom section - AI Feedback */}
        <div className="split-pane-bottom">
          <AIFeedbackPanel
            feedback={feedback}
            loading={loadingState.feedback || loadingState.all}
            error={errorState.feedback || errorState.all}
            onFeedbackClick={handleFeedbackClick}
          />
        </div>
      </div>

      {/* Responsive overlay message for small screens */}
      <div className="responsive-message">
        <p>Resize your screen to see the split-pane comparison interface</p>
      </div>
    </div>
  );
}

export default SplitPaneComparison;
