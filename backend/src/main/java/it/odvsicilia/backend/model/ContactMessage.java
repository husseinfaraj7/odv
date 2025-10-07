package it.odvsicilia.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "contact_messages")
public class ContactMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Nome è obbligatorio")
    @Size(max = 100, message = "Nome non può superare 100 caratteri")
    @Column(nullable = false, length = 100)
    private String name;
    
    @NotBlank(message = "Email è obbligatoria")
    @Email(message = "Email deve essere valida")
    @Size(max = 150, message = "Email non può superare 150 caratteri")
    @Column(nullable = false, length = 150)
    private String email;
    
    @Size(max = 20, message = "Telefono non può superare 20 caratteri")
    @Column(length = 20)
    private String phone;
    
    @NotBlank(message = "Oggetto è obbligatorio")
    @Size(max = 200, message = "Oggetto non può superare 200 caratteri")
    @Column(nullable = false, length = 200)
    private String subject;
    
    @NotBlank(message = "Messaggio è obbligatorio")
    @Size(max = 2000, message = "Messaggio non può superare 2000 caratteri")
    @Column(nullable = false, length = 2000)
    private String message;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
    
    // Constructors
    public ContactMessage() {
        this.createdAt = LocalDateTime.now();
    }
    
    public ContactMessage(String name, String email, String phone, String subject, String message) {
        this();
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.subject = subject;
        this.message = message;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
}
