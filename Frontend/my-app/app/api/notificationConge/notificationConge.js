import { authFetch, API_BASE_URL } from "@/lib/apiClient";

const API_URL = `${API_BASE_URL}/notification`;

export const getNotificationNonLues = async (utilisateurId) => {
  if (!utilisateurId) return [];

  try {
    const response = await authFetch(`${API_URL}/utilisateur/${utilisateurId}/non-lues`);
    if (!response.ok) {
      console.warn(`Avertissement HTTP getNotificationNonLues: ${response.status}`);
      return [];
    }
    const data = await response.json();
    return Array.isArray(data) ? data : [];
  } catch (error) {
    console.warn("Erreur obtenue lors de la récupération des notifications non lues :", error.message);
    return [];
  }
};

export const getToutesLesNotifications = async (utilisateurId) => {
  if (!utilisateurId) return [];

  try {
    const response = await authFetch(`${API_URL}/utilisateur/${utilisateurId}`);
    if (!response.ok) {
      console.warn(`Avertissement HTTP getToutesLesNotifications: ${response.status}`);
      return [];
    }
    const data = await response.json();
    return Array.isArray(data) ? data : [];
  } catch (error) {
    console.warn("Erreur obtenue lors de la récupération des notifications :", error.message);
    return [];
  }
};

export const marquerNotificationLue = async (notificationId) => {
  try {
    const response = await authFetch(`${API_URL}/${notificationId}/lire`, {
      method: "PUT",
    });
    if (!response.ok) throw new Error("Erreur lors de la mise à jour de la notification");
    return true;
  } catch (error) {
    console.warn("Erreur obtenue lors du marquage comme lue :", error.message);
    return false;
  }
};

export const marquerNotificaitonNonLue = marquerNotificationLue;