package com.sportiq.backend.web;

import com.sportiq.backend.exception.ResourceNotFoundException;
import com.sportiq.backend.security.JwtAuthenticationEntryPoint;
import com.sportiq.backend.security.JwtAuthenticationFilter;
import com.sportiq.backend.security.JwtService;
import com.sportiq.backend.security.SecurityConfig;
import com.sportiq.backend.service.AnalysisService;
import com.sportiq.backend.service.AuthService;
import com.sportiq.backend.service.ChatService;
import com.sportiq.backend.service.DietService;
import com.sportiq.backend.service.HistoryService;
import com.sportiq.backend.service.SportSkillService;
import com.sportiq.backend.web.dto.AnalysisSessionResponse;
import com.sportiq.backend.web.dto.AuthResponse;
import com.sportiq.backend.web.dto.DietAdviceResponse;
import com.sportiq.backend.web.dto.LoginRequest;
import com.sportiq.backend.web.dto.RegisterRequest;
import com.sportiq.backend.web.dto.RegisterResponse;
import com.sportiq.backend.web.dto.ScoreResponse;
import com.sportiq.backend.web.dto.SkillResponse;
import com.sportiq.backend.web.dto.SportResponse;
import com.sportiq.backend.web.dto.UserResponse;
import com.sportiq.backend.web.dto.VerifyEmailResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtService.class})
@ActiveProfiles("test")
class WebControllersTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private SportSkillService sportSkillService;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private DietService dietService;

    @MockitoBean
    private AnalysisService analysisService;

    @MockitoBean
    private HistoryService historyService;

    private String token() {
        return "Bearer " + jwtService.generateToken(7L, "u@sportiq.app");
    }

    @Test
    void registerReturnsCreatedWithPendingAccount() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new RegisterResponse(1L, "a@b.dev", "Coach", false));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@b.dev","password":"password123","fullName":"Coach"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("a@b.dev"))
                .andExpect(jsonPath("$.verified").value(false));
    }

    @Test
    void registerRejectsInvalidBody() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"x","fullName":""}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmailConfirmsAccount() throws Exception {
        when(authService.verifyEmail("a@b.dev", "123456"))
                .thenReturn(new VerifyEmailResponse(1L, "a@b.dev", true));

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@b.dev","otp":"123456"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void verifyEmailRejectsBadOtp() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@b.dev","otp":"12"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resendOtpSendsFreshCode() throws Exception {
        when(authService.resendOtp("a@b.dev"))
                .thenReturn(new com.sportiq.backend.web.dto.ResendOtpResponse("A new verification code has been sent."));

        mockMvc.perform(post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@b.dev"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("A new verification code has been sent."));
    }

    @Test
    void resendOtpRejectsBlankEmail() throws Exception {
        mockMvc.perform(post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":" "}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsToken() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("tok", new UserResponse(1L, "a@b.dev", "Coach")));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@b.dev","password":"password123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("tok"));
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        when(authService.getCurrentUser(7L)).thenReturn(new UserResponse(7L, "u@sportiq.app", "Coach"));

        mockMvc.perform(get("/api/users/me").header("Authorization", token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("u@sportiq.app"));
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsSports() throws Exception {
        when(sportSkillService.listSports()).thenReturn(List.of(new SportResponse(1, "Cricket")));

        mockMvc.perform(get("/api/sports").header("Authorization", token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Cricket"));
    }

    @Test
    void listsSkillsBySport() throws Exception {
        when(sportSkillService.listSkillsBySport(2)).thenReturn(List.of(new SkillResponse(3, 2, "Shooting")));

        mockMvc.perform(get("/api/sports/2/skills").header("Authorization", token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Shooting"));
    }

    @Test
    void listsHistoryWithFilters() throws Exception {
        AnalysisSessionResponse session = new AnalysisSessionResponse(
                10L, 2, "Football", 3, "Shooting", Instant.parse("2026-01-01T00:00:00Z"),
                List.of(new ScoreResponse("Power", 90)), List.of(), List.of(), List.of());
        when(historyService.list(anyLong(), anyInt(), anyInt(), any()))
                .thenReturn(new PageImpl<>(List.of(session)));

        mockMvc.perform(get("/api/analyses?page=0&size=20&sportId=2&skillId=3")
                        .header("Authorization", token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sportName").value("Football"))
                .andExpect(jsonPath("$.content[0].scores[0].score").value(90));
    }

    @Test
    void getSessionReturns404WhenNotFound() throws Exception {
        when(historyService.get(anyLong(), anyLong()))
                .thenThrow(new ResourceNotFoundException("Analysis session not found"));

        mockMvc.perform(get("/api/analyses/999").header("Authorization", token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void analysisEndpointRequiresMultipartParts() throws Exception {
        mockMvc.perform(post("/api/analyses")
                        .header("Authorization", token())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("sportId", "2")
                        .param("skillId", "3"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatReturnsReply() throws Exception {
        when(chatService.reply("How can I improve my cricket batting?"))
                .thenReturn("Keep your head still and stay balanced.");

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"How can I improve my cricket batting?"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Keep your head still and stay balanced."));
    }

    @Test
    void chatReturnsSportsOnlyRefusalForOffTopicQuestion() throws Exception {
        when(chatService.reply("What is Java?")).thenReturn(ChatService.NON_SPORT_REPLY);

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"What is Java?"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value(ChatService.NON_SPORT_REPLY));
    }

    @Test
    void chatRejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"   "}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatRejectsOversizedMessage() throws Exception {
        String message = "x".repeat(2001);
        mockMvc.perform(post("/api/chat")
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + message + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"How do I improve my serve?"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dietAdviceReturnsStructuredPlan() throws Exception {
        when(dietService.advise(any()))
                .thenReturn(new DietAdviceResponse(
                        "Aim to cut about 500 kcal daily.",
                        List.of("Eat more vegetables."),
                        List.of("Breakfast: oats with fruit."),
                        List.of("Walk 30 minutes daily.")));

        mockMvc.perform(post("/api/diet-advice")
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"calculator":"bmi","heightCm":175,"weightKg":85,"bmi":27.8,"bmiCategory":"Overweight"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Aim to cut about 500 kcal daily."))
                .andExpect(jsonPath("$.foodTips[0]").value("Eat more vegetables."))
                .andExpect(jsonPath("$.mealPlan[0]").value("Breakfast: oats with fruit."))
                .andExpect(jsonPath("$.lifestyleTips[0]").value("Walk 30 minutes daily."));
    }

    @Test
    void dietAdviceRejectsMissingCalculator() throws Exception {
        mockMvc.perform(post("/api/diet-advice")
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}