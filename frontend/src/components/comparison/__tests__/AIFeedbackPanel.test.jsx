import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import AIFeedbackPanel from '../AIFeedbackPanel';

describe('AIFeedbackPanel', () => {
  const mockFeedback = [
    {
      id: 'f1',
      lineNumber: 10,
      severity: 'error',
      title: 'Potential null pointer exception',
      message: 'Variable `data` might be null. Check before accessing properties.',
      suggestion: 'if (data && data.property) { ... }'
    },
    {
      id: 'f2',
      lineNumber: 15,
      severity: 'warning',
      title: 'Unused variable',
      message: 'Variable `temp` is assigned but never used.'
    },
    {
      id: 'f3',
      lineNumber: 22,
      severity: 'info',
      title: 'Code style improvement',
      message: 'Consider using const instead of let for immutable variables.'
    }
  ];

  test('renders loading state', () => {
    render(<AIFeedbackPanel loading={true} />);
    expect(screen.getByText('Loading AI feedback...')).toBeInTheDocument();
  });

  test('renders error state', () => {
    const error = 'Failed to load feedback';
    render(<AIFeedbackPanel error={error} />);
    expect(screen.getByText(error)).toBeInTheDocument();
  });

  test('renders collapsed by default', () => {
    const { container } = render(<AIFeedbackPanel feedback={mockFeedback} />);
    expect(container.querySelector('.ai-feedback-panel')).toHaveClass('collapsed');
  });

  test('shows feedback count in header', () => {
    render(<AIFeedbackPanel feedback={mockFeedback} />);
    expect(screen.getByText('3 issues found')).toBeInTheDocument();
  });

  test('displays severity stats', () => {
    render(<AIFeedbackPanel feedback={mockFeedback} />);
    
    const stats = screen.getAllByRole('generic'); // Stats are just spans
    const statTexts = stats.map(s => s.textContent);
    expect(statTexts.some(t => t.includes('1'))).toBe(true);
  });

  test('expands panel when header is clicked', async () => {
    const user = userEvent.setup();
    const { container } = render(<AIFeedbackPanel feedback={mockFeedback} />);
    
    const header = container.querySelector('.ai-feedback-header');
    await user.click(header);
    
    expect(container.querySelector('.ai-feedback-panel')).toHaveClass('expanded');
  });

  test('renders all feedback items when expanded', async () => {
    const user = userEvent.setup();
    const { container } = render(<AIFeedbackPanel feedback={mockFeedback} />);
    
    const header = container.querySelector('.ai-feedback-header');
    await user.click(header);
    
    expect(screen.getByText('Potential null pointer exception')).toBeInTheDocument();
    expect(screen.getByText('Unused variable')).toBeInTheDocument();
    expect(screen.getByText('Code style improvement')).toBeInTheDocument();
  });

  test('displays line numbers in feedback items', async () => {
    const user = userEvent.setup();
    const { container } = render(<AIFeedbackPanel feedback={mockFeedback} />);
    
    const header = container.querySelector('.ai-feedback-header');
    await user.click(header);
    
    expect(screen.getByText('Line 10')).toBeInTheDocument();
    expect(screen.getByText('Line 15')).toBeInTheDocument();
  });

  test('displays suggestions when provided', async () => {
    const user = userEvent.setup();
    const { container } = render(<AIFeedbackPanel feedback={mockFeedback} />);
    
    const header = container.querySelector('.ai-feedback-header');
    await user.click(header);
    
    expect(screen.getByText(/if \(data && data\.property\)/)).toBeInTheDocument();
  });

  test('renders empty state when no issues', async () => {
    const user = userEvent.setup();
    const { container } = render(<AIFeedbackPanel feedback={[]} />);
    
    const header = container.querySelector('.ai-feedback-header');
    await user.click(header);
    
    expect(screen.getByText('No issues found!')).toBeInTheDocument();
  });

  test('calls onFeedbackClick when feedback item is clicked', async () => {
    const user = userEvent.setup();
    const mockClick = vi.fn();
    const { container } = render(
      <AIFeedbackPanel 
        feedback={mockFeedback}
        onFeedbackClick={mockClick}
      />
    );
    
    const header = container.querySelector('.ai-feedback-header');
    await user.click(header);
    
    const firstItem = container.querySelector('.ai-feedback-item');
    await user.click(firstItem);
    
    expect(mockClick).toHaveBeenCalledWith(mockFeedback[0]);
  });

  test('sorts feedback by severity', () => {
    const unsortedFeedback = [
      { id: '1', severity: 'info', title: 'Info', message: 'Info msg' },
      { id: '2', severity: 'error', title: 'Error', message: 'Error msg' },
      { id: '3', severity: 'warning', title: 'Warning', message: 'Warning msg' },
    ];
    
    render(<AIFeedbackPanel feedback={unsortedFeedback} />);
    
    // Stats should reflect correct counts
    expect(screen.getByText('1 issue found')).toBeInTheDocument();
  });

  test('handles feedback without optional fields', async () => {
    const user = userEvent.setup();
    const minimalFeedback = [
      { id: '1', message: 'Something is wrong' }
    ];
    
    const { container } = render(<AIFeedbackPanel feedback={minimalFeedback} />);
    
    const header = container.querySelector('.ai-feedback-header');
    await user.click(header);
    
    expect(screen.getByText('Something is wrong')).toBeInTheDocument();
  });

  test('collapses panel on double header click', async () => {
    const user = userEvent.setup();
    const { container } = render(<AIFeedbackPanel feedback={mockFeedback} />);
    
    const header = container.querySelector('.ai-feedback-header');
    
    // Expand
    await user.click(header);
    expect(container.querySelector('.ai-feedback-panel')).toHaveClass('expanded');
    
    // Collapse
    await user.click(header);
    expect(container.querySelector('.ai-feedback-panel')).toHaveClass('collapsed');
  });
});
