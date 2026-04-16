package com.seniorapp.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.seniorapp.entity.UserGroup;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {}
