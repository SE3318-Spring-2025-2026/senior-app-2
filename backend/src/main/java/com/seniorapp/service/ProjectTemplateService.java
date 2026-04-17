package com.seniorapp.service;

import com.seniorapp.dto.ProjectTemplateResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectTemplateService {

    public List<ProjectTemplateResponse> getAvailableTemplates() {
        return List.of(
                new ProjectTemplateResponse(
                        "proj-se1111",
                        "SE 1111 Graduation Project",
                        4,
                        List.of("Analysis", "Design", "Implementation")
                ),
                new ProjectTemplateResponse(
                        "proj-da2201",
                        "Data Analysis Project Template",
                        3,
                        List.of("Data Collection", "Modeling", "Final Report")
                ),
                new ProjectTemplateResponse(
                        "proj-ma3302",
                        "Mobile App Framework",
                        5,
                        List.of("Requirements", "UI Prototype", "App Build", "Testing")
                )
        );
    }
}
