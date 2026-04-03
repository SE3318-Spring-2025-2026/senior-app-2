package com.seniorapp.repository;

import com.seniorapp.entity.OAuthState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface OAuthStateRepository extends JpaRepository<OAuthState, String> {
}