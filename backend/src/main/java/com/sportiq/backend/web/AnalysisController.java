package com.sportiq.backend.web;

import com.sportiq.backend.security.SecurityUtils;
import com.sportiq.backend.service.AnalysisService;
import com.sportiq.backend.service.HistoryService;
import com.sportiq.backend.web.dto.AnalysisSessionResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequestMapping("/api/analyses")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final HistoryService historyService;

    public AnalysisController(AnalysisService analysisService, HistoryService historyService) {
        this.analysisService = analysisService;
        this.historyService = historyService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalysisSessionResponse> analyze(
            @RequestPart("file") MultipartFile file,
            @RequestParam @NotNull @Min(1) Integer sportId,
            @RequestParam @NotNull @Min(1) Integer skillId) {
        AnalysisSessionResponse response =
                analysisService.analyze(SecurityUtils.currentUserId(), sportId, skillId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public Page<AnalysisSessionResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer sportId,
            @RequestParam(required = false) Integer skillId) {
        return historyService.list(SecurityUtils.currentUserId(), sportId, skillId, PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public AnalysisSessionResponse get(@PathVariable @NotNull Long id) {
        return historyService.get(SecurityUtils.currentUserId(), id);
    }
}