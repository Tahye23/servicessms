package com.example.myproject.web.rest;

import com.example.myproject.domain.*;
import com.example.myproject.repository.ConfigurationRepository;
import com.example.myproject.repository.UserRepository;
import com.example.myproject.security.SecurityUtils;
import com.example.myproject.service.MessageDeliveryStatusSyncService;
import com.example.myproject.service.SendWhatsappService;
import com.example.myproject.service.UserService;
import com.example.myproject.web.rest.dto.TemplateRequest;
import com.example.myproject.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/whats-apps")
public class SendWhatsappRessource {

    @Autowired
    private SendWhatsappService sendWhatsappService;

    @Autowired
    private MessageDeliveryStatusSyncService messageDeliveryStatusSyncService;

    private final ConfigurationRepository configurationRepository;

    private final UserRepository userRepository;

    public SendWhatsappRessource(ConfigurationRepository configurationRepository, UserRepository userRepository) {
        this.configurationRepository = configurationRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<SendWhatsapp> create(@RequestBody SendWhatsapp sendWhatsapp) {
        SendWhatsapp result = sendWhatsappService.save(sendWhatsapp);
        return ResponseEntity.created(URI.create("/api/send-whatsapps/" + result.getId())).body(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SendWhatsapp> update(@PathVariable Long id, @RequestBody SendWhatsapp sendWhatsapp) {
        sendWhatsapp.setId(id);
        SendWhatsapp result = sendWhatsappService.update(sendWhatsapp);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SendWhatsapp> partialUpdate(@PathVariable Long id, @RequestBody SendWhatsapp sendWhatsapp) {
        sendWhatsapp.setId(id);
        Optional<SendWhatsapp> result = sendWhatsappService.partialUpdate(sendWhatsapp);
        return result.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<SendWhatsapp>> getAll(@ParameterObject Pageable pageable) {
        Page<SendWhatsapp> pageResult = sendWhatsappService.findAll(pageable);
        return ResponseEntity.ok(pageResult);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SendWhatsapp> getOne(@PathVariable Long id) {
        Optional<SendWhatsapp> sendWhatsapp = sendWhatsappService.findOne(id);
        return sendWhatsapp.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sendWhatsappService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/create-template")
    public ResponseEntity<?> createTemplate(@Valid @RequestBody TemplateRequest templateRequest) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new IllegalStateException("L'utilisateur n'est pas authentifié."));
        User user = userRepository.findOneByLogin(login).orElseThrow(() -> new IllegalStateException("Utilisateur introuvable."));
        Configuration cfg = configurationRepository
            .findOneByUserLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("Partner has no WhatsApp configuration", "whatsapp", "noconfig"));
        if (!cfg.isVerified()) {
            Map<String, Object> errorBody = Map.of("error", Map.of("error_user_msg", "Votre configuration WhatsApp n’est pas vérifiée."));
            return ResponseEntity.badRequest().body(errorBody);
        }
        return sendWhatsappService.sendTemplateToWhatsApp(templateRequest, user, cfg);
    }

    @PostMapping("/{id}/sync-delivery-status")
    public ResponseEntity<Void> syncDeliveryStatus(@PathVariable Long id) {
        messageDeliveryStatusSyncService.syncDeliveryStatusBySendSmsId(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/update-status")
    public ResponseEntity<Void> updateSendSmsStatus(@PathVariable("id") Long sendSmsId) {
        messageDeliveryStatusSyncService.updateSendSmsStatus(sendSmsId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteSendSmsAndMessages(@PathVariable Long id) {
        messageDeliveryStatusSyncService.deleteSendSmsWithMessages(id);
        return ResponseEntity.noContent().build();
    }
}
