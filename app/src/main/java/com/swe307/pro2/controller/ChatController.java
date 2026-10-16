package com.swe307.pro2.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.swe307.pro2.dto.ChatRequestDto;
import com.swe307.pro2.dto.ChatResponseDto;
import com.swe307.pro2.service.OllamaService;

@RestController 
public class ChatController {

    private static final Logger log=LoggerFactory.getLogger(ChatController.class);

    private final OllamaService _ollama;

    public ChatController(OllamaService ollama)
    {
        _ollama=ollama;
    }

    @PostMapping("/api/chat")
    public ResponseEntity<ChatResponseDto> chat(@RequestBody ChatRequestDto request){
        if(request.message()==null || request.message().isBlank()){
            return ResponseEntity.badRequest().body(new ChatResponseDto("Lütfen bir soru yazın!"));
        }

        try{
            String reply =_ollama.chat(request.message());
            return ResponseEntity.ok(new ChatResponseDto(reply));
        } catch(Exception e)
        {
            log.error("Ollama ile konusulamadı!", e);
            return ResponseEntity.status(503)
            .body(new ChatResponseDto("Ollamaya ulaşılamadı, sunucu calısıyor mu?"));
        }
    }
    
}
