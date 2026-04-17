package com.seniorapp.repository;

import com.seniorapp.entity.User;
import com.seniorapp.entity.UserGroup;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {

    Optional<UserGroup> findByTeamLeader(User teamLeader);

    boolean existsByGroupName(String groupName);
}
