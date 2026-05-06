import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import MarkdownRenderer from '../MarkdownRenderer';

describe('MarkdownRenderer', () => {
  test('renders markdown heading', () => {
    const content = '# Hello World';
    render(<MarkdownRenderer content={content} />);
    const heading = screen.getByRole('heading', { level: 1 });
    expect(heading).toBeInTheDocument();
    expect(heading).toHaveTextContent('Hello World');
  });

  test('renders markdown bold text', () => {
    const content = 'This is **bold** text';
    render(<MarkdownRenderer content={content} />);
    const strong = screen.getByText('bold');
    expect(strong.tagName).toBe('STRONG');
  });

  test('renders markdown italic text', () => {
    const content = 'This is *italic* text';
    render(<MarkdownRenderer content={content} />);
    const em = screen.getByText('italic');
    expect(em.tagName).toBe('EM');
  });

  test('renders inline code', () => {
    const content = 'Use `const x = 5` in your code';
    render(<MarkdownRenderer content={content} />);
    const code = screen.getByText('const x = 5');
    expect(code.tagName).toBe('CODE');
  });

  test('renders markdown links', () => {
    const content = '[Click here](https://example.com)';
    render(<MarkdownRenderer content={content} />);
    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', 'https://example.com');
    expect(link).toHaveAttribute('target', '_blank');
  });

  test('renders lists', () => {
    const content = '- Item 1\n- Item 2\n- Item 3';
    render(<MarkdownRenderer content={content} />);
    const listItems = screen.getAllByRole('listitem');
    expect(listItems).toHaveLength(3);
  });

  test('handles empty content gracefully', () => {
    const { container } = render(<MarkdownRenderer content="" />);
    expect(container.querySelector('.markdown-renderer')).toBeInTheDocument();
  });

  test('handles null content gracefully', () => {
    const { container } = render(<MarkdownRenderer content={null} />);
    expect(container.querySelector('.markdown-renderer')).toBeInTheDocument();
  });

  test('applies custom className', () => {
    const { container } = render(
      <MarkdownRenderer content="Test" className="custom-class" />
    );
    expect(container.querySelector('.markdown-renderer')).toHaveClass('custom-class');
  });

  test('renders complex markdown', () => {
    const content = `# Title
## Subtitle
**Bold** and *italic* text with \`code\`
- List item 1
- List item 2
[Link](https://example.com)`;
    
    render(<MarkdownRenderer content={content} />);
    
    expect(screen.getByRole('heading', { level: 1 })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2 })).toBeInTheDocument();
    expect(screen.getByRole('link')).toBeInTheDocument();
  });
});
