package com.seniorapp.repository;

import com.seniorapp.entity.AdvisorRequest;
import com.seniorapp.entity.GroupInviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdvisorRequestRepository extends JpaRepository<AdvisorRequest, Long> {
    List<AdvisorRequest> findAllByGroupIdAndStatus(Long groupId, GroupInviteStatus status);
    List<AdvisorRequest> findAllByProfessorIdAndStatus(Long professorId, GroupInviteStatus status);
    List<AdvisorRequest> findAllByStatus(GroupInviteStatus status); // Bunu ekle
}