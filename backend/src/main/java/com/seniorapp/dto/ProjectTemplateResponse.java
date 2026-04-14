package com.seniorapp.dto;

import java.util.List;

import com.seniorapp.entity.Project;

/**
 * Results for a project template, which students can choose from when creating a new project.
 */
public record ProjectTemplateResponse (
    List<Project> projects
) {}
