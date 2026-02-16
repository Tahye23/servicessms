package com.example.myproject.repository;

import com.example.myproject.domain.NodeButton;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NodeButtonRepository extends JpaRepository<NodeButton, Long> {
    List<NodeButton> findByNodeIdOrderByButtonOrder(Long nodeId);

    void deleteByNodeId(Long nodeId);
}
