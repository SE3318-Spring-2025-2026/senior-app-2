import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TeamManagement from '../pages/TeamManagement';
import * as api from '../services/api';

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    user: { role: 'STUDENT', fullName: 'Test Student', email: 'student@test.com' },
  }),
}));

vi.mock('../services/api', () => ({
  createProjectFromTemplateForTeam: vi.fn(),
  createTeam: vi.fn(),
  deleteGroup: vi.fn(),
  getMyTeams: vi.fn(),
  getProjectTemplates: vi.fn(),
  inviteStudentToTeam: vi.fn(),
  listAdvisorOptionsForTeam: vi.fn(),
  listStudentsForInvite: vi.fn(),
}));

describe('TeamManagement Page - Delete Group', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.listStudentsForInvite.mockResolvedValue({ data: [] });
    api.getProjectTemplates.mockResolvedValue({ data: [] });
  });

  it('shows delete button only for current user leader and deletes group after confirmation', async () => {
    const user = userEvent.setup();
    api.getMyTeams.mockResolvedValue({
      data: [
        {
          groupId: 1,
          groupName: 'My Group',
          currentUserLeader: true,
          members: [],
          project: null,
        },
        {
          groupId: 2,
          groupName: 'Other Group',
          currentUserLeader: false,
          members: [],
          project: null,
        },
      ],
    });
    api.deleteGroup.mockResolvedValue({ message: 'ok' });

    render(<TeamManagement />);

    await waitFor(() => {
      expect(screen.getByText('My Group')).toBeInTheDocument();
      expect(screen.getByText('Other Group')).toBeInTheDocument();
    });

    const deleteButtons = screen.getAllByText('Delete');
    expect(deleteButtons).toHaveLength(1);

    await user.click(deleteButtons[0]);
    expect(screen.getByText('Delete Group')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Delete Group' }));

    await waitFor(() => {
      expect(api.deleteGroup).toHaveBeenCalledWith(1);
    });
  });

  it('shows error message when delete fails', async () => {
    const user = userEvent.setup();
    api.getMyTeams.mockResolvedValue({
      data: [
        {
          groupId: 1,
          groupName: 'My Group',
          currentUserLeader: true,
          members: [],
          project: null,
        },
      ],
    });
    api.deleteGroup.mockRejectedValue(new Error('Delete failed'));

    render(<TeamManagement />);

    await waitFor(() => {
      expect(screen.getByText('My Group')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Delete'));
    await user.click(screen.getByRole('button', { name: 'Delete Group' }));

    await waitFor(() => {
      expect(screen.getByText('Delete failed')).toBeInTheDocument();
    });
  });
});
