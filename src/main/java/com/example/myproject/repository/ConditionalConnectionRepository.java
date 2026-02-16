package com.example.myproject.repository;

import com.example.myproject.domain.ConditionalConnection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConditionalConnectionRepository extends JpaRepository<ConditionalConnection, Long> {
    List<ConditionalConnection> findByNodeIdOrderByConnectionOrder(Long nodeId);

    void deleteByNodeId(Long nodeId);
}
