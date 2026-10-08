import { authFetch } from "@/lib/apiClient";

const API_URL = "http://localhost:8080/historiques";

export const getAllHistorique = async () => {
  try {
    const response = await authFetch(`${API_URL}/all`);
    if (!response.ok) {
      console.warn(`Avertissement HTTP getAllHistorique: ${response.status}`);
      return [];
    }
    const data = await response.json();
    return Array.isArray(data) ? data : [];
  } catch (error) {
    console.warn("Erreur lors de la récupération de l'historique général: ", error.message);
    return [];
  }
};

export const getHistoriqueUtilisateur = async (utilisateurId) => {
  if (!utilisateurId) return [];

  try {
    const response = await authFetch(`${API_URL}/utilisateur/${utilisateurId}`);
    if (!response.ok) {
      console.warn(`Avertissement HTTP getHistoriqueUtilisateur: ${response.status}`);
      return [];
    }
    const data = await response.json();
    return Array.isArray(data) ? data : [];
  } catch (error) {
    console.warn("Erreur lors de la récupération des mouvements: ", error.message);
    return [];
  }
};

export const getHistoriquePdf = async (scope) => {
  const response = await authFetch(
    `${API_URL}/export/pdf?scope=${encodeURIComponent(scope)}`,
    { headers: { Accept: "application/pdf" } },
  );
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Erreur lors de l'export PDF (${response.status}).`);
  }
  return response.blob();
};