package com.example.myproject.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

@Entity
@Table(name = "conditional_connection")
public class ConditionalConnection extends AbstractAuditingEntity<Long> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    private Long id;

    @NotNull
    @Size(max = 50)
    @Column(name = "connection_id", length = 50, nullable = false)
    private String connectionId;

    @NotNull
    @Size(max = 100)
    @Column(name = "condition_value", length = 100, nullable = false)
    private String conditionValue;

    @NotNull
    @Column(name = "next_node_id", nullable = false)
    private String nextNodeId;

    @Size(max = 100)
    @Column(name = "label", length = 100)
    private String label;

    @Column(name = "operator")
    private String operator;

    @Column(name = "connection_order")
    private Integer connectionOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id")
    private FlowNode node;

    // Getters et setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getConditionValue() {
        return conditionValue;
    }

    public void setConditionValue(String conditionValue) {
        this.conditionValue = conditionValue;
    }

    public String getNextNodeId() {
        return nextNodeId;
    }

    public void setNextNodeId(String nextNodeId) {
        this.nextNodeId = nextNodeId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Integer getConnectionOrder() {
        return connectionOrder;
    }

    public void setConnectionOrder(Integer connectionOrder) {
        this.connectionOrder = connectionOrder;
    }

    public FlowNode getNode() {
        return node;
    }

    public void setNode(FlowNode node) {
        this.node = node;
    }
}
