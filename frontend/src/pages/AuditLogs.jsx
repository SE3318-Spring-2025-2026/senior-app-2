import { useState, useEffect, useCallback } from 'react';
import { getLogs } from '../services/api';
import './AuditLogs.css';

function AuditLogs() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Filtre State'leri
  const [filters, setFilters] = useState({
    module: '',
    severity: '',
    action: '',
    status: '',
    ipAddress: ''
  });

  const fetchLogs = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getLogs(page, 20, filters);
      setLogs(data.content || []);
      setTotalPages(data.totalPages || 0);
      setError('');
    } catch (err) {
      setError('Loglar yüklenirken bir hata oluştu.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [page, filters]);

  useEffect(() => {
    const handler = setTimeout(() => {
      fetchLogs();
    }, 400); // Debounce
    return () => clearTimeout(handler);
  }, [fetchLogs]);

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters(prev => ({ ...prev, [name]: value }));
    setPage(0); // Filtre değişince ilk sayfaya dön
  };

  return (
    <div className="logs-page">
      <div className="logs-header">
        <h1>Audit Logs</h1>
        {loading && <span className="loading-spinner">Yükleniyor...</span>}
      </div>

      {error && <div className="logs-error">{error}</div>}

      <div className="logs-table-wrapper">
        <table className="logs-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Action</th>
              <th>Module</th>
              <th>Severity</th>
              <th>Status</th>
              <th>IP Address</th>
              <th>Date</th>
            </tr>
            <tr className="filter-row">
              <td></td>
              <td><input name="action" placeholder="Filtrele..." value={filters.action} onChange={handleFilterChange} /></td>
              <td><input name="module" placeholder="Filtrele..." value={filters.module} onChange={handleFilterChange} /></td>
              <td>
                <select name="severity" value={filters.severity} onChange={handleFilterChange}>
                  <option value="">Hepsi</option>
                  <option value="info">Info</option>
                  <option value="warning">Warning</option>
                  <option value="critical">Critical</option>
                </select>
              </td>
              <td><input name="status" placeholder="Filtrele..." value={filters.status} onChange={handleFilterChange} /></td>
              <td><input name="ipAddress" placeholder="IP Ara..." value={filters.ipAddress} onChange={handleFilterChange} /></td>
              <td></td>
            </tr>
          </thead>
          <tbody>
            {logs.length > 0 ? logs.map((log) => (
              <tr key={log.id}>
                <td className="id-col">#{log.id}</td>
                <td><strong>{log.action}</strong></td>
                <td>{log.module}</td>
                <td>
                  <span className={`severity-badge ${log.severity?.toLowerCase()}`}>
                    {log.severity}
                  </span>
                </td>
                <td>{log.status}</td>
                <td>{log.ipAddress || '-'}</td>
                <td>{new Date(log.createdAt).toLocaleString()}</td>
              </tr>
            )) : !loading && <tr><td colSpan="7" className="no-data">Kayıt bulunamadı.</td></tr>}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="pagination">
          <button onClick={() => setPage(p => p - 1)} disabled={page === 0}>Önceki</button>
          <span>{page + 1} / {totalPages}</span>
          <button onClick={() => setPage(p => p + 1)} disabled={page >= totalPages - 1}>Sonraki</button>
        </div>
      )}
    </div>
  );
}

export default AuditLogs;