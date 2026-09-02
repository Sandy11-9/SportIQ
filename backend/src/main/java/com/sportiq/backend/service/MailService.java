package com.sportiq.backend.service;

import com.sportiq.backend.entity.AppUser;
import com.sportiq.backend.exception.MailSendException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends the 6-digit email verification code for new registrations
 * ({@code lld.md} §2.1). Uses spring-boot-starter-mail; properties come
 * from {@code spring.mail.*}.
 */
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String from;

    public MailService(JavaMailSender mailSender,
                       @org.springframework.beans.factory.annotation.Value("${spring.mail.from:noreply@localhost}")
                       String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendVerificationOtp(AppUser user, String otp, long ttlSeconds) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("SportIQ - Verify your email");
        message.setText("Hi " + user.getFullName() + ",\n\n"
                + "Your SportIQ verification code is: " + otp + "\n\n"
                + "This code expires in " + (ttlSeconds / 60) + " minutes.\n"
                + "If you did not create a SportIQ account, please ignore this email.\n\n"
                + "Thanks,\nThe SportIQ Team");
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new MailSendException("Could not send verification email to " + user.getEmail());
        }
    }
}
