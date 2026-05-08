import { useMemo } from 'react';
import './MarkdownRenderer.css';

/**
 * Simple Markdown renderer for AI feedback comments
 * Supports: headings, bold, italic, code, links, lists
 */
function MarkdownRenderer({ content, className = '' }) {
  const html = useMemo(() => {
    if (!content) return '';

    let text = content;

    // Escape HTML characters first (but not markdown syntax)
    text = text.replace(/[&<>"]/g, (char) => {
      const escapeMap = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' };
      return escapeMap[char];
    });

    // Headers (##, ###, ####)
    text = text.replace(/^#### (.*?)$/gm, '<h4>$1</h4>');
    text = text.replace(/^### (.*?)$/gm, '<h3>$1</h3>');
    text = text.replace(/^## (.*?)$/gm, '<h2>$1</h2>');
    text = text.replace(/^# (.*?)$/gm, '<h1>$1</h1>');

    // Inline code
    text = text.replace(/`([^`]+)`/g, '<code>$1</code>');

    // Bold (**text**)
    text = text.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    text = text.replace(/__([^_]+)__/g, '<strong>$1</strong>');

    // Italic (*text* or _text_)
    text = text.replace(/\*([^*]+)\*/g, '<em>$1</em>');
    text = text.replace(/_([^_]+)_/g, '<em>$1</em>');

    // Links [text](url)
    text = text.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>');

    // Line breaks (double newline = paragraph)
    text = text.replace(/\n\n+/g, '</p><p>');
    text = `<p>${text}</p>`;

    // Single line breaks
    text = text.replace(/\n/g, '<br />');

    // Unordered lists
    text = text.replace(/^- (.*?)$/gm, '<li>$1</li>');
    text = text.replace(/(<li>.*?<\/li>)/s, '<ul>$1</ul>');

    return text;
  }, [content]);

  return (
    <div
      className={`markdown-renderer ${className}`}
      dangerouslySetInnerHTML={{ __html: html }}
    />
  );
}

export default MarkdownRenderer;
