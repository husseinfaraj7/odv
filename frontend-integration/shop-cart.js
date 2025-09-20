// Shopping Cart and Order Handler for ODV Sicilia Website
// Add this script to your shop pages

class ShoppingCart {
  constructor(apiBaseUrl = "https://your-render-app.onrender.com/api") {
    this.apiBaseUrl = apiBaseUrl
    this.cart = this.loadCart()
    this.init()
  }

  init() {
    this.renderCart()
    this.addEventListeners()
    this.addCartStyles()
  }

  addEventListeners() {
    // Add to cart buttons
    document.addEventListener("click", (e) => {
      if (e.target.matches(".add-to-cart-btn")) {
        e.preventDefault()
        this.addToCart(e.target)
      }

      if (e.target.matches(".remove-from-cart")) {
        e.preventDefault()
        this.removeFromCart(e.target.dataset.productId)
      }

      if (e.target.matches(".update-quantity")) {
        e.preventDefault()
        this.updateQuantity(e.target.dataset.productId, Number.parseInt(e.target.value))
      }

      if (e.target.matches("#checkout-btn")) {
        e.preventDefault()
        this.showCheckoutForm()
      }
    })

    // Checkout form submission
    document.addEventListener("submit", (e) => {
      if (e.target.matches("#checkout-form")) {
        e.preventDefault()
        this.handleCheckout(e.target)
      }
    })
  }

  addToCart(button) {
    const productData = {
      id: button.dataset.productId || Date.now().toString(),
      name: button.dataset.productName,
      price: Number.parseFloat(button.dataset.productPrice),
      description: button.dataset.productDescription || "",
      sku: button.dataset.productSku || "",
      quantity: 1,
    }

    const existingItem = this.cart.find((item) => item.id === productData.id)

    if (existingItem) {
      existingItem.quantity += 1
    } else {
      this.cart.push(productData)
    }

    this.saveCart()
    this.renderCart()
    this.showMessage(`${productData.name} aggiunto al carrello!`, "success")
  }

  removeFromCart(productId) {
    this.cart = this.cart.filter((item) => item.id !== productId)
    this.saveCart()
    this.renderCart()
  }

  updateQuantity(productId, quantity) {
    const item = this.cart.find((item) => item.id === productId)
    if (item) {
      if (quantity <= 0) {
        this.removeFromCart(productId)
      } else {
        item.quantity = quantity
        this.saveCart()
        this.renderCart()
      }
    }
  }

  getCartTotal() {
    return this.cart.reduce((total, item) => total + item.price * item.quantity, 0)
  }

  renderCart() {
    const cartContainer = document.querySelector("#cart-container")
    if (!cartContainer) return

    if (this.cart.length === 0) {
      cartContainer.innerHTML = "<p>Il carrello è vuoto</p>"
      return
    }

    const cartHTML = `
            <div class="cart-items">
                ${this.cart
                  .map(
                    (item) => `
                    <div class="cart-item">
                        <h4>${item.name}</h4>
                        <p>Prezzo: €${item.price.toFixed(2)}</p>
                        <div class="quantity-controls">
                            <label>Quantità:</label>
                            <input type="number" 
                                   class="update-quantity" 
                                   data-product-id="${item.id}" 
                                   value="${item.quantity}" 
                                   min="1">
                        </div>
                        <p>Subtotale: €${(item.price * item.quantity).toFixed(2)}</p>
                        <button class="remove-from-cart" data-product-id="${item.id}">Rimuovi</button>
                    </div>
                `,
                  )
                  .join("")}
            </div>
            <div class="cart-total">
                <h3>Totale: €${this.getCartTotal().toFixed(2)}</h3>
                <button id="checkout-btn" class="checkout-button">Procedi all'ordine</button>
            </div>
        `

    cartContainer.innerHTML = cartHTML
  }

  showCheckoutForm() {
    const checkoutHTML = `
            <div id="checkout-modal" class="checkout-modal">
                <div class="checkout-content">
                    <h2>Completa il tuo ordine</h2>
                    <form id="checkout-form">
                        <div class="form-group">
                            <label for="customer-name">Nome Completo *</label>
                            <input type="text" id="customer-name" name="customerName" required>
                        </div>
                        
                        <div class="form-group">
                            <label for="customer-email">Email *</label>
                            <input type="email" id="customer-email" name="customerEmail" required>
                        </div>
                        
                        <div class="form-group">
                            <label for="customer-phone">Telefono</label>
                            <input type="tel" id="customer-phone" name="customerPhone">
                        </div>
                        
                        <div class="form-group">
                            <label for="shipping-address">Indirizzo di spedizione *</label>
                            <textarea id="shipping-address" name="shippingAddress" required></textarea>
                        </div>
                        
                        <div class="form-row">
                            <div class="form-group">
                                <label for="shipping-city">Città *</label>
                                <input type="text" id="shipping-city" name="shippingCity" required>
                            </div>
                            
                            <div class="form-group">
                                <label for="shipping-postal-code">CAP *</label>
                                <input type="text" id="shipping-postal-code" name="shippingPostalCode" required>
                            </div>
                        </div>
                        
                        <div class="form-group">
                            <label for="payment-method">Metodo di pagamento</label>
                            <select id="payment-method" name="paymentMethod">
                                <option value="bonifico">Bonifico bancario</option>
                                <option value="contrassegno">Contrassegno</option>
                                <option value="paypal">PayPal</option>
                            </select>
                        </div>
                        
                        <div class="form-group">
                            <label for="notes">Note aggiuntive</label>
                            <textarea id="notes" name="notes"></textarea>
                        </div>
                        
                        <div class="order-summary">
                            <h3>Riepilogo ordine</h3>
                            ${this.cart
                              .map(
                                (item) => `
                                <div class="order-item">
                                    ${item.name} x ${item.quantity} = €${(item.price * item.quantity).toFixed(2)}
                                </div>
                            `,
                              )
                              .join("")}
                            <div class="order-total">
                                <strong>Totale: €${this.getCartTotal().toFixed(2)}</strong>
                            </div>
                        </div>
                        
                        <div class="form-actions">
                            <button type="button" onclick="this.closest('.checkout-modal').remove()">Annulla</button>
                            <button type="submit">Conferma Ordine</button>
                        </div>
                    </form>
                </div>
            </div>
        `

    document.body.insertAdjacentHTML("beforeend", checkoutHTML)
  }

