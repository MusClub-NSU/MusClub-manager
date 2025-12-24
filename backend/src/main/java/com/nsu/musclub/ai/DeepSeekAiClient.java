package com.nsu.musclub.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class DeepSeekAiClient implements AiTextClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiUrl;
    private final String apiKey;
    private final String model;

    public DeepSeekAiClient(
            @Value("${deepseek.api-url}") String apiUrl,
            @Value("${deepseek.api-key}") String apiKey,
            @Value("${deepseek.model:deepseek-chat}"
            ) String model) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String generateText(String systemPrompt, String userPrompt) {
        DeepSeekRequest request = new DeepSeekRequest();
        request.model = this.model;
        request.messages = Arrays.asList(new Message("system", systemPrompt), new Message("user", userPrompt));
        request.stream = false;
        request.max_tokens = 512;
        request.temperature = 0.7;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<DeepSeekRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<DeepSeekResponse> responseEntity = restTemplate.postForEntity(apiUrl, entity, DeepSeekResponse.class);

        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("DeepSeek API returned non-2xx status: " + responseEntity.getStatusCode());
        }

        DeepSeekResponse response = responseEntity.getBody();
        if (response == null || response.choices == null || response.choices.isEmpty()) {
            throw new RuntimeException("Empty response from DeepSeek API");
        }

        Message msg = response.choices.get(0).message;
        if (msg == null || msg.content == null) {
            throw new RuntimeException("No message content in DeepSeek response");
        }

        return msg.content.trim();
    }


    public static class DeepSeekRequest {
        public String model;
        public List<Message> messages;
        public Boolean stream;
        public Integer max_tokens;
        public Double temperature;
    }

    public static class Message {
        public String role;
        public String content;

        public Message() {
        }

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public static class DeepSeekResponse {
        public List<Choice> choices;
    }

    public static class Choice {
        public Message message;
    }
}
