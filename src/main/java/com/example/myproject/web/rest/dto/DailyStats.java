package com.example.myproject.web.rest.dto;

import java.time.LocalDate;

public class DailyStats {

    private String day;
    private LocalDate date;
    private Long count;

    public DailyStats() {}

    public DailyStats(String day, LocalDate date, Long count) {
        this.day = day;
        this.date = date;
        this.count = count;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
