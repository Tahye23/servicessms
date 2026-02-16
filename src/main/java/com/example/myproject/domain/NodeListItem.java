package com.example.myproject.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

@Entity
@Table(name = "node_list_item")
public class NodeListItem extends AbstractAuditingEntity<Long> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    private Long id;

    @NotNull
    @Size(max = 50)
    @Column(name = "item_id", length = 50, nullable = false)
    private String itemId;

    @NotNull
    @Size(max = 100)
    @Column(name = "title", length = 100, nullable = false)
    private String title;

    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

    @Size(max = 100)
    @Column(name = "value", length = 100)
    private String value;

    @Column(name = "next_node_id")
    private String nextNodeId;

    @Column(name = "store_in_variable")
    private String storeInVariable;

    @Column(name = "item_order")
    private Integer itemOrder;

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

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Integer getItemOrder() {
        return itemOrder;
    }

    public void setItemOrder(Integer itemOrder) {
        this.itemOrder = itemOrder;
    }

    public FlowNode getNode() {
        return node;
    }

    public void setNode(FlowNode node) {
        this.node = node;
    }
}
