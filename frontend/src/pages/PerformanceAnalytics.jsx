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

  // Filtre State
  const [filters, setFilters] = useState({
    studentId: '',
    groupId: '',
    startDate: null,
    endDate: null,
  });

  // Başlangıç: Mevcut öğrenci ve grupları yükle
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
        setError('Veriler yüklenirken hata oluştu');
      } finally {
        setLoading(false);
      }
    };

    fetchInitialData();
  }, []);

  // Filtreleme değiştiğinde verileri yükle
  useEffect(() => {
    const fetchAnalyticsData = async () => {
      try {
        setLoading(true);
        setError(null);

        // Trend verilerini her zaman yükle
        const trends = await getPerformanceTrends(filters);
        setTrendData(trends || []);

        // Öğrenci seçiliyse öğrenci performansını yükle
        if (filters.studentId) {
          const studentPerf = await getStudentPerformance(filters.studentId);
          setStudentPerformance(studentPerf || null);
        } else {
          setStudentPerformance(null);
        }

        // Grup seçiliyse grup performansını yükle
        if (filters.groupId) {
          const groupPerf = await getGroupPerformance(filters.groupId);
          setGroupPerformance(groupPerf || null);
        } else {
          setGroupPerformance(null);
        }
      } catch (err) {
        console.error('Error loading analytics data:', err);
        setError('Analitik verileri yüklenirken hata oluştu');
      } finally {
        setLoading(false);
      }
    };

    fetchAnalyticsData();
  }, [filters]);

  // Filtreleme değişimi
  const handleFilterChange = (newFilters) => {
    setFilters(newFilters);
  };

  // PDF olarak raporu dışa aktarma
  const handleExportPDF = async () => {
    try {
      await exportToPDF('analytics-dashboard', {
        fileName: 'Performans-Analitik-Raporu.pdf',
        title: 'Performans Analitik Raporu',
        metadata: {
          'Rapor Tarihi': new Date().toLocaleDateString('tr-TR'),
          'Öğrenci': filters.studentId ? 'Seçili' : 'Tüm',
          'Grup': filters.groupId ? 'Seçili' : 'Tüm',
        },
      });
    } catch (err) {
      console.error('PDF export error:', err);
      setError('PDF dışa aktarırken hata oluştu');
    }
  };

  // CSV olarak veriyi dışa aktarma
  const handleExportCSV = () => {
    try {
      if (trendData && trendData.length > 0) {
        exportToCSV(trendData, 'Performans-Trend-Verileri.csv');
      } else {
        setError('Dışa aktaracak veri bulunamadı');
      }
    } catch (err) {
      console.error('CSV export error:', err);
      setError('CSV dışa aktarırken hata oluştu');
    }
  };

  return (
    <div className="performance-analytics-page">
      <div className="page-header">
        <h1>📊 Performans Analitik Dashboard</h1>
        <p>Öğrenci ve grup performansını zaman serisine göre analiz edin</p>
      </div>

      {error && (
        <div className="alert alert-error">
          <strong>Hata:</strong> {error}
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
          {/* Export Düğmeleri */}
          <div className="export-controls">
            <button
              onClick={handleExportPDF}
              disabled={loading}
              className="btn-primary"
              title="Raporu PDF olarak indir"
            >
              📥 PDF Olarak İndir
            </button>
            <button
              onClick={handleExportCSV}
              disabled={loading || !trendData}
              className="btn-secondary"
              title="Verileri CSV olarak indir"
            >
              📊 CSV Olarak İndir
            </button>
          </div>

          {/* Radar Chart - Öğrenci Performans */}
          {filters.studentId && (
            <div className="chart-section radar-section">
              <PerformanceRadarChart
                data={studentPerformance?.metrics || []}
                studentName={
                  students.find((s) => s.id === filters.studentId)?.name ||
                  'Öğrenci'
                }
                loading={loading}
              />
            </div>
          )}

          {/* Radar Chart - Grup Performans */}
          {filters.groupId && groupPerformance && (
            <div className="chart-section radar-section">
              <PerformanceRadarChart
                data={groupPerformance?.metrics || []}
                studentName={
                  groups.find((g) => g.id === filters.groupId)?.name || 'Grup'
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
                title="Performans Trendi - Semester Boyunca Skor İlerleme"
                loading={loading}
                type="line"
              />
            </div>
          )}

          {/* Özet İstatistikler */}
          {(studentPerformance || groupPerformance) && (
            <div className="summary-section">
              <h3>📈 Özet İstatistikler</h3>
              <div className="summary-grid">
                {studentPerformance?.summary && (
                  <>
                    <div className="summary-card">
                      <h4>Ortalama Skor</h4>
                      <p className="summary-value">
                        {studentPerformance.summary.averageScore}%
                      </p>
                    </div>
                    <div className="summary-card">
                      <h4>En Yüksek Skor</h4>
                      <p className="summary-value">
                        {studentPerformance.summary.maxScore}%
                      </p>
                    </div>
                    <div className="summary-card">
                      <h4>En Düşük Skor</h4>
                      <p className="summary-value">
                        {studentPerformance.summary.minScore}%
                      </p>
                    </div>
                    <div className="summary-card">
                      <h4>İlerleme</h4>
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
                      <h4>Grup Ortalama Skoru</h4>
                      <p className="summary-value">
                        {groupPerformance.summary.averageScore}%
                      </p>
                    </div>
                    <div className="summary-card">
                      <h4>Grup Üye Sayısı</h4>
                      <p className="summary-value">
                        {groupPerformance.summary.memberCount}
                      </p>
                    </div>
                    <div className="summary-card">
                      <h4>Proje Sayısı</h4>
                      <p className="summary-value">
                        {groupPerformance.summary.projectCount}
                      </p>
                    </div>
                  </>
                )}
              </div>
            </div>
          )}

          {/* Boş Durum */}
          {!filters.studentId && !filters.groupId && (
            <div className="empty-state">
              <p>📋 Başlamak için sol taraftan bir öğrenci veya grup seçin</p>
            </div>
          )}
        </main>
      </div>
    </div>
  );
};

export default PerformanceAnalytics;
