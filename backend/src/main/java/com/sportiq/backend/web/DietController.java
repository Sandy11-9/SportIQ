package com.sportiq.backend.web;

import com.sportiq.backend.service.DietService;
import com.sportiq.backend.web.dto.DietAdviceRequest;
import com.sportiq.backend.web.dto.DietAdviceResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Diet advice endpoint: {@code POST /api/diet-advice} with the BMI or Calorie
 * calculator inputs returns a structured {@link DietAdviceResponse}.
 */
@RestController
@RequestMapping("/api/diet-advice")
public class DietController {

    private final DietService dietService;

    public DietController(DietService dietService) {
        this.dietService = dietService;
    }

    @PostMapping
    public DietAdviceResponse advise(@Valid @RequestBody DietAdviceRequest request) {
        return dietService.advise(request);
    }
}
