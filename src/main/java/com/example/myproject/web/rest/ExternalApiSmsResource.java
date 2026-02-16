package com.example.myproject.web.rest;

import com.example.myproject.domain.Sms;
import com.example.myproject.domain.enumeration.MessageType;
import com.example.myproject.repository.SmsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/external-api-messages")
public class ExternalApiSmsResource {

    private final Logger log = LoggerFactory.getLogger(ExternalApiSmsResource.class);
    private final SmsRepository smsRepository;

    public ExternalApiSmsResource(SmsRepository smsRepository) {
        this.smsRepository = smsRepository;
    }

    @GetMapping("")
    public ResponseEntity<Page<Sms>> getExternalApiMessages(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) MessageType type,
        @RequestParam(required = false) String search,
        Pageable pageable
    ) {
        log.debug("REST request to get external API messages - status: {}, type: {}, search: {}", status, type, search);

        try {
            // Convertir le MessageType en String, gérer le null
            String typeString = type != null ? type.name() : null;

            Page<Sms> result = smsRepository.findExternalApiMessages(status, typeString, search, pageable);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error fetching external API messages", e);
            throw e;
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ExternalApiStats> getExternalApiStats() {
        log.debug("REST request to get external API stats");

        try {
            long totalSms = smsRepository.countExternalApiByType(MessageType.SMS);
            long totalWhatsapp = smsRepository.countExternalApiByType(MessageType.WHATSAPP);

            long successSms = smsRepository.countExternalApiByTypeAndStatus(MessageType.SMS, "SENT");
            long successWhatsapp = smsRepository.countExternalApiByTypeAndStatus(MessageType.WHATSAPP, "SENT");

            long failedSms = smsRepository.countExternalApiByTypeAndStatus(MessageType.SMS, "FAILED");
            long failedWhatsapp = smsRepository.countExternalApiByTypeAndStatus(MessageType.WHATSAPP, "FAILED");

            long totalSegments = smsRepository.sumExternalApiSegments();

            ExternalApiStats stats = new ExternalApiStats(
                totalSms,
                totalWhatsapp,
                successSms,
                successWhatsapp,
                failedSms,
                failedWhatsapp,
                totalSegments
            );

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching external API stats", e);
            throw e;
        }
    }

    public static class ExternalApiStats {

        public long totalSms;
        public long totalWhatsapp;
        public long successSms;
        public long successWhatsapp;
        public long failedSms;
        public long failedWhatsapp;
        public long totalSegments;

        public ExternalApiStats(
            long totalSms,
            long totalWhatsapp,
            long successSms,
            long successWhatsapp,
            long failedSms,
            long failedWhatsapp,
            long totalSegments
        ) {
            this.totalSms = totalSms;
            this.totalWhatsapp = totalWhatsapp;
            this.successSms = successSms;
            this.successWhatsapp = successWhatsapp;
            this.failedSms = failedSms;
            this.failedWhatsapp = failedWhatsapp;
            this.totalSegments = totalSegments;
        }
    }
}
