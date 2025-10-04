package it.odvsicilia.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ShippingAddressDto {

    @NotBlank(message = "Indirizzo è obbligatorio")
    @Size(max = 255, message = "L'indirizzo non può superare i 255 caratteri")
    private String street;

    @NotBlank(message = "Città è obbligatoria")
    @Size(max = 100, message = "La città non può superare i 100 caratteri")
    private String city;

    @Size(max = 100, message = "La provincia non può superare i 100 caratteri")
    private String state; // Provincia/Regione

    @NotBlank(message = "CAP è obbligatorio")
    @Size(max = 20, message = "Il CAP non può superare i 20 caratteri")
    private String zipCode;

    @NotBlank(message = "Il paese è obbligatorio")
    @Size(max = 100, message = "Il paese non può superare i 100 caratteri")
    private String country = "IT";

    // Constructors
    public ShippingAddressDto() {}

    public ShippingAddressDto(String street, String city, String state, String zipCode, String country) {
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.country = country;
    }

    // Getters and Setters
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zi

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}
