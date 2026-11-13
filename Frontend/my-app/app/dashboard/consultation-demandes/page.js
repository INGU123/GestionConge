"use client";

import { useEffect, useState } from "react";
import { authFetch, getCurrentUser } from "@/lib/apiClient";

const API = "http://localhost:8080";

export default function SuiviReliquatsPage() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [role] = useState(() =>
    String(getCurrentUser()?.role || "").trim().toUpperCase().replace(/^ROLE_/, "")
  );

  useEffect(() => {
    if (role !== "ADMIN") {
      setLoading(false);
      return;
    }

    authFetch(API + "/circuit-conge/admin/consultation")
      .then(async (response) => {
        if (!response.ok) throw new Error("Consultation du circuit indisponible");
        return response.json();
      })
      .then(setRows)
      .catch((error) => {
        console.error("Erreur de consultation du circuit des reliquats:", error);
        setRows([]);
      })
      .finally(() => setLoading(false));
  }, [role]);

  if (role !== "ADMIN") {
    return (
      <section className="space-y-3">
        <h1 className="text-2xl font-bold">Circuit des reliquats</h1>
        <p className="text-sm text-slate-500">
          Votre rôle ne dispose pas d'un écran de consultation administrative du circuit.
          Les demandes classiques de congé ne sont plus accessibles dans l'application.
        </p>
      </section>
    );
  }

  return (
    <section className="space-y-5">
      <header>
        <h1 className="text-2xl font-bold">Suivi des dossiers de reliquat</h1>
        <p className="text-sm text-slate-500">
          Vue administrative en lecture seule des dossiers transmis dans le circuit hiérarchique.
        </p>
      </header>
      <div className="card bg-base-100 border p-5 overflow-x-auto">
        <table className="table table-sm">
          <thead>
            <tr>
              <th>Dossier</th><th>Collaborateur</th><th>Type</th><th>Période</th>
              <th>Jours</th><th>Reliquat vérifié</th><th>Statut</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((item) => (
              <tr key={item.id}>
                <td>#{item.id}</td>
                <td>{item.utilisateurPrenom} {item.utilisateurNom} ({item.utilisateurMatricule})</td>
                <td>#{item.typeCongeId}</td>
                <td>{item.dateDebut} → {item.dateFin}</td>
                <td>{item.nombreJours}</td>
                <td>{item.reliquatJours ?? "—"}</td>
                <td>{item.statut}</td>
              </tr>
            ))}
            {!loading && !rows.length && (
              <tr><td colSpan="7" className="text-center text-slate-400">Aucun dossier de reliquat.</td></tr>
            )}
          </tbody>
        </table>
        {loading && <p className="text-center p-6">Chargement…</p>}
      </div>
    </section>
  );
}
