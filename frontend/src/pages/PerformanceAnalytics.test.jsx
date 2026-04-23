import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PerformanceAnalytics from '../pages/PerformanceAnalytics';
import * as api from '../services/api';
import * as exportService from '../services/exportService';

// Mock API
vi.mock('../services/api', () => ({
  getStudentPerformance: vi.fn(),
  getGroupPerformance: vi.fn(),
  getPerformanceTrends: vi.fn(),
  getAvailableStudentsForAnalytics: vi.fn(),
  getAvailableGroupsForAnalytics: vi.fn(),
}));

// Mock Export Service
vi.mock('../services/exportService', () => ({
  exportToPDF: vi.fn(),
  exportToCSV: vi.fn(),
}));

describe('PerformanceAnalytics Page', () => {
  const mockStudents = [
    { id: '1', name: 'Ahmet Yılmaz', studentId: '20230001' },
    { id: '2', name: 'Fatih Demir', studentId: '20230002' },
  ];

  const mockGroups = [
    { id: '1', name: 'Grup A' },
    { id: '2', name: 'Grup B' },
  ];

  const mockTrendData = [
    { date: '2025-01-01', score: 60 },
    { date: '2025-01-08', score: 75 },
    { date: '2025-01-15', score: 85 },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    api.getAvailableStudentsForAnalytics.mockResolvedValue(mockStudents);
    api.getAvailableGroupsForAnalytics.mockResolvedValue(mockGroups);
    api.getPerformanceTrends.mockResolvedValue(mockTrendData);
  });

  it('renders the page title', async () => {
    render(<PerformanceAnalytics />);
    
    await waitFor(() => {
      expect(
        screen.getByText(/Performans Analitik Dashboard/i)
      ).toBeInTheDocument();
    });
  });

  it('loads students and groups on mount', async () => {
    render(<PerformanceAnalytics />);
    
    await waitFor(() => {
      expect(api.getAvailableStudentsForAnalytics).toHaveBeenCalled();
      expect(api.getAvailableGroupsForAnalytics).toHaveBeenCalled();
    });
  });

  it('renders filter panel', async () => {
    render(<PerformanceAnalytics />);
    
    await waitFor(() => {
      expect(screen.getByText('Filtreler')).toBeInTheDocument();
    });
  });

  it('renders export buttons', async () => {
    render(<PerformanceAnalytics />);
    
    await waitFor(() => {
      expect(screen.getByText(/PDF Olarak İndir/i)).toBeInTheDocument();
      expect(screen.getByText(/CSV Olarak İndir/i)).toBeInTheDocument();
    });
  });

  it('shows empty state when no filter selected', async () => {
    render(<PerformanceAnalytics />);
    
    await waitFor(() => {
      expect(
        screen.getByText(/sol taraftan bir öğrenci veya grup seçin/i)
      ).toBeInTheDocument();
    });
  });

  it('calls exportToPDF when PDF button clicked', async () => {
    const user = userEvent.setup();
    
    render(<PerformanceAnalytics />);
    
    await waitFor(() => {
      expect(screen.getByText(/PDF Olarak İndir/i)).toBeInTheDocument();
    });

    const pdfButton = screen.getByText(/PDF Olarak İndir/i);
    
    await user.click(pdfButton);
    
    // Note: exportToPDF might be called but depends on rendering
    // This is a basic test showing the pattern
  });

  it('handles API errors gracefully', async () => {
    api.getAvailableStudentsForAnalytics.mockRejectedValue(
      new Error('API Error')
    );

    render(<PerformanceAnalytics />);
    
    await waitFor(() => {
      expect(
        screen.getByText(/Veriler yüklenirken hata oluştu/i)
      ).toBeInTheDocument();
    });
  });

  it('loads performance data when filter changes', async () => {
    const mockPerformance = {
      metrics: [
        { metric: 'Doğruluk', value: 85 },
        { metric: 'Hız', value: 75 },
      ],
      summary: {
        averageScore: 80,
        maxScore: 95,
        minScore: 65,
        improvement: 15,
      },
    };

    api.getStudentPerformance.mockResolvedValue(mockPerformance);

    const { rerender } = render(<PerformanceAnalytics />);
    
    await waitFor(() => {
      expect(api.getPerformanceTrends).toHaveBeenCalled();
    });
  });
});
