# Faraj Project - Development Guide

## Tech Stack
- **Backend**: Spring Boot 3.2, Java 17, Maven, JPA/Hibernate, PostgreSQL
- **Frontend**: Next.js (referenced but not present in this repo)
- **Database**: PostgreSQL (production), H2 (development)
- **Deployment**: Render.com, Docker

## Initial Setup
```bash
# Backend - requires Java 17 and Maven
cd backend
./mvnw clean install
```

## Commands
- **Build**: `cd backend && ./mvnw clean package`
- **Test**: `cd backend && ./mvnw test`
- **Dev Server**: `cd backend && ./mvnw spring-boot:run`
- **Lint**: Uses Maven compiler warnings (no separate linter configured)

## Architecture
- REST API backend with controllers, services, repositories
- Package structure: `it.odvsicilia.backend.{controller,service,repository,model,dto,config}`
- Handles orders and contact messages for ODV Sicilia website