package com.example.myproject.web.rest;

import com.example.myproject.repository.*;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/send-sms")
@Transactional
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class SendSmsAdminResource {

    private final Logger log = LoggerFactory.getLogger(SendSmsAdminResource.class);

    private final SendSmsRepository sendSmsRepository;
    private final SmsRepository smsRepository;

    public SendSmsAdminResource(SendSmsRepository sendSmsRepository, SmsRepository smsRepository) {
        this.sendSmsRepository = sendSmsRepository;
        this.smsRepository = smsRepository;
    }

    /**
     * DELETE /admin/send-sms/{id} : Supprime un SendSms et tous ses SMS liés
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteSendSmsWithMessages(@PathVariable Long id) {
        log.debug("REST request to delete SendSms and all related SMS: {}", id);

        try {
            // Vérifier si le SendSms existe
            if (!sendSmsRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "SendSms non trouvé avec l'ID: " + id));
            }

            // 1. Supprimer tous les SMS liés
            int deletedSmsCount = smsRepository.deleteBySendSmsId(id);
            log.info("Supprimé {} SMS liés au SendSms {}", deletedSmsCount, id);

            // 2. Supprimer le SendSms
            sendSmsRepository.deleteById(id);
            log.info("SendSms {} supprimé", id);

            return ResponseEntity.ok(
                Map.of(
                    "deletedSendSmsId",
                    id,
                    "deletedSmsCount",
                    deletedSmsCount,
                    "message",
                    String.format("SendSms %d supprimé avec %d SMS", id, deletedSmsCount)
                )
            );
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du SendSms {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "Erreur lors de la suppression : " + e.getMessage())
            );
        }
    }
}
