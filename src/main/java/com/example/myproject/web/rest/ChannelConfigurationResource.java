package com.example.myproject.web.rest;

import com.example.myproject.domain.ChannelConfiguration;
import com.example.myproject.service.ChannelConfigurationService;
import com.example.myproject.service.EmailSenderService;
import com.example.myproject.service.SmsSenderService;
import org.springframework.web.bind.annotation.*;

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

    // Sauvegarde de la configuration (avec chiffrement)
    @PostMapping
    public ChannelConfiguration save(@RequestBody ChannelConfiguration cfg, @RequestParam String password) {
        return service.save(cfg, password);
    }

    // Test SMTP (sans sauvegarder)
    @PostMapping("/test-email")
    public String testEmail(@RequestBody ChannelConfiguration cfg, @RequestParam String password) {
        try {
            // chiffrer puis déchiffrer (simulation réelle)
            cfg.setEncryptedPassword(service.save(cfg, password).getEncryptedPassword());
            String decrypted = service.decryptPassword(cfg);

            emailSenderService.sendTestEmail(cfg, decrypted);
            return "Email envoyé avec succès";
        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur SMTP : " + e.getMessage();
        }
    }

    // 🔥 TEST SMS
    @PostMapping("/test-sms")
    public String testSms(@RequestBody ChannelConfiguration cfg, @RequestParam String password) {
        try {
            // chiffrer le password comme en prod
            cfg.setEncryptedPassword(service.save(cfg, password).getEncryptedPassword());

            smsSenderService.test(cfg);

            return "SMS envoyé  avec succès";
        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur SMS : " + e.getMessage();
        }
    }
}
