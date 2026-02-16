package com.example.myproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Entité PartnershipRequest pour stocker les demandes de partenariat
 */
@Entity
@Table(name = "partnership_request")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PartnershipRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    private Long id;

    @NotNull
    @Size(min = 2, max = 100)
    @Column(name = "first_name", length = 100, nullable = false)
    private String firstName;

    @NotNull
    @Size(min = 2, max = 100)
    @Column(name = "last_name", length = 100, nullable = false)
    private String lastName;

    @NotNull
    @Email
    @Size(min = 5, max = 254)
    @Column(name = "email", length = 254, nullable = false)
    private String email;

    @NotNull
    @Size(min = 10, max = 20)
    @Column(name = "phone", length = 20, nullable = false)
    private String phone;

    @NotNull
    @Size(min = 2, max = 150)
    @Column(name = "company_name", length = 150, nullable = false)
    private String companyName;

    @NotNull
    @Size(min = 2, max = 100)
    @Column(name = "industry", length = 100, nullable = false)
    private String industry;

    @NotNull
    @Size(min = 20, max = 2000)
    @Column(name = "project_description", length = 2000, nullable = false)
    private String projectDescription;

    @Size(max = 50)
    @Column(name = "monthly_volume", length = 50)
    private String monthlyVolume;

    @Size(max = 50)
    @Column(name = "launch_date", length = 50)
    private String launchDate;

    @Column(name = "selected_plan_id")
    private Long selectedPlanId;

    @Size(max = 100)
    @Column(name = "selected_plan_name", length = 100)
    private String selectedPlanName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "created_date")
    private Instant createdDate = Instant.now();

    @Column(name = "processed_date")
    private Instant processedDate;

    @Size(max = 1000)
    @Column(name = "admin_notes", length = 1000)
    private String adminNotes;

    // Constructeurs
    public PartnershipRequest() {}

    public PartnershipRequest(
        String firstName,
        String lastName,
        String email,
        String phone,
        String companyName,
        String industry,
        String projectDescription
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.companyName = companyName;
        this.industry = industry;
        this.projectDescription = projectDescription;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }

    public String getMonthlyVolume() {
        return monthlyVolume;
    }

    public void setMonthlyVolume(String monthlyVolume) {
        this.monthlyVolume = monthlyVolume;
    }

    public String getLaunchDate() {
        return launchDate;
    }

    public void setLaunchDate(String launchDate) {
        this.launchDate = launchDate;
    }

    public Long getSelectedPlanId() {
        return selectedPlanId;
    }

    public void setSelectedPlanId(Long selectedPlanId) {
        this.selectedPlanId = selectedPlanId;
    }

    public String getSelectedPlanName() {
        return selectedPlanName;
    }

    public void setSelectedPlanName(String selectedPlanName) {
        this.selectedPlanName = selectedPlanName;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getProcessedDate() {
        return processedDate;
    }

    public void setProcessedDate(Instant processedDate) {
        this.processedDate = processedDate;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PartnershipRequest)) return false;
        return id != null && id.equals(((PartnershipRequest) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return (
            "PartnershipRequest{" +
            "id=" +
            id +
            ", firstName='" +
            firstName +
            '\'' +
            ", lastName='" +
            lastName +
            '\'' +
            ", email='" +
            email +
            '\'' +
            ", companyName='" +
            companyName +
            '\'' +
            ", industry='" +
            industry +
            '\'' +
            ", status=" +
            status +
            ", createdDate=" +
            createdDate +
            '}'
        );
    }

    /**
     * Enum pour le statut des demandes
     */
    public enum RequestStatus {
        PENDING("En attente"),
        APPROVED("Approuvée"),
        REJECTED("Rejetée"),
        IN_REVIEW("En cours d'examen");

        private final String displayName;

        RequestStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
