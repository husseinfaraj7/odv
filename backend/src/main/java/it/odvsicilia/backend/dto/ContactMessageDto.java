package it.odvsicilia.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ContactMessageDto {
    
    @NotBlank(message = "Nome è obbligatorio")
    @Size(max = 100, message = "Nome non può superare 100 caratteri")
    private String name;
    
    @NotBlank(message = "Email è obbligatoria")
    @Email(message = "Email deve essere valida")
    @Size(max = 150, message = "Email non può superare 150 caratteri")
    private String email;
    
    @Size(max = 20, message = "Telefono non può superare 20 caratteri")
    private String phone;
    
    @NotBlank(message = "Oggetto è obbligatorio")
    @Size(max = 200, message = "Oggetto non può superare 200 caratteri")
    private String subject;
    
    @NotBlank(message = "Messaggio è obbligatorio")
    @Size(max = 2000, message = "Messaggio non può superare 2000 caratteri")
    private String message;
    
    // Constructors
    public ContactMessageDto() {}
    
    public ContactMessageDto(String name, String email, String phone, String subject, String message) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.subject = subject;
        this.message = message;
    }
    
    // Getters and Setters
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
}
