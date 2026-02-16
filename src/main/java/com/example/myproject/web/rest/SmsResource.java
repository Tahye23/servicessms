package com.example.myproject.web.rest;

import com.example.myproject.SMSService;
import com.example.myproject.domain.Sms;
import com.example.myproject.domain.enumeration.MessageType;
import com.example.myproject.repository.SmsRepository;
import com.example.myproject.web.rest.dto.SmsStatusResult;
import com.example.myproject.web.rest.errors.ResourceNotFoundException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sms")
public class SmsResource {

    @Autowired
    private SMSService smsService;

    @Autowired
    private SmsRepository smsRepository;

    @PostMapping("/create")
    public ResponseEntity<Sms> createSms(@RequestBody Sms sms) {
        Sms createdSms = smsService.createSms(sms);
        return ResponseEntity.ok(createdSms);
    }

    @GetMapping
    public ResponseEntity<Page<Sms>> getAllSms(Pageable pageable) {
        String login = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(smsService.getAllSmsForUser(pageable, login));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Sms> getSmsById(@PathVariable Long id) {
        String login = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(smsService.getSmsSecure(id, login));
    }

    // Dans un Resource/Controller
    @GetMapping("/{smsId}/query-status")
    public ResponseEntity<SmsStatusResult> querySmsStatus(@PathVariable Long smsId) {
        Sms sms = smsRepository.findById(smsId).orElseThrow(() -> new ResourceNotFoundException("SMS non trouvé"));

        if (sms.getMessageId() == null) {
            return ResponseEntity.badRequest().build();
        }

        SmsStatusResult result = smsService.queryMessageStatus(sms.getMessageId());
        return ResponseEntity.ok(result);
    }
}
