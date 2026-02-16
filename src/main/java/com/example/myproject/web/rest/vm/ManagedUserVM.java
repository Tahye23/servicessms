package com.example.myproject.web.rest.vm;

import com.example.myproject.service.dto.AdminUserDTO;
import jakarta.validation.constraints.Size;

/**
 * View Model extending the AdminUserDTO, which is meant to be used in the user management UI.
 */
public class ManagedUserVM extends AdminUserDTO {

    public static final int PASSWORD_MIN_LENGTH = 4;

    public static final int PASSWORD_MAX_LENGTH = 100;

    @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH)
    private String password;

    // Dans ManagedUserVM.java, ajoutez :
    private String planType = "FREE"; // Valeur par défaut

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public ManagedUserVM() {
        // Empty constructor needed for Jackson.
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ManagedUserVM{" + super.toString() + "} ";
    }
}
