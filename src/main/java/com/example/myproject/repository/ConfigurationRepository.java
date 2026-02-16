package com.example.myproject.repository;

import com.example.myproject.domain.Configuration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigurationRepository extends JpaRepository<Configuration, Long> {
    boolean existsByUserLogin(String userLogin);
    Optional<Configuration> findOneByUserLogin(String userLogin);

    @Query("SELECT c FROM Configuration c WHERE c.userLogin = :userLogin")
    Optional<Configuration> findFullByUserLogin(@Param("userLogin") String userLogin);

    Optional<Configuration> findOneByBusinessIdAndUserLoginIgnoreCase(String businessId, String partnerName);
}
