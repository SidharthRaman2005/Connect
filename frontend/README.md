# Connect frontend

React.js web client for the Connect campus networking platform.

## Structure

- `src/pages/` — route-level screens
- `src/components/` — shared feature components
- `src/auth/` — authentication session context
- `src/theme/` — shared light/dark theme state
- `src/lib/` — backend API client and media helpers
- `public/` — static browser assets

## Setup

1. Copy `.env.example` to `.env`.
2. Set `VITE_GOOGLE_CLIENT_ID` to the Google OAuth web client ID.
3. Set `VITE_API_BASE_URL` to the backend API URL.

```powershell
npm install
npm run dev
```

Google OAuth must allow the local frontend origin in Google Cloud Console. Module 2 provides the styled landing/sign-in screen and connects it to the existing backend authentication flow.
