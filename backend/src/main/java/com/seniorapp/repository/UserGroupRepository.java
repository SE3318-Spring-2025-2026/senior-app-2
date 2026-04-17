package com.seniorapp.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.seniorapp.entity.User;
import com.seniorapp.entity.UserGroup;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {

    /** Find a group by its team leader. */
    Optional<UserGroup> findByTeamLeader(User teamLeader);

    /** Find groups where a user is a member. */
    @Query("SELECT ug FROM UserGroup ug JOIN ug.members m WHERE m = :user")
    Optional<UserGroup> findByMember(@Param("user") User user);

    /** Find groups where a user is either team leader or a member. */
    @Query("SELECT ug FROM UserGroup ug WHERE ug.teamLeader = :user OR :user IN (ug.members)")
    Optional<UserGroup> findByTeamLeaderOrMember(@Param("user") User user);
}
