package com.example.myproject.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

@Entity
@Table(name = "node_button")
public class NodeButton extends AbstractAuditingEntity<Long> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    private Long id;

    @NotNull
    @Size(max = 50)
    @Column(name = "button_id", length = 50, nullable = false)
    private String buttonId;

    @NotNull
    @Size(max = 100)
    @Column(name = "text", length = 100, nullable = false)
    private String text;

    @Size(max = 100)
    @Column(name = "value", length = 100)
    private String value;

    @Column(name = "next_node_id")
    private String nextNodeId;

    @Column(name = "store_in_variable")
    private String storeInVariable;

    @Column(name = "button_order")
    private Integer buttonOrder;

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

    public String getButtonId() {
        return buttonId;
    }

    public void setButtonId(String buttonId) {
        this.buttonId = buttonId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getNextNodeId() {
        return nextNodeId;
    }

    public void setNextNodeId(String nextNodeId) {
        this.nextNodeId = nextNodeId;
    }

    public String getStoreInVariable() {
        return storeInVariable;
    }

    public void setStoreInVariable(String storeInVariable) {
        this.storeInVariable = storeInVariable;
    }

    public Integer getButtonOrder() {
        return buttonOrder;
    }

    public void setButtonOrder(Integer buttonOrder) {
        this.buttonOrder = buttonOrder;
    }

    public FlowNode getNode() {
        return node;
    }

    public void setNode(FlowNode node) {
        this.node = node;
    }
}
