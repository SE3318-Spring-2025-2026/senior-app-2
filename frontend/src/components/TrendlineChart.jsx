import React from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  Area,
  AreaChart,
} from 'recharts';
import { format } from 'date-fns';
import { tr } from 'date-fns/locale';

/**
 * TrendlineChart Component - Shows score progress over time
 * @param {Object} props
 * @param {Array} props.data - Trend data
 * @param {string} props.title - Chart title
 * @param {boolean} props.loading - Loading state
 * @param {string} props.type - Chart type ('line' or 'area')
 */
const TrendlineChart = ({ data, title, loading, type = 'line' }) => {
  if (loading) {
    return (
      <div className="trendline-chart-container loading">
        <p>Loading data...</p>
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className="trendline-chart-container empty">
        <p>No data available</p>
      </div>
    );
  }

  const ChartComponent = type === 'area' ? AreaChart : LineChart;

  return (
    <div className="trendline-chart-container">
      <h3>{title}</h3>
      <ResponsiveContainer width="100%" height={300}>
        <ChartComponent data={data}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis
            dataKey="date"
            tickFormatter={(date) => {
              try {
                return format(new Date(date), 'MMM dd', { locale: tr });
              } catch {
                return date;
              }
            }}
          />
          <YAxis domain={[0, 100]} />
          <Tooltip
            contentStyle={{
              backgroundColor: '#f9f9f9',
              border: '1px solid #ccc',
              borderRadius: '4px',
            }}
            labelFormatter={(label) => {
              try {
                return format(new Date(label), 'dd MMMM yyyy', { locale: tr });
              } catch {
                return label;
              }
            }}
            formatter={(value) => `${value}%`}
          />
          <Legend />
          {type === 'area' ? (
            <>
              <Area
                type="monotone"
                dataKey="score"
                stroke="#82ca9d"
                fill="#82ca9d"
                fillOpacity={0.6}
                name="Skor"
              />
            </>
          ) : (
            <>
              <Line
                type="monotone"
                dataKey="score"
                stroke="#82ca9d"
                dot={{ fill: '#82ca9d', r: 5 }}
                activeDot={{ r: 7 }}
                name="Skor"
              />
              {data[0]?.trend && (
                <Line
                  type="monotone"
                  dataKey="trend"
                  stroke="#ffc658"
                  strokeDasharray="5 5"
                  name="Trend"
                />
              )}
            </>
          )}
        </ChartComponent>
      </ResponsiveContainer>
    </div>
  );
};

export default TrendlineChart;
