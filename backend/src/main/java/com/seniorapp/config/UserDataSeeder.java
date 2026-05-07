package com.seniorapp.config;

import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class UserDataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(UserDataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.atInfo().setMessage("SEEDING USERS").log();

        // set up users suggested in GETTING_STARTED.md
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
        if (!userRepository.existsByEmail("professor@seniorapp.com")) {
            User professor = new User();
            professor.setEmail("professor@seniorapp.com");
            professor.setPassword(passwordEncoder.encode("prof123"));
            professor.setFullName("Example Professor");
            professor.setRole(Role.COORDINATOR);
            professor.setEnabled(true);
            userRepository.save(professor);
            log.info("Default professor user seeded (professor@seniorapp.com). Change password after first login.");
        }
        if (!userRepository.existsByEmail("student@seniorapp.com")) {
            User student = new User();
            student.setEmail("student@seniorapp.com");
            student.setPassword(passwordEncoder.encode("student123"));
            student.setFullName("Example Student");
            student.setRole(Role.STUDENT);
            student.setEnabled(true);
            userRepository.save(student);
            log.info("Default student user seeded (student@seniorapp.com). Change password after first login.");
        }
    }
}
