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

    @Value("${gemini.api.url}")
    private String geminiApiUrl;
    @Value("${gemini.api.key}")
    private String geminiAPiKey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String generateEmailReply(EmailRequest emailRequest) {
        //craft a prompt
        String prompt = generatePrompt(emailRequest);

        //generate the request body for gemini api
/*        Map<String, Object> requestBody = Map.of(
                "contents", new Object[] {
                        Map.of("parts", new Object[] {
                                Map.of("text", prompt)
                        })
                }
        );*/



        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        List<Map<String, Object>> partsList = new ArrayList<>(textPart.size());
        partsList.add(textPart);

        Map<String, Object> contentObject = new HashMap<>();
        contentObject.put("parts", partsList);


        List<Map<String, Object>> contentList = new ArrayList<>(contentObject.size());
        contentList.add(contentObject);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", contentList);


        
        //make a request and store the response
        String response = webClient.post()
                .uri(geminiApiUrl + geminiAPiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        //return response

        return extractResponseContent(response);

    }

    private String extractResponseContent(String response) {
        try{
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootnode = mapper.readTree(response);

            return rootnode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        } catch(Exception e) {
            return "Error processing request" + e.getMessage();
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