  async handleCheckout(form) {
    const formData = new FormData(form)

    const orderData = {
      customerName: formData.get("customerName"),
      customerEmail: formData.get("customerEmail"),
      customerPhone: formData.get("customerPhone"),
      shippingAddress: formData.get("shippingAddress"),
      shippingCity: formData.get("shippingCity"),
      shippingPostalCode: formData.get("shippingPostalCode"),
      shippingCountry: "Italia",
      totalAmount: this.getCartTotal(),
      paymentMethod: formData.get("paymentMethod"),
      notes: formData.get("notes"),
      items: this.cart.map((item) => ({
        productName: item.name,
        productDescription: item.description,
        quantity: item.quantity,
        unitPrice: item.price,
        productSku: item.sku,
      })),
    }

    // Show loading state
    const submitButton = form.querySelector('button[type="submit"]')
    submitButton.disabled = true
    submitButton.textContent = "Invio in corso..."

    try {
      const response = await fetch(`${this.apiBaseUrl}/orders/create`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(orderData),
      })

      const result = await response.json()

      if (result.success) {
        this.showMessage(`Ordine creato con successo! Numero ordine: ${result.orderNumber}`, "success")
        this.clearCart()
        document.querySelector("#checkout-modal").remove()
      } else {
        this.showMessage(result.message, "error")
      }
    } catch (error) {
      console.error("Error creating order:", error)
      this.showMessage("Errore di connessione. Riprova più tardi.", "error")
    } finally {
      submitButton.disabled = false
      submitButton.textContent = "Conferma Ordine"
    }
  }

  loadCart() {
    const saved = localStorage.getItem("odv-sicilia-cart")
    return saved ? JSON.parse(saved) : []
  }

  saveCart() {
    localStorage.setItem("odv-sicilia-cart", JSON.stringify(this.cart))
  }

  clearCart() {
    this.cart = []
    this.saveCart()
    this.renderCart()
  }

  showMessage(message, type) {
    const messageDiv = document.createElement("div")
    messageDiv.className = `cart-message cart-message-${type}`
    messageDiv.textContent = message

    document.body.appendChild(messageDiv)

    setTimeout(() => {
      if (messageDiv.parentNode) {
        messageDiv.remove()
      }
    }, 5000)
  }

  addCartStyles() {
    if (!document.querySelector("#cart-styles")) {
      const style = document.createElement("style")
      style.id = "cart-styles"
      style.textContent = `
                .cart-item {
                    border: 1px solid #ddd;
                    padding: 15px;
                    margin: 10px 0;
                    border-radius: 5px;
                }
                
                .cart-total {
                    text-align: center;
                    margin: 20px 0;
                }
                
                .checkout-button {
                    background-color: #f4a261;
                    color: white;
                    padding: 12px 24px;
                    border: none;
                    border-radius: 5px;
                    cursor: pointer;
                    font-size: 16px;
                }
                
                .checkout-modal {
                    position: fixed;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                    background-color: rgba(0,0,0,0.5);
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    z-index: 1000;
                }
                
                .checkout-content {
                    background: white;
                    padding: 30px;
                    border-radius: 10px;
                    max-width: 600px;
                    max-height: 80vh;
                    overflow-y: auto;
                    width: 90%;
                }
                
                .form-group {
                    margin-bottom: 15px;
                }
                
                .form-row {
                    display: flex;
                    gap: 15px;
                }
                
                .form-row .form-group {
                    flex: 1;
                }
                
                .form-group label {
                    display: block;
                    margin-bottom: 5px;
                    font-weight: bold;
                }
                
                .form-group input,
                .form-group textarea,
                .form-group select {
                    width: 100%;
                    padding: 8px;
                    border: 1px solid #ddd;
                    border-radius: 4px;
                    box-sizing: border-box;
                }
                
                .order-summary {
                    background-color: #f8f9fa;
                    padding: 15px;
                    border-radius: 5px;
                    margin: 20px 0;
                }
                
                .form-actions {
                    display: flex;
                    gap: 10px;
                    justify-content: flex-end;
                    margin-top: 20px;
                }
                
                .form-actions button {
                    padding: 10px 20px;
                    border: none;
                    border-radius: 5px;
                    cursor: pointer;
                }
                
                .form-actions button[type="submit"] {
                    background-color: #2c5530;
                    color: white;
                }
                
                .form-actions button[type="button"] {
                    background-color: #6c757d;
                    color: white;
                }
                
                .cart-message {
                    position: fixed;
                    top: 20px;
                    right: 20px;
                    padding: 15px;
                    border-radius: 5px;
                    z-index: 1001;
                    font-weight: bold;
                }
                
                .cart-message-success {
                    background-color: #d4edda;
                    color: #155724;
                    border: 1px solid #c3e6cb;
                }
                
                .cart-message-error {
                    background-color: #f8d7da;
                    color: #721c24;
                    border: 1px solid #f5c6cb;
                }
            `
      document.head.appendChild(style)
    }
  }
}

// Initialize when DOM is loaded
document.addEventListener("DOMContentLoaded", () => {
  // Replace with your actual Render app URL
  const API_BASE_URL = "https://your-render-app.onrender.com/api"

  // Initialize shopping cart
  window.shoppingCart = new ShoppingCart(API_BASE_URL)
})
