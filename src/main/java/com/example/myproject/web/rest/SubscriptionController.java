package com.example.myproject.web.rest;

import com.example.myproject.domain.ExtendedUser;
import com.example.myproject.domain.User;
import com.example.myproject.repository.ExtendedUserRepository;
import com.example.myproject.repository.SendSmsRepository;
import com.example.myproject.repository.UserRepository;
import com.example.myproject.security.AuthoritiesConstants;
import com.example.myproject.security.SecurityUtils;
import com.example.myproject.service.SubscriptionService;
import com.example.myproject.service.dto.CreateSubscriptionRequest;
import com.example.myproject.service.dto.SubscriptionAccessDTO;
import com.example.myproject.service.dto.UpdateCounterRequest;
import com.example.myproject.service.dto.UserSubscriptionDTO;
import com.example.myproject.web.rest.dto.StatsResponseDTO;
import com.example.myproject.web.rest.dto.SubscriptionInfoDTO;
import com.example.myproject.web.rest.dto.TypeStatsDTO;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    private final ExtendedUserRepository extendedUserRepository;
    private final Logger log = LoggerFactory.getLogger(SubscriptionController.class);
    private final SubscriptionService subscriptionService;
    private final SendSmsRepository sendSmsRepository;
    private final UserRepository userRepository;

    public SubscriptionController(
        ExtendedUserRepository extendedUserRepository,
        SubscriptionService subscriptionService,
        SendSmsRepository sendSmsRepository,
        UserRepository userRepository
    ) {
        this.extendedUserRepository = extendedUserRepository;
        this.subscriptionService = subscriptionService;
        this.sendSmsRepository = sendSmsRepository;
        this.userRepository = userRepository;
    }

    /**
     * GET /api/subscription/access : Obtient les accès d'abonnement de l'utilisateur connecté
     */
    /* @GetMapping("/access")
    public ResponseEntity<SubscriptionAccessDTO> getUserAccess() {
        log.debug("REST request to get user subscription access");

        Long userId = SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("No authenticated user found"));

        SubscriptionAccessDTO access = subscriptionService.calculateUserAccess(userId);
        return ResponseEntity.ok(access);
    }*/

    /**
     * GET /api/subscription/user : Obtient tous les abonnements de l'utilisateur connecté
     */
    @GetMapping("/user/partner-subscription")
    public ResponseEntity<List<UserSubscriptionDTO>> getPartnerSubscriptions() {
        log.debug("REST request to get partner subscriptions for current user");

        Long currentUserId = SecurityUtils.getCurrentUserId().orElseThrow(() -> new RuntimeException("No authenticated user found"));
        Long userId = determineEffectiveUserId(currentUserId);
        User currentUser = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // Utiliser l'ID du partenaire s'il existe, sinon l'ID de l'utilisateur actuel
        Long targetUserId = currentUser.getPartnerUserId() != null ? currentUser.getPartnerUserId() : userId;

        List<UserSubscriptionDTO> subscriptions = subscriptionService.getUserSubscriptions(targetUserId);
        return ResponseEntity.ok(subscriptions);
    }

    @PutMapping("/sidebar-visibility")
    public ResponseEntity<Void> updateSidebarVisibility(@RequestParam Boolean visible) {
        Long userId = SecurityUtils.getCurrentUserId().orElseThrow(() -> new RuntimeException("Utilisateur non authentifié"));

        subscriptionService.updateSidebarVisibilityForAllUserSubscriptions(userId, visible);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/subscription : Crée un nouvel abonnement
     */
    @PostMapping
    public ResponseEntity<UserSubscriptionDTO> createSubscription(@Valid @RequestBody CreateSubscriptionRequest request) {
        log.debug("REST request to create subscription: {}", request);

        Long userId = SecurityUtils.getCurrentUserId().orElseThrow(() -> new RuntimeException("No authenticated user found"));

        request.setUserId(userId);
        UserSubscriptionDTO subscription = subscriptionService.createSubscription(request);
        return ResponseEntity.ok(subscription);
    }

    /**
     * PUT /api/subscription/{id}/cancel : Annule un abonnement
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelSubscription(@PathVariable Long id) {
        log.debug("REST request to cancel subscription: {}", id);

        Long userId = SecurityUtils.getCurrentUserId().orElseThrow(() -> new RuntimeException("No authenticated user found"));

        subscriptionService.cancelSubscription(id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * PUT /api/subscription/counter : Met à jour les compteurs d'usage
     */
    /*@PutMapping("/counter")
    public ResponseEntity<Void> updateCounter(@Valid @RequestBody UpdateCounterRequest request) {
        log.debug("REST request to update usage counter: {}", request);

        Long userId = SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("No authenticated user found"));

        request.setUserId(userId);
        subscriptionService.updateUsageCounter(userId, request);
        return ResponseEntity.ok().build();
    }
*/
    /**
     * POST /api/subscription/{id}/renew : Renouvelle un abonnement
     */
    @PostMapping("/{id}/renew")
    public ResponseEntity<UserSubscriptionDTO> renewSubscription(@PathVariable Long id) {
        log.debug("REST request to renew subscription: {}", id);

        UserSubscriptionDTO renewed = subscriptionService.renewSubscription(id);
        return ResponseEntity.ok(renewed);
    }

    /**
     * GET /api/subscription/expiring : Obtient les abonnements qui expirent bientôt (ADMIN)
     */
    @GetMapping("/expiring")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<UserSubscriptionDTO>> getExpiringSoonSubscriptions(@RequestParam(defaultValue = "7") int days) {
        log.debug("REST request to get expiring subscriptions within {} days", days);

        List<UserSubscriptionDTO> expiring = subscriptionService.getExpiringSoonSubscriptions(days);
        return ResponseEntity.ok(expiring);
    }

    /**
     * POST /api/subscription/check-expired : Vérifie et met à jour les abonnements expirés (ADMIN)
     */
    @PostMapping("/check-expired")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> checkExpiredSubscriptions() {
        log.debug("REST request to check expired subscriptions");

        subscriptionService.checkAndUpdateExpiredSubscriptions();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats-by-range")
    @PreAuthorize(
        "hasAnyAuthority('" + AuthoritiesConstants.ADMIN + "', '" + AuthoritiesConstants.PARTNER + "', '" + AuthoritiesConstants.USER + "')"
    )
    public ResponseEntity<StatsResponseDTO> getStatsBetweenDates(
        @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm") LocalDateTime startDate,
        @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm") LocalDateTime endDate
    ) {
        try {
            String userLogin = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new IllegalStateException("L'utilisateur n'est pas authentifié."));

            log.debug("Récupération des statistiques pour l'utilisateur: {}", userLogin);
            String login = determineEffectiveUserLogin(userLogin);
            ZonedDateTime start = startDate != null ? startDate.atZone(ZoneId.systemDefault()) : null;
            ZonedDateTime end = endDate != null ? endDate.atZone(ZoneId.systemDefault()) : null;

            // Récupération des statistiques d'envoi
            List<Object[]> statsList;
            if (start != null && end != null) {
                statsList = sendSmsRepository.sumStatsByLoginAndTypeWithDate(login, start, end);
            } else {
                statsList = sendSmsRepository.sumStatsByLoginAndType(login);
            }

            // Récupération de l'utilisateur étendu
            ExtendedUser extendedUser = extendedUserRepository
                .findOneByUserLogin(login)
                .orElseThrow(() -> new IllegalStateException("Utilisateur étendu non trouvé pour ce login."));

            // Récupération des abonnements actifs
            List<UserSubscriptionDTO> subscriptions = subscriptionService.getUserSubscriptions(extendedUser.getId());
            UserSubscriptionDTO activeSubscription = subscriptions.isEmpty() ? createDefaultSubscription() : subscriptions.get(0);

            // Calcul des prix et du solde
            BigDecimal prixSms = calculateSmsPrice(activeSubscription);
            BigDecimal prixWhatsapp = calculateWhatsappPrice(activeSubscription);
            double soldeAbonnement = calculateSubscriptionBalance(activeSubscription);

            // Construction des statistiques par type
            List<TypeStatsDTO> typeStats = buildTypeStats(statsList, prixSms, prixWhatsapp);

            // Calcul des totaux
            double totalConsomme = calculateTotalConsumed(typeStats);
            double totalMessages = typeStats.stream().mapToDouble(t -> t.getSuccess()).sum();
            double totalMessagesFailed = typeStats.stream().mapToDouble(t -> t.getFailed()).sum();
            double totalMessagesPending = typeStats.stream().mapToDouble(t -> t.getPending()).sum();

            double soldeRestant = Math.max(0, soldeAbonnement - totalConsomme);

            StatsResponseDTO responseDTO = new StatsResponseDTO(
                typeStats,
                soldeAbonnement,
                totalConsomme,
                soldeRestant,
                totalMessages,
                totalMessagesFailed,
                totalMessagesPending
            );

            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des statistiques", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                createErrorResponse("Erreur lors de la récupération des statistiques")
            );
        }
    }

    private String determineEffectiveUserLogin(String currentUserLogin) {
        boolean isUser =
            SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_USER") && !SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_ADMIN");

        if (!isUser) {
            return currentUserLogin;
        }

        return userRepository.findOneByLogin(currentUserLogin).map(User::getExpediteur).filter(Objects::nonNull).orElse(currentUserLogin);
    }

    private Long determineEffectiveUserId(Long currentUserId) {
        boolean isUser = SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_USER");

        if (!isUser) {
            return currentUserId;
        }

        return userRepository.findById(currentUserId).map(User::getPartnerUserId).filter(Objects::nonNull).orElse(currentUserId);
    }

    /**
     * Calcule le prix SMS selon l'abonnement
     */
    private BigDecimal calculateSmsPrice(UserSubscriptionDTO subscription) {
        // Prix par défaut si pas d'abonnement
        BigDecimal defaultPrice = BigDecimal.valueOf(0.50); // 0.50 MRU par SMS

        if (subscription == null || subscription.getPrice() == null) {
            return defaultPrice;
        }

        // Si c'est un plan gratuit, prix fixe
        if ("FREE".equals(subscription.getPlanType())) {
            return BigDecimal.valueOf(0.60); // Prix plus élevé pour plan gratuit
        }

        // Pour les plans payants, calculer le prix unitaire
        int totalMessages =
            (subscription.getSmsLimit() != null ? subscription.getSmsLimit() : 0) +
            (subscription.getWhatsappLimit() != null ? subscription.getWhatsappLimit() : 0);

        if (totalMessages > 0) {
            return subscription.getPrice().divide(BigDecimal.valueOf(totalMessages), 4, RoundingMode.HALF_UP);
        }

        return defaultPrice;
    }

    /**
     * Calcule le prix WhatsApp selon l'abonnement
     */
    private BigDecimal calculateWhatsappPrice(UserSubscriptionDTO subscription) {
        // Prix par défaut si pas d'abonnement
        BigDecimal defaultPrice = BigDecimal.valueOf(0.30); // 0.30 MRU par WhatsApp

        if (subscription == null || subscription.getPrice() == null) {
            return defaultPrice;
        }

        // Si c'est un plan gratuit, prix fixe
        if ("FREE".equals(subscription.getPlanType())) {
            return BigDecimal.valueOf(0.40); // Prix plus élevé pour plan gratuit
        }

        // Pour les plans payants, calculer le prix unitaire (généralement moins cher que SMS)
        BigDecimal smsPrice = calculateSmsPrice(subscription);
        return smsPrice.multiply(BigDecimal.valueOf(0.8)); // WhatsApp 20% moins cher que SMS
    }

    /**
     * Calcule le solde disponible selon l'abonnement
     */
    private double calculateSubscriptionBalance(UserSubscriptionDTO subscription) {
        if (subscription == null) {
            return 0.0;
        }

        // Pour le plan gratuit, le solde correspond aux messages restants
        if ("FREE".equals(subscription.getPlanType())) {
            int remainingMessages =
                (subscription.getSmsRemaining() != null ? subscription.getSmsRemaining() : 0) +
                (subscription.getWhatsappRemaining() != null ? subscription.getWhatsappRemaining() : 0);
            return remainingMessages * 0.50; // Valeur estimée
        }

        // Pour les plans payants, utiliser le montant payé
        if (subscription.getPrice() != null) {
            return subscription.getPrice().doubleValue();
        }

        return 0.0;
    }

    /**
     * Construit les statistiques par type
     */
    private List<TypeStatsDTO> buildTypeStats(List<Object[]> statsList, BigDecimal prixSms, BigDecimal prixWhatsapp) {
        List<TypeStatsDTO> typeStats = new ArrayList<>();

        for (Object[] row : statsList) {
            String type = row[0] != null ? row[0].toString() : "";
            long total = row[1] != null ? ((Number) row[1]).longValue() : 0;
            long success = row[2] != null ? ((Number) row[2]).longValue() : 0;
            long failed = row[3] != null ? ((Number) row[3]).longValue() : 0;
            long pending = total - (success + failed);

            BigDecimal unitPrice = "SMS".equalsIgnoreCase(type) ? prixSms : prixWhatsapp;

            typeStats.add(new TypeStatsDTO(type, total, success, failed, pending, unitPrice.doubleValue()));
        }

        // Ajouter les types manquants avec des valeurs zéro
        List<String> expectedTypes = Arrays.asList("SMS", "WHATSAPP");
        for (String expectedType : expectedTypes) {
            boolean found = typeStats.stream().anyMatch(ts -> expectedType.equalsIgnoreCase(ts.getType()));
            if (!found) {
                BigDecimal price = "SMS".equalsIgnoreCase(expectedType) ? prixSms : prixWhatsapp;
                typeStats.add(new TypeStatsDTO(expectedType, 0L, 0L, 0L, 0L, price.doubleValue()));
            }
        }

        return typeStats;
    }

    /**
     * Calcule le total consommé
     */
    private double calculateTotalConsumed(List<TypeStatsDTO> typeStats) {
        return typeStats.stream().mapToDouble(t -> t.getSuccess() * t.getUnitPrice()).sum();
    }

    /**
     * Crée un abonnement par défaut
     */
    private UserSubscriptionDTO createDefaultSubscription() {
        UserSubscriptionDTO defaultSub = new UserSubscriptionDTO();
        defaultSub.setId(0L);
        defaultSub.setPlanName("Plan Gratuit");
        defaultSub.setPlanType("FREE");
        defaultSub.setStatus("ACTIVE");
        defaultSub.setIsActive(true);
        defaultSub.setSmsLimit(10);
        defaultSub.setWhatsappLimit(5);
        defaultSub.setSmsUsed(0);
        defaultSub.setWhatsappUsed(0);
        defaultSub.setSmsRemaining(10);
        defaultSub.setWhatsappRemaining(5);
        defaultSub.setPrice(BigDecimal.valueOf(25.0)); // Prix fictif pour le plan gratuit
        defaultSub.setCurrency("MRU");
        return defaultSub;
    }

    /**
     * Crée une réponse d'erreur
     */
    private StatsResponseDTO createErrorResponse(String message) {
        List<TypeStatsDTO> emptyStats = Arrays.asList(
            new TypeStatsDTO("SMS", 0L, 0L, 0L, 0L, 0.50),
            new TypeStatsDTO("WHATSAPP", 0L, 0L, 0L, 0L, 0.30)
        );

        return new StatsResponseDTO(emptyStats, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }

    /**
     * GET /api/user/subscription-info : Récupère les informations d'abonnement pour le dashboard
     */
    @GetMapping("/user/subscription-info")
    @PreAuthorize(
        "hasAnyAuthority('" + AuthoritiesConstants.ADMIN + "', '" + AuthoritiesConstants.PARTNER + "', '" + AuthoritiesConstants.USER + "')"
    )
    public ResponseEntity<SubscriptionInfoDTO> getSubscriptionInfo() {
        try {
            String currentUserLogin = getCurrentUserLoginOrThrow();
            String login = determineEffectiveUserLogin(currentUserLogin);
            log.debug("Récupération des informations d'abonnement pour: {}", login);

            SubscriptionInfoDTO info = buildSubscriptionInfo(login);
            return ResponseEntity.ok(info);
        } catch (IllegalStateException e) {
            log.warn("Erreur métier lors de la récupération des informations d'abonnement: {}", e.getMessage());
            return ResponseEntity.ok(createDefaultSubscriptionInfo());
        } catch (Exception e) {
            log.error("Erreur technique lors de la récupération des informations d'abonnement", e);
            return ResponseEntity.ok(createDefaultSubscriptionInfo());
        }
    }

    // Méthodes extraites pour améliorer la lisibilité et les tests

    private String getCurrentUserLoginOrThrow() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new IllegalStateException("L'utilisateur n'est pas authentifié."));
    }

    private SubscriptionInfoDTO buildSubscriptionInfo(String login) {
        ExtendedUser extendedUser = getExtendedUserOrThrow(login);
        UserSubscriptionDTO activeSubscription = getActiveSubscription(extendedUser.getUser().getId());
        return mapToSubscriptionInfoDTO(activeSubscription);
    }

    private ExtendedUser getExtendedUserOrThrow(String login) {
        return extendedUserRepository
            .findOneByUserLogin(login)
            .orElseThrow(() -> new IllegalStateException("Utilisateur étendu non trouvé pour ce login."));
    }

    private UserSubscriptionDTO getActiveSubscription(Long userId) {
        List<UserSubscriptionDTO> subscriptions = subscriptionService.getUserSubscriptions(userId);
        return subscriptions.isEmpty() ? createDefaultSubscription() : subscriptions.get(0);
    }

    private SubscriptionInfoDTO mapToSubscriptionInfoDTO(UserSubscriptionDTO subscription) {
        SubscriptionInfoDTO info = new SubscriptionInfoDTO();

        // Informations de base
        info.setSubscriptionType(subscription.getPlanType());
        info.setPlanName(subscription.getPlanName());

        // Capacités de communication
        info.setCanSendSMS(subscription.getSmsRemaining() > 0);
        info.setCanSendWhatsApp(subscription.getWhatsappRemaining() > 0);

        // Limites et quota
        info.setSmsRemaining(subscription.getSmsRemaining());
        info.setWhatsappRemaining(subscription.getWhatsappRemaining());
        info.setSmsLimit(subscription.getSmsLimit());
        info.setWhatsappLimit(subscription.getWhatsappLimit());

        // Permissions
        info.setCanManageTemplates(subscription.getCustomCanManageTemplates());
        info.setCanViewAnalytics(subscription.getCustomCanViewAnalytics());
        info.setCanManageUsers(subscription.getCustomCanManageUsers());

        // Informations d'expiration
        info.setIsExpiringSoon(subscription.getIsExpiringSoon());
        info.setDaysUntilExpiration(subscription.getDaysUntilExpiration());
        info.setEndDate(subscription.getEndDate());

        return info;
    }

    private SubscriptionInfoDTO createDefaultSubscriptionInfo() {
        SubscriptionInfoDTO defaultInfo = new SubscriptionInfoDTO();
        defaultInfo.setSubscriptionType("FREE");
        defaultInfo.setPlanName("Plan Gratuit");
        defaultInfo.setCanSendSMS(true);
        defaultInfo.setCanSendWhatsApp(false);
        defaultInfo.setSmsRemaining(10);
        defaultInfo.setWhatsappRemaining(0);
        defaultInfo.setSmsLimit(10);
        defaultInfo.setWhatsappLimit(0);
        defaultInfo.setCanManageTemplates(false);
        defaultInfo.setCanViewAnalytics(false);
        defaultInfo.setCanManageUsers(false);
        defaultInfo.setIsExpiringSoon(false);
        defaultInfo.setDaysUntilExpiration(null);
        defaultInfo.setEndDate(null);

        return defaultInfo;
    }
}
