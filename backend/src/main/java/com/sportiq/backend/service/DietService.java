package com.sportiq.backend.service;

import com.sportiq.backend.exception.ChatUnavailableException;
import com.sportiq.backend.web.dto.DietAdviceRequest;
import com.sportiq.backend.web.dto.DietAdviceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Backs {@code POST /api/diet-advice}. Builds a nutrition-coach prompt from
 * the BMI or Calorie calculator inputs and asks phi4-mini for a structured
 * diet &amp; lifestyle plan. Unreadable model output surfaces as
 * {@link ChatUnavailableException} (503) instead of leaking internals.
 */
@Service
public class DietService {

    public static final String UNAVAILABLE_REPLY =
            "SportIQ Assistant is temporarily unavailable. Please try again.";

    private static final Logger log = LoggerFactory.getLogger(DietService.class);

    private static final Map<String, String> ACTIVITY_LABELS = Map.of(
            "1.2", "Sedentary (little exercise)",
            "1.375", "Light (1–3 sessions / week)",
            "1.55", "Moderate (3–5 sessions / week)",
            "1.725", "Very active (6–7 sessions / week)",
            "1.9", "Athlete (hard training daily)");

    private static final String SYSTEM_PROMPT = """
            You are SportIQ's certified nutrition coach. A user just used one of \
            our calculators and wants a personalised diet and lifestyle plan.\n
            Rules:
            - Use ONLY the user profile and numbers provided in the prompt.
            - The diet must be safe, practical and tailored to the user's goal \
            (lose, maintain, or gain weight based on the calculator result).
            - Give a 1-2 sentence summary, then 3-5 specific food tips, then a \
            balanced sample daily meal plan (breakfast, lunch, dinner, snacks) \
            with rough portions, then 3-4 lifestyle tips.
            - Return a single JSON object with EXACTLY these four keys and no \
            other text or markdown: \
            {"summary":"...","foodTips":["..."],"mealPlan":["..."],"lifestyleTips":["..."]}
            """;

    private final ChatClient dietChatClient;
    private final ObjectMapper objectMapper;

    public DietService(@Qualifier("dietChatClient") ChatClient dietChatClient,
                       ObjectMapper objectMapper) {
        this.dietChatClient = dietChatClient;
        this.objectMapper = objectMapper;
    }

    public DietAdviceResponse advise(DietAdviceRequest request) {
        if (request == null || request.calculator() == null) {
            throw new IllegalArgumentException("calculator is required");
        }
        try {
            String content = dietChatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt(request))
                    .call()
                    .content();
            return parse(content);
        } catch (Exception ex) {
            log.warn("Diet advice failed: {}", ex.getMessage());
            throw new ChatUnavailableException(UNAVAILABLE_REPLY, ex);
        }
    }

    private DietAdviceResponse parse(String content) {
        String json = extractJsonObject(content == null ? "" : content);
        JsonNode root = objectMapper.readTree(json);
        if (root == null || !root.isObject()) {
            throw new IllegalStateException("Diet advice is not a JSON object");
        }
        return new DietAdviceResponse(
                text(root, "summary"),
                strings(root, "foodTips"),
                strings(root, "mealPlan"),
                strings(root, "lifestyleTips"));
    }

    private static String extractJsonObject(String content) {
        String trimmed = stripFences(content);
        int start = trimmed.indexOf('{');
        if (start == -1) {
            throw new IllegalStateException("Diet advice does not contain a JSON object");
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return trimmed.substring(start, i + 1);
                }
            }
        }
        throw new IllegalStateException("Diet advice JSON object is unbalanced");
    }

    private static String stripFences(String content) {
        String json = content.trim();
        int fence = json.indexOf("```");
        if (fence != -1) {
            json = json.substring(json.indexOf('\n', fence) + 1);
            int endFence = json.lastIndexOf("```");
            if (endFence != -1) {
                json = json.substring(0, endFence);
            }
            json = json.trim();
        }
        return json;
    }

    private static String text(JsonNode node, String key) {
        JsonNode field = node.get(key);
        return field == null || field.isMissingNode() || field.isNull() ? "" : field.asText();
    }

    private static List<String> strings(JsonNode node, String key) {
        JsonNode field = node.get(key);
        List<String> out = new ArrayList<>();
        if (field == null || field.isMissingNode() || field.isNull()) {
            return out;
        }
        if (field.isArray()) {
            for (JsonNode item : field) {
                if (item.isNull()) {
                    continue;
                }
                if (item.isObject()) {
                    out.addAll(flattenObject(item));
                } else {
                    out.add(asText(item));
                }
            }
        } else if (field.isObject()) {
            out.addAll(flattenObject(field));
        } else {
            out.add(asText(field));
        }
        return out;
    }

    private static List<String> flattenObject(JsonNode object) {
        List<String> out = new ArrayList<>();
        object.properties().forEach(entry -> {
            String label = entry.getKey();
            JsonNode value = entry.getValue();
            if (value.isArray()) {
                out.add(capitalize(label) + ": " + joinItems(value));
            } else if (value.isObject()) {
                out.add(capitalize(label) + ": " + value.toString());
            } else if (!value.isNull()) {
                out.add(capitalize(label) + ": " + asText(value));
            }
        });
        return out;
    }

    private static String joinItems(JsonNode array) {
        List<String> items = new ArrayList<>();
        for (JsonNode item : array) {
            if (!item.isNull()) {
                items.add(asText(item));
            }
        }
        return String.join("; ", items);
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static String asText(JsonNode item) {
        return item.isTextual() ? item.asText() : item.toString();
    }

    private String userPrompt(DietAdviceRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("User profile:\n");
        if (request.gender() != null) {
            sb.append("- Gender: ").append(request.gender()).append('\n');
        }
        if (request.age() != null) {
            sb.append("- Age: ").append(request.age()).append('\n');
        }
        if (request.heightCm() != null) {
            sb.append("- Height: ").append(request.heightCm()).append(" cm\n");
        }
        if (request.weightKg() != null) {
            sb.append("- Weight: ").append(request.weightKg()).append(" kg\n");
        }
        if (request.activity() != null) {
            String label = ACTIVITY_LABELS.getOrDefault(
                    request.activity().trim().toLowerCase(Locale.ROOT), request.activity());
            sb.append("- Activity level: ").append(label).append('\n');
        }

        String calculator = request.calculator().trim().toLowerCase(Locale.ROOT);
        if (calculator.equals("bmi")) {
            sb.append("Goal context: BMI calculator result.\n");
            if (request.bmi() != null) {
                sb.append("- BMI: ").append(request.bmi()).append('\n');
            }
            if (request.bmiCategory() != null) {
                sb.append("- BMI category: ").append(request.bmiCategory()).append('\n');
            }
        } else if (calculator.equals("calories")) {
            sb.append("Goal context: calorie calculator result.\n");
            if (request.bmr() != null) {
                sb.append("- Resting burn (BMR): ").append(request.bmr()).append(" kcal\n");
            }
            if (request.tdee() != null) {
                sb.append("- Daily maintenance calories (TDEE): ").append(request.tdee()).append(" kcal\n");
            }
        } else {
            sb.append("Goal context: ").append(request.calculator()).append('\n');
        }
        return sb.toString();
    }
}
