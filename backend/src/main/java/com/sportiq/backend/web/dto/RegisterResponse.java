package com.sportiq.backend.web.dto;

import com.sportiq.backend.entity.AppUser;

/**
 * Registration confirmation for a <b>pending</b> account. No token is issued;
 * the account becomes usable only after the emailed OTP is confirmed.
 */
public record RegisterResponse(Long id, String email, String fullName, boolean verified) {

    public static RegisterResponse from(AppUser user) {
        return new RegisterResponse(user.getId(), user.getEmail(), user.getFullName(), user.isEmailVerified());
    }
}
