package com.sportiq.backend.web.dto;

import java.util.List;

/**
 * Structured diet &amp; lifestyle plan produced by the reasoning model for the
 * BMI or Calorie page.
 */
public record DietAdviceResponse(
        String summary,
        List<String> foodTips,
        List<String> mealPlan,
        List<String> lifestyleTips) {
}
