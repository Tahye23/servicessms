package com.example.myproject.service;

import com.example.myproject.domain.Contact;
import com.example.myproject.domain.Template;
import com.example.myproject.web.rest.SendSmsResource;
import com.example.myproject.web.rest.dto.ButtonRequest;
import com.example.myproject.web.rest.dto.ComponentRequest;
import com.example.myproject.web.rest.dto.TemplateRequest;
import com.example.myproject.web.rest.dto.VariableDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TemplateMessageBuilder {

    private final ObjectMapper mapper;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^:}]+):([^}]+)\\}\\}");
    private final Logger log = LoggerFactory.getLogger(TemplateMessageBuilder.class);

    public TemplateMessageBuilder(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Map<String, Object> buildTemplateBlock(Template tpl, List<VariableDTO> varsList) {
        Map<String, Object> fullPayload = buildPayload(tpl, varsList, "FAKE"); // "to" est ignoré
        return (Map<String, Object>) fullPayload.get("template");
    }

    /**
     * Construit uniquement la partie "template" (sans audience ni "to"),
     * exactement comme dans buildPayload(), et toujours avec la clé "components" même vide.
     */
    public Map<String, Object> buildBulkMarketingLitePayload(Template tpl, List<VariableDTO> varsList, List<String> recipients) {
        Map<String, Object> templateNode = buildTemplateNode(tpl, varsList);

        Map<String, Object> payload = new LinkedHashMap<>();
        // ✅ AJOUT DU PARAMÈTRE OBLIGATOIRE
        payload.put("messaging_product", "whatsapp");
        payload.put("send_time", "now");
        payload.put("audience", Map.of("type", "custom", "contacts", recipients.stream().map(phone -> Map.of("phone", phone)).toList()));
        payload.put("message_template", templateNode);

        return payload;
    }

    public Map<String, Object> buildTemplateNode(Template tpl, List<VariableDTO> varsList) {
        try {
            TemplateRequest req = mapper.readValue(tpl.getContent(), TemplateRequest.class);

            Map<String, Object> templateNode = new LinkedHashMap<>();
            templateNode.put("name", tpl.getName());
            templateNode.put("language", Map.of("code", req.getLanguage()));

            List<Map<String, Object>> components = new ArrayList<>();

            // 1️⃣ HEADER avec variables
            processHeaderComponent(req, tpl, varsList, components);

            // 2️⃣ BODY avec variables
            processBodyComponent(req, varsList, components);

            // 3️⃣ FOOTER (si présent)
            processFooterComponent(req, varsList, components);

            // 4️⃣ BUTTONS
            processButtonsComponent(req, varsList, components);

            if (!components.isEmpty()) {
                templateNode.put("components", components);
            }

            return templateNode;
        } catch (Exception e) {
            log.error("[ERROR] buildTemplateNode: {}", e.getMessage(), e);
            throw new RuntimeException("Impossible de construire le template node", e);
        }
    }

    private void processHeaderComponent(
        TemplateRequest req,
        Template tpl,
        List<VariableDTO> varsList,
        List<Map<String, Object>> components
    ) {
        req
            .getComponents()
            .stream()
            .filter(c -> "HEADER".equalsIgnoreCase(c.getType()))
            .findFirst()
            .ifPresent(headerComp -> {
                Map<String, Object> header = new HashMap<>();
                header.put("type", "header");

                List<Map<String, Object>> params = new ArrayList<>();

                // Si c'est un média (IMAGE, VIDEO, DOCUMENT)
                if (
                    headerComp.getFormat() != null &&
                    List.of("IMAGE", "VIDEO", "DOCUMENT").contains(headerComp.getFormat().toUpperCase()) &&
                    tpl.getCode() != null
                ) {
                    Map<String, Object> mediaParam = new HashMap<>();
                    String format = headerComp.getFormat().toLowerCase();
                    mediaParam.put("type", format);
                    mediaParam.put(format, Map.of("id", tpl.getCode()));
                    params.add(mediaParam);
                } else if ("TEXT".equalsIgnoreCase(headerComp.getFormat())) {
                    // Pour les headers TEXT, ajouter les variables texte
                    varsList
                        .stream()
                        .filter(v -> "HEADER".equalsIgnoreCase(v.getType()))
                        .sorted(Comparator.comparingInt(VariableDTO::getOrdre))
                        .forEach(v -> {
                            Map<String, Object> textParam = new HashMap<>();
                            textParam.put("type", "text");
                            textParam.put("text", v.getValeur());
                            params.add(textParam);
                        });
                }

                if (!params.isEmpty()) {
                    header.put("parameters", params);
                    components.add(header);
                }
            });
    }

    private void processBodyComponent(TemplateRequest req, List<VariableDTO> varsList, List<Map<String, Object>> components) {
        // D'abord, vérifier si le template a des variables dans le BODY
        List<VariableDTO> bodyVars = varsList
            .stream()
            .filter(v -> "BODY".equalsIgnoreCase(v.getType()))
            .sorted(Comparator.comparingInt(VariableDTO::getOrdre))
            .toList();

        // Seulement ajouter le composant BODY s'il y a des variables
        if (!bodyVars.isEmpty()) {
            List<Map<String, Object>> bodyParams = bodyVars
                .stream()
                .map(v -> {
                    Map<String, Object> param = new HashMap<>();
                    param.put("type", "text");
                    param.put("text", v.getValeur());
                    return param;
                })
                .toList();

            Map<String, Object> bodyComponent = new HashMap<>();
            bodyComponent.put("type", "body");
            bodyComponent.put("parameters", bodyParams);
            components.add(bodyComponent);
        }
    }

    private void processFooterComponent(TemplateRequest req, List<VariableDTO> varsList, List<Map<String, Object>> components) {
        List<VariableDTO> footerVars = varsList
            .stream()
            .filter(v -> "FOOTER".equalsIgnoreCase(v.getType()))
            .sorted(Comparator.comparingInt(VariableDTO::getOrdre))
            .toList();

        if (!footerVars.isEmpty()) {
            List<Map<String, Object>> footerParams = footerVars
                .stream()
                .map(v -> {
                    Map<String, Object> param = new HashMap<>();
                    param.put("type", "text");
                    param.put("text", v.getValeur());
                    return param;
                })
                .toList();

            Map<String, Object> footerComponent = new HashMap<>();
            footerComponent.put("type", "footer");
            footerComponent.put("parameters", footerParams);
            components.add(footerComponent);
        }
    }

    private void processButtonsComponent(TemplateRequest req, List<VariableDTO> varsList, List<Map<String, Object>> components) {
        // Pour les boutons quick_reply, on n'a généralement pas de paramètres dynamiques
        // Les boutons sont définis dans le template Meta directement

        // Si vous avez des variables pour les boutons, adaptez selon vos besoins
        List<VariableDTO> buttonVars = varsList
            .stream()
            .filter(v -> "BUTTON".equalsIgnoreCase(v.getType()))
            .sorted(Comparator.comparingInt(VariableDTO::getOrdre))
            .toList();

        for (VariableDTO buttonVar : buttonVars) {
            Map<String, Object> button = new HashMap<>();
            button.put("type", "button");
            button.put("sub_type", "quick_reply");
            button.put("index", String.valueOf(buttonVar.getOrdre()));
            button.put("parameters", List.of(Map.of("type", "payload", "payload", buttonVar.getValeur())));
            components.add(button);
        }
    }

    /**
     * Construit le payload JSON pour l'API WhatsApp
     * @param tpl      entité JPA Template
     * @param varsList map des placeholders -> valeurs
     */
    public Map<String, Object> buildPayload(Template tpl, List<VariableDTO> varsList, String to) {
        try {
            TemplateRequest req = mapper.readValue(tpl.getContent(), TemplateRequest.class);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("messaging_product", "whatsapp");
            body.put("to", to);
            body.put("type", "template");

            Map<String, Object> templateNode = new LinkedHashMap<>();
            templateNode.put("name", tpl.getName());
            templateNode.put("language", Map.of("code", req.getLanguage()));

            List<Map<String, Object>> components = new ArrayList<>();

            // 1️⃣ Header media si présent
            req
                .getComponents()
                .stream()
                .filter(
                    c ->
                        "HEADER".equals(c.getType()) &&
                        tpl.getCode() != null &&
                        Arrays.asList("IMAGE", "VIDEO", "DOCUMENT").contains(c.getFormat())
                )
                .findFirst()
                .ifPresent(c -> {
                    Map<String, Object> header = new HashMap<>();
                    header.put("type", "header");
                    Map<String, Object> param = new HashMap<>();
                    String fmt = c.getFormat().toLowerCase();
                    param.put("type", fmt);
                    param.put(fmt, Map.of("id", tpl.getCode()));
                    header.put("parameters", List.of(param));
                    components.add(header);
                });

            // 2️⃣ Body variables
            List<Map<String, Object>> bodyParams = varsList
                .stream()
                .filter(v -> "BODY".equalsIgnoreCase(v.getType()))
                .sorted(Comparator.comparingInt(VariableDTO::getOrdre))
                .map(v -> Map.<String, Object>of("type", "text", "text", v.getValeur()))
                .collect(Collectors.toList());
            if (!bodyParams.isEmpty()) {
                Map<String, Object> bodyComp = new HashMap<>();
                bodyComp.put("type", "body");
                bodyComp.put("parameters", bodyParams);
                components.add(bodyComp);
            }

            // 3️⃣ Footer variables (similaire au body)
            List<Map<String, Object>> footerParams = varsList
                .stream()
                .filter(v -> "FOOTER".equalsIgnoreCase(v.getType()))
                .sorted(Comparator.comparingInt(VariableDTO::getOrdre))
                .map(v -> Map.<String, Object>of("type", "text", "text", v.getValeur()))
                .collect(Collectors.toList());
            if (!footerParams.isEmpty()) {
                Map<String, Object> footerComp = new HashMap<>();
                footerComp.put("type", "footer");
                footerComp.put("parameters", footerParams);
                components.add(footerComp);
            }

            templateNode.put("components", components); // même si vide
            body.put("template", templateNode);
            return body;
        } catch (Exception e) {
            System.out.println("[ERROR] buildPayload exception: " + e.getMessage());
            e.printStackTrace(System.out);
            throw new RuntimeException("Failed to build WhatsApp payload", e);
        }
    }

    private String applyVariables(String text, Map<String, String> vars) {
        Pattern pattern = Pattern.compile("\\{\\{([^:}]+)(?::([^}]*))?\\}\\}");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String def = matcher.group(2) != null ? matcher.group(2).trim() : "";
            String rep = vars.getOrDefault(key, def);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String cleanHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "").replaceAll("&nbsp;", " ").replaceAll("\\s+", " ").trim();
    }

    public Map<String, String> extractVariables(Template tpl, Contact contact) {
        Map<String, String> vars = new HashMap<>();
        Map<String, String> contactValues = new HashMap<>();

        if (contact != null) {
            if (contact.getConnom() != null) contactValues.put("nom", contact.getConnom());
            if (contact.getConprenom() != null) contactValues.put("prenom", contact.getConprenom());
            if (contact.getContelephone() != null) contactValues.put("telephone", contact.getContelephone());
            try {
                if (contact.getCustomFields() != null && !contact.getCustomFields().isBlank()) {
                    Map<String, String> custom = mapper.readValue(contact.getCustomFields(), new TypeReference<>() {});
                    contactValues.putAll(custom);
                }
            } catch (Exception e) {
                log.error("Erreur lecture champs personnalisés: {}", e.getMessage());
            }
        }

        try {
            TemplateRequest req = mapper.readValue(tpl.getContent(), TemplateRequest.class);
            Pattern pattern = Pattern.compile("\\{\\{([^:}]+)(?::([^}]*))?\\}\\}");
            Set<String> seen = new HashSet<>();

            for (ComponentRequest comp : req.getComponents()) {
                // 1) Extraction depuis le corps ou l’en-tête (comp.getText())
                if (comp.getText() != null) {
                    Matcher m = pattern.matcher(comp.getText());
                    while (m.find()) {
                        String name = m.group(1).trim();
                        String def = m.group(2) != null ? m.group(2).trim() : "";
                        if (seen.add(name)) {
                            vars.put(name, contactValues.getOrDefault(name, def));
                        }
                    }
                }

                // 2) Extraction depuis les boutons
                if (comp.getButtons() != null) {
                    for (ButtonRequest btn : comp.getButtons()) {
                        List<String> fields = new ArrayList<>();

                        // Toujours ajouter le texte du bouton
                        if (btn.getText() != null) {
                            fields.add(btn.getText());
                        }
                        // Si vous avez aussi des URLs dans certains boutons
                        if (btn.getUrl() != null) {
                            fields.add(btn.getUrl());
                        }
                        // (on oublie btn.getPayload() qui est toujours null ici)

                        // Maintenant on cherche {{var|default}} dans chacun
                        for (String field : fields) {
                            Matcher m = pattern.matcher(field);
                            while (m.find()) {
                                String name = m.group(1).trim();
                                String def = m.group(2) != null ? m.group(2).trim() : "";
                                if (seen.add(name)) {
                                    vars.put(name, contactValues.getOrDefault(name, def));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Impossible d'extraire les variables du template", e);
        }

        return vars;
    }

    public String buildMsgDataWithHtml(String templateContent, Contact contact) {
        if (templateContent == null || templateContent.isBlank()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        try {
            // Désérialisation du JSON en TemplateRequest
            TemplateRequest tplReq = mapper.readValue(templateContent, TemplateRequest.class);

            // Pattern pour {{clé:default}} ou {{clé}}
            Pattern pattern = Pattern.compile("\\{\\{\\s*([^:}]+)(?::([^}]*))?\\s*\\}\\}");

            for (ComponentRequest comp : tplReq.getComponents()) {
                if (comp.getText() == null) continue;

                String raw = comp.getText();
                Matcher m = pattern.matcher(raw);
                StringBuffer replaced = new StringBuffer();

                while (m.find()) {
                    String key = m.group(1).trim();
                    String defaultVal = m.group(2) != null ? m.group(2).trim() : "";
                    String val = lookupContactValue(key, contact);
                    if (val == null || val.isBlank()) {
                        val = defaultVal;
                    }
                    m.appendReplacement(replaced, Matcher.quoteReplacement(val));
                }
                m.appendTail(replaced);

                // On ajoute **tel quel**, HTML conservé
                sb.append(replaced.toString()).append("\n");
            }
        } catch (Exception e) {
            log.error("Impossible de parser buildMsgDataWithHtml: {}", e.getMessage(), e);
        }

        return sb.toString().trim();
    }

    /**
     * Renvoie la valeur du champ dans Contact ou dans ses customFields JSON.
     */
    private String lookupContactValue(String key, Contact contact) {
        if (contact == null) {
            return null;
        }
        switch (key) {
            case "nom":
                return contact.getConnom();
            case "prenom":
                return contact.getConprenom();
            case "telephone":
                return contact.getContelephone();
            default:
                // tenter de lire dans customFields JSON
                try {
                    if (contact.getCustomFields() != null && !contact.getCustomFields().isBlank()) {
                        Map<String, String> custom = mapper.readValue(
                            contact.getCustomFields(),
                            new TypeReference<Map<String, String>>() {}
                        );
                        return custom.get(key);
                    }
                } catch (Exception ignored) {}
                return null;
        }
    }
}
