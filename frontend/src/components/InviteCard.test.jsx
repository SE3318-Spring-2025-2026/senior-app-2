import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import InviteCard from '../components/InviteCard';

describe('InviteCard Component', () => {
  const mockInvite = {
    inviteId: 1,
    groupId: 101,
    groupName: 'Senior Project Team A',
    invitedByUserId: 5,
    invitedAt: '2025-05-01T10:30:00',
  };

  const mockOnRespond = vi.fn();

  it('renders invite card with group details', () => {
    render(
      <InviteCard
        invite={mockInvite}
        onRespond={mockOnRespond}
        inviterName="John Doe"
      />
    );

    expect(screen.getByText('Senior Project Team A')).toBeInTheDocument();
    expect(screen.getByText('Group #101')).toBeInTheDocument();
    expect(screen.getByText('John Doe')).toBeInTheDocument();
  };

  it('displays group avatar with first letter', () => {
    render(
      <InviteCard
        invite={mockInvite}
        onRespond={mockOnRespond}
      />
    );

    expect(screen.getByText('S')).toBeInTheDocument();
  });

  it('shows formatted date for invitation', () => {
    render(
      <InviteCard
        invite={mockInvite}
        onRespond={mockOnRespond}
      />
    );

    expect(screen.getByText(/May 1, 2025/)).toBeInTheDocument();
  });

  it('renders accept and decline buttons', () => {
    render(
      <InviteCard
        invite={mockInvite}
        onRespond={mockOnRespond}
      />
    );

    expect(screen.getByText('Accept Invitation')).toBeInTheDocument();
    expect(screen.getByText('Decline')).toBeInTheDocument();
  });

  it('calls onRespond with ACCEPT when accept button clicked', async () => {
    const user = userEvent.setup();
    mockOnRespond.mockResolvedValueOnce();

    render(
      <InviteCard
        invite={mockInvite}
        onRespond={mockOnRespond}
      />
    );

    const acceptButton = screen.getByText('Accept Invitation');
    await user.click(acceptButton);

    await waitFor(() => {
      expect(mockOnRespond).toHaveBeenCalledWith(1, 'ACCEPT');
    });
  });

  it('calls onRespond with DECLINE when decline button clicked', async () => {
    const user = userEvent.setup();
    mockOnRespond.mockResolvedValueOnce();

    render(
      <InviteCard
        invite={mockInvite}
        onRespond={mockOnRespond}
      />
    );

    const declineButton = screen.getByText('Decline');
    await user.click(declineButton);

    await waitFor(() => {
      expect(mockOnRespond).toHaveBeenCalledWith(1, 'DECLINE');
    });
  });

  it('shows loading state during invite processing', async () => {
    const user = userEvent.setup();
    mockOnRespond.mockImplementation(() => new Promise(() => {})); // Never resolves

    render(
      <InviteCard
        invite={mockInvite}
        onRespond={mockOnRespond}
      />
    );

    const acceptButton = screen.getByText('Accept Invitation');
    await user.click(acceptButton);

    await waitFor(() => {
      expect(screen.getByText('Processing...')).toBeInTheDocument();
    });
  });

  it('displays success state after accepting invitation', async () => {
    const user = userEvent.setup();
    mockOnRespond.mockResolvedValueOnce();

    render(
      <InviteCard
        invite={mockInvite}
        onRespond={mockOnRespond}
      />
    );

    const acceptButton = screen.getByText('Accept Invitation');
    await user.click(acceptButton);

    await waitFor(() => {
      expect(screen.getByText("You've joined Senior Project Team A!")).toBeInTheDocument();
    });
  });

  it('shows error state when invitation fails', async () => {
    const user = userEvent.setup();
    mockOnRespond.mockRejectedValueOnce(new Error('Network error'));

    render(
      <InviteCard
        invite={mockInvite}
        onRespond={mockOnRespond}
      />
    );

    const acceptButton = screen.getByText('Accept Invitation');
    await user.click(acceptButton);

    await waitFor(() => {
      expect(screen.getByText('Something went wrong')).toBeInTheDocument();
      expect(screen.getByText('Network error')).toBeInTheDocument();
    });
  });

  it('shows expired state for expired invitations', async () => {
    const user = userEvent.setup();
    mockOnRespond.mockRejectedValueOnce(new Error('Invitation has expired'));

    render(
      <InviteCard
        invite={mockInvite}
        onRespond={mockOnRespond}
      />
    );

    const acceptButton = screen.getByText('Accept Invitation');
    await user.click(acceptButton);

    await waitFor(() => {
      expect(screen.getByText('Invitation Expired')).toBeInTheDocument();
      expect(screen.getByText('This invitation is no longer valid')).toBeInTheDocument();
    });
  });

  it('allows retry after error state', async () => {
    const user = userEvent.setup();
    mockOnRespond.mockRejectedValueOnce(new Error('Network error'));

    render(
      <InviteCard
        invite={mockInvite}
        onRespond={mockOnRespond}
      />
    );

    const acceptButton = screen.getByText('Accept Invitation');
    await user.click(acceptButton);

    await waitFor(() => {
      expect(screen.getByText('Try Again')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Try Again'));

    await waitFor(() => {
      expect(screen.getByText('Accept Invitation')).toBeInTheDocument();
    });
  });

  it('displays fallback for unknown inviter', () => {
    render(
      <InviteCard
        invite={mockInvite}
        onRespond={mockOnRespond}
      />
    );

    expect(screen.getByText('User #5')).toBeInTheDocument();
  });

  it('handles missing invitation date gracefully', () => {
    const inviteWithoutDate = {
      ...mockInvite,
      invitedAt: null,
    };

    render(
      <InviteCard
        invite={inviteWithoutDate}
        onRespond={mockOnRespond}
      />
    );

    expect(screen.getByText('Unknown')).toBeInTheDocument();
  });

  it('disables buttons during loading', async () => {
    const user = userEvent.setup();
    mockOnRespond.mockImplementation(() => new Promise(() => {}));

    render(
      <InviteCard
        invite={mockInvite}
        onRespond={mockOnRespond}
      />
    );

    const acceptButton = screen.getByText('Accept Invitation');
    await user.click(acceptButton);

    await waitFor(() => {
      expect(acceptButton).toBeDisabled();
    });
  });

  it('applies correct CSS classes for state transitions', async () => {
    const user = userEvent.setup();
    mockOnRespond.mockResolvedValueOnce();

    const { container } = render(
      <InviteCard
        invite={mockInvite}
        onRespond={mockOnRespond}
      />
    );

    // Initial state
    expect(container.firstChild).toHaveClass('invite-card');

    const acceptButton = screen.getByText('Accept Invitation');
    await user.click(acceptButton);

    // Success state
    await waitFor(() => {
      expect(container.firstChild).toHaveClass('success');
    });
  });
});
