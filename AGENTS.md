# AGENTS.md - ODV Sicilia Backend

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
