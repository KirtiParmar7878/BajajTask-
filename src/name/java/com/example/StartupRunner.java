package com.example.webhookclient;

import com.example.webhookclient.dto.GenerateWebhookResponse;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StartupRunner implements ApplicationRunner {

    private final WebClient webClient;
    private final Environment env;

    public StartupRunner(WebClient.Builder webClientBuilder, Environment env) {
        this.webClient = webClientBuilder.build();
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String generateUrl = env.getProperty("app.generateWebhookUrl");
        String name = env.getProperty("app.name", "kirti parmar");
        String regNo = env.getProperty("app.regNo", "2211401261");
        String email = env.getProperty("app.email", "2211401261@stu.manit.ac.in");

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", name);
        requestBody.put("regNo", regNo);
        requestBody.put("email", email);

        // 1) POST to generateWebhook
        GenerateWebhookResponse resp = webClient.post()
                .uri(generateUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(GenerateWebhookResponse.class)
                .block(Duration.ofSeconds(10));

        if (resp == null || resp.getWebhook() == null) {
            throw new IllegalStateException("generateWebhook returned null or missing fields: " + resp);
        }

        String webhookUrl = resp.getWebhook();
        String accessToken = resp.getAccessToken();
        System.out.println("Received webhookUrl=" + webhookUrl + " accessToken=" + (accessToken != null ? "[REDACTED]" : "null"));

        // 2) Decide which question (last two digits of regNo)
        int lastTwo = extractLastTwoDigits(regNo);
        boolean isOdd = (lastTwo % 2 == 1);
        String finalQuery;

        if (isOdd) {
            // QUESTION 1: use the provided SQL (MySQL flavored here). Use whichever SQL dialect your evaluator expects.
            finalQuery = getQuestion1FinalQuery();
        } else {
            // QUESTION 2 placeholder (if you have question2, fill it here)
            finalQuery = "-- Question 2 SQL goes here";
        }

        // 3) Save finalQuery to file
        Path out = Path.of("final-query.sql");
        Files.writeString(out, finalQuery, StandardCharsets.UTF_8);
        System.out.println("Saved finalQuery to " + out.toAbsolutePath());

        // 4) POST solution to webhook (Authorization: accessToken). Some APIs require "Bearer " prefix. Check the API.
        Map<String, String> submitBody = Map.of("finalQuery", finalQuery);

        // Try with raw token first; if the server expects "Bearer ", switch to "Bearer " + accessToken.
        String authHeaderValue = accessToken; // or "Bearer " + accessToken

        String submitResponse = webClient.post()
                .uri(webhookUrl)
                .header("Authorization", authHeaderValue)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(submitBody)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(10));

        System.out.println("Submit response: " + submitResponse);
    }

    private static int extractLastTwoDigits(String regNo) {
        // get digits only
        String digits = regNo.replaceAll("\\D+", "");
        if (digits.length() == 0) return 0;
        if (digits.length() == 1) return Integer.parseInt(digits);
        return Integer.parseInt(digits.substring(digits.length() - 2));
    }

    private static String getQuestion1FinalQuery() {
        // MySQL version
        return """
                SELECT
                  p.amount AS SALARY,
                  CONCAT(e.first_name, ' ', e.last_name) AS NAME,
                  TIMESTAMPDIFF(YEAR, e.dob, CURDATE()) AS AGE,
                  d.department_name AS DEPARTMENT_NAME
                FROM payments p
                JOIN employee e ON p.emp_id = e.emp_id
                JOIN department d ON e.department = d.department_id
                WHERE DAY(p.payment_time) <> 1
                  AND p.amount = (
                    SELECT MAX(amount) FROM payments WHERE DAY(payment_time) <> 1
                  );
                """;
    }
}