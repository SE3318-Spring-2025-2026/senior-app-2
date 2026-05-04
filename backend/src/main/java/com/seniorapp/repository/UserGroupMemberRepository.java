package com.seniorapp.repository;

import com.seniorapp.entity.GroupInviteStatus;
import com.seniorapp.entity.UserGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserGroupMemberRepository extends JpaRepository<UserGroupMember, Long> {
    boolean existsByUserIdAndStatus(Long userId, GroupInviteStatus status);

    boolean existsByGroupIdAndUserIdAndStatus(Long groupId, Long userId, GroupInviteStatus status);

    List<UserGroupMember> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, GroupInviteStatus status);

    List<UserGroupMember> findByGroupIdAndStatus(Long groupId, GroupInviteStatus status);

    List<UserGroupMember> findByGroupIdInAndStatus(List<Long> groupIds, GroupInviteStatus status);

    Optional<UserGroupMember> findByIdAndUserId(Long id, Long userId);

    Optional<UserGroupMember> findByGroupIdAndUserIdAndStatus(Long groupId, Long userId, GroupInviteStatus status);

    List<UserGroupMember> findByGroupIdOrderByCreatedAtAsc(Long groupId);

    boolean existsByGroupIdAndUserIdAndStatusIn(Long groupId, Long userId, List<GroupInviteStatus> statuses);
}
