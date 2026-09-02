package com.sportiq.backend.service;

import com.sportiq.backend.entity.AppUser;
import com.sportiq.backend.exception.DuplicateEmailException;
import com.sportiq.backend.exception.EmailNotVerifiedException;
import com.sportiq.backend.exception.InvalidCredentialsException;
import com.sportiq.backend.exception.OtpException;
import com.sportiq.backend.exception.ResourceNotFoundException;
import com.sportiq.backend.repository.AppUserRepository;
import com.sportiq.backend.security.JwtService;
import com.sportiq.backend.web.dto.AuthResponse;
import com.sportiq.backend.web.dto.LoginRequest;
import com.sportiq.backend.web.dto.RegisterRequest;
import com.sportiq.backend.web.dto.RegisterResponse;
import com.sportiq.backend.web.dto.UserResponse;
import com.sportiq.backend.web.dto.VerifyEmailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private AppUserRepository userRepository;
    private JwtService jwtService;
    private OtpService otpService;
    private MailService mailService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(AppUserRepository.class);
        jwtService = mock(JwtService.class);
        otpService = mock(OtpService.class);
        mailService = mock(MailService.class);
        authService = new AuthService(userRepository, new BCryptPasswordEncoder(), jwtService, otpService, mailService);
        when(jwtService.generateToken(any(), any())).thenReturn("token-abc");
        when(otpService.generate()).thenReturn("123456");
        when(otpService.hash("123456")).thenReturn("otp-hash");
        when(otpService.ttlSeconds()).thenReturn(600L);
        when(otpService.expiresAt()).thenReturn(Instant.now().plusSeconds(600));
        when(otpService.resendCooldownSeconds()).thenReturn(30L);
    }

    @Test
    void registersPendingAccountAndSendsOtp() {
        when(userRepository.existsByEmail("a@b.dev")).thenReturn(false);
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterResponse response = authService.register(new RegisterRequest(" A@b.dev ", "password123", "  Coach  "));

        assertEquals("a@b.dev", response.email());
        assertEquals("Coach", response.fullName());
        assertFalse(response.verified());
        verify(mailService).sendVerificationOtp(any(AppUser.class), any(String.class), any(Long.class));

        org.mockito.ArgumentCaptor<AppUser> captor = org.mockito.ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        AppUser saved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertTrue(saved.getPasswordHash().startsWith("$2"));
        assertEquals("otp-hash", saved.getOtpHash());
    }

    @Test
    void rejectsDuplicateEmail() {
        when(userRepository.existsByEmail("a@b.dev")).thenReturn(true);

        assertThrows(DuplicateEmailException.class,
                () -> authService.register(new RegisterRequest("a@b.dev", "password123", "Coach")));
    }

    @Test
    void logsInOnlyAfterEmailVerified() {
        AppUser user = new AppUser("a@b.dev", new BCryptPasswordEncoder().encode("password123"), "Coach");
        user.markVerified();
        when(userRepository.findByEmail("a@b.dev")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(new LoginRequest("a@b.dev", "password123"));

        assertEquals("token-abc", response.token());
        assertEquals("a@b.dev", response.user().email());
    }

    @Test
    void rejectsLoginBeforeEmailVerified() {
        AppUser user = new AppUser("a@b.dev", new BCryptPasswordEncoder().encode("password123"), "Coach");
        user.setOtp("otp-hash", Instant.now().plusSeconds(600));
        when(userRepository.findByEmail("a@b.dev")).thenReturn(Optional.of(user));

        assertThrows(EmailNotVerifiedException.class,
                () -> authService.login(new LoginRequest("a@b.dev", "password123")));
    }

    @Test
    void rejectsUnknownEmail() {
        when(userRepository.findByEmail("nope@b.dev")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("nope@b.dev", "password123")));
    }

    @Test
    void rejectsWrongPassword() {
        AppUser user = new AppUser("a@b.dev", new BCryptPasswordEncoder().encode("right-pass"), "Coach");
        user.markVerified();
        when(userRepository.findByEmail("a@b.dev")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("a@b.dev", "wrong-pass")));
    }

    @Test
    void verifiesEmailWithCorrectOtp() {
        AppUser user = new AppUser("a@b.dev", "h", "Coach");
        user.setOtp("otp-hash", Instant.now().plusSeconds(600));
        when(userRepository.findByEmail("a@b.dev")).thenReturn(Optional.of(user));
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        VerifyEmailResponse response = authService.verifyEmail("a@b.dev", "123456");

        assertTrue(response.verified());
        assertTrue(user.isEmailVerified());
        assertEquals(null, user.getOtpHash());
    }

    @Test
    void rejectsWrongOtp() {
        when(otpService.hash("000000")).thenReturn("wrong-hash");
        AppUser user = new AppUser("a@b.dev", "h", "Coach");
        user.setOtp("otp-hash", Instant.now().plusSeconds(600));
        when(userRepository.findByEmail("a@b.dev")).thenReturn(Optional.of(user));

        assertThrows(OtpException.class, () -> authService.verifyEmail("a@b.dev", "000000"));
    }

    @Test
    void rejectsExpiredOtp() {
        AppUser user = new AppUser("a@b.dev", "h", "Coach");
        user.setOtp("otp-hash", Instant.now().minusSeconds(10));
        when(userRepository.findByEmail("a@b.dev")).thenReturn(Optional.of(user));

        assertThrows(OtpException.class, () -> authService.verifyEmail("a@b.dev", "123456"));
    }

    @Test
    void resendOtpGeneratesFreshCode() {
        AppUser user = new AppUser("a@b.dev", "h", "Coach");
        user.setOtp("old-hash", Instant.now().plusSeconds(120));
        when(userRepository.findByEmail("a@b.dev")).thenReturn(Optional.of(user));
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.resendOtp("a@b.dev");

        verify(mailService).sendVerificationOtp(any(AppUser.class), any(String.class), any(Long.class));
        assertEquals("otp-hash", user.getOtpHash());
    }

    @Test
    void resendOtpHonoursCooldown() {
        AppUser user = new AppUser("a@b.dev", "h", "Coach");
        user.setOtp("otp-hash", Instant.now().plusSeconds(590));
        when(userRepository.findByEmail("a@b.dev")).thenReturn(Optional.of(user));

        assertThrows(OtpException.class, () -> authService.resendOtp("a@b.dev"));
    }

    @Test
    void returnsCurrentUser() {
        when(userRepository.findById(9L)).thenReturn(Optional.of(new AppUser("a@b.dev", "h", "Coach")));

        UserResponse response = authService.getCurrentUser(9L);

        assertEquals("a@b.dev", response.email());
        assertEquals("Coach", response.fullName());
    }

    @Test
    void throwsWhenUserMissing() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.getCurrentUser(9L));
    }
}
