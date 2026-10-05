"use client";

import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { useEffect, useMemo, useState } from "react";
import { getJoursFeries } from "@/app/api/joursFeries/joursFeries";
import {
  annulerDemandeConge,
  creerDemandeConge,
  getMesDemandes,
} from "@/app/api/demandeConge/demandeConge";
import { getAllTypeConge } from "@/app/api/typeConge/typeConge";
import { getCurrentUser } from "@/lib/apiClient";

const toLocalDateKey = (date) => {
  if (!date) return "";
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const computeWorkingDays = (start, end, holidays = []) => {
  if (!start || !end) return 0;

  const startDate = new Date(start);
  const endDate = new Date(end);
  const holidaySet = new Set(
    holidays
      .map((holiday) => {
        const rawDate = holiday?.date || holiday?.jour || holiday?.libelle;
        if (!rawDate) return null;
        const parsed = new Date(rawDate);
        return Number.isNaN(parsed.getTime()) ? null : toLocalDateKey(parsed);
      })
      .filter(Boolean)
  );

  let count = 0;
  const cursor = new Date(startDate);
  cursor.setHours(0, 0, 0, 0);
  endDate.setHours(0, 0, 0, 0);

  while (cursor <= endDate) {
    const day = cursor.getDay();
    const dateKey = toLocalDateKey(cursor);
    if (day !== 0 && day !== 6 && !holidaySet.has(dateKey)) {
      count += 1;
    }
    cursor.setDate(cursor.getDate() + 1);
  }

  return count;
};

export default function DemandesPage() {
  const [utilisateurId, setUtilisateurId] = useState(null);
  const [formData, setFormData] = useState({
    typeCongeId: "",
    debut: null,
    fin: null,
    commentaire: "",
  });
  const [demandes, setDemandes] = useState([]);
  const [typesConge, setTypesConge] = useState([]);
  const [holidays, setHolidays] = useState([]);
  const [loading, setLoading] = useState(false);

  const workingDaysSummary = useMemo(() => {
    if (!formData.debut || !formData.fin) return { calendarDays: 0, workingDays: 0 };
    const start = new Date(formData.debut);
    const end = new Date(formData.fin);
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return { calendarDays: 0, workingDays: 0 };

    const calendarDays = Math.max(0, Math.ceil((end - start) / (1000 * 60 * 60 * 24)) + 1);
    const workingDays = computeWorkingDays(start, end, holidays);
    return { calendarDays, workingDays };
  }, [formData.debut, formData.fin, holidays]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const user = getCurrentUser();
        const currentUserId = Number(user?.id);
        if (!currentUserId) {
          throw new Error("Utilisateur connecté introuvable");
        }

        setUtilisateurId(currentUserId);
        const [demandesData, typesData, holidayData] = await Promise.all([
          getMesDemandes(currentUserId),
          getAllTypeConge(),
          getJoursFeries(),
        ]);

        setDemandes(Array.isArray(demandesData) ? demandesData : []);
        setTypesConge(Array.isArray(typesData) ? typesData : []);
        setHolidays(Array.isArray(holidayData) ? holidayData : []);
      } catch (err) {
        console.error("Erreur lors du chargement des demandes:", err);
      }
    };

    fetchData();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!utilisateurId) {
      alert("Utilisateur connecté introuvable");
      return;
    }
    if (!formData.typeCongeId || !formData.debut || !formData.fin) {
      alert("Veuillez renseigner le type et les deux dates de congé");
      return;
    }
    if (formData.fin < formData.debut) {
      alert("La date de fin doit être postérieure ou égale à la date de début");
      return;
    }

    setLoading(true);
    try {
      const nouvelleDemande = await creerDemandeConge(utilisateurId, formData);
      setDemandes((currentDemandes) => [...currentDemandes, nouvelleDemande]);
      setFormData({
        typeCongeId: "",
        debut: null,
        fin: null,
        commentaire: "",
      });
    } catch (err) {
      console.error("Erreur création demande:", err);
      alert(`Erreur lors de la création de la demande: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleAnnuler = async (demandeId) => {
    if (!utilisateurId) return;
    try {
      const updated = await annulerDemandeConge(demandeId, utilisateurId);
      setDemandes((currentDemandes) =>
        currentDemandes.map((demande) =>
          demande.id === demandeId ? updated : demande,
        ),
      );
    } catch (err) {
      console.error("Erreur annulation demande:", err);
      alert("Erreur lors de l'annulation");
    }
  };

  const getTypeLabel = (demande) => {
    const targetId = demande?.typeCongeId || demande?.typeConge?.id;
    const type = typesConge.find((item) => String(item.id) === String(targetId));
    return type?.libelle || type?.code || demande?.typeConge?.libelle || `Type #${targetId || "N/A"}`;
  };

  return (
    <div className="min-h-screen p-6 space-y-8 max-w-7xl mx-auto" style={{ background: "transparent" }}>
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b pb-5" style={{ borderColor: "var(--border)" }}>
        <div>
          <h1 className="text-2xl font-bold tracking-tight" style={{ color: "var(--text-main)" }}>
            Mes Demandes de Congé
          </h1>
          <p className="text-sm mt-1" style={{ color: "var(--text-muted)" }}>
            Gestion et suivi de vos absences — Port de Toamasina (SPAT)
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        <section className="lg:col-span-7 rounded-xl shadow-sm border overflow-hidden" style={{ background: "var(--panel)", borderColor: "var(--border)" }}>
          <div className="px-6 py-4 text-white" style={{ background: "linear-gradient(135deg, #0b1f3a, #123a6d)" }}>
            <h2 className="font-semibold text-lg flex items-center gap-2">
              <svg className="w-5 h-5 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
              </svg>
              Nouvelle demande de congé
            </h2>
          </div>

          <form onSubmit={handleSubmit} className="p-6 space-y-5">
            <div>
              <label htmlFor="typeCongeId" className="block text-sm font-semibold mb-1.5" style={{ color: "var(--text-main)" }}>
                Type de congé <span className="text-red-500">*</span>
              </label>
              <select
                id="typeCongeId"
                name="typeCongeId"
                value={formData.typeCongeId}
                onChange={(e) => setFormData({ ...formData, typeCongeId: e.target.value })}
                className="w-full rounded-lg shadow-sm text-sm py-2.5 px-3 transition-colors border"
                style={{ background: "var(--panel-soft)", borderColor: "var(--border)", color: "var(--text-main)" }}
                required
              >
                <option value="">Sélectionner un type de congé</option>
                {typesConge.map((type) => (
                  <option key={type.id} value={type.id}>
                    {type.libelle || type.code}
                  </option>
                ))}
              </select>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="flex flex-col">
                <label className="block text-sm font-semibold mb-1.5" style={{ color: "var(--text-main)" }}>
                  Date de début <span className="text-red-500">*</span>
                </label>
                <div className="relative border rounded-lg p-1.5 focus-within:ring-1 transition-all" style={{ background: "var(--panel-soft)", borderColor: "var(--border)" }}>
                  <DatePicker
                    selected={formData.debut}
                    onChange={(date) => setFormData({ ...formData, debut: date })}
                    dateFormat="yyyy-MM-dd"
                    className="w-full bg-transparent text-sm outline-none cursor-pointer"
                    placeholderText="Choisir une date"
                    style={{ color: "var(--text-main)" }}
                  />
                </div>
              </div>

              <div className="flex flex-col">
                <label className="block text-sm font-semibold mb-1.5" style={{ color: "var(--text-main)" }}>
                  Date de fin <span className="text-red-500">*</span>
                </label>
                <div className="relative border rounded-lg p-1.5 focus-within:ring-1 transition-all" style={{ background: "var(--panel-soft)", borderColor: "var(--border)" }}>
                  <DatePicker
                    selected={formData.fin}
                    onChange={(date) => setFormData({ ...formData, fin: date })}
                    dateFormat="yyyy-MM-dd"
                    className="w-full bg-transparent text-sm outline-none cursor-pointer"
                    placeholderText="Choisir une date"
                    style={{ color: "var(--text-main)" }}
                  />
                </div>
              </div>
            </div>

            {(formData.debut || formData.fin) && (
              <div className="rounded-xl border p-4" style={{ background: "var(--primary-soft)", borderColor: "var(--primary-border)" }}>
                <div className="text-sm font-semibold" style={{ color: "var(--primary)" }}>
                  Calcul des jours ouvrés
                </div>
                <div className="mt-2 flex flex-wrap gap-3 text-sm" style={{ color: "var(--text-main)" }}>
                  <span className="badge badge-ghost">{workingDaysSummary.calendarDays} jours calendaires</span>
                  <span className="badge badge-info text-white">{workingDaysSummary.workingDays} jours ouvrés</span>
                </div>
              </div>
            )}

            <div>
              <label htmlFor="commentaire" className="block text-sm font-semibold mb-1.5" style={{ color: "var(--text-main)" }}>
                Commentaire ou justification <span className="text-red-500">*</span>
              </label>
              <textarea
                id="commentaire"
                name="commentaire"
                value={formData.commentaire}
                onChange={(e) => setFormData({ ...formData, commentaire: e.target.value })}
                className="w-full rounded-lg shadow-sm text-sm p-3 border resize-none transition-colors"
                rows="3"
                placeholder="Saisissez un motif ou une précision concernant votre absence..."
                style={{ background: "var(--panel-soft)", borderColor: "var(--border)", color: "var(--text-main)" }}
                required
              />
            </div>

            <div className="pt-2">
              <button
                type="submit"
                disabled={loading}
                className="w-full sm:w-auto inline-flex justify-center items-center gap-2 text-white font-medium px-6 py-2.5 rounded-lg shadow-sm transition-all disabled:opacity-50 text-sm"
                style={{ background: "linear-gradient(135deg, #0b3d74, #2563eb)" }}
              >
                {loading ? (
                  <>
                    <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                    </svg>
                    Traitement en cours...
                  </>
                ) : (
                  <>
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                    </svg>
                    Soumettre la demande
                  </>
                )}
              </button>
            </div>
          </form>
        </section>

        <section className="lg:col-span-5 rounded-xl shadow-sm border overflow-hidden" style={{ background: "var(--panel)", borderColor: "var(--border)" }}>
          <div className="px-6 py-4 border-b flex items-center justify-between" style={{ background: "var(--panel-alt)", borderColor: "var(--border)" }}>
            <h2 className="font-semibold text-lg flex items-center gap-2" style={{ color: "var(--text-main)" }}>
              <svg className="w-5 h-5" style={{ color: "var(--text-muted)" }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              Historique
            </h2>
          </div>

          <div className="p-4 space-y-3 max-h-[760px] overflow-y-auto">
            {demandes.length === 0 ? (
              <div className="rounded-lg border p-5 text-sm" style={{ background: "var(--panel-soft)", borderColor: "var(--border)", color: "var(--text-muted)" }}>
                Aucune demande pour le moment.
              </div>
            ) : (
              demandes.map((demande) => (
                <div key={demande.id} className="rounded-lg border p-4" style={{ background: "var(--panel-soft)", borderColor: "var(--border)" }}>
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <div className="font-semibold" style={{ color: "var(--text-main)" }}>{getTypeLabel(demande)}</div>
                      <div className="text-xs mt-1" style={{ color: "var(--text-muted)" }}>
                        {demande.dateDebut} → {demande.dateFin}
                      </div>
                    </div>
                    <span className="badge badge-sm uppercase" style={{ background: demande.statut === "VALIDEE" ? "rgba(16,185,129,0.12)" : demande.statut === "REFUSEE" ? "rgba(239,68,68,0.12)" : "rgba(59,130,246,0.12)", color: "var(--text-main)" }}>
                      {demande.statut}
                    </span>
                  </div>

                  <p className="mt-3 text-sm" style={{ color: "var(--text-muted)" }}>
                    {demande.commentaire || "Aucun commentaire."}
                  </p>

                  {demande.statut === "EN_ATTENTE" && (
                    <button
                      type="button"
                      onClick={() => handleAnnuler(demande.id)}
                      className="mt-4 btn btn-sm btn-outline"
                    >
                      Annuler la demande
                    </button>
                  )}
                </div>
              ))
            )}
          </div>
        </section>
      </div>
    </div>
  );
}