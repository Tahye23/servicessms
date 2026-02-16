package com.example.myproject.web.rest;

import com.example.myproject.SMSService;
import com.example.myproject.domain.*;
import com.example.myproject.domain.enumeration.MessageType;
import com.example.myproject.repository.*;
import com.example.myproject.security.SecurityUtils;
import com.example.myproject.service.*;
import com.example.myproject.web.rest.dto.*;
import com.example.myproject.web.rest.errors.*;
import com.example.myproject.web.rest.errors.CustomException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 *  CONTRÔLEUR MODERNE : SendSms (nettoyé et optimisé)
 */
@RestController
@RequestMapping("/api/send-sms")
public class SendSmsResource {

    private static final Logger log = LoggerFactory.getLogger(SendSmsResource.class);
    private static final String ENTITY_NAME = "sendSms";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    // ===== REPOSITORIES =====
    @Autowired
    private SendSmsRepository sendSmsRepository;

    @Autowired
    private SmsRepository smsRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private AbonnementRepository abonnementRepository;

    @Autowired
    private ExtendedUserRepository extendedUserRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContactRepository contactRepository;

    // ===== SERVICES =====
    @Autowired
    private SendSmsCreationService creationService;

    @Autowired
    private SendSmsProgressService progressService;

    @Autowired
    private SendSmsQuotaService quotaService;

    @Autowired
    private SmsBulkService smsBulkService;

    @Autowired
    private WhatsAppBulkService whatsAppBulkService;

    @Autowired
    private SMSService smsService;

    @Autowired
    private SendWhatsappService sendWhatsappService;

    @Autowired
    private CampaignHistoryService campaignHistoryService;

    @Autowired
    private SmsResetService smsResetService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    // ===== CRÉATION SMS =====

    /**
     *  CRÉER UN NOUVEAU SMS (unitaire ou bulk)
     */
    @PostMapping("")
    public ResponseEntity<SendSms> createSendSms(@RequestBody SendSms sendSms, @RequestParam(defaultValue = "false") boolean test)
        throws URISyntaxException, JsonProcessingException {
        log.info("[CREATE-SMS] Type: {}, Bulk: {}", sendSms.getType(), sendSms.getIsbulk());

        // Validation
        if (sendSms.getId() != null) {
            throw new BadRequestAlertException("Un nouveau SMS ne peut avoir un ID", ENTITY_NAME, "idexists");
        }

        // Authentification
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new IllegalStateException("Non authentifié"));

        // Utilisateur
        ExtendedUser extendedUser = resolveExtendedUser(sendSms, login);
        sendSms.setUser(extendedUser);

        // Template
        Template template = templateRepository
            .findById(sendSms.getTemplate_id())
            .orElseThrow(() -> new ResourceNotFoundException("Template introuvable"));

        // Vérification quotas
        if (!test) {
            int messageCount = sendSms.getIsbulk() ? calculateBulkMessageCount(sendSms) : 1;

            quotaService.verifyQuotasForSend(extendedUser.getId(), sendSms.getType(), messageCount);
        }

        // Création
        SendSms created;
        if (Boolean.TRUE.equals(sendSms.getIsbulk())) {
            Long groupeId = Optional.ofNullable(sendSms.getDestinataires())
                .map(Groupe::getId)
                .orElseThrow(() -> new CustomException("Groupe requis", HttpStatus.BAD_REQUEST.value()));

            created = creationService.createBulkSms(sendSms, template, groupeId, login);
        } else {
            Contact contact = Optional.ofNullable(sendSms.getDestinateur()).orElseGet(() -> {
                Contact c = new Contact();
                c.setContelephone(sendSms.getReceiver());
                return c;
            });

            created = creationService.createSingleSms(sendSms, template, contact, login);
        }

