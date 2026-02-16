package com.example.myproject.web.rest;

import com.example.myproject.SMSService;
import com.example.myproject.domain.Chat;
import com.example.myproject.domain.Sms;
import com.example.myproject.security.SecurityUtils;
import com.example.myproject.service.ChatService;
import com.example.myproject.service.dto.ChatSummaryDTO;
import com.example.myproject.web.rest.dto.ChatMessageRequest;
import com.example.myproject.web.rest.dto.ChatMessageResponseDTO;
import com.example.myproject.web.rest.dto.ChatRequestDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chats")
@Validated
public class ChatController {

    private final ChatService chatService;

    @Autowired
    private SMSService smsService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<Chat> createChat(@Valid @RequestBody ChatRequestDTO request) {
        Chat result = chatService.createChat(request);
        return ResponseEntity.created(URI.create("/api/chats/" + result.getId())).body(result);
    }

    @PostMapping("/{chatId}/messages")
    public ResponseEntity<ChatMessageResponseDTO> sendChatMessage(
        @RequestParam(name = "test", defaultValue = "false") boolean test,
        @Valid @RequestBody ChatMessageRequest request
    ) throws JsonProcessingException {
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new IllegalStateException("Non authentifié"));
        ChatMessageResponseDTO response = chatService.sendMessage(test, request, login);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<Chat>> getAllChats(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastUpdated"));
        Page<Chat> chatsPage = chatService.getAllChats(pageable);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(chatsPage.getTotalElements()));

        return ResponseEntity.ok().headers(headers).body(chatsPage.getContent());
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<Sms>> getMessagesByChatId(
        @PathVariable Long chatId,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "sendDate"));
        Page<Sms> messagesPage = smsService.getMessagesByChatId(chatId, pageable);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(messagesPage.getTotalElements()));

        return ResponseEntity.ok().headers(headers).body(messagesPage.getContent());
    }

    @GetMapping("/by-contact/{contactId}/group-by-channel")
    public ResponseEntity<Map<String, List<ChatSummaryDTO>>> getChatsGroupedByChannel(@PathVariable Long contactId) {
        return chatService.getChatsGroupedByChannel(contactId);
    }
}
