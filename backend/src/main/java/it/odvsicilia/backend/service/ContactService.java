package it.odvsicilia.backend.service;

import it.odvsicilia.backend.dto.ContactMessageDto;
import it.odvsicilia.backend.exception.ContactNotFoundException;
import it.odvsicilia.backend.model.ContactMessage;
import it.odvsicilia.backend.repository.ContactMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ContactService {
    
    @Autowired
    private ContactMessageRepository contactMessageRepository;
    
    @Autowired
    private EmailService emailService;
    
    public ContactMessage saveAndSendContactMessage(ContactMessageDto contactDto) {
        // Save to database
        ContactMessage contactMessage = new ContactMessage(
            contactDto.getName(),
            contactDto.getEmail(),
            contactDto.getPhone(),
            contactDto.getSubject(),
            contactDto.getMessage()
        );
        
        ContactMessage savedMessage = contactMessageRepository.save(contactMessage);
        
        // Send emails
        try {
            // Send notification to admin
            emailService.sendContactNotificationToAdmin(
                contactDto.getName(),
                contactDto.getEmail(),
                contactDto.getSubject(),
                contactDto.getMessage(),
                contactDto.getPhone()
            );
            
            // Send confirmation to customer
            emailService.sendContactConfirmationToCustomer(
                contactDto.getName(),
                contactDto.getEmail(),
                contactDto.getSubject()
            );
        } catch (Exception e) {
            System.err.println("Errore nell'invio delle email: " + e.getMessage());
            // Don't fail the entire operation if email fails
        }
        
        return savedMessage;
    }
    
    public List<ContactMessage> getAllMessages() {
        return contactMessageRepository.findAllByOrderByCreatedAtDesc();
    }
    
    public List<ContactMessage> getUnreadMessages() {
        return contactMessageRepository.findByIsReadFalseOrderByCreatedAtDesc();
    }
    
    public long getUnreadMessagesCount() {
        return contactMessageRepository.countUnreadMessages();
    }
    
    public ContactMessage markAsRead(Long messageId) {
        ContactMessage message = contactMessageRepository.findById(messageId)
            .orElseThrow(() -> new ContactNotFoundException("Messaggio con ID " + messageId + " non trovato", "CONTACT_MESSAGE_NOT_FOUND"));
        
        message.setIsRead(true);
        return contactMessageRepository.save(message);
    }
    }
}
