import { useState } from 'react';
import './InviteCard.css';

const STATUS = {
  IDLE: 'idle',
  LOADING: 'loading',
  SUCCESS: 'success',
  ERROR: 'error',
  EXPIRED: 'expired',
};

function InviteCard({ invite, onRespond, inviterName }) {
  const [status, setStatus] = useState(STATUS.IDLE);
  const [errorMessage, setErrorMessage] = useState('');

  const formatDate = (dateString) => {
    if (!dateString) return 'Unknown';
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);
  };

  const getTimeAgo = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    const now = new Date();
    const diffInHours = Math.floor((now - date) / (1000 * 60 * 60));

    if (diffInHours < 1) return 'Just now';
    if (diffInHours < 24) return `${diffInHours}h ago`;
    const diffInDays = Math.floor(diffInHours / 24);
    if (diffInDays < 7) return `${diffInDays}d ago`;
    return formatDate(dateString);
  };

  const handleAction = async (action) => {
    setStatus(STATUS.LOADING);
    setErrorMessage('');

    try {
      await onRespond(invite.inviteId, action);
      setStatus(action === 'ACCEPT' ? STATUS.SUCCESS : STATUS.IDLE);
    } catch (error) {
      const message = error?.message || 'Failed to process invitation';
      if (message.toLowerCase().includes('expired')) {
        setStatus(STATUS.EXPIRED);
      } else {
        setStatus(STATUS.ERROR);
        setErrorMessage(message);
      }
    }
  };

  const renderContent = () => {
    switch (status) {
      case STATUS.SUCCESS:
        return (
          <div className="invite-state success-state">
            <div className="state-icon">&#10003;</div>
            <h4>You&apos;ve joined {invite.groupName}!</h4>
            <p>You are now a member of this team.</p>
          </div>
        );

      case STATUS.EXPIRED:
        return (
          <div className="invite-state expired-state">
            <div className="state-icon">&#x23F0;</div>
            <h4>Invitation Expired</h4>
            <p>This invitation is no longer valid. Please ask the team leader to send a new invite.</p>
          </div>
        );

      case STATUS.ERROR:
        return (
          <div className="invite-state error-state">
            <div className="state-icon">&#9888;</div>
            <h4>Something went wrong</h4>
            <p>{errorMessage}</p>
            <button
              className="invite-retry-btn"
              onClick={() => setStatus(STATUS.IDLE)}
            >
              Try Again
            </button>
          </div>
        );

      default:
        return (
          <>
            <div className="invite-header">
              <div className="invite-avatar">
                {invite.groupName?.charAt(0)?.toUpperCase() || 'G'}
              </div>
              <div className="invite-meta">
                <h4 className="invite-title">{invite.groupName}</h4>
                <span className="invite-subtitle">Group #{invite.groupId}</span>
              </div>
              <span className="invite-time" title={formatDate(invite.invitedAt)}>
                {getTimeAgo(invite.invitedAt)}
              </span>
            </div>

            <div className="invite-details">
              <div className="detail-row">
                <span className="detail-label">Invited by</span>
                <span className="detail-value">{inviterName || `User #${invite.invitedByUserId}`}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Sent</span>
                <span className="detail-value">{formatDate(invite.invitedAt)}</span>
              </div>
            </div>

            <div className="invite-actions-bar">
              <button
                className="invite-btn invite-btn-decline"
                onClick={() => handleAction('DECLINE')}
                disabled={status === STATUS.LOADING}
              >
                {status === STATUS.LOADING ? (
                  <span className="btn-spinner" />
                ) : (
                  'Decline'
                )}
              </button>
              <button
                className="invite-btn invite-btn-accept"
                onClick={() => handleAction('ACCEPT')}
                disabled={status === STATUS.LOADING}
              >
                {status === STATUS.LOADING ? (
                  <>
                    <span className="btn-spinner" />
                    Processing...
                  </>
                ) : (
                  'Accept Invitation'
                )}
              </button>
            </div>
          </>
        );
    }
  };

  return (
    <div className={`invite-card ${status !== STATUS.IDLE && status !== STATUS.LOADING ? 'invite-card-state' : ''} ${status}`}>
      {renderContent()}
    </div>
  );
}

export default InviteCard;
export { STATUS };
