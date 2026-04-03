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
    private String studentId; // Öğrenci Numarası

    private LocalDateTime createdAt = LocalDateTime.now();

    public StudentWhitelist() {}

    public StudentWhitelist(String studentId) {
        this.studentId = studentId;
    }

    public Long getId() { return id; }
    public String getStudentId() { return studentId; }
}