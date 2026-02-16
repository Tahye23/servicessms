package com.example.myproject.web.rest;

import com.example.myproject.service.SmsDlrService;
import com.example.myproject.web.rest.dto.DlrCallbackRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sms-dlr")
public class SmsDlrResource {

    private static final Logger log = LoggerFactory.getLogger(SmsDlrResource.class);

    @Autowired
    private SmsDlrService smsDlrService;

    /**
     * ✅ ENDPOINT CALLBACK DLR KANNEL
     * Kannel appellera cette URL après chaque envoi
     */
    @GetMapping("/callback")
    public ResponseEntity<String> handleDlrCallback(
        @RequestParam(value = "msgid", required = false) String messageId,
        @RequestParam(value = "status", required = false) String statusCode,
        @RequestParam(value = "phone", required = false) String phoneNumber,
        @RequestParam(value = "ts", required = false) String timestamp,
        @RequestParam(value = "smsc", required = false) String smscId,
        @RequestParam(value = "err", required = false) String errorCode
    ) {
        log.debug("[DLR-CALLBACK] Reçu: msgid={}, status={}, phone={}", messageId, statusCode, phoneNumber);

        try {
            // ✅ VALIDATION RAPIDE
            if (messageId == null || messageId.isEmpty()) {
                log.warn("[DLR-CALLBACK] Message ID manquant");
                return ResponseEntity.badRequest().body("Missing messageId");
            }

            if (statusCode == null || statusCode.isEmpty()) {
                log.warn("[DLR-CALLBACK] Status manquant");
                return ResponseEntity.badRequest().body("Missing status");
            }

            // ✅ CONSTRUIRE LE DTO
            DlrCallbackRequest request = DlrCallbackRequest.builder()
                .messageId(messageId)
                .statusCode(parseStatusCode(statusCode))
                .phoneNumber(phoneNumber)
                .timestamp(parseTimestamp(timestamp))
                .smscId(smscId)
                .errorCode(errorCode)
                .build();

            // ✅ TRAITER DE MANIÈRE ASYNCHRONE
            // smsDlrService.processDlrFromSmpp(request);

            // ✅ RÉPONDRE IMMÉDIATEMENT À KANNEL
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("[DLR-CALLBACK] Erreur: {}", e.getMessage(), e);
            // ⚠️  Toujours répondre 200 OK même en cas d'erreur
            // Sinon Kannel va retry infiniment
            return ResponseEntity.ok("ERROR");
        }
    }

    /**
     * ✅ ENDPOINT ADMIN: Statistiques DLR
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDlrStats() {
        try {
            Map<String, Object> stats = smsDlrService.getDlrStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Erreur récupération stats DLR: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * ✅ HELPER: Parser le code statut
     */
    private Integer parseStatusCode(String statusCode) {
        try {
            return Integer.parseInt(statusCode);
        } catch (NumberFormatException e) {
            log.warn("Code statut invalide: {}", statusCode);
            return null;
        }
    }

    /**
     * ✅ HELPER: Parser le timestamp
     */
    private Long parseTimestamp(String timestamp) {
        try {
            return timestamp != null ? Long.parseLong(timestamp) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
