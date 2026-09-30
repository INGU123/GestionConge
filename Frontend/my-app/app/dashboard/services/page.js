"use client";

import { useEffect, useMemo, useState } from "react";
import { authFetch, getCurrentUser } from "@/lib/apiClient";

const API_URL = "http://localhost:8080/services";

export default function ServicesPage() {
  const [user, setUser] = useState(null);
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [form, setForm] = useState({ nom: "", responsable_id: "", effectifMinimum: 1 });

  useEffect(() => {
    const currentUser = getCurrentUser();
    setUser(currentUser);
    loadServices();
  }, []);

  const loadServices = async () => {
    try {
      setLoading(true);
      const response = await authFetch(API_URL);
      if (!response.ok) throw new Error("Impossible de charger les services.");
      const data = await response.json();
      setServices(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || "Erreur lors du chargement.");
    } finally {
      setLoading(false);
    }
  };

  const isAdmin = user?.role?.toUpperCase() === "ADMIN";

  const submitForm = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");

    try {
      const payload = {
        nom: form.nom.trim(),
        responsable_id: form.responsable_id ? Number(form.responsable_id) : null,
        effectifMinimum: Number(form.effectifMinimum || 1),
      };

      const response = await authFetch(`${API_URL}/create`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        const details = await response.text();
        throw new Error(details || "Erreur lors de la création.");
      }

      setSuccess("Service créé avec succès.");
      setForm({ nom: "", responsable_id: "", effectifMinimum: 1 });
      await loadServices();
    } catch (err) {
      setError(err.message || "Erreur inconnue.");
    }
  };

  const deleteService = async (id) => {
    if (!isAdmin) return;
    try {
      const response = await authFetch(`${API_URL}/${id}`, { method: "DELETE" });
      if (!response.ok) throw new Error("Suppression impossible.");
      setSuccess("Service supprimé.");
      await loadServices();
    } catch (err) {
      setError(err.message || "Erreur lors de la suppression.");
    }
  };

  const totalEffectif = useMemo(
    () => services.reduce((sum, s) => sum + Number(s.effectifMinimum || 0), 0),
    [services]
  );

  return (
    <div className="space-y-6 p-4">
      <div className="rounded-2xl bg-slate-900 text-white p-6 shadow-md">
        <p className="text-xs uppercase tracking-[0.2em] text-blue-400">SPAT</p>
        <h1 className="mt-2 text-2xl font-bold">Gestion des services</h1>
      </div>

      {error && <div className="alert alert-error text-white">{error}</div>}
      {success && <div className="alert alert-success text-white">{success}</div>}

      {isAdmin && (
        <form onSubmit={submitForm} className="grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 md:grid-cols-4">
          <input
            className="input input-bordered"
            value={form.nom}
            placeholder="Nom du service"
            onChange={(e) => setForm((f) => ({ ...f, nom: e.target.value }))}
            required
          />
          <input
            type="number"
            className="input input-bordered"
            value={form.responsable_id}
            placeholder="ID responsable"
            onChange={(e) => setForm((f) => ({ ...f, responsable_id: e.target.value }))}
          />
          <input
            type="number"
            min="1"
            className="input input-bordered"
            value={form.effectifMinimum}
            onChange={(e) => setForm((f) => ({ ...f, effectifMinimum: e.target.value }))}
          />
          <button type="submit" className="btn btn-primary">Créer un service</button>
        </form>
      )}

      <div className="rounded-2xl border border-slate-200 bg-white p-4">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold">Liste des services</h2>
          <span className="badge badge-primary">{services.length} services</span>
        </div>

        {loading ? (
          <div>Chargement…</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="table w-full">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Nom</th>
                  <th>Responsable</th>
                  <th>Effectif min.</th>
                  {isAdmin && <th>Action</th>}
                </tr>
              </thead>
              <tbody>
                {services.map((service) => (
                  <tr key={service.id}>
                    <td>{service.id}</td>
                    <td>{service.nom}</td>
                    <td>{service.responsable_id ?? "—"}</td>
                    <td>{service.effectifMinimum ?? 0}</td>
                    {isAdmin && (
                      <td>
                        <button
                          className="btn btn-error btn-sm"
                          onClick={() => deleteService(service.id)}
                        >
                          Supprimer
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="rounded-2xl bg-slate-100 p-4 text-sm text-slate-700">
        Effectif minimum total : <strong>{totalEffectif}</strong>
      </div>
    </div>
  );
}
