package com.sportiq.backend.web.dto;

import com.sportiq.backend.entity.AppUser;

public record UserResponse(Long id, String email, String fullName) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName());
    }
}