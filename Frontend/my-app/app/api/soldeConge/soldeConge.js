import { authFetch } from "@/lib/apiClient";

const API_URL = "http://localhost:8080/solde";

export const getSoldesUtilisateur = async (utilisateurId) => {
  if (!utilisateurId) return [];

  try {
    const response = await authFetch(`${API_URL}/utilisateur/${utilisateurId}`);
    if (!response.ok) {
      console.warn(`Avertissement HTTP getSoldesUtilisateur: ${response.status}`);
      return [];
    }
    const data = await response.json();
    return Array.isArray(data) ? data : [];
  } catch (error) {
    console.warn("Erreur obtenue lors de la récupération du solde: ", error.message);
    return [];
  }
};

export const initialiserSoldesAnnuels = async (utilisateurId, annee) => {
  try {
    const response = await authFetch(`${API_URL}/initialiser/${utilisateurId}?annee=${annee}`, {
      method: "POST",
    });
    if (!response.ok) throw new Error("Erreur d'initialisation de solde annuel");
    return true;
  } catch (error) {
    console.warn("Erreur lors de l'initialisation: ", error.message);
    throw error;
  }
};

export const getAllSoldes = async () => {
  try {
    const response = await authFetch(`${API_URL}/all`);
    if (!response.ok) {
      console.warn(`Avertissement HTTP getAllSoldes: ${response.status}`);
      return [];
    }
    const data = await response.json();
    return Array.isArray(data) ? data : [];
  } catch (error) {
    console.warn("Erreur obtenue: ", error.message);
    return [];
  }
};

export const ajustSolde = async (soldeId, nouveauNombreJoursRestants, motif) => {
  try {
    const response = await authFetch(
      `${API_URL}/ajuster/${soldeId}?nouveauNombreJoursRestants=${encodeURIComponent(
        Math.round(Number(nouveauNombreJoursRestants))
      )}&motif=${encodeURIComponent(motif?.trim() || "")}`,
      {
        method: "PUT",
      }
    );
    if (!response.ok) throw new Error("Erreur de mise à jour du solde");
    return await response.json();
  } catch (error) {
    console.warn("Erreur obtenue lors de l'ajustement: ", error.message);
    throw error;
  }
};