package com.seniorapp.dto;

import java.util.ArrayList;
import java.util.List;

public class GroupInviteRespondResultDto {
    private boolean selectionRequired;
    private boolean autoAssigned;
    private String assignedCommitteeName;
    private Long assignedCommitteeId;
    private String message;
    private List<CommitteeOption> committeeOptions = new ArrayList<>();

    public boolean isSelectionRequired() {
        return selectionRequired;
    }

    public void setSelectionRequired(boolean selectionRequired) {
        this.selectionRequired = selectionRequired;
    }

    public boolean isAutoAssigned() {
        return autoAssigned;
    }

    public void setAutoAssigned(boolean autoAssigned) {
        this.autoAssigned = autoAssigned;
    }

    public String getAssignedCommitteeName() {
        return assignedCommitteeName;
    }

    public void setAssignedCommitteeName(String assignedCommitteeName) {
        this.assignedCommitteeName = assignedCommitteeName;
    }

    public Long getAssignedCommitteeId() {
        return assignedCommitteeId;
    }

    public void setAssignedCommitteeId(Long assignedCommitteeId) {
        this.assignedCommitteeId = assignedCommitteeId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<CommitteeOption> getCommitteeOptions() {
        return committeeOptions;
    }

    public void setCommitteeOptions(List<CommitteeOption> committeeOptions) {
        this.committeeOptions = committeeOptions;
    }

    public static class CommitteeOption {
        private Long committeeId;
        private String committeeName;
        private long assignedGroupCount;

        public Long getCommitteeId() {
            return committeeId;
        }

        public void setCommitteeId(Long committeeId) {
            this.committeeId = committeeId;
        }

        public String getCommitteeName() {
            return committeeName;
        }

        public void setCommitteeName(String committeeName) {
            this.committeeName = committeeName;
        }

        public long getAssignedGroupCount() {
            return assignedGroupCount;
        }

        public void setAssignedGroupCount(long assignedGroupCount) {
            this.assignedGroupCount = assignedGroupCount;
        }
    }
}