        return ResponseEntity.created(new URI("/api/send-sms/" + created.getId())).body(created);
    }

    /**
     *  REFRESH : Ajouter nouveaux contacts
     */
    @PostMapping("/{sendSmsId}/refresh")
    @Transactional
    public ResponseEntity<SendSms> refreshSendSms(@PathVariable Long sendSmsId) throws JsonProcessingException {
        SendSms sendSms = sendSmsRepository.findById(sendSmsId).orElseThrow(() -> new ResourceNotFoundException("SendSms introuvable"));

        Long groupeId = Optional.ofNullable(sendSms.getDestinataires()).map(Groupe::getId).orElse(null);

        if (groupeId == null) {
            return ResponseEntity.ok(sendSms);
        }

        SendSms refreshed = creationService.refreshBulkSms(sendSmsId, groupeId);
        return ResponseEntity.ok(refreshed);
    }

    // ===== ENVOI SMS =====

    /**
     *  ENVOYER UNE CAMPAGNE
     */
    @PostMapping("/send/{id}")
    @Transactional
    public ResponseEntity<SendSmsResponseDTO> sendSmsById(
        @PathVariable Long id,
        @RequestParam(value = "test", required = false, defaultValue = "false") boolean test
    ) {
        try {
            String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new IllegalStateException("Non authentifié"));

            SendSms sendSms = sendSmsRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("SMS introuvable"));

            Template template = templateRepository
                .findById(sendSms.getTemplate_id())
                .orElseThrow(() -> new ResourceNotFoundException("Template introuvable"));

            return sendSms.getIsbulk() ? handleBulkSms(sendSms, template, test, login) : handleSingleSms(sendSms, template, test, login);
        } catch (CustomException ex) {
            return ResponseEntity.status(ex.getHttpStatusCode()).body(SendSmsResponseDTO.empty());
        } catch (Exception ex) {
            log.error("Erreur envoi", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(SendSmsResponseDTO.empty());
        }
    }

    /**
     *  BULK SMS -
     */
    public ResponseEntity<SendSmsResponseDTO> handleBulkSms(SendSms sendSms, Template template, boolean test, String login) {
        String bulkId = sendSms.getBulkId();
        Long sendSmsId = sendSms.getId();

        Long totalPending = smsRepository.countByBulkIdAndDeliveryStatus(bulkId, "pending");

        if (totalPending == 0) {
            return ResponseEntity.ok(new SendSmsResponseDTO(bulkId, sendSms.getTotalRecipients(), true, sendSms.getIsSent()));
        }

        if (!test) {
            quotaService.verifyQuotasForSend(sendSms.getUser().getId(), sendSms.getType(), totalPending.intValue());
        }

        int newRetryCount = sendSms.getRetryCount() != null ? sendSms.getRetryCount() + 1 : 1;

        campaignHistoryService.saveRetryAttempt(sendSms, newRetryCount);

        sendSms.setInprocess(true);
        sendSms.setRetryCount(newRetryCount);
        sendSms.setLastRetryDate(Instant.now());
        sendSms.setTotalPending(totalPending.intValue());
        sendSms.setIsSent(null);
        sendSmsRepository.save(sendSms);

        if (sendSms.getType() == MessageType.WHATSAPP) {
            whatsAppBulkService.processBulkByBatch(bulkId, test, sendSmsId, template.getId(), login);
        } else {
            smsBulkService.processBulkByBatch(bulkId, test, sendSmsId, template.getId(), login);
        }

        return ResponseEntity.ok(new SendSmsResponseDTO(bulkId, sendSms.getTotalRecipients(), true, sendSms.getIsSent()));
    }

    /**
     *  SINGLE SMS
     */
    @Transactional
    public ResponseEntity<SendSmsResponseDTO> handleSingleSms(SendSms sendSms, Template template, boolean test, String login) {
        int messageCount = sendSms.getTotalMessage() != null ? sendSms.getTotalMessage() : 1;

        if (!test) {
            quotaService.verifyQuotasForSend(sendSms.getUser().getId(), sendSms.getType(), messageCount);
        }

        List<VariableDTO> varsList = parseVarsJson(sendSms.getVars());
        boolean success = false;
        String messageId = null;
        String error = null;

        SmsSendResult smsResult = null;

        try {
            if (sendSms.getType() == MessageType.WHATSAPP) {
                if (test) {
                    success = true;
                } else {
                    SendMessageResult result = sendWhatsappService.sendMessageAndGetId(sendSms.getReceiver(), template, varsList, login);
                    success = result.isSuccess();
                    messageId = result.getMessageId();
                    error = result.getError();
                }
            } else {
                smsResult = smsService.send(sendSms.getSender(), sendSms.getReceiver(), sendSms.getMsgdata());
                success = smsResult.isSuccess();
                messageId = smsResult.getMessageId();
                error = smsResult.getError();
            }
        } catch (Exception ex) {
            error = ex.getMessage();
            success = false;
        }

        sendSms.setIsSent(success);
        sendSms.setMessageId(messageId);
        sendSms.setDeliveryStatus(success ? "sent" : "failed");
        sendSms.setLast_error(error);
        sendSmsRepository.save(sendSms);

        if (success && !test) {
            quotaService.decrementQuotasAfterSend(sendSms.getUser().getId(), sendSms.getType(), messageCount);
        }

        return ResponseEntity.ok(new SendSmsResponseDTO(null, messageCount, true, success));
    }

    // ===== ENVOI SMS UNITAIRE =====

    /**
     *  ENVOYER UN SMS SPÉCIFIQUE PAR ID
     */
    @PostMapping("/sms/{smsId}/send")
    @Transactional
    public ResponseEntity<Map<String, Object>> sendSingleSmsById(
        @PathVariable Long smsId,
        @RequestParam(value = "test", required = false, defaultValue = "false") boolean test
    ) {
        log.info("[SEND-SINGLE] SMS ID: {}, test: {}", smsId, test);

        try {
            Sms sms = smsRepository.findById(smsId).orElseThrow(() -> new ResourceNotFoundException("SMS introuvable"));

            SendSms sendSms = sms.getBatch();
            if (sendSms == null) {
                throw new CustomException("SMS non lié à un SendSms", HttpStatus.BAD_REQUEST.value());
            }

            Template template = templateRepository
                .findById(sms.getTemplate_id())
                .orElseThrow(() -> new ResourceNotFoundException("Template introuvable"));

            String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new IllegalStateException("Non authentifié"));

            // Vérification quotas
            int messageCount = sms.getTotalMessage() != null ? sms.getTotalMessage() : 1;
            if (!test) {
                quotaService.verifyQuotasForSend(sendSms.getUser().getId(), sms.getType(), messageCount);
            }

            String oldStatus = sms.getDeliveryStatus();

            // Envoi
            boolean success = false;
            String messageId = null;
            String error = null;

            if (sms.getType() == MessageType.WHATSAPP) {
                List<VariableDTO> varsList = parseVarsJson(sms.getVars());
                if (test) {
                    success = true;
                    messageId = "TEST_WA_" + System.currentTimeMillis();
                } else {
                    SendMessageResult result = sendWhatsappService.sendMessageAndGetId(sms.getReceiver(), template, varsList, login);
                    success = result.isSuccess();
                    messageId = result.getMessageId();
                    error = result.getError();
                }
            } else {
                if (test) {
                    success = true;
                    messageId = "TEST_SMS_" + System.currentTimeMillis();
                } else {
                    success = smsService.goforSend(sms.getSender(), sms.getReceiver(), sms.getMsgdata());
                }
            }

            // Mise à jour SMS
            String newStatus = success ? "sent" : "failed";
            sms.setStatus(success ? "SENT" : "FAILED");
            sms.setDeliveryStatus(newStatus);
            sms.setSent(success);
            sms.setSendDate(Instant.now());
            sms.setMessageId(messageId);
            sms.setLast_error(error);
            smsRepository.save(sms);

            // Mise à jour compteurs SendSms
            updateSendSmsCounters(sendSms, oldStatus, newStatus);

            // Décrémenter quotas
            if (success && !test) {
                quotaService.decrementQuotasAfterSend(sendSms.getUser().getId(), sms.getType(), messageCount);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("smsId", smsId);
            response.put("messageId", messageId);
            response.put("status", newStatus);
            response.put("error", error);

            return ResponseEntity.ok(response);
        } catch (CustomException ex) {
            return ResponseEntity.status(ex.getHttpStatusCode()).body(Map.of("success", false, "error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("[SEND-SINGLE] Erreur", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "error", ex.getMessage()));
        }
    }

    /**
     *  ENVOYER À UN GROUPE TEST
     */
    @PostMapping("/{sendSmsId}/send-test")
    @Transactional
    public ResponseEntity<Map<String, Object>> sendToTestGroup(@PathVariable Long sendSmsId, @RequestParam Long testGroupId) {
        try {
            String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new IllegalStateException("Non authentifié"));

            SendSms sendSms = sendSmsRepository.findById(sendSmsId).orElseThrow(() -> new ResourceNotFoundException("SendSms introuvable"));

            Template template = templateRepository
                .findById(sendSms.getTemplate_id())
                .orElseThrow(() -> new ResourceNotFoundException("Template introuvable"));

            // Vérifier quotas pour TOUS les contacts du groupe test
            int totalContacts = contactRepository.countByGroupeId(testGroupId);
            quotaService.verifyQuotasForSend(sendSms.getUser().getId(), sendSms.getType(), totalContacts);

            // TODO: Implémenter l'envoi réel

            return ResponseEntity.ok(Map.of("success", true, "message", "Envoi test lancé"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "error", ex.getMessage()));
        }
    }

    // ===== MONITORING & STATS =====

    /**
     *  PROGRESSION D'UNE CAMPAGNE
     */
    @GetMapping("/sms/bulk-progress/{sendSmsId}")
    public ResponseEntity<BulkProgressResponse> getBulkProgress(@PathVariable Long sendSmsId) {
        try {
            BulkProgressResponse response = progressService.calculateBulkProgress(sendSmsId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[PROGRESS] Erreur sendSmsId {}: {}", sendSmsId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(BulkProgressResponse.error(e.getMessage()));
        }
    }

    /**
     *  LISTE DES SMS D'UNE CAMPAGNE (avec filtres)
     */
    @GetMapping("/sms/by-bulk/{bulkId}")
    public ResponseEntity<Map<String, Object>> getSmsByBulk(
        @PathVariable Long bulkId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String deliveryStatus,
        @RequestParam(required = false) Instant dateFrom,
        @RequestParam(required = false) Instant dateTo
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size);

            Page<Sms> smsPage = smsRepository.findByBulkIdWithFiltersNative(bulkId, search, deliveryStatus, dateFrom, dateTo, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("content", smsPage.getContent());
            response.put("totalPages", smsPage.getTotalPages());
            response.put("totalElements", smsPage.getTotalElements());
            response.put("number", smsPage.getNumber());
            response.put("size", smsPage.getSize());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[SMS-LIST] Erreur bulkId {}: {}", bulkId, e.getMessage());
            return ResponseEntity.ok(Map.of("content", List.of(), "totalPages", 0, "totalElements", 0));
        }
    }

    /**
     *  EXPORTER SMS EN CSV
     */
    @GetMapping("/sms/by-bulk/{bulkId}/export/csv")
    public ResponseEntity<byte[]> exportSmsContactsToCsv(
        @PathVariable Long bulkId,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String deliveryStatus,
        @RequestParam(required = false) Instant dateFrom,
        @RequestParam(required = false) Instant dateTo
    ) {
        try {
            List<Sms> allSms = smsRepository.findAllByBulkIdWithFilters(bulkId, search, deliveryStatus, dateFrom, dateTo);

            byte[] csvContent = generateCsvContent(allSms);

            String filename = String.format("sms-contacts-%d-%s.csv", bulkId, Instant.now().toString().replace(":", "-"));

            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + filename)
                .header("Content-Type", "text/csv")
                .body(csvContent);
        } catch (Exception e) {
            log.error("[CSV-EXPORT] Erreur bulkId {}: {}", bulkId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== ACTIONS =====

    /**
     *  ARRÊTER UNE CAMPAGNE
     */
    @PostMapping("/{id}/stop")
    public ResponseEntity<Map<String, Object>> stopBulkSending(@PathVariable Long id) {
        log.info("[STOP] SendSms ID: {}", id);

        try {
            boolean stopped = smsBulkService.stopBulkProcessing(id);

            SendSms sendSms = sendSmsRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("SendSms introuvable"));

            String bulkId = sendSms.getBulkId();
            int currentRetryCount = sendSms.getRetryCount() != null ? sendSms.getRetryCount() : 1;

            long sent = smsRepository.countByBulkIdAndStatus(bulkId, "SENT");
            long failed = smsRepository.countByBulkIdAndStatus(bulkId, "FAILED");
            long pending = smsRepository.countByBulkId(bulkId) - sent - failed;

            campaignHistoryService.updateRetryAttemptStopped(
                id,
                currentRetryCount,
                (int) sent,
                (int) failed,
                (int) pending,
                "Arrêté par utilisateur"
            );

            sendSms.setInprocess(false);
            sendSmsRepository.save(sendSms);

            return ResponseEntity.ok(
                Map.of(
                    "success",
                    true,
                    "stopped",
                    stopped,
                    "message",
                    "Campagne arrêtée",
                    "stats",
                    Map.of("sent", sent, "failed", failed, "pending", pending)
                )
            );
        } catch (Exception e) {
            log.error("[STOP] Erreur", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     *  RÉINITIALISER SMS ÉCHOUÉS
     */
    @PostMapping("/{id}/reset-failed")
    @Transactional
    public ResponseEntity<Map<String, Object>> resetFailedSms(@PathVariable Long id) {
        log.info("[RESET] SendSms ID: {}", id);

        try {
            SendSms sendSms = sendSmsRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("SendSms introuvable"));

            if (Boolean.TRUE.equals(sendSms.getInprocess())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("success", false, "message", "Envoi en cours"));
            }

            Map<String, Integer> statsBefore = smsResetService.getResetStatistics(id);
            SmsResetService.ResetResult result = smsResetService.resetFailedSms(id);

            if (result.hasReset()) {
                sendSms.setTotalPending(sendSms.getTotalPending() + result.getActuallyResetCount());
                sendSms.setTotalFailed(Math.max(0, sendSms.getTotalFailed() - result.getActuallyResetCount()));
                sendSmsRepository.save(sendSms);
            }

            Map<String, Integer> statsAfter = smsResetService.getResetStatistics(id);

            return ResponseEntity.ok(
                Map.of(
                    "success",
                    true,
                    "resetCount",
                    result.getActuallyResetCount(),
                    "statsBefore",
                    statsBefore,
                    "statsAfter",
                    statsAfter,
                    "message",
                    result.getActuallyResetCount() + " SMS réinitialisés"
                )
            );
        } catch (Exception e) {
            log.error("[RESET] Erreur", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     *  VÉRIFIER SI RESET NÉCESSAIRE
     */
    @GetMapping("/{id}/reset-needed")
    public ResponseEntity<Map<String, Object>> checkResetNeeded(@PathVariable Long id) {
        try {
            boolean needed = smsResetService.isResetNeeded(id);
            Map<String, Integer> stats = smsResetService.getResetStatistics(id);

            return ResponseEntity.ok(Map.of("resetNeeded", needed, "statistics", stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // ===== LECTURE =====

    /**
     *  OBTENIR UN SMS PAR ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<SendSms> getSendSms(@PathVariable Long id) {
        return sendSmsRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     *  LISTE FILTRÉE DES SMS
     */
    @GetMapping("")
    public ResponseEntity<List<SendSms>> getAllSendSms(
        Pageable pageable,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Boolean isSent,
        @RequestParam(required = false) Boolean isBulk,
        @RequestParam(required = false) String receiver,
        @RequestParam(required = false) MessageType type
    ) {
        try {
            String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new IllegalStateException("Non authentifié"));

            String effectiveLogin;

            if (SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_ADMIN")) {
                // Admin voit tout
                effectiveLogin = null;
            } else if (SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_PARTNER")) {
                effectiveLogin = login;
            } else {
                // User normal : utilise expediteur pour voir les données de son partner
                effectiveLogin = determineEffectiveUserLogin(login);
            }

            Page<SendSms> page = sendSmsRepository.findFiltered(effectiveLogin, search, isSent, isBulk, receiver, null, type, pageable);

            return ResponseEntity.ok(page.getContent());
        } catch (Exception e) {
            log.error("[LIST] Erreur", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     *  SUPPRIMER UN SMS
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSendSms(@PathVariable Long id) {
        log.info("[DELETE] SendSms ID: {}", id);

        if (!sendSmsRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        sendSmsRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ===== HELPERS =====

    private ExtendedUser resolveExtendedUser(SendSms sendSms, String login) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_PARTNER", "ROLE_ADMIN")) {
            return extendedUserRepository.findOneByUserLogin(login).orElseThrow(() -> new IllegalStateException("Partner introuvable"));
        } else if (SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_USER")) {
            User user = userRepository.findOneByLogin(login).orElseThrow(() -> new IllegalStateException("Utilisateur introuvable"));
            return extendedUserRepository
                .findOneByUserId(user.getPartnerUserId())
                .orElseThrow(() -> new IllegalStateException("Partner introuvable"));
        } else {
            throw new IllegalStateException("Rôle non autorisé");
        }
    }

    private int calculateBulkMessageCount(SendSms sendSms) {
        Long groupeId = Optional.ofNullable(sendSms.getDestinataires()).map(Groupe::getId).orElse(null);

        if (groupeId != null) {
            return contactRepository.countByGroupeId(groupeId);
        }
        return 0;
    }

    private void updateSendSmsCounters(SendSms sendSms, String oldStatus, String newStatus) {
        int totalSuccess = sendSms.getTotalSuccess() != null ? sendSms.getTotalSuccess() : 0;
        int totalFailed = sendSms.getTotalFailed() != null ? sendSms.getTotalFailed() : 0;
        int totalPending = sendSms.getTotalPending() != null ? sendSms.getTotalPending() : 0;

        String normalizedOld = normalizeStatus(oldStatus);
        String normalizedNew = normalizeStatus(newStatus);

        // Décrémenter ancien
        switch (normalizedOld) {
            case "pending":
                totalPending = Math.max(0, totalPending - 1);
                break;
            case "failed":
                totalFailed = Math.max(0, totalFailed - 1);
                break;
            case "sent":
                totalSuccess = Math.max(0, totalSuccess - 1);
                break;
        }

        // Incrémenter nouveau
        switch (normalizedNew) {
            case "pending":
                totalPending++;
                break;
            case "failed":
                totalFailed++;
                break;
            case "sent":
                totalSuccess++;
                break;
        }

        sendSms.setTotalSuccess(totalSuccess);
        sendSms.setTotalFailed(totalFailed);
        sendSms.setTotalPending(totalPending);

        int total = totalSuccess + totalFailed + totalPending;
        if (total > 0) {
            sendSms.setSuccessRate((totalSuccess * 100.0) / total);
            sendSms.setFailureRate((totalFailed * 100.0) / total);
        }

        sendSmsRepository.save(sendSms);
    }

    private String normalizeStatus(String status) {
        if (status == null) return "pending";
        String lower = status.toLowerCase().trim();
        if (lower.equals("sent") || lower.equals("delivered") || lower.equals("read")) return "sent";
        if (lower.equals("failed") || lower.equals("error")) return "failed";
        return "pending";
    }

    private String determineEffectiveUserLogin(String currentUserLogin) {
        return userRepository.findOneByLogin(currentUserLogin).map(User::getExpediteur).filter(Objects::nonNull).orElse(currentUserLogin);
    }

    private List<VariableDTO> parseVarsJson(String varsJson) {
        if (varsJson == null || varsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(varsJson, new TypeReference<List<VariableDTO>>() {});
        } catch (Exception e) {
            log.warn("[PARSE-VARS] Erreur: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private byte[] generateCsvContent(List<Sms> smsList) {
        StringBuilder csv = new StringBuilder();
        csv.append("Destinataire,Nom,Statut,Date,Segments,MessageID,Erreur\n");

        for (Sms sms : smsList) {
            csv.append(escapeCsv(sms.getReceiver())).append(",");
            csv.append(escapeCsv(sms.getNamereceiver())).append(",");
            csv.append(escapeCsv(sms.getDeliveryStatus())).append(",");
            csv.append(sms.getSendDate() != null ? sms.getSendDate().toString() : "").append(",");
            csv.append(sms.getTotalMessage() != null ? sms.getTotalMessage() : 1).append(",");
            csv.append(escapeCsv(sms.getMessageId())).append(",");
            csv.append(escapeCsv(sms.getLast_error())).append("\n");
        }

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String escapeCsv(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
