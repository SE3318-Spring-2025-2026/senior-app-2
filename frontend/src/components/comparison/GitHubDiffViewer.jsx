import { useMemo } from 'react';
import './GitHubDiffViewer.css';

/**
 * GitHubDiffViewer - Displays GitHub diff with AI feedback highlighting
 *
 * Props:
 * - diff: String containing the unified diff or code content
 * - highlightedLines: Array of { lineNumber, severity, message } to highlight
 * - fileName: String name of the file being displayed
 * - loading: Boolean to show loading state
 * - error: String error message if any
 * - onLineClick: Callback when a line is clicked with (lineNumber, lineContent)
 */
function GitHubDiffViewer({
  diff = '',
  highlightedLines = [],
  fileName = 'code.diff',
  loading = false,
  error = null,
  onLineClick = null,
}) {
  const lines = useMemo(() => {
    if (!diff) return [];
    return diff.split('\n');
  }, [diff]);

  const highlightMap = useMemo(() => {
    const map = {};
    highlightedLines.forEach((h) => {
      map[h.lineNumber] = h;
    });
    return map;
  }, [highlightedLines]);

  const getLineNumber = (index) => {
    let lineNum = 0;
    for (let i = 0; i <= index; i++) {
      const line = lines[i];
      if (line.startsWith('@@')) {
        // Parse line numbers from diff header like @@ -1,5 +2,6 @@
        const match = line.match(/@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@/);
        if (match) {
          lineNum = parseInt(match[1], 10) - 1;
        }
      } else if (!line.startsWith('---') && !line.startsWith('+++') && !line.startsWith('@@')) {
        if (line.startsWith('-')) {
          // Deleted line doesn't increment
        } else {
          lineNum++;
        }
      }
    }
    return lineNum;
  };

  const parsedLines = useMemo(() => {
    const result = [];
    let lineNum = 0;

    lines.forEach((line, idx) => {
      if (line.startsWith('@@')) {
        // Parse diff header
        const match = line.match(/@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@/);
        if (match) {
          lineNum = parseInt(match[1], 10) - 1;
        }
        result.push({
          type: 'header',
          content: line,
          lineNumber: null,
          highlight: null,
        });
      } else if (line.startsWith('---') || line.startsWith('+++')) {
        result.push({
          type: 'meta',
          content: line,
          lineNumber: null,
          highlight: null,
        });
      } else if (line.startsWith('-')) {
        result.push({
          type: 'delete',
          content: line.slice(1),
          lineNumber: null,
          highlight: null,
        });
      } else if (line.startsWith('+')) {
        lineNum++;
        const highlight = highlightMap[lineNum];
        result.push({
          type: 'add',
          content: line.slice(1),
          lineNumber: lineNum,
          highlight,
        });
      } else if (line.startsWith('\\')) {
        result.push({
          type: 'meta',
          content: line,
          lineNumber: null,
          highlight: null,
        });
      } else {
        lineNum++;
        const highlight = highlightMap[lineNum];
        result.push({
          type: 'context',
          content: line,
          lineNumber: lineNum,
          highlight,
        });
      }
    });

    return result;
  }, [lines, highlightMap]);

  if (loading) {
    return (
      <div className="github-viewer github-viewer-loading">
        <div className="github-loading-spinner" />
        <p>Loading GitHub diff...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="github-viewer github-viewer-error">
        <div className="github-error-icon">⚠️</div>
        <p>{error}</p>
      </div>
    );
  }

  if (!diff || diff.trim().length === 0) {
    return (
      <div className="github-viewer github-viewer-empty">
        <div className="github-empty-icon">📝</div>
        <p>No code diff available</p>
      </div>
    );
  }

  return (
    <div className="github-viewer">
      {/* Header */}
      <div className="github-header">
        <div className="github-file-icon">📄</div>
        <span className="github-file-name">{fileName}</span>
        <span className="github-file-badge">
          {highlightedLines.length} issue{highlightedLines.length !== 1 ? 's' : ''}
        </span>
      </div>

      {/* Diff content */}
      <div className="github-diff-container">
        <table className="github-diff-table">
          <tbody>
            {parsedLines.map((line, idx) => {
              const isHighlighted = line.highlight;
              const severity = line.highlight?.severity || 'info';

              return (
                <tr
                  key={idx}
                  className={`diff-line diff-line-${line.type} ${
                    isHighlighted ? `diff-line-highlighted diff-line-${severity}` : ''
                  }`}
                  onClick={() => {
                    if (onLineClick && line.lineNumber) {
                      onLineClick(line.lineNumber, line.content);
                    }
                  }}
                >
                  {/* Line numbers */}
                  <td className="diff-line-num diff-line-num-old">
                    {line.type === 'delete' ? '🗑' : ''}
                  </td>
                  <td className="diff-line-num diff-line-num-new">
                    {line.type === 'add' ? '➕' : line.type === 'context' ? line.lineNumber : ''}
                  </td>

                  {/* Line content */}
                  <td className="diff-line-content">
                    <code className="diff-code">{line.content}</code>
                  </td>

                  {/* Highlight indicator */}
                  {isHighlighted && (
                    <td className="diff-highlight-indicator">
                      <div className="diff-highlight-dot" title={line.highlight.message} />
                    </td>
                  )}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Highlighted lines summary */}
      {highlightedLines.length > 0 && (
        <div className="github-summary">
          <div className="github-summary-title">Issues Found:</div>
          <div className="github-summary-items">
            {highlightedLines.map((h, idx) => (
              <div key={idx} className={`github-summary-item github-summary-${h.severity}`}>
                <span className="github-summary-line">Line {h.lineNumber}</span>
                <span className="github-summary-msg">{h.message}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default GitHubDiffViewer;
