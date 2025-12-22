# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Flowable 7.x-based insurance claim management system** demonstrating full BPM workflow capabilities. The system integrates CMMN Case Management, BPMN Process Engine, and DMN Decision Engine to handle complex insurance claim workflows.

## Tech Stack & Architecture

### Backend (Spring Boot 3.5.8 + Java 17)
- **Framework**: Spring Boot with JPA/Hibernate
- **Workflow Engine**: Flowable 7.2.0 (CMMN + BPMN + DMN)
- **Database**: PostgreSQL 15 with dual database setup
- **Security**: Spring Security with custom UserDetailsService and BCrypt
- **API Documentation**: SpringDoc OpenAPI (Swagger)
- **Build**: Maven with Lombok integration

### Frontend (React 18 + TypeScript)
- **UI Library**: Ant Design 5.12.8 with consistent component patterns
- **State Management**: React Context for authentication
- **HTTP Client**: Axios with interceptors for Basic Auth
- **Routing**: React Router DOM 6 with protected routes
- **Build**: Create React App with proxy configuration

### Database Setup
The application requires two PostgreSQL databases running locally:

#### Required Databases
1. **App Database**: `flowable_cline` (business data: users, policies, claims)
2. **Flowable Database**: `flowable_demo` (workflow engine tables)

#### Local PostgreSQL Setup
```bash
# Connect to PostgreSQL as postgres user
psql -U postgres

# Create the required databases
CREATE DATABASE flowable_cline;
CREATE DATABASE flowable_demo;

# Verify databases were created
\l
```

#### Configuration
- **Connection**: Local PostgreSQL on localhost:5432
- **Username**: postgres
- **Password**: postgres (update as needed)
- **Connection Pooling**: HikariCP with custom settings
- **Schema**: Auto-initialized with `init-db.sql` and Flowable engine tables

## Development Commands

### Quick Start
```bash
# Setup local PostgreSQL databases (see Database Setup section)
# Start backend (runs on http://localhost:8080/api)
cd backend && mvn spring-boot:run

# Start frontend (runs on http://localhost:3000)
cd frontend && npm start
```

### Backend Development
```bash
cd backend
mvn clean install          # Build project
mvn test                   # Run unit tests
mvn spring-boot:run        # Start development server

# Key URLs:
# - Application: http://localhost:8080/api
# - Swagger UI: http://localhost:8080/api/swagger-ui.html
# - Flowable Admin: http://localhost:8080/api/flowable-rest
```

### Frontend Development
```bash
cd frontend
npm install               # Install dependencies
npm start                 # Start dev server with hot reload
npm test                  # Run Jest tests
npm run build            # Production build
```

## Architecture Patterns

### Backend Structure (Domain-Driven Design)
```
backend/src/main/java/com/flowable/demo/
├── domain/              # Domain layer
│   ├── model/          # JPA entities (User, ClaimCase, InsurancePolicy, etc.)
│   └── repository/     # Spring Data repositories
├── service/            # Application services (CustomUserDetailsService, DataInitializer)
├── web/rest/           # REST API controllers and DTOs
└── config/             # Spring configuration classes
```

### Frontend Structure
```
frontend/src/
├── components/         # React components with TypeScript
├── contexts/          # React contexts (AuthContext)
├── services/          # API service layer with Axios
├── types/             # TypeScript type definitions
└── App.tsx           # Main application with routing
```

## Flowable Integration

### Workflow Definitions Location
- **CMMN Cases**: `backend/src/main/resources/processes/ClaimCase.cmmn`
- **BPMN Processes**: `backend/src/main/resources/processes/ClaimPaymentProcess.bpmn`
- **DMN Decisions**: `backend/src/main/resources/processes/ClaimDecisionTable.dmn`

### Key Flowable Configuration
- **Auto-deployment**: Process definitions auto-deployed on startup
- **Separate Databases**: Business data separate from workflow engine
- **Async Executor**: Enabled for background processing
- **History Level**: Full audit trail configured

## Security & Authentication

### Backend Security
- **Custom Authentication**: `CustomUserDetailsService` with BCrypt passwords
- **Default Users**: Auto-created admin user (admin/admin) with roles
- **CORS**: Configured for localhost:3000 development
- **Protected Endpoints**: All `/api/*` require authentication except `/api/users/current`

### Frontend Authentication
- **AuthContext**: Centralized authentication state management
- **ProtectedRoute**: Route protection component
- **Basic Auth**: Automatic Authorization header injection
- **Session Management**: localStorage with automatic cleanup

## Database Schema

### Key Entities
- **User & Role**: User management with ManyToMany relationships
- **InsurancePolicy**: Insurance policies with coverage details
- **ClaimCase**: Main business entity with workflow integration
- **ClaimDocument**: Document management for claims
- **ClaimHistory**: Audit trail with trigger-managed timestamps

### Database Initialization
- **Schema Creation**: Automatic via `spring.jpa.hibernate.ddl-auto=update`
- **Seed Data**: `DataInitializer` creates roles and admin user
- **Timestamp Management**: Database triggers for `updated_at` fields

## API Structure

### REST Endpoints
```
/api/users/*           # User management and authentication
/api/cases/*           # Claim case management
/api/tasks/*           # Flowable task operations
/api/policies/*        # Insurance policy management
/flowable-rest/*       # Flowable REST API
```

### Frontend API Service
- **Centralized**: All API calls through `/services/api.ts`
- **Type-safe**: Complete TypeScript interfaces
- **Error Handling**: Automatic 401 redirect to login
- **Interceptors**: Automatic Basic Auth header injection

## Development Patterns

### Backend Patterns
- **DTO Pattern**: Data Transfer Objects for API layer in `web/rest/dto/`
- **Repository Pattern**: Spring Data with custom query methods
- **UUID Primary Keys**: All entities use UUID for IDs
- **Transactional Services**: Proper transaction boundaries
- **Configuration Management**: Environment-specific YAML configs

### Frontend Patterns
- **Functional Components**: React hooks with TypeScript
- **Context API**: Authentication and global state
- **Ant Design**: Consistent UI component usage
- **Route Guards**: Protected routes with authentication checks

## Testing Setup

### Backend
- **TestContainers**: Configured for integration testing
- **Maven Surefire**: Unit test execution
- **Test Profile**: Available for test-specific configuration

### Frontend
- **Jest**: Test runner with React Testing Library
- **Coverage**: Configured but currently no implementations
- **Test Scripts**: Standard Create React App test setup

## Important Configuration Files

- `backend/src/main/resources/application.yml`: Main Spring configuration
- `frontend/package.json`: Dependencies and scripts
- `backend/src/main/resources/init-db.sql`: Database schema and seed data

## Common Development Tasks

### Adding New API Endpoints
1. Create DTO in `backend/src/main/java/com/flowable/demo/web/rest/dto/`
2. Add REST controller method in `web/rest/*Resource.java`
3. Add TypeScript types in `frontend/src/types/index.ts`
4. Add API service methods in `frontend/src/services/api.ts`

### Modifying Workflows
1. Update process definitions in `backend/src/main/resources/processes/`
2. Test changes via Flowable Admin UI
3. Update frontend task handling if needed

### Database Changes
1. Modify `init-db.sql` for schema changes
2. Update JPA entities in `domain/model/`
3. Consider using Flyway/Liquibase for production migrations

### Adding New User Roles
1. Update `DataInitializer.java` role creation
2. Add role checks in `CustomUserDetailsService`
3. Update frontend role-based UI if needed
