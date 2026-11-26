
import { authFetch } from "@/lib/apiClient";

const API_URL = "http://localhost:8080/type-conge";

export const creerTypeConge = async (typeData) => {
  const payload = {
    ...typeData,
    code: typeData.code?.trim().toUpperCase(),
    libelle: typeData.libelle?.trim(),
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
    throw new Error(
      errText || "Erreur lors de la création du type de congé."
    );
  }

  return await response.json();
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
    if (Array.isArray(data?.content)) return data.content;
    if (Array.isArray(data?.types)) return data.types;

    return [];
  } catch (error) {
    console.error(
      "Erreur lors de la récupération des types de congé:",
      error
    );
    return [];
  }
};

export const updateTypeConge = async (id, typeData) => {
  const payload = {
    ...typeData,
    code: typeData.code?.trim().toUpperCase(),
    libelle: typeData.libelle?.trim(),
  };

  const response = await authFetch(`${API_URL}/update/${id}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(
      errText || "Erreur lors de la mise à jour du type de congé."
    );
  }

  return await response.json();
};

export const deleteTypeConge = async (id) => {
  const response = await authFetch(`${API_URL}/delete/${id}`, {
    method: "DELETE",
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(errText || "Erreur lors de la suppression.");
  }

  return true;
};
