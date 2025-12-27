package com.email.writer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;


@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value("${openai.api.url}")
    private String openAIApiUrl;
    @Value("${openai.api.key}")
    private String openAIAPiKey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String generateEmailReply(EmailRequest emailRequest) {
        //craft a prompt
        String prompt = generatePrompt(emailRequest);


        // OpenAI Request Body Structure
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo"); // or "gpt-4o"

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "You are a helpful assistant that writes email replies."));
        messages.add(Map.of("role", "user", "content", prompt));

        requestBody.put("messages", messages);


        try{
            String response = webClient.post()
                    .uri(openAIApiUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + openAIAPiKey) // OpenAI uses Bearer token
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractResponseContent(response);
        }catch (Exception e){
            return "Error calling OpenAI API: " + e.getMessage();
        }

    }

    private String extractResponseContent(String response) {
        try{
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootnode = mapper.readTree(response);

            return rootnode.path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText()
                    .trim();
        } catch(Exception e) {
            return "Error processing OpenAI request" + e.getMessage();
        }
    }


    private String generatePrompt(EmailRequest emailRequest) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate A professional Email reply for the following email, do not include the subject line:\n");
        if(emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()){
            prompt.append("use a ").append(emailRequest.getTone()).append("tone.");
        }

        prompt.append("\n Original Email :\n").append(emailRequest.getEmailContent());

        return prompt.toString();
    }
}
