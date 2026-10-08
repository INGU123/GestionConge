"use client";

import { useEffect, useState } from "react";
import {
  getAllHistorique,
  getHistoriquePdf,
  getHistoriqueUtilisateur,
} from "../../api/historiqueMouvements/historiqueMouvement";
import { getAllTypeConge } from "../../api/typeConge/typeConge";
import { getCurrentUser, authFetch } from "@/lib/apiClient";
import {
  History,
  User,
  ArrowUpRight,
  ArrowDownLeft,
  Calendar,
  MessageSquare,
  Users,
  Layers,
  ShieldCheck,
  Download,
} from "lucide-react";
import { useToast } from "../../components/ToastProvider";

export default function HistoriquePage() {
  const [currentUser, setCurrentUser] = useState(null);
  const [mouvements, setMouvements] = useState([]);
  const [typesConge, setTypesConge] = useState([]);
  const [utilisateurs, setUtilisateurs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [viewAll, setViewAll] = useState(false);
  const [exporting, setExporting] = useState(false);
  const showToast = useToast();

  useEffect(() => {
    let isMounted = true;

    async function fetchData() {
      const user = getCurrentUser();
      if (!user) {
        if (isMounted) setLoading(false);
        return;
      }

      if (isMounted) {
        setCurrentUser(user);
        const isManagerRole = user.role === "ADMIN" || user.role === "MANAGER";
        setViewAll(isManagerRole);
      }

      try {
        if (isMounted) setLoading(true);
        const isManager = user.role === "ADMIN" || user.role === "MANAGER";

        const [mouvementsData, typesData, usersRes] = await Promise.all([
          viewAll && isManager
            ? getAllHistorique()
            : getHistoriqueUtilisateur(user.id),
          getAllTypeConge().catch(() => []),
          authFetch("http://localhost:8080/utilisateur/all")
            .then((r) => (r.ok ? r.json() : []))
            .catch(() => []),
        ]);

        if (isMounted) {
          setMouvements(Array.isArray(mouvementsData) ? mouvementsData : []);
          setTypesConge(Array.isArray(typesData) ? typesData : []);
          setUtilisateurs(Array.isArray(usersRes) ? usersRes : []);
        }
      } catch (err) {
        console.error("Erreur lors du chargement :", err);
        if (isMounted) showToast("Impossible de charger l'historique.", "error");
        if (isMounted) setMouvements([]);
      } finally {
        if (isMounted) setLoading(false);
      }
    }

    fetchData();

    return () => {
      isMounted = false;
    };
  }, [viewAll, showToast]);

  const getUserName = (userId) => {
    const found = utilisateurs.find((u) => u.id === userId);
    if (found) return found.prenom ? `${found.prenom} ${found.nom || ""}` : found.email;
    if (String(currentUser?.id) === String(userId)) {
      const name = `${currentUser.prenom || ""} ${currentUser.nom || ""}`.trim();
      return name || currentUser.email || `Utilisateur #${userId}`;
    }
    return `Utilisateur #${userId}`;
  };

  const getTypeLabel = (typeId) => {
    const found = typesConge.find((t) => t.id === typeId);
    return found?.libelle || found?.code || `Type #${typeId}`;
  };

  const isManager = currentUser?.role === "ADMIN" || currentUser?.role === "MANAGER";

  const handleExportPdf = async () => {
    setExporting(true);
    try {
      const blob = await getHistoriquePdf(viewAll && isManager ? "all" : "mine");
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      const today = new Date();
      const date = [
        today.getFullYear(),
        String(today.getMonth() + 1).padStart(2, "0"),
        String(today.getDate()).padStart(2, "0"),
      ].join("-");
      link.href = url;
      link.download = `historique_conges_${date}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
      showToast("L'historique a été exporté en PDF.", "success");
    } catch (error) {
      console.error("Erreur lors de l'export PDF de l'historique :", error);
      showToast(error.message || "Impossible d'exporter l'historique en PDF.", "error");
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Banner Institutionnelle SPAT */}
      <div className="bg-slate-900 text-white rounded-2xl p-6 shadow-md border-l-4 border-blue-600 flex justify-between items-center flex-wrap gap-4">
        <div>
          <div className="text-xs font-semibold uppercase tracking-widest text-blue-400 mb-1">
            SPAT — Société du Port à Gestion Autonome de Toamasina
          </div>
          <h1 className="text-2xl font-bold tracking-tight flex items-center gap-2">
            <History className="w-6 h-6 text-blue-400" />
            {`Historique des Mouvements & Journal d'Audit`}
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Traçabilité complète des débits, crédits, ajustements et régularisations de soldes de congés.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          {isManager && (
            <div className="inline-flex p-1 bg-slate-800 rounded-xl border border-slate-700">
              <button
                onClick={() => setViewAll(false)}
                className={`px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all flex items-center gap-1.5 ${
                  !viewAll
                    ? "bg-blue-600 text-white shadow-sm"
                    : "text-slate-300 hover:text-white"
                }`}
              >
                <User className="w-3.5 h-3.5" />
                Mes mouvements
              </button>
              <button
                onClick={() => setViewAll(true)}
                className={`px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all flex items-center gap-1.5 ${
                  viewAll
                    ? "bg-blue-600 text-white shadow-sm"
                    : "text-slate-300 hover:text-white"
                }`}
              >
                <Users className="w-3.5 h-3.5" />
                Tous les collaborateurs
              </button>
            </div>
          )}
          <button
            type="button"
            onClick={handleExportPdf}
            disabled={exporting || loading}
            className="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {exporting ? (
              <span className="loading loading-spinner loading-xs" />
            ) : (
              <Download className="h-4 w-4" />
            )}
            {exporting ? "Export en cours..." : "Exporter en PDF"}
          </button>
        </div>
      </div>

      {/* Tableau des mouvements */}
      <div className="bg-white shadow-sm border border-slate-200 rounded-2xl p-6">
        {loading ? (
          <div className="flex justify-center items-center py-16">
            <span className="loading loading-spinner loading-lg text-primary"></span>
          </div>
        ) : mouvements.length === 0 ? (
          <div className="text-center py-16 text-slate-400">
            <History className="w-12 h-12 mx-auto mb-2 text-slate-300" />
            <p className="font-semibold text-slate-600">Aucun mouvement enregistré.</p>
            <p className="text-sm mt-1 text-slate-400">
              Les mouvements apparaîtront lors de la validation des congés ou ajustements de soldes.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="table w-full">
              <thead>
                <tr className="bg-slate-50 text-slate-600 text-xs uppercase tracking-wider border-b border-slate-200">
                  <th className="py-3">Date</th>
                  <th className="py-3">Collaborateur</th>
                  <th className="py-3">Type de Mouvement</th>
                  <th className="py-3">Type de Congé</th>
                  <th className="py-3">Quantité</th>
                  <th className="py-3">Commentaire</th>
                  <th className="py-3">Effectué par</th>
                </tr>
              </thead>
              <tbody>
                {mouvements.slice().reverse().map((m, idx) => {
                  const isDebit = m.typeMouvement === "DEBIT_CONGE";

                  return (
                    <tr key={m.id || idx} className="hover:bg-slate-50/80 transition-colors border-b border-slate-100">
                      {/* Date */}
                      <td className="py-3.5 text-xs text-slate-600 font-medium">
                        <div className="flex items-center gap-1.5">
                          <Calendar className="w-3.5 h-3.5 text-slate-400" />
                          {m.date ? new Date(m.date).toLocaleDateString("fr-FR") : "-"}
                        </div>
                      </td>

                      {/* Collaborateur */}
                      <td className="py-3.5">
                        <div className="flex items-center gap-2">
                          <div className="w-7 h-7 rounded-full bg-slate-100 text-slate-600 flex items-center justify-center font-bold text-xs border border-slate-200">
                            <User className="w-3.5 h-3.5" />
                          </div>
                          <span className="font-bold text-slate-800 text-sm">
                            {getUserName(m.utilisateurId)}
                          </span>
                        </div>
                      </td>

                      {/* Type Mouvement */}
                      <td className="py-3.5">
                        {isDebit ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded text-xs font-bold bg-rose-50 text-rose-700 border border-rose-200">
                            <ArrowDownLeft className="w-3.5 h-3.5 text-rose-600" />
                            DÉBIT
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            <ArrowUpRight className="w-3.5 h-3.5 text-emerald-600" />
                            CRÉDIT
                          </span>
                        )}
                      </td>

                      {/* Type Congé */}
                      <td className="py-3.5 font-semibold text-slate-700 text-sm">
                        <div className="flex items-center gap-1.5">
                          <Layers className="w-3.5 h-3.5 text-slate-400" />
                          {getTypeLabel(m.type_conge_id)}
                        </div>
                      </td>

                      {/* Quantité */}
                      <td className="py-3.5">
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded text-xs font-bold ${
                          isDebit 
                            ? "bg-rose-50 text-rose-700 border border-rose-200" 
                            : "bg-blue-50 text-blue-700 border border-blue-200"
                        }`}>
                          {isDebit ? `-${m.quantite}` : `+${m.quantite}`} j
                        </span>
                      </td>

                      {/* Commentaire */}
                      <td className="py-3.5 text-xs text-slate-500 max-w-xs">
                        <div className="flex items-center gap-1 truncate" title={m.commentaire}>
                          {m.commentaire ? (
                            <>
                              <MessageSquare className="w-3 h-3 text-slate-400 shrink-0" />
                              <span className="truncate">{m.commentaire}</span>
                            </>
                          ) : (
                            <span className="text-slate-300 italic">-</span>
                          )}
                        </div>
                      </td>

                      {/* Effectué par */}
                      <td className="py-3.5 text-xs text-slate-600 font-medium">
                        <div className="flex items-center gap-1">
                          <ShieldCheck className="w-3.5 h-3.5 text-slate-400" />
                          {m.effectue_par ? getUserName(m.effectue_par) : "Système"}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}