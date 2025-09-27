package it.odvsicilia.backend.service;

import it.odvsicilia.backend.config.BrevoConfig;
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
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.net.ConnectException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import jakarta.mail.SendFailedException;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import sibApi.TransactionalEmailsApi;
import sibModel.*;

import java.util.Arrays;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired
    private JavaMailSender javaMailSender;
    
    @Autowired(required = false)
    private BrevoConfig brevoConfig;
    
    @Autowired(required = false)
    private TransactionalEmailsApi transactionalEmailsApi;
    
    @Value("${brevo.api.key}")
    private String brevoApiKey;
    
    @Value("${brevo.sender.email}")
    private String senderEmail;
    
    @Value("${brevo.sender.name}")
    private String senderName;
    
    @Value("${brevo.admin.email}")
    private String adminEmail;
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Pattern templateParameterPattern;
    private final TransactionalEmailsApi transactionalEmailsApi;
    
    public EmailService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.templateParameterPattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
        this.transactionalEmailsApi = new TransactionalEmailsApi();
    }
    
    /**
     * Checks if Brevo integration is properly configured and enabled.
     * 
     * @return true if Brevo can be used, false if fallback to SMTP is needed
     */
    private boolean isBrevoEnabled() {
        return brevoConfig != null && 
               transactionalEmailsApi != null && 
               brevoConfig.getApiKey() != null && 
               !brevoConfig.getApiKey().trim().isEmpty();
    }
    
    public void setBrevoApiKey(String apiKey) {
        this.transactionalEmailsApi.getApiClient().setApiKey("api-key", apiKey);
    }
    
    private void sendEmailViaBrevoAPI(String toEmail, String toName, String subject, String htmlContent) {
        try {
            setBrevoApiKey(brevoApiKey);
            
            SendSmtpEmail emailRequest = new SendSmtpEmail();
            
            // Set sender
            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setEmail(senderEmail);
            sender.setName(senderName);
            emailRequest.setSender(sender);
            
            // Set recipient
            SendSmtpEmailTo recipient = new SendSmtpEmailTo();
            recipient.setEmail(toEmail);
            recipient.setName(toName);
            emailRequest.setTo(Arrays.asList(recipient));
            
            // Set subject and content
            emailRequest.setSubject(subject);
            emailRequest.setHtmlContent(htmlContent);
            
            // Send email
            CreateSmtpEmail result = transactionalEmailsApi.sendTransacEmail(emailRequest);
            
            // Validate response
            if (result.getMessageId() == null || result.getMessageId().isEmpty()) {
                throw new EmailDeliveryException("Errore invio email via Brevo API - Nessun messageId restituito", "BREVO_API_INVALID_RESPONSE");
            }
            
        } catch (ApiException e) {
            // Comprehensive error handling with status code mapping
            String errorMessage = "Errore invio email via Brevo API - Status: " + e.getCode();
            if (e.getResponseBody() != null && !e.getResponseBody().isEmpty()) {
                errorMessage += " - Response: " + e.getResponseBody();
            }
            
            switch (e.getCode()) {
                case 400: throw new EmailDeliveryException(errorMessage + " - Richiesta non valida", "BREVO_API_BAD_REQUEST", e);
                case 401: throw new EmailDeliveryException(errorMessage + " - API key non valida", "BREVO_API_UNAUTHORIZED", e);
                case 402: throw new EmailDeliveryException(errorMessage + " - Crediti insufficienti", "BREVO_API_INSUFFICIENT_CREDITS", e);
                case 403: throw new EmailDeliveryException(errorMessage + " - Accesso negato", "BREVO_API_FORBIDDEN", e);
                case 404: throw new EmailDeliveryException(errorMessage + " - Risorsa non trovata", "BREVO_API_NOT_FOUND", e);
                case 429: throw new EmailDeliveryException(errorMessage + " - Troppi tentativi", "BREVO_API_RATE_LIMIT", e);
                default:
                    if (e.getCode() >= 500) {
                        throw new EmailDeliveryException(errorMessage + " - Errore server Brevo", "BREVO_API_SERVER_ERROR", e);
                    } else {
                        throw new EmailDeliveryException(errorMessage + " - Errore client", "BREVO_API_CLIENT_ERROR", e);
                    }
            }
        } catch (Exception e) {
            throw new EmailDeliveryException("Errore imprevisto invio email via Brevo API: " + e.getMessage(), "BREVO_API_UNEXPECTED_ERROR", e);
        }
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
    
    private void sendEmailViaSMTP(String toEmail, String toName, String subject, String htmlContent) {
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
                                                    
        } catch (ConnectException e) {
            logger.error("SMTP connection failed. From: {} To: {} Subject: {}, Error: {}", 
                        senderEmail, toEmail, subject, e.getMessage(), e);
            throw new EmailSmtpException("Errore connessione server SMTP: " + e.getMessage(), 
                                       "SMTP_CONNECTION_FAILED", context, e);
                                       
        } catch (MessagingException e) {
            if (e.getMessage().toLowerCase().contains("timeout") || e.getMessage().toLowerCase().contains("timed out")) {
                logger.error("SMTP timeout occurred. From: {} To: {} Subject: {}, Error: {}", 
                            senderEmail, toEmail, subject, e.getMessage(), e);
                throw new EmailApiTimeoutException("Timeout durante invio email SMTP: " + e.getMessage(), 
                                                 "SMTP_TIMEOUT", context, e);
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
    
    private void sendEmail(Map<String, Object> emailData) {
        Map<String, Object> context = new HashMap<>();
        context.put("transport", "BREVO_API");
        context.put("apiUrl", "https://api.brevo.com/v3/smtp/email");
        
        // Extract recipient information for logging
        Object toData = emailData.get("to");
        String recipient = "unknown";
        String subject = emailData.get("subject") != null ? emailData.get("subject").toString() : "unknown";
        
        if (toData instanceof java.util.List) {
            java.util.List<?> toList = (java.util.List<?>) toData;
            if (!toList.isEmpty() && toList.get(0) instanceof Map) {
                Map<?, ?> firstRecipient = (Map<?, ?>) toList.get(0);
                recipient = firstRecipient.get("email") != null ? firstRecipient.get("email").toString() : "unknown";
            }
        }
        
        context.put("recipient", recipient);
        context.put("subject", subject);
        
        logger.debug("Attempting Brevo API email send. To: {} Subject: {}", recipient, subject);
        
        try {
            String jsonBody = objectMapper.writeValueAsString(emailData);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("accept", "application/json")
                    .header("api-key", brevoApiKey)
                    .header("content-type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 201) {
                logger.debug("Brevo API email sent successfully. To: {} Subject: {}", recipient, subject);
            } else if (response.statusCode() == 400) {
                logger.error("Brevo API bad request - invalid recipient or data. To: {} Subject: {}, Status: {}, Response: {}", 
                            recipient, subject, response.statusCode(), response.body());
                throw new EmailInvalidRecipientException("Errore invio email - dati non validi: " + response.body(), 
                                                        "BREVO_INVALID_DATA", context);
            } else if (response.statusCode() == 401) {
                logger.error("Brevo API authentication failed. To: {} Subject: {}, Status: {}, Response: {}", 
                            recipient, subject, response.statusCode(), response.body());
                throw new EmailAuthenticationException("Errore autenticazione API Brevo: " + response.body(), 
                                                     "BREVO_AUTH_FAILED", context);
            } else {
                logger.error("Brevo API error. To: {} Subject: {}, Status: {}, Response: {}", 
                            recipient, subject, response.statusCode(), response.body());
                throw new EmailDeliveryException("Errore invio email - Status: " + response.statusCode() + " - Response: " + response.body(), 
                                               "BREVO_API_ERROR", context);
            }
            
        } catch (HttpTimeoutException e) {
            logger.error("Brevo API timeout. To: {} Subject: {}, Error: {}", recipient, subject, e.getMessage(), e);
            throw new EmailApiTimeoutException("Timeout durante chiamata API Brevo: " + e.getMessage(), 
                                             "BREVO_API_TIMEOUT", context, e);
                                             
        } catch (ConnectException e) {
            logger.error("Brevo API connection failed. To: {} Subject: {}, Error: {}", recipient, subject, e.getMessage(), e);
            throw new EmailDeliveryException("Errore connessione API Brevo: " + e.getMessage(), 
                                           "BREVO_CONNECTION_FAILED", context, e);
                                           
        } catch (IOException e) {
            logger.error("Brevo API IO error. To: {} Subject: {}, Error: {}", recipient, subject, e.getMessage(), e);
            throw new EmailDeliveryException("Errore IO durante chiamata API Brevo: " + e.getMessage(), 
                                           "BREVO_IO_ERROR", context, e);
                                           
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Brevo API call interrupted. To: {} Subject: {}, Error: {}", recipient, subject, e.getMessage(), e);
            throw new EmailDeliveryException("Chiamata API Brevo interrotta: " + e.getMessage(), 
                                           "BREVO_INTERRUPTED", context, e);
                                           
        } catch (EmailDeliveryException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during Brevo API call. To: {} Subject: {}, Error: {}", 
                        recipient, subject, e.getMessage(), e);
            throw new EmailDeliveryException("Errore imprevisto durante chiamata API Brevo: " + e.getMessage(), 
                                           "BREVO_UNEXPECTED_ERROR", context, e);
        }
    }
    
    private Map<String, Object> buildBrevoEmailData(String toEmail, String toName, String subject, String htmlContent) {
        Map<String, Object> emailData = new HashMap<>();
        Map<String, Object> sender = new HashMap<>();
        sender.put("name", senderName);
        sender.put("email", senderEmail);
        
        Map<String, Object> recipient = new HashMap<>();
        recipient.put("email", toEmail);
        recipient.put("name", toName);
        
        emailData.put("sender", sender);
        emailData.put("to", List.of(recipient));
        emailData.put("subject", subject);
        emailData.put("htmlContent", htmlContent);
        
        return emailData;
    }
    
    private void logFallbackEvent(String toEmail, String emailType, String brevoFailureReason, String timestamp) {
        logger.info("[{}] FALLBACK_EVENT: {} email to {} switched from Brevo to SMTP due to {}", 
                   timestamp, emailType, toEmail, brevoFailureReason);
    }
    
    private String determineFailureReason(Exception e) {
        if (e instanceof ConnectException || e instanceof SocketTimeoutException) {
            return "NETWORK_CONNECTIVITY_ISSUE";
        } else if (e.getMessage() != null && e.getMessage().contains("429")) {
            return "RATE_LIMIT_EXCEEDED";
        } else if (e.getMessage() != null && e.getMessage().contains("401")) {
            return "AUTHENTICATION_FAILED";
        } else if (e.getMessage() != null && e.getMessage().contains("403")) {
            return "AUTHORIZATION_FAILED";
        } else if (e instanceof IOException) {
            return "IO_EXCEPTION";
        } else if (e instanceof InterruptedException) {
            return "REQUEST_INTERRUPTED";
        } else {
            return "UNKNOWN_ERROR: " + e.getClass().getSimpleName();
        }
    }
    
    /**
     * Sends an email using a Brevo template
     */
    private void sendEmailViaBrevoTemplate(Long templateId, String toEmail, String toName, 
                                         Map<String, Object> templateParams) throws IOException, InterruptedException {
        Map<String, Object> emailData = new HashMap<>();
        emailData.put("templateId", templateId);
        
        Map<String, Object> to = new HashMap<>();
        to.put("email", toEmail);
        to.put("name", toName);
        emailData.put("to", List.of(to));
        
        Map<String, Object> sender = new HashMap<>();
        sender.put("email", senderEmail);
        sender.put("name", senderName);
        emailData.put("sender", sender);
        
        // Remove system parameters and pass only template parameters
        Map<String, Object> cleanParams = new HashMap<>(templateParams);
        cleanParams.remove("templateId");
        cleanParams.remove("customerEmail"); // Remove as it's handled by 'to' field
        
        if (!cleanParams.isEmpty()) {
            emailData.put("params", cleanParams);
        }
        
        sendEmail(emailData);
    }
    
    /**
     * Checks if the request is for a Brevo template based on the presence of templateId
     */
    private boolean isBrevoTemplateRequest(Map<String, Object> templateParams) {
        return templateParams.containsKey("templateId") && templateParams.get("templateId") instanceof Long;
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
    
    /**
     * Sends email with template parameter processing for custom HTML templates
     */
    public void sendEmailWithTemplate(String toEmail, String toName, String subject, 
                                    String htmlTemplate, Map<String, Object> templateParams) {
        try {
            validateTemplateParameters(htmlTemplate, templateParams);
            String processedHtml = processTemplateParameters(htmlTemplate, templateParams);
            sendEmailViaSMTP(toEmail, toName, subject, processedHtml);
        } catch (MessagingException e) {
            throw new EmailDeliveryException("Errore nell'invio email con template personalizzato", "CUSTOM_TEMPLATE_EMAIL_FAILED", e);
        }
    
    private void logFallbackEvent(String toEmail, String emailType, String brevoFailureReason, String timestamp) {
        logger.warn("FALLBACK_EVENT: [{}] Email delivery fallback triggered - Type: {}, Recipient: {}, " +
                   "Brevo failure: {}, Fallback method: SMTP, Status: SUCCESS", 
                   timestamp, emailType, toEmail, brevoFailureReason);
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
                            per scoprire di più sui nostri prodotti di olio extravergine di oliva siciliano.
                        </p>
                    </div>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="https://odvsicilia.it" 
                           style="background-color: #f4a261; color: white; padding: 12px 24px; 
                                  text-decoration: none; border-radius: 5px; display: inline-block;">
                            Visita il nostro sito
                        </a>
                    </div>
                    
                    <div style="text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee;">
                        <p style="color: #666; font-size: 14px;">
                            Cordiali saluti,<br>
                            <strong>Il team di ODV Sicilia</strong>
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
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c5530; border-bottom: 2px solid #f4a261; padding-bottom: 10px;">
                        Nuovo Ordine Ricevuto
                    </h2>
                    
                    <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                        <h3 style="color: #2c5530; margin-top: 0;">Dettagli Ordine:</h3>
                        <p><strong>Numero Ordine:</strong> %s</p>
                        <p><strong>Cliente:</strong> %s</p>
                        <p><strong>Email:</strong> %s</p>
                        <p><strong>Totale:</strong> €%s</p>
                    </div>
                    
                    <div style="background-color: #fff; padding: 20px; border-left: 4px solid #f4a261; margin: 20px 0;">
                        <h4 style="color: #2c5530; margin-top: 0;">Prodotti Ordinati:</h4>
                        <div style="white-space: pre-wrap;">%s</div>
                    </div>
                    
                    <div style="text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee;">
                        <p style="color: #666; font-size: 14px;">
                            Accedi al pannello admin per gestire questo ordine
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """, orderNumber, customerName, customerEmail, totalAmount, items);
    }
    
    private String buildOrderConfirmationEmailHtml(String customerName, String orderNumber, 
                                                  String totalAmount, String items) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c5530; border-bottom: 2px solid #f4a261; padding-bottom: 10px;">
                        Conferma Ordine
                    </h2>
                    
                    <p>Caro/a <strong>%s</strong>,</p>
                    
                    <p>Grazie per il tuo ordine! Ecco i dettagli:</p>
                    
                    <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                        <h3 style="color: #2c5530; margin-top: 0;">Riepilogo Ordine:</h3>
                        <p><strong>Numero Ordine:</strong> %s</p>
                        <p><strong>Totale:</strong> €%s</p>
                    </div>
                    
                    <div style="background-color: #fff; padding: 20px; border-left: 4px solid #f4a261; margin: 20px 0;">
                        <h4 style="color: #2c5530; margin-top: 0;">Prodotti Ordinati:</h4>
                        <div style="white-space: pre-wrap;">%s</div>
                    </div>
                    
                    <div style="background-color: #e8f5e8; padding: 15px; border-radius: 8px; margin: 20px 0;">
                        <p style="margin: 0; color: #2c5530;">
                            <strong>Il tuo ordine è stato ricevuto e sarà processato a breve.</strong><br>
                            Ti invieremo un'altra email quando l'ordine sarà spedito.
                        </p>
                    </div>
                    
                    <div style="text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee;">
                        <p style="color: #666; font-size: 14px;">
                            Grazie per aver scelto ODV Sicilia!<br>
                            <strong>Il team di ODV Sicilia</strong>
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """, customerName, orderNumber, totalAmount, items);
    }
    
    private void validateEmailInputs(String customerName, String customerEmail, String subject) {
        Map<String, Object> context = new HashMap<>();
        context.put("customerName", customerName);
        context.put("customerEmail", customerEmail);
        context.put("subject", subject);
        
        if (customerName == null || customerName.trim().isEmpty()) {
            logger.warn("Email validation failed - missing customer name. CustomerEmail: {}, Subject: {}", customerEmail, subject);
            throw new EmailDeliveryException("Nome cliente è obbligatorio per l'invio email", "CUSTOMER_NAME_REQUIRED", context);
        }
        if (customerEmail == null || customerEmail.trim().isEmpty()) {
            logger.warn("Email validation failed - missing customer email. CustomerName: {}, Subject: {}", customerName, subject);
            throw new EmailDeliveryException("Email cliente è obbligatoria per l'invio email", "CUSTOMER_EMAIL_REQUIRED", context);
        }
        if (subject == null || subject.trim().isEmpty()) {
            logger.warn("Email validation failed - missing subject. CustomerName: {}, CustomerEmail: {}", customerName, customerEmail);
            throw new EmailDeliveryException("Oggetto è obbligatorio per l'invio email", "EMAIL_SUBJECT_REQUIRED", context);
        }
    }
    
    private void validateRecipientEmail(String email) {
        Map<String, Object> context = new HashMap<>();
        context.put("email", email);
        
        if (email == null || email.trim().isEmpty()) {
            logger.warn("Email validation failed - empty recipient email");
            throw new EmailInvalidRecipientException("Indirizzo email destinatario non può essere vuoto", 
                                                    "RECIPIENT_EMAIL_EMPTY", context);
        }
        
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException e) {
            logger.warn("Email validation failed - invalid recipient email format: {}, Error: {}", email, e.getMessage());
            throw new EmailInvalidRecipientException("Formato email destinatario non valido: " + e.getMessage(), 
                                                    "RECIPIENT_EMAIL_INVALID_FORMAT", context, e);
        }
    }
    
    private void validateOrderEmailInputs(String orderNumber, String customerName, String customerEmail, String totalAmount) {
        validateEmailInputs(customerName, customerEmail, orderNumber);
        
        Map<String, Object> context = new HashMap<>();
        context.put("orderNumber", orderNumber);
        context.put("customerName", customerName);
        context.put("customerEmail", customerEmail);
        context.put("totalAmount", totalAmount);
        
        if (totalAmount == null || totalAmount.trim().isEmpty()) {
            logger.warn("Order email validation failed - missing total amount. Order: {}, Customer: {}", orderNumber, customerName);
            throw new EmailDeliveryException("Totale ordine è obbligatorio per l'invio email", "ORDER_TOTAL_REQUIRED", context);
        }
    }
}
