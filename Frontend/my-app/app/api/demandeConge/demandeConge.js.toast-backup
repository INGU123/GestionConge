import { authFetch } from "@/lib/apiClient";

const API_URL = "http://localhost:8080/conge";

export const creerDemandeConge = async (utilisateurId, demandeData) => {
  const formatDate = (date) => {
    if (!date) return "";
    if (typeof date === "string") return date;
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  };

  try {
    const response = await authFetch(`${API_URL}/demander/${utilisateurId}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        typeCongeId: Number(demandeData.typeCongeId),
        dateDebut: formatDate(demandeData.debut),
        dateFin: formatDate(demandeData.fin),
        commentaire: (demandeData.commentaire || "").trim(),
      }),
    });

    if (!response.ok) {
      const details = await response.text();
      throw new Error(
        details || `Erreur lors de la création de la demande (${response.status})`
      );
    }
    return await response.json();
  } catch (error) {
    console.warn("Erreur lors de la création de la demande:", error.message);
    throw error;
  }
};

export const traiterDemandeConge = async (
  demandeId,
  managerId,
  statut,
  motifRefus = ""
) => {
  const params = new URLSearchParams({
    managerId: String(managerId),
    statut,
    motifRefus: motifRefus || "",
  });
  const response = await authFetch(
    `${API_URL}/${demandeId}/traiter?${params.toString()}`,
    {
      method: "PUT",
    }
  );
  if (!response.ok) throw new Error("Erreur lors du traitement de la demande");
  return await response.json();
};

export const annulerDemandeConge = async (demandeId, utilisateurId) => {
  const response = await authFetch(
    `${API_URL}/${demandeId}/annuler?utilisateurId=${utilisateurId}`,
    {
      method: "PUT",
    }
  );
  if (!response.ok)
    throw new Error("Erreur lors de l'annulation de la demande");
  return await response.json();
};

export const getMesDemandes = async (utilisateurId) => {
  if (!utilisateurId) return [];

  try {
    const response = await authFetch(`${API_URL}/mes-demandes/${utilisateurId}`);
    if (!response.ok) {
      // Utilisation de console.warn pour éviter le pop-up rouge d'erreur Next.js en dev
      console.warn(`Avertissement HTTP getMesDemandes: ${response.status}`);
      return [];
    }
    const data = await response.json();
    return Array.isArray(data) ? data : [];
  } catch (error) {
    console.warn("Erreur réseau lors de la récupération des demandes:", error);
    return [];
  }
};

export const getDemandesEnAttenteManager = async (managerId) => {
  if (!managerId) return [];

  try {
    const response = await authFetch(`${API_URL}/manager/${managerId}/en-attente`);
    if (!response.ok) {
      console.warn(`Avertissement HTTP getDemandesEnAttenteManager: ${response.status}`);
      return [];
    }
    const data = await response.json();
    return Array.isArray(data) ? data : [];
  } catch (error) {
    console.warn("Erreur lors de la récupération des demandes du manager:", error);
    return [];
  }
};

export const getToutesDemandesEnAttente = async () => {
  try {
    const response = await authFetch(`${API_URL}/en-attente`);
    if (!response.ok) return [];
    const data = await response.json();
    return Array.isArray(data) ? data : [];
  } catch (error) {
    console.warn("Erreur lors de la récupération de toutes les demandes en attente:", error);
    return [];
  }
};

export const getAllDemandes = async () => {
  try {
    const response = await authFetch(`${API_URL}/all`);
    if (!response.ok) return [];
    const data = await response.json();
    return Array.isArray(data) ? data : [];
  } catch (error) {
    console.warn("Erreur lors de la récupération de toutes les demandes:", error);
    return [];
  }
};