package com.example.myproject.service.dto;

import com.example.myproject.domain.PartnershipRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;

public class PartnershipRequestDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(min = 2, max = 100)
    private String firstName;

    @NotNull
    @Size(min = 2, max = 100)
    private String lastName;

    @NotNull
    @Email
    @Size(min = 5, max = 254)
    private String email;

    @NotNull
    @Size(min = 10, max = 20)
    private String phone;

    @NotNull
    @Size(min = 2, max = 150)
    private String companyName;

    @NotNull
    @Size(min = 2, max = 100)
    private String industry;

    @NotNull
    @Size(min = 20, max = 2000)
    private String projectDescription;

    @Size(max = 50)
    private String monthlyVolume;

    @Size(max = 50)
    private String launchDate;

    private Long selectedPlanId;

    @Size(max = 100)
    private String selectedPlanName;

    private PartnershipRequest.RequestStatus status;

    private Instant createdDate;

    private Instant processedDate;

    @Size(max = 1000)
    private String adminNotes;

    // Constructeurs
    public PartnershipRequestDTO() {}

    public PartnershipRequestDTO(
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

    public PartnershipRequest.RequestStatus getStatus() {
        return status;
    }

    public void setStatus(PartnershipRequest.RequestStatus status) {
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
        if (!(o instanceof PartnershipRequestDTO)) return false;
        PartnershipRequestDTO that = (PartnershipRequestDTO) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
