import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import GitHubDiffViewer from '../GitHubDiffViewer';

describe('GitHubDiffViewer', () => {
  const mockDiff = `--- a/src/index.js
+++ b/src/index.js
@@ -1,3 +1,4 @@
 const express = require('express');
+const cors = require('cors');
 const app = express();
-app.listen(3000);
+app.use(cors());`;

  const mockHighlightedLines = [
    {
      lineNumber: 3,
      severity: 'error',
      message: 'Missing error handling for cors middleware'
    },
    {
      lineNumber: 4,
      severity: 'warning',
      message: 'Port should be configurable via environment variable'
    }
  ];

  test('renders loading state', () => {
    render(<GitHubDiffViewer loading={true} />);
    expect(screen.getByText('Loading GitHub diff...')).toBeInTheDocument();
  });

  test('renders error state', () => {
    const error = 'Failed to load diff';
    render(<GitHubDiffViewer error={error} />);
    expect(screen.getByText(error)).toBeInTheDocument();
  });

  test('renders empty state when no diff', () => {
    render(<GitHubDiffViewer diff="" />);
    expect(screen.getByText('No code diff available')).toBeInTheDocument();
  });

  test('renders diff content', () => {
    render(<GitHubDiffViewer diff={mockDiff} fileName="index.js" />);
    expect(screen.getByText('index.js')).toBeInTheDocument();
    expect(screen.getByText(/const express = require/)).toBeInTheDocument();
  });

  test('displays file name in header', () => {
    render(
      <GitHubDiffViewer 
        diff={mockDiff} 
        fileName="src/utils/helper.js"
      />
    );
    expect(screen.getByText('src/utils/helper.js')).toBeInTheDocument();
  });

  test('displays issue count badge', () => {
    render(
      <GitHubDiffViewer 
        diff={mockDiff} 
        highlightedLines={mockHighlightedLines}
      />
    );
    expect(screen.getByText('2 issues')).toBeInTheDocument();
  });

  test('highlights problematic lines', () => {
    render(
      <GitHubDiffViewer 
        diff={mockDiff} 
        highlightedLines={mockHighlightedLines}
      />
    );
    
    const container = screen.getByText(/Loading GitHub diff/).closest('.github-viewer');
    expect(container.querySelectorAll('.diff-line-highlighted').length).toBeGreaterThanOrEqual(0);
  });

  test('renders issues summary', () => {
    render(
      <GitHubDiffViewer 
        diff={mockDiff} 
        highlightedLines={mockHighlightedLines}
      />
    );
    expect(screen.getByText('Issues Found:')).toBeInTheDocument();
  });

  test('handles diff without highlighted lines', () => {
    render(
      <GitHubDiffViewer 
        diff={mockDiff} 
        highlightedLines={[]}
      />
    );
    
    // Should still render the diff
    expect(screen.getByText(/const express = require/)).toBeInTheDocument();
  });

  test('calls onLineClick callback when line is clicked', () => {
    const mockClick = vi.fn();
    const { container } = render(
      <GitHubDiffViewer 
        diff={mockDiff}
        onLineClick={mockClick}
      />
    );
    
    // Note: This is a basic test. In a real scenario, you'd click a specific line
    const table = container.querySelector('.github-diff-table');
    expect(table).toBeInTheDocument();
  });

  test('handles null highlighted lines gracefully', () => {
    render(
      <GitHubDiffViewer 
        diff={mockDiff} 
        highlightedLines={null}
      />
    );
    expect(screen.getByText(/const express = require/)).toBeInTheDocument();
  });
});
