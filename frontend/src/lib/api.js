// Shared API client for the Spring Boot backend.
const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";
const API_ORIGIN = BASE_URL.replace(/\/api\/?$/, "");

export function chatSocketUrl(ticket) {
  const protocol = API_ORIGIN.startsWith("https:") ? "wss:" : "ws:";
  const host = API_ORIGIN.replace(/^https?:/, "");
  return `${protocol}${host}/ws/chat?ticket=${encodeURIComponent(ticket)}`;
}

export function resolveMediaUrl(path) {
  if (!path) return "";
  if (/^https?:\/\//i.test(path)) return path;
  return `${API_ORIGIN}${path.startsWith("/") ? "" : "/"}${path}`;
}

let authToken = null;
let unauthorizedHandler = null;

export function setAuthToken(token) {
  authToken = token;
}

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler;
}

export async function apiFetch(path, options = {}) {
  const { body, headers, ...rest } = options;
  const finalHeaders = { ...headers };
  let finalBody = body;

  if (body !== undefined && !(body instanceof FormData)) {
    finalHeaders["Content-Type"] = "application/json";
    finalBody = JSON.stringify(body);
  }
  if (authToken) {
    finalHeaders.Authorization = `Bearer ${authToken}`;
  }

  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      ...rest,
      headers: finalHeaders,
      body: finalBody,
    });
  } catch (networkError) {
    const error = new Error("Unable to reach the server. Is the backend running?");
    error.cause = networkError;
    throw error;
  }

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch (parseError) {
      const error = new Error("The server returned an invalid response.");
      error.cause = parseError;
      throw error;
    }
  }

  if (!response.ok) {
    const error = new Error((data && data.message) || `Request failed (${response.status})`);
    error.status = response.status;
    error.data = data;
    if (response.status === 401 && unauthorizedHandler) {
      unauthorizedHandler();
    }
    throw error;
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
