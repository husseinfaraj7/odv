# AGENTS.md - ODV Sicilia Backend

## Local Development Setup

### Quick Start (H2 In-Memory Database)
The fastest way to get started is to run the application without any database configuration. The application will automatically use an H2 in-memory database:

```powershell
# Activate the dev profile (uses H2 database automatically)
.\mvn.ps1 spring-boot:run -Dspring-boot.run.profiles=dev
```

The H2 console will be available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:testdb`)

### Using Local PostgreSQL (Optional)
If you prefer to use a local PostgreSQL database:

1. Copy `.env.example` to `.env`
2. Uncomment and configure the `DATABASE_URL` and related variables:
   ```
   DATABASE_URL=jdbc:postgresql://localhost:5432/odvsicilia
   DATABASE_USER=postgres
   DATABASE_PASSWORD=yourpassword
   ```
3. Run the application (with or without dev profile):
   ```powershell
   .\mvn.ps1 spring-boot:run
   ```

### Environment Variables
All available configuration options are documented in `.env.example`. Key variables:
- **DATABASE_URL**: Optional for local dev (uses H2 if not set)
- **BREVO_API_KEY**: Required for email functionality
- **SUPABASE_***: Optional for local dev if not using Supabase features
- **PORT**: Server port (defaults to 8080)

## Commands

### Setup
```powershell
.\mvn.ps1 clean install
```

### Build
```powershell
.\mvn.ps1 clean package -DskipTests
```

### Lint
No dedicated linter configured. Use IDE or Maven checkstyle plugin if needed.

### Test
```powershell
.\mvn.ps1 test
```

### Dev Server
```powershell
.\mvn.ps1 spring-boot:run
```

## Tech Stack
- **Framework**: Spring Boot 3.2.0 (Java 17)
- **Database**: PostgreSQL (with H2 for dev)
- **Email**: Brevo SMTP service
- **Build**: Maven (via Docker wrapper script `mvn.ps1`)
- **Deployment**: Docker, Render.com

## Architecture
- **Package**: `it.odvsicilia.backend`
- **Structure**: Standard Spring Boot layered architecture (controller → service → repository → model)
- **Key Components**: Order management, contact messages, email services
- **Config**: Environment-based config via `.env` files and `application.properties`

## Code Style
- Standard Java conventions (camelCase, PascalCase for classes)
- Spring annotations for dependency injection
- DTOs for API request/response
- Custom exceptions with global exception handler
- JPA entities with Hibernate ORM
