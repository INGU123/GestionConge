"use client";

import { useCallback, useEffect, useState } from "react";
import { FiCheck as Check, FiX as X, FiRefreshCw as RefreshCw, FiShield as ShieldCheck } from "react-icons/fi";
import { authFetch, getCurrentUser } from "@/lib/apiClient";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export default function ReinitialisationsPage() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busyIds, setBusyIds] = useState(() => new Set());
  const [message, setMessage] = useState("");
  const user = getCurrentUser();
  const isAdmin = ["ADMIN", "ROLE_ADMIN"].includes(String(user?.role || "").toUpperCase());

  const loadRequests = useCallback(async (showSpinner = false) => {
    if (!isAdmin) { setLoading(false); return; }
    if (showSpinner) setLoading(true);
    try {
      const response = await authFetch(`${API_URL}/auth/admin/password-reset-requests`);
      if (!response.ok) throw new Error("Impossible de charger les demandes.");
      setRequests(await response.json());
    } catch (error) {
      setMessage(error.message || "Erreur de connexion au serveur.");
    } finally {
      if (showSpinner) setLoading(false);
    }
  }, [isAdmin]);

  useEffect(() => { loadRequests(true); }, [loadRequests]);

  const handleDecision = async (id, decision) => {
    setBusyIds((previous) => new Set(previous).add(id));
    setMessage("");
    try {
      const response = await authFetch(`${API_URL}/auth/admin/password-reset-requests/${id}/${decision}`, { method: "POST" });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || "La décision n'a pas pu être enregistrée.");
      setMessage(data.message || "Opération terminée.");
      await loadRequests(false);
    } catch (error) {
      setMessage(error.message || "Erreur de connexion au serveur.");
    } finally {
      setBusyIds((previous) => { const next = new Set(previous); next.delete(id); return next; });
    }
  };

  if (!isAdmin) return <div className="alert alert-error">Cette rubrique est réservée à l'administrateur.</div>;

  return (
    <div className="space-y-6">
      <div className="bg-slate-900 text-white rounded-2xl p-6 border-l-4 border-blue-600">
        <h1 className="text-2xl font-bold flex items-center gap-2"><ShieldCheck className="w-6 h-6 text-blue-400" /> Réinitialisations de mot de passe</h1>
        <p className="text-slate-300 text-sm mt-2">Examinez les demandes des agents SPAT. Après acceptation, un mot de passe temporaire est généré et envoyé à l'adresse e-mail enregistrée.</p>
      </div>
      {message && <div className="alert alert-info"><span>{message}</span></div>}
      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm p-5">
        <div className="flex items-center justify-between gap-3 mb-4">
          <h2 className="font-bold text-slate-800">Demandes en attente ({requests.length})</h2>
          <button onClick={() => loadRequests(true)} className="btn btn-sm btn-outline" disabled={loading}><RefreshCw className="w-4 h-4" /> Actualiser</button>
        </div>
        {loading ? <div className="flex justify-center py-10"><span className="loading loading-spinner loading-lg" /></div> :
          requests.length === 0 ? <p className="text-sm text-slate-500 py-8 text-center">Aucune demande en attente.</p> :
          <div className="overflow-x-auto"><table className="table w-full">
            <thead><tr><th>Agent</th><th>Matricule</th><th>E-mail enregistré</th><th>Date de demande</th><th>Décision</th></tr></thead>
            <tbody>{requests.map((request) => <tr key={request.id}>
              <td className="font-semibold">{request.prenom} {request.nom}</td>
              <td>{request.matricule}</td>
              <td>{request.email}</td>
              <td>{new Date(request.dateDemande).toLocaleString("fr-FR")}</td>
              <td><div className="flex gap-2">
                <button className="btn btn-success btn-sm" disabled={busyIds.has(request.id)} title="Accepter et envoyer un mot de passe temporaire" onClick={() => handleDecision(request.id, "approve")}>{busyIds.has(request.id) ? <span className="loading loading-spinner loading-xs" /> : <Check className="w-4 h-4" />} Accepter</button>
                <button className="btn btn-outline btn-error btn-sm" disabled={busyIds.has(request.id)} title="Refuser la demande" onClick={() => handleDecision(request.id, "reject")}>{busyIds.has(request.id) ? <span className="loading loading-spinner loading-xs" /> : <X className="w-4 h-4" />} Refuser</button>
              </div></td>
            </tr>)}</tbody>
          </table></div>}
      </div>
    </div>
  );
}
