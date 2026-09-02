package com.sportiq.backend.service;

import com.sportiq.backend.exception.ChatUnavailableException;
import com.sportiq.backend.web.dto.DietAdviceRequest;
import com.sportiq.backend.web.dto.DietAdviceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DietServiceTest {

    private ChatClient chat;
    private ChatClient.ChatClientRequestSpec spec;
    private ChatClient.CallResponseSpec call;
    private DietService service;

    @BeforeEach
    void setUp() {
        chat = mock(ChatClient.class);
        spec = mock(ChatClient.ChatClientRequestSpec.class);
        call = mock(ChatClient.CallResponseSpec.class);

        when(chat.prompt()).thenReturn(spec);
        when(spec.system(anyString())).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(call);

        service = new DietService(chat, new ObjectMapper());
    }

    @Test
    void returnsStructuredPlanForBmiInputs() {
        when(call.content()).thenReturn("""
                {"summary":"Aim to cut about 500 kcal daily.","foodTips":["Eat more vegetables.","Prioritise lean protein."],"mealPlan":["Breakfast: oats with fruit."],"lifestyleTips":["Walk 30 minutes daily."]}""");

        DietAdviceRequest request = new DietAdviceRequest(
                "bmi", null, null, 175.0, 85.0, null, 27.8, "Overweight", null, null);

        DietAdviceResponse response = service.advise(request);
        assertEquals("Aim to cut about 500 kcal daily.", response.summary());
        assertEquals(List.of("Eat more vegetables.", "Prioritise lean protein."), response.foodTips());
        assertEquals(List.of("Breakfast: oats with fruit."), response.mealPlan());
        assertEquals(List.of("Walk 30 minutes daily."), response.lifestyleTips());
        verify(chat).prompt();
    }

    @Test
    void returnsStructuredPlanForCalorieInputs() {
        when(call.content()).thenReturn("""
                {"summary":"Maintain around 2500 kcal daily.","foodTips":["Balance macros each meal."],"mealPlan":["Lunch: grilled chicken, rice, salad."],"lifestyleTips":["Sleep 7-8 hours."]}""");

        DietAdviceRequest request = new DietAdviceRequest(
                "calories", "male", 22, 175.0, 70.0, "1.55", null, null, 1690.0, 2620.0);

        DietAdviceResponse response = service.advise(request);
        assertEquals("Maintain around 2500 kcal daily.", response.summary());
        assertEquals(List.of("Lunch: grilled chicken, rice, salad."), response.mealPlan());
    }

    @Test
    void handlesMarkdownFencedJson() {
        when(call.content()).thenReturn("""
                ```json
                {"summary":"Cut 500 kcal.","foodTips":["Veggies."],"mealPlan":["Oats."],"lifestyleTips":["Walk."]}
                ```""");

        DietAdviceResponse response = service.advise(
                new DietAdviceRequest("bmi", null, null, 175.0, 85.0, null, 27.8, "Overweight", null, null));
        assertEquals("Cut 500 kcal.", response.summary());
        assertEquals(List.of("Veggies."), response.foodTips());
    }

    @Test
    void toleratesStringMealPlanInsteadOfArray() {
        when(call.content()).thenReturn("""
                {"summary":"Plan.","foodTips":["Veggies."],"mealPlan":"Breakfast: oats.\\nLunch: rice.","lifestyleTips":["Walk."]}""");

        DietAdviceResponse response = service.advise(
                new DietAdviceRequest("bmi", null, null, 175.0, 85.0, null, 27.8, "Overweight", null, null));
        assertEquals(List.of("Breakfast: oats.\nLunch: rice."), response.mealPlan());
    }

    @Test
    void flattensObjectMealPlanIntoLines() {
        when(call.content()).thenReturn("""
                {"summary":"Plan.","foodTips":["Veggies."],"mealPlan":{"breakfast":"Oatmeal (1 cup)","lunch":"Grilled chicken (150g)","dinner":"Baked salmon (200g)","snacks":["Greek yogurt","Carrot sticks"]},"lifestyleTips":["Walk."]}""");

        DietAdviceResponse response = service.advise(
                new DietAdviceRequest("bmi", null, null, 175.0, 85.0, null, 27.8, "Overweight", null, null));
        assertEquals(List.of(
                "Breakfast: Oatmeal (1 cup)",
                "Lunch: Grilled chicken (150g)",
                "Dinner: Baked salmon (200g)",
                "Snacks: Greek yogurt; Carrot sticks"), response.mealPlan());
    }

    @Test
    void extractsJsonObjectFromSurroundingProse() {
        when(call.content()).thenReturn("""
                Here is your plan:
                {"summary":"Plan.","foodTips":["Veggies."],"mealPlan":["Oats."],"lifestyleTips":["Walk."]}
                Hope that helps!""");

        DietAdviceResponse response = service.advise(
                new DietAdviceRequest("bmi", null, null, 175.0, 85.0, null, 27.8, "Overweight", null, null));
        assertEquals("Plan.", response.summary());
        assertEquals(List.of("Oats."), response.mealPlan());
    }

    @Test
    void handlesJsonWithBracesInsideStringValues() {
        when(call.content()).thenReturn("""
                {"summary":"Plan.","foodTips":["Veggies."],"mealPlan":["Use {a} template for your {meals}"],"lifestyleTips":["Walk."]}""");

        DietAdviceResponse response = service.advise(
                new DietAdviceRequest("bmi", null, null, 175.0, 85.0, null, 27.8, "Overweight", null, null));
        assertEquals(List.of("Use {a} template for your {meals}"), response.mealPlan());
    }

    @Test
    void flattensArrayOfObjectMealsIntoLines() {
        when(call.content()).thenReturn("""
                {"summary":"Plan.","foodTips":["Veggies."],"mealPlan":[{"breakfast":"Oatmeal with berries (1 cup)"},{"lunch":"Grilled chicken salad (200g)"},{"dinner":"Baked salmon (150g)"},{"snacks":"Greek yogurt, apple slices"}],"lifestyleTips":["Walk."]}""");

        DietAdviceResponse response = service.advise(
                new DietAdviceRequest("bmi", null, null, 175.0, 85.0, null, 27.8, "Overweight", null, null));
        assertEquals(List.of(
                "Breakfast: Oatmeal with berries (1 cup)",
                "Lunch: Grilled chicken salad (200g)",
                "Dinner: Baked salmon (150g)",
                "Snacks: Greek yogurt, apple slices"), response.mealPlan());
    }

    @Test
    void toleratesMissingSections() {
        when(call.content()).thenReturn("{\"summary\":\"Plan.\"}");

        DietAdviceResponse response = service.advise(
                new DietAdviceRequest("bmi", null, null, 175.0, 85.0, null, 27.8, "Overweight", null, null));
        assertEquals("Plan.", response.summary());
        assertEquals(List.of(), response.foodTips());
        assertEquals(List.of(), response.mealPlan());
        assertEquals(List.of(), response.lifestyleTips());
    }

    @Test
    void rejectsMissingCalculator() {
        DietAdviceRequest request = new DietAdviceRequest(
                null, null, null, null, null, null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.advise(request));
        verify(chat, never()).prompt();
    }

    @Test
    void mapsModelFailureToUnavailable() {
        when(call.content()).thenThrow(new RuntimeException("connection refused"));

        DietAdviceRequest request = new DietAdviceRequest(
                "bmi", null, null, 175.0, 85.0, null, 27.8, "Overweight", null, null);

        assertThrows(ChatUnavailableException.class, () -> service.advise(request));
    }
}