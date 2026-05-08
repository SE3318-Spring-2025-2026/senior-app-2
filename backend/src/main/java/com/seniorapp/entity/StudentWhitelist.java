package com.seniorapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_whitelist")
public class StudentWhitelist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String studentId;

    // Frontend'deki row.addedBy alanı için
    private String addedBy; 

    // Frontend'deki row.addedDate alanı için
    private LocalDateTime addedDate = LocalDateTime.now();

    // Frontend'deki row.linked kontrolü için (GitHub hesabı bağlandı mı?)
    private boolean linked = false;

    // Eğer bağlandıysa o kullanıcının ID'sini tutmak için
    private Long userId;

    public StudentWhitelist() {}

    public StudentWhitelist(String studentId, String addedBy) {
        this.studentId = studentId;
        this.addedBy = addedBy;
        this.addedDate = LocalDateTime.now();
    }

    // Getter ve Setter'lar
    public Long getId() { return id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getAddedBy() { return addedBy; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }
    public LocalDateTime getAddedDate() { return addedDate; }
    public boolean isLinked() { return linked; }
    public void setLinked(boolean linked) { this.linked = linked; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}