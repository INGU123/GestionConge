const API_URL = "http://localhost:8080/utilisateur";

export const loginUser = async (identifier, motDePass) => {
  const idTrimmed = identifier ? identifier.trim() : "";
  const response = await fetch(`${API_URL}/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      matricule: idTrimmed,
      email: idTrimmed,
      password: motDePass,
      mot_de_pass: motDePass,
    }),
  });

  if (!response.ok) {
    const errorText = await response.text();
    let parsedMessage = errorText;
    try {
      const jsonErr = JSON.parse(errorText);
      if (jsonErr && jsonErr.message) {
        parsedMessage = jsonErr.message;
      }
    } catch {}
    throw new Error(parsedMessage || "Matricule ou mot de passe incorrect.");
  }

  return await response.json(); // Retourne { token, utilisateur }
};