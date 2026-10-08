"use client";

import { useEffect, useState } from "react";
import {
  getAllTypeConge,
  creerTypeConge,
  deleteTypeConge,
} from "../../api/typeConge/typeConge";
import {
  FolderKanban,
  PlusCircle,
  Trash2,
  FileText,
  Check,
  X,
  Layers
} from "lucide-react";
import { useToast } from "../../components/ToastProvider";
import ConfirmDialog from "../../components/ConfirmDialog";

export default function TypeCongePage() {
  const [types, setTypes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [typeToDelete, setTypeToDelete] = useState(null);
  const [deletingTypeId, setDeletingTypeId] = useState(null);
  const showToast = useToast();

  const [formData, setFormData] = useState({
    code: "",
    libelle: "",
    nombreJoursParAn: 30,
    regle_acquisition: "STANDARD",
    justificatifObligatoire: false,
    couleur: "#2563eb",
    actif: true,
  });

  useEffect(() => {
    const fetchTypes = async () => {
      try {
        setLoading(true);
        const data = await getAllTypeConge();
        setTypes(Array.isArray(data) ? data : []);
      } catch (err) {
        console.error("Erreur récupération types:", err);
        showToast(`Impossible de charger les types de congé : ${err.message}`, "error");
      } finally {
        setLoading(false);
      }
    };

    void fetchTypes();
  }, [showToast]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleAddType = async (e) => {
    e.preventDefault();
    if (!formData.code.trim() || !formData.libelle.trim()) {
      showToast("Veuillez renseigner le code et le libellé.", "warning");
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        code: formData.code.trim().toUpperCase(),
        libelle: formData.libelle.trim(),
        nombreJoursParAn: Math.round(Number(formData.nombreJoursParAn)) || 0,
        regle_acquisition: formData.regle_acquisition || "STANDARD",
        justificatifObligatoire: !!formData.justificatifObligatoire,
        couleur: formData.couleur || "#2563eb",
        actif: formData.actif !== false,
      };
      const created = await creerTypeConge(payload);
      setTypes((prev) => [...prev, created]);
      setFormData({
        code: "",
        libelle: "",
        nombreJoursParAn: 30,
        regle_acquisition: "STANDARD",
        justificatifObligatoire: false,
        couleur: "#2563eb",
        actif: true,
      });
      showToast("Type de congé créé avec succès.", "success");
    } catch (err) {
      showToast(`Erreur lors de la création : ${err.message}`, "error");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!typeToDelete) return;
    const id = typeToDelete.id;
    setDeletingTypeId(id);
    try {
      await deleteTypeConge(id);
      setTypes((prev) => prev.filter((t) => t.id !== id));
      setTypeToDelete(null);
      showToast("Type de congé supprimé.", "success");
    } catch (err) {
      showToast(`Erreur lors de la suppression : ${err.message}`, "error");
    } finally {
      setDeletingTypeId(null);
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
            <FolderKanban className="w-6 h-6 text-blue-400" />
            Configuration des Types de Congés
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Définissez les différentes catégories d&apos;absences, quotas annuels et règles d&apos;imputation RH.
          </p>
        </div>
      </div>

      {/* Formulaire de création */}
      <div className="bg-white shadow-sm border border-slate-200 rounded-2xl p-6">
        <h2 className="text-lg font-bold text-slate-800 mb-4 flex items-center gap-2 border-b border-slate-100 pb-3">
          <PlusCircle className="w-5 h-5 text-blue-600" />
          Nouveau Type de Congé
        </h2>
        <form onSubmit={handleAddType} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-bold uppercase text-slate-600 mb-1">
                Code (Sigle)
              </label>
              <input
                type="text"
                name="code"
                value={formData.code}
                onChange={handleChange}
                placeholder="Ex: CP, RTT, MAL"
                required
                className="input input-bordered w-full uppercase font-mono text-sm bg-white text-slate-800 focus:outline-hidden"
              />
            </div>

            <div className="sm:col-span-2">
              <label className="block text-xs font-bold uppercase text-slate-600 mb-1">
                Libellé
              </label>
              <input
                type="text"
                name="libelle"
                value={formData.libelle}
                onChange={handleChange}
                placeholder="Ex: Congés Payés Annuels"
                required
                className="input input-bordered w-full text-sm bg-white text-slate-800 focus:outline-hidden"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 items-center">
            <div>
              <label className="block text-xs font-bold uppercase text-slate-600 mb-1">
                Nombre de jours par an
              </label>
              <input
                type="number"
                name="nombreJoursParAn"
                min="0"
                value={formData.nombreJoursParAn}
                onChange={handleChange}
                required
                className="input input-bordered w-full text-sm bg-white text-slate-800 focus:outline-hidden"
              />
            </div>

            <div className="form-control">
              <label className="label cursor-pointer justify-start gap-3 mt-4">
                <input
                  type="checkbox"
                  name="justificatifObligatoire"
                  checked={formData.justificatifObligatoire}
                  onChange={handleChange}
                  className="checkbox checkbox-primary"
                />
                <span className="label-text font-semibold text-slate-700 text-sm">
                  Justificatif obligatoire
                </span>
              </label>
            </div>

            <div className="form-control">
              <label className="label cursor-pointer justify-start gap-3 mt-4">
                <input
                  type="checkbox"
                  name="actif"
                  checked={formData.actif}
                  onChange={handleChange}
                  className="checkbox checkbox-success"
                />
                <span className="label-text font-semibold text-slate-700 text-sm">
                  Type actif
                </span>
              </label>
            </div>
          </div>

          <div className="flex justify-end pt-2">
            <button
              type="submit"
              disabled={submitting}
              className="btn btn-primary text-white font-semibold px-6 gap-2 shadow-xs"
            >
              {submitting ? (
                <>
                  <span className="loading loading-spinner loading-xs"></span>
                  Ajout en cours...
                </>
              ) : (
                <>
                  <PlusCircle className="w-4 h-4" />
                  Ajouter le type de congé
                </>
              )}
            </button>
          </div>
        </form>
      </div>

      {/* Liste des types */}
      <div className="bg-white shadow-sm border border-slate-200 rounded-2xl p-6">
        <h2 className="text-lg font-bold text-slate-800 mb-4 flex items-center gap-2 border-b border-slate-100 pb-3">
          <Layers className="w-5 h-5 text-blue-600" />
          Types de Congés Disponibles
        </h2>

        {loading ? (
          <div className="flex justify-center items-center py-12">
            <span className="loading loading-spinner loading-md text-primary"></span>
          </div>
        ) : types.length === 0 ? (
          <div className="text-center py-12 text-slate-400">
            <FolderKanban className="w-12 h-12 mx-auto mb-2 text-slate-300" />
            <p className="font-semibold text-slate-600">Aucun type de congé configuré pour le moment.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="table w-full">
              <thead>
                <tr className="bg-slate-50 text-slate-600 text-xs uppercase tracking-wider border-b border-slate-200">
                  <th className="py-3">Code</th>
                  <th className="py-3">Libellé</th>
                  <th className="py-3">Quota Ancien / An</th>
                  <th className="py-3">Justificatif</th>
                  <th className="py-3">Statut</th>
                  <th className="py-3 text-right">Action</th>
                </tr>
              </thead>
              <tbody>
                {types.map((t) => (
                  <tr key={t.id} className="hover:bg-slate-50/80 transition-colors border-b border-slate-100">
                    <td className="py-3.5">
                      <span className="inline-flex items-center px-2.5 py-1 rounded-md font-mono font-bold text-xs bg-blue-50 text-blue-700 border border-blue-200">
                        {t.code || `ID-${t.id}`}
                      </span>
                    </td>
                    <td className="py-3.5 font-bold text-slate-800 text-sm">
                      {t.libelle || t.nom || "-"}
                    </td>
                    <td className="py-3.5 font-bold text-blue-600 text-sm">
                      {t.nombreJoursParAn ?? 0} jours
                    </td>
                    <td className="py-3.5">
                      {t.justificatifObligatoire ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded text-xs font-bold bg-amber-50 text-amber-700 border border-amber-200">
                          <FileText className="w-3 h-3" />
                          Obligatoire
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded text-xs font-medium bg-slate-50 text-slate-500 border border-slate-200">
                          Non requis
                        </span>
                      )}
                    </td>
                    <td className="py-3.5">
                      {t.actif !== false ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                          <Check className="w-3 h-3" />
                          Actif
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded text-xs font-medium bg-slate-50 text-slate-400 border border-slate-200">
                          <X className="w-3 h-3" />
                          Inactif
                        </span>
                      )}
                    </td>
                    <td className="py-3.5 text-right">
                      <button
                        onClick={() => setTypeToDelete(t)}
                        className="btn btn-ghost btn-xs text-rose-600 hover:bg-rose-50 font-semibold gap-1"
                        title="Supprimer ce type de congé"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
      {typeToDelete && (
        <ConfirmDialog
          title="Supprimer ce type de congé ?"
          message={`Le type « ${typeToDelete.libelle || typeToDelete.code} » sera supprimé. Cette action ne peut pas être annulée.`}
          pending={deletingTypeId === typeToDelete.id}
          onCancel={() => {
            if (deletingTypeId === null) setTypeToDelete(null);
          }}
          onConfirm={handleDelete}
        />
      )}
    </div>
  );
}