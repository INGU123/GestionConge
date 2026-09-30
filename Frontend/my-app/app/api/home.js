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