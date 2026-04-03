package com.seniorapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "valid_student_ids", indexes = {
    @Index(name = "idx_student_id", columnList = "studentId")
})
public class ValidStudentId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private User account;

    @Column(nullable = false, updatable = false)
    private LocalDateTime addedDate = LocalDateTime.now();

    @Column(nullable = false)
    private String addedBy;

    public ValidStudentId() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public User getAccount() { return account; }
    public void setAccount(User account) { this.account = account; }

    public LocalDateTime getAddedDate() { return addedDate; }
    public void setAddedDate(LocalDateTime addedDate) { this.addedDate = addedDate; }

    public String getAddedBy() { return addedBy; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }
}
