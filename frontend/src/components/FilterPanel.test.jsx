import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import FilterPanel from '../components/FilterPanel';

describe('FilterPanel Component', () => {
  const mockStudents = [
    { id: '1', name: 'Ahmet Yılmaz', studentId: '20230001' },
    { id: '2', name: 'Fatih Demir', studentId: '20230002' },
  ];

  const mockGroups = [
    { id: '1', name: 'Grup A' },
    { id: '2', name: 'Grup B' },
  ];

  const mockOnFilterChange = vi.fn();

  it('renders filter panel title', () => {
    render(
      <FilterPanel
        students={mockStudents}
        groups={mockGroups}
        onFilterChange={mockOnFilterChange}
        loading={false}
      />
    );
    expect(screen.getByText('Filtreler')).toBeInTheDocument();
  });

  it('renders student select with options', () => {
    render(
      <FilterPanel
        students={mockStudents}
        groups={mockGroups}
        onFilterChange={mockOnFilterChange}
        loading={false}
      />
    );
    
    const studentSelect = screen.getByDisplayValue('-- Tüm Öğrenciler --');
    expect(studentSelect).toBeInTheDocument();
    
    const options = screen.getAllByRole('option');
    expect(options.length).toBeGreaterThan(0);
  });

  it('renders group select with options', () => {
    render(
      <FilterPanel
        students={mockStudents}
        groups={mockGroups}
        onFilterChange={mockOnFilterChange}
        loading={false}
      />
    );
    
    const groupSelect = screen.getByDisplayValue('-- Tüm Gruplar --');
    expect(groupSelect).toBeInTheDocument();
  });

  it('disables selects when loading', () => {
    render(
      <FilterPanel
        students={mockStudents}
        groups={mockGroups}
        onFilterChange={mockOnFilterChange}
        loading={true}
      />
    );
    
    const selects = screen.getAllByRole('combobox');
    selects.forEach((select) => {
      expect(select).toBeDisabled();
    });
  });

  it('calls onFilterChange when filter changes', async () => {
    const user = userEvent.setup();
    render(
      <FilterPanel
        students={mockStudents}
        groups={mockGroups}
        onFilterChange={mockOnFilterChange}
        loading={false}
      />
    );

    const studentSelects = screen.getAllByRole('combobox');
    const studentSelect = studentSelects[0];
    
    await user.selectOptions(studentSelect, '1');
    
    await waitFor(() => {
      expect(mockOnFilterChange).toHaveBeenCalled();
    });
  });

  it('renders reset button', () => {
    render(
      <FilterPanel
        students={mockStudents}
        groups={mockGroups}
        onFilterChange={mockOnFilterChange}
        loading={false}
      />
    );
    
    expect(screen.getByText('Filtreleri Temizle')).toBeInTheDocument();
  });

  it('reset button is disabled when loading', () => {
    render(
      <FilterPanel
        students={mockStudents}
        groups={mockGroups}
        onFilterChange={mockOnFilterChange}
        loading={true}
      />
    );
    
    const resetButton = screen.getByText('Filtreleri Temizle');
    expect(resetButton).toBeDisabled();
  });
});
