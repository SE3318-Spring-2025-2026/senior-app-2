import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import SyncStatusPanel from './SyncStatusPanel';
import * as apiService from '../services/api';

vi.mock('../services/api');

describe('SyncStatusPanel UI Polling', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        cleanup();
        document.body.innerHTML = '';
        // Sahte zamanlayıcıları tamamen devre dışı bırakıyoruz, gerçek zamanla gidelim
        vi.useRealTimers();
    });

    it('should show loading and eventually COMPLETED', async () => {
        apiService.triggerSync.mockResolvedValue({ jobId: 'job-123' });
        
        // Mock cevaplarını hazırlıyoruz
        apiService.getSyncStatus
            .mockResolvedValueOnce({ status: 'IN_PROGRESS' })
            .mockResolvedValueOnce({ status: 'COMPLETED' });

        render(<SyncStatusPanel groupId="test-group" />);
        
        const syncButton = screen.getByRole('button', { name: /start sync/i });
        fireEvent.click(syncButton);

        // 1. Loading yazısını hemen kontrol et
        await waitFor(() => {
            expect(screen.queryByText(/loading/i)).toBeTruthy();
        });

        // 2. İlk polling: IN_PROGRESS (Bileşendeki 2 saniyelik intervali bekliyoruz)
        // Timeout süresini 3 saniye yapıyoruz ki gerçek 2 saniye rahatça geçsin
        await waitFor(() => {
            const statusText = screen.getByTestId('sync-status-text');
            expect(statusText.textContent).toBe('IN_PROGRESS');
        }, { timeout: 4000 });

        // 3. İkinci polling: COMPLETED
        await waitFor(() => {
            const statusText = screen.getByTestId('sync-status-text');
            expect(statusText.textContent).toBe('COMPLETED');
            expect(syncButton.disabled).toBe(false);
        }, { timeout: 4000 });

    }, 15000); // Toplam test süresi sınırı

    it('should display ERROR when trigger fails', async () => {
        apiService.triggerSync.mockRejectedValue(new Error('API Fail'));

        render(<SyncStatusPanel groupId="test-group" />);
        
        fireEvent.click(screen.getByRole('button', { name: /start sync/i }));

        const statusText = await screen.findByTestId('sync-status-text', {}, { timeout: 3000 });
        expect(statusText.textContent).toBe('ERROR');
    });
});