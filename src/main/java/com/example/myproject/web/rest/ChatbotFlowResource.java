// ChatbotFlowResource.java - AJOUTER ces imports et méthodes

package com.example.myproject.web.rest;

import com.example.myproject.domain.ChatbotFlow;
import com.example.myproject.security.SecurityUtils;
import com.example.myproject.service.ChatbotFlowService;
import com.example.myproject.service.MetaMediaUploadService; // NOUVEAU
import com.example.myproject.web.rest.dto.flow.ApiResponse;
import com.example.myproject.web.rest.dto.flow.FlowPayload;
import com.example.myproject.web.rest.dto.flow.MediaUploadResponse; // NOUVEAU
import com.example.myproject.web.rest.dto.flow.SaveFlowResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // NOUVEAU

@RestController
@RequestMapping("/api/chatbot-flows")
public class ChatbotFlowResource {

    private final Logger log = LoggerFactory.getLogger(ChatbotFlowResource.class);
    private final ChatbotFlowService chatbotFlowService;
    private final MetaMediaUploadService metaMediaUploadService; // NOUVEAU

    public ChatbotFlowResource(
        ChatbotFlowService chatbotFlowService,
        MetaMediaUploadService metaMediaUploadService // NOUVEAU
    ) {
        this.chatbotFlowService = chatbotFlowService;
        this.metaMediaUploadService = metaMediaUploadService; // NOUVEAU
    }

    /**
     * POST /api/chatbot-flows/save : Créer ou Modifier le flow
     */
    @PostMapping("/save")
    public ResponseEntity<ApiResponse<SaveFlowResponse>> saveFlow(@Valid @RequestBody FlowPayload flowPayload) {
        try {
            Long userId = getCurrentUserId();
            log.debug("Sauvegarde flow pour user: {}", userId);

            // Sauvegarder ou mettre à jour
            ChatbotFlow savedFlow = chatbotFlowService.saveOrUpdateFlow(userId, flowPayload);

            // Réponse
            SaveFlowResponse response = new SaveFlowResponse();
            response.setFlowId(savedFlow.getId().toString());
            response.setId(savedFlow.getId());
            response.setSuccess(true);
            response.setMessage("Flow sauvegardé avec succès");

            return ResponseEntity.ok(new ApiResponse<>(true, response, "Flow sauvegardé avec succès"));
        } catch (Exception e) {
            log.error("Erreur sauvegarde flow: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, null, e.getMessage()));
        }
    }

    /**
     * GET /api/chatbot-flows/current : Récupérer le flow actuel
     */
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<FlowPayload>> getCurrentFlow() {
        try {
            Long userId = getCurrentUserId();
            log.debug("Récupération flow actuel pour user: {}", userId);

            FlowPayload currentFlow = chatbotFlowService.getCurrentFlow(userId);

            String message = currentFlow != null ? "Flow trouvé" : "Aucun flow trouvé";
            return ResponseEntity.ok(new ApiResponse<>(true, currentFlow, message));
        } catch (Exception e) {
            log.error("Erreur récupération flow: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, null, e.getMessage()));
        }
    }

    // ================================
    // NOUVEAU: ENDPOINT UPLOAD MÉDIA
    // ================================

    /**
     * POST /api/chatbot-flows/upload-media : Upload média vers Meta WhatsApp
     */
    @PostMapping("/upload-media")
    public ResponseEntity<ApiResponse<MediaUploadResponse>> uploadMedia(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "type", defaultValue = "auto") String type
    ) {
        try {
            String userLogin = getCurrentUserLogin(); // Récupérer le login utilisateur
            log.info("📤 Upload média pour utilisateur: {} (taille: {} bytes)", userLogin, file.getSize());

            // Validation du fichier
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, null, "Fichier vide"));
            }

            // Déterminer le type automatiquement si pas spécifié
            if ("auto".equals(type)) {
                type = determineMediaType(file.getContentType());
            }

            // Upload vers Meta
            MetaMediaUploadService.MetaUploadResult result = metaMediaUploadService.uploadToMeta(file, userLogin);

            if (result.isSuccess()) {
                // Créer la réponse
                MediaUploadResponse response = new MediaUploadResponse();
                response.setUrl("meta://" + result.getMediaId()); // URL spéciale pour le front
                response.setMediaId(result.getMediaId());
                response.setFilename(result.getFilename());
                response.setMimeType(result.getMimeType());
                response.setFileSize(result.getFileSize());
                response.setProvider("meta");

                log.info("✅ Média uploadé vers Meta: {} → {}", file.getOriginalFilename(), result.getMediaId());

                return ResponseEntity.ok(new ApiResponse<>(true, response, "Média uploadé avec succès"));
            } else {
                log.error("❌ Échec upload Meta: {}", result.getMessage());
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, null, "Erreur upload: " + result.getMessage()));
            }
        } catch (Exception e) {
            log.error("💥 Erreur lors de l'upload: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, null, "Erreur serveur: " + e.getMessage()));
        }
    }

    /**
     * GET /api/chatbot-flows/media-info/{mediaId} : Récupérer les infos d'un média
     */
    @GetMapping("/media-info/{mediaId}")
    public ResponseEntity<ApiResponse<MediaUploadResponse>> getMediaInfo(@PathVariable String mediaId) {
        try {
            String userLogin = getCurrentUserLogin();
            log.debug("📋 Récupération info média: {} pour user: {}", mediaId, userLogin);

            MetaMediaUploadService.MetaMediaInfo info = metaMediaUploadService.getMetaMediaInfo(mediaId, userLogin);

            if (info != null) {
                MediaUploadResponse response = new MediaUploadResponse();
                response.setUrl("meta://" + info.getId());
                response.setMediaId(info.getId());
                response.setFilename(info.getId()); // Meta ne retourne pas le nom original
                response.setMimeType(info.getMimeType());
                response.setFileSize(info.getFileSize());
                response.setProvider("meta");

                return ResponseEntity.ok(new ApiResponse<>(true, response, "Info récupérées"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("💥 Erreur récupération info média: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, null, "Erreur: " + e.getMessage()));
        }
    }

    // ================================
    // MÉTHODES UTILITAIRES
    // ================================

    private String determineMediaType(String mimeType) {
        if (mimeType == null) return "document";

        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("video/")) return "video";
        if (mimeType.startsWith("audio/")) return "audio";
        return "document";
    }

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId().orElseThrow(() -> new IllegalStateException("Non authentifié"));
    }

    private String getCurrentUserLogin() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new IllegalStateException("Non authentifié"));
    }
}
