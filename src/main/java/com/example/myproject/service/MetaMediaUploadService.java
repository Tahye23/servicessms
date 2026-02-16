package com.example.myproject.service;

import com.example.myproject.domain.Configuration;
import com.example.myproject.repository.ConfigurationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MetaMediaUploadService {

    private final Logger log = LoggerFactory.getLogger(MetaMediaUploadService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ConfigurationRepository configurationRepository;

    // Cache simple des médias uploadés (optionnel)
    private final Map<String, MetaMediaInfo> mediaCache = new ConcurrentHashMap<>();

    public MetaMediaUploadService(ConfigurationRepository configurationRepository) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.configurationRepository = configurationRepository;
    }

    /**
     * Upload direct d'un fichier vers Meta WhatsApp Business API
     */
    public MetaUploadResult uploadToMeta(MultipartFile file, String userLogin) {
        try {
            log.info(
                "📤 Upload direct vers Meta: {} ({}), taille: {} bytes",
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
            );

            // 1. Récupérer la configuration utilisateur
            Optional<Configuration> configOpt = configurationRepository.findOneByUserLogin(userLogin);
            if (!configOpt.isPresent()) {
                return MetaUploadResult.error("Configuration WhatsApp non trouvée pour l'utilisateur: " + userLogin);
            }

            Configuration config = configOpt.get();

            // 2. Valider le fichier
            if (file.isEmpty()) {
                return MetaUploadResult.error("Fichier vide");
            }

            if (file.getSize() > 16 * 1024 * 1024) { // Limite Meta: 16MB
                return MetaUploadResult.error("Fichier trop volumineux (max 16MB pour WhatsApp)");
            }

            if (!isValidWhatsAppMediaType(file.getContentType())) {
                return MetaUploadResult.error("Type de fichier non supporté par WhatsApp: " + file.getContentType());
            }

            // 3. Préparer l'URL Meta
            String uploadUrl = String.format("https://graph.facebook.com/v18.0/%s/media", config.getPhoneNumberId());

            // 4. Préparer les headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(config.getAccessToken());

            // 5. Préparer le body multipart
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("messaging_product", "whatsapp");
            body.add("type", file.getContentType());

            // Créer la ressource fichier
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            log.debug("🔄 Envoi vers Meta API: {}", uploadUrl);

            // 6. Appel API Meta
            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                // 7. Parser la réponse
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                String mediaId = jsonResponse.path("id").asText();

                log.info("✅ Upload Meta réussi: {} → mediaId: {}", file.getOriginalFilename(), mediaId);

                // 8. Mettre en cache (optionnel)
                MetaMediaInfo mediaInfo = new MetaMediaInfo();
                mediaInfo.setId(mediaId);
                mediaInfo.setMimeType(file.getContentType());
                mediaInfo.setFileSize(file.getSize());
                mediaInfo.setOriginalFilename(file.getOriginalFilename());
                mediaCache.put(mediaId, mediaInfo);

                return MetaUploadResult.success(mediaId, file.getOriginalFilename(), file.getContentType(), file.getSize());
            } else {
                log.error("❌ Erreur API Meta: {} - {}", response.getStatusCode(), response.getBody());
                return MetaUploadResult.error("Erreur API Meta: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("💥 Exception lors de l'upload Meta: {}", e.getMessage(), e);
            return MetaUploadResult.error("Exception: " + e.getMessage());
        }
    }

    /**
     * Récupérer les informations d'un média depuis Meta
     */
    public MetaMediaInfo getMetaMediaInfo(String mediaId, String userLogin) {
        try {
            // Vérifier d'abord le cache
            MetaMediaInfo cached = mediaCache.get(mediaId);
            if (cached != null) {
                log.debug("📋 Info média récupérées du cache: {}", mediaId);
                return cached;
            }

            // Récupérer depuis Meta
            Optional<Configuration> configOpt = configurationRepository.findOneByUserLogin(userLogin);
            if (!configOpt.isPresent()) {
                log.warn("⚠️ Configuration non trouvée pour user: {}", userLogin);
                return null;
            }

            Configuration config = configOpt.get();
            String url = String.format("https://graph.facebook.com/v18.0/%s", mediaId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(config.getAccessToken());

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());

                MetaMediaInfo mediaInfo = new MetaMediaInfo();
                mediaInfo.setId(jsonResponse.path("id").asText());
                mediaInfo.setUrl(jsonResponse.path("url").asText());
                mediaInfo.setMimeType(jsonResponse.path("mime_type").asText());
                mediaInfo.setSha256(jsonResponse.path("sha256").asText());
                mediaInfo.setFileSize(jsonResponse.path("file_size").asLong());

                // Mettre en cache
                mediaCache.put(mediaId, mediaInfo);

                log.debug("✅ Info média Meta récupérées: {}", mediaId);
                return mediaInfo;
            } else {
                log.warn("⚠️ Erreur récupération Meta info: {} - {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ Erreur récupération info Meta média: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * Télécharger un média depuis Meta (si nécessaire)
     */
    public byte[] downloadMetaMedia(String mediaId, String userLogin) {
        try {
            MetaMediaInfo mediaInfo = getMetaMediaInfo(mediaId, userLogin);
            if (mediaInfo == null || mediaInfo.getUrl() == null) {
                log.warn("⚠️ Impossible de récupérer l'URL pour le média: {}", mediaId);
                return null;
            }

            Optional<Configuration> configOpt = configurationRepository.findOneByUserLogin(userLogin);
            if (!configOpt.isPresent()) {
                return null;
            }

            Configuration config = configOpt.get();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(config.getAccessToken());

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(mediaInfo.getUrl(), HttpMethod.GET, entity, byte[].class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Média téléchargé depuis Meta: {} ({} bytes)", mediaId, response.getBody().length);
                return response.getBody();
            } else {
                log.warn("⚠️ Erreur téléchargement Meta: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("❌ Erreur téléchargement Meta média: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * Valider si le type de fichier est supporté par WhatsApp
     */
    private boolean isValidWhatsAppMediaType(String mimeType) {
        if (mimeType == null) return false;

        // Types supportés par WhatsApp Business API
        return (
            mimeType.equals("image/jpeg") ||
            mimeType.equals("image/png") ||
            mimeType.equals("image/webp") ||
            mimeType.equals("video/mp4") ||
            mimeType.equals("video/3gpp") ||
            mimeType.equals("audio/aac") ||
            mimeType.equals("audio/mp4") ||
            mimeType.equals("audio/mpeg") ||
            mimeType.equals("audio/amr") ||
            mimeType.equals("audio/ogg") ||
            mimeType.equals("application/pdf") ||
            mimeType.equals("application/vnd.ms-powerpoint") ||
            mimeType.equals("application/msword") ||
            mimeType.equals("application/vnd.ms-excel") ||
            mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
            mimeType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation") ||
            mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
            mimeType.equals("text/plain")
        );
    }

    /**
     * Nettoyer le cache (à appeler périodiquement si nécessaire)
     */
    public void clearCache() {
        mediaCache.clear();
        log.info("🧹 Cache des médias Meta nettoyé");
    }

    /**
     * Obtenir des statistiques du cache
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("cacheSize", mediaCache.size());
        stats.put("mediaIds", mediaCache.keySet());
        return stats;
    }

    // ================================
    // CLASSES DE RÉSULTAT
    // ================================

    public static class MetaUploadResult {

        private final boolean success;
        private final String message;
        private final String mediaId;
        private final String filename;
        private final String mimeType;
        private final Long fileSize;

        private MetaUploadResult(boolean success, String message, String mediaId, String filename, String mimeType, Long fileSize) {
            this.success = success;
            this.message = message;
            this.mediaId = mediaId;
            this.filename = filename;
            this.mimeType = mimeType;
            this.fileSize = fileSize;
        }

        public static MetaUploadResult success(String mediaId, String filename, String mimeType, Long fileSize) {
            return new MetaUploadResult(true, "Success", mediaId, filename, mimeType, fileSize);
        }

        public static MetaUploadResult error(String message) {
            return new MetaUploadResult(false, message, null, null, null, null);
        }

        // Getters
        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getMediaId() {
            return mediaId;
        }

        public String getFilename() {
            return filename;
        }

        public String getMimeType() {
            return mimeType;
        }

        public Long getFileSize() {
            return fileSize;
        }

        @Override
        public String toString() {
            return (
                "MetaUploadResult{" +
                "success=" +
                success +
                ", message='" +
                message +
                '\'' +
                ", mediaId='" +
                mediaId +
                '\'' +
                ", filename='" +
                filename +
                '\'' +
                ", mimeType='" +
                mimeType +
                '\'' +
                ", fileSize=" +
                fileSize +
                '}'
            );
        }
    }

    public static class MetaMediaInfo {

        private String id;
        private String url;
        private String mimeType;
        private String sha256;
        private Long fileSize;
        private String originalFilename;

        // Constructeurs
        public MetaMediaInfo() {}

        // Getters et setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getSha256() {
            return sha256;
        }

        public void setSha256(String sha256) {
            this.sha256 = sha256;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }

        public String getOriginalFilename() {
            return originalFilename;
        }

        public void setOriginalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
        }

        @Override
        public String toString() {
            return (
                "MetaMediaInfo{" +
                "id='" +
                id +
                '\'' +
                ", url='" +
                url +
                '\'' +
                ", mimeType='" +
                mimeType +
                '\'' +
                ", sha256='" +
                sha256 +
                '\'' +
                ", fileSize=" +
                fileSize +
                ", originalFilename='" +
                originalFilename +
                '\'' +
                '}'
            );
        }
    }
}
