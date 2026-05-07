import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import GroupMembers from './GroupMembers';
import AddGroupMember from './AddGroupMember';
import './GroupMembers.css';

/**
 * GroupMemberManagement - Container component for managing group members
 * @param {string} groupId - The ID of the group to manage
 * @param {boolean} [showTitle=true] - Whether to show the component title
 */
function GroupMemberManagement({ groupId, showTitle = true }) {
  const { user } = useAuth();
  const [groupData, setGroupData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const isTeamLeader = user?.role === 'TEAM_LEADER';

  // Note: In a real scenario, you would fetch group data from API
  // For now, we're using local state management
  useEffect(() => {
    // Simulate loading group data
    const loadGroupData = async () => {
      try {
        // TODO: Uncomment when backend is ready
        // const data = await getGroup(groupId);
        // setGroupData(data);

        // Placeholder data for demonstration
        setGroupData({
          groupId,
          groupName: 'Tech Titans',
          teamLeader: user?.studentId,
          members: ['std-12345', 'std-54321', 'std-99999'],
        });
      } catch (err) {
        setError(err.message || 'Failed to load group data');
      } finally {
        setLoading(false);
      }
    };

    loadGroupData();
  }, [groupId, user?.studentId]);

  const handleMemberRemoved = (studentId) => {
    setGroupData((prev) => ({
      ...prev,
      members: prev.members.filter((id) => id !== studentId),
    }));
  };

  const handleMemberAdded = (studentId) => {
    setGroupData((prev) => ({
      ...prev,
      members: [...prev.members, studentId],
    }));
  };

  if (loading) {
    return <div className="group-management-loading">Loading group data...</div>;
  }

  if (error) {
    return <div className="alert alert-error">{error}</div>;
  }

  if (!groupData) {
    return <div className="alert alert-error">Group not found</div>;
  }

  return (
    <div className="group-management-container">
      {showTitle && (
        <div className="group-management-header">
          <h2>Group: {groupData.groupName}</h2>
          {isTeamLeader && (
            <span className="role-badge">Team Leader</span>
          )}
        </div>
      )}

      <div className="group-management-content">
        <GroupMembers
          groupId={groupId}
          members={groupData.members}
          isTeamLeader={isTeamLeader}
          onMemberRemoved={handleMemberRemoved}
        />

        <AddGroupMember
          groupId={groupId}
          isTeamLeader={isTeamLeader}
          onMemberAdded={handleMemberAdded}
        />
      </div>
    </div>
  );
}

export default GroupMemberManagement;
