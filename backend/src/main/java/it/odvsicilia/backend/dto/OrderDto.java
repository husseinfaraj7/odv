package it.odvsicilia.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class OrderDto {
    
    @NotBlank(message = "Nome è obbligatorio")
    private String customerName;
    
    @NotBlank(message = "Email è obbligatoria")
    @Email(message = "Email deve essere valida")
    private String customerEmail;
    
    private String customerPhone;
    
    @NotNull(message = "Indirizzo di spedizione è obbligatorio")
    @Valid
    private ShippingAddressDto shippingAddress;
    
    private String shippingCity;
    private String shippingPostalCode;
    private String shippingCountry = "Italia";
    
    private BigDecimal subtotal;
    private BigDecimal shippingCost;
    
    @NotNull(message = "Totale è obbligatorio")
    private BigDecimal totalAmount;
    
    private String paymentMethod;
    
    private String notes;
    
    private String company;
    private String contactMethod;
    private String deliveryNotes;
    
    @NotEmpty(message = "Almeno un prodotto è obbligatorio")
    @Valid
    private List<OrderItemDto> items;
    
    // Constructors
    public OrderDto() {}
    
    // Getters and Setters
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    
    public ShippingAddressDto getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(ShippingAddressDto shippingAddress) { 
        this.shippingAddress = shippingAddress;
        if (shippingAddress != null) {
            this.shippingCity = shippingAddress.getCity();
            this.shippingPostalCode = shippingAddress.getZipCode();
            this.shippingCountry = shippingAddress.getCountry();
        }
    }
    
    public String getShippingCity() { return shippingCity; }
    public void setShippingCity(String shippingCity) { this.shippingCity = shippingCity; }
    
    public String getShippingPostalCode() { return shippingPostalCode; }
    public void setShippingPostalCode(String shippingPostalCode) { this.shippingPostalCode = shippingPostalCode; }
    
    public String getShippingCountry() { return shippingCountry; }
    public void setShippingCountry(String shippingCountry) { this.shippingCountry = shippingCountry; }
    
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    
    public BigDecimal getShippingCost() { return shippingCost; }
    public void setShippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    
    public String getContactMethod() { return contactMethod; }
    public void setContactMethod(String contactMethod) { this.contactMethod = contactMethod; }
    
    public String getDeliveryNotes() { return deliveryNotes; }
    public void setDeliveryNotes(String deliveryNotes) { this.deliveryNotes = deliveryNotes; }
    
    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }
}
