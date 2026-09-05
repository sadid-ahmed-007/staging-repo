package com.eduauth.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    public void sendOtpEmail(String toEmail, String otp, String name) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Verify your email - EduAuth Registry");
            message.setText("Hello " + name + ",\n\nYour verification code is: " + otp + "\n\nThis code expires in 10 minutes.\n\nThank you,\nEduAuth Registry");
            
            mailSender.send(message);
            System.out.println("Email sent successfully to: " + toEmail);
        } catch (Exception e) {
            System.err.println("Failed to send OTP email: " + e.getMessage());
        }
    }

    public void sendApprovalEmail(String toEmail, String name) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Your EduAuth Registry account has been approved");
            message.setText("Hello " + name + ",\n\nYour account has been reviewed and approved by the administration. You can now log in to the system.\n\nThank you,\nEduAuth Registry");
            
            mailSender.send(message);
            System.out.println("Approval email sent successfully to: " + toEmail);
        } catch (Exception e) {
            System.err.println("Failed to send approval email: " + e.getMessage());
        }
    }
}
