"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  CalendarDays,
  Clock,
  CheckCircle2,
  XCircle,
  FileText,
  PieChart,
  Eye
} from "lucide-react";

import { getMesDemandes } from "../api/demandeConge/demandeConge";
import { getSoldesUtilisateur } from "../api/soldeConge/soldeConge";
import { getAllTypeConge } from "../api/typeConge/typeConge";
import { getCurrentUser } from "@/lib/apiClient";

export default function Dashboard() {
  const [user, setUser] = useState(null);
  const [demandes, setDemandes] = useState([]);
  const [soldes, setSoldes] = useState([]);
  const [typesConge, setTypesConge] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const currentUser = getCurrentUser();
        if (!currentUser) {
          setLoading(false);
          return;
        }
        setUser(currentUser);

        const currentUserId = Number(currentUser.id);
        if (currentUserId) {
          const [demandesData, soldesData, typesData] = await Promise.all([
            getMesDemandes(currentUserId).catch(() => []),
            getSoldesUtilisateur(currentUserId).catch(() => []),
            getAllTypeConge().catch(() => []),
          ]);
          setDemandes(Array.isArray(demandesData) ? demandesData : []);
          setSoldes(Array.isArray(soldesData) ? soldesData : []);
          setTypesConge(Array.isArray(typesData) ? typesData : []);
        }
      } catch (err) {
        console.error("Erreur chargement dashboard:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  const totalRestant = soldes.reduce(
    (acc, s) => acc + (Number(s.soldeRestant) || 0),
    0
  );
  const enAttenteCount = demandes.filter((d) => d.statut === "EN_ATTENTE").length;
  const valideCount = demandes.filter((d) => d.statut === "VALIDEE").length;
  const refuseCount = demandes.filter((d) => d.statut === "REFUSEE").length;
  const isAdmin = ["ADMIN", "ROLE_ADMIN"].includes(user?.role?.toUpperCase());

  const getTypeLabel = (typeId) => {
    const found = typesConge.find((t) => t.id === typeId);
    return found?.libelle || found?.code || `Type #${typeId}`;
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <span className="loading loading-spinner loading-lg text-primary"></span>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Dynamic Institution Banner */}
      <div className="bg-slate-900 text-white rounded-2xl p-6 shadow-md border-l-4 border-blue-600 flex justify-between items-center flex-wrap gap-4">
        <div>
          <div className="text-xs font-semibold uppercase tracking-widest text-blue-400 mb-1">
            SPAT — Gestion des Congés & Permissions
          </div>
          <h1 className="text-2xl font-bold tracking-tight">
            Bienvenue, {user?.prenom ? `${user.prenom} ${user.nom || ""}` : user?.email || "Agent SPAT"}
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Consultation de vos soldes réglementaires et état du traitement de vos demandes.
          </p>
        </div>
        <div className="flex gap-3">
          {!isAdmin && (
            <Link
              href="/dashboard/demande"
              className="btn btn-primary btn-sm font-semibold gap-2 shadow"
            >
              Nouvelle demande
            </Link>
          )}
          <Link
            href="/dashboard/solde"
            className="btn btn-outline btn-sm text-slate-200 border-slate-600 hover:bg-slate-800 hover:text-white gap-2"
          >
            <Eye className="w-4 h-4" />
            Consulter mes soldes
          </Link>
        </div>
      </div>

      {/* KPI Key Indicators */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200 flex items-center justify-between">
          <div>
            <p className="text-xs font-bold uppercase text-slate-500 tracking-wider">
              Solde Restant Global
            </p>
            <p className="text-3xl font-bold text-slate-800 mt-1">
              {totalRestant}{" "}
              <span className="text-sm font-normal text-slate-500">jours</span>
            </p>
          </div>
          <div className="w-12 h-12 rounded-lg bg-blue-50 text-blue-700 flex items-center justify-center border border-blue-100">
            <CalendarDays className="w-6 h-6" />
          </div>
        </div>

        <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200 flex items-center justify-between">
          <div>
            <p className="text-xs font-bold uppercase text-slate-500 tracking-wider">
              {` En cours d'instruction`}
            </p>
            <p className="text-3xl font-bold text-amber-600 mt-1">
              {enAttenteCount}
            </p>
          </div>
          <div className="w-12 h-12 rounded-lg bg-amber-50 text-amber-700 flex items-center justify-center border border-amber-100">
            <Clock className="w-6 h-6" />
          </div>
        </div>

        <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200 flex items-center justify-between">
          <div>
            <p className="text-xs font-bold uppercase text-slate-500 tracking-wider">
              Demandes Validées
            </p>
            <p className="text-3xl font-bold text-emerald-600 mt-1">
              {valideCount}
            </p>
          </div>
          <div className="w-12 h-12 rounded-lg bg-emerald-50 text-emerald-700 flex items-center justify-center border border-emerald-100">
            <CheckCircle2 className="w-6 h-6" />
          </div>
        </div>

        <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200 flex items-center justify-between">
          <div>
            <p className="text-xs font-bold uppercase text-slate-500 tracking-wider">
              Demandes Refusées
            </p>
            <p className="text-3xl font-bold text-rose-600 mt-1">
              {refuseCount}
            </p>
          </div>
          <div className="w-12 h-12 rounded-lg bg-rose-50 text-rose-700 flex items-center justify-center border border-rose-100">
            <XCircle className="w-6 h-6" />
          </div>
        </div>
      </div>

      {/* Tables section : Recent Demandes & Solde Summary */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recent Demandes */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5">
          <div className="flex justify-between items-center mb-4 pb-2 border-b border-slate-100">
            <h2 className="text-base font-bold text-slate-800 flex items-center gap-2">
              <FileText className="w-5 h-5 text-blue-600" />
              Dernières Demandes
            </h2>
            <Link
              href="/dashboard/demande"
              className="text-xs font-semibold text-blue-700 hover:text-blue-900"
            >
              {`Voir tout l'historique`} →
            </Link>
          </div>

          {demandes.length === 0 ? (
            <div className="text-center py-8 text-slate-400 text-sm">
              {`Aucune demande enregistrée dans l'historique.`}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="table table-sm w-full">
                <thead>
                  <tr className="text-slate-500 border-b text-xs">
                    <th>Type de congé</th>
                    <th>Période</th>
                    <th>Statut</th>
                  </tr>
                </thead>
                <tbody>
                  {demandes.slice(-5).reverse().map((d) => (
                    <tr key={d.id} className="hover:bg-slate-50/80 border-b border-slate-100">
                      <td className="font-medium text-slate-800">
                        {getTypeLabel(d.typeCongeId)}
                      </td>
                      <td className="text-xs text-slate-600">
                        {d.dateDebut} → {d.dateFin}
                      </td>
                      <td>
                        <span
                          className={`badge badge-sm font-semibold ${d.statut === "VALIDEE"
                              ? "badge-success text-white"
                              : d.statut === "REFUSEE"
                                ? "badge-error text-white"
                                : d.statut === "ANNULEE"
                                  ? "badge-ghost text-slate-600"
                                  : "badge-warning text-slate-800"
                            }`}
                        >
                          {d.statut === "EN_ATTENTE" ? "En attente" : d.statut}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Soldes breakdown */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5">
          <div className="flex justify-between items-center mb-4 pb-2 border-b border-slate-100">
            <h2 className="text-base font-bold text-slate-800 flex items-center gap-2">
              <PieChart className="w-5 h-5 text-blue-600" />
              Répartition des Soldes
            </h2>
            <Link
              href="/dashboard/solde"
              className="text-xs font-semibold text-blue-700 hover:text-blue-900"
            >
              Consulter le détail →
            </Link>
          </div>

          {soldes.length === 0 ? (
            <div className="text-center py-8 text-slate-400 text-sm">
              Aucun solde de congé initialisé.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="table table-sm w-full">
                <thead>
                  <tr className="text-slate-500 border-b text-xs">
                    <th>Motif / Type</th>
                    <th>Droits</th>
                    <th>Consommés</th>
                    <th>Solde restant</th>
                  </tr>
                </thead>
                <tbody>
                  {soldes.map((s) => (
                    <tr key={s.id} className="hover:bg-slate-50/80 border-b border-slate-100">
                      <td className="font-medium text-slate-800">
                        {getTypeLabel(s.typeCongeId)}
                      </td>
                      <td className="text-slate-600 font-medium">
                        {s.soldeAquis ?? s.soldeAcquis ?? 0} j
                      </td>
                      <td className="text-slate-600 font-medium">
                        {s.soldePris ?? 0} j
                      </td>
                      <td className="text-blue-700 font-bold">
                        {s.soldeRestant ?? 0} j
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}