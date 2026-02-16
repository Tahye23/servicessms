package com.example.myproject.service.mapper;

import com.example.myproject.domain.Application;
import com.example.myproject.domain.TokensApp;
import com.example.myproject.service.dto.TokensAppDTO;
import com.example.myproject.web.rest.dto.ApplicationDTO;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ApplicationMapper {

    public static ApplicationDTO toDto(Application app) {
        if (app == null) {
            return null;
        }

        ApplicationDTO dto = new ApplicationDTO();
        dto.setId(app.getId());
        dto.setName(app.getName());
        dto.setDescription(app.getDescription());
        dto.setUserId(app.getUserId());
        dto.setIsActive(app.getIsActive());
        dto.setEnvironment(app.getEnvironment().name());

        dto.setDailyLimit(app.getDailyLimit());
        dto.setMonthlyLimit(app.getMonthlyLimit());
        dto.setCurrentDailyUsage(app.getCurrentDailyUsage());
        dto.setCurrentMonthlyUsage(app.getCurrentMonthlyUsage());
        dto.setTotalApiCalls(app.getTotalApiCalls());
        dto.setLastApiCall(app.getLastApiCall());

        dto.setCreatedAt(app.getCreatedAt());
        dto.setUpdatedAt(app.getUpdatedAt());
        dto.setAllowedServices(app.getAllowedServices());

        // tokens
        if (app.getTokens() != null) {
            Set<TokensAppDTO> tokens = app.getTokens().stream().map(ApplicationMapper::toTokenDto).collect(Collectors.toSet());
            dto.setTokens(tokens);
        }

        return dto;
    }

    private static TokensAppDTO toTokenDto(TokensApp token) {
        TokensAppDTO dto = new TokensAppDTO();
        dto.setId(token.getId());
        dto.setToken(token.getToken());
        dto.setActive(token.getActive());
        dto.setDateExpiration(token.getDateExpiration());
        dto.setLastUsedAt(token.getLastUsedAt());
        dto.setUserLogin(token.getUserLogin());
        dto.setIsExpired(token.getDateExpiration() != null && token.getDateExpiration().isBefore(ZonedDateTime.now()));
        return dto;
    }
}
