import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import JiraRequirementViewer from '../JiraRequirementViewer';

describe('JiraRequirementViewer', () => {
  const mockRequirement = {
    id: 1,
    key: 'PROJ-001',
    summary: 'Implement user authentication',
    description: 'Users should be able to log in with email and password',
    acceptanceCriteria: [
      'User can enter email and password',
      'System validates credentials',
      'User receives session token on success'
    ],
    priority: 'High',
    status: 'In Progress',
    assignee: 'John Doe'
  };

  test('renders loading state', () => {
    render(<JiraRequirementViewer loading={true} />);
    expect(screen.getByText('Loading Jira requirement...')).toBeInTheDocument();
  });

  test('renders error state', () => {
    const error = 'Failed to load requirement';
    render(<JiraRequirementViewer error={error} />);
    expect(screen.getByText(error)).toBeInTheDocument();
  });

  test('renders empty state when no requirement', () => {
    render(<JiraRequirementViewer />);
    expect(screen.getByText('No requirement selected')).toBeInTheDocument();
  });

  test('renders requirement with all fields', () => {
    render(<JiraRequirementViewer requirement={mockRequirement} />);
    
    expect(screen.getByText('PROJ-001')).toBeInTheDocument();
    expect(screen.getByText('Implement user authentication')).toBeInTheDocument();
    expect(screen.getByText('Users should be able to log in with email and password')).toBeInTheDocument();
  });

  test('renders acceptance criteria as list', () => {
    render(<JiraRequirementViewer requirement={mockRequirement} />);
    
    const criteriaItems = screen.getAllByRole('listitem');
    expect(criteriaItems).toHaveLength(3);
  });

  test('renders metadata information', () => {
    render(<JiraRequirementViewer requirement={mockRequirement} />);
    
    expect(screen.getByText('High')).toBeInTheDocument();
    expect(screen.getByText('In Progress')).toBeInTheDocument();
    expect(screen.getByText('John Doe')).toBeInTheDocument();
  });

  test('handles requirement with string acceptance criteria', () => {
    const requirement = {
      ...mockRequirement,
      acceptanceCriteria: 'Item 1; Item 2; Item 3'
    };
    
    render(<JiraRequirementViewer requirement={requirement} />);
    const items = screen.getAllByRole('listitem');
    expect(items.length).toBeGreaterThan(0);
  });

  test('handles requirement without optional fields', () => {
    const minimalRequirement = {
      key: 'PROJ-002',
      summary: 'Simple requirement'
    };
    
    render(<JiraRequirementViewer requirement={minimalRequirement} />);
    expect(screen.getByText('Simple requirement')).toBeInTheDocument();
  });
});
