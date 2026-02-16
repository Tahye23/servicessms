package com.example.myproject.web.rest;

import com.example.myproject.service.CampaignHistoryService;
import com.example.myproject.service.dto.RetryAttemptDTO;
import com.example.myproject.web.rest.dto.CampaignHistoryDTO;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignHistoryController {

    private static final Logger log = LoggerFactory.getLogger(CampaignHistoryController.class);

    @Autowired
    private CampaignHistoryService campaignHistoryService;

    @GetMapping("/{campaignId}/history")
    public ResponseEntity<CampaignHistoryDTO> getCampaignHistory(@PathVariable Long campaignId) {
        log.info("GET /api/campaigns/{}/history", campaignId);

        try {
            CampaignHistoryDTO history = campaignHistoryService.getCampaignHistory(campaignId);
            log.info("History returned with {} attempts", history.getRetryCount());
            return ResponseEntity.ok(history);
        } catch (EntityNotFoundException e) {
            log.warn("Campaign {} not found", campaignId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Server error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{campaignId}/retry-history")
    public ResponseEntity<List<RetryAttemptDTO>> getRetryHistory(@PathVariable Long campaignId) {
        log.info("GET /api/campaigns/{}/retry-history", campaignId);

        try {
            List<RetryAttemptDTO> retryHistory = campaignHistoryService.getRetryHistory(campaignId);
            log.info("{} attempts returned", retryHistory.size());
            return ResponseEntity.ok(retryHistory);
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
            return ResponseEntity.ok(List.of());
        }
    }
}
