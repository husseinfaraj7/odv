# AGENTS.md

## Commands

### Initial Setup
```bash
# Setup environment configuration (choose one)
./setup-env.sh local    # For local development with H2 database
./setup-env.sh render   # For Render deployment
./setup-env.sh production # For production deployment

# Edit the generated .env file with your actual values
```

### Build
```bash
cd backend
./mvnw clean compile
```

### Lint
```bash
cd backend
./mvnw verify
```

### Test
```bash
cd backend
./mvnw test
```

### Dev Server
```bash
cd backend
./mvnw spring-boot:run
```

## Tech Stack & Architecture

**Backend**: Spring Boot 3.2 with Java 17, JPA/Hibernate, PostgreSQL/H2  
**Deployment**: Docker on Render.com with PostgreSQL  
**Email**: Brevo SMTP integration  
**External**: Supabase integration for additional services  

## Code Style

- Java 17 features and syntax
- Spring Boot conventions and annotations
- Maven standard directory structure (`src/main/java`, `src/test/java`)
- Environment-based configuration with `.env` files
- RESTful API design patterns
