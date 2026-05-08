package com.seniorapp.repository;

import com.seniorapp.entity.JiraOAuthState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JiraOAuthStateRepository extends JpaRepository<JiraOAuthState, String> {
}
