package it.odvsicilia.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Properties;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${smtp.host}")
    private String smtpHost;
    
    @Value("${smtp.port}")
    private int smtpPort;
    
    @Value("${smtp.username}")
    private String smtpUsername;
    
    @Value("${smtp.password}")
    private String smtpPassword;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://odvsicilia.it", "http://localhost:3000", "http://127.0.0.1:5500")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
    
    @Bean
public JavaMailSender javaMailSender() {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    
    // Basic SMTP configuration
    mailSender.setHost(smtpHost);
    mailSender.setPort(smtpPort);
    mailSender.setUsername(smtpUsername);
    mailSender.setPassword(smtpPassword);
    
    // SMTP properties for Brevo
    Properties props = mailSender.getJavaMailProperties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.starttls.required", "true");
    props.put("mail.smtp.ssl.trust", smtpHost);
    props.put("mail.smtp.connectiontimeout", "5000");
    props.put("mail.smtp.timeout", "5000");
    props.put("mail.smtp.writetimeout", "5000");
    props.put("mail.debug", "false");
    
    return mailSender;
}
}