package it.odvsicilia.backend.service;

import it.odvsicilia.backend.exception.EmailDeliveryException;
import it.odvsicilia.backend.exception.EmailSmtpException;
import it.odvsicilia.backend.exception.EmailAuthenticationException;
import it.odvsicilia.backend.exception.EmailApiTimeoutException;
import it.odvsicilia.backend.exception.EmailInvalidRecipientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.AuthenticationFailedException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.mail.SendFailedException;


import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

import java.util.Arrays;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired
    private JavaMailSender javaMailSender;
    
    @Value("${brevo.api.key}")
    private String brevoApiKey;
    
    @Value("${brevo.sender.email}")
    private String senderEmail;
    
    @Value("${brevo.sender.name}")
    private String senderName;
    
    @Value("${brevo.admin.email:ussofaraj@gmail.com}")
    private String adminEmail;
    
    private final Pattern templateParameterPattern;
    
    public EmailService() {
        this.templateParameterPattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    }
    

    
    public void sendContactNotificationToAdmin(String customerName, String customerEmail, 
                                             String subject, String message, String phone) {
        Map<String, Object> context = new HashMap<>();
        context.put("recipient", adminEmail);
        context.put("customerName", customerName);
        context.put("customerEmail", customerEmail);
        context.put("subject", "Nuovo messaggio di contatto - " + subject);
        context.put("operation", "sendContactNotificationToAdmin");
        
        logger.info("Starting email send for contact notification to admin. Recipient: {}, Customer: {}, Subject: {}", 
                   adminEmail, customerName, subject);
        
        try {
            validateEmailInputs(customerName, customerEmail, subject);
            validateRecipientEmail(adminEmail);
            
            String htmlContent = buildContactAdminEmailHtml(customerName, customerEmail, subject, message, phone);
            
            sendEmailViaSMTP(adminEmail, "Admin ODV Sicilia", "Nuovo messaggio di contatto - " + subject, htmlContent);
            
            logger.info("Successfully sent contact notification email to admin. Recipient: {}, Customer: {}, Subject: {}", 
                       adminEmail, customerName, subject);
                       
        } catch (EmailDeliveryException e) {
            logger.error("Failed to send contact notification email to admin. Recipient: {}, Customer: {}, Subject: {}, Error: {}", 
                        adminEmail, customerName, subject, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error sending contact notification email to admin. Recipient: {}, Customer: {}, Subject: {}, Error: {}", 
                        adminEmail, customerName, subject, e.getMessage(), e);
            throw new EmailDeliveryException("Errore imprevisto nell'invio email admin contatto: " + e.getMessage(), 
                                           "ADMIN_CONTACT_EMAIL_UNEXPECTED", context, e);
        }
    }
    
    public void sendContactConfirmationToCustomer(String customerName, String customerEmail, String subject) {
        Map<String, Object> context = new HashMap<>();
        context.put("recipient", customerEmail);
        context.put("customerName", customerName);
        context.put("subject", "Conferma ricezione messaggio - ODV Sicilia");
        context.put("operation", "sendContactConfirmationToCustomer");
        
        logger.info("Starting email send for contact confirmation to customer. Recipient: {}, Customer: {}, Subject: {}", 
                   customerEmail, customerName, subject);
        
        try {
            validateEmailInputs(customerName, customerEmail, subject);
            validateRecipientEmail(customerEmail);
            
            String htmlContent = buildContactConfirmationEmailHtml(customerName, subject);
            
            sendEmailViaSMTP(customerEmail, customerName, "Conferma ricezione messaggio - ODV Sicilia", htmlContent);
            
            logger.info("Successfully sent contact confirmation email to customer. Recipient: {}, Customer: {}, Subject: {}", 
                       customerEmail, customerName, subject);
                       
        } catch (EmailDeliveryException e) {
            logger.error("Failed to send contact confirmation email to customer. Recipient: {}, Customer: {}, Subject: {}, Error: {}", 
                        customerEmail, customerName, subject, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error sending contact confirmation email to customer. Recipient: {}, Customer: {}, Subject: {}, Error: {}", 
                        customerEmail, customerName, subject, e.getMessage(), e);
            throw new EmailDeliveryException("Errore imprevisto nell'invio email conferma contatto: " + e.getMessage(), 
                                           "CUSTOMER_CONTACT_CONFIRMATION_UNEXPECTED", context, e);
        }
    }
    
    public void sendOrderNotificationToAdmin(String orderNumber, String customerName, 
                                           String customerEmail, String totalAmount, String items) {
        Map<String, Object> context = new HashMap<>();
        context.put("recipient", adminEmail);
        context.put("customerName", customerName);
        context.put("customerEmail", customerEmail);
        context.put("orderNumber", orderNumber);
        context.put("totalAmount", totalAmount);
        context.put("subject", "Nuovo ordine ricevuto - " + orderNumber);
        context.put("operation", "sendOrderNotificationToAdmin");
        
        logger.info("Starting email send for order notification to admin. Recipient: {}, Order: {}, Customer: {}, Amount: €{}", 
                   adminEmail, orderNumber, customerName, totalAmount);
        
        try {
            validateOrderEmailInputs(orderNumber, customerName, customerEmail, totalAmount);
            validateRecipientEmail(adminEmail);
            
            String htmlContent = buildOrderAdminEmailHtml(orderNumber, customerName, customerEmail, totalAmount, items);
            
            sendEmailViaSMTP(adminEmail, "Admin ODV Sicilia", "Nuovo ordine ricevuto - " + orderNumber, htmlContent);
            
            logger.info("Successfully sent order notification email to admin. Recipient: {}, Order: {}, Customer: {}, Amount: €{}", 
                       adminEmail, orderNumber, customerName, totalAmount);
                       
        } catch (EmailDeliveryException e) {
            logger.error("Failed to send order notification email to admin. Recipient: {}, Order: {}, Customer: {}, Amount: €{}, Error: {}", 
                        adminEmail, orderNumber, customerName, totalAmount, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error sending order notification email to admin. Recipient: {}, Order: {}, Customer: {}, Amount: €{}, Error: {}", 
                        adminEmail, orderNumber, customerName, totalAmount, e.getMessage(), e);
            throw new EmailDeliveryException("Errore imprevisto nell'invio email admin ordine: " + e.getMessage(), 
                                           "ADMIN_ORDER_EMAIL_UNEXPECTED", context, e);
        }
    }
    
    public void sendOrderConfirmationToCustomer(String customerName, String customerEmail, 
                                              String orderNumber, String totalAmount, String items) {
        Map<String, Object> context = new HashMap<>();
        context.put("recipient", customerEmail);
        context.put("customerName", customerName);
        context.put("orderNumber", orderNumber);
        context.put("totalAmount", totalAmount);
        context.put("subject", "Conferma ordine " + orderNumber + " - ODV Sicilia");
        context.put("operation", "sendOrderConfirmationToCustomer");
        
        logger.info("Starting email send for order confirmation to customer. Recipient: {}, Order: {}, Customer: {}, Amount: €{}", 
                   customerEmail, orderNumber, customerName, totalAmount);
        
        try {
            validateOrderEmailInputs(orderNumber, customerName, customerEmail, totalAmount);
            validateRecipientEmail(customerEmail);
            
            String htmlContent = buildOrderConfirmationEmailHtml(customerName, orderNumber, totalAmount, items);
            
            sendEmailViaSMTP(customerEmail, customerName, "Conferma ordine " + orderNumber + " - ODV Sicilia", htmlContent);
            
            logger.info("Successfully sent order confirmation email to customer. Recipient: {}, Order: {}, Customer: {}, Amount: €{}", 
                       customerEmail, orderNumber, customerName, totalAmount);
                       
        } catch (EmailDeliveryException e) {
            logger.error("Failed to send order confirmation email to customer. Recipient: {}, Order: {}, Customer: {}, Amount: €{}, Error: {}", 
                        customerEmail, orderNumber, customerName, totalAmount, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error sending order confirmation email to customer. Recipient: {}, Order: {}, Customer: {}, Amount: €{}, Error: {}", 
                        customerEmail, orderNumber, customerName, totalAmount, e.getMessage(), e);
            throw new EmailDeliveryException("Errore imprevisto nell'invio email conferma ordine: " + e.getMessage(), 
                                           "CUSTOMER_ORDER_CONFIRMATION_UNEXPECTED", context, e);
        }
    }
    
    public void sendEmailViaSMTP(String toEmail, String toName, String subject, String htmlContent) {
        Map<String, Object> context = new HashMap<>();
        context.put("recipient", toEmail);
        context.put("recipientName", toName);
        context.put("subject", subject);
        context.put("senderEmail", senderEmail);
        context.put("transport", "SMTP");
        
        logger.debug("Attempting SMTP email send. From: {} To: {} Subject: {}", senderEmail, toEmail, subject);
        
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(senderEmail, senderName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            javaMailSender.send(message);
            
            logger.debug("SMTP email sent successfully. From: {} To: {} Subject: {}", senderEmail, toEmail, subject);
            
        } catch (AuthenticationFailedException e) {
            logger.error("SMTP authentication failed. From: {} To: {} Subject: {}, Error: {}", 
                        senderEmail, toEmail, subject, e.getMessage(), e);
            throw new EmailAuthenticationException("Errore autenticazione SMTP: " + e.getMessage(), 
                                                 "SMTP_AUTHENTICATION_FAILED", context, e);
                                                 
        } catch (SendFailedException e) {
            logger.error("SMTP send failed - invalid recipient or server rejection. From: {} To: {} Subject: {}, Error: {}", 
                        senderEmail, toEmail, subject, e.getMessage(), e);
            throw new EmailInvalidRecipientException("Errore invio SMTP - destinatario non valido o rifiutato: " + e.getMessage(), 
                                                    "SMTP_SEND_FAILED", context, e);
                                                    

        } catch (MessagingException e) {
            String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            
            if (errorMessage.contains("timeout") || errorMessage.contains("timed out")) {
                logger.error("SMTP timeout occurred. From: {} To: {} Subject: {}, Error: {}", 
                            senderEmail, toEmail, subject, e.getMessage(), e);
                throw new EmailApiTimeoutException("Timeout durante invio email SMTP: " + e.getMessage(), 
                                                 "SMTP_TIMEOUT", context, e);
            } else if (errorMessage.contains("connect") || errorMessage.contains("connection") || 
                      errorMessage.contains("refused") || errorMessage.contains("unreachable") ||
                      errorMessage.contains("host") || errorMessage.contains("network") ||
                      errorMessage.contains("failed") || errorMessage.contains("cannot connect")) {
                logger.error("SMTP connection failed. From: {} To: {} Subject: {}, Error: {}", 
                            senderEmail, toEmail, subject, e.getMessage(), e);
                throw new EmailSmtpException("Errore connessione server SMTP: " + e.getMessage(), 
                                           "SMTP_CONNECTION_FAILED", context, e);
            } else {
                logger.error("SMTP messaging error. From: {} To: {} Subject: {}, Error: {}", 
                            senderEmail, toEmail, subject, e.getMessage(), e);
                throw new EmailSmtpException("Errore invio email via SMTP: " + e.getMessage(), 
                                           "SMTP_MESSAGING_ERROR", context, e);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error during SMTP send. From: {} To: {} Subject: {}, Error: {}", 
                        senderEmail, toEmail, subject, e.getMessage(), e);
            throw new EmailSmtpException("Errore imprevisto invio email SMTP: " + e.getMessage(), 
                                       "SMTP_UNEXPECTED_ERROR", context, e);
        }
    }
    

    
    /**
     * Extracts template parameters from HTML content using {{parameter}} syntax
     */
    public Set<String> extractTemplateParameters(String htmlContent) {
        Set<String> parameters = new HashSet<>();
        Matcher matcher = templateParameterPattern.matcher(htmlContent);
        
        while (matcher.find()) {
            parameters.add(matcher.group(1).trim());
        }
        
        return parameters;
    }
    
    /**
     * Processes dynamic content placeholders in HTML templates
     */
    public String processTemplateParameters(String htmlContent, Map<String, Object> parameters) {
        if (htmlContent == null || parameters == null) {
            return htmlContent;
        }
        
        String processedContent = htmlContent;
        
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            processedContent = processedContent.replace(placeholder, value);
        }
        
        return processedContent;
    }
    
    /**
     * Validates that all required template parameters are provided
     */
    public void validateTemplateParameters(String htmlContent, Map<String, Object> parameters) {
        Set<String> requiredParams = extractTemplateParameters(htmlContent);
        List<String> missingParams = new ArrayList<>();
        
        for (String param : requiredParams) {
            if (!parameters.containsKey(param)) {
                missingParams.add(param);
            }
        }
        
        if (!missingParams.isEmpty()) {
            throw new EmailDeliveryException("Parametri template mancanti: " + String.join(", ", missingParams), "TEMPLATE_PARAMS_MISSING");
        }
    }
    
    private String buildContactAdminEmailHtml(String customerName, String customerEmail, 
                                            String subject, String message, String phone) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c5530; border-bottom: 2px solid #f4a261; padding-bottom: 10px;">
                        Nuovo Messaggio di Contatto
                    </h2>
                    
                    <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                        <h3 style="color: #2c5530; margin-top: 0;">Dettagli del Messaggio:</h3>
                        <p><strong>Nome:</strong> %s</p>
                        <p><strong>Email:</strong> %s</p>
                        <p><strong>Telefono:</strong> %s</p>
                        <p><strong>Oggetto:</strong> %s</p>
                    </div>
                    
                    <div style="background-color: #fff; padding: 20px; border-left: 4px solid #f4a261; margin: 20px 0;">
                        <h4 style="color: #2c5530; margin-top: 0;">Messaggio:</h4>
                        <p style="white-space: pre-wrap;">%s</p>
                    </div>
                    
                    <div style="text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee;">
                        <p style="color: #666; font-size: 14px;">
                            Questo messaggio è stato inviato dal sito web ODV Sicilia
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """, customerName, customerEmail, phone != null ? phone : "Non fornito", subject, message);
    }
    
    private String buildContactConfirmationEmailHtml(String customerName, String subject) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c5530; border-bottom: 2px solid #f4a261; padding-bottom: 10px;">
                        Grazie per averci contattato!
                    </h2>
                    
                    <p>Caro/a <strong>%s</strong>,</p>
                    
                    <p>Abbiamo ricevuto il tuo messaggio riguardo: <strong>%s</strong></p>
                    
                    <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                        <p style="margin: 0;">
                            Ti risponderemo il prima possibile. Nel frattempo, puoi visitare il nostro sito
                            per rimanere aggiornato sulle nostre attività.
                        </p>
                    </div>
                    
                    <div style="text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee;">
                        <p style="color: #666; font-size: 14px;">
                            Cordiali saluti,<br>
                            Il Team ODV Sicilia
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """, customerName, subject);
    }
    
    private String buildOrderAdminEmailHtml(String orderNumber, String customerName, 
                                          String customerEmail, String totalAmount, String items) {
        return String.format("""
