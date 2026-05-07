// Vitest importları eklendi
import { describe, test, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import ProfessorDashboard from './ProfessorDashboard';

describe('ProfessorDashboard Component Tests', () => {
  
  test('TEST 1: Gelen isteklerin üzerinde Proje Adı (Template) gözükmeli', () => {
    render(<ProfessorDashboard />);
    
    // Ekranda "SE 1111 Graduation Project" yazısını ve "Tech Titans" grubunu arıyoruz
    const projectName = screen.getByText(/Project: SE 1111 Graduation Project/i);
    const groupName = screen.getByText(/Tech Titans/i);
    
    // Beklenti: İkisi de ekranda kabak gibi OLMALI
    expect(projectName).toBeInTheDocument();
    expect(groupName).toBeInTheDocument();
  });

});