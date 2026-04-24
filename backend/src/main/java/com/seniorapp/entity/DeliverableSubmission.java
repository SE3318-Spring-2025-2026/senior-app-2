package com.seniorapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Bir öğrenci grubunun belirli bir deliverable için yaptığı submission.
 * Dosya yükleme veya metin editörü ile içerik girişi desteklenir.
 * Dosyalar disk üzerinde storage/{templateId}/{groupId}/{deliverableId}/ altında saklanır.
 */
@Entity
@Table(name = "deliverable_submissions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"deliverable_id", "group_id"})
})
public class DeliverableSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "deliverable_id", nullable = false)
    private ProjectDeliverable deliverable;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "submitted_by_user_id", nullable = false)
    private Long submittedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_type", nullable = false, length = 20)
    private SubmissionType submissionType;

    /** Metin editöründen girilen içerik (TEXT_EDITOR tipi için) */
    @Lob
    @Column(name = "text_content", columnDefinition = "LONGTEXT")
    private String textContent;

    /** Dosyanın disk üzerindeki tam yolu (FILE_UPLOAD tipi için) */
    @Column(name = "file_path", length = 1000)
    private String filePath;

    /** Yüklenen dosyanın orijinal adı */
    @Column(name = "original_file_name", length = 500)
    private String originalFileName;

    /** Dosya boyutu (byte cinsinden) */
    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubmissionStatus status = SubmissionStatus.DRAFT;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.submittedAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ProjectDeliverable getDeliverable() { return deliverable; }
    public void setDeliverable(ProjectDeliverable deliverable) { this.deliverable = deliverable; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public Long getSubmittedByUserId() { return submittedByUserId; }
    public void setSubmittedByUserId(Long submittedByUserId) { this.submittedByUserId = submittedByUserId; }

    public SubmissionType getSubmissionType() { return submissionType; }
    public void setSubmissionType(SubmissionType submissionType) { this.submissionType = submissionType; }

    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public SubmissionStatus getStatus() { return status; }
    public void setStatus(SubmissionStatus status) { this.status = status; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
