package it.odvsicilia.controller;

import it.odvsicilia.model.Contact;
import it.odvsicilia.repository.ContactRepository;
import it.odvsicilia.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/contact")
@CrossOrigin(origins = "${cors.allowed.origins}")
public class ContactController {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private EmailService emailService;

    @PostMapping
    public ResponseEntity<?> createContact(@Valid @RequestBody Contact contact) {
        try {
            Contact savedContact = contactRepository.save(contact);
            
            // Invia email di conferma al cliente
            emailService.sendContactConfirmation(contact.getEmail(), contact.getName());
            
            // Notifica admin
            String adminContent = String.format("""
                <h3>Nuovo Messaggio di Contatto</h3>
                <p><strong>Nome:</strong> %s</p>
                <p><strong>Email:</strong> %s</p>
                <p><strong>Telefono:</strong> %s</p>
                <p><strong>Oggetto:</strong> %s</p>
                <p><strong>Messaggio:</strong></p>
                <p>%s</p>
                """, 
                contact.getName(),
                contact.getEmail(),
                contact.getPhone(),
                contact.getSubject(),
                contact.getMessage()
            );
            
            emailService.sendAdminNotification("Nuovo Messaggio - ODV Sicilia", adminContent);
            
            return ResponseEntity.ok().body("{\"success\": \"Messaggio inviato con successo! Ti risponderemo presto.\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"Errore nell'invio del messaggio: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping
    public List<Contact> getAllContacts() {
        return contactRepository.findAllByOrderByCreatedAtDesc();
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        return contactRepository.findById(id)
                .map(contact -> {
                    contact.setStatus("read");
                    contactRepository.save(contact);
                    return ResponseEntity.ok().body("{\"success\": \"Messaggio segnato come letto\"}");
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
