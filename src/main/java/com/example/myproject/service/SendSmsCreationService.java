package com.example.myproject.service;

import com.example.myproject.domain.*;
import com.example.myproject.domain.enumeration.MessageType;
import com.example.myproject.repository.*;
import com.example.myproject.service.event.SmsBulkInsertEvent;
import com.example.myproject.web.rest.dto.TemplateRequest;
import com.example.myproject.web.rest.dto.VariableDTO;
import com.example.myproject.web.rest.errors.CustomException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SendSmsCreationService {

    private static final Logger log = LoggerFactory.getLogger(SendSmsCreationService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private SendSmsRepository sendSmsRepository;

    @Autowired
    private SmsRepository smsRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private TemplateMessageBuilder builder;

    @Autowired
    private GroupeRepository groupeRepository;

    @Autowired
    private GroupedecontactRepository groupedecontactRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher; // ✅ AJOUTÉ

    /**
     * ✅ CRÉATION SMS UNITAIRE
     */
    @Transactional
    public SendSms createSingleSms(SendSms sendSms, Template template, Contact contact, String login) throws JsonProcessingException {
        Instant now = Instant.now();

        String numero = contact != null ? contact.getContelephone() : sendSms.getReceiver();
        if (numero == null || numero.isBlank()) {
            throw new CustomException("Le numéro est requis", HttpStatus.BAD_REQUEST.value());
        }

        sendSms.setNamereceiver(contact != null ? contact.getConnom() : null);
        sendSms.setReceiver(numero);
        sendSms.setConnom(contact != null ? contact.getConnom() : null);
        sendSms.setDeliveryStatus("pending");
        sendSms.setSent(null);
        sendSms.setBulkCreatedAt(now);
        sendSms.setTotalRecipients(1);

        if (sendSms.getType() == MessageType.SMS) {
            String contenu = templateService.applyTemplate(template.getContent(), contact);
            sendSms.setMsgdata(contenu);
            int segments = templateService.calculateSmsSegments(contenu);
            sendSms.setTotalMessage(segments);
        } else {
            Map<String, String> extracted = builder.extractVariables(template, contact);
            TemplateRequest tplReq = parseTemplate(template);
            List<VariableDTO> listeVars = templateService.buildListeVars(tplReq, extracted);
            sendSms.setVars(MAPPER.writeValueAsString(listeVars));
            sendSms.setMsgdata(builder.buildMsgDataWithHtml(template.getContent(), contact));
            sendSms.setTotalMessage(1);
        }

        SendSms saved = sendSmsRepository.save(sendSms);
        createSmsEntity(saved, template, contact, now, login);

        return saved;
    }

    /**
     * ✅ CRÉATION SMS BULK (MODIFIÉ)
     */
    @Transactional
    public SendSms createBulkSms(SendSms sendSms, Template template, Long groupeId, String login) throws JsonProcessingException {
        Instant now = Instant.now();

        List<Contact> contacts = groupedecontactRepository.findAllContactsByGroupeId(groupeId);
        if (contacts.isEmpty()) {
            throw new CustomException("Aucun contact dans le groupe", HttpStatus.BAD_REQUEST.value());
        }

        Groupe groupe = groupeRepository
            .findById(groupeId)
            .orElseThrow(() -> new CustomException("Groupe introuvable", HttpStatus.BAD_REQUEST.value()));

        // Métadonnées
        sendSms.setGrotitre(groupe.getGrotitre());
        sendSms.setBulkCreatedAt(now);
        sendSms.setIsbulk(true);
        sendSms.setInprocess(false);
        sendSms.setIsSent(null);
        sendSms.setTotalRecipients(contacts.size());
        sendSms.setDeliveryStatus("pending");
        sendSms.setNamereceiver(groupe.getGrotitre());

        // BulkId unique
        String bulkId = generateUniqueBulkId();
        sendSms.setBulkId(bulkId);

        SendSms saved = sendSmsRepository.save(sendSms);

        // Préparer les SMS
        TemplateRequest tplReq = sendSms.getType() == MessageType.WHATSAPP ? parseTemplate(template) : null;

        List<Sms> smsList = new ArrayList<>();
        int totalMessages = 0;

        for (Contact contact : contacts) {
            Sms sms = buildSmsForContact(contact, saved, template, sendSms, bulkId, tplReq, now, login);
            totalMessages += sms.getTotalMessage() != null ? sms.getTotalMessage() : 1;
            smsList.add(sms);
        }

        // Mise à jour totaux
        saved.setTotalMessage(totalMessages);
        saved.setTotalPending(totalMessages);
        sendSmsRepository.save(saved);

        // ✅ PUBLIER L'ÉVÉNEMENT (sera traité APRÈS le commit de la transaction)
        eventPublisher.publishEvent(new SmsBulkInsertEvent(smsList, saved.getId()));

        log.info("[CREATE-BULK] Événement publié pour SendSms#{} avec {} SMS", saved.getId(), smsList.size());

        return saved;
    }

    /**
     * ✅ REFRESH : Ajouter nouveaux contacts
     */
    @Transactional
    public SendSms refreshBulkSms(Long sendSmsId, Long groupeId) throws JsonProcessingException {
        SendSms sendSms = sendSmsRepository
            .findById(sendSmsId)
            .orElseThrow(() -> new CustomException("SendSms introuvable", HttpStatus.NOT_FOUND.value()));

        List<Contact> contacts = groupedecontactRepository.findAllContactsByGroupeId(groupeId);

        List<Sms> existingSmsList = smsRepository.findByBatchId(sendSmsId);
        Set<String> existingNumbers = existingSmsList.stream().map(Sms::getReceiver).collect(Collectors.toSet());

        List<Contact> newContacts = contacts
            .stream()
            .filter(c -> !existingNumbers.contains(c.getContelephone()))
            .collect(Collectors.toList());

        if (newContacts.isEmpty()) {
            return sendSms;
        }

        Template template = templateRepository
            .findById(sendSms.getTemplate_id())
            .orElseThrow(() -> new CustomException("Template introuvable", HttpStatus.NOT_FOUND.value()));

        Instant now = Instant.now();
        List<Sms> smsToInsert = new ArrayList<>();
        int totalMessagesAdded = 0;

        for (Contact contact : newContacts) {
            Sms sms = new Sms();
            sms.setBatch(sendSms);
            sms.setTemplate_id(sendSms.getTemplate_id());
            sms.setType(sendSms.getType());
            sms.setSender(sendSms.getSender());
            sms.setNamereceiver(contact.getConnom());
            sms.setReceiver(contact.getContelephone());
            sms.setStatus("PENDING");
            sms.setDeliveryStatus("pending");
            sms.setSent(null);
            sms.setBulkCreatedAt(now);

            if (sendSms.getType() == MessageType.SMS) {
                String contenu = templateService.applyTemplate(sendSms.getMsgdata(), contact);
                sms.setMsgdata(contenu);
                int segments = templateService.calculateSmsSegments(contenu);
                sms.setTotalMessage(segments);
                totalMessagesAdded += segments;
            } else {
                sms.setMsgdata(sendSms.getMsgdata());
                sms.setTotalMessage(1);
                totalMessagesAdded += 1;
            }

            smsToInsert.add(sms);
        }

        smsRepository.saveAll(smsToInsert);

        sendSms.setTotalMessage((sendSms.getTotalMessage() != null ? sendSms.getTotalMessage() : 0) + totalMessagesAdded);
        sendSms.setTotalRecipients(sendSms.getTotalRecipients() + newContacts.size());

        return sendSmsRepository.save(sendSms);
    }

    // ===== HELPERS (inchangés) =====

    private void createSmsEntity(SendSms sendSms, Template template, Contact contact, Instant now, String login) {
        Sms sms = new Sms();
        sms.setBatch(sendSms);
        sms.setTemplate_id(template.getId());
        sms.setType(sendSms.getType());
        sms.setSender(sendSms.getSender());
        sms.setNamereceiver(contact != null ? contact.getConnom() : null);
        sms.setReceiver(contact != null ? contact.getContelephone() : sendSms.getReceiver());
        sms.setStatus("PENDING");
        sms.setDeliveryStatus("pending");
        sms.setUser_login(login);
        sms.setSent(null);
        sms.setBulkCreatedAt(now);
        sms.setMsgdata(sendSms.getMsgdata());
        sms.setVars(sendSms.getVars());
        smsRepository.save(sms);
    }

    private Sms buildSmsForContact(
        Contact contact,
        SendSms savedBatch,
        Template template,
        SendSms sendSms,
        String bulkId,
        TemplateRequest tplReq,
        Instant now,
        String login
    ) throws JsonProcessingException {
        Sms sms = new Sms();
        sms.setBatch(savedBatch);
        sms.setTemplate_id(template.getId());
        sms.setSender(sendSms.getSender());
        sms.setReceiver(contact.getContelephone());
        sms.setBulkId(bulkId);
        sms.setStatus("PENDING");
        sms.setUser_login(login);
        sms.setNamereceiver(contact.getConnom());
        sms.setDeliveryStatus("pending");
        sms.setType(sendSms.getType());
        sms.setSent(null);
        sms.setBulkCreatedAt(now);

        if (sendSms.getType() == MessageType.SMS) {
            String msgData = templateService.applyTemplate(template.getContent(), contact);
            sms.setMsgdata(msgData);
            sms.setTotalMessage(templateService.calculateSmsSegments(msgData));
        } else {
            Map<String, String> extracted = builder.extractVariables(template, contact);
            List<VariableDTO> varsList = templateService.buildListeVars(tplReq, extracted);
            sms.setVars(MAPPER.writeValueAsString(varsList));
            sms.setMsgdata(builder.buildMsgDataWithHtml(template.getContent(), contact));
            sms.setTotalMessage(1);
        }

        return sms;
    }

    private String generateUniqueBulkId() {
        String bulkId;
        do {
            bulkId = UUID.randomUUID().toString();
        } while (sendSmsRepository.existsByBulkId(bulkId));
        return bulkId;
    }

    private TemplateRequest parseTemplate(Template template) {
        try {
            String cleaned = cleanTemplateJson(template.getContent());
            return MAPPER.readValue(cleaned, TemplateRequest.class);
        } catch (JsonProcessingException e) {
            throw new CustomException("Template invalide: " + e.getMessage(), HttpStatus.BAD_REQUEST.value());
        }
    }

    private String cleanTemplateJson(String jsonContent) {
        try {
            JsonNode rootNode = MAPPER.readTree(jsonContent);
            if (rootNode.has("components") && rootNode.get("components").isArray()) {
                ArrayNode componentsArray = (ArrayNode) rootNode.get("components");
                for (JsonNode component : componentsArray) {
                    if (component instanceof ObjectNode) {
                        ObjectNode componentObj = (ObjectNode) component;
                        componentObj.remove("mediaValidForMediaComponents");
                        componentObj.remove("textValidForTextComponents");
                        componentObj.remove("buttonsValidForButtonComponents");

                        if (componentObj.has("buttons") && componentObj.get("buttons").isArray()) {
                            ArrayNode buttonsArray = (ArrayNode) componentObj.get("buttons");
                            for (JsonNode button : buttonsArray) {
                                if (button instanceof ObjectNode) {
                                    ObjectNode buttonObj = (ObjectNode) button;
                                    buttonObj.remove("phoneValidForPhoneButtons");
                                    buttonObj.remove("urlValidForUrlButtons");
                                }
                            }
                        }
                    }
                }
            }
            return MAPPER.writeValueAsString(rootNode);
        } catch (JsonProcessingException e) {
            return jsonContent;
        }
    }
}
