package com.seniorapp.dto;

import com.seniorapp.entity.GroupInviteStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdvisorRequestDto {
    private Long id;
    private Long groupId;
    private String groupName;
    private String professorName;
    private GroupInviteStatus status;
    private LocalDateTime createdAt;
}