package com.example.myproject;

import com.example.myproject.domain.*;
import com.example.myproject.domain.enumeration.Channel;
import com.example.myproject.repository.*;
import com.example.myproject.security.SecurityUtils;
import com.example.myproject.service.ChannelConfigurationService;
import com.example.myproject.web.rest.dto.SendResult;
import com.example.myproject.web.rest.dto.SmsSendResult;
import com.example.myproject.web.rest.dto.SmsStatusResult;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.camel.*;
import org.apache.camel.builder.ExchangeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class SMSService {

    private final Logger log = LoggerFactory.getLogger(SMSService.class);

    @Autowired
    private CamelContext context;

    @Autowired
    private ProducerTemplate template;

    @Autowired
    private ChannelConfigurationRepository channelConfigurationRepository;

    @Autowired
    private ChannelConfigurationService channelConfigurationService;

    @Autowired
    private ChatRepository chatRepository;

    private final SmsRepository smsRepository;
    private final SendSmsRepository sendSmsRepository;

    @Value("${sms.fast-mode:true}")
    private boolean fastMode;

    @Value("${dlr.base-url:https://votre-serveur.com}")
    private String dlrBaseUrl;

    public SMSService(SmsRepository smsRepository, SendSmsRepository sendSmsRepository) {
        this.smsRepository = smsRepository;
        this.sendSmsRepository = sendSmsRepository;
    }

    //  UTILS

    private String getCurrentUser() {
        return SecurityUtils.getCurrentUserLogin().orElse("admin");
    }

    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return null;
        return phoneNumber.replaceAll("[\\s\\-\\(\\)\\+]", "");
    }

    private boolean containsUnicodeCharacters(String message) {
        if (message == null) return false;
        return message.chars().anyMatch(c -> c > 127);
    }

    private int getDataCoding(String message) {
        return containsUnicodeCharacters(message) ? 8 : 0;
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.matches("^[0-9]{8,15}$");
    }

    private String buildDlrCallbackUrl() {
        return dlrBaseUrl + "/api/sms-dlr/callback" + "?msgid=%I&status=%d&phone=%p&ts=%t&smsc=%A&err=%e";
    }

    private String extractMessageId(Exchange exchange) {
        Object messageIdObj = exchange.getMessage().getHeader("CamelSmppId");

        if (messageIdObj instanceof List<?> list) {
            return list.isEmpty() ? null : list.get(0).toString();
        }
        return messageIdObj == null ? null : messageIdObj.toString();
    }

    //  ROUTING OPERATEUR

    private ChannelConfiguration getSmsConfigForNumber(String destinationNumber, String login) {
        String digitsOnly = destinationNumber.replaceAll("\\D", ""); // supprime tout sauf chiffres
        String cleaned = digitsOnly.startsWith("222") ? digitsOnly.substring(3) : digitsOnly;

        // Liste des opérateurs par priorité
        List<String> preferredOperators = new ArrayList<>();

        if (cleaned.startsWith("2")) {
            preferredOperators.add("Chinguitel");
            preferredOperators.add("Mattel"); // fallback
        } else if (cleaned.startsWith("3") || cleaned.startsWith("4")) {
            preferredOperators.add("Mattel");
            preferredOperators.add("Chinguitel"); // fallback
        } else {
            throw new RuntimeException("Numéro non supporté: " + destinationNumber);
        }

        for (String operator : preferredOperators) {
            log.debug("Tentative de routage {} -> opérateur {}", destinationNumber, operator);

            Optional<ChannelConfiguration> cfg = channelConfigurationRepository.findByUserLoginAndChannelTypeAndSmsOperator(
                login,
                Channel.SMS,
                operator
            );

            if (cfg.isEmpty()) {
                cfg = channelConfigurationRepository.findByUserLoginAndChannelTypeAndSmsOperator("admin", Channel.SMS, operator);
            }

            if (cfg.isPresent()) {
                return cfg.get(); // retourne dès qu'une configuration est trouvée
            }
        }

        throw new RuntimeException(
            "Aucune config SMS disponible pour " + destinationNumber + " avec les opérateurs: " + preferredOperators
        );
    }

    // ENVOI SMS

    public SendResult sendSmsWithRouting(String sourceAddress, String destinationAddress, String message, String login) {
        try {
            ChannelConfiguration cfg = getSmsConfigForNumber(destinationAddress, login);

            if (!Boolean.TRUE.equals(cfg.getVerified())) {
                return SendResult.fail("Configuration non vérifiée", null);
            }

            String password = channelConfigurationService.decryptPassword(cfg);

            String smppUri = String.format(
                "smpp://%s:%d?systemId=%s&password=%s&enquireLinkTimer=5000&transactionTimer=10000",
                cfg.getHost(),
                cfg.getPort(),
                cfg.getUsername(),
                password
            );

            String source = formatPhoneNumber(sourceAddress);
            String dest = formatPhoneNumber(destinationAddress);

            if (!isValidPhoneNumber(dest)) {
                return SendResult.fail("Numéro invalide: " + dest, null);
            }

            int dataCoding = getDataCoding(message);

            Exchange exchange = ExchangeBuilder.anExchange(context)
                .withHeader("CamelSmppDestAddr", List.of(dest))
                .withHeader("CamelSmppSourceAddr", source)
                .withHeader("CamelSmppDestAddrTon", 1)
                .withHeader("CamelSmppDestAddrNpi", 1)
                .withHeader("CamelSmppSourceAddrTon", 1)
                .withHeader("CamelSmppSourceAddrNpi", 1)
                .withHeader("CamelSmppDataCoding", dataCoding)
                .withHeader("CamelSmppAlphabet", dataCoding == 8 ? 8 : 0)
                .withHeader("CamelSmppRegisteredDelivery", 1)
                .withHeader("CamelSmppDlrUrl", buildDlrCallbackUrl())
                .withPattern(ExchangePattern.InOnly)
                .withBody(message)
                .build();

            exchange = template.send(smppUri, exchange);

            if (exchange.getException() != null) {
                return SendResult.fail(exchange.getException().getMessage(), null);
            }

            String messageId = extractMessageId(exchange);

            return SendResult.ok(messageId);
        } catch (Exception e) {
            log.error("Erreur envoi SMS {} -> {} : {}", sourceAddress, destinationAddress, e.getMessage(), e);
            return SendResult.fail(e.getMessage(), null);
        }
    }

    // COMPATIBILITE

    public SendResult sendSmsWithRouting(String sourceAddress, String destinationAddress, String message) {
        return sendSmsWithRouting(sourceAddress, destinationAddress, message, getCurrentUser());
    }

    public SendResult goforSendFastResult(String sourceAddress, String destinationAddress, String message, String login) {
        return sendSmsWithRouting(sourceAddress, destinationAddress, message, login);
    }

    public SendResult goforSendFastResult(String sourceAddress, String destinationAddress, String message) {
        return sendSmsWithRouting(sourceAddress, destinationAddress, message);
    }

    public boolean goforSend(String sourceAddress, String destinationAddress, String message) {
        return goforSendFastResult(sourceAddress, destinationAddress, message).isSuccess();
    }

    public SmsSendResult send(String sourceAddress, String destinationAddress, String message) {
        SendResult result = sendSmsWithRouting(sourceAddress, destinationAddress, message);

        SmsSendResult sms = new SmsSendResult();
        sms.setSuccess(result.isSuccess());
        sms.setMessageId(result.getMessageId());
        sms.setError(result.getError());

        return sms;
    }

    // STATUS

    public SmsStatusResult queryMessageStatus(String messageId) {
        try {
            Exchange exchange = ExchangeBuilder.anExchange(context)
                .withHeader("CamelSmppCommand", "query_sm")
                .withHeader("CamelSmppMessageId", messageId)
                .withPattern(ExchangePattern.InOut)
                .build();

            Exchange result = template.send("smpp://smppSession", exchange);

            if (result.getException() != null) {
                return SmsStatusResult.unknown(result.getException().getMessage());
            }

            Byte state = result.getMessage().getHeader("CamelSmppMessageState", Byte.class);
            String error = result.getMessage().getHeader("CamelSmppError", String.class);

            return new SmsStatusResult(mapMessageState(state), state, error);
        } catch (Exception e) {
            return SmsStatusResult.unknown(e.getMessage());
        }
    }

    private String mapMessageState(Byte state) {
        if (state == null) return "UNKNOWN";

        return switch (state.intValue()) {
            case 0 -> "SCHEDULED";
            case 1 -> "ENROUTE";
            case 2 -> "DELIVERED";
            case 3 -> "EXPIRED";
            case 4 -> "DELETED";
            case 5 -> "UNDELIVERABLE";
            case 6 -> "ACCEPTED";
            case 7 -> "UNKNOWN";
            case 8 -> "REJECTED";
            default -> "UNKNOWN";
        };
    }

    public Page<Sms> getMessagesByChatId(Long chatId, Pageable pageable) {
        return smsRepository.findByChatIdOrderBySendDateAsc(chatId, pageable);
    }

    public Sms createSms(Sms sms) {
        sms.setSendDate(java.time.Instant.now());
        sms.setSent(false);

        if (sms.getChat() != null && sms.getChat().getId() != null) {
            Chat chat = chatRepository.findById(sms.getChat().getId()).orElseThrow(() -> new RuntimeException("Chat not found"));
            sms.setChat(chat);
        }

        if (sms.getBatch() != null && sms.getBatch().getId() != null) {
            SendSms batch = sendSmsRepository.findById(sms.getBatch().getId()).orElseThrow(() -> new RuntimeException("Batch not found"));
            sms.setBatch(batch);
        }

        return smsRepository.save(sms);
    }

    public Page<Sms> getAllSmsForUser(Pageable pageable, String login) {
        return smsRepository.findAllByUser(login, pageable);
    }

    public Sms getSmsSecure(Long id, String login) {
        return smsRepository
            .findByIdAndUser(id, login)
            .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Access denied"));
    }

    public boolean executeAndLog(Sms sendSms, String numero, String login) {
        try {
            // Déterminer la config et l'opérateur utilisé
            ChannelConfiguration cfg = getSmsConfigForNumber(numero, login);
            String operator = cfg.getSmsOperator(); // Assurez-vous que ChannelConfiguration a ce champ

            log.info("Envoi du SMS #{} à {} via l'opérateur {}", sendSms.getId(), numero, operator);

            // Envoyer le SMS
            SendResult result = sendSmsWithRouting(sendSms.getSender(), numero, sendSms.getMsgdata());
            boolean ok = result.isSuccess();

            if (ok) {
                log.debug("SMS #{} envoyé à {} avec succès via {}", sendSms.getId(), numero, operator);
            } else {
                log.error("Échec de l'envoi du SMS #{} à {} via {}: {}", sendSms.getId(), numero, operator, result.getError());
            }

            return ok;
        } catch (Exception ex) {
            log.error("Exception lors de l'envoi de SMS #{} à {}: {}", sendSms.getId(), numero, ex.getMessage(), ex);
            return false;
        }
    }

    @PostConstruct
    public void init() {
        log.info("SMS Service démarré - Fast mode: {}", fastMode);
    }
}
