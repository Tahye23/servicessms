package com.example.myproject.service;

import com.example.myproject.SMSService;
import com.example.myproject.domain.*;
import com.example.myproject.domain.enumeration.Channel;
import com.example.myproject.domain.enumeration.ContentType;
import com.example.myproject.domain.enumeration.Direction;
import com.example.myproject.domain.enumeration.MessageType;
import com.example.myproject.repository.ChatRepository;
import com.example.myproject.repository.ContactRepository;
import com.example.myproject.repository.SmsRepository;
import com.example.myproject.repository.TemplateRepository;
import com.example.myproject.service.dto.ChatSummaryDTO;
import com.example.myproject.web.rest.dto.*;
import com.example.myproject.web.rest.errors.BadRequestAlertException;
import com.example.myproject.web.rest.errors.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private static final String ENTITY_NAME = "chat";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ChatRepository chatRepository;
    private final SmsRepository smsRepository;
    private final ContactRepository contactRepository;
    private final TemplateRepository templateRepository;
    private final TemplateService templateService;
    private final SendWhatsappService whatsAppService;
    private final SMSService smsService;
    private final TemplateMessageBuilder builder;

    public ChatService(
        ChatRepository chatRepository,
        SmsRepository smsRepository,
        ContactRepository contactRepository,
        TemplateRepository templateRepository,
        TemplateService templateService,
        SendWhatsappService whatsAppService,
        SMSService smsService,
        TemplateMessageBuilder builder
    ) {
        this.chatRepository = chatRepository;
        this.smsRepository = smsRepository;
        this.contactRepository = contactRepository;
        this.templateRepository = templateRepository;
        this.templateService = templateService;
        this.whatsAppService = whatsAppService;
        this.smsService = smsService;
        this.builder = builder;
    }

    @Transactional
    public Chat save(Chat chat) {
        return chatRepository.save(chat);
    }

    @Transactional
    public void saveAll(List<Chat> chats) {
        chatRepository.saveAll(chats);
    }

    /**
     * Crée un nouveau Chat pour le contact donné et le canal spécifié.
     * @throws ResourceNotFoundException si le contact n'existe pas.
     */
    @Transactional
    public Chat createChat(ChatRequestDTO req) {
        Contact contact = contactRepository
            .findById(req.getContactId())
            .orElseThrow(() -> new ResourceNotFoundException("contact " + req.getContactId() + " introuvable"));

        Chat chat = new Chat();
        chat.setContact(contact);
        chat.setChannel(req.getChannel());
        chat.setLastUpdated(Instant.now());

        return chatRepository.save(chat);
    }

    /**
     * Récupère ou crée une conversation (chat) pour un contact et un canal.
     */
    public Chat getOrCreateChat(Contact contact, Channel channel) {
        return chatRepository
            .findByContactIdAndChannel(contact.getId(), channel)
            .orElseGet(() -> {
                Chat chat = new Chat();
                chat.setContact(contact);
                chat.setChannel(channel);
                chat.setLastUpdated(Instant.now());
                return chatRepository.save(chat);
            });
    }

    @Transactional
    public ChatMessageResponseDTO sendMessage(boolean test, ChatMessageRequest request, String login) throws JsonProcessingException {
        Sms sms = new Sms();
        Chat chat = chatRepository
            .findById(request.getChatId())
            .orElseThrow(() -> new ResourceNotFoundException("Chat " + request.getChatId() + " introuvable"));

        Template template = templateRepository
            .findById(request.getTemplate_id())
            .orElseThrow(() -> new ResourceNotFoundException("Template " + request.getTemplate_id() + " not found"));
        TemplateRequest tplReq = null;
        if (request.getType() == MessageType.WHATSAPP) {
            try {
                tplReq = MAPPER.readValue(template.getContent(), TemplateRequest.class);
            } catch (JsonProcessingException e) {
                throw new CustomException("Contenu du template invalide", HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        }
        Contact dest = Optional.ofNullable(chat.getContact()).filter(c -> c.getContelephone() != null).orElse(null);
        String phone = dest != null ? dest.getContelephone() : sms.getReceiver();
        if (phone == null || phone.isBlank()) {
            throw new BadRequestAlertException("Le numéro est requis", ENTITY_NAME, "errorNumber");
        }
        String messageId = null;
        boolean success;
        sms.setSender(template.getExpediteur());
        sms.setReceiver(phone);
        if (request.getType() == MessageType.SMS) {
            String contenu = templateService.applyTemplate(template.getContent(), dest);
            sms.setMsgdata(contenu);
            int segments = templateService.calculateSmsSegments(contenu);
            sms.setTotalMessage(segments);
            success = test || smsService.executeAndLog(sms, phone);
        } else {
            Map<String, String> extracted = builder.extractVariables(template, dest);
            List<VariableDTO> listeVars = templateService.buildListeVars(tplReq, extracted);
            sms.setVars(MAPPER.writeValueAsString(listeVars));
            sms.setMsgdata(builder.buildMsgDataWithHtml(template.getContent(), dest));
            if (test) {
                success = true;
            } else {
                SendMessageResult sendMessageResult = whatsAppService.sendMessageAndGetId(phone, template, listeVars, login);
                messageId = sendMessageResult.getMessageId();
                sms.setLast_error(sendMessageResult.getError());
                success = messageId != null && !messageId.isEmpty();
            }
        }
        sms.setChat(chat);
        sms.setTemplate_id(template.getId());
        sms.setType(request.getType());
        sms.setDirection(Direction.OUTBOUND);
        sms.setStatus("PENDING");
        sms.setSent(null);
        sms.setBatch(null);
        sms.setContentType(ContentType.TEMPLATE);
        sms.setContentType(ContentType.PLAIN_TEXT);
        sms.setMessageId(messageId);
        sms.setSendDate(Instant.now());
        sms.setDeliveryStatus("PENDING");
        smsRepository.save(sms);
        chat.setLastUpdated(Instant.now());
        chatRepository.save(chat);
        return new ChatMessageResponseDTO(messageId, success);
    }

    public Page<Chat> getAllChats(Pageable pageable) {
        return chatRepository.findAll(pageable);
    }

    public ResponseEntity<Map<String, List<ChatSummaryDTO>>> getChatsGroupedByChannel(Long contactId) {
        List<Chat> chats = chatRepository.findByContactId(contactId);

        Map<String, List<ChatSummaryDTO>> grouped = new HashMap<>();

        for (Chat chat : chats) {
            String channel = chat.getChannel().name();
            ChatSummaryDTO dto = new ChatSummaryDTO(chat.getId(), chat.getLastUpdated());

            grouped.computeIfAbsent(channel, k -> new ArrayList<>()).add(dto);
        }

        return ResponseEntity.ok(grouped);
    }
}
