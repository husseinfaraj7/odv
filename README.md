# Hussein Project Website

A full-stack web application with React/Next.js frontend and Java Spring Boot backend for Hussein Project.

## Project Structure

```
├── backend/                 # Java Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── test/
│   └── pom.xml
├── frontend/                # Next.js frontend
│   ├── app/
│   ├── components/
│   ├── hooks/
│   ├── lib/
│   ├── public/
│   ├── frontend-integration/
│   └── package.json
└── README.md
```

## Setup and Development

### Backend (Java Spring Boot)

```bash
cd backend
mvn clean install        # Install dependencies
mvn spring-boot:run     # Run backend server (port 8080)
```

### Frontend (Next.js)

```bash
cd frontend
pnpm install            # Install dependencies
pnpm dev                # Run development server (port 3000)
pnpm build              # Build for production
pnpm lint               # Run linter
```

## Features

- Contact form management with email notifications
- Shopping cart and order processing
- PostgreSQL database integration
- Responsive React components with Tailwind CSS
- API proxy configuration for seamless frontend-backend communication
- Client-side JavaScript integration files for embedding in existing websites

## Tech Stack

### Backend
- Java 17
- Spring Boot 3.2
- Spring Data JPA
- PostgreSQL
- Maven

<<<<<<< HEAD
### Frontend
- Next.js 14
- React 18
- TypeScript
- Tailwind CSS 4
- shadcn/ui components
- pnpm

## Deployment

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

### 3. Configuration System

The frontend integration files use an automatic configuration system that detects the correct API endpoints based on the environment:

- **Development**: `http://localhost:8080/api`
- **Staging**: `https://hussein-project-staging.onrender.com/api` (for hostnames containing "staging")
- **Production**: `https://hussein-project.onrender.com/api` (default)

You can override these defaults by setting the environment variable:
```
NEXT_PUBLIC_API_BASE_URL=https://your-custom-api.onrender.com/api
```

### 4. Add to Your Website

Include the JavaScript files in your HTML pages - no configuration required:

**For Contact Form:**
```html
<script src="contact-form.js"></script>
```

**For Shop:**
```html
<script src="shop-cart.js"></script>
```

The files will automatically detect and use the correct API endpoints for your environment.

## API Endpoints

- `POST /api/contact/send` - Send contact message
- `POST /api/orders/create` - Create new order
- Contact and order management endpoints

## Integration

The `frontend/frontend-integration/` directory contains JavaScript files that can be embedded in existing websites to add contact form and shopping cart functionality, automatically connecting to the backend API through the Next.js proxy.