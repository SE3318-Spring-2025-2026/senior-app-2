import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PerformanceRadarChart from '../components/RadarChart';

describe('PerformanceRadarChart Component', () => {
  const mockData = [
    { metric: 'Doğruluk', value: 85 },
    { metric: 'Hız', value: 75 },
    { metric: 'Kalite', value: 90 },
    { metric: 'İşbirliği', value: 80 },
    { metric: 'Zamanında Teslimat', value: 88 },
  ];

  it('renders the chart title', () => {
    render(
      <PerformanceRadarChart
        data={mockData}
        studentName="Ahmet Yılmaz"
        loading={false}
      />
    );
    expect(screen.getByText(/Ahmet Yılmaz - Performans Metrikleri/i)).toBeInTheDocument();
  });

  it('displays loading state', () => {
    render(
      <PerformanceRadarChart
        data={[]}
        studentName="Ahmet Yılmaz"
        loading={true}
      />
    );
    expect(screen.getByText(/Veriler yükleniyor/i)).toBeInTheDocument();
  });

  it('displays empty state when no data', () => {
    render(
      <PerformanceRadarChart
        data={[]}
        studentName="Ahmet Yılmaz"
        loading={false}
      />
    );
    expect(screen.getByText(/Görüntülenecek veri bulunamadı/i)).toBeInTheDocument();
  });

  it('renders with proper structure', () => {
    const { container } = render(
      <PerformanceRadarChart
        data={mockData}
        studentName="Test Student"
        loading={false}
      />
    );
    const chartContainer = container.querySelector('.radar-chart-container');
    expect(chartContainer).toBeInTheDocument();
  });
});
