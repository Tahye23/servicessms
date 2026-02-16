package com.example.myproject.service;

import com.example.myproject.domain.Abonnement;
import com.example.myproject.domain.ExtendedUser;
import com.example.myproject.domain.PlanAbonnement;
import com.example.myproject.domain.User;
import com.example.myproject.repository.AbonnementRepository;
import com.example.myproject.repository.ExtendedUserRepository;
import com.example.myproject.repository.PlanabonnementRepository;
import com.example.myproject.repository.UserRepository;
import com.example.myproject.security.SecurityUtils;
import com.example.myproject.service.dto.CreateSubscriptionRequest;
import com.example.myproject.service.dto.SubscriptionAccessDTO;
import com.example.myproject.service.dto.UpdateCounterRequest;
import com.example.myproject.service.dto.UserSubscriptionDTO;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SubscriptionService {

    private final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final AbonnementRepository abonnementRepository;
    private final PlanabonnementRepository planAbonnementRepository;
    private final ExtendedUserRepository extendedUserRepository;
    private final UserRepository userRepository;

    public SubscriptionService(
        AbonnementRepository abonnementRepository,
        PlanabonnementRepository planAbonnementRepository,
        ExtendedUserRepository extendedUserRepository,
        UserRepository userRepository
    ) {
        this.abonnementRepository = abonnementRepository;
        this.planAbonnementRepository = planAbonnementRepository;
        this.extendedUserRepository = extendedUserRepository;
        this.userRepository = userRepository;
    }

    /**
     * Obtient toutes les souscriptions d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<UserSubscriptionDTO> getUserSubscriptions(Long userId) {
        log.debug("Getting user subscriptions for user: {}", userId);

        List<Abonnement> activeSubscriptions = getActiveSubscriptions(userId);

        if (activeSubscriptions.isEmpty()) {
            return Collections.singletonList(createEmptyUserSubscriptionDTO());
        }

        UserSubscriptionDTO aggregatedDto = aggregateSubscriptions(activeSubscriptions);
        return Collections.singletonList(aggregatedDto);
    }

    // Méthodes extraites pour améliorer la lisibilité et la testabilité

    private List<Abonnement> getActiveSubscriptions(Long userId) {
        return abonnementRepository
            .findByUser_User_IdOrderByCreatedDateDesc(userId)
            .stream()
            .filter(this::isActiveSubscription)
            .collect(Collectors.toList());
    }

    private boolean isActiveSubscription(Abonnement abonnement) {
        return Abonnement.SubscriptionStatus.ACTIVE.equals(abonnement.getStatus());
    }

    private UserSubscriptionDTO aggregateSubscriptions(List<Abonnement> activeSubscriptions) {
        UserSubscriptionDTO dto = new UserSubscriptionDTO();

        // Informations de base
        setBasicInfo(dto, activeSubscriptions);

        // Dates
        setDateInfo(dto, activeSubscriptions);

        // Limites et utilisation
        setUsageLimits(dto, activeSubscriptions);

        // Prix et monnaie
        setPriceInfo(dto, activeSubscriptions);

        // Permissions
        setPermissions(dto, activeSubscriptions);

        // Bonus et carryover
        setBonusInfo(dto, activeSubscriptions);

        // Features
        setFeatures(dto, activeSubscriptions);

        // ✅ MODIFICATION : Ne jamais expirer
        setNoExpirationInfo(dto);

        return dto;
    }

    private void setBasicInfo(UserSubscriptionDTO dto, List<Abonnement> subscriptions) {
        dto.setId(subscriptions.stream().map(Abonnement::getId).max(Long::compareTo).orElse(null));

        dto.setPlanName(subscriptions.stream().map(this::getPlanName).distinct().collect(Collectors.joining(", ")));

        dto.setPlanType(determinePlanType(subscriptions));
        dto.setStatus("ACTIVE");
        dto.setIsActive(true);
    }

    private String getPlanName(Abonnement abonnement) {
        return Optional.ofNullable(abonnement.getPlan()).map(PlanAbonnement::getAbpName).orElse("Inconnu");
    }

    private String determinePlanType(List<Abonnement> subscriptions) {
        Set<String> planTypes = subscriptions.stream().map(this::extractPlanType).collect(Collectors.toSet());

        if (planTypes.size() == 1) {
            return planTypes.iterator().next();
        }

        // Logique de priorisation
        if (planTypes.contains("PREMIUM")) return "PREMIUM";
        if (planTypes.contains("SMS") && planTypes.contains("WHATSAPP")) return "PREMIUM";
        if (planTypes.contains("SMS")) return "SMS";
        if (planTypes.contains("WHATSAPP")) return "WHATSAPP";

        return "UNKNOWN";
    }

    private String extractPlanType(Abonnement abonnement) {
        return Optional.ofNullable(abonnement.getPlan()).map(PlanAbonnement::getPlanType).map(Enum::name).orElse("UNKNOWN");
    }

    private void setDateInfo(UserSubscriptionDTO dto, List<Abonnement> subscriptions) {
        dto.setStartDate(
            subscriptions.stream().map(Abonnement::getStartDate).filter(Objects::nonNull).min(LocalDate::compareTo).orElse(null)
        );

        // ✅ MODIFICATION : endDate peut être null (pas d'expiration)
        dto.setEndDate(null); // Toujours null pour pas d'expiration

        dto.setCreatedDate(
            subscriptions.stream().map(Abonnement::getCreatedDate).filter(Objects::nonNull).max(Instant::compareTo).orElse(null)
        );
    }

    private void setUsageLimits(UserSubscriptionDTO dto, List<Abonnement> subscriptions) {
        UsageLimits limits = calculateUsageLimits(subscriptions);

        dto.setSmsLimit(limits.smsLimit);
        dto.setWhatsappLimit(limits.whatsappLimit);
        dto.setSmsUsed(limits.smsUsed);
        dto.setWhatsappUsed(limits.whatsappUsed);
        dto.setSmsRemaining(Math.max(0, limits.smsLimit - limits.smsUsed));
        dto.setWhatsappRemaining(Math.max(0, limits.whatsappLimit - limits.whatsappUsed));
    }

    private UsageLimits calculateUsageLimits(List<Abonnement> subscriptions) {
        int smsLimit = subscriptions.stream().mapToInt(this::getEffectiveSmsLimit).sum();

        int whatsappLimit = subscriptions.stream().mapToInt(this::getEffectiveWhatsappLimit).sum();

        int smsUsed = subscriptions.stream().mapToInt(ab -> Optional.ofNullable(ab.getSmsUsed()).orElse(0)).sum();

        int whatsappUsed = subscriptions.stream().mapToInt(ab -> Optional.ofNullable(ab.getWhatsappUsed()).orElse(0)).sum();

        return new UsageLimits(smsLimit, whatsappLimit, smsUsed, whatsappUsed);
    }

    private int getEffectiveSmsLimit(Abonnement abonnement) {
        return Optional.ofNullable(abonnement.getCustomSmsLimit()).orElseGet(
            () -> Optional.ofNullable(abonnement.getPlan()).map(PlanAbonnement::getSmsLimit).orElse(0)
        );
    }

    private int getEffectiveWhatsappLimit(Abonnement abonnement) {
        return Optional.ofNullable(abonnement.getCustomWhatsappLimit()).orElseGet(
            () -> Optional.ofNullable(abonnement.getPlan()).map(PlanAbonnement::getWhatsappLimit).orElse(0)
        );
    }

    private void setPriceInfo(UserSubscriptionDTO dto, List<Abonnement> subscriptions) {
        BigDecimal totalPrice = subscriptions.stream().map(this::getPlanPrice).reduce(BigDecimal.ZERO, BigDecimal::add);

        dto.setPrice(totalPrice);

        String currency = subscriptions.stream().map(this::getPlanCurrency).filter(Objects::nonNull).findFirst().orElse("UNKNOWN");

        dto.setCurrency(currency);
    }

    private BigDecimal getPlanPrice(Abonnement abonnement) {
        return Optional.ofNullable(abonnement.getPlan()).map(PlanAbonnement::getAbpPrice).orElse(BigDecimal.ZERO);
    }

    private String getPlanCurrency(Abonnement abonnement) {
        return Optional.ofNullable(abonnement.getPlan()).map(PlanAbonnement::getAbpCurrency).orElse(null);
    }

    private void setPermissions(UserSubscriptionDTO dto, List<Abonnement> subscriptions) {
        dto.setCustomCanManageUsers(hasAnyPermission(subscriptions, Abonnement::getCustomCanManageUsers));
        dto.setSidebarVisible(hasAnyPermission(subscriptions, Abonnement::getSidebarVisible));
        dto.setCustomCanManageTemplates(hasAnyPermission(subscriptions, Abonnement::getCustomCanManageTemplates));
        dto.setCustomCanViewConversations(hasAnyPermission(subscriptions, Abonnement::getCustomCanViewConversations));
        dto.setCustomCanViewAnalytics(hasAnyPermission(subscriptions, Abonnement::getCustomCanViewAnalytics));
        dto.setCustomPrioritySupport(hasAnyPermission(subscriptions, Abonnement::getCustomPrioritySupport));
        dto.setCanViewDashboard(hasAnyPermission(subscriptions, Abonnement::getCanViewDashboard));
        dto.setCanManageAPI(hasAnyPermission(subscriptions, Abonnement::getCanManageAPI));
    }

    private boolean hasAnyPermission(List<Abonnement> subscriptions, Function<Abonnement, Boolean> permissionExtractor) {
        return subscriptions.stream().map(permissionExtractor).anyMatch(Boolean.TRUE::equals);
    }

    private void setBonusInfo(UserSubscriptionDTO dto, List<Abonnement> subscriptions) {
        dto.setBonusSmsEnabled(hasAnyBonusEnabled(subscriptions, Abonnement::getBonusSmsEnabled));
        dto.setBonusSmsAmount(sumBonusAmounts(subscriptions, Abonnement::getBonusSmsEnabled, Abonnement::getBonusSmsAmount));

        dto.setBonusWhatsappEnabled(hasAnyBonusEnabled(subscriptions, Abonnement::getBonusWhatsappEnabled));
        dto.setBonusWhatsappAmount(sumBonusAmounts(subscriptions, Abonnement::getBonusWhatsappEnabled, Abonnement::getBonusWhatsappAmount));

        dto.setAllowSmsCarryover(hasAnyBonusEnabled(subscriptions, Abonnement::getAllowSmsCarryover));
        dto.setAllowWhatsappCarryover(hasAnyBonusEnabled(subscriptions, Abonnement::getAllowWhatsappCarryover));

        dto.setCarriedOverSms(sumCarryover(subscriptions, Abonnement::getCarriedOverSms));
        dto.setCarriedOverWhatsapp(sumCarryover(subscriptions, Abonnement::getCarriedOverWhatsapp));
    }

    private boolean hasAnyBonusEnabled(List<Abonnement> subscriptions, Function<Abonnement, Boolean> bonusExtractor) {
        return subscriptions.stream().map(bonusExtractor).anyMatch(Boolean.TRUE::equals);
    }

    private int sumBonusAmounts(
        List<Abonnement> subscriptions,
        Function<Abonnement, Boolean> enabledExtractor,
        Function<Abonnement, Integer> amountExtractor
    ) {
        return subscriptions
            .stream()
            .filter(ab -> Boolean.TRUE.equals(enabledExtractor.apply(ab)))
            .map(amountExtractor)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .sum();
    }

    private int sumCarryover(List<Abonnement> subscriptions, Function<Abonnement, Integer> carryoverExtractor) {
        return subscriptions.stream().map(carryoverExtractor).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
    }

    private void setFeatures(UserSubscriptionDTO dto, List<Abonnement> subscriptions) {
        Set<String> allFeatures = subscriptions.stream().flatMap(this::extractFeatures).collect(Collectors.toSet());

        dto.setFeatures(allFeatures);
    }

    private Stream<String> extractFeatures(Abonnement abonnement) {
        List<String> features = new ArrayList<>();

        addFeatureIfEnabled(features, abonnement.getCustomCanManageUsers(), "manage-users");
        addFeatureIfEnabled(features, abonnement.getCustomCanManageTemplates(), "manage-templates");
        addFeatureIfEnabled(features, abonnement.getCustomCanViewConversations(), "view-conversations");
        addFeatureIfEnabled(features, abonnement.getCustomCanViewAnalytics(), "view-analytics");
        addFeatureIfEnabled(features, abonnement.getCustomPrioritySupport(), "priority-support");
        addFeatureIfEnabled(features, abonnement.getCanViewDashboard(), "view-dashboard");
        addFeatureIfEnabled(features, abonnement.getCanManageAPI(), "manage-api");
        addFeatureIfEnabled(features, abonnement.getBonusSmsEnabled(), "bonus-sms");
        addFeatureIfEnabled(features, abonnement.getBonusWhatsappEnabled(), "bonus-whatsapp");

        return features.stream();
    }

    private void addFeatureIfEnabled(List<String> features, Boolean enabled, String featureName) {
        if (Boolean.TRUE.equals(enabled)) {
            features.add(featureName);
        }
    }

    /**
     * ✅ NOUVELLE MÉTHODE : Pas d'expiration
     */
    private void setNoExpirationInfo(UserSubscriptionDTO dto) {
        dto.setDaysUntilExpiration(null); // Pas de limite
        dto.setIsExpiringSoon(false); // Jamais en expiration
    }

    /**
     * ✅ ANCIENNE MÉTHODE COMMENTÉE (ne plus utiliser)
     */
    /*
    private void setExpirationInfo(UserSubscriptionDTO dto) {
        if (dto.getEndDate() != null) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), dto.getEndDate());
            dto.setDaysUntilExpiration(Math.max(0, daysLeft));
            dto.setIsExpiringSoon(daysLeft > 0 && daysLeft <= 7);
        } else {
            dto.setDaysUntilExpiration(null);
            dto.setIsExpiringSoon(false);
        }
    }
    */

    // Classe interne pour les limites d'utilisation
    private static class UsageLimits {

        final int smsLimit;
        final int whatsappLimit;
        final int smsUsed;
        final int whatsappUsed;

        UsageLimits(int smsLimit, int whatsappLimit, int smsUsed, int whatsappUsed) {
            this.smsLimit = smsLimit;
            this.whatsappLimit = whatsappLimit;
            this.smsUsed = smsUsed;
            this.whatsappUsed = whatsappUsed;
        }
    }

    private UserSubscriptionDTO createEmptyUserSubscriptionDTO() {
        UserSubscriptionDTO dto = new UserSubscriptionDTO();
        dto.setPlanType("NONE");
        dto.setSmsLimit(0);
        dto.setWhatsappLimit(0);
        dto.setCanViewDashboard(false);
        dto.setSmsUsed(0);
        dto.setWhatsappUsed(0);
        dto.setSmsRemaining(0);
        dto.setWhatsappRemaining(0);
        dto.setIsActive(false);
        dto.setStatus("INACTIVE");
        dto.setDaysUntilExpiration(null);
        dto.setIsExpiringSoon(false); // ✅ Toujours false
        return dto;
    }

    @Transactional
    public void updateSidebarVisibilityForAllUserSubscriptions(Long userId, Boolean visible) {
        List<Abonnement> abonnements = abonnementRepository.findByUser_User_IdOrderByCreatedDateDesc(userId);

        // Filtrer abonnements actifs (optionnel, adapte selon besoin)
        List<Abonnement> activeSubs = abonnements
            .stream()
            .filter(ab -> Abonnement.SubscriptionStatus.ACTIVE.equals(ab.getStatus()))
            .collect(Collectors.toList());

        // Mettre à jour tous les abonnements filtrés
        for (Abonnement ab : activeSubs) {
            ab.setSidebarVisible(visible);
        }

        abonnementRepository.saveAll(activeSubs);
    }

    /**
     * Crée un nouvel abonnement
     */
    @Transactional
    public UserSubscriptionDTO createSubscription(CreateSubscriptionRequest request) {
        log.debug("Creating subscription: {}", request);

        User user = userRepository
            .findById(request.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found: " + request.getUserId()));
        ExtendedUser extendedUser = extendedUserRepository
            .findOneByUserLogin(user.getLogin())
            .orElseThrow(() -> new RuntimeException("User not found: " + user.getLogin()));
        PlanAbonnement plan = planAbonnementRepository
            .findById(request.getPlanId())
            .orElseThrow(() -> new RuntimeException("Plan not found: " + request.getPlanId()));

        // Vérifier si l'utilisateur a déjà un abonnement actif pour ce plan
        List<Abonnement> existingActive = abonnementRepository.findActiveByUserIdAndPlanType(request.getUserId(), plan.getPlanType());

        if (!existingActive.isEmpty()) {
            log.warn("User {} already has active subscription for plan type {}", request.getUserId(), plan.getPlanType());
            // Optionnel: désactiver l'ancien ou permettre plusieurs abonnements
        }

        // Créer le nouvel abonnement
        Abonnement abonnement = new Abonnement();
        abonnement.setUser(extendedUser);
        abonnement.setPlan(plan);
        abonnement.setStartDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now());

        // ✅ MODIFICATION : Pas de date de fin
        abonnement.setEndDate(null); // Jamais d'expiration

        abonnement.setPaymentMethod(request.getPaymentMethod());
        abonnement.setTransactionId(request.getTransactionId());
        abonnement.setAutoRenew(false); // ✅ Pas besoin de renouvellement automatique
        abonnement.setStatus(Abonnement.SubscriptionStatus.ACTIVE);

        abonnement = abonnementRepository.save(abonnement);

        // Mettre à jour l'ExtendedUser
        updateExtendedUserAfterSubscription(user, plan);

        log.info("Created subscription {} for user {}", abonnement.getId(), user.getId());
        return new UserSubscriptionDTO(abonnement);
    }

    /**
     * Annule un abonnement
     */
    @Transactional
    public void cancelSubscription(Long subscriptionId, Long userId) {
        log.debug("Cancelling subscription: {} for user: {}", subscriptionId, userId);

        Abonnement abonnement = abonnementRepository
            .findById(subscriptionId)
            .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));

        if (!abonnement.getUser().getId().equals(userId)) {
            throw new RuntimeException("Subscription does not belong to user");
        }

        abonnement.setStatus(Abonnement.SubscriptionStatus.CANCELLED);
        abonnement.setAutoRenew(false);
        abonnementRepository.save(abonnement);

        log.info("Cancelled subscription {} for user {}", subscriptionId, userId);
    }

    /**
     * ✅ MODIFICATION : Cette méthode ne fait plus rien (pas d'expiration)
     */
    @Transactional
    public void checkAndUpdateExpiredSubscriptions() {
        log.debug("Checking for expired subscriptions - DISABLED (no expiration)");
        // Ne rien faire - les abonnements n'expirent jamais
    }

    /**
     * ✅ MODIFICATION : Retourne toujours une liste vide (pas d'expiration)
     */
    @Transactional(readOnly = true)
    public List<UserSubscriptionDTO> getExpiringSoonSubscriptions(int days) {
        log.debug("Getting expiring soon subscriptions - DISABLED (no expiration)");
        return Collections.emptyList(); // Aucun abonnement n'expire
    }

    /**
     * ✅ MODIFICATION : Cette méthode n'est plus nécessaire (pas de renouvellement)
     */
    @Transactional
    public UserSubscriptionDTO renewSubscription(Long subscriptionId) {
        log.debug("Renewing subscription - DISABLED (no expiration)");
        throw new UnsupportedOperationException("Subscription renewal is disabled - subscriptions never expire");
    }

    private void updateExtendedUserAfterSubscription(User user, PlanAbonnement plan) {
        ExtendedUser extendedUser = extendedUserRepository.findByUserId(user.getId());
        if (extendedUser == null) {
            extendedUser = new ExtendedUser(user);
        }

        // Mettre à jour les quotas si nécessaire
        if (extendedUser.getSubscriptionStartDate() == null) {
            extendedUser.setSubscriptionStartDate(LocalDate.now());
        }

        extendedUserRepository.save(extendedUser);
    }
    /**
     * ✅ SUPPRIMÉE : Plus besoin de calculer la date de fin
     */
    /*
    private LocalDate calculateNewEndDate(LocalDate startDate, PlanAbonnement plan) {
        String period = plan.getAbpPeriod();
        if ("MONTHLY".equals(period)) {
            return startDate.plusMonths(1);
        } else if ("YEARLY".equals(period)) {
            return startDate.plusYears(1);
        } else if ("LIFETIME".equals(period)) {
            return null; // Pas d'expiration
        } else {
            return startDate.plusMonths(1); // Par défaut mensuel
        }
    }
    */
}
