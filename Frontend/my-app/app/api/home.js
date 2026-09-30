export async function loginUser(matricule, password) {
  const response = await fetch("http://localhost:8080/utilisateur/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      matricule: matricule,
      password: password
    }),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || "Matricule ou mot de passe incorrect.");
  }

  return await response.json();
}

export async function postData(endpoint, payload, options = {}) {
  const response = await fetch(`http://localhost:8080${endpoint}`, {
    method: options.method || "POST",
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    },
    body: JSON.stringify(payload),
    ...options,
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || "Erreur lors de l'appel API.");
  }

  return await response.json();
}