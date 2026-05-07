import { useAuth } from '../context/AuthContext';
import GroupMemberManagement from '../components/GroupMemberManagement';
import './GroupManagement.css';

function GroupManagement() {
  const { user } = useAuth();

  // Get group ID from route params or user data
  // For now using a placeholder - adjust based on your routing strategy
  const groupId = 'grp-98765'; // This should come from params or user context

  const isTeamLeader = user?.role === 'TEAM_LEADER';

  if (!isTeamLeader) {
    return (
      <div className="group-management-page">
        <div className="alert alert-error">
          You don't have permission to access this page. Only Team Leaders can manage groups.
        </div>
      </div>
    );
  }

  return (
    <div className="group-management-page">
      <GroupMemberManagement groupId={groupId} showTitle={true} />
    </div>
  );
}

export default GroupManagement;
