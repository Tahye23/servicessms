package com.example.myproject;

import com.example.myproject.domain.ChannelConfiguration;
import com.example.myproject.domain.Chat;
import com.example.myproject.domain.SendSms;
import com.example.myproject.domain.Sms;
import com.example.myproject.domain.enumeration.Channel;
import com.example.myproject.repository.ChannelConfigurationRepository;
import com.example.myproject.repository.ChatRepository;
import com.example.myproject.repository.SendSmsRepository;
import com.example.myproject.repository.SmsRepository;
import com.example.myproject.security.SecurityUtils;
import com.example.myproject.service.ChannelConfigurationService;
import com.example.myproject.web.rest.dto.SendResult;
import com.example.myproject.web.rest.dto.SmsSendResult;
import com.example.myproject.web.rest.dto.SmsStatusResult;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.ExchangeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class SMSService {

    @Autowired
    private CamelContext context;

    @Autowired
    private ProducerTemplate template;

    @Autowired
    private ChannelConfigurationRepository channelConfigurationRepository;

    @Autowired
    private ChannelConfigurationService channelConfigurationService;

    private final SmsRepository smsRepository;
    private final SendSmsRepository sendSmsRepository;
    private final Logger log = LoggerFactory.getLogger(SMSService.class);

    @Autowired
    private ChatRepository chatRepository;

    @Value("${sms.fast-mode:true}")
    private boolean fastMode;

    @Value("${dlr.base-url:https://votre-serveur.com}")
    private String dlrBaseUrl;

    public SMSService(SmsRepository smsRepository, SendSmsRepository sendSmsRepository) {
        this.smsRepository = smsRepository;
        this.sendSmsRepository = sendSmsRepository;
    }

    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return phoneNumber;
        }

        String cleaned = phoneNumber.replaceAll("[\\s\\-\\(\\)\\+]", "");

        if (cleaned.matches("^[A-Za-z0-9]{2,11}$") && cleaned.matches(".*[A-Za-z].*")) {
            return cleaned;
        }

        if (cleaned.matches("^\\d+$")) {
            if (cleaned.startsWith("0") && cleaned.length() == 10) {
                cleaned = "33" + cleaned.substring(1);
            } else if (cleaned.startsWith("0") && cleaned.length() == 9) {
                cleaned = "221" + cleaned.substring(1);
            } else if (
                !cleaned.startsWith("33") &&
                !cleaned.startsWith("221") &&
                !cleaned.startsWith("1") &&
                !cleaned.startsWith("44") &&
                !cleaned.startsWith("49") &&
                !cleaned.startsWith("39")
            ) {
                if (cleaned.length() == 8) {
                    cleaned = "221" + cleaned;
                } else if (cleaned.length() == 9) {
                    cleaned = "33" + cleaned;
                }
            }
        }

        return cleaned;
    }

    private boolean containsUnicodeCharacters(String message) {
        if (message == null) return false;

        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c > 127) {
                return true;
            }
        }
        return false;
    }

    private int getDataCoding(String message) {
        if (containsUnicodeCharacters(message)) {
            return 8;
        }
        return 0;
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }

        return phoneNumber.matches("^[A-Za-z0-9]{2,15}$");
    }

    private String buildDlrCallbackUrl() {
        return (dlrBaseUrl + "/api/sms-dlr/callback" + "?msgid=%I" + "&status=%d" + "&phone=%p" + "&ts=%t" + "&smsc=%A" + "&err=%e");
    }

    public SendResult goforSendFastResult(String sourceAddress, String destinationAddress, String message) {
        try {
            String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new RuntimeException("User not authenticated"));

            ChannelConfiguration cfg = channelConfigurationRepository
                .findByUserLoginAndChannelType(login, Channel.SMS)
                .orElseThrow(() -> new RuntimeException("No SMS configuration found for user"));

            if (!Boolean.TRUE.equals(cfg.getVerified())) {
                return SendResult.fail("SMS configuration not verified", null);
            }

            String decryptedPassword = channelConfigurationService.decryptPassword(cfg);

            String smppUri = String.format(
                "smpp://%s:%d?systemId=%s&password=%s&enquireLinkTimer=5000&transactionTimer=10000",
                cfg.getHost(),
                cfg.getPort(),
                cfg.getUsername(),
                decryptedPassword
            );

            log.debug("Using SMPP host {}:{} for user {}", cfg.getHost(), cfg.getPort(), login);

            String dlrUrl = buildDlrCallbackUrl();

            String formattedSource = formatPhoneNumber(sourceAddress);
            String formattedDestination = formatPhoneNumber(destinationAddress);

            if (!isValidPhoneNumber(formattedDestination)) {
                return SendResult.fail("Invalid destination: " + destinationAddress, null);
            }

            // 🧠 6️⃣ Détection Unicode
            int dataCoding = getDataCoding(message);
            int destTon = 1, destNpi = 1, srcTon = 1, srcNpi = 1;

            if (formattedSource.matches("^[A-Za-z0-9]{2,11}$") && formattedSource.matches(".*[A-Za-z].*")) {
                srcTon = 5; // Alphanumeric sender
                srcNpi = 0;
            }

            // 📡 7️⃣ Construire exchange SMPP
            Exchange exchange = ExchangeBuilder.anExchange(context)
                .withHeader("CamelSmppDestAddr", List.of(formattedDestination))
                .withHeader("CamelSmppSourceAddr", formattedSource)
                .withHeader("CamelSmppDestAddrTon", destTon)
                .withHeader("CamelSmppDestAddrNpi", destNpi)
                .withHeader("CamelSmppSourceAddrTon", srcTon)
                .withHeader("CamelSmppSourceAddrNpi", srcNpi)
                .withHeader("CamelSmppDataCoding", dataCoding)
                .withHeader("CamelSmppAlphabet", dataCoding == 8 ? 8 : 0)
                .withHeader("CamelSmppRegisteredDelivery", 1)
                .withHeader("CamelSmppProtocolId", 0)
                .withHeader("CamelSmppEsmClass", 0)
                .withHeader("CamelSmppPriorityFlag", 0)
                .withHeader("CamelSmppDlrUrl", dlrUrl)
                .withPattern(ExchangePattern.InOnly)
                .withBody(message)
                .build();

            // 🚀 8️⃣ Envoi vers opérateur
            exchange = template.send(smppUri, exchange);

            if (exchange.getException() != null) {
                String err = exchange.getException().getMessage();
                log.warn("SMPP send failed {} -> {}: {}", sourceAddress, destinationAddress, err);
                return SendResult.fail(err, null);
            }

            // 📨 9️⃣ Extraire messageId
            String messageId = extractMessageId(exchange);

            if (messageId == null || messageId.isEmpty()) {
                log.warn("No messageId returned by SMPP operator");
                return SendResult.fail("No messageId returned", null);
            }

            log.debug("SMS sent successfully. MessageId={}", messageId);

            return SendResult.ok(messageId);
        } catch (IllegalArgumentException e) {
            return SendResult.fail(e.getMessage(), null);
        } catch (Exception e) {
            log.error("SMS send error: {}", e.getMessage(), e);
            return SendResult.fail(e.getMessage(), null);
        }
    }

    private String extractMessageId(Exchange exchange) {
        Object messageIdObj = exchange.getMessage().getHeader("CamelSmppId");

        if (messageIdObj instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) messageIdObj;
            return list.isEmpty() ? null : list.get(0).toString();
        }

        return messageIdObj == null ? null : messageIdObj.toString();
    }

    public boolean goforSend(String sourceAddress, String destinationAddress, String message) {
        SendResult result = goforSendFastResult(sourceAddress, destinationAddress, message);
        return result.isSuccess();
    }

    public SmsSendResult send(String sourceAddress, String destinationAddress, String message) {
        SendResult result = goforSendFastResult(sourceAddress, destinationAddress, message);

        SmsSendResult smsSendResult = new SmsSendResult();
        smsSendResult.setSuccess(result.isSuccess());
        smsSendResult.setMessageId(result.getMessageId());
        smsSendResult.setError(result.getError());

        return smsSendResult;
    }

    @PostConstruct
    public void init() {
        log.info("SMS SERVICE initialized - Fast mode: {}", fastMode);
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

    public Page<Sms> getMessagesByChatId(Long chatId, Pageable pageable) {
        return smsRepository.findByChatIdOrderBySendDateAsc(chatId, pageable);
    }

    public Page<Sms> getAllSmsForUser(Pageable pageable, String login) {
        return smsRepository.findAllByUser(login, pageable);
    }

    public Sms getSmsSecure(Long id, String login) {
        return smsRepository
            .findByIdAndUser(id, login)
            .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Access denied"));
    }

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

            Byte messageState = result.getMessage().getHeader("CamelSmppMessageState", Byte.class);
            String errorCode = result.getMessage().getHeader("CamelSmppError", String.class);

            return new SmsStatusResult(mapMessageState(messageState), messageState, errorCode);
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

    public boolean executeAndLog(Sms sendSms, String numero) {
        try {
            boolean ok = goforSend(sendSms.getSender(), numero, sendSms.getMsgdata());
            if (ok) {
                log.debug("SMS #{} envoyé à {}", sendSms.getId(), numero); // Réduire niveau de log
            } else {
                log.error("Échec envoi de SMS #{} vers {}", sendSms.getId(), numero);
            }
            return ok;
        } catch (Exception ex) {
            log.error("Exception lors de l'envoi de SMS #{} à {}: {}", sendSms.getId(), numero, ex.getMessage());
            return false;
        }
    }
}
