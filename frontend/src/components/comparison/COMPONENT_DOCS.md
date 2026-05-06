# Comparison UI Components - Documentation

## Overview

The Comparison UI component system provides a comprehensive solution for reviewing code changes against Jira requirements with AI-powered feedback. It features a split-pane layout, synchronized highlighting, and responsive design for desktop and tablet devices.

### Features

- 📋 **Split-Pane Layout**: Side-by-side view of requirements and code
- 🎯 **Intelligent Highlighting**: Line-level highlighting based on AI feedback
- 📝 **Markdown Support**: Rich text rendering for AI comments
- 🤖 **AI Feedback Panel**: Collapsible panel with severity-sorted feedback
- ⚡ **Responsive Design**: Works on desktop, tablet, and mobile
- ♿ **Accessible**: Semantic HTML and keyboard navigation support
- 🎨 **Customizable**: Props-based configuration for all components

---

## Components

### 1. SplitPaneComparison

**Main container component** that orchestrates the entire comparison interface.

#### Props

```tsx
interface SplitPaneComparisonProps {
  // Data
  requirement?: {
    id: string;
    key: string;
    summary: string;
    description?: string;
    acceptanceCriteria?: string[] | string;
    priority?: string;
    status?: string;
    assignee?: string;
  };
  diff?: string;
  highlightedLines?: Array<{
    lineNumber: number;
    severity: 'error' | 'warning' | 'info';
    message: string;
  }>;
  feedback?: Array<{
    id: string;
    lineNumber?: number;
    severity: 'error' | 'warning' | 'info';
    title: string;
    message: string;
    suggestion?: string;
  }>;
  
  // State
  loading?: boolean | {
    requirement?: boolean;
    diff?: boolean;
    feedback?: boolean;
    all?: boolean;
  };
  error?: string | {
    requirement?: string;
    diff?: string;
    feedback?: string;
    all?: string;
  };
  
  // Configuration
  fileName?: string;
  
  // Callbacks
  onLineClick?: (lineNumber: number, lineContent: string) => void;
  onFeedbackClick?: (feedback: FeedbackItem) => void;
}
```

#### Usage

```jsx
import SplitPaneComparison from '@/components/comparison/SplitPaneComparison';

export function CodeReviewPage() {
  const [requirement, setRequirement] = useState(null);
  const [diff, setDiff] = useState('');
  const [feedback, setFeedback] = useState([]);
  const [loading, setLoading] = useState({});

  useEffect(() => {
    // Load data from APIs
    loadComparisonData();
  }, []);

  return (
    <div style={{ height: '100vh' }}>
      <SplitPaneComparison
        requirement={requirement}
        diff={diff}
        feedback={feedback}
        loading={loading}
        fileName="src/auth/login.js"
        onLineClick={(line, content) => {
          console.log(`Clicked line ${line}: ${content}`);
        }}
      />
    </div>
  );
}
```

#### Features

- Draggable divider to adjust pane sizes (min 20%, max 80%)
- Responsive breakpoints for mobile/tablet
- Passes data to child components
- Coordinates callbacks between sub-components
- Separate loading states for each section

---

### 2. JiraRequirementViewer

**Displays Jira requirement** in a formatted, readable view.

#### Props

```tsx
interface JiraRequirementViewerProps {
  requirement?: {
    id?: string;
    key: string;
    summary: string;
    description?: string;
    acceptanceCriteria?: string[] | string;
    priority?: string;
    status?: string;
    assignee?: string;
  };
  loading?: boolean;
  error?: string;
}
```

#### Features

- Issue key badge with color coding
- Rich description display
- Acceptance criteria as checkable list
- Metadata display (priority, status, assignee)
- Scrollable content area
- Empty state handling

#### Example

```jsx
<JiraRequirementViewer
  requirement={{
    key: 'AUTH-001',
    summary: 'User Login Feature',
    description: 'Implement secure user authentication',
    acceptanceCriteria: [
      'User can enter credentials',
      'Invalid credentials show error',
      'Valid login creates session'
    ],
    priority: 'High',
    status: 'In Progress'
  }}
  loading={false}
/>
```

---

### 3. GitHubDiffViewer

**Displays unified diff** with line-level highlighting and syntax awareness.

#### Props

```tsx
interface GitHubDiffViewerProps {
  diff?: string;
  highlightedLines?: Array<{
    lineNumber: number;
    severity: 'error' | 'warning' | 'info';
    message: string;
  }>;
  fileName?: string;
  loading?: boolean;
  error?: string;
  onLineClick?: (lineNumber: number, lineContent: string) => void;
}
```

#### Highlighting Behavior

- **Red border**: Error severity (high priority issues)
- **Orange border**: Warning severity (medium priority)
- **Blue border**: Info severity (suggestions)
- **Hover effect**: All lines highlight on hover for clarity
- **Summary panel**: Displays all issues with line numbers

#### Features

- Unified diff format support
- Line number tracking
- Delete/Add/Context line distinction
- Issue summary with severity breakdown
- Click handlers for line selection
- Scrollable with custom scrollbar

#### Example

```jsx
<GitHubDiffViewer
  diff={`--- a/file.js
+++ b/file.js
@@ -1,3 +1,3 @@
 function login() {}
-return user;
+return user || null;`}
  highlightedLines={[
    {
      lineNumber: 3,
      severity: 'error',
      message: 'Null handling missing'
    }
  ]}
  fileName="src/auth.js"
  onLineClick={(line, content) => {
    console.log(`Line ${line}: ${content}`);
  }}
/>
```

---

### 4. AIFeedbackPanel

**Displays AI-generated review feedback** in a collapsible, sortable panel.

#### Props

```tsx
interface AIFeedbackPanelProps {
  feedback?: Array<{
    id: string;
    lineNumber?: number;
    severity: 'error' | 'warning' | 'info';
    title: string;
    message: string;
    suggestion?: string;
  }>;
  loading?: boolean;
  error?: string;
  onFeedbackClick?: (feedback: FeedbackItem) => void;
}
```

#### Features

- Automatic sorting by severity (errors first)
- Severity badge with emoji indicators
- Expandable/collapsible interface
- Issue count with breakdown stats
- Markdown rendering for messages
- Code suggestion display
- Line number linking
- Empty state with checkmark

#### Example

```jsx
<AIFeedbackPanel
  feedback={[
    {
      id: 'f1',
      lineNumber: 10,
      severity: 'error',
      title: 'Potential null pointer',
      message: 'Variable `user` might be null. Use optional chaining.',
      suggestion: 'return user?.name || "Unknown";'
    },
    {
      id: 'f2',
      lineNumber: 15,
      severity: 'warning',
      title: 'Performance issue',
      message: 'Consider caching this result'
    }
  ]}
  onFeedbackClick={(item) => {
    scrollToLine(item.lineNumber);
  }}
/>
```

---

### 5. MarkdownRenderer

**Renders Markdown content** to HTML with sanitization.

#### Props

```tsx
interface MarkdownRendererProps {
  content?: string;
  className?: string;
}
```

#### Supported Syntax

- Headers: `# H1`, `## H2`, `### H3`, `#### H4`
- Bold: `**text**` or `__text__`
- Italic: `*text*` or `_text_`
- Code: `` `code` ``
- Links: `[text](url)`
- Lists: `- item` or `* item`
- Line breaks: `\n`

#### Example

```jsx
<MarkdownRenderer
  content={`
# Code Review Feedback

The implementation looks good, but we found **3 issues**:

1. Missing error handling
2. \`const\` should be used instead of \`let\`
3. See [best practices](https://example.com)

**Suggested fix:**
\`\`\`
if (data && data.valid) {
  process(data);
}
\`\`\`
  `}
/>
```

---

## API Integration

### Required Backend Endpoints

```typescript
// Get full comparison data
GET /api/comparison/{projectId}
Response: {
  requirement: RequirementObj,
  diff: string,
  highlightedLines: HighlightedLine[],
  feedback: FeedbackItem[]
}

// Get AI feedback separately
GET /api/comparison/{projectId}/ai-feedback
Response: FeedbackItem[]

// Get Jira requirement
GET /api/comparison/requirements/{requirementId}
Response: RequirementObj

// Get GitHub diff
GET /api/comparison/{projectId}/diff?branch=main&baseBranch=develop
Response: string (unified diff format)

// Update feedback status
PATCH /api/comparison/{projectId}/feedback/{feedbackId}
Body: { status: 'resolved' | 'dismissed' | 'pending' }

// Export comparison
GET /api/comparison/{projectId}/export?format=pdf|csv
Response: Blob
```

### Frontend API Hooks

```typescript
import {
  getComparisonData,
  getAIFeedback,
  getJiraRequirement,
  getGitHubDiff,
  updateFeedbackStatus,
  exportComparison
} from '@/services/api';

// Usage example
const { data, loading, error } = useQuery(
  () => getComparisonData(projectId),
  [projectId]
);
```

---

## Usage Examples

### Basic Integration

```jsx
import SplitPaneComparison from '@/components/comparison/SplitPaneComparison';
import { useEffect, useState } from 'react';
import { getComparisonData } from '@/services/api';

function CodeReviewPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    getComparisonData('project-123')
      .then(setData)
      .catch(setError)
      .finally(() => setLoading(false));
  }, []);

  return (
    <div style={{ height: '100vh' }}>
      <SplitPaneComparison
        {...data}
        loading={loading}
        error={error?.message}
      />
    </div>
  );
}
```

### Advanced with Callbacks

```jsx
function AdvancedReview() {
  const handleLineClick = (lineNumber, content) => {
    // Highlight corresponding requirement section
    const requirementEl = document.querySelector(
      `[data-requirement-line="${lineNumber}"]`
    );
    if (requirementEl) {
      requirementEl.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const handleFeedbackClick = (feedback) => {
    // Open resolution dialog
    showResolutionDialog(feedback);
  };

  return (
    <SplitPaneComparison
      requirement={requirement}
      diff={diff}
      feedback={feedback}
      highlightedLines={highlightedLines}
      onLineClick={handleLineClick}
      onFeedbackClick={handleFeedbackClick}
    />
  );
}
```

---

## Styling & Theming

### CSS Variables (for future use)

```css
:root {
  --comparison-primary: #0366d6;
  --comparison-error: #dc3545;
  --comparison-warning: #ff9800;
  --comparison-info: #0366d6;
  --comparison-border: #e1e4e8;
  --comparison-bg: #f6f8fa;
}
```

### Customization

Each component can be styled by overriding CSS classes:

```css
/* Override Jira viewer styling */
.jira-viewer {
  background-color: #fff;
  font-family: 'Segoe UI', sans-serif;
}

.jira-badge {
  background-color: var(--comparison-primary);
}

/* Override diff viewer styling */
.github-viewer {
  background-color: #fafbfc;
}

.diff-line-add {
  background-color: #f0fff4;
}
```

---

## Responsive Behavior

### Desktop (>1024px)
- Horizontal split-pane layout
- 40% left, 60% right by default
- Bottom feedback panel (30% height)

### Tablet (768px - 1024px)
- Vertical split-pane layout
- Draggable divider between requirement and diff
- Feedback panel expands to accommodate

### Mobile (<768px)
- Responsive tabs or scroll view
- Full-width single pane
- Feedback panel at bottom

---

## Testing

### Running Tests

```bash
npm test -- components/comparison/__tests__

# With coverage
npm test -- --coverage components/comparison/__tests__
```

### Test Files

- `MarkdownRenderer.test.jsx` - Markdown parsing and rendering
- `JiraRequirementViewer.test.jsx` - Requirement display
- `GitHubDiffViewer.test.jsx` - Diff parsing and highlighting
- `AIFeedbackPanel.test.jsx` - Feedback display and interactions
- `SplitPaneComparison.test.jsx` - Integration and layout

---

## Accessibility

### Keyboard Navigation

- `Tab`: Navigate between interactive elements
- `Enter`: Expand/collapse feedback panel
- `Arrow keys`: Scroll within panes

### Screen Readers

- Semantic HTML structure
- ARIA labels on interactive elements
- Proper heading hierarchy
- Alternative text for icons

### Color Contrast

- All text meets WCAG AA standards
- No reliance on color alone for information
- Redundant severity indicators (emoji + color)

---

## Performance

### Optimization Tips

1. **Memoization**: Components use `useMemo` for expensive computations
2. **Virtual Scrolling**: Consider for very large diffs
3. **Lazy Loading**: Load diff viewer only when needed
4. **Debouncing**: Divider resize events are throttled

### Bundle Size

- All components: ~50KB (uncompressed)
- Individual components: 5-15KB each
- No external dependencies beyond React

---

## Common Issues

### Issue: Divider doesn't resize properly

**Solution**: Ensure parent container has fixed height
```jsx
<div style={{ height: '100vh' }}>
  <SplitPaneComparison {...props} />
</div>
```

### Issue: Highlighted lines not showing

**Solution**: Verify `lineNumber` in `highlightedLines` matches actual line numbers in diff

### Issue: Markdown not rendering

**Solution**: Ensure markdown syntax is correct and escape special characters properly

---

## Future Enhancements

- [ ] Virtual scrolling for large diffs
- [ ] Syntax highlighting for code
- [ ] Diff annotations and comments
- [ ] Side-by-side vs unified diff toggle
- [ ] Export to PDF with styling
- [ ] Integration with GitHub API
- [ ] Real-time collaboration features
- [ ] Custom themes/branding

---

## Support & Contributing

For issues or improvements, please refer to the project's contribution guidelines.

Last Updated: April 2026
