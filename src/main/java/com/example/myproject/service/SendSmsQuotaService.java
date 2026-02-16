package com.example.myproject.service;

import com.example.myproject.domain.Abonnement;
import com.example.myproject.domain.enumeration.MessageType;
import com.example.myproject.repository.AbonnementRepository;
import com.example.myproject.web.rest.errors.CustomException;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * ✅ SERVICE DÉDIÉ : Gestion des quotas SMS/WhatsApp
 */
@Service
public class SendSmsQuotaService {

    private static final Logger log = LoggerFactory.getLogger(SendSmsQuotaService.class);

    @Autowired
    private AbonnementRepository abonnementRepository;

    @Autowired
    private AbonnementService abonnementService;

    /**
     * ✅ VÉRIFIER QUOTAS AVANT ENVOI
     */
    public void verifyQuotasForSend(Long userId, MessageType messageType, int messageCount) {
        List<Abonnement> activeSubscriptions = abonnementRepository.findActiveByUserId(userId);

        if (activeSubscriptions.isEmpty()) {
            throw new CustomException("Aucun abonnement actif", HttpStatus.FORBIDDEN.value());
        }

        int totalAvailable = 0;

        for (Abonnement abonnement : activeSubscriptions) {
            if (messageType == MessageType.SMS && hasSmsPermission(abonnement)) {
                totalAvailable += calculateAvailableSms(abonnement);
            } else if (messageType == MessageType.WHATSAPP && hasWhatsappPermission(abonnement)) {
                totalAvailable += calculateAvailableWhatsapp(abonnement);
            }
        }

        if (totalAvailable < messageCount) {
            String typeStr = messageType == MessageType.SMS ? "SMS" : "WhatsApp";
            throw new CustomException(
                String.format("Quota %s insuffisant. Disponible: %d, Requis: %d", typeStr, totalAvailable, messageCount),
                HttpStatus.TOO_MANY_REQUESTS.value()
            );
        }

        log.info("[QUOTA-CHECK] ✅ User {}: {} {} disponibles ({} requis)", userId, totalAvailable, messageType, messageCount);
    }

    /**
     * ✅ DÉCRÉMENTER QUOTAS APRÈS ENVOI RÉUSSI
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decrementQuotasAfterSend(Long userId, MessageType messageType, int messageCount) {
        try {
            List<Abonnement> activeSubscriptions = abonnementRepository.findActiveByUserId(userId);

            if (activeSubscriptions.isEmpty()) {
                log.warn("[QUOTA-DECREMENT] Aucun abonnement actif pour user {}", userId);
                return;
            }

            log.info("[QUOTA-DECREMENT] User {}: Décrément de {} {}", userId, messageCount, messageType);

            abonnementService.decrementQuotasAfterSend(activeSubscriptions, messageType, messageCount);
            abonnementRepository.saveAll(activeSubscriptions);

            log.info("[QUOTA-DECREMENT] ✅ Quotas mis à jour pour user {}", userId);
        } catch (Exception e) {
            log.error("[QUOTA-DECREMENT] ❌ Erreur user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * ✅ OBTENIR QUOTAS DISPONIBLES
     */
    public QuotaInfo getAvailableQuotas(Long userId) {
        List<Abonnement> activeSubscriptions = abonnementRepository.findActiveByUserId(userId);

        int totalSms = 0;
        int totalWhatsapp = 0;

        for (Abonnement abonnement : activeSubscriptions) {
            if (hasSmsPermission(abonnement)) {
                totalSms += calculateAvailableSms(abonnement);
            }
            if (hasWhatsappPermission(abonnement)) {
                totalWhatsapp += calculateAvailableWhatsapp(abonnement);
            }
        }

        return new QuotaInfo(totalSms, totalWhatsapp);
    }

    // ===== HELPERS =====

    private boolean hasSmsPermission(Abonnement abonnement) {
        return abonnement.getCustomSmsLimit() != null && abonnement.getCustomSmsLimit() > 0;
    }

    private boolean hasWhatsappPermission(Abonnement abonnement) {
        return abonnement.getCustomWhatsappLimit() != null && abonnement.getCustomWhatsappLimit() > 0;
    }

    private int calculateAvailableSms(Abonnement abonnement) {
        int totalAvailable = 0;
        int used = abonnement.getSmsUsed() != null ? abonnement.getSmsUsed() : 0;

        // Quota principal
        int mainQuota = abonnement.getCustomSmsLimit();
        totalAvailable += Math.max(0, mainQuota - used);

        // Bonus SMS
        if (Boolean.TRUE.equals(abonnement.getBonusSmsEnabled()) && abonnement.getBonusSmsAmount() != null) {
            totalAvailable += abonnement.getBonusSmsAmount();
        }

        // SMS reportés (carryover)
        if (Boolean.TRUE.equals(abonnement.getAllowSmsCarryover()) && abonnement.getCarriedOverSms() != null) {
            totalAvailable += abonnement.getCarriedOverSms();
        }

        return totalAvailable;
    }

    private int calculateAvailableWhatsapp(Abonnement abonnement) {
        int totalAvailable = 0;
        int used = abonnement.getWhatsappUsed() != null ? abonnement.getWhatsappUsed() : 0;

        // Quota principal
        int mainQuota = abonnement.getCustomWhatsappLimit();
        totalAvailable += Math.max(0, mainQuota - used);

        // Bonus WhatsApp
        if (Boolean.TRUE.equals(abonnement.getBonusWhatsappEnabled()) && abonnement.getBonusWhatsappAmount() != null) {
            totalAvailable += abonnement.getBonusWhatsappAmount();
        }

        // WhatsApp reportés (carryover)
        if (Boolean.TRUE.equals(abonnement.getAllowWhatsappCarryover()) && abonnement.getCarriedOverWhatsapp() != null) {
            totalAvailable += abonnement.getCarriedOverWhatsapp();
        }

        return totalAvailable;
    }

    // ===== CLASSE INTERNE =====

    public static class QuotaInfo {

        private final int availableSms;
        private final int availableWhatsapp;

        public QuotaInfo(int availableSms, int availableWhatsapp) {
            this.availableSms = availableSms;
            this.availableWhatsapp = availableWhatsapp;
        }

        public int getAvailableSms() {
            return availableSms;
        }

        public int getAvailableWhatsapp() {
            return availableWhatsapp;
        }
    }
}
