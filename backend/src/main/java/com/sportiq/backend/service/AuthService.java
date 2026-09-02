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
import com.sportiq.backend.web.dto.ResendOtpResponse;
import com.sportiq.backend.web.dto.UserResponse;
import com.sportiq.backend.web.dto.VerifyEmailResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final MailService mailService;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, OtpService otpService, MailService mailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.mailService = mailService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("Email already registered");
        }
        AppUser user = new AppUser(email, passwordEncoder.encode(request.password()), request.fullName().trim());
        user = userRepository.save(user);
        sendOtp(user);
        return RegisterResponse.from(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmail(normalize(request.email()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email not verified. Check your inbox for the verification code.");
        }
        return new AuthResponse(jwtService.generateToken(user.getId(), user.getEmail()), UserResponse.from(user));
    }

    @Transactional
    public VerifyEmailResponse verifyEmail(String email, String otp) {
        AppUser user = userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.isEmailVerified()) {
            return new VerifyEmailResponse(user.getId(), user.getEmail(), true);
        }
        String expected = user.getOtpHash();
        if (expected == null || user.getOtpExpiresAt() == null) {
            throw new OtpException("No verification code is active. Request a new one.");
        }
        if (Instant.now().isAfter(user.getOtpExpiresAt())) {
            throw new OtpException("Verification code expired. Request a new one.");
        }
        if (!expected.equals(otpService.hash(otp))) {
            throw new OtpException("Incorrect verification code.");
        }
        user.markVerified();
        userRepository.save(user);
        return new VerifyEmailResponse(user.getId(), user.getEmail(), true);
    }

    @Transactional
    public ResendOtpResponse resendOtp(String email) {
        AppUser user = userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.isEmailVerified()) {
            return new ResendOtpResponse("Email already verified. You can log in.");
        }
        if (user.getOtpExpiresAt() != null
                && otpService.resendCooldownSeconds() > 0) {
            long remainingSeconds = java.time.Duration.between(Instant.now(), user.getOtpExpiresAt()).getSeconds();
            if (remainingSeconds > otpService.ttlSeconds() - otpService.resendCooldownSeconds()) {
                throw new OtpException("Too many requests. Try again in a moment.");
            }
        }
        sendOtp(user);
        return new ResendOtpResponse("A new verification code has been sent to your email.");
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        return UserResponse.from(userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    private void sendOtp(AppUser user) {
        String otp = otpService.generate();
        user.setOtp(otpService.hash(otp), otpService.expiresAt());
        userRepository.save(user);
        mailService.sendVerificationOtp(user, otp, otpService.ttlSeconds());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
