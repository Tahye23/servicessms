package com.example.myproject.repository;

import com.example.myproject.domain.UserTokenApi;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the UserTokenApi entity.
 */
@SuppressWarnings("unused")
@Repository
public interface UserTokenApiRepository extends JpaRepository<UserTokenApi, Long> {}
