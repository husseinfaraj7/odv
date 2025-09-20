package it.odvsicilia.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EmailService {
    
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
    
    public EmailService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    public void sendContactNotificationToAdmin(String customerName, String customerEmail, 
                                             String subject, String message, String phone) {
        try {
            String htmlContent = buildContactAdminEmailHtml(customerName, customerEmail, subject, message, phone);
            
            Map<String, Object> emailData = Map.of(
                "sender", Map.of("name", senderName, "email", senderEmail),
                "to", new Object[]{Map.of("email", adminEmail, "name", "Admin ODV Sicilia")},
                "subject", "Nuovo messaggio di contatto - " + subject,
                "htmlContent", htmlContent
            );
            
            sendEmail(emailData);
        } catch (Exception e) {
            System.err.println("Errore nell'invio email admin contatto: " + e.getMessage());
        }
    }
    
    public void sendContactConfirmationToCustomer(String customerName, String customerEmail, String subject) {
        try {
            String htmlContent = buildContactConfirmationEmailHtml(customerName, subject);
            
            Map<String, Object> emailData = Map.of(
                "sender", Map.of("name", senderName, "email", senderEmail),
                "to", new Object[]{Map.of("email", customerEmail, "name", customerName)},
                "subject", "Conferma ricezione messaggio - ODV Sicilia",
                "htmlContent", htmlContent
            );
            
            sendEmail(emailData);
        } catch (Exception e) {
            System.err.println("Errore nell'invio email conferma contatto: " + e.getMessage());
        }
    }
    
    public void sendOrderNotificationToAdmin(String orderNumber, String customerName, 
                                           String customerEmail, String totalAmount, String items) {
        try {
            String htmlContent = buildOrderAdminEmailHtml(orderNumber, customerName, customerEmail, totalAmount, items);
            
            Map<String, Object> emailData = Map.of(
                "sender", Map.of("name", senderName, "email", senderEmail),
                "to", new Object[]{Map.of("email", adminEmail, "name", "Admin ODV Sicilia")},
                "subject", "Nuovo ordine ricevuto - " + orderNumber,
                "htmlContent", htmlContent
            );
            
            sendEmail(emailData);
        } catch (Exception e) {
            System.err.println("Errore nell'invio email admin ordine: " + e.getMessage());
        }
    }
    
    public void sendOrderConfirmationToCustomer(String customerName, String customerEmail, 
                                              String orderNumber, String totalAmount, String items) {
        try {
            String htmlContent = buildOrderConfirmationEmailHtml(customerName, orderNumber, totalAmount, items);
            
            Map<String, Object> emailData = Map.of(
                "sender", Map.of("name", senderName, "email", senderEmail),
                "to", new Object[]{Map.of("email", customerEmail, "name", customerName)},
                "subject", "Conferma ordine " + orderNumber + " - ODV Sicilia",
                "htmlContent", htmlContent
            );
            
            sendEmail(emailData);
        } catch (Exception e) {
            System.err.println("Errore nell'invio email conferma ordine: " + e.getMessage());
        }
    }
    
    private void sendEmail(Map<String, Object> emailData) throws IOException, InterruptedException {
        String jsonBody = objectMapper.writeValueAsString(emailData);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .header("accept", "application/json")
                .header("api-key", brevoApiKey)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 201) {
            throw new RuntimeException("Errore invio email: " + response.body());
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
}
