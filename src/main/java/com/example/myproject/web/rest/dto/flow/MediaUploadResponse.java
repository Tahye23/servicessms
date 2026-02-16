package com.example.myproject.web.rest.dto.flow;

public class MediaUploadResponse {

    private String url; // URL pour le front (ex: "meta://123456")
    private String mediaId; // ID Meta du média
    private String filename; // Nom du fichier
    private String mimeType; // Type MIME
    private Long fileSize; // Taille en bytes
    private String provider; // "meta" pour WhatsApp

    // Constructeurs
    public MediaUploadResponse() {}

    public MediaUploadResponse(String url, String mediaId, String filename, String mimeType, Long fileSize, String provider) {
        this.url = url;
        this.mediaId = mediaId;
        this.filename = filename;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.provider = provider;
    }

    // Getters et Setters
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    @Override
    public String toString() {
        return (
            "MediaUploadResponse{" +
            "url='" +
            url +
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
            ", provider='" +
            provider +
            '\'' +
            '}'
        );
    }
}
