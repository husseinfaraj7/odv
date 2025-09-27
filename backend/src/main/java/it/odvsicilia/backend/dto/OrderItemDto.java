package it.odvsicilia.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class OrderItemDto {
    
    @NotBlank(message = "Nome prodotto è obbligatorio")
    private String productName;
    
    private String productDescription;
    
    @NotNull(message = "Quantità è obbligatoria")
    @Positive(message = "Quantità deve essere positiva")
    private Integer quantity;
    
    @NotNull(message = "Prezzo unitario è obbligatorio")
    @Positive(message = "Prezzo unitario deve essere positivo")
    private BigDecimal unitPrice;
    
    private String productSku;
    
    // Constructors
    public OrderItemDto() {}
    
    public OrderItemDto(String productName, Integer quantity, BigDecimal unitPrice) {
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }
    
    // Getters and Setters
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public String getProductDescription() { return productDescription; }
    public void setProductDescription(String productDescription) { this.productDescription = productDescription; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    
    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }
}
