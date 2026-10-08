"use client";

import { useEffect, useState } from "react";
import {
  getToutesDemandesEnAttente,
  getAllDemandes,
  traiterDemandeConge,
} from "../../api/demandeConge/demandeConge";
import { getAllTypeConge } from "../../api/typeConge/typeConge";
import { getCurrentUser, authFetch } from "@/lib/apiClient";
import { useToast } from "../../components/ToastProvider";
import {
  CheckCircle2,
  XCircle,
  ShieldAlert,
  Clock,
  Filter,
  Check,
  X,
  Inbox,
  User,
  Calendar,
  MessageSquare,
  FileCheck2
} from "lucide-react";

export default function ValidationPage() {
  const [manager] = useState(() => getCurrentUser());
  const [demandes, setDemandes] = useState([]);
  const [typesConge, setTypesConge] = useState([]);
  const [utilisateurs, setUtilisateurs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState("EN_ATTENTE");
  const [modalRefus, setModalRefus] = useState({ open: false, demandeId: null, motif: "" });
  const [actionLoading, setActionLoading] = useState(null);
  const showToast = useToast();

  useEffect(() => {
    let ignore = false;
    const fetchData = async () => {
      try {
        setLoading(true);
        const [demandesData, typesData, usersRes] = await Promise.all([
          filter === "EN_ATTENTE" ? getToutesDemandesEnAttente() : getAllDemandes(),
          getAllTypeConge().catch(() => []),
          authFetch("http://localhost:8080/utilisateur/all")
            .then((r) => (r && r.ok ? r.json() : []))
            .catch(() => []),
        ]);

        if (!ignore) {
          setDemandes(Array.isArray(demandesData) ? demandesData : []);
          setTypesConge(Array.isArray(typesData) ? typesData : []);
          setUtilisateurs(Array.isArray(usersRes) ? usersRes : []);
        }
      } catch (err) {
        console.error("Erreur chargement validation:", err);
        if (!ignore) showToast("Impossible de charger les demandes à valider.", "error");
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    };

    void fetchData();
    return () => {
      ignore = true;
    };
  }, [filter, showToast]);

  const role = manager?.role ? String(manager.role).toUpperCase() : "";
  const canValidate = role === "ADMIN" || role === "MANAGER";

  if (!loading && manager && !canValidate) {
    return (
      <div className="bg-amber-50 border-l-4 border-amber-500 rounded-2xl p-6 shadow-sm flex items-start gap-4">
        <ShieldAlert className="w-6 h-6 text-amber-600 shrink-0 mt-0.5" />
        <div>
          <h3 className="text-base font-bold text-amber-900">Accès Restreint — SPAT</h3>
          <p className="text-sm text-amber-700 mt-1">
            Seuls les Responsables Hiérarchiques (Managers) et Administrateurs RH sont habilités à traiter les demandes d&apos;autorisation d&apos;absence.
          </p>
        </div>
      </div>
    );
  }

  const getUserName = (userId) => {
    if (!userId) return "Collaborateur Inconnu";
    const found = utilisateurs.find((u) => u.id === userId);
    if (!found) return `Collaborateur #${userId}`;
    return found.prenom ? `${found.prenom} ${found.nom}` : found.email || `Collaborateur #${userId}`;
  };

  const getTypeLabel = (typeId) => {
    const found = typesConge.find((t) => t.id === typeId);
    return found?.libelle || found?.code || `Type #${typeId}`;
  };

  const calculateDays = (demande) => {
    if (!demande) return 1;
    if (demande.nombreJours) return Math.round(Number(demande.nombreJours));
    if (!demande.dateDebut || !demande.dateFin) return 1;
    const d1 = new Date(demande.dateDebut);
    const d2 = new Date(demande.dateFin);
    const diffTime = Math.abs(d2 - d1);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
    return isNaN(diffDays) ? 1 : diffDays;
  };

  const handleValider = async (demandeId) => {
    if (!manager?.id) {
      showToast("Validateur non identifié.", "warning");
      return;
    }
    setActionLoading(demandeId);
    try {
      await traiterDemandeConge(demandeId, manager.id, "VALIDEE");
      setDemandes((prev) =>
        prev.map((d) => (d.id === demandeId ? { ...d, statut: "VALIDEE" } : d))
      );
      showToast("Demande validée avec succès.", "success");
    } catch (err) {
      showToast(`Erreur lors de la validation : ${err.message}`, "error");
    } finally {
      setActionLoading(null);
    }
  };

  const handleOpenRefus = (demandeId) => {
    setModalRefus({ open: true, demandeId, motif: "" });
  };

  const handleConfirmRefus = async () => {
    if (!manager?.id || !modalRefus.demandeId) return;
    setActionLoading(modalRefus.demandeId);
    try {
      await traiterDemandeConge(
        modalRefus.demandeId,
        manager.id,
        "REFUSEE",
        modalRefus.motif
      );
      setDemandes((prev) =>
        prev.map((d) =>
          d.id === modalRefus.demandeId
            ? { ...d, statut: "REFUSEE", motifRefus: modalRefus.motif }
            : d
        )
      );
      showToast("Demande refusée.", "success");
      setModalRefus({ open: false, demandeId: null, motif: "" });
    } catch (err) {
      showToast(`Erreur lors du refus : ${err.message}`, "error");
    } finally {
      setActionLoading(null);
    }
  };

  const getStatusBadge = (statut) => {
    switch (statut) {
      case "VALIDEE":
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
            <CheckCircle2 className="w-3.5 h-3.5" />
            VALIDÉE
          </span>
        );
      case "REFUSEE":
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-bold bg-rose-50 text-rose-700 border border-rose-200">
            <XCircle className="w-3.5 h-3.5" />
            REFUSÉE
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-bold bg-amber-50 text-amber-700 border border-amber-200">
            <Clock className="w-3.5 h-3.5" />
            EN ATTENTE
          </span>
        );
    }
  };

  return (
    <div className="space-y-6">
      {/* Banner SPAT */}
      <div className="bg-slate-900 text-white rounded-2xl p-6 shadow-md border-l-4 border-blue-600 flex justify-between items-center flex-wrap gap-4">
        <div>
          <div className="text-xs font-semibold uppercase tracking-widest text-blue-400 mb-1">
            SPAT — Société du Port à Gestion Autonome de Toamasina
          </div>
          <h1 className="text-2xl font-bold tracking-tight flex items-center gap-2">
            <FileCheck2 className="w-6 h-6 text-blue-400" />
            Validation des Demandes d&apos;Absence &amp; Congés
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Espace d&apos;arbitrage hiérarchique pour l&apos;instruction et la validation des autorisations d&apos;absence.
          </p>
        </div>

        {/* Filtre d'affichage */}
        <div className="inline-flex p-1 bg-slate-800 rounded-xl border border-slate-700">
          <button
            onClick={() => setFilter("EN_ATTENTE")}
            className={`px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all flex items-center gap-1.5 ${
              filter === "EN_ATTENTE"
                ? "bg-blue-600 text-white shadow-sm"
                : "text-slate-300 hover:text-white"
            }`}
          >
            <Clock className="w-3.5 h-3.5" />
            En attente uniquement
          </button>
          <button
            onClick={() => setFilter("TOUTES")}
            className={`px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all flex items-center gap-1.5 ${
              filter === "TOUTES"
                ? "bg-blue-600 text-white shadow-sm"
                : "text-slate-300 hover:text-white"
            }`}
          >
            <Filter className="w-3.5 h-3.5" />
            Toutes les demandes
          </button>
        </div>
      </div>

      {/* Main Table Container */}
      <div className="bg-white shadow-sm border border-slate-200 rounded-2xl p-6">
        {loading ? (
          <div className="flex justify-center items-center py-16">
            <span className="loading loading-spinner loading-lg text-primary"></span>
          </div>
        ) : demandes.length === 0 ? (
          <div className="text-center py-16 text-slate-400">
            <div className="w-16 h-16 bg-slate-50 text-slate-400 rounded-2xl flex items-center justify-center mx-auto mb-3 border border-slate-100">
              <Inbox className="w-8 h-8" />
            </div>
            <p className="font-bold text-slate-700 text-base">Aucune demande enregistrée.</p>
            <p className="text-sm text-slate-400 mt-1">
              Aucun dossier ne requiert votre arbitrage pour ce filtre.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="table w-full">
              <thead>
                <tr className="bg-slate-50 text-slate-600 text-xs uppercase tracking-wider border-b border-slate-200">
                  <th className="py-3">Collaborateur</th>
                  <th className="py-3">Motif / Nature</th>
                  <th className="py-3">Période Demandée</th>
                  <th className="py-3">Durée</th>
                  <th className="py-3">Commentaire</th>
                  <th className="py-3">Statut Actuel</th>
                  <th className="py-3 text-center">Décision</th>
                </tr>
              </thead>
              <tbody>
                {demandes.map((demande) => {
                  const days = calculateDays(demande);
                  const isPending = demande.statut === "EN_ATTENTE";

                  return (
                    <tr
                      key={demande.id}
                      className="hover:bg-slate-50/80 transition-colors border-b border-slate-100"
                    >
                      {/* Agent */}
                      <td className="py-3.5">
                        <div className="flex items-center gap-2.5">
                          <div className="w-8 h-8 rounded-full bg-blue-50 text-blue-700 flex items-center justify-center font-bold text-xs border border-blue-100">
                            <User className="w-4 h-4" />
                          </div>
                          <div>
                            <p className="font-bold text-slate-800 text-sm">
                              {getUserName(demande.utilisateurId)}
                            </p>
                          </div>
                        </div>
                      </td>

                      {/* Type de congé */}
                      <td className="py-3.5 font-semibold text-slate-700 text-sm">
                        {getTypeLabel(demande.typeCongeId)}
                      </td>

                      {/* Dates */}
                      <td className="py-3.5 text-xs text-slate-600">
                        <div className="flex items-center gap-1.5 font-medium">
                          <Calendar className="w-3.5 h-3.5 text-slate-400" />
                          <span>{demande.dateDebut}</span>
                          <span className="text-slate-400">→</span>
                          <span>{demande.dateFin}</span>
                        </div>
                      </td>

                      {/* Durée */}
                      <td className="py-3.5">
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded text-xs font-bold bg-blue-50 text-blue-700 border border-blue-200">
                          {days} j
                        </span>
                      </td>

                      {/* Commentaire */}
                      <td className="py-3.5 text-xs text-slate-500 max-w-xs">
                        <div className="flex items-center gap-1 truncate" title={demande.commentaire}>
                          {demande.commentaire ? (
                            <>
                              <MessageSquare className="w-3 h-3 text-slate-400 shrink-0" />
                              <span className="truncate">{demande.commentaire}</span>
                            </>
                          ) : (
                            <span className="text-slate-300 italic">Aucun motif</span>
                          )}
                        </div>
                      </td>

                      {/* Statut */}
                      <td className="py-3.5">{getStatusBadge(demande.statut)}</td>

                      {/* Actions */}
                      <td className="py-3.5 text-center">
                        {isPending ? (
                          <div className="flex items-center justify-center gap-2">
                            <button
                              onClick={() => handleValider(demande.id)}
                              disabled={actionLoading === demande.id}
                              className="btn btn-xs btn-success text-white font-semibold gap-1 shadow-xs"
                            >
                              {actionLoading === demande.id ? (
                                <span className="loading loading-spinner loading-xs"></span>
                              ) : (
                                <>
                                  <Check className="w-3.5 h-3.5" />
                                  Valider
                                </>
                              )}
                            </button>
                            <button
                              onClick={() => handleOpenRefus(demande.id)}
                              disabled={actionLoading === demande.id}
                              className="btn btn-xs btn-error text-white font-semibold gap-1 shadow-xs"
                            >
                              <X className="w-3.5 h-3.5" />
                              Refuser
                            </button>
                          </div>
                        ) : (
                          <span className="text-xs text-slate-400 font-medium italic">
                            Dossier clos
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal de motif de refus */}
      {modalRefus.open && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6 space-y-4 text-slate-800 border border-slate-100">
            <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2 pb-2 border-b border-slate-100">
              <XCircle className="w-5 h-5 text-rose-600" />
              Confirmation de Refus de Congé
            </h3>
            <p className="text-xs text-slate-500 leading-relaxed">
              Veuillez préciser la raison motivant le rejet de cette demande d&apos;absence afin d&apos;en informer le collaborateur :
            </p>
            <textarea
              rows={3}
              value={modalRefus.motif}
              onChange={(e) => setModalRefus({ ...modalRefus, motif: e.target.value })}
              placeholder="Ex: Impératifs de service, effectif critique sur la période, solde insuffisant..."
              className="textarea textarea-bordered w-full text-sm text-slate-800 bg-white focus:outline-hidden"
            />
            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                onClick={() => setModalRefus({ open: false, demandeId: null, motif: "" })}
                className="btn btn-ghost btn-sm font-semibold"
              >
                Annuler
              </button>
              <button
                onClick={handleConfirmRefus}
                className="btn btn-error btn-sm text-white font-semibold gap-1"
              >
                <X className="w-4 h-4" />
                Confirmer le refus
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}