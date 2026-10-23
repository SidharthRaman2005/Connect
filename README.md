# Connect

A college-exclusive networking and collaboration platform for VIT-AP students —
discover like-minded people, showcase projects, build connections, and find
teammates for hackathons, projects, and research.

## Tech stack

- **Frontend:** React 19 + Vite, React Router, `@react-oauth/google`
- **Backend:** Java 21, Spring Boot 3.4, Maven, REST APIs
- **Database:** PostgreSQL 18
- **Auth:** Google Sign-In, with server-side ID-token validation (Phase 1)

## Project layout

```
Connect/
├── frontend/     React + Vite app
└── backend/      Spring Boot app
    └── db/init.sql   one-time database creation
```

## Prerequisites

- JDK 21, Maven 3.9+
- Node.js 18+ and npm
- PostgreSQL 18 running locally on port 5432

## First-time setup

### 1. Database

Create the `connect` database once (psql will prompt for your postgres password):

```bash
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -f backend/db/init.sql
```

### 2. Backend

```bash
cp backend/.env.example backend/.env    # then set DB_PASSWORD in backend/.env
cd backend
mvn spring-boot:run
```

Health check: http://localhost:8080/api/health

### 3. Frontend

```bash
cp frontend/.env.example frontend/.env  # VITE_GOOGLE_CLIENT_ID is prefilled
cd frontend
npm install
npm run dev
```

App: http://localhost:5173

## Security notes

- Credentials live only in gitignored `.env` files — never in source.
- The Google OAuth **client secret** must never appear in the frontend.
- `ddl-auto=update` is a development convenience; switch to Flyway migrations
  before any real deployment.
