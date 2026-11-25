"use client";

import { useCallback, useEffect, useState } from "react";
import { authFetch } from "@/lib/apiClient";
import { useToast } from "../../Components/ToastProvider";

const API = "http://localhost:8080";

export default function DemandePersonnellePage() {
  const [types, setTypes] = useState([]);
  const [dossiers, setDossiers] = useState([]);
  const [form, setForm] = useState({ typeCongeId: "", dateDebut: "", dateFin: "", commentaire: "" });
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const toast = useToast();

  const load = useCallback(async () => {
    const [typesResponse, dossiersResponse] = await Promise.all([
      authFetch(API + "/type-conge/all"),
      authFetch(API + "/circuit-conge/mes-demandes-personnelles"),
    ]);
    const typesData = await typesResponse.json().catch(() => []);
    const dossiersData = await dossiersResponse.json().catch(() => []);
    if (!typesResponse.ok) throw new Error(typesData.message || "Chargement des types de congé impossible.");
    if (!dossiersResponse.ok) throw new Error(dossiersData.message || "Chargement de vos demandes impossible.");
    setTypes(Array.isArray(typesData) ? typesData : []);
    setDossiers(Array.isArray(dossiersData) ? dossiersData : []);
  }, []);

  useEffect(() => {
    load().catch(error => toast(error.message, "error")).finally(() => setLoading(false));
  }, [load, toast]);

  const create = async event => {
    event.preventDefault();
    if (form.dateFin < form.dateDebut) {
      toast("La date de fin doit être postérieure ou égale à la date de début.", "warning");
      return;
    }
    setBusy(true);
    try {
      const response = await authFetch(API + "/circuit-conge/personnelle", {
        method: "POST",
        body: JSON.stringify({ ...form, typeCongeId: Number(form.typeCongeId) }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || "Création de la demande impossible.");
      setForm({ typeCongeId: "", dateDebut: "", dateFin: "", commentaire: "" });
      toast("Demande enregistrée. Elle attend la vérification du reliquat par l’administration.", "success");
      await load();
    } catch (error) {
      toast(error.message, "error");
    } finally {
      setBusy(false);
    }
  };

  const submit = async id => {
    setBusy(true);
    try {
      const response = await authFetch(API + "/circuit-conge/" + id + "/soumettre-personnelle", { method: "POST" });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || "Transmission impossible.");
      toast(data.statut === "DIRECTEUR_EN_ATTENTE"
        ? "Votre demande a été transmise directement au directeur."
        : "Votre demande a été transmise au chef de département.", "success");
      await load();
    } catch (error) {
      toast(error.message, "error");
    } finally {
      setBusy(false);
    }
  };

  const statutLabel = statut => ({
    RELIQUAT_EN_ATTENTE: "Vérification du reliquat en attente",
    RELIQUAT_RETOURNE: "Reliquat vérifié — transmission à effectuer",
    CHEF_EN_ATTENTE: "En attente du chef de département",
    DIRECTEUR_EN_ATTENTE: "En attente de la décision du directeur",
    VALIDEE: "Validée",
    REFUSEE: "Refusée",
  }[statut] || statut);

  return (
    <section className="space-y-6">
      <header>
        <p className="text-xs font-semibold uppercase tracking-[0.16em] text-primary">Demande personnelle</p>
        <h1 className="mt-1 text-2xl font-bold">Mon congé</h1>
        <p className="mt-1 text-sm text-base-content/65">
          Vous pouvez déposer votre propre demande. Personne ne peut vérifier, examiner ou décider son propre dossier.
        </p>
      </header>

      <form onSubmit={create} className="card space-y-4 border border-base-300 bg-base-100 p-5 shadow-sm">
        <h2 className="font-semibold">Nouvelle demande</h2>
        <label className="form-control">
          <span className="mb-2 text-sm font-medium">Type de congé *</span>
          <select required className="select select-bordered w-full" value={form.typeCongeId}
            onChange={event => setForm(value => ({ ...value, typeCongeId: event.target.value }))}>
            <option value="">Choisir un type</option>
            {types.map(type => <option key={type.id} value={type.id}>{type.libelle || type.code}</option>)}
          </select>
        </label>
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="form-control">
            <span className="mb-2 text-sm font-medium">Date de début *</span>
            <input required type="date" className="input input-bordered w-full" value={form.dateDebut}
              onChange={event => setForm(value => ({ ...value, dateDebut: event.target.value }))} />
          </label>
          <label className="form-control">
            <span className="mb-2 text-sm font-medium">Date de fin *</span>
            <input required type="date" min={form.dateDebut || undefined} className="input input-bordered w-full" value={form.dateFin}
              onChange={event => setForm(value => ({ ...value, dateFin: event.target.value }))} />
          </label>
        </div>
        <label className="form-control">
          <span className="mb-2 text-sm font-medium">Commentaire (facultatif)</span>
          <textarea rows={3} maxLength={1500} className="textarea textarea-bordered w-full" value={form.commentaire}
            onChange={event => setForm(value => ({ ...value, commentaire: event.target.value }))} />
        </label>
        <div className="flex justify-end">
          <button className="btn btn-primary" type="submit" disabled={busy || !types.length}>
            {busy ? "Traitement…" : "Enregistrer ma demande"}
          </button>
        </div>
      </form>

      <section className="card overflow-hidden border border-base-300 bg-base-100 shadow-sm">
        <div className="border-b border-base-300 p-5"><h2 className="font-semibold">Mes demandes personnelles</h2></div>
        <div className="overflow-x-auto">
          <table className="table table-sm">
            <thead><tr><th>Type</th><th>Période</th><th>Jours</th><th>Étape</th><th>Action</th></tr></thead>
            <tbody>
              {dossiers.map(dossier => {
                const type = types.find(item => String(item.id) === String(dossier.typeCongeId));
                return <tr key={dossier.id}>
                  <td>{type?.libelle || type?.code || ("Type #" + dossier.typeCongeId)}</td>
                  <td>{dossier.dateDebut} → {dossier.dateFin}</td>
                  <td>{dossier.nombreJours}</td>
                  <td>{statutLabel(dossier.statut)}</td>
                  <td>
                    {dossier.statut === "RELIQUAT_RETOURNE" && (
                      <button type="button" className="btn btn-sm btn-primary" disabled={busy}
                        onClick={() => submit(dossier.id)}>Transmettre</button>
                    )}
                    {dossier.statut === "REFUSEE" && <span className="text-xs text-error">{dossier.motifRefus || "Motif non renseigné"}</span>}
                  </td>
                </tr>;
              })}
              {!loading && !dossiers.length && <tr><td colSpan={5} className="py-8 text-center text-base-content/50">Aucune demande personnelle.</td></tr>}
            </tbody>
          </table>
          {loading && <p className="p-5 text-center text-sm text-base-content/60">Chargement…</p>}
        </div>
      </section>
    </section>
  );
}
