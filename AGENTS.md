# Agent Commands & Repository Info

## Commands

### Initial Setup
```bash
# Frontend
pnpm install

# Backend  
cd backend && ./mvnw spring-boot:run
```

### Build
- Frontend: `pnpm build` - Build production bundle
- Backend: `cd backend && ./mvnw clean package`

### Lint
```bash
pnpm lint
```

### Tests
- Frontend: No test command configured
- Backend: `cd backend && ./mvnw test`

### Dev Server
```bash
pnpm dev
```

## Tech Stack
- **Frontend**: Next.js 14 + React 18 + TypeScript
- **Styling**: Tailwind CSS v4 + shadcn/ui components  
- **Backend**: Java Spring Boot (separate service) + PostgreSQL + Maven
- **Package Manager**: pnpm
- **Icons**: Lucide React
- **UI Components**: Radix UI primitives + shadcn/ui

## Architecture
- **Mixed Structure**: Frontend in root, Backend in `/backend`
- **App Router**: Using Next.js 14 app directory structure (`/app`)
- **Components**: Reusable UI components in `/components`
- **Utilities**: Helper functions in `/lib`
- **Hooks**: Custom React hooks in `/hooks`
- **Styles**: Global styles in `/styles`, component styles in `/app`
- Backend API endpoints for contact forms and orders
- Deployment on Render with PostgreSQL database

## Code Style & Conventions
- TypeScript strict mode enabled
- ESLint with Next.js configuration (build errors ignored in config)
- shadcn/ui "new-york" style with neutral base color
- CSS variables for theming
- Path aliases configured (`@/*` → project root)
- Component naming: PascalCase for components
- File structure: Co-locate related files
