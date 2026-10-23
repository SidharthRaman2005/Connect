-- One-time database setup for Connect.
--
-- Run this ONCE as the PostgreSQL superuser. From a terminal:
--
--   "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -f backend/db/init.sql
--
-- psql will prompt for your postgres password. This script only creates the
-- database; the application itself creates/updates tables on startup (JPA
-- ddl-auto=update during development).

-- Create the application database (ignore the error if it already exists).
CREATE DATABASE connect;

-- ---------------------------------------------------------------------------
-- OPTIONAL (recommended for better security): use a dedicated app role instead
-- of the postgres superuser. If you do this, set DB_USERNAME=connect_app and
-- DB_PASSWORD=<the password below> in backend/.env, and pick a strong password.
--
--   CREATE ROLE connect_app WITH LOGIN PASSWORD 'change-me-strong';
--   GRANT ALL PRIVILEGES ON DATABASE connect TO connect_app;
--   \connect connect
--   GRANT ALL ON SCHEMA public TO connect_app;
-- ---------------------------------------------------------------------------
