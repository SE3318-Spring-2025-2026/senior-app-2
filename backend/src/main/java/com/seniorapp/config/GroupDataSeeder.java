package com.seniorapp.config;

import com.seniorapp.entity.UserGroup;
import com.seniorapp.entity.User;
import com.seniorapp.repository.UserGroupRepository;
import com.seniorapp.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class GroupDataSeeder implements CommandLineRunner {
    @PersistenceContext
    private EntityManager entityManager;

    private static final Logger log = LoggerFactory.getLogger(GroupDataSeeder.class);

    private final UserRepository userRepository;
    private final UserGroupRepository userGroups;

    public GroupDataSeeder(UserRepository userRepository, UserGroupRepository userGroups) {
        this.userRepository = userRepository;
        this.userGroups = userGroups;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.atInfo().setMessage("SEEDING GROUPS").log();

        // set up a basic group
        if (!userGroups.findById(1L).isPresent()) {
            UserGroup seedGroup = new UserGroup();

            // --- BU SATIR SQL HATASINI ÇÖZÜYOR ---
            seedGroup.setGroupName("Senior App Default Group");
            // -------------------------------------

            User coordinator = userRepository.findByEmail("professor@seniorapp.com").orElseThrow(() -> new RuntimeException("User not found."));
            entityManager.merge(coordinator);
            seedGroup.setCoordinator(coordinator);

            User teamLeader = userRepository.findByEmail("student@seniorapp.com").orElseThrow(() -> new RuntimeException("User not found."));
            entityManager.merge(teamLeader);
            seedGroup.setTeamLeader(teamLeader);

            // Team leader will change, but the leader will still be a member of the group when that happens, so they should also be known as a member of the group
            List<User> currentMembers = seedGroup.getMembers();
            if (null == currentMembers) {
                currentMembers = new ArrayList<>();
            }

            currentMembers.add(teamLeader);
            seedGroup.setMembers(currentMembers);

            userGroups.save(seedGroup);
            log.info("Default group seeded with group name, default professor as coordinator and default student as team lead.");
        }
    }
}