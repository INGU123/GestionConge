import { authFetch, API_BASE_URL } from "@/lib/apiClient";

const API_URL = `${API_BASE_URL}/type-conge`;

export const creerTypeConge = async (typeData) => {
  try {
    const payload = {
      ...typeData,
      regle_acquisition: typeData.regle_acquisition || "STANDARD",
      nombreJoursParAn: Number(typeData.nombreJoursParAn) || 0,
      actif: typeData.actif !== false,
      couleur: typeData.couleur || "#2563eb",
    };

    const response = await authFetch(`${API_URL}/create`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      const errText = await response.text();
      throw new Error(errText || "Erreur lors de la création du type de congé");
    }
    return await response.json();
  } catch (error) {
    console.error("Erreur obtenu : ", error.message);
    throw error;
  }
};

export const getAllTypeConge = async () => {
  try {
    const response = await authFetch(`${API_URL}/all`);
    if (!response.ok) {
      console.error("Erreur HTTP getAllTypeConge:", response.status);
      return [];
    }
    const data = await response.json();

    if (Array.isArray(data)) return data;
    if (data && Array.isArray(data.content)) return data.content;
    if (data && Array.isArray(data.types)) return data.types;

    return [];
  } catch (error) {
    console.error("Erreur lors de la récupération des types de congé:", error);
    return [];
  }
};

export const updateTypeConge = async (id, typeData) => {
  const response = await authFetch(`${API_URL}/update/${id}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(typeData),
  });
  if (!response.ok) throw new Error("Erreur lors de la mise à jour du type de congé");
  return await response.json();
};

export const deleteTypeConge = async (id) => {
  const response = await authFetch(`${API_URL}/delete/${id}`, {
    method: "DELETE",
  });
  if (!response.ok) throw new Error("Erreur lors de la suppression");
  return true;
};