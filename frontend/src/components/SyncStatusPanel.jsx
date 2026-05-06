import { useState, useEffect } from 'react';
import { triggerSync, getSyncStatus } from '../services/api';

export default function SyncStatusPanel({ groupId }) {
    const [status, setStatus] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSync = async () => {
        try {
            setLoading(true);
            setStatus('Loading...');
            
            // 1. Senkronizasyonu başlat ve jobId al
            const response = await triggerSync({ groupId, source: 'BOTH' });
            const jobId = response.jobId;

            // 2. Polling: Her 2 saniyede bir durumu kontrol et
            const interval = setInterval(async () => {
                try {
                    const statusRes = await getSyncStatus(jobId);
                    setStatus(statusRes.status);

                    if (statusRes.status === 'COMPLETED' || statusRes.status === 'FAILED') {
                        clearInterval(interval);
                        setLoading(false);
                    }
                } catch (err) {
                    clearInterval(interval);
                    setLoading(false);
                    setStatus('ERROR');
                }
            }, 2000);
        } catch (error) {
            setLoading(false);
            setStatus('ERROR');
        }
    };

    return (
        <div className="sync-panel" style={{ padding: '20px', border: '1px solid #ccc', borderRadius: '8px', marginTop: '20px' }}>
            <h3>Manual Data Sync</h3>
            <p>Pull latest changes from GitHub and Jira.</p>
            <button 
                onClick={handleSync} 
                disabled={loading}
                style={{ padding: '10px 15px', backgroundColor: loading ? '#ccc' : '#007bff', color: '#fff', border: 'none', borderRadius: '4px', cursor: loading ? 'not-allowed' : 'pointer' }}
            >
                {loading ? 'Syncing...' : 'Start Sync'}
            </button>
            {status && (
                <p style={{ marginTop: '10px', fontWeight: 'bold' }}>
                    Status: <span data-testid="sync-status-text">{status}</span>
                </p>
            )}
        </div>
    );
}