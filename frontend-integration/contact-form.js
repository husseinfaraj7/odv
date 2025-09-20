// Contact Form Handler for ODV Sicilia Website
// Add this script to your contact page

class ContactFormHandler {
  constructor(formSelector, apiBaseUrl = "https://your-render-app.onrender.com/api") {
    this.form = document.querySelector(formSelector)
    this.apiBaseUrl = apiBaseUrl
    this.init()
  }

  init() {
    if (!this.form) {
      console.error("Contact form not found")
      return
    }

    this.form.addEventListener("submit", this.handleSubmit.bind(this))
    this.addLoadingStyles()
  }

  async handleSubmit(event) {
    event.preventDefault()

    const formData = new FormData(this.form)
    const contactData = {
      name: formData.get("name") || formData.get("nome"),
      email: formData.get("email"),
      phone: formData.get("phone") || formData.get("telefono"),
      subject: formData.get("subject") || formData.get("oggetto"),
      message: formData.get("message") || formData.get("messaggio"),
    }

    // Validate required fields
    if (!contactData.name || !contactData.email || !contactData.subject || !contactData.message) {
      this.showMessage("Per favore compila tutti i campi obbligatori.", "error")
      return
    }

    // Show loading state
    this.setLoadingState(true)

    try {
      const response = await fetch(`${this.apiBaseUrl}/contact/send`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(contactData),
      })

      const result = await response.json()

      if (result.success) {
        this.showMessage(result.message, "success")
        this.form.reset()
      } else {
        this.showMessage(result.message, "error")
      }
    } catch (error) {
      console.error("Error sending contact message:", error)
      this.showMessage("Errore di connessione. Riprova più tardi.", "error")
    } finally {
      this.setLoadingState(false)
    }
  }

  setLoadingState(loading) {
    const submitButton = this.form.querySelector('button[type="submit"], input[type="submit"]')
    if (submitButton) {
      submitButton.disabled = loading
      submitButton.textContent = loading ? "Invio in corso..." : "Invia Messaggio"
    }
  }

  showMessage(message, type) {
    // Remove existing messages
    const existingMessage = document.querySelector(".contact-message")
    if (existingMessage) {
      existingMessage.remove()
    }

    // Create new message element
    const messageDiv = document.createElement("div")
    messageDiv.className = `contact-message contact-message-${type}`
    messageDiv.textContent = message

    // Insert message before form
    this.form.parentNode.insertBefore(messageDiv, this.form)

    // Auto-remove success messages after 5 seconds
    if (type === "success") {
      setTimeout(() => {
        if (messageDiv.parentNode) {
          messageDiv.remove()
        }
      }, 5000)
    }
  }

  addLoadingStyles() {
    // Add CSS styles if not already present
    if (!document.querySelector("#contact-form-styles")) {
      const style = document.createElement("style")
      style.id = "contact-form-styles"
      style.textContent = `
                .contact-message {
                    padding: 15px;
                    margin: 15px 0;
                    border-radius: 5px;
                    font-weight: bold;
                }
                .contact-message-success {
                    background-color: #d4edda;
                    color: #155724;
                    border: 1px solid #c3e6cb;
                }
                .contact-message-error {
                    background-color: #f8d7da;
                    color: #721c24;
                    border: 1px solid #f5c6cb;
                }
                button[disabled] {
                    opacity: 0.6;
                    cursor: not-allowed;
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

  // Initialize contact form handler
  // Update the selector to match your contact form
  new ContactFormHandler("#contact-form", API_BASE_URL)
})
