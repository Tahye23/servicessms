package com.example.myproject.service.dto.flow;

import java.util.ArrayList;
import java.util.List;

public class FlowData {

    private List<FlowNode> nodes;
    private List<FlowConnection> connections;
    private List<FlowTrigger> triggers;

    public FlowData() {}

    public List<FlowNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<FlowNode> nodes) {
        this.nodes = nodes;
    }

    public List<FlowConnection> getConnections() {
        return connections;
    }

    public void setConnections(List<FlowConnection> connections) {
        this.connections = connections;
    }

    public List<FlowTrigger> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<FlowTrigger> triggers) {
        this.triggers = triggers;
    }
}
