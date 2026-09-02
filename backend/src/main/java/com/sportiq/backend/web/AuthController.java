package com.sportiq.backend.web;

import com.sportiq.backend.service.AuthService;
import com.sportiq.backend.web.dto.AuthResponse;
import com.sportiq.backend.web.dto.LoginRequest;
import com.sportiq.backend.web.dto.RegisterRequest;
import com.sportiq.backend.web.dto.RegisterResponse;
import com.sportiq.backend.web.dto.ResendOtpRequest;
import com.sportiq.backend.web.dto.ResendOtpResponse;
import com.sportiq.backend.web.dto.VerifyEmailRequest;
import com.sportiq.backend.web.dto.VerifyEmailResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/verify-email")
    public VerifyEmailResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return authService.verifyEmail(request.email(), request.otp());
    }

    @PostMapping("/resend-otp")
    public ResendOtpResponse resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return authService.resendOtp(request.email());
    }
}