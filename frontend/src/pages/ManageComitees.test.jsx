import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import ManageComitees from './ManageComitees';
import * as api from '../services/api';

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    user: { role: 'COORDINATOR' },
  }),
}));

vi.mock('../services/api', () => ({
  addProfessorToTemplateCommittee: vi.fn(),
  createTemplateCommittee: vi.fn(),
  deleteTemplateCommittee: vi.fn(),
  getTemplateCommittees: vi.fn(),
  getTemplateProfessors: vi.fn(),
  removeProfessorFromTemplateCommittee: vi.fn(),
}));

describe('ManageComitees', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.getTemplateCommittees.mockResolvedValue({
      data: [{ committeeId: 10, name: 'Committee A', professors: [] }],
    });
    api.getTemplateProfessors.mockResolvedValue({
      data: [
        { userId: 1001, fullName: 'Prof User', email: 'prof@test.com' },
        { userId: 1002, fullName: 'Coord User', email: 'coord@test.com' },
      ],
    });
    api.addProfessorToTemplateCommittee.mockResolvedValue({
      data: {
        committeeId: 10,
        name: 'Committee A',
        professors: [{ userId: 1002, fullName: 'Coord User', email: 'coord@test.com' }],
      },
    });
  });

  it('lists professors/coordinators and allows assigning a coordinator', async () => {
    render(
      <MemoryRouter initialEntries={['/panel/templates/1/manage-comitees']}>
        <Routes>
          <Route path="/panel/templates/:templateId/manage-comitees" element={<ManageComitees />} />
        </Routes>
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(api.getTemplateCommittees).toHaveBeenCalledWith('1');
      expect(api.getTemplateProfessors).toHaveBeenCalled();
    });

    fireEvent.click(screen.getByTitle('Settings'));

    expect(screen.getByText('Prof User')).toBeInTheDocument();
    expect(screen.getByText('Coord User')).toBeInTheDocument();

    const coordinatorRow = screen.getByText('Coord User').closest('.popup-row');
    fireEvent.click(coordinatorRow.querySelector('button'));

    await waitFor(() => {
      expect(api.addProfessorToTemplateCommittee).toHaveBeenCalledWith('1', 10, 1002);
    });
  });
});
