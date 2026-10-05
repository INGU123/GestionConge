// Client centralisé pour tous les appels API avec injection automatique du token JWT

export function getAuthToken() {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("token");
}

export function getCurrentUser() {
  if (typeof window === "undefined") return null;
  const stored = localStorage.getItem("user");
  if (!stored) return null;
  try {
    const parsed = JSON.parse(stored);
    if (parsed && parsed.utilisateur) {
      return parsed.utilisateur;
    }
    return parsed;
  } catch (e) {
    console.error("Erreur lors de la lecture du profil utilisateur:", e);
    return null;
  }
}

export function saveAuthSession(token, user) {
  if (typeof window === "undefined") return;
  if (token) {
    localStorage.setItem("token", token);
  }
  if (user) {
    localStorage.setItem("user", JSON.stringify(user));
  }
}

export function clearAuthSession() {
  if (typeof window === "undefined") return;
  localStorage.removeItem("token");
  localStorage.removeItem("user");
}

export async function authFetch(url, options = {}) {
  const token = getAuthToken();
  const headers = {
    Accept: "application/json",
    ...(options.headers || {}),
  };

  // N'ajouter Content-Type application/json que si on envoie un body et que ce n'est pas FormData
  if (options.body && !(options.body instanceof FormData) && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    if (typeof window !== "undefined") {
      clearAuthSession();
    }
  }

  return response;
}
