package it.odvsicilia.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sendinblue.ApiClient;
import sendinblue.Configuration;
import sendinblue.auth.ApiKeyAuth;
import sibApi.TransactionalEmailsApi;
import sibModel.*;
import java.util.Arrays;

@Service
public class EmailService {

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Value("${brevo.sender.name}")
    private String senderName;

    public void sendOrderConfirmation(String customerEmail, String customerName, String totalAmount) {
        try {
            ApiClient defaultClient = Configuration.getDefaultApiClient();
            ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
            apiKey.setApiKey(brevoApiKey);

            TransactionalEmailsApi apiInstance = new TransactionalEmailsApi();
            
            SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
            
            // Sender
            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setEmail(senderEmail);
            sender.setName(senderName);
            sendSmtpEmail.setSender(sender);

            // Recipients
            SendSmtpEmailTo recipient = new SendSmtpEmailTo();
            recipient.setEmail(customerEmail);
            recipient.setName(customerName);
            sendSmtpEmail.setTo(Arrays.asList(recipient));

            // Email content
            sendSmtpEmail.setSubject("Conferma Ordine - ODV Sicilia");
            
            String htmlContent = String.format("""
                <h2>Grazie per il tuo ordine, %s!</h2>
                <p>Abbiamo ricevuto il tuo ordine per un totale di <strong>€%s</strong>.</p>
                <p>Ti contatteremo presto per confermare i dettagli e organizzare la consegna.</p>
                <br>
                <p>Grazie per aver scelto ODV Sicilia!</p>
                <p><strong>L'Olio di Valeria</strong><br>
                San Biagio Platani (AG)<br>
                www.odvsicilia.it</p>
                """, customerName, totalAmount);
                
            sendSmtpEmail.setHtmlContent(htmlContent);

            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (Exception e) {
            System.err.println("Errore invio email ordine: " + e.getMessage());
        }
    }

    public void sendContactConfirmation(String customerEmail, String customerName) {
        try {
            ApiClient defaultClient = Configuration.getDefaultApiClient();
            ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
            apiKey.setApiKey(brevoApiKey);

            TransactionalEmailsApi apiInstance = new TransactionalEmailsApi();
            
            SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
            
            // Sender
            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setEmail(senderEmail);
            sender.setName(senderName);
            sendSmtpEmail.setSender(sender);

            // Recipients
            SendSmtpEmailTo recipient = new SendSmtpEmailTo();
            recipient.setEmail(customerEmail);
            recipient.setName(customerName);
            sendSmtpEmail.setTo(Arrays.asList(recipient));

            // Email content
            sendSmtpEmail.setSubject("Messaggio ricevuto - ODV Sicilia");
            
            String htmlContent = String.format("""
                <h2>Caro %s,</h2>
                <p>Abbiamo ricevuto il tuo messaggio e ti risponderemo al più presto.</p>
                <p>Grazie per averci contattato!</p>
                <br>
                <p><strong>ODV Sicilia - L'Olio di Valeria</strong><br>
                San Biagio Platani (AG)<br>
                www.odvsicilia.it</p>
                """, customerName);
                
            sendSmtpEmail.setHtmlContent(htmlContent);

            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (Exception e) {
            System.err.println("Errore invio email contatto: " + e.getMessage());
        }
    }

    public void sendAdminNotification(String subject, String content) {
        try {
            ApiClient defaultClient = Configuration.getDefaultApiClient();
            ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
            apiKey.setApiKey(brevoApiKey);

            TransactionalEmailsApi apiInstance = new TransactionalEmailsApi();
            
            SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
            
            // Sender
            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setEmail(senderEmail);
            sender.setName("ODV Sistema");
            sendSmtpEmail.setSender(sender);

            // Admin recipient
            SendSmtpEmailTo recipient = new SendSmtpEmailTo();
            recipient.setEmail(senderEmail);
            recipient.setName("Admin ODV");
            sendSmtpEmail.setTo(Arrays.asList(recipient));

            // Email content
            sendSmtpEmail.setSubject(subject);
            sendSmtpEmail.setHtmlContent(content);

            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (Exception e) {
            System.err.println("Errore invio email admin: " + e.getMessage());
        }
    }
}