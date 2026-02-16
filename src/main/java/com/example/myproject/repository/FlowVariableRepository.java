package com.example.myproject.repository;

import com.example.myproject.domain.FlowVariable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlowVariableRepository extends JpaRepository<FlowVariable, Long> {
    List<FlowVariable> findByFlowId(Long flowId);

    Optional<FlowVariable> findByFlowIdAndName(Long flowId, String name);

    void deleteByFlowId(Long flowId);
}
