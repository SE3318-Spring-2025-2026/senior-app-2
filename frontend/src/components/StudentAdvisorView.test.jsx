import { describe, test, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom'; 
import StudentAdvisorView from './StudentAdvisorView';

describe('StudentAdvisorView Component Tests', () => {
  
  test('TEST 1: Proje şablonu (projectId) seçilmediyse kilit ekranı gelmeli', () => {
    render(<StudentAdvisorView groupId="grp-98765" />);
    
    // ÇÖZÜM: Cümlenin ortasındaki <strong> etiketi testi bozduğu için, direkt başlığı arıyoruz!
    const warningTitle = screen.getByText(/Action Required/i);
    expect(warningTitle).toBeInTheDocument();
  });

  test('TEST 2: Proje şablonu (projectId) verildiyse dropdown ve hocalar gelmeli', () => {
    render(<StudentAdvisorView groupId="grp-98765" projectId="proj-se1111" />);
    
    const dropdownLabel = screen.getByText(/Select Committee Professor/i);
    const projectInfoText = screen.getByText(/Selected Project: proj-se1111/i);
    
    expect(dropdownLabel).toBeInTheDocument();
    expect(projectInfoText).toBeInTheDocument();
  });

});