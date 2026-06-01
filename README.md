# Jeera

[![CI](https://github.com/amankhandelwaal/jeera/actions/workflows/ci.yml/badge.svg)](https://github.com/amankhandelwaal/jeera/actions/workflows/ci.yml)
[![CodeQL](https://github.com/amankhandelwaal/jeera/actions/workflows/codeql.yml/badge.svg)](https://github.com/amankhandelwaal/jeera/actions/workflows/codeql.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Java 17](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Boot 4.0](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen.svg)

Jeera is a role-based bug tracking and workflow management application for engineering teams.
It is built as a server-rendered web app using Spring Boot, Thymeleaf, and PostgreSQL.

Jeera is designed for teams that need strict issue lifecycle control, clear ownership, and auditable transitions from reporting to closure.

## What Jeera Does

Jeera supports end-to-end bug handling across four actor types:

- Admin: manages platform users and project ownership controls.
- PM: manages projects, members, assignments, and duplicate rejection decisions.
- Developer: analyzes and resolves assigned issues.
- Tester: verifies resolved issues and decides close or reopen.

Core behaviors:

- Role-aware dashboards and actions.
- Project-scoped issue numbering.
- Controlled status transitions.
- Activity logs for traceability.
- In-app notifications with unread badge polling.
- Admin user/project management from a unified admin dashboard.

## Tech Stack

- Java 17
- Spring Boot 4.0.5
- Spring MVC + Thymeleaf
- Spring Security (session/form login)
- Spring Data JPA + Hibernate
- PostgreSQL
- Bootstrap 5 (CDN)
- Lombok

## Architecture At A Glance

Top-level package layout:

- `src/main/java/com/jeera/config`: security and mvc config, startup seed.
- `src/main/java/com/jeera/controller`: web controllers by domain.
- `src/main/java/com/jeera/service`: business logic, permission checks, transitions, notifications.
- `src/main/java/com/jeera/repository`: JPA repositories.
- `src/main/java/com/jeera/model`: entities and enums.
- `src/main/java/com/jeera/dto`: request/view models.
- `src/main/resources/templates`: Thymeleaf pages and fragments.
- `src/main/resources/static`: CSS and JavaScript.

## Role Model

Global system roles:

- `ADMIN`
- `USER`

Project roles (stored in project membership):

- `DEVELOPER`
- `TESTER`

Notes:

- PM capability is represented via project ownership and permissions.
- A `USER` gains practical access through project membership/ownership.

## Issue Lifecycle (Current Implementation)

Allowed transitions:

- `REPORTED -> OPEN`
- `REPORTED -> REJECTED`
- `OPEN -> ASSIGNED`
- `OPEN -> REJECTED`
- `ASSIGNED -> IN_ANALYSIS`
- `IN_ANALYSIS -> IN_PROGRESS`
- `IN_ANALYSIS -> MARK_REJECTED`
- `IN_PROGRESS -> RESOLVED`
- `RESOLVED -> UNDER_VERIFICATION`
- `UNDER_VERIFICATION -> CLOSED`
- `UNDER_VERIFICATION -> OPEN` (reopen on failed verification, assignee cleared)
- `MARK_REJECTED -> OPEN`
- `MARK_REJECTED -> REJECTED`

Important rule:

- `REJECTED` is terminal in current logic (no transition out).

## Main Pages

- `/login`: authenticate user.
- `/register`: end-user sign-up page.
- `/dashboard`: role-aware dashboard views.
- `/projects`: project listing.
- `/projects/{id}/issues`: issue listing + filters.
- `/projects/{id}/issues/{issueId}`: issue detail, comments, activity, actions.
- `/projects/{id}/members`: member management.
- `/notifications`: notification center.
- `/profile`: profile update view.
- `/admin/dashboard?tab=users|projects`: unified admin control panel.

## Notifications

The app polls unread notification count every 30 seconds from:

- `GET /notifications/unread-count`

The bell indicator is updated client-side via `src/main/resources/static/js/main.js`.

## Local Setup

### Prerequisites

- JDK 17+
- PostgreSQL 14+ (or compatible)

### 1) Create Database

Create DB and user (example values used in dev profile):

- database: `jeera_db`
- user: `jeera_user`

### 2) Configure Properties

`src/main/resources/application.properties`:

- active profile is `dev`

`src/main/resources/application-dev.properties` should contain valid datasource values for your machine.

### 3) Build

Windows PowerShell:

```powershell
.\mvnw.cmd clean compile
```

macOS/Linux:

```bash
./mvnw clean compile
```

### 4) Run

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
./mvnw spring-boot:run
```

Open:

- `http://localhost:8080`

## Seed Data And Fresh Start

On empty DB, the initializer seeds one admin user:

- username: `admin`
- email: `admin@jeera.com`
- password: `admin123`

If DB is not empty, seeding is skipped.


## How To Use Jeera (Typical Flow)

1. Admin logs in and creates users.
2. PM creates project and adds members (developers/testers).
3. Team members report issues.
4. PM assigns issue to developer.
5. Developer moves issue through analysis and fix states.
6. Tester picks resolved issue for verification.
7. Tester closes or reopens.
8. All participants track updates via notifications and activity log.

## UI Notes

- Dark mode supported with theme toggle in navbar.
- Theme preference persists in browser local storage (`jeera-theme-preference`).
- Bootstrap text utility overrides are included to prevent unreadable black text in dark mode.


## Git Workflow

- `main`: release/stable
- `dev`: integration
- `feat/*`, `fix/*`: working branches

Recommended:

1. Branch from `dev`.
2. Commit atomically.
3. Open PR into `dev`.
4. Merge `dev` into `main` for release.

## Deployment Notes

Jeera is a full Spring Boot backend + server-rendered frontend app.

- Best fit: Render, Railway, Fly.io, or any JVM-capable host with PostgreSQL.
- Vercel is not the ideal host for this architecture unless you split frontend/backend and re-architect runtime concerns.

Minimum deployment requirements:

- Java runtime (17+)
- PostgreSQL instance
- Environment variables for datasource URL/username/password

## License

This project is for academic/team workflow use unless otherwise specified by repository owner.