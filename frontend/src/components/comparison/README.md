# Code Review Comparison UI - Implementation Guide

## Quick Start

### 1. Import Components

```jsx
import SplitPaneComparison from '@/components/comparison/SplitPaneComparison';
import { getComparisonData } from '@/services/api';

function MyReviewPage() {
  const [data, setData] = useState(null);

  useEffect(() => {
    getComparisonData(projectId).then(setData);
  }, [projectId]);

  return <SplitPaneComparison {...data} />;
}
```

### 2. Add Route to App.jsx

```jsx
import CodeReviewComparison from './pages/CodeReviewComparison';

<Route path="/panel/review/:projectId" element={<CodeReviewComparison />} />
```

### 3. Add Navigation Link in Layout.jsx

```jsx
{user?.role === 'PROFESSOR' && (
  <NavLink to="/panel/review/projects" className="nav-item">
    Code Reviews
  </NavLink>
)}
```

---

## Project Structure

```
frontend/src/components/comparison/
├── SplitPaneComparison.jsx              # Main container component
├── SplitPaneComparison.css              # Layout styling
├── JiraRequirementViewer.jsx            # Left pane - requirements
├── JiraRequirementViewer.css
├── GitHubDiffViewer.jsx                 # Right top pane - code diff
├── GitHubDiffViewer.css
├── AIFeedbackPanel.jsx                  # Right bottom pane - AI feedback
├── AIFeedbackPanel.css
├── MarkdownRenderer.jsx                 # Markdown to HTML converter
├── MarkdownRenderer.css
├── COMPONENT_DOCS.md                    # Full component documentation
├── README.md                            # This file
└── __tests__/
    ├── SplitPaneComparison.test.jsx
    ├── JiraRequirementViewer.test.jsx
    ├── GitHubDiffViewer.test.jsx
    ├── AIFeedbackPanel.test.jsx
    └── MarkdownRenderer.test.jsx

frontend/src/pages/
├── CodeReviewComparison.jsx             # Integration page
└── CodeReviewComparison.css

frontend/src/services/
└── api.js                               # API endpoints (extended)
```

---

## Component Hierarchy

```
CodeReviewComparison (page)
└── SplitPaneComparison (container)
    ├── JiraRequirementViewer (left pane)
    ├── GitHubDiffViewer (right top)
    └── AIFeedbackPanel (right bottom)
        └── MarkdownRenderer (for feedback messages)
```

---

## Data Flow

### 1. Loading Comparison Data

```
CodeReviewComparison.jsx
  ↓
getComparisonData(projectId) [API call]
  ↓
Backend: /api/comparison/{projectId}
  ↓
Response: {
  requirement: {...},
  diff: "...",
  feedback: [...],
  highlightedLines: [...]
}
  ↓
SplitPaneComparison receives props
  ↓
Sub-components render with data
```

### 2. User Interactions

```
User clicks on line in diff
  ↓
GitHubDiffViewer.onLineClick()
  ↓
CodeReviewComparison.handleLineClick()
  ↓
Can trigger callbacks or update parent state
```

### 3. Feedback Highlighting

```
highlightedLines prop
  ↓
GitHubDiffViewer maps line numbers
  ↓
Line rendered with appropriate CSS class
  ↓
Visual highlighting applied (red/orange/blue border)
```

---

## Styling System

### Color Scheme

```css
--primary: #0366d6      /* Main brand color */
--success: #28a745      /* Success/approved */
--error: #dc3545        /* Error/critical issues */
--warning: #ff9800      /* Warnings */
--info: #0366d6         /* Information */
--border: #e1e4e8       /* Borders and dividers */
--bg-light: #f6f8fa     /* Light backgrounds */
```

### CSS Architecture

- **BEM naming convention**: `.block__element--modifier`
- **Component-scoped CSS**: Each component has its own CSS file
- **Mobile-first**: Responsive breakpoints at 768px and 1024px
- **No framework dependencies**: Pure CSS for consistency

### Responsive Breakpoints

```css
/* Desktop: 1400px+ */
Split pane: 40% left, 60% right (horizontal)

/* Tablet: 1024px - 1400px */
Split pane: Vertical (requirement on top, diff below)

/* Mobile: < 768px */
Stacked layout with tabs or full-width sections
```

---

## Backend API Requirements

### Endpoints to Implement

1. **GET /api/comparison/{projectId}**
   - Returns full comparison data
   - Required fields: requirement, diff, feedback
   - Optional: highlightedLines

2. **GET /api/comparison/{projectId}/ai-feedback**
   - Returns array of feedback items
   - Each item: id, lineNumber, severity, title, message, suggestion

3. **GET /api/comparison/requirements/{requirementId}**
   - Returns single Jira requirement

4. **GET /api/comparison/{projectId}/diff**
   - Returns unified diff format
   - Query params: branch, baseBranch, filePath

5. **PATCH /api/comparison/{projectId}/feedback/{feedbackId}**
   - Update feedback status (resolved/dismissed)

### Example Response Format

```json
{
  "requirement": {
    "id": "req-123",
    "key": "AUTH-001",
    "summary": "User Login Feature",
    "description": "Implement secure authentication",
    "acceptanceCriteria": [
      "User can login with email",
      "Password must be validated",
      "Session token generated"
    ],
    "priority": "High",
    "status": "In Progress",
    "assignee": "John Doe"
  },
  "diff": "--- a/auth.js\n+++ b/auth.js\n@@ -1,3 +1,4 @@\n...",
  "highlightedLines": [
    {
      "lineNumber": 10,
      "severity": "error",
      "message": "Missing null check"
    }
  ],
  "feedback": [
    {
      "id": "f1",
      "lineNumber": 10,
      "severity": "error",
      "title": "Null pointer risk",
      "message": "Variable may be undefined",
      "suggestion": "if (user) { ... }"
    }
  ]
}
```

---

## Testing

### Run Tests

```bash
# All comparison tests
npm test -- components/comparison

# Specific component
npm test -- components/comparison/__tests__/MarkdownRenderer.test.jsx

# With coverage
npm test -- --coverage components/comparison

# Watch mode
npm test -- --watch components/comparison
```

### Test Coverage

- ✅ Component rendering
- ✅ Props validation
- ✅ State management
- ✅ User interactions
- ✅ Error handling
- ✅ Responsive behavior
- ✅ Accessibility

---

## Performance Optimization

### Current Optimizations

1. **Memoization**: Components use `useMemo` for expensive computations
2. **Conditional Rendering**: Only render visible content
3. **CSS-based Styling**: No inline styles for better performance
4. **Lazy Loading**: Optional for large diffs

### Further Improvements

```jsx
// Consider virtual scrolling for very large diffs
import { FixedSizeList } from 'react-window';

// Or implement pagination
<DiffViewer 
  diff={diff}
  pageSize={50}
  currentPage={page}
/>
```

---

## Accessibility

### Features Implemented

- ✅ Semantic HTML
- ✅ Keyboard navigation
- ✅ ARIA labels
- ✅ Color contrast (WCAG AA)
- ✅ Focus indicators
- ✅ Screen reader support

### Testing Accessibility

```bash
# Using axe DevTools
npm install --save-dev @axe-core/react

# In tests
import { axe } from 'jest-axe';

test('should have no accessibility violations', async () => {
  const { container } = render(<SplitPaneComparison {...props} />);
  const results = await axe(container);
  expect(results).toHaveNoViolations();
});
```

---

## Troubleshooting

### Issue: Split pane divider not dragging

**Cause**: Parent container doesn't have fixed height
**Solution**: 
```jsx
<div style={{ height: '100vh' }}>
  <SplitPaneComparison {...props} />
</div>
```

### Issue: Markdown not rendering

**Cause**: Invalid markdown syntax
**Solution**: Check syntax - use backticks for code, ** for bold, etc.

### Issue: Highlighted lines not showing

**Cause**: lineNumber doesn't match actual diff lines
**Solution**: Verify line numbers from backend are correct

### Issue: Feedback panel not scrolling

**Cause**: Missing overflow-y: auto
**Solution**: Check AIFeedbackPanel.css for scrollbar styling

---

## Contributing

When adding new features:

1. **Maintain component isolation**: Each component should be self-contained
2. **Follow naming conventions**: Use BEM for CSS classes
3. **Add tests**: Write tests for new functionality
4. **Update documentation**: Keep COMPONENT_DOCS.md current
5. **Test responsiveness**: Verify mobile/tablet/desktop layouts
6. **Verify accessibility**: Use keyboard navigation and screen reader

---

## Future Enhancements

- [ ] Syntax highlighting for code
- [ ] Side-by-side diff comparison
- [ ] Comments and annotations on lines
- [ ] Export to PDF with formatting
- [ ] Real-time collaboration
- [ ] GitHub API integration
- [ ] Jira webhook integration
- [ ] Custom branding/themes

---

## Support

For issues or questions:
1. Check COMPONENT_DOCS.md for detailed documentation
2. Review test files for usage examples
3. See example integration in CodeReviewComparison.jsx
4. Check Git history for recent changes

---

## License & Credits

Built with React, following accessibility best practices and responsive design principles.

Last Updated: April 2026
