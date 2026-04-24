import React from 'react';
import {
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
  Radar,
  Legend,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';

/**
 * RadarChart Component - Displays student performance metrics
 * @param {Object} props
 * @param {Array} props.data - Performance data
 * @param {string} props.studentName - Student name
 * @param {boolean} props.loading - Loading state
 */
const PerformanceRadarChart = ({ data, studentName, loading }) => {
  if (loading) {
    return (
      <div className="radar-chart-container loading">
        <p>Loading data...</p>
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className="radar-chart-container empty">
        <p>No data available</p>
      </div>
    );
  }

  return (
    <div className="radar-chart-container">
      <h3>{studentName} - Performance Metrics</h3>
      <ResponsiveContainer width="100%" height={400}>
        <RadarChart data={data}>
          <PolarGrid stroke="#ccc" />
          <PolarAngleAxis dataKey="metric" />
          <PolarRadiusAxis angle={90} domain={[0, 100]} />
          <Radar
            name="Performance"
            dataKey="value"
            stroke="#8884d8"
            fill="#8884d8"
            fillOpacity={0.6}
          />
          <Tooltip
            contentStyle={{
              backgroundColor: '#f9f9f9',
              border: '1px solid #ccc',
              borderRadius: '4px',
            }}
            formatter={(value) => `${value}%`}
          />
          <Legend />
        </RadarChart>
      </ResponsiveContainer>
    </div>
  );
};

export default PerformanceRadarChart;
