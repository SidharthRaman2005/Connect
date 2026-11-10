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
