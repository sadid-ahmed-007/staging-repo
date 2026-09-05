package com.eduauth.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfig {
    // Spring Boot auto-configures JavaMailSender from application.properties.
    // Custom beans (e.g., MimeMessageHelper wrappers) will be added in the
    // email / notification step.
}
