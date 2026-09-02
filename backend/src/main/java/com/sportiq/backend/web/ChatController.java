package com.sportiq.backend.web;

import com.sportiq.backend.service.ChatService;
import com.sportiq.backend.web.dto.ChatRequest;
import com.sportiq.backend.web.dto.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chatbot endpoint: {@code POST /api/chat} with
 * {@code {"message": "..."}} returns {@code {"reply": "..."}}.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return new ChatResponse(chatService.reply(request.message()));
    }
}