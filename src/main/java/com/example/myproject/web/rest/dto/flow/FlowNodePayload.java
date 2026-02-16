package com.example.myproject.web.rest.dto.flow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class FlowNodePayload {

    @NotBlank
    private String id;

    @NotBlank
    private String type;

    @NotNull
    private Integer x;

    @NotNull
    private Integer y;

    @NotNull
    private Integer order;

    private String label;
    private String nextNodeId;
    private NodeDataPayload data;

    // Constructeurs
    public FlowNodePayload() {}

    public @NotBlank String getId() {
        return id;
    }

    public void setId(@NotBlank String id) {
        this.id = id;
    }

    public @NotNull Integer getX() {
        return x;
    }

    public void setX(@NotNull Integer x) {
        this.x = x;
    }

    public @NotBlank String getType() {
        return type;
    }

    public void setType(@NotBlank String type) {
        this.type = type;
    }

    public @NotNull Integer getY() {
        return y;
    }

    public void setY(@NotNull Integer y) {
        this.y = y;
    }

    public @NotNull Integer getOrder() {
        return order;
    }

    public void setOrder(@NotNull Integer order) {
        this.order = order;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getNextNodeId() {
        return nextNodeId;
    }

    public void setNextNodeId(String nextNodeId) {
        this.nextNodeId = nextNodeId;
    }

    public NodeDataPayload getData() {
        return data;
    }

    public void setData(NodeDataPayload data) {
        this.data = data;
    }
}
