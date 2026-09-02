package com.sportiq.backend.web;

import com.sportiq.backend.security.SecurityUtils;
import com.sportiq.backend.service.AuthService;
import com.sportiq.backend.web.dto.UserResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public UserResponse me() {
        return authService.getCurrentUser(SecurityUtils.currentUserId());
    }
}