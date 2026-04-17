package com.seniorapp.repository;

import com.seniorapp.entity.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
    
    // Grup adı kontrolü için
    boolean existsByGroupName(String groupName);

    // Öğrenci zaten bir grubun üyesi mi veya lideri mi kontrolü için
    boolean existsByMembersIdOrTeamLeaderId(Long memberId, Long leaderId);
}