package com.example.myproject.service;

import com.example.myproject.domain.ExtendedUser;
import com.example.myproject.domain.User;
import com.example.myproject.repository.ExtendedUserRepository;
import com.example.myproject.repository.UserRepository;
import com.example.myproject.service.dto.ExtendedUserDTO;
import com.example.myproject.service.dto.SubscriptionAccessDTO;
import com.example.myproject.service.dto.UserSubscriptionDTO;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExtendedUserService {

    private final Logger log = LoggerFactory.getLogger(ExtendedUserService.class);

    private final ExtendedUserRepository extendedUserRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    public ExtendedUserService(
        ExtendedUserRepository extendedUserRepository,
        UserRepository userRepository,
        SubscriptionService subscriptionService
    ) {
        this.extendedUserRepository = extendedUserRepository;
        this.userRepository = userRepository;
        this.subscriptionService = subscriptionService;
    }

    /**
     * Obtient les informations étendues d'un utilisateur avec ses abonnements
     */
    @Transactional(readOnly = true)
    public Optional<ExtendedUserDTO> getExtendedUser(Long userId) {
        log.debug("Getting extended user information for user: {}", userId);

        ExtendedUser extendedUser = extendedUserRepository.findByUserId(userId);
        if (extendedUser == null) {
            return Optional.empty();
        }

        ExtendedUserDTO dto = new ExtendedUserDTO(extendedUser);

        // Ajouter les abonnements
        List<UserSubscriptionDTO> subscriptions = subscriptionService.getUserSubscriptions(userId);
        dto.setSubscriptions(subscriptions);

        // Ajouter les accès calculés
        //     SubscriptionAccessDTO access = subscriptionService.calculateUserAccess(userId);
        //    dto.setAccess(access);

        return Optional.of(dto);
    }

    /**
     * Crée ou met à jour un ExtendedUser
     */
    @Transactional
    public ExtendedUserDTO createOrUpdateExtendedUser(Long userId, ExtendedUserDTO dto) {
        log.debug("Creating or updating extended user for user: {}", userId);

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found: " + userId));

        ExtendedUser extendedUser = extendedUserRepository.findByUserId(userId);
        if (extendedUser == null) {
            extendedUser = new ExtendedUser(user);
        }

        // Mettre à jour les champs
        extendedUser.setPhoneNumber(dto.getPhoneNumber());
        extendedUser.setCompanyName(dto.getCompanyName());
        extendedUser.setWebsite(dto.getWebsite());
        extendedUser.setTimezone(dto.getTimezone());
        extendedUser.setLanguage(dto.getLanguage());

        extendedUser = extendedUserRepository.save(extendedUser);

        return new ExtendedUserDTO(extendedUser);
    }

    /**
     * Met à jour la dernière connexion d'un utilisateur
     */
    @Transactional
    public void updateLastLogin(Long userId) {
        log.debug("Updating last login for user: {}", userId);

        ExtendedUser extendedUser = extendedUserRepository.findByUserId(userId);
        if (extendedUser != null) {
            //extendedUser.updateLastLogin();
            extendedUserRepository.save(extendedUser);
        }
    }

    /**
     * Génère une nouvelle clé API pour un utilisateur
     */
    @Transactional
    public String generateApiKey(Long userId) {
        log.debug("Generating new API key for user: {}", userId);

        ExtendedUser extendedUser = extendedUserRepository.findByUserId(userId);
        if (extendedUser == null) {
            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found: " + userId));
            extendedUser = new ExtendedUser(user);
        }

        // extendedUser.generateNewApiKey();
        extendedUser = extendedUserRepository.save(extendedUser);

        log.info("Generated new API key for user: {}", userId);
        return extendedUser.getApiKey();
    }

    /**
     * Valide une clé API
     */
    @Transactional(readOnly = true)
    public Optional<ExtendedUser> validateApiKey(String apiKey) {
        log.debug("Validating API key");

        ExtendedUser extendedUser = extendedUserRepository.findByApiKey(apiKey);
        if (extendedUser != null) {
            // Mettre à jour la dernière utilisation (dans une transaction séparée)
            updateApiKeyLastUsed(extendedUser.getId());
            return Optional.of(extendedUser);
        }

        return Optional.empty();
    }

    @Transactional
    protected void updateApiKeyLastUsed(Long extendedUserId) {
        ExtendedUser extendedUser = extendedUserRepository.findById(extendedUserId).orElse(null);
        if (extendedUser != null) {
            // extendedUser.updateApiKeyLastUsed();
            extendedUserRepository.save(extendedUser);
        }
    }

    /**
     * Obtient les statistiques d'usage d'un utilisateur
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserUsageStats(Long userId) {
        ExtendedUser extendedUser = extendedUserRepository.findByUserId(userId);
        if (extendedUser == null) {
            return Map.of();
        }

        // extendedUser.resetMonthlyQuotasIfNeeded();

        return Map.of(
            "smsUsedThisMonth",
            extendedUser.getSmsUsedThisMonth(),
            "whatsappUsedThisMonth",
            extendedUser.getWhatsappUsedThisMonth(),
            "totalMessagesSent",
            extendedUser.getTotalMessagesSent(),
            "loginCount",
            extendedUser.getLoginCount(),
            "lastLogin",
            extendedUser.getLastLogin(),
            "accountCreated",
            extendedUser.getAccountCreated()
        );
    }
}
