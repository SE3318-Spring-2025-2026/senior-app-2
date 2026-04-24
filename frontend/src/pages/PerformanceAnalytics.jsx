import React, { useState, useEffect } from 'react';
import PerformanceRadarChart from '../components/RadarChart';
import TrendlineChart from '../components/TrendlineChart';
import FilterPanel from '../components/FilterPanel';
import {
  getStudentPerformance,
  getGroupPerformance,
  getPerformanceTrends,
  getAvailableStudentsForAnalytics,
  getAvailableGroupsForAnalytics,
} from '../services/api';
import { exportToPDF, exportToCSV } from '../services/exportService';
import './PerformanceAnalytics.css';

const PerformanceAnalytics = () => {
  // State Management
  const [studentPerformance, setStudentPerformance] = useState(null);
  const [groupPerformance, setGroupPerformance] = useState(null);
  const [trendData, setTrendData] = useState(null);
  const [students, setStudents] = useState([]);
  const [groups, setGroups] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Filter State
  const [filters, setFilters] = useState({
    studentId: '',
    groupId: '',
    startDate: null,
    endDate: null,
  });

  // Initial: Load available students and groups
  useEffect(() => {
    const fetchInitialData = async () => {
      try {
        setLoading(true);
        setError(null);

        const [studentsData, groupsData] = await Promise.all([
          getAvailableStudentsForAnalytics(),
          getAvailableGroupsForAnalytics(),
        ]);

        setStudents(studentsData || []);
        setGroups(groupsData || []);
      } catch (err) {
        console.error('Error loading initial data:', err);
        setError('Error loading data');
      } finally {
        setLoading(false);
      }
    };

    fetchInitialData();
  }, []);

  // Load data when filter changes
  useEffect(() => {
    const fetchAnalyticsData = async () => {
      try {
        setLoading(true);
        setError(null);

        // Always load trend data
        const trends = await getPerformanceTrends(filters);
        setTrendData(trends || []);

        // Load student performance if selected
        if (filters.studentId) {
          const studentPerf = await getStudentPerformance(filters.studentId);
          setStudentPerformance(studentPerf || null);
        } else {
          setStudentPerformance(null);
        }

        // Load group performance if selected
        if (filters.groupId) {
          const groupPerf = await getGroupPerformance(filters.groupId);
          setGroupPerformance(groupPerf || null);
        } else {
          setGroupPerformance(null);
        }
      } catch (err) {
        console.error('Error loading analytics data:', err);
        setError('Error loading analytics data');
      } finally {
        setLoading(false);
      }
    };

    fetchAnalyticsData();
  }, [filters]);

  // Handle filter change
  const handleFilterChange = (newFilters) => {
    setFilters(newFilters);
  };

  // Export report as PDF
  const handleExportPDF = async () => {
    try {
      await exportToPDF('analytics-dashboard', {
        fileName: 'Performance-Analytics-Report.pdf',
        title: 'Performance Analytics Report',
        metadata: {
          'Report Date': new Date().toLocaleDateString('en-US'),
          'Student': filters.studentId ? 'Selected' : 'All',
          'Group': filters.groupId ? 'Selected' : 'All',
        },
      });
    } catch (err) {
      console.error('PDF export error:', err);
      setError('Error exporting PDF');
    }
  };

  // Export data as CSV
  const handleExportCSV = () => {
    try {
      if (trendData && trendData.length > 0) {
        exportToCSV(trendData, 'Performance-Trend-Data.csv');
      } else {
        setError('No data to export');
      }
    } catch (err) {
      console.error('CSV export error:', err);
      setError('Error exporting CSV');
    }
  };

  return (
    <div className="performance-analytics-page">
      <div className="page-header">
        <h1>📊 Performance Analytics Dashboard</h1>
        <p>Analyze student and group performance over time</p>
      </div>

      {error && (
        <div className="alert alert-error">
          <strong>Error:</strong> {error}
        </div>
      )}

      <div className="analytics-container">
        {/* Filtre Paneli */}
        <aside className="filter-sidebar">
          <FilterPanel
            students={students}
            groups={groups}
            onFilterChange={handleFilterChange}
            loading={loading}
          />
        </aside>

        {/* Ana İçerik */}
        <main className="analytics-content" id="analytics-dashboard">
          {/* Export Buttons */}
          <div className="export-controls">
            <button
              onClick={handleExportPDF}
              disabled={loading}
              className="btn-primary"
              title="Download report as PDF"
            >
              📥 Download PDF
            </button>
            <button
              onClick={handleExportCSV}
              disabled={loading || !trendData}
              className="btn-secondary"
              title="Download data as CSV"
            >
              📊 Download CSV
            </button>
          </div>

          {/* Radar Chart - Student Performance */}
          {filters.studentId && (
            <div className="chart-section radar-section">
              <PerformanceRadarChart
                data={studentPerformance?.metrics || []}
                studentName={
                  students.find((s) => s.id === filters.studentId)?.name ||
                  'Student'
                }
                loading={loading}
              />
            </div>
          )}

          {/* Radar Chart - Group Performance */}
          {filters.groupId && groupPerformance && (
            <div className="chart-section radar-section">
              <PerformanceRadarChart
                data={groupPerformance?.metrics || []}
                studentName={
                  groups.find((g) => g.id === filters.groupId)?.name || 'Group'
                }
                loading={loading}
              />
            </div>
          )}

          {/* Trendline Chart */}
          {trendData && trendData.length > 0 && (
            <div className="chart-section trendline-section">
              <TrendlineChart
                data={trendData}
                title="Performance Trends - Score Progress Throughout the Semester"
                loading={loading}
                type="line"
              />
            </div>
          )}

          {/* Summary Statistics */}
          {(studentPerformance || groupPerformance) && (
            <div className="summary-section">
              <h3>📈 Summary Statistics</h3>
              <div className="summary-grid">
                {studentPerformance?.summary && (
                  <>
                    <div className="summary-card">
                      <h4>Average Score</h4>
                      <p className="summary-value">
                        {studentPerformance.summary.averageScore}%
                      </p>
                    </div>
                    <div className="summary-card">
                      <h4>Highest Score</h4>
                      <p className="summary-value">
                        {studentPerformance.summary.maxScore}%
                      </p>
                    </div>
                    <div className="summary-card">
                      <h4>Lowest Score</h4>
                      <p className="summary-value">
                        {studentPerformance.summary.minScore}%
                      </p>
                    </div>
                    <div className="summary-card">
                      <h4>Improvement</h4>
                      <p
                        className={`summary-value ${
                          studentPerformance.summary.improvement >= 0
                            ? 'positive'
                            : 'negative'
                        }`}
                      >
                        {studentPerformance.summary.improvement > 0 ? '+' : ''}
                        {studentPerformance.summary.improvement}%
                      </p>
                    </div>
                  </>
                )}
                {groupPerformance?.summary && (
                  <>
                    <div className="summary-card">
                      <h4>Group Average Score</h4>
                      <p className="summary-value">
                        {groupPerformance.summary.averageScore}%
                      </p>
                    </div>
                    <div className="summary-card">
                      <h4>Group Members</h4>
                      <p className="summary-value">
                        {groupPerformance.summary.memberCount}
                      </p>
                    </div>
                    <div className="summary-card">
                      <h4>Project Count</h4>
                      <p className="summary-value">
                        {groupPerformance.summary.projectCount}
                      </p>
                    </div>
                  </>
                )}
              </div>
            </div>
          )}

          {/* Empty State */}
          {!filters.studentId && !filters.groupId && (
            <div className="empty-state">
              <p>📋 Select a student or group from the sidebar to get started</p>
            </div>
          )}
        </main>
      </div>
    </div>
  );
};

export default PerformanceAnalytics;
