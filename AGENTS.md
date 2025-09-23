# Faraj Project (ODV Sicilia) - Agent Guide

## Setup Commands
- **Install dependencies**: `cd backend && mvn dependency:go-offline`
- **Build**: `cd backend && mvn clean package`
- **Run tests**: `cd backend && mvn test`
- **Run dev server**: `cd backend && mvn spring-boot:run`
- **Docker build**: `docker build -t faraj-project .`

## Tech Stack
- **Backend**: Java 17 + Spring Boot 3.2.0 + Maven
- **Database**: PostgreSQL (Supabase) with H2 for local/testing
- **Email**: Brevo SMTP service
- **Deployment**: Render.com with Docker

## Architecture
- RESTful API with layered architecture (Controller → Service → Repository)
- JPA/Hibernate for data access
- DTO pattern for API responses
- Centralized exception handling

## Code Conventions
- Package structure: `it.odvsicilia.backend.*`
- Camel case for methods/variables, Pascal case for classes
- DTOs for external API contracts
- Service layer for business logic
- Repository pattern for data access
