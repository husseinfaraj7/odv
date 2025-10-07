# Agent Instructions for ODV Sicilia Backend

## Commands

**Setup:** Maven dependencies are managed via Docker. No virtual environment needed.

**Build:** `.\mvn.ps1 clean package -DskipTests` (or `cd backend; mvn clean package -DskipTests`)

**Lint:** No dedicated linter configured. Use IDE defaults for Java formatting.

**Test:** `.\mvn.ps1 test` (or `cd backend; mvn test`)

**Dev Server:** `.\mvn.ps1 spring-boot:run` (or `cd backend; mvn spring-boot:run`)

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.2.0, Maven
- **Database:** PostgreSQL (Supabase), H2 (dev/testing)
- **Email:** Brevo SMTP API
- **Deployment:** Docker, Render.com

## Architecture

Standard layered architecture: `controller/` → `service/` → `repository/` → `model/`
- Package structure: `it.odvsicilia.backend.*`
- DTOs for API contracts, JPA entities in `model/`
- Global exception handling via `@ControllerAdvice`

## Code Style

- No comments unless complex logic requires explanation
- Use constructor injection (`@Autowired` on fields currently used)
- Validate input with `@Valid` and Jakarta validation annotations
- RESTful endpoints under `/api/` prefix
