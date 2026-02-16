package com.example.myproject.service.dto.flow;

import java.util.ArrayList;
import java.util.List;

public class FlowNode {

    private String id;
    private String type;
    private Integer x;
    private Integer y;
    private NodeData data;
    private String title;
    private String description;
    private Integer order;
    private List<NodeConnection> connections;

    public FlowNode() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public NodeData getData() {
        return data;
    }

    public void setData(NodeData data) {
        this.data = data;
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

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public List<NodeConnection> getConnections() {
        return connections;
    }

    public void setConnections(List<NodeConnection> connections) {
        this.connections = connections;
    }
}
