import { useState, useEffect, useCallback } from 'react';
import { getLogs } from '../services/api';
import './AuditLogs.css';

const API_URL = 'http://localhost:8080/api';

async function request(endpoint) {
  const token = localStorage.getItem('token');
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const response = await fetch(`${API_URL}${endpoint}`, { headers });
  const text = await response.text();
  let data;
  try { data = text ? JSON.parse(text) : {}; } catch { data = {}; }
  if (!response.ok) throw new Error(data.message || data.error || 'Something went wrong');
  return data;
}

function AuditLogs() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Filters
  const [moduleFilter, setModuleFilter] = useState('');
  const [severityFilter, setSeverityFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedLogId, setExpandedLogId] = useState(null);

  // Stats
  const [stats, setStats] = useState(null);

  useEffect(() => {
    request('/logs/stats')
      .then((data) => setStats(data))
      .catch(() => setStats(null));
  }, []);

  const fetchLogs = useCallback(async (pageNumber) => {
    setLoading(true);
    try {
      let endpoint;
      if (moduleFilter || severityFilter) {
        const params = new URLSearchParams();
        if (moduleFilter) params.set('module', moduleFilter);
        if (severityFilter) params.set('severity', severityFilter);
        params.set('page', pageNumber);
        params.set('size', '20');
        endpoint = `/logs/filter?${params.toString()}`;
      } else {
        endpoint = `/logs?page=${pageNumber}&size=20`;
      }
      const data = await request(endpoint);
      let content = data.content || [];
      // Client-side search filter
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        content = content.filter(
          (log) =>
            (log.action || '').toLowerCase().includes(q) ||
            (log.message || '').toLowerCase().includes(q) ||
            (log.module || '').toLowerCase().includes(q)
        );
      }
      setLogs(content);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [moduleFilter, severityFilter, searchQuery]);

  useEffect(() => {
    fetchLogs(page);
  }, [page, fetchLogs]);

  useEffect(() => {
    setPage(0);
  }, [moduleFilter, severityFilter, searchQuery]);

  function handlePrev() { if (page > 0) setPage(page - 1); }
  function handleNext() { if (page < totalPages - 1) setPage(page + 1); }
  function toggleExpand(id) { setExpandedLogId(expandedLogId === id ? null : id); }
  function clearFilters() {
    setModuleFilter('');
    setSeverityFilter('');
    setSearchQuery('');
    setPage(0);
  }

  const getSeverityClass = (severity) => {
    if (!severity) return '';
    const s = severity.toLowerCase();
    if (s === 'critical' || s === 'high') return 'critical';
    if (s === 'warning') return 'warning';
    return 'info';
  };

  const getSeverityIcon = (severity) => {
    if (!severity) return '📋';
    const s = severity.toLowerCase();
    if (s === 'critical' || s === 'high') return '🔴';
    if (s === 'warning') return '🟡';
    return '🔵';
  };

  const hasActiveFilters = moduleFilter || severityFilter || searchQuery;

  return (
    <div className="logs-page">
      {/* Header */}
      <div className="logs-header">
        <div className="logs-header-text">
          <h1>
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
              <polyline points="14,2 14,8 20,8" />
              <line x1="16" y1="13" x2="8" y2="13" />
              <line x1="16" y1="17" x2="8" y2="17" />
              <polyline points="10,9 9,9 8,9" />
            </svg>
            Audit Logs
          </h1>
          <p className="logs-subtitle">Monitor system activity, security events, and user actions</p>
        </div>
      </div>

      {/* Stats Cards */}
      {stats && (
        <div className="logs-stats-grid">
          <div className="logs-stat-card">
            <div className="logs-stat-icon total">📊</div>
            <div className="logs-stat-info">
              <span className="logs-stat-value">{stats.totalLogs?.toLocaleString() || 0}</span>
              <span className="logs-stat-label">Total Logs</span>
            </div>
          </div>
          <div className="logs-stat-card">
            <div className="logs-stat-icon security">🛡️</div>
            <div className="logs-stat-info">
              <span className="logs-stat-value">{stats.securityEvents?.toLocaleString() || 0}</span>
              <span className="logs-stat-label">Security Events</span>
            </div>
          </div>
          <div className="logs-stat-card">
            <div className="logs-stat-icon warning">⚠️</div>
            <div className="logs-stat-info">
              <span className="logs-stat-value">{stats.warningLogs?.toLocaleString() || 0}</span>
              <span className="logs-stat-label">Warnings</span>
            </div>
          </div>
          <div className="logs-stat-card">
            <div className="logs-stat-icon critical">🚨</div>
            <div className="logs-stat-info">
              <span className="logs-stat-value">{stats.criticalLogs?.toLocaleString() || 0}</span>
              <span className="logs-stat-label">Critical</span>
            </div>
          </div>
        </div>
      )}

      {/* Filter Bar */}
      <div className="logs-filter-bar">
        <div className="logs-search-wrapper">
          <svg className="logs-search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            type="text"
            className="logs-search"
            placeholder="Search logs by action, module, or message..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
        <div className="logs-filter-group">
          <select className="logs-filter-select" value={moduleFilter} onChange={(e) => setModuleFilter(e.target.value)}>
            <option value="">All Modules</option>
            <option value="authentication">Authentication</option>
            <option value="security">Security</option>
            <option value="user_management">User Management</option>
            <option value="project_management">Project Management</option>
            <option value="group_management">Group Management</option>
            <option value="submission">Submission</option>
            <option value="log_management">Log Management</option>
          </select>
          <select className="logs-filter-select" value={severityFilter} onChange={(e) => setSeverityFilter(e.target.value)}>
            <option value="">All Severity</option>
            <option value="info">Info</option>
            <option value="warning">Warning</option>
            <option value="critical">Critical</option>
            <option value="high">High</option>
          </select>
          {hasActiveFilters && (
            <button className="logs-clear-btn" onClick={clearFilters}>
              ✕ Clear
            </button>
          )}
        </div>
      </div>

      {error && <div className="logs-error">{error}</div>}

      {/* Results Info */}
      <div className="logs-results-info">
        <span>
          {loading ? 'Loading...' : `Showing ${logs.length} logs`}
          {totalElements > 0 && !hasActiveFilters && ` of ${totalElements} total`}
        </span>
      </div>

      {/* Table */}
      <div className="logs-table-wrapper">
        {loading && page === 0 ? (
          <div className="logs-table-loading">
            <div className="logs-spinner" />
            <span>Loading logs...</span>
          </div>
        ) : (
          <table className="logs-table">
            <thead>
              <tr>
                <th style={{width:'50px'}}>#</th>
                <th>Severity</th>
                <th>Action</th>
                <th>Module</th>
                <th>User</th>
                <th>IP Address</th>
                <th>Date & Time</th>
                <th style={{width:'50px'}}></th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <>
                  <tr key={log.id} className={`logs-row severity-row-${getSeverityClass(log.severity)}`} onClick={() => toggleExpand(log.id)}>
                    <td className="logs-id">{log.id}</td>
                    <td>
                      <span className={`severity-badge ${getSeverityClass(log.severity)}`}>
                        {getSeverityIcon(log.severity)} {log.severity}
                      </span>
                    </td>
                    <td className="logs-action">{log.action}</td>
                    <td>
                      <span className="logs-module-badge">{log.module}</span>
                    </td>
                    <td>{log.userId || <span className="logs-system-tag">System</span>}</td>
                    <td className="logs-ip">{log.ipAddress || '—'}</td>
                    <td className="logs-date">{new Date(log.createdAt).toLocaleString('tr-TR')}</td>
                    <td className="logs-expand-cell">
                      <span className={`logs-expand-icon ${expandedLogId === log.id ? 'expanded' : ''}`}>▾</span>
                    </td>
                  </tr>
                  {expandedLogId === log.id && (
                    <tr key={`detail-${log.id}`} className="logs-detail-row">
                      <td colSpan="8">
                        <div className="logs-detail-content">
                          <div className="logs-detail-grid">
                            <div className="logs-detail-item">
                              <span className="logs-detail-label">Message</span>
                              <span className="logs-detail-value">{log.message || 'No message'}</span>
                            </div>
                            <div className="logs-detail-item">
                              <span className="logs-detail-label">Status</span>
                              <span className="logs-detail-value">{log.status || '—'}</span>
                            </div>
                            <div className="logs-detail-item">
                              <span className="logs-detail-label">User Role</span>
                              <span className="logs-detail-value">{log.userRole || '—'}</span>
                            </div>
                            <div className="logs-detail-item">
                              <span className="logs-detail-label">Endpoint</span>
                              <span className="logs-detail-value">{log.endpoint || '—'}</span>
                            </div>
                            <div className="logs-detail-item">
                              <span className="logs-detail-label">HTTP Method</span>
                              <span className="logs-detail-value">{log.httpMethod || '—'}</span>
                            </div>
                            <div className="logs-detail-item">
                              <span className="logs-detail-label">User Agent</span>
                              <span className="logs-detail-value logs-detail-ua">{log.userAgent || '—'}</span>
                            </div>
                          </div>
                        </div>
                      </td>
                    </tr>
                  )}
                </>
              ))}
              {logs.length === 0 && !loading && (
                <tr>
                  <td colSpan="8" className="logs-empty-row">
                    <div className="logs-empty">
                      <span className="logs-empty-icon">🔍</span>
                      <span>No logs found matching your criteria.</span>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="pagination">
          <button onClick={handlePrev} disabled={page === 0}>
            ← Previous
          </button>
          <div className="pagination-info">
            <span className="pagination-current">Page {page + 1}</span>
            <span className="pagination-total">of {totalPages}</span>
          </div>
          <button onClick={handleNext} disabled={page >= totalPages - 1}>
            Next →
          </button>
        </div>
      )}
    </div>
  );
}

export default AuditLogs;
