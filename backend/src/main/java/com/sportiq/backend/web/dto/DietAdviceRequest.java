package com.sportiq.backend.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Calculator inputs sent by the BMI or Calorie page so the nutrition coach
 * can tailor a diet plan. Only the fields relevant to each calculator are
 * populated (BMI sends height/weight + result; calories sends the full
 * profile + BMR/TDEE).
 */
public record DietAdviceRequest(
        @NotBlank(message = "calculator is required")
        String calculator,
        String gender,
        Integer age,
        Double heightCm,
        Double weightKg,
        String activity,
        Double bmi,
        String bmiCategory,
        Double bmr,
        Double tdee) {
}
