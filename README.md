# ODV Sicilia Backend

A small Java Spring Boot backend application for handling contact forms and shop orders for the ODV Sicilia website.

## Features

- **Contact Form Handler**: Receives and stores contact messages
- **Order Management**: Handles shop orders with customer details
- **Email Integration**: Sends notifications via Brevo (Sendinblue)
- **Admin Notifications**: Automatic emails to admin for new messages/orders
- **Customer Confirmations**: Confirmation emails to customers
- **RESTful API**: Clean API endpoints for frontend integration

## Quick Setup

### 1. Deploy to Render

1. Fork this repository
2. Connect to Render and create a new Web Service
3. Use the `render.yaml` configuration
4. Set environment variables:
   - `BREVO_API_KEY`: Your Brevo API key
   - `ADMIN_EMAIL`: Your admin email address
   - `DATABASE_URL`: Automatically set by Render PostgreSQL

### 2. Get Brevo API Key

1. Sign up at [Brevo](https://www.brevo.com)
2. Go to SMTP & API → API Keys
3. Create a new API key
4. Add it to your Render environment variables

### 3. Update Frontend

Replace the API URL in the JavaScript files:
\`\`\`javascript
const API_BASE_URL = 'https://your-render-app.onrender.com/api';
\`\`\`

### 4. Add to Your Website

Include the JavaScript files in your HTML pages:

**For Contact Form:**
\`\`\`html
<script src="contact-form.js"></script>
\`\`\`

**For Shop:**
\`\`\`html
<script src="shop-cart.js"></script>
\`\`\`

## API Endpoints

### Contact Messages
- `POST /api/contact/send` - Send contact message
- `GET /api/contact/messages` - Get all messages (admin)
- `GET /api/contact/unread-count` - Get unread count
- `PUT /api/contact/mark-read/{id}` - Mark message as read

### Orders
- `POST /api/orders/create` - Create new order
- `GET /api/orders/all` - Get all orders (admin)
- `GET /api/orders/{orderNumber}` - Get specific order
- `PUT /api/orders/{orderNumber}/status` - Update order status
- `GET /api/orders/stats` - Get order statistics

## Environment Variables

```properties
# Required
BREVO_API_KEY=your-brevo-api-key
ADMIN_EMAIL=admin@odvsicilia.it

# Optional (with defaults)
BREVO_SENDER_EMAIL=noreply@odvsicilia.it
BREVO_SENDER_NAME=ODV Sicilia
FRONTEND_URL=https://odvsicilia.it
DATABASE_URL=jdbc:postgresql://...
