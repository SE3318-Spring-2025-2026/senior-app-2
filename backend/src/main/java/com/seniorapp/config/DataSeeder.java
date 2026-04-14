package com.seniorapp.config;

import com.seniorapp.entity.Project;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.repository.ProjectRepository;
import com.seniorapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, ProjectRepository projectRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@seniorapp.com")) {
            User admin = new User();
            admin.setEmail("admin@seniorapp.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("System Admin");
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
            log.info("Default admin user seeded (admin@seniorapp.com). Change password after first login.");
        }

        if (!projectRepository.findByName("Senioritis Project").isPresent()) {
            Project firstProject = new Project(
                "GP - 001",
                "Senioritis Project",
                5,
                "Students will deliver the most amazing project that wows all observers"
            );
            projectRepository.save(firstProject);
            log.info("First project seeded.");
        }

        if (!projectRepository.findByName("Senioritis Projectile").isPresent()) {
            Project secondProject = new Project(
                "GP - 002",
                "Senioritis Projectile",
                3,
                "Students will deliver the most amazing project that wows all observers really, really fast"
            );
            projectRepository.save(secondProject);
            log.info("Second project seeded.");
        }
    }
}
