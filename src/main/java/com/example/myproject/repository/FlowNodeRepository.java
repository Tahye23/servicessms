package com.example.myproject.repository;

import com.example.myproject.domain.FlowNode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FlowNodeRepository extends JpaRepository<FlowNode, Long> {
    List<FlowNode> findByFlowIdOrderByNodeOrder(Long flowId);

    Optional<FlowNode> findByNodeIdAndFlowId(String nodeId, Long flowId);

    @Query(
        "SELECT n FROM FlowNode n LEFT JOIN FETCH n.buttons LEFT JOIN FETCH n.listItems LEFT JOIN FETCH n.conditionalConnections WHERE n.flow.id = :flowId ORDER BY n.nodeOrder"
    )
    List<FlowNode> findByFlowIdWithDetails(@Param("flowId") Long flowId);

    @Query("SELECT n FROM FlowNode n WHERE n.flow.id = :flowId AND n.nodeType = 'start'")
    Optional<FlowNode> findStartNodeByFlowId(@Param("flowId") Long flowId);

    void deleteByFlowId(Long flowId);
}
