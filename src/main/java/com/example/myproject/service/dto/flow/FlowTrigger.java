package com.example.myproject.service.dto.flow;

public class FlowTrigger {

    private String type; // keyword, exact_match, regex, start, menu, fallback, time_based, user_attribute
    private String value;
    private Boolean active;

    public FlowTrigger() {}

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
