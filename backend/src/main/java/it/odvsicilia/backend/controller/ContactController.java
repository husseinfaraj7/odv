package it.odvsicilia.backend.controller;

import it.odvsicilia.backend.dto.ApiResponse;
import it.odvsicilia.backend.dto.ContactMessageDto;
import it.odvsicilia.backend.service.ContactService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
@CrossOrigin(origins = {"https://odvsicilia.it", "http://localhost:3000"})
public class ContactController {
    
    private static final Logger logger = LoggerFactory.getLogger(ContactController.class);
    
    @Autowired
    private ContactService contactService;
    
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<String>> sendContactMessage(@Valid @RequestBody ContactMessageDto contactDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Errori di validazione");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Dati non validi", errorMessage));
        }
        
        try {
            contactService.saveAndSendContactMessage(contactDto);
            return ResponseEntity.ok(
                ApiResponse.success("Messaggio inviato con successo! Ti risponderemo presto.")
            );
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error in contact message: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Dati non validi", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error sending contact message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Errore interno del server", "Errore nell'invio del messaggio. Riprova più tardi."));
        }
    }
    
    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<?>> getAllMessages() {
        try {
            return ResponseEntity.ok(
                ApiResponse.success("Messaggi recuperati con successo", contactService.getAllMessages())
            );
        } catch (Exception e) {
            logger.error("Error retrieving all messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Errore interno del server", "Errore nel recupero dei messaggi."));
        }
    }
    
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        try {
            long count = contactService.getUnreadMessagesCount();
            return ResponseEntity.ok(
                ApiResponse.success("Conteggio recuperato con successo", count)
            );
        } catch (Exception e) {
            logger.error("Error getting unread count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Errore interno del server", "Errore nel conteggio dei messaggi."));
        }
    }
    
    @PutMapping("/mark-read/{id}")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("ID non valido", "L'ID del messaggio deve essere un numero positivo"));
        }
        
        try {
            boolean updated = contactService.markAsRead(id);
            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Messaggio non trovato", "Nessun messaggio trovato con l'ID specificato"));
            }
            return ResponseEntity.ok(
                ApiResponse.success("Messaggio contrassegnato come letto.")
            );
        } catch (Exception e) {
            logger.error("Error marking message as read", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Errore interno del server", "Errore nell'aggiornamento del messaggio."));
        }
    }
}
