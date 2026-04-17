# Jeera

Jeera is a managed workflow bug tracking system built for role-based engineering teams. It helps project managers, developers, testers, and administrators collaborate on issue lifecycles in a structured and auditable way.

The application is server-rendered using Spring Boot and Thymeleaf, with PostgreSQL as the backing database.

## What Jeera Solves

Teams often need more than a simple ticket list. Jeera models the full bug workflow with explicit roles and controlled status transitions.

- PMs can manage projects, members, and assignment flow.
- Developers can analyze and resolve assigned issues.
- Testers can verify fixes and close or reopen issues.
- Admins can manage users and system-level access.

## Core Features

- Role-based access and workflow actions.
- Project-scoped issue tracking with per-project issue numbering.
- Explicit issue lifecycle states (reported, assigned, in progress, resolved, verification, closed/rejected).
- Issue comments and activity timeline.
- In-app notifications with unread-count polling.
- Server-side rendered UI with Thymeleaf and Bootstrap.

## Technology Stack

- Java 17+
- Spring Boot 3.x
- Spring Data JPA and Hibernate
- Spring Security (form login and session auth)
- Thymeleaf
- PostgreSQL
- Lombok

## Project Structure

- src/main/java/com/jeera
	- config: security and MVC configuration
	- controller: web controllers
	- dto: form and request models
	- model: entities and enums
	- repository: Spring Data repositories
	- service: business logic and permissions
- src/main/resources
	- templates: Thymeleaf views
	- static: CSS and JavaScript assets
	- application.properties and profile-specific properties

## Running Jeera Locally

### Prerequisites

- JDK 17 or newer
- PostgreSQL running locally

### 1) Configure Database

Set the database connection values in your local Spring properties (for example application-dev.properties):

- spring.datasource.url
- spring.datasource.username
- spring.datasource.password

If needed, create a database in PostgreSQL first.

### 2) Build

On macOS/Linux:

```bash
./mvnw clean compile
```

On Windows PowerShell:

```powershell
.\mvnw.cmd clean compile
```

### 3) Run

On macOS/Linux:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Once started, open the app in your browser at the default Spring Boot port (usually http://localhost:8080).

## Development Workflow

- main: stable branch
- dev: integration branch
- feat/*: feature branches opened as PRs into dev

Recommended flow:

- Create a feature branch from dev.
- Make atomic commits by concern.
- Open a PR into dev.
- Merge after review and verification.

## Useful Commands

Run tests:

```bash
./mvnw test
```

Windows PowerShell:

```powershell
.\mvnw.cmd test
```