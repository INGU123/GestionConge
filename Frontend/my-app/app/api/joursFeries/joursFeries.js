import { authFetch, API_BASE_URL } from "@/lib/apiClient";

const API_URL = `${API_BASE_URL}/jours-feries`;

export const getJoursFeries = async () => {
  try {
    const response = await authFetch(`${API_URL}/all`);
    if (!response.ok) {
      console.warn(`Avertissement HTTP getJoursFeries: ${response.status}`);
      return [];
    }
    const data = await response.json();
    return Array.isArray(data) ? data : [];
  } catch (error) {
    console.warn("Erreur lors de la récupération des jours fériés:", error);
    return [];
  }
};
