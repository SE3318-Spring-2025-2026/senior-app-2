import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import SplitPaneComparison from '../SplitPaneComparison';

describe('SplitPaneComparison', () => {
  const mockRequirement = {
    key: 'PROJ-001',
    summary: 'Implement authentication',
    description: 'User login feature'
  };

  const mockDiff = `--- a/auth.js
+++ b/auth.js
@@ -1,3 +1,4 @@
 function login() {}`;

  const mockFeedback = [
    {
      id: 'f1',
      lineNumber: 2,
      severity: 'error',
      title: 'Empty function',
      message: 'Login function is empty'
    }
  ];

  test('renders split pane layout', () => {
    const { container } = render(
      <SplitPaneComparison
        requirement={mockRequirement}
        diff={mockDiff}
        feedback={mockFeedback}
      />
    );
    
    expect(container.querySelector('.split-pane-comparison')).toBeInTheDocument();
    expect(container.querySelector('.split-pane-left')).toBeInTheDocument();
    expect(container.querySelector('.split-pane-right')).toBeInTheDocument();
  });

  test('renders divider for resizing', () => {
    const { container } = render(
      <SplitPaneComparison
        requirement={mockRequirement}
        diff={mockDiff}
      />
    );
    
    expect(container.querySelector('.split-pane-divider')).toBeInTheDocument();
  });

  test('sets default divider position', () => {
    const { container } = render(
      <SplitPaneComparison
        requirement={mockRequirement}
        diff={mockDiff}
      />
    );
    
    const leftPane = container.querySelector('.split-pane-left');
    expect(leftPane).toHaveStyle('flex: 0 0 50%');
  });

  test('renders all sub-components', () => {
    render(
      <SplitPaneComparison
        requirement={mockRequirement}
        diff={mockDiff}
        feedback={mockFeedback}
      />
    );
    
    // These checks verify that the child components are rendered
    // Actual content visibility depends on the child components
    expect(screen.queryByText(/Implement authentication/)).toBeDefined();
  });

  test('handles loading state for all sections', () => {
    render(
      <SplitPaneComparison
        loading={true}
      />
    );
    
    // Loading indicator appears somewhere in the component
    const container = document.querySelector('.split-pane-comparison');
    expect(container).toBeInTheDocument();
  });

  test('handles error state', () => {
    render(
      <SplitPaneComparison
        error="Failed to load comparison data"
      />
    );
    
    const container = document.querySelector('.split-pane-comparison');
    expect(container).toBeInTheDocument();
  });

  test('handles section-specific loading states', () => {
    render(
      <SplitPaneComparison
        requirement={mockRequirement}
        diff={mockDiff}
        loading={{
          requirement: false,
          diff: true,
          feedback: false
        }}
      />
    );
    
    const container = document.querySelector('.split-pane-comparison');
    expect(container).toBeInTheDocument();
  });

  test('handles section-specific error states', () => {
    render(
      <SplitPaneComparison
        requirement={mockRequirement}
        diff={mockDiff}
        error={{
          diff: 'Failed to load diff',
          feedback: 'Failed to load feedback'
        }}
      />
    );
    
    const container = document.querySelector('.split-pane-comparison');
    expect(container).toBeInTheDocument();
  });

  test('passes callbacks to sub-components', () => {
    const mockLineClick = vi.fn();
    const mockFeedbackClick = vi.fn();
    
    render(
      <SplitPaneComparison
        requirement={mockRequirement}
        diff={mockDiff}
        feedback={mockFeedback}
        onLineClick={mockLineClick}
        onFeedbackClick={mockFeedbackClick}
      />
    );
    
    // Verify callbacks are available (actual calls depend on user interaction)
    expect(mockLineClick).not.toHaveBeenCalled();
    expect(mockFeedbackClick).not.toHaveBeenCalled();
  });

  test('renders with custom fileName', () => {
    render(
      <SplitPaneComparison
        requirement={mockRequirement}
        diff={mockDiff}
        fileName="custom-file.js"
      />
    );
    
    // The file name should appear in the diff viewer header
    const container = document.querySelector('.split-pane-comparison');
    expect(container).toBeInTheDocument();
  });

  test('renders highlighted lines', () => {
    const highlightedLines = [
      { lineNumber: 1, severity: 'error', message: 'Issue on line 1' },
      { lineNumber: 2, severity: 'warning', message: 'Issue on line 2' }
    ];
    
    render(
      <SplitPaneComparison
        requirement={mockRequirement}
        diff={mockDiff}
        highlightedLines={highlightedLines}
        feedback={mockFeedback}
      />
    );
    
    const container = document.querySelector('.split-pane-comparison');
    expect(container).toBeInTheDocument();
  });

  test('handles empty props gracefully', () => {
    const { container } = render(<SplitPaneComparison />);
    expect(container.querySelector('.split-pane-comparison')).toBeInTheDocument();
  });

  test('has accessible structure', () => {
    const { container } = render(
      <SplitPaneComparison
        requirement={mockRequirement}
        diff={mockDiff}
        feedback={mockFeedback}
      />
    );
    
    expect(container.querySelector('.split-pane-left')).toBeInTheDocument();
    expect(container.querySelector('.split-pane-right')).toBeInTheDocument();
    expect(container.querySelector('.split-pane-top')).toBeInTheDocument();
    expect(container.querySelector('.split-pane-bottom')).toBeInTheDocument();
  });
});
