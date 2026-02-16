package com.example.myproject.repository;

import com.example.myproject.domain.NodeListItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NodeListItemRepository extends JpaRepository<NodeListItem, Long> {
    List<NodeListItem> findByNodeIdOrderByItemOrder(Long nodeId);

    void deleteByNodeId(Long nodeId);
}
