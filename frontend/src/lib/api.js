// Shared API client for talking to the Spring Boot backend.
//
// Centralizes the base URL and auth handling so pages/components don't repeat
// fetch boilerplate. Auth token handling is a thin stub for now; the real
// session token is wired up in the Phase 1 auth modules.

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

// The backend origin without the trailing "/api" — used to resolve relative
// media paths (e.g. uploaded profile pictures served at /uploads/...).
const API_ORIGIN = BASE_URL.replace(/\/api\/?$/, "");

/**
 * Resolve an image path from the backend into a URL usable in <img src>.
 * Absolute URLs (e.g. Google avatars) are returned unchanged; relative paths
 * like "/uploads/x.png" are prefixed with the backend origin.
 */
export function resolveMediaUrl(path) {
  if (!path) return "";
  if (/^https?:\/\//i.test(path)) return path;
  return `${API_ORIGIN}${path.startsWith("/") ? "" : "/"}${path}`;
}

let authToken = null;

/** Set (or clear, with null) the token sent on subsequent requests. */
export function setAuthToken(token) {
  authToken = token;
}

/**
 * Make a JSON request to the backend.
 * @param {string} path     Path relative to the API base, e.g. "/health".
 * @param {object} [options] fetch options; `body` may be a plain object.
 * @returns {Promise<any>} Parsed JSON (or null for empty responses).
 * @throws {Error} with `.status` and `.data` on non-2xx responses.
 */
export async function apiFetch(path, options = {}) {
  const { body, headers, ...rest } = options;

  const finalHeaders = { ...headers };
  let finalBody = body;
  if (body !== undefined && !(body instanceof FormData)) {
    finalHeaders["Content-Type"] = "application/json";
    finalBody = JSON.stringify(body);
  }
  if (authToken) {
    finalHeaders["Authorization"] = `Bearer ${authToken}`;
  }

  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      ...rest,
      headers: finalHeaders,
      body: finalBody,
    });
  } catch (networkError) {
    const err = new Error("Unable to reach the server. Is the backend running?");
    err.cause = networkError;
    throw err;
  }

  const text = await response.text();
  const data = text ? JSON.parse(text) : null;

  if (!response.ok) {
    const err = new Error((data && data.message) || `Request failed (${response.status})`);
    err.status = response.status;
    err.data = data;
    throw err;
  }
  return data;
}

export const api = {
  get: (path, options) => apiFetch(path, { ...options, method: "GET" }),
  post: (path, body, options) => apiFetch(path, { ...options, method: "POST", body }),
  put: (path, body, options) => apiFetch(path, { ...options, method: "PUT", body }),
  patch: (path, body, options) => apiFetch(path, { ...options, method: "PATCH", body }),
  del: (path, options) => apiFetch(path, { ...options, method: "DELETE" }),
};
