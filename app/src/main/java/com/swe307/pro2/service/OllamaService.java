package com.swe307.pro2.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;

@Service 
public class OllamaService {

    private static final Logger log= LoggerFactory.getLogger(OllamaService.class);

    private final RestClient _client;
    private final String _model;

    public OllamaService(@Value("${ollama.url}") String url,
                         @Value("${ollama.model}") String model) {
        _client = RestClient.builder().baseUrl(url).build();
        _model = model;
    }

    public String chat(String userMessage){
        log.info("Ollama'ya soru gonderiliyor: {}", userMessage);

        Map<String,Object> body= Map.of(
            "model", _model,
            "stream", false,
            "messages", List.of(Map.of("role","user","content",userMessage))

        );

        JsonNode response = _client.post()
        .uri("/api/chat")
        .body(body)
        .retrieve()
        .body(JsonNode.class);

        String reply = response.path("message").path("content").asString();
        log.info("Ollama cevap verdi ({}karakter)", reply.length());
        return reply;

    }
}
