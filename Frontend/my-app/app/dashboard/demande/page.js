"use client";

import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { useEffect, useState } from "react";
import {
  annulerDemandeConge,
  creerDemandeConge,
  getMesDemandes,
} from "../../api/demandeConge/demandeConge.js";
import { getAllTypeConge } from "../../api/typeConge/typeConge.js";

import { getCurrentUser } from "@/lib/apiClient";

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
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const user = getCurrentUser();
        const currentUserId = Number(user?.id);
        if (!currentUserId) {
          throw new Error("Utilisateur connecté introuvable");
        }

        setUtilisateurId(currentUserId);
        const [demandesData, typesData] = await Promise.all([
          getMesDemandes(currentUserId),
          getAllTypeConge(),
        ]);
        setDemandes(Array.isArray(demandesData) ? demandesData : []);
        setTypesConge(Array.isArray(typesData) ? typesData : []);
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
    return type?.libelle || type?.code || demande?.typeConge?.libelle || `Type #${targetId || 'N/A'}`;
  };

  return (
    <div className="min-h-screen bg-slate-50/50 p-6 space-y-8 max-w-7xl mx-auto">
      {/* En-tête de section */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Mes Demandes de Congé
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Gestion et suivi de vos absences — Port de Toamasina (SPAT)
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Formulaire de demande (7 colonnes sur desktop) */}
        <section className="lg:col-span-7 bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <div className="bg-slate-900 px-6 py-4 text-white">
            <h2 className="font-semibold text-lg flex items-center gap-2">
              <svg className="w-5 h-5 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
              </svg>
              Nouvelle demande de congé
            </h2>
          </div>

          <form onSubmit={handleSubmit} className="p-6 space-y-5">
            {/* Type de congé */}
            <div>
              <label htmlFor="typeCongeId" className="block text-sm font-semibold text-slate-700 mb-1.5">
                Type de congé <span className="text-red-500">*</span>
              </label>
              <select
                id="typeCongeId"
                name="typeCongeId"
                value={formData.typeCongeId}
                onChange={(e) =>
                  setFormData({ ...formData, typeCongeId: e.target.value })
                }
                className="w-full rounded-lg border-slate-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-sm py-2.5 px-3 transition-colors border"
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

            {/* Dates début et fin */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="flex flex-col">
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">
                  Date de début <span className="text-red-500">*</span>
                </label>
                <div className="relative border border-slate-300 rounded-lg p-1.5 bg-slate-50 focus-within:bg-white focus-within:border-blue-500 focus-within:ring-1 focus-within:ring-blue-500 transition-all">
                  <DatePicker
                    selected={formData.debut}
                    onChange={(date) => setFormData({ ...formData, debut: date })}
                    dateFormat="yyyy-MM-dd"
                    className="w-full bg-transparent text-sm text-slate-800 outline-none cursor-pointer"
                    placeholderText="Choisir une date"
                  />
                </div>
              </div>

              <div className="flex flex-col">
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">
                  Date de fin <span className="text-red-500">*</span>
                </label>
                <div className="relative border border-slate-300 rounded-lg p-1.5 bg-slate-50 focus-within:bg-white focus-within:border-blue-500 focus-within:ring-1 focus-within:ring-blue-500 transition-all">
                  <DatePicker
                    selected={formData.fin}
                    onChange={(date) => setFormData({ ...formData, fin: date })}
                    dateFormat="yyyy-MM-dd"
                    className="w-full bg-transparent text-sm text-slate-800 outline-none cursor-pointer"
                    placeholderText="Choisir une date"
                  />
                </div>
              </div>
            </div>

            {/* Commentaire */}
            <div>
              <label htmlFor="commentaire" className="block text-sm font-semibold text-slate-700 mb-1.5">
                Commentaire ou justification <span className="text-red-500">*</span>
              </label>
              <textarea
                id="commentaire"
                name="commentaire"
                value={formData.commentaire}
                onChange={(e) =>
                  setFormData({ ...formData, commentaire: e.target.value })
                }
                className="w-full rounded-lg border-slate-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-sm p-3 border resize-none transition-colors"
                rows="3"
                placeholder="Saisissez un motif ou une précision concernant votre absence..."
                required
              />
            </div>

            {/* Bouton de soumission */}
            <div className="pt-2">
              <button
                type="submit"
                disabled={loading}
                className="w-full sm:w-auto inline-flex justify-center items-center gap-2 bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white font-medium px-6 py-2.5 rounded-lg shadow-sm transition-all disabled:opacity-50 text-sm"
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

        {/* Historique des demandes (5 colonnes sur desktop) */}
        <section className="lg:col-span-5 bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50">
            <h2 className="font-semibold text-slate-800 text-lg flex items-center gap-2">
              <svg className="w-5 h-5 text-slate-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              Historique
            </h2>
            <span className="text-xs font-semibold px-2.5 py-1 bg-slate-200 text-slate-700 rounded-full">
              {demandes.length}
            </span>
          </div>

          <div className="p-6">
            {demandes.length === 0 ? (
              <div className="text-center py-8">
                <svg className="mx-auto h-12 w-12 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <p className="mt-2 text-sm text-slate-500 font-medium">Aucune demande enregistrée pour l`instant.</p>
              </div>
            ) : (
              <ul className="space-y-3">
                {demandes.map((demande) => {
                  const isEnAttente = demande.statut === "EN_ATTENTE";
                  const isValide = demande.statut === "VALIDE" || demande.statut === "APPROUVE";
                  const isRefuse = demande.statut === "REFUSE" || demande.statut === "ANNULE";

                  return (
                    <li
                      key={demande.id}
                      className="p-4 rounded-lg border border-slate-100 bg-slate-50/50 hover:bg-slate-50 transition-colors flex flex-col justify-between gap-3"
                    >
                      <div className="flex justify-between items-start gap-2">
                        <div>
                          <p className="font-semibold text-slate-900 text-sm">
                            {getTypeLabel(demande)}
                          </p>
                          <p className="text-xs text-slate-500 mt-1 flex items-center gap-1">
                            <svg className="w-3.5 h-3.5 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                            </svg>
                            {demande.dateDebut || demande.debut} → {demande.dateFin || demande.fin}
                          </p>
                        </div>

                        {/* Badge de statut */}
                        <span
                          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${
                            isEnAttente
                              ? "bg-amber-50 text-amber-800 border-amber-200"
                              : isValide
                              ? "bg-emerald-50 text-emerald-800 border-emerald-200"
                              : isRefuse
                              ? "bg-rose-50 text-rose-800 border-rose-200"
                              : "bg-slate-100 text-slate-800 border-slate-200"
                          }`}
                        >
                          <span
                            className={`w-1.5 h-1.5 rounded-full mr-1.5 ${
                              isEnAttente
                                ? "bg-amber-500"
                                : isValide
                                ? "bg-emerald-500"
                                : isRefuse
                                ? "bg-rose-500"
                                : "bg-slate-400"
                            }`}
                          />
                          {demande.statut}
                        </span>
                      </div>

                      {/* Action Annuler */}
                      {isEnAttente && (
                        <div className="pt-2 border-t border-slate-100 flex justify-end">
                          <button
                            onClick={() => handleAnnuler(demande.id)}
                            className="text-xs text-rose-600 hover:text-rose-700 hover:bg-rose-50 font-medium px-2.5 py-1 rounded transition-colors"
                          >
                            Annuler la demande
                          </button>
                        </div>
                      )}
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}