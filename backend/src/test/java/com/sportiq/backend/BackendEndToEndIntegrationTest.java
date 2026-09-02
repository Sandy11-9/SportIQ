package com.sportiq.backend;

import com.sportiq.backend.ai.AnalysisResult;
import com.sportiq.backend.ai.SpringAiOllamaService;
import com.sportiq.backend.entity.Skill;
import com.sportiq.backend.service.MailService;
import com.sportiq.backend.service.OtpService;
import com.sportiq.backend.support.PostgresTestBase;
import com.sportiq.backend.support.TestMediaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendEndToEndIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpringAiOllamaService ollamaService;

    @MockitoBean
    private MailService mailService;

    @MockitoBean
    private OtpService otpService;

    @BeforeEach
    void stubOtp() {
        when(otpService.generate()).thenReturn("123456");
        when(otpService.hash(any())).thenReturn("stubbed-otp-hash");
        when(otpService.ttlSeconds()).thenReturn(600L);
        when(otpService.expiresAt()).thenReturn(Instant.now().plusSeconds(600));
        when(otpService.resendCooldownSeconds()).thenReturn(30L);
    }

    private String uniqueEmail() {
        return "coach+" + UUID.randomUUID() + "@sportiq.app";
    }

    private String registerAndGetToken(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123","fullName":"Coach"}""".formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verified").value(false));

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","otp":"123456"}""".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}""".formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    private void stubAiPipeline() {
        when(ollamaService.describeFrames(any(Skill.class), anyList()))
                .thenReturn(List.of("o1", "o2", "o3", "o4", "o5", "o6", "o7", "o8", "o9", "o10"));
        when(ollamaService.aggregate(any(Skill.class), anyList(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyDouble()))
                .thenReturn(new AnalysisResult(
                List.of(new AnalysisResult.ScoreEntry("Placement", 82), new AnalysisResult.ScoreEntry("Power", 70)),
                List.of("clean strike"),
                List.of("plant foot wide"),
                List.of("keep head down")));
    }

    @Test
    void registerLoginAndBrowseCatalog() throws Exception {
        String email = uniqueEmail();
        String token = registerAndGetToken(email);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}""".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        mockMvc.perform(get("/api/sports").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Cricket")))
                .andExpect(jsonPath("$[*].name", hasItem("Football")));

        mockMvc.perform(get("/api/sports/2/skills").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Shooting")));

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void duplicateRegistrationIsConflict() throws Exception {
        String email = uniqueEmail();
        registerAndGetToken(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123","fullName":"Coach"}""".formatted(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void protectedEndpointsRejectAnonymous() throws Exception {
        mockMvc.perform(get("/api/sports")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/analyses")).andExpect(status().isUnauthorized());
    }

    @Test
    void uploadAnalysisEndToEnd() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        stubAiPipeline();

        byte[] video = TestMediaUtil.videoBytes(6);
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", video);

        String created = mockMvc.perform(multipart("/api/analyses")
                        .file(file)
                        .param("sportId", "2")
                        .param("skillId", "3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sportName").value("Football"))
                .andExpect(jsonPath("$.skillName").value("Shooting"))
                .andExpect(jsonPath("$.scores[0].category").value("Placement"))
                .andExpect(jsonPath("$.scores[0].score").value(82))
                .andExpect(jsonPath("$.goodPoints[0]").value("clean strike"))
                .andReturn().getResponse().getContentAsString();

        String id = created.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1");

        mockMvc.perform(get("/api/analyses?page=0&size=10").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(Integer.parseInt(id)))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/analyses/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skillName").value("Shooting"));
    }

    @Test
    void uploadTooShortVideoIsUnprocessable() throws Exception {
        String token = registerAndGetToken(uniqueEmail());

        MockMultipartFile file = new MockMultipartFile("file", "short.mp4", "video/mp4",
                TestMediaUtil.videoBytes(2));

        mockMvc.perform(multipart("/api/analyses")
                        .file(file)
                        .param("sportId", "2")
                        .param("skillId", "3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void unknownSkillIsNotFound() throws Exception {
        String token = registerAndGetToken(uniqueEmail());

        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4",
                TestMediaUtil.videoBytes(6));

        mockMvc.perform(multipart("/api/analyses")
                        .file(file)
                        .param("sportId", "2")
                        .param("skillId", "99")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void analysisWithoutVideoIsBadRequest() throws Exception {
        String token = registerAndGetToken(uniqueEmail());

        mockMvc.perform(multipart("/api/analyses")
                        .param("sportId", "2")
                        .param("skillId", "3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}