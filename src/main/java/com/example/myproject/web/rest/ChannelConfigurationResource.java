package com.example.myproject.web.rest;

import com.example.myproject.domain.ChannelConfiguration;
import com.example.myproject.domain.enumeration.Channel;
import com.example.myproject.service.ChannelConfigurationService;
import com.example.myproject.service.EmailSenderService;
import com.example.myproject.service.SmsSenderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/channels")
public class ChannelConfigurationResource {

    private final ChannelConfigurationService service;
    private final EmailSenderService emailSenderService;
    private final SmsSenderService smsSenderService;

    public ChannelConfigurationResource(
        ChannelConfigurationService service,
        EmailSenderService emailSenderService,
        SmsSenderService smsSenderService
    ) {
        this.service = service;
        this.emailSenderService = emailSenderService;
        this.smsSenderService = smsSenderService;
    }

    /**
     * Sauvegarde propre avec gestion d’erreur claire
     */
    @PostMapping
    public ResponseEntity<?> saveOrUpdate(@RequestBody ChannelConfiguration cfg, @RequestParam String password) {
        try {
            ChannelConfiguration saved = service.saveOrUpdate(cfg, password);
            return ResponseEntity.ok(saved);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    public ResponseEntity<?> update(
        @PathVariable Long id,
        @Valid @RequestBody ChannelConfiguration cfg,
        @RequestParam(required = false) String password
    ) {
        try {
            ChannelConfiguration updated = service.update(id, cfg, password);
            return ResponseEntity.ok(updated);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok("Configuration supprimée avec succès");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    /**
     * Test EMAIL sans sauvegarder
     */
    @PostMapping("/test-email")
    public ResponseEntity<?> testEmail(@RequestBody ChannelConfiguration cfg, @RequestParam String password) {
        try {
            String encrypted = service.encryptPassword(password);
            cfg.setEncryptedPassword(encrypted);

            String decrypted = service.decryptPassword(cfg);
            emailSenderService.sendTestEmail(cfg, decrypted);

            return ResponseEntity.ok("Email envoyé avec succès");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur SMTP : " + e.getMessage());
        }
    }

    /**
     * Test SMS sans sauvegarder
     */
    @PostMapping("/test-sms")
    public ResponseEntity<?> testSms(@RequestBody ChannelConfiguration cfg, @RequestParam String password) {
        try {
            String encrypted = service.encryptPassword(password);
            cfg.setEncryptedPassword(encrypted);

            smsSenderService.test(cfg);

            return ResponseEntity.ok("SMS envoyé avec succès");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur SMS : " + e.getMessage());
        }
    }
}
