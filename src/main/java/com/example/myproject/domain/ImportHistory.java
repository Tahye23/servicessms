package com.example.myproject.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;

@Entity
@Table(name = "import_history")
public class ImportHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_login")
    private String user_login;

    @Column(name = "bulk_id", unique = true)
    private String bulkId;

    @Column(name = "import_date")
    private ZonedDateTime importDate;

    @Column(name = "total_lines")
    private int totalLines;

    @Column(name = "inserted_count")
    private int insertedCount;

    @Column(name = "rejected_count")
    private int rejectedCount;

    @Column(name = "duplicate_count")
    private int duplicateCount;

    @Column(name = "status")
    private String status; // Par exemple, "PENDING", "COMPLETED", "FAILED"

    // Getters et setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBulkId() {
        return bulkId;
    }

    public void setBulkId(String bulkId) {
        this.bulkId = bulkId;
    }

    public ZonedDateTime getImportDate() {
        return importDate;
    }

    public void setImportDate(ZonedDateTime importDate) {
        this.importDate = importDate;
    }

    public int getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(int totalLines) {
        this.totalLines = totalLines;
    }

    public int getInsertedCount() {
        return insertedCount;
    }

    public void setInsertedCount(int insertedCount) {
        this.insertedCount = insertedCount;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(int rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(int duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUser_login() {
        return user_login;
    }

    public void setUser_login(String user_login) {
        this.user_login = user_login;
    }
}
