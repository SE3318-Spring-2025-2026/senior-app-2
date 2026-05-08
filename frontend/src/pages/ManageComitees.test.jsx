import { beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
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
    cleanup();
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

  it('lists professors/coordinators in select box and allows assigning a coordinator', async () => {
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

    const select = screen.getByRole('combobox');
    expect(select).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Prof User (prof@test.com)' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Coord User (coord@test.com)' })).toBeInTheDocument();

    fireEvent.change(select, { target: { value: '1002' } });
    fireEvent.click(screen.getByRole('button', { name: 'Add member' }));

    await waitFor(() => {
      expect(api.addProfessorToTemplateCommittee).toHaveBeenCalledWith('1', 10, 1002);
    });
  });

  it('prevents adding more than five members from popup select flow', async () => {
    api.getTemplateCommittees.mockResolvedValueOnce({
      data: [{
        committeeId: 10,
        name: 'Committee A',
        professors: [
          { userId: 1, fullName: 'Member 1', email: 'member1@test.com' },
          { userId: 2, fullName: 'Member 2', email: 'member2@test.com' },
          { userId: 3, fullName: 'Member 3', email: 'member3@test.com' },
          { userId: 4, fullName: 'Member 4', email: 'member4@test.com' },
          { userId: 5, fullName: 'Member 5', email: 'member5@test.com' },
        ],
      }],
    });

    render(
      <MemoryRouter initialEntries={['/panel/templates/1/manage-comitees']}>
        <Routes>
          <Route path="/panel/templates/:templateId/manage-comitees" element={<ManageComitees />} />
        </Routes>
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(api.getTemplateCommittees).toHaveBeenCalledWith('1');
    });

    fireEvent.click(screen.getByTitle('Settings'));
    expect(screen.getByText('5/5 members assigned')).toBeInTheDocument();

    const select = screen.getByRole('combobox');
    expect(select).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Add member' })).toBeDisabled();
    expect(api.addProfessorToTemplateCommittee).not.toHaveBeenCalled();
  });
});
