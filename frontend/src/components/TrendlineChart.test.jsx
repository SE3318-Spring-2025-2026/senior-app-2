import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import TrendlineChart from '../components/TrendlineChart';

describe('TrendlineChart Component', () => {
  const mockTrendData = [
    { date: '2025-01-01', score: 60, trend: 60 },
    { date: '2025-01-08', score: 65, trend: 62.5 },
    { date: '2025-01-15', score: 72, trend: 66.67 },
    { date: '2025-01-22', score: 78, trend: 71.67 },
    { date: '2025-01-29', score: 85, trend: 76 },
  ];

  it('renders the chart title', () => {
    render(
      <TrendlineChart
        data={mockTrendData}
        title="Test Trend Chart"
        loading={false}
      />
    );
    expect(screen.getByText('Test Trend Chart')).toBeInTheDocument();
  });

  it('displays loading state', () => {
    render(
      <TrendlineChart
        data={[]}
        title="Test Chart"
        loading={true}
      />
    );
    expect(screen.getByText(/Loading data/i)).toBeInTheDocument();
  });

  it('displays empty state when no data provided', () => {
    render(
      <TrendlineChart
        data={[]}
        title="Test Chart"
        loading={false}
      />
    );
    expect(screen.getByText(/No data available/i)).toBeInTheDocument();
  });

  it('renders with line chart type', () => {
    const { container } = render(
      <TrendlineChart
        data={mockTrendData}
        title="Line Chart Test"
        loading={false}
        type="line"
      />
    );
    const chartContainer = container.querySelector('.trendline-chart-container');
    expect(chartContainer).toBeInTheDocument();
  });

  it('renders with area chart type', () => {
    const { container } = render(
      <TrendlineChart
        data={mockTrendData}
        title="Area Chart Test"
        loading={false}
        type="area"
      />
    );
    const chartContainer = container.querySelector('.trendline-chart-container');
    expect(chartContainer).toBeInTheDocument();
  });
});
