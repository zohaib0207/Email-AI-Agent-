package com.email.email.writer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.Map;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiAPIUrl;

    @Value("${gemini.api.key}")
    private String geminiAPIkey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String generateEmailReply(EmailRequest emailRequest) {
        String prompt = buildPrompt(emailRequest);

        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        try {
            String response = Mono.fromCallable(() -> {
                        return webClient.post()
                                .uri(geminiAPIUrl + "?key=" + geminiAPIkey)
                                .header("Content-Type", "application/json")
                                .bodyValue(requestBody)
                                .retrieve()
                                .bodyToMono(String.class)
                                .block();
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .block();

            return extractResponseContent(response);
        } catch (Exception e) {
            return "Connection Error: " + e.getMessage();
        }
    }

    private String extractResponseContent(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);

            // Check if the AI actually returned a result
            if (rootNode.has("candidates")) {
                return rootNode.path("candidates").get(0)
                        .path("content").path("parts").get(0)
                        .path("text").asText();
            }
            return "Google Error: " + response;
        } catch (Exception e) {
            return "Parsing Error: " + e.getMessage();
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        return "Generate a professional email reply. Tone: " + emailRequest.getTone() +
                "\nOriginal Email==: " + emailRequest.getEmailContent();
    }
}