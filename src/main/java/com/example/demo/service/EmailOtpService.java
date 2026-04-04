package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailOtpService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailOtpService.class);
    private final JavaMailSender mailSender;
    private final String senderAddress;
    private final String mailUsername;

    public EmailOtpService(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String senderAddress,
            @Value("${spring.mail.username:}") String mailUsername) {
        this.mailSender = mailSender;
        this.senderAddress = senderAddress;
        this.mailUsername = mailUsername;
    }

    public boolean sendOtpCode(String recipientEmail, String otpCode) {
        if (!StringUtils.hasText(senderAddress) || !StringUtils.hasText(mailUsername)) {
            return false;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(recipientEmail);
        message.setSubject("Votre code OTP CNI Management");
        message.setText("""
                Bonjour,

                Votre code OTP est : %s

                Ce code expire dans 5 minutes.

                Si vous n'etes pas a l'origine de cette demande, ignorez cet e-mail.
                """.formatted(otpCode));

        try {
            mailSender.send(message);
            return true;
        } catch (MailAuthenticationException ex) {
            LOGGER.warn("SMTP auth failed while sending OTP mail: {}", ex.getMessage());
            return false;
        } catch (MailException ex) {
            LOGGER.warn("SMTP send failed while sending OTP mail: {}", ex.getMessage());
            return false;
        }
    }

    public boolean sendPasswordResetCode(String recipientEmail, String otpCode) {
        if (!StringUtils.hasText(senderAddress) || !StringUtils.hasText(mailUsername)) {
            return false;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(recipientEmail);
        message.setSubject("Reinitialisation mot de passe CNI Management");
        message.setText("""
                Bonjour,

                Votre code de reinitialisation est : %s

                Ce code expire dans 5 minutes.
                """.formatted(otpCode));

        try {
            mailSender.send(message);
            return true;
        } catch (MailAuthenticationException ex) {
            LOGGER.warn("SMTP auth failed while sending password reset mail: {}", ex.getMessage());
            return false;
        } catch (MailException ex) {
            LOGGER.warn("SMTP send failed while sending password reset mail: {}", ex.getMessage());
            return false;
        }
    }
}
