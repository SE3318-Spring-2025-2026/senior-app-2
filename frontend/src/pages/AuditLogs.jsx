import { useState, useEffect } from 'react';
import { getLogs } from '../services/api';
import './AuditLogs.css';

function AuditLogs() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    fetchLogs(page);
  }, [page]);

  async function fetchLogs(pageNumber) {
    setLoading(true);
    try {
      const data = await getLogs(pageNumber, 20);
      setLogs(data.content || []);
      setTotalPages(data.totalPages || 0);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function handlePrev() {
    if (page > 0) setPage(page - 1);
  }

  function handleNext() {
    if (page < totalPages - 1) setPage(page + 1);
  }

  if (loading && page === 0) return <div className="logs-loading">Loading logs...</div>;

  return (
    <div className="logs-page">
      <h1>Audit Logs</h1>
      {error && <div className="logs-error">{error}</div>}

      <div className="logs-table-wrapper">
        <table className="logs-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Action</th>
              <th>User ID</th>
              <th>Module</th>
              <th>Severity</th>
              <th>IP Address</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            {logs.map((log) => (
              <tr key={log.id} className={`severity-${log.severity?.toLowerCase()}`}>
                <td>{log.id}</td>
                <td>{log.action}</td>
                <td>{log.userId || 'System'}</td>
                <td>{log.module}</td>
                <td>
                  <span className={`severity-badge ${log.severity?.toLowerCase()}`}>
                    {log.severity}
                  </span>
                </td>
                <td>{log.ipAddress || '-'}</td>
                <td>{new Date(log.createdAt).toLocaleString()}</td>
              </tr>
            ))}
            {logs.length === 0 && (
              <tr>
                <td colSpan="7" style={{ textAlign: 'center' }}>No logs found.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="pagination">
          <button onClick={handlePrev} disabled={page === 0}>Previous</button>
          <span>Page {page + 1} of {totalPages}</span>
          <button onClick={handleNext} disabled={page >= totalPages - 1}>Next</button>
        </div>
      )}
    </div>
  );
}

export default AuditLogs;
