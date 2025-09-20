package it.odvsicilia.backend.controller;

import it.odvsicilia.backend.dto.ContactMessageDto;
import it.odvsicilia.backend.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/contact")
@CrossOrigin(origins = {"https://odvsicilia.it", "http://localhost:3000"})
public class ContactController {
    
    @Autowired
    private ContactService contactService;
    
    @PostMapping("/send")
    public ResponseEntity<?> sendContactMessage(@Valid @RequestBody ContactMessageDto contactDto) {
        try {
            contactService.saveAndSendContactMessage(contactDto);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Messaggio inviato con successo! Ti risponderemo presto."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Errore nell'invio del messaggio. Riprova più tardi."
            ));
        }
    }
    
    @GetMapping("/messages")
    public ResponseEntity<?> getAllMessages() {
        try {
            return ResponseEntity.ok(contactService.getAllMessages());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Errore nel recupero dei messaggi."
            ));
        }
    }
    
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount() {
        try {
            long count = contactService.getUnreadMessagesCount();
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Errore nel conteggio dei messaggi."
            ));
        }
    }
    
    @PutMapping("/mark-read/{id}")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        try {
            contactService.markAsRead(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Messaggio contrassegnato come letto."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Errore nell'aggiornamento del messaggio."
            ));
        }
    }
}
