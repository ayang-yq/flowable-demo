# GEMINI Project Analysis: flowable-demo

## Project Overview

This is a full-stack insurance claim management system designed to demonstrate the capabilities of the Flowable 7.x process engine. The project showcases a complete, end-to-end workflow for handling insurance claims, from initial case creation to final payment processing.

The architecture is split into a Java-based backend and a React-based frontend.

-   **Backend:** A Spring Boot application that uses Flowable for orchestrating complex business processes. It leverages the CMMN (Case Management Model and Notation), BPMN (Business Process Model and Notation), and DMN (Decision Model and Notation) engines. The backend is built with Java 17 and Maven, and it uses a PostgreSQL database for data persistence.

-   **Frontend:** A modern, single-page application built with React, TypeScript, and the Ant Design component library. It provides a user-friendly interface for managing claims, tasks, and administrative functions.

-   **Business Logic:** The core business logic revolves around a CMMN case for the overall claim lifecycle. This case includes stages for document collection, assessment, and review. It triggers a DMN decision table to automatically determine the appropriate actions based on claim details (e.g., claim amount, policy type). For payments, it launches a separate BPMN process.

## Building and Running

### Prerequisites

-   Java 17+
-   Maven 3.8+
-   Node.js 18+
-   PostgreSQL

### 1. Database Setup

The application requires two PostgreSQL databases.

```bash
# Connect to PostgreSQL
psql -U postgres

# Create the databases
CREATE DATABASE flowable_cline;
CREATE DATABASE flowable_demo;
```

**Note:** The default database credentials are `flowable_cline` / `flowable_cline` and the host is `localhost:5432`, as configured in `backend/src/main/resources/application.yml`.

### 2. Running the Backend

The backend is a standard Spring Boot application built with Maven.

```bash
# Navigate to the backend directory
cd backend

# Clean the project and install dependencies
mvn clean install

# Run the application
mvn spring-boot:run
```

The backend API will be available at `http://localhost:8080/api`. The OpenAPI/Swagger documentation can be accessed at `http://localhost:8080/api/swagger-ui.html`.

### 3. Running the Frontend

The frontend is a React application managed with npm.

```bash
# Navigate to the frontend directory
cd frontend

# Install dependencies
npm install

# Start the development server
npm start
```

The frontend will be available at `http://localhost:3000`. It is pre-configured to proxy API requests to the backend at `http://localhost:8080`.

## Development Conventions

### Backend

-   **Architecture:** The backend follows principles of Domain-Driven Design (DDD), with a clear separation between the `domain`, `service`, and `web` layers.
-   **Code Style:** The code uses Lombok for reducing boilerplate and MapStruct for object mapping.
-   **Testing:** Unit tests are located in `src/test/java` and can be run with `mvn test`. Testcontainers is used for integration tests, providing an isolated PostgreSQL instance.
-   **Configuration:** Application settings are managed in `backend/src/main/resources/application.yml`, with distinct profiles for `dev` and `prod` environments.

### Frontend

-   **Component-Based:** The UI is built with reusable React components located in `frontend/src/components`.
-   **Typed Language:** TypeScript is used for type safety, with type definitions found in `frontend/src/types`.
-   **State Management:** State is primarily managed using React Context.
-   **API Interaction:** API calls are centralized in the `frontend/src/services` directory, using Axios as the HTTP client.

### Committing

This project does not have any specific commit message conventions.
