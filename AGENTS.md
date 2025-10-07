# AGENTS.md - ODV Sicilia Backend

## Commands

### Setup
```powershell
# No installation needed - uses Docker-based Maven wrapper
./mvn.ps1 clean install
```

### Build
```powershell
./mvn.ps1 clean package
```

### Test
```powershell
./mvn.ps1 test
```

### Lint
```powershell
./mvn.ps1 checkstyle:check
```

### Dev Server
```powershell
cd backend; ./mvn.ps1 spring-boot:run
```

## Tech Stack
- **Backend**: Spring Boot 3.2.0 (Java 17)
- **Database**: PostgreSQL (prod), H2 (dev)
- **ORM**: Spring Data JPA / Hibernate
- **Email**: Brevo SMTP API
- **Deployment**: Docker on Render.com

## Architecture
- **MVC Pattern**: Controllers → Services → Repositories
- **Package Structure**: `it.odvsicilia.backend.{controller,service,repository,model,dto,exception,config}`
- **DTOs**: Separate request/response objects from entities
- **Global Exception Handling**: Centralized in `GlobalExceptionHandler`

## Code Style
- **Naming**: camelCase for variables/methods, PascalCase for classes
- **Validation**: Use Jakarta validation annotations on DTOs
- **Logging**: SLF4J logger in controllers/services
- **Error Handling**: Custom exceptions with specific error codes
- **Cross-Origin**: Configured via `@CrossOrigin` annotations or `CorsConfig`
