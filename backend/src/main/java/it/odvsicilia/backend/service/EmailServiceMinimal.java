package it.odvsicilia.backend.service;

import it.odvsicilia.backend.exception.EmailDeliveryException;
import it.odvsicilia.backend.exception.EmailSmtpException;
import it.odvsicilia.backend.exception.EmailAuthenticationException;
import it.odvsicilia.backend.exception.EmailApiTimeoutException;
import it.odvsicilia.backend.exception.EmailInvalidRecipientException;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

/**
 * Minimal EmailService implementation for testing compilation and SMTP error handling
 * without Spring/Jakarta Mail dependencies
 */
public class EmailServiceMinimal {
    
    private String senderEmail = "test@odvsicilia.it";
    private String senderName = "ODV Sicilia Test";
    private String adminEmail = "admin@odvsicilia.it";
    
    private final Pattern templateParameterPattern;
    private final Pattern emailPattern;
    
    public EmailServiceMinimal() {
        this.templateParameterPattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
        this.emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    
    /**
     * Main SMTP sending method that demonstrates proper exception handling
     */
    public void sendEmailViaSMTP(String toEmail, String toName, String subject, String htmlContent) {
        Map<String, Object> context = new HashMap<>();
        context.put("recipient", toEmail);
        context.put("recipientName", toName);
        context.put("subject", subject);
        context.put("senderEmail", senderEmail);
        context.put("transport", "SMTP");
        
        try {
            // Validate recipient email
            validateRecipientEmail(toEmail);
            
            // Simulate SMTP connection and sending
            simulateSMTPSend(toEmail, toName, subject, htmlContent);
            
        } catch (ConnectException e) {
            // This catches connection-related SMTP errors as requested
            throw new EmailSmtpException("Errore connessione server SMTP: " + e.getMessage(), 
                                       "SMTP_CONNECTION_FAILED", context, e);
                                       
        } catch (RuntimeException e) {
            // Simulate various SMTP exceptions
            String message = e.getMessage().toLowerCase();
            
            if (message.contains("authentication")) {
                throw new EmailAuthenticationException("Errore autenticazione SMTP: " + e.getMessage(), 
                                                     "SMTP_AUTHENTICATION_FAILED", context, e);
            } else if (message.contains("timeout")) {
                throw new EmailApiTimeoutException("Timeout durante invio email SMTP: " + e.getMessage(), 
                                                 "SMTP_TIMEOUT", context, e);
            } else if (message.contains("invalid recipient")) {
                throw new EmailInvalidRecipientException("Errore invio SMTP - destinatario non valido: " + e.getMessage(), 
                                                        "SMTP_SEND_FAILED", context, e);
            } else {
                throw new EmailSmtpException("Errore invio email via SMTP: " + e.getMessage(), 
                                           "SMTP_MESSAGING_ERROR", context, e);
            }
            
        } catch (Exception e) {
            throw new EmailSmtpException("Errore imprevisto invio email SMTP: " + e.getMessage(), 
                                       "SMTP_UNEXPECTED_ERROR", context, e);
        }
    }
    
    /**
     * Simulate SMTP sending - throws various exceptions for testing
     */
    private void simulateSMTPSend(String toEmail, String toName, String subject, String htmlContent) throws ConnectException {
        // For testing purposes, simulate different error conditions based on email content
        if (toEmail.contains("connection-error")) {
            throw new ConnectException("SMTP server unavailable");
        }
        
        if (subject.contains("auth-error")) {
            throw new RuntimeException("Authentication failed");
        }
        
        if (subject.contains("timeout-error")) {
            throw new RuntimeException("Connection timed out");
        }
        
        if (toEmail.contains("invalid@")) {
            throw new RuntimeException("Invalid recipient");
        }
        
        // If we get here, the email "sent" successfully
        System.out.println("Email sent successfully to: " + toEmail);
    }
    
    /**
     * Contact notification method with proper exception handling
     */
    public void sendContactNotificationToAdmin(String customerName, String customerEmail, 
                                             String subject, String message, String phone) {
        Map<String, Object> context = new HashMap<>();
        context.put("recipient", adminEmail);
        context.put("customerName", customerName);
        context.put("customerEmail", customerEmail);
        context.put("subject", "Nuovo messaggio di contatto - " + subject);
        context.put("operation", "sendContactNotificationToAdmin");
        
        try {
            validateEmailInputs(customerName, customerEmail, subject);
            validateRecipientEmail(adminEmail);
            
            String htmlContent = buildContactAdminEmailHtml(customerName, customerEmail, subject, message, phone);
            
            sendEmailViaSMTP(adminEmail, "Admin ODV Sicilia", "Nuovo messaggio di contatto - " + subject, htmlContent);
            
        } catch (EmailDeliveryException e) {
            throw e;
        } catch (Exception e) {
            throw new EmailDeliveryException("Errore imprevisto nell'invio email admin contatto: " + e.getMessage(), 
                                           "ADMIN_CONTACT_EMAIL_UNEXPECTED", context, e);
        }
    }
    
    /**
     * Order notification method with proper exception handling
     */
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
        
        try {
            validateOrderEmailInputs(orderNumber, customerName, customerEmail, totalAmount);
            validateRecipientEmail(adminEmail);
            
            String htmlContent = buildOrderAdminEmailHtml(orderNumber, customerName, customerEmail, totalAmount, items);
            
            sendEmailViaSMTP(adminEmail, "Admin ODV Sicilia", "Nuovo ordine ricevuto - " + orderNumber, htmlContent);
            
        } catch (EmailDeliveryException e) {
            throw e;
        } catch (Exception e) {
            throw new EmailDeliveryException("Errore imprevisto nell'invio email admin ordine: " + e.getMessage(), 
                                           "ADMIN_ORDER_EMAIL_UNEXPECTED", context, e);
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
            <body>
                <h2>Nuovo Messaggio di Contatto</h2>
                <p><strong>Nome:</strong> %s</p>
                <p><strong>Email:</strong> %s</p>
                <p><strong>Telefono:</strong> %s</p>
                <p><strong>Oggetto:</strong> %s</p>
                <p><strong>Messaggio:</strong> %s</p>
            </body>
            </html>
            """, customerName, customerEmail, phone != null ? phone : "Non fornito", subject, message);
    }
    
    private String buildOrderAdminEmailHtml(String orderNumber, String customerName, 
                                          String customerEmail, String totalAmount, String items) {
        return String.format("""
            <html>
            <body>
                <h2>Nuovo Ordine Ricevuto</h2>
                <p><strong>Numero Ordine:</strong> %s</p>
                <p><strong>Cliente:</strong> %s (%s)</p>
                <p><strong>Importo:</strong> €%s</p>
                <p><strong>Articoli:</strong> %s</p>
            </body>
            </html>
            """, orderNumber, customerName, customerEmail, totalAmount, items != null ? items : "Non specificati");
    }
    
    private void validateEmailInputs(String customerName, String customerEmail, String subject) {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new EmailDeliveryException("Nome cliente richiesto", "CUSTOMER_NAME_REQUIRED");
        }
        if (customerEmail == null || customerEmail.trim().isEmpty()) {
            throw new EmailDeliveryException("Email cliente richiesta", "CUSTOMER_EMAIL_REQUIRED");
        }
        if (subject == null || subject.trim().isEmpty()) {
            throw new EmailDeliveryException("Oggetto richiesto", "SUBJECT_REQUIRED");
        }
    }
    
    private void validateOrderEmailInputs(String orderNumber, String customerName, String customerEmail, String totalAmount) {
        validateEmailInputs(customerName, customerEmail, "Order " + orderNumber);
        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            throw new EmailDeliveryException("Numero ordine richiesto", "ORDER_NUMBER_REQUIRED");
        }
        if (totalAmount == null || totalAmount.trim().isEmpty()) {
            throw new EmailDeliveryException("Importo totale richiesto", "TOTAL_AMOUNT_REQUIRED");
        }
    }
    
    private void validateRecipientEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new EmailInvalidRecipientException("Email destinatario non valida: vuota", "RECIPIENT_EMAIL_EMPTY");
        }
        
        if (!emailPattern.matcher(email).matches()) {
            throw new EmailInvalidRecipientException("Email destinatario non valida: " + email, "RECIPIENT_EMAIL_INVALID");
        }
    }
}