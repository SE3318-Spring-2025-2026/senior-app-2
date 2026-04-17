package com.seniorapp.repository;

import com.seniorapp.entity.User;
import com.seniorapp.entity.UserGroup;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {

    Optional<UserGroup> findByTeamLeader(User teamLeader);

    @Query("SELECT ug FROM UserGroup ug JOIN ug.members m WHERE m = :user")
    Optional<UserGroup> findByMember(@Param("user") User user);

    @Query("SELECT ug FROM UserGroup ug WHERE ug.teamLeader = :user OR :user IN (ug.members)")
    Optional<UserGroup> findByTeamLeaderOrMember(@Param("user") User user);

    boolean existsByGroupName(String groupName);

    boolean existsByMembersIdOrTeamLeaderId(Long memberId, Long leaderId);
}
