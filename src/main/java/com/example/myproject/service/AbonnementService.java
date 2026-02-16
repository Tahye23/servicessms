package com.example.myproject.service;

import com.example.myproject.domain.Abonnement;
import com.example.myproject.domain.PlanAbonnement;
import com.example.myproject.domain.User;
import com.example.myproject.domain.enumeration.MessageType;
import com.example.myproject.repository.AbonnementRepository;
import com.example.myproject.repository.PlanabonnementRepository;
import com.example.myproject.repository.UserRepository;
import com.example.myproject.service.dto.AbonnementDTO;
import com.example.myproject.service.mapper.AbonnementMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AbonnementService {

    private final Logger log = LoggerFactory.getLogger(AbonnementService.class);

    private final AbonnementRepository abonnementRepository;
    private final AbonnementMapper abonnementMapper;
    private final UserRepository userRepository;
    private final PlanabonnementRepository planAbonnementRepository;

    public AbonnementService(
        AbonnementRepository abonnementRepository,
        AbonnementMapper abonnementMapper,
        UserRepository userRepository,
        PlanabonnementRepository planAbonnementRepository
    ) {
        this.abonnementRepository = abonnementRepository;
        this.abonnementMapper = abonnementMapper;
        this.userRepository = userRepository;
        this.planAbonnementRepository = planAbonnementRepository;
    }

    // ===== CRUD =====

    public AbonnementDTO save(AbonnementDTO abonnementDTO) {
        log.debug("Request to save Abonnement : {}", abonnementDTO);
        // validateAbonnementCreation(abonnementDTO);

        Abonnement abonnement = abonnementMapper.toEntity(abonnementDTO);

        if (abonnement.getStartDate() == null) {
            abonnement.setStartDate(LocalDate.now());
        }
        if (abonnement.getEndDate() == null && abonnement.getPlan() != null) {
            abonnement.setEndDate(calculateEndDate(abonnement.getStartDate(), abonnement.getPlan()));
        }
        abonnement.setCreatedDate(Instant.now());
        abonnement.setUpdatedDate(Instant.now());
        abonnement = abonnementRepository.save(abonnement);
        return enrichAbonnementDTO(abonnementMapper.toDto(abonnement));
    }

    public AbonnementDTO update(AbonnementDTO abonnementDTO) {
        log.debug("Request to update Abonnement : {}", abonnementDTO);
        Abonnement abonnement = abonnementMapper.toEntity(abonnementDTO);
        abonnement = abonnementRepository.save(abonnement);
        return enrichAbonnementDTO(abonnementMapper.toDto(abonnement));
    }

    public Optional<AbonnementDTO> partialUpdate(AbonnementDTO abonnementDTO) {
        log.debug("Request to partially update Abonnement : {}", abonnementDTO);

        return abonnementRepository
            .findById(abonnementDTO.getId())
            .map(existingAbonnement -> {
                abonnementMapper.partialUpdate(existingAbonnement, abonnementDTO);
                return existingAbonnement;
            })
            .map(abonnementRepository::save)
            .map(abonnementMapper::toDto)
            .map(this::enrichAbonnementDTO);
    }

    @Transactional(readOnly = true)
    public Page<AbonnementDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Abonnements");
        return abonnementRepository.findAll(pageable).map(abonnementMapper::toDto).map(this::enrichAbonnementDTO);
    }

    @Transactional(readOnly = true)
    public Optional<AbonnementDTO> findOne(Long id) {
        log.debug("Request to get Abonnement : {}", id);
        return abonnementRepository.findById(id).map(abonnementMapper::toDto).map(this::enrichAbonnementDTO);
    }

    public void delete(Long id) {
        log.debug("Request to delete Abonnement : {}", id);
        abonnementRepository.deleteById(id);
    }

    // ===== MÉTIER =====

    @Transactional(readOnly = true)
    public Optional<AbonnementDTO> findActiveByUserId(Long userId) {
        log.debug("Request to get active Abonnement for user : {}", userId);
        return abonnementRepository.findActivedByUserId(userId).map(abonnementMapper::toDto).map(this::enrichAbonnementDTO);
    }

    @Transactional(readOnly = true)
    public Optional<AbonnementDTO> getUserUsageStats(Long userId) {
        log.debug("Request to get usage stats for user : {}", userId);
        return findActiveByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean canUserSendSms(Long userId) {
        log.debug("Checking if user {} can send SMS", userId);
        return abonnementRepository.canUserSendSms(userId).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean canUserSendWhatsapp(Long userId) {
        log.debug("Checking if user {} can send WhatsApp", userId);
        return abonnementRepository.canUserSendWhatsapp(userId).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean canUserMakeApiCall(Long userId) {
        log.debug("Checking if user {} can make API call", userId);
        return abonnementRepository.canUserMakeApiCall(userId).orElse(false);
    }

    public boolean incrementSmsUsage(Long userId) {
        log.debug("Incrementing SMS usage for user : {}", userId);

        if (!canUserSendSms(userId)) {
            log.warn("User {} has reached SMS limit", userId);
            return false;
        }
        int updated = abonnementRepository.incrementSmsUsage(userId);
        boolean success = updated > 0;
        if (success) {
            log.info("SMS usage incremented for user : {}", userId);
            checkUsageLimits(userId);
        }
        return success;
    }

    public boolean incrementWhatsappUsage(Long userId) {
        log.debug("Incrementing WhatsApp usage for user : {}", userId);

        if (!canUserSendWhatsapp(userId)) {
            log.warn("User {} has reached WhatsApp limit", userId);
            return false;
        }
        int updated = abonnementRepository.incrementWhatsappUsage(userId);
        boolean success = updated > 0;
        if (success) {
            log.info("WhatsApp usage incremented for user : {}", userId);
            checkUsageLimits(userId);
        }
        return success;
    }

    public boolean incrementApiCallsToday(Long userId) {
        log.debug("Incrementing API calls for user : {}", userId);

        if (!canUserMakeApiCall(userId)) {
            log.warn("User {} has reached API call limit", userId);
            return false;
        }
        int updated = abonnementRepository.incrementApiCallsToday(userId);
        return updated > 0;
    }

    // ===== EXPIRATION / STATUT =====

    @Transactional(readOnly = true)
    public List<AbonnementDTO> findExpiringSoon(LocalDate expirationDate) {
        log.debug("Finding abonnements expiring before : {}", expirationDate);
        return abonnementRepository
            .findExpiringSoon(expirationDate)
            .stream()
            .map(abonnementMapper::toDto)
            .map(this::enrichAbonnementDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AbonnementDTO> findExpired() {
        log.debug("Finding expired abonnements");
        return abonnementRepository.findExpired().stream().map(abonnementMapper::toDto).map(this::enrichAbonnementDTO).toList();
    }

    public int expirePastDueAbonnements() {
        log.debug("Expiring past due abonnements");
        int expiredCount = abonnementRepository.expirePastDueAbonnements();
        int expiredTrialsCount = abonnementRepository.expireTrials();
        int totalExpired = expiredCount + expiredTrialsCount;
        if (totalExpired > 0) {
            log.info("Expired {} abonnements ({} regular, {} trials)", totalExpired, expiredCount, expiredTrialsCount);
        }
        return totalExpired;
    }

    public AbonnementDTO suspendAbonnement(Long abonnementId) {
        log.debug("Suspending abonnement : {}", abonnementId);
        abonnementRepository.suspendAbonnement(abonnementId);
        return findOne(abonnementId).orElseThrow(() -> new RuntimeException("Abonnement not found: " + abonnementId));
    }

    public AbonnementDTO reactivateAbonnement(Long abonnementId) {
        log.debug("Reactivating abonnement : {}", abonnementId);
        abonnementRepository.reactivateAbonnement(abonnementId);
        return findOne(abonnementId).orElseThrow(() -> new RuntimeException("Abonnement not found: " + abonnementId));
    }

    public AbonnementDTO renewAbonnement(Long abonnementId, int months) {
        log.debug("Renewing abonnement {} for {} months", abonnementId, months);

        Abonnement abonnement = abonnementRepository
            .findById(abonnementId)
            .orElseThrow(() -> new RuntimeException("Abonnement not found: " + abonnementId));
        LocalDate newEndDate = abonnement.getEndDate() != null
            ? abonnement.getEndDate().plusMonths(months)
            : LocalDate.now().plusMonths(months);

        abonnement.setEndDate(newEndDate);
        abonnement.setStatus(Abonnement.SubscriptionStatus.ACTIVE);

        abonnement.setSmsUsed(0);
        abonnement.setWhatsappUsed(0);
        abonnement.setApiCallsToday(0);

        abonnement = abonnementRepository.save(abonnement);

        log.info("Abonnement {} renewed until {}", abonnementId, newEndDate);
        return enrichAbonnementDTO(abonnementMapper.toDto(abonnement));
    }

    // ===== STATISTIQUES =====

    @Transactional(readOnly = true)
    public Object getSubscriptionStatistics() {
        log.debug("Getting subscription statistics");
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalActive", abonnementRepository.countActiveAbonnements());
        stats.put("newThisMonth", abonnementRepository.countNewSubscriptionsThisMonth());

        List<Object[]> statusCounts = abonnementRepository.countByStatus();
        Map<String, Long> statusStats = new HashMap<>();
        for (Object[] row : statusCounts) {
            statusStats.put(row[0].toString(), (Long) row[1]);
        }
        stats.put("byStatus", statusStats);

        List<Object[]> planCounts = abonnementRepository.countByPlan();
        Map<String, Long> planStats = new HashMap<>();
        for (Object[] row : planCounts) {
            planStats.put((String) row[0], (Long) row[1]);
        }
        stats.put("byPlan", planStats);
        LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        LocalDate endDate = startDate.plusMonths(1);

        Instant start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        stats.put("monthlyRevenue", abonnementRepository.getMonthlyRevenueByPlan(start, end));
        stats.put("smsUsage", abonnementRepository.getSmsUsageStatsByPlan());
        stats.put("whatsappUsage", abonnementRepository.getWhatsappUsageStatsByPlan());

        return stats;
    }

    // ===== OUTILS/UTILS PRIVÉS =====

    private void validateAbonnementCreation(AbonnementDTO abonnementDTO) {
        Optional<Abonnement> existingActive = abonnementRepository.findActivedByUserId(abonnementDTO.getUserId());
        if (existingActive.isPresent()) {
            throw new RuntimeException("User already has an active subscription");
        }
    }

    private LocalDate calculateEndDate(LocalDate startDate, PlanAbonnement plan) {
        // ✅ Toujours null - pas d'expiration
        return null;
    }

    private AbonnementDTO enrichAbonnementDTO(AbonnementDTO dto) {
        if (dto == null) return null;

        // Charger les infos user
        /*if (dto.getUserId() != null) {
            userRepository.findById(dto.getUserId()).ifPresent(user -> {
                dto.setUserLogin(user.getLogin());
                dto.setUserEmail(user.getEmail());
            });
        }

        // Charger les infos plan
        if (dto.getPlanId() != null) {
            planAbonnementRepository.findById(dto.getPlanId()).ifPresent(plan -> {
                dto.setPlanName(plan.getAbpName());
                dto.setPlanType(plan.getPlanType() != null ? plan.getPlanType().name() : null);
                dto.setSmsLimit(plan.getSmsLimit());
                dto.setWhatsappLimit(plan.getWhatsappLimit());
            });
        }*/

        // Si besoin : calculs avancés sur le DTO (en fonction de ton DTO)
        // dto.setSmsRemaining(...);
        // dto.setWhatsappRemaining(...);

        return dto;
    }

    private void checkUsageLimits(Long userId) {
        try {
            Optional<AbonnementDTO> abonnement = findActiveByUserId(userId);
            if (abonnement.isEmpty()) return;

            AbonnementDTO dto = abonnement.get();

            // Calcul fictif pour l'exemple (à adapter selon ton DTO)
            double smsUsagePercent = 90;
            double whatsappUsagePercent = 23;

            // ✅ PAS DE VÉRIFICATION D'EXPIRATION - Seulement quotas

            if (smsUsagePercent >= 95) {
                log.warn("User {} has used {}% of SMS quota", userId, smsUsagePercent);
                // TODO: Notification critique
            } else if (smsUsagePercent >= 80) {
                log.info("User {} has used {}% of SMS quota", userId, smsUsagePercent);
                // TODO: Notification alerte
            }

            if (whatsappUsagePercent >= 95) {
                log.warn("User {} has used {}% of WhatsApp quota", userId, whatsappUsagePercent);
                // TODO: Notification critique
            } else if (whatsappUsagePercent >= 80) {
                log.info("User {} has used {}% of WhatsApp quota", userId, whatsappUsagePercent);
                // TODO: Notification alerte
            }
        } catch (Exception e) {
            log.error("Error checking usage limits for user {}: {}", userId, e.getMessage());
        }
    }

    public boolean isOwner(Long abonnementId, String userLogin) {
        return abonnementRepository
            .findById(abonnementId)
            .map(abonnement -> abonnement.getUser().getUser().getLogin().equals(userLogin))
            .orElse(false);
    }

    // ===== MÉTHODES DE MAINTENANCE (optionnelles, pas obligatoires pour le core CRUD) =====

    public int resetMonthlyUsage() {
        log.info("Resetting monthly usage for all active subscriptions");
        return abonnementRepository.resetMonthlyUsage();
    }

    public void processAutoRenewals() {
        log.info("Processing auto-renewals");
        LocalDate renewalDate = LocalDate.now().plusDays(3);
        List<Abonnement> toRenew = abonnementRepository.findForAutoRenewal(renewalDate);
        for (Abonnement abonnement : toRenew) {
            try {
                renewAbonnement(abonnement.getId(), 1);
                log.info("Auto-renewed abonnement {} for user {}", abonnement.getId(), abonnement.getUser().getUser().getLogin());
            } catch (Exception e) {
                log.error(
                    "Failed to auto-renew abonnement {} for user {}: {}",
                    abonnement.getId(),
                    abonnement.getUser().getUser().getLogin(),
                    e.getMessage()
                );
            }
        }
    }

    // ====== RECHERCHE AVANCÉE ET EXPORTS =====

    @Transactional(readOnly = true)
    public List<AbonnementDTO> searchAbonnements(String userEmail, String planName, String status) {
        List<Abonnement> results;
        if (userEmail != null && !userEmail.trim().isEmpty()) {
            results = abonnementRepository.findByUserEmail(userEmail.trim());
        } else if (planName != null && !planName.trim().isEmpty()) {
            results = abonnementRepository.findByPlanName(planName.trim());
        } else {
            results = abonnementRepository.findAll();
        }

        // Filtrer par statut si spécifié
        if (status != null && !status.trim().isEmpty()) {
            try {
                Abonnement.SubscriptionStatus statusEnum = Abonnement.SubscriptionStatus.valueOf(status.toUpperCase());
                results = results.stream().filter(a -> a.getStatus() == statusEnum).toList();
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status filter: {}", status);
            }
        }
        return results.stream().map(abonnementMapper::toDto).map(this::enrichAbonnementDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<AbonnementDTO> findRequiringAttention() {
        log.debug("Finding subscriptions requiring attention");
        LocalDate alertDate = LocalDate.now().plusDays(7);
        return abonnementRepository
            .findRequiringAttention(alertDate)
            .stream()
            .map(abonnementMapper::toDto)
            .map(this::enrichAbonnementDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AbonnementDTO> findHighUsageAbonnements() {
        log.debug("Finding high usage subscriptions");
        return abonnementRepository
            .findHighUsageAbonnements()
            .stream()
            .map(abonnementMapper::toDto)
            .map(this::enrichAbonnementDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AbonnementDTO> exportAllAbonnements() {
        log.debug("Exporting all subscriptions with full details");
        return abonnementRepository.findAllWithDetails().stream().map(abonnementMapper::toDto).map(this::enrichAbonnementDTO).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> generateUsageReport(LocalDate startDate, LocalDate endDate) {
        log.debug("Generating usage report from {} to {}", startDate, endDate);
        Map<String, Object> report = new HashMap<>();

        Instant start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<Abonnement> createdInPeriod = abonnementRepository.findCreatedBetween(startDate, endDate);
        report.put("newSubscriptions", createdInPeriod.size());
        report.put("newSubscriptionsList", createdInPeriod.stream().map(abonnementMapper::toDto).map(this::enrichAbonnementDTO).toList());
        report.put("smsUsageByPlan", abonnementRepository.getSmsUsageStatsByPlan());
        report.put("whatsappUsageByPlan", abonnementRepository.getWhatsappUsageStatsByPlan());
        report.put("monthlyRevenue", abonnementRepository.getMonthlyRevenueByPlan(start, end));
        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> calculatePerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        long totalActive = abonnementRepository.countActiveAbonnements();
        long newThisMonth = abonnementRepository.countNewSubscriptionsThisMonth();
        metrics.put("monthlyGrowthRate", totalActive > 0 ? ((double) newThisMonth / totalActive) * 100 : 0);
        List<AbonnementDTO> attention = findRequiringAttention();
        metrics.put("requiresAttentionCount", attention.size());
        List<AbonnementDTO> highUsage = findHighUsageAbonnements();
        metrics.put("highUsageCount", highUsage.size());
        List<AbonnementDTO> expiringSoon = findExpiringSoon(LocalDate.now().plusDays(30));
        metrics.put("expiringSoonCount", expiringSoon.size());
        return metrics;
    }

    public void decrementQuotasAfterSend(List<Abonnement> subscriptions, MessageType messageType, int messageCount) {
        int remainingToDecrement = messageCount;

        for (Abonnement abonnement : subscriptions) {
            if (remainingToDecrement <= 0) break;

            // ✅ PAS DE VÉRIFICATION D'EXPIRATION - Les abonnements n'expirent jamais

            if (messageType == MessageType.SMS && hasSmsPermission(abonnement)) {
                remainingToDecrement = decrementSmsQuotaWithBonus(abonnement, remainingToDecrement);
            } else if (messageType == MessageType.WHATSAPP && hasWhatsappPermission(abonnement)) {
                remainingToDecrement = decrementWhatsappQuotaWithBonus(abonnement, remainingToDecrement);
            }

            abonnement.setUpdatedDate(Instant.now());
        }

        log.info("[DECREMENT-QUOTAS] Décrémentation terminée - Reste: {}", remainingToDecrement);
    }

    // Décrémenter le quota SMS avec gestion des bonus
    public int decrementSmsQuotaWithBonus(Abonnement abonnement, int toDecrement) {
        int remaining = toDecrement;

        // 1. Décrémenter d'abord le quota principal
        int currentUsed = abonnement.getSmsUsed() != null ? abonnement.getSmsUsed() : 0;
        int mainQuota = getMainSmsQuota(abonnement);
        int availableFromMain = Math.max(0, mainQuota - currentUsed);

        if (availableFromMain > 0 && remaining > 0) {
            int useFromMain = Math.min(remaining, availableFromMain);
            abonnement.setSmsUsed(currentUsed + useFromMain);
            remaining -= useFromMain;
        }

        // 2. Si il reste à décrémenter et qu'il y a des bonus
        if (
            remaining > 0 && abonnement.getBonusSmsEnabled() && abonnement.getBonusSmsAmount() != null && abonnement.getBonusSmsAmount() > 0
        ) {
            int useFromBonus = Math.min(remaining, abonnement.getBonusSmsAmount());
            abonnement.setBonusSmsAmount(abonnement.getBonusSmsAmount() - useFromBonus);
            remaining -= useFromBonus;
        }

        // 3. Si il reste à décrémenter et qu'il y a du carryover
        if (
            remaining > 0 &&
            abonnement.getAllowSmsCarryover() &&
            abonnement.getCarriedOverSms() != null &&
            abonnement.getCarriedOverSms() > 0
        ) {
            int useFromCarryover = Math.min(remaining, abonnement.getCarriedOverSms());
            abonnement.setCarriedOverSms(abonnement.getCarriedOverSms() - useFromCarryover);
            remaining -= useFromCarryover;
        }

        return remaining;
    }

    // Décrémenter le quota WhatsApp avec gestion des bonus
    public int decrementWhatsappQuotaWithBonus(Abonnement abonnement, int toDecrement) {
        int remaining = toDecrement;

        // 1. Décrémenter d'abord le quota principal
        int currentUsed = abonnement.getWhatsappUsed() != null ? abonnement.getWhatsappUsed() : 0;
        int mainQuota = getMainWhatsappQuota(abonnement);
        int availableFromMain = Math.max(0, mainQuota - currentUsed);

        if (availableFromMain > 0 && remaining > 0) {
            int useFromMain = Math.min(remaining, availableFromMain);
            abonnement.setWhatsappUsed(currentUsed + useFromMain);
            remaining -= useFromMain;
        }

        // 2. Si il reste à décrémenter et qu'il y a des bonus
        if (
            remaining > 0 &&
            abonnement.getBonusWhatsappEnabled() &&
            abonnement.getBonusWhatsappAmount() != null &&
            abonnement.getBonusWhatsappAmount() > 0
        ) {
            int useFromBonus = Math.min(remaining, abonnement.getBonusWhatsappAmount());
            abonnement.setBonusWhatsappAmount(abonnement.getBonusWhatsappAmount() - useFromBonus);
            remaining -= useFromBonus;
        }

        // 3. Si il reste à décrémenter et qu'il y a du carryover
        if (
            remaining > 0 &&
            abonnement.getAllowWhatsappCarryover() &&
            abonnement.getCarriedOverWhatsapp() != null &&
            abonnement.getCarriedOverWhatsapp() > 0
        ) {
            int useFromCarryover = Math.min(remaining, abonnement.getCarriedOverWhatsapp());
            abonnement.setCarriedOverWhatsapp(abonnement.getCarriedOverWhatsapp() - useFromCarryover);
            remaining -= useFromCarryover;
        }

        return remaining;
    }

    public boolean hasSmsPermission(Abonnement abonnement) {
        // Vérifier les permissions dans le plan standard
        if (abonnement.getCustomSmsLimit() > 0) {
            return true;
        }

        return false;
    }

    // Vérifier si l'abonnement a la permission WhatsApp
    public boolean hasWhatsappPermission(Abonnement abonnement) {
        // Vérifier les permissions dans le plan standard
        if (abonnement.getCustomWhatsappLimit() > 0) {
            return true;
        }

        return false;
    }

    public int getMainSmsQuota(Abonnement abonnement) {
        return abonnement.getCustomSmsLimit();
    }

    // Obtenir le quota principal WhatsApp
    public int getMainWhatsappQuota(Abonnement abonnement) {
        return abonnement.getCustomWhatsappLimit();
    }
}
