package com.restaurantmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantmanager.dto.request.ScannedMenuCategoryInput;
import com.restaurantmanager.dto.request.ScannedMenuItemInput;
import com.restaurantmanager.entity.FoodType;
import com.restaurantmanager.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Reads a photo/scan of a restaurant menu using a vision-capable AI model and extracts structured items. */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiVisionService {

    private static final long MAX_IMAGE_BYTES = 8L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    private static final String PROMPT = """
            You are reading a photo or scan of a restaurant menu. Extract every menu category \
            and every item within each category, along with its price.

            Respond with ONLY a JSON object (no markdown fences, no commentary) matching exactly \
            this shape:
            {
              "categories": [
                {
                  "name": "the category heading as printed, e.g. Starters, Main Course",
                  "items": [
                    {
                      "name": "the item name",
                      "description": "any description text under the item, or null",
                      "price": 0.0,
                      "foodType": "VEG, EGG, or NON_VEG if you can tell from a dot/symbol or the dish itself, else null"
                    }
                  ]
                }
              ]
            }

            The "price" field must be a plain decimal number with no currency symbol. If the photo \
            has no visible category headings, put everything under one category named "Menu". Skip \
            any text that isn't a menu item (addresses, phone numbers, decorative text).
            """;

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.builder()
            .requestFactory(clientHttpRequestFactory())
            .build();

    private static ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(15));
        factory.setReadTimeout(Duration.ofSeconds(45));
        return factory;
    }

    @Value("${app.ai.gemini.api-key:}")
    private String apiKey;

    @Value("${app.ai.gemini.model:gemini-2.0-flash}")
    private String model;

    public List<ScannedMenuCategoryInput> scanMenuImage(MultipartFile file) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException(
                    "AI menu scanning isn't configured on this server. Set the GEMINI_API_KEY environment variable.");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was uploaded");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BadRequestException("That photo is too large; please upload one under 8MB.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Please upload a PNG, JPEG, or WEBP photo of the menu.");
        }

        byte[] imageBytes;
        try {
            imageBytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file");
        }

        String rawResponse = callGemini(imageBytes, contentType);
        String extractedJson = extractText(rawResponse);
        List<ScannedMenuCategoryInput> categories = parseCategories(extractedJson);
        if (categories.isEmpty()) {
            throw new BadRequestException("Couldn't find any menu items in that photo. Try a clearer, well-lit photo.");
        }
        return categories;
    }

    private String callGemini(byte[] imageBytes, String contentType) {
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        Map<String, Object> textPart = Map.of("text", PROMPT);
        Map<String, Object> imagePart = Map.of("inline_data", Map.of("mime_type", contentType, "data", base64));
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(textPart, imagePart))),
                "generationConfig", Map.of("responseMimeType", "application/json"));

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model
                + ":generateContent?key=" + apiKey;

        try {
            return restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (HttpStatusCodeException e) {
            log.warn("Gemini API call failed with status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BadRequestException(
                    "Couldn't reach the AI menu-scanning service (status " + e.getStatusCode().value() + "). Please try again.");
        } catch (RestClientException e) {
            log.warn("Gemini API call failed", e);
            String cause = e.getCause() != null ? e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage()
                    : e.getClass().getSimpleName() + ": " + e.getMessage();
            throw new BadRequestException("Couldn't reach the AI menu-scanning service. Please try again. (" + cause + ")");
        }
    }

    private String extractText(String rawResponse) {
        JsonNode textNode;
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        } catch (Exception e) {
            throw new BadRequestException("Unexpected response from the AI menu-scanning service.");
        }
        if (textNode.isMissingNode() || textNode.asText().isBlank()) {
            throw new BadRequestException("The AI didn't return any text for that photo. Try again.");
        }
        return textNode.asText();
    }

    private List<ScannedMenuCategoryInput> parseCategories(String json) {
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new BadRequestException("The AI couldn't read that menu clearly. Try a clearer photo.");
        }

        List<ScannedMenuCategoryInput> categories = new ArrayList<>();
        for (JsonNode categoryNode : root.path("categories")) {
            String categoryName = textOrNull(categoryNode, "name");
            if (categoryName == null || categoryName.isBlank()) {
                continue;
            }

            List<ScannedMenuItemInput> items = new ArrayList<>();
            for (JsonNode itemNode : categoryNode.path("items")) {
                String itemName = textOrNull(itemNode, "name");
                if (itemName == null || itemName.isBlank()) {
                    continue;
                }
                BigDecimal price = parsePrice(itemNode.path("price"));
                String description = textOrNull(itemNode, "description");
                FoodType foodType = parseFoodType(textOrNull(itemNode, "foodType"));
                items.add(new ScannedMenuItemInput(itemName.trim(), description, price, foodType));
            }
            if (!items.isEmpty()) {
                categories.add(new ScannedMenuCategoryInput(categoryName.trim(), items));
            }
        }
        return categories;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return (value.isMissingNode() || value.isNull()) ? null : value.asText();
    }

    private BigDecimal parsePrice(JsonNode priceNode) {
        if (priceNode.isNumber()) {
            return priceNode.decimalValue();
        }
        if (priceNode.isTextual()) {
            String digits = priceNode.asText().replaceAll("[^0-9.]", "");
            if (!digits.isEmpty()) {
                try {
                    return new BigDecimal(digits);
                } catch (NumberFormatException ignored) {
                    // fall through to zero below
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private FoodType parseFoodType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return FoodType.valueOf(raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
