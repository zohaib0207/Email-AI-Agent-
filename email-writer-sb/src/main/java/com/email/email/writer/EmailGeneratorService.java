package com.email.email.writer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;
    @Value("${gemini.api.key}")
    private String geminiAPIkey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<String> generateEmailReply(EmailRequest emailRequest) {
        String tone = (emailRequest.getTone() == null || emailRequest.getTone().isEmpty())
                ? "professional"
                : emailRequest.getTone();

        String prompt = "Write ONLY ONE " + tone + " email reply.\n"
                + "Do not give multiple options.\n"
                + "Do not add explanations.\n"
                + "Just give the final email.\n\n"
                + "Email:\n" + emailRequest.getEmailContent();

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                )
        );

        return webClient.post()
                .uri("https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + geminiAPIkey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .bodyValue(requestBody)
                .exchangeToMono(response ->
                        response.bodyToMono(String.class)
                                .map(body -> {
                                    System.out.println("STATUS: " + response.statusCode());
                                    System.out.println("BODY: " + body);
                                    return body;
                                })
                )
                .retry(2)
                .map(this::extractResponseContent)
                .onErrorResume(e -> Mono.just("ERROR: " + e.getMessage()));
    }

    private String extractResponseContent(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);

            return root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

        } catch (Exception e) {
            return "Parsing failed: " + response;
        }
    }
}