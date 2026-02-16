package com.example.myproject.service;

import com.example.myproject.SMSService;
import com.example.myproject.domain.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import tech.jhipster.config.JHipsterProperties;

/**
 * Service for sending emails asynchronously.
 * <p>
 * We use the {@link Async} annotation to send emails asynchronously.
 */
@Service
public class MailService {

    private final Logger log = LoggerFactory.getLogger(MailService.class);

    private static final String USER = "user";

    private static final String BASE_URL = "baseUrl";

    private final JHipsterProperties jHipsterProperties;

    private final SendWhatsappService sendWhatsappService;
    private final JavaMailSender javaMailSender;

    private final MessageSource messageSource;
    private final SMSService sMSSErvice;
    private final SpringTemplateEngine templateEngine;

    public MailService(
        JHipsterProperties jHipsterProperties,
        SendWhatsappService sendWhatsappService,
        JavaMailSender javaMailSender,
        MessageSource messageSource,
        SMSService sMSSErvice,
        SpringTemplateEngine templateEngine
    ) {
        this.jHipsterProperties = jHipsterProperties;
        this.sendWhatsappService = sendWhatsappService;
        this.javaMailSender = javaMailSender;
        this.messageSource = messageSource;
        this.sMSSErvice = sMSSErvice;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendEmail(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        this.sendEmailSync(to, subject, content, isMultipart, isHtml);
    }

    private void sendEmailSync(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        log.debug(
            "Send email[multipart '{}' and html '{}'] to '{}' with subject '{}' and content={}",
            isMultipart,
            isHtml,
            to,
            subject,
            content
        );

        // Prepare message using a Spring helper
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name());
            message.setTo(to);
            message.setFrom(jHipsterProperties.getMail().getFrom());
            message.setSubject(subject);
            message.setText(content, isHtml);
            javaMailSender.send(mimeMessage);
            log.debug("Sent email to User '{}'", to);
        } catch (MailException | MessagingException e) {
            log.warn("Email could not be sent to user '{}'", to, e);
        }
    }

    @Async
    public void sendEmailFromTemplate(User user, String templateName, String titleKey) {
        this.sendEmailFromTemplateSync(user, templateName, titleKey);
    }

    private void sendEmailFromTemplateSync(User user, String templateName, String titleKey) {
        if (user.getEmail() == null) {
            log.debug("Email doesn't exist for user '{}'", user.getLogin());
            return;
        }
        Locale locale = Locale.forLanguageTag(user.getLangKey());
        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable(BASE_URL, jHipsterProperties.getMail().getBaseUrl());
        String content = templateEngine.process(templateName, context);
        String subject = messageSource.getMessage(titleKey, null, locale);
        this.sendEmailSync(user.getEmail(), subject, content, false, true);
    }

    @Async
    public void sendActivationEmail(User user) {
        log.debug("Sending activation email to '{}'", user.getEmail());
        this.sendEmailFromTemplateSync(user, "mail/activationEmail", "email.activation.title");
    }

    @Async
    public void sendCreationEmail(User user) {
        log.debug("Sending creation email to '{}'", user.getEmail());
        this.sendEmailFromTemplateSync(user, "mail/creationEmail", "email.activation.title");
    }

    @Async
    public void sendCreationSms(User user, String sendService) {
        if (user.getPhone() == null || user.getPhone().isEmpty()) {
            log.warn("Aucun numéro de téléphone pour l'utilisateur '{}'", user.getLogin());
            return;
        }

        String baseUrl = jHipsterProperties.getMail().getBaseUrl();
        String message = String.format(
            "Bonjour %s, votre compte a été créé. Activez-le ici : %s/account/reset/finish?key=%s",
            user.getLogin(),
            baseUrl,
            user.getResetKey()
        );

        try {
            boolean sent = false;
            if ("sms".equals(sendService)) {
                sent = sMSSErvice.goforSend("Richatt", user.getPhone(), message);
            } else if ("wtssp".equals(sendService)) {
                sent = sendWhatsappService.sendMessageAuth(user, sendService);
            } else {
                this.sendEmailFromTemplateSync(user, "mail/creationEmail", "email.activation.title");
                sent = sMSSErvice.goforSend("Richatt", user.getPhone(), message);
            }
            if (sent) {
                log.info("SMS envoyé avec succès à {}", user.getPhone());
            } else {
                log.error("Échec de l'envoi du SMS à {}", user.getPhone());
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du SMS à {} : {}", user.getPhone(), e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetMail(User user) {
        log.debug("Sending password reset email to '{}'", user.getEmail());
        this.sendEmailFromTemplateSync(user, "mail/passwordResetEmail", "email.reset.title");
    }
}
