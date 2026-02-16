package com.example.myproject.repository;

import com.example.myproject.domain.UserService;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the UserService entity.
 */
@SuppressWarnings("unused")
@Repository
public interface UserServiceRepository extends JpaRepository<UserService, Long> {}
