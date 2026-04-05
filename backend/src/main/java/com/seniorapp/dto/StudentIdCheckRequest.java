package com.seniorapp.dto;

import jakarta.validation.constraints.NotBlank;

public class StudentIdCheckRequest {

    @NotBlank
    private String studentId;

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
}
