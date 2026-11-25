# Connect

A student networking and collaboration platform for the VIT-AP community.

## Project layout

```
Connect/
├── frontend/     React.js web app (Vite)
└── backend/      Spring Boot API and PostgreSQL setup
```

## Prerequisites

- Node.js and npm
- JDK 21 and Maven 3.9+
- PostgreSQL 18

## Backend

Create the database once:

```powershell
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -f backend/db/init.sql
```

Copy `backend/.env.example` to `backend/.env`, set `DB_PASSWORD`, then start the API:

For an existing database, apply the checked-in SQL migrations once before starting the API. Replace `postgres` with the `DB_USERNAME` configured in `backend/.env` if needed:

```powershell
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d connect -f backend/src/main/resources/db/migration/V2026112501__expand_collab_post_status_check.sql
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d connect -f backend/src/main/resources/db/migration/V2026112502__remove_student_last_login_at.sql
```

The API's development `ddl-auto: update` setting creates the new safety and blog tables when it starts.

```powershell
cd backend
mvn spring-boot:run
```

Health check: `http://localhost:8080/api/health`

## Frontend

Set up `frontend/.env` from `frontend/.env.example` and configure the Google OAuth client ID and backend URL first. See [frontend/README.md](frontend/README.md) for frontend setup.

```powershell
cd frontend
npm install
npm run dev
```

Google sign-in requires an authorized web origin configured in Google Cloud Console.
