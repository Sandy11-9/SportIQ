package com.sportiq.backend.web;

import com.sportiq.backend.exception.ChatUnavailableException;
import com.sportiq.backend.exception.DuplicateEmailException;
import com.sportiq.backend.exception.InvalidCredentialsException;
import com.sportiq.backend.exception.LlmAnalysisException;
import com.sportiq.backend.exception.ResourceNotFoundException;
import com.sportiq.backend.exception.VideoValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void videoValidationIs422() throws Exception {
        mockMvc.perform(get("/video"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Video must be 5-20 seconds"));
    }

    @Test
    void llmAnalysisIs422() throws Exception {
        mockMvc.perform(get("/llm"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void badCredentialsIs401() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void missingResourceIs404() throws Exception {
        mockMvc.perform(get("/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void duplicateEmailIs409() throws Exception {
        mockMvc.perform(get("/dupe"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void chatUnavailableIs503WithCleanMessage() throws Exception {
        mockMvc.perform(get("/chat-unavailable"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message")
                        .value("SportIQ Assistant is temporarily unavailable. Please try again."));
    }

    @Test
    void invalidArgumentIs400() throws Exception {
        mockMvc.perform(get("/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please enter a sports question."));
    }

    @Test
    void oversizedUploadIs413() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/upload").file(file))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.message").value(containsString("exceeds")));
    }

    @Test
    void genericExceptionIs500() throws Exception {
        mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Controller
    static class ThrowingController {

        @GetMapping("/video")
        public void video() {
            throw new VideoValidationException("Video must be 5-20 seconds");
        }

        @GetMapping("/llm")
        public void llm() {
            throw new LlmAnalysisException("aggregation failed");
        }

        @GetMapping("/login")
        public void login() {
            throw new InvalidCredentialsException("bad credentials");
        }

        @GetMapping("/missing")
        public void missing() {
            throw new ResourceNotFoundException("User not found");
        }

        @GetMapping("/dupe")
        public void dupe() {
            throw new DuplicateEmailException("email already registered");
        }

        @GetMapping("/chat-unavailable")
        public void chatUnavailable() {
            throw new ChatUnavailableException("ollama connection refused: some deep internal detail");
        }

        @GetMapping("/invalid")
        public void invalid() {
            throw new IllegalArgumentException("Please enter a sports question.");
        }

        @GetMapping("/boom")
        public void boom() {
            throw new IllegalStateException("kaboom");
        }

        @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public void upload(@RequestPart("file") MultipartFile file) {
            throw new MaxUploadSizeExceededException(10_000_000);
        }
    }
}