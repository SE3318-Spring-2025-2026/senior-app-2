// Vitest importları eklendi
import { describe, test, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import AdminTransferPanel from './AdminTransferPanel';

describe('AdminTransferPanel Component Tests', () => {
  
  test('TEST 1: Admin ekranında gerekli inputlar ve zorunlu transfer butonu olmalı', () => {
    render(<AdminTransferPanel />);
    
    // Ekranda input kutularını ve kırmızı butonu arıyoruz
    const groupIdInput = screen.getByPlaceholderText(/e.g., grp-98765/i);
    const profIdInput = screen.getByPlaceholderText(/e.g., prof-505/i);
    const forceButton = screen.getByText(/Force Transfer Group/i);
    
    // Beklenti: Hepsi ekranda hazır kıta bekliyor OLMALI
    expect(groupIdInput).toBeInTheDocument();
    expect(profIdInput).toBeInTheDocument();
    expect(forceButton).toBeInTheDocument();
  });

});