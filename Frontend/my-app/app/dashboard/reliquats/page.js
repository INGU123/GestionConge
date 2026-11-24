"use client";

import { useCallback, useEffect, useState } from "react";
import { authFetch } from "@/lib/apiClient";
import { useToast } from "../../Components/ToastProvider";
import { getAllTypeConge } from "../../api/typeConge/typeConge";

const API = "http://localhost:8080";

export default function ReliquatsPage() {
  const [rows, setRows] = useState([]);
  const [typesConge, setTypesConge] = useState([]);
  const [values, setValues] = useState({});
  const [calculs, setCalculs] = useState({});
  const [busy, setBusy] = useState(null);
  const toast = useToast();

  const load = useCallback(async () => {
    const response = await authFetch(API + "/circuit-conge/administration/en-attente");
    if (!response.ok) throw new Error("Chargement impossible");
    const data = await response.json();
    setRows(data);
    const loadedTypes = await getAllTypeConge();
    setTypesConge(loadedTypes);
    const dossiersAvecEmploye = data.filter(dossier => Boolean(dossier.employeId));
    const previews = await Promise.all(dossiersAvecEmploye.map(async dossier => {
      try {
        const response = await authFetch(API + "/reliquats/dossier/" + dossier.id + "/calculer", { method: "POST" });
        const result = await response.json();
        if (!response.ok) throw new Error(result.message || "Calcul automatique indisponible.");
        return { id: dossier.id, result };
      } catch (error) {
        return { id: dossier.id, error: error.message };
      }
    }));
    setCalculs(Object.fromEntries(previews.map(({ id, result, error }) => [id, result || { error }])));
    setValues(previous => {
      const next = { ...previous };
      previews.forEach(({ id, result }) => {
        if (!result) return;
        next[id] = {
          ...previous[id],
          days: previous[id]?.days ?? String(result.reliquatDisponible),
          period: previous[id]?.period ?? result.periodeReference
        };
      });
      return next;
    });
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => {
      load().catch(error => toast(error.message, "error"));
    }, 0);
    return () => clearTimeout(timer);
  }, [load, toast]);

  const getType = dossier => {
    const typeId = dossier.typeCongeId ?? dossier.typeConge?.id;
    return typesConge.find(item => String(item.id) === String(typeId)) || dossier.typeConge || null;
  };

  const getTypeLabel = dossier => {
    const type = getType(dossier);
    return type?.libelle || type?.code || "Type de congé";
  };

  const getCalculationExplanation = dossier => {
    const type = getType(dossier);
    const code = String(type?.code || "").trim().toUpperCase();
    const libelle = String(type?.libelle || "").toUpperCase();
    if (code.includes("ANNUEL") || libelle.includes("ANNUEL")) {
      return "Le système calcule les droits annuels à partir de la date réelle d’embauche et des congés validés.";
    }
    if (code === "NAISS_PAT") {
      return "Le calcul applique le plafond spécifique de paternité et déduit les jours déjà validés.";
    }
    if (["MAR_SAL", "MAR_ENF", "DEC_CONJ", "CIRC_ENF", "URGENCE", "AUT_FAM"].includes(code)) {
      return "Le calcul applique le plafond légal commun des permissions familiales et déduit les jours déjà validés.";
    }
    return "Le solde RH enregistré est prioritaire. En son absence, le calcul utilise le plafond annuel configuré pour ce type et déduit les congés validés.";
  };

  const confirmAutomatically = async dossier => {
    const id = dossier.id;
    setBusy(id);
    try {
      const value = values[id] || {};
      const response = await authFetch(API + "/reliquats/dossier/" + id + "/confirmer-automatiquement", {
        method: "POST",
        body: JSON.stringify({ observation: value.observation?.trim() || "" })
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || "Confirmation automatique impossible.");
      toast("Reliquat calculé et confirmé automatiquement.", "success");
      await load();
    } catch (error) {
      toast(error.message, "error");
    } finally {
      setBusy(null);
    }
  };

  const downloadScan = async id => {
    try {
      const response = await authFetch(API + "/circuit-conge/" + id + "/piece-jointe");
      if (!response.ok) {
        const detail = await response.text();
        throw new Error(detail || "Scan indisponible pour ce dossier.");
      }
      const blob = await response.blob();
      const extension = blob.type.includes("pdf") ? "pdf"
        : blob.type.includes("png") ? "png"
        : blob.type.includes("jpeg") ? "jpg" : "bin";
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "scan-demande-conge-" + id + "." + extension;
      document.body.appendChild(link);
      link.click();
      link.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (error) {
      toast(error.message, "error");
    }
  };

  const update = (id, key, value) => {
    setValues(previous => ({
      ...previous,
      [id]: { ...previous[id], [key]: value }
    }));
  };



  return (
    <section className="space-y-5">
      <header>
        <h1 className="text-2xl font-bold">Administration du personnel — Vérification des reliquats</h1>
        <p className="text-sm text-slate-500">
          Tous les soldes sont lus depuis les données RH enregistrées. Aucun nombre de jours ni référence n'est saisi manuellement.
        </p>
      </header>

      {rows.map(dossier => {
        const value = values[dossier.id] || {};
        const isEmployee = Boolean(dossier.employeId);
        const automaticCalculation = isEmployee;
        const calcul = calculs[dossier.id];
        return (
          <article key={dossier.id} className="card bg-base-100 border p-5 space-y-3">
            <header className="space-y-2">
              <p className="text-xs font-bold uppercase tracking-wide text-base-content/60">Dossier #{dossier.id}</p>
              <h2 className="text-lg font-bold">{dossier.utilisateurPrenom} {dossier.utilisateurNom}</h2>
              <p className="text-sm text-base-content/70">Matricule : {dossier.utilisateurMatricule}</p>
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 flex-1">
                  <label className="form-control"><span className="label-text text-xs">Type de congé</span>
                    <input readOnly aria-readonly="true" className="input input-bordered input-sm bg-base-200" value={getTypeLabel(dossier)} />
                  </label>
                  <label className="form-control"><span className="label-text text-xs">Nombre de jours demandés</span>
                    <input readOnly aria-readonly="true" className="input input-bordered input-sm bg-base-200" value={`${dossier.nombreJours} jour(s)`} />
                  </label>
                </div>
                <p className="text-xs text-base-content/60">{dossier.dateDebut} → {dossier.dateFin}</p>
                <button type="button" className="btn btn-sm btn-outline"
                  onClick={() => downloadScan(dossier.id)}>
                  Télécharger le scan
                </button>
              </div>
            </header>

            <div className="rounded-xl border border-base-300 bg-base-200/50 p-4 space-y-3">
              {automaticCalculation ? <>
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <p className="text-sm text-base-content/75">{getCalculationExplanation(dossier)} Aucune durée n’est inventée.</p>
                </div>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                  <div className="rounded-lg bg-base-100 border border-base-300 p-3">
                    <label className="form-control"><span className="label-text text-xs">Droits acquis</span><input readOnly aria-readonly="true" className="input input-bordered bg-base-200" value={calcul && !calcul.error ? calcul.droitsAcquis + " j" : "Indisponible"} /></label>
                  </div>
                  <div className="rounded-lg bg-base-100 border border-base-300 p-3">
                    <label className="form-control"><span className="label-text text-xs">Jours consommés</span><input readOnly aria-readonly="true" className="input input-bordered bg-base-200" value={calcul && !calcul.error ? calcul.joursConsommes + " j" : "Indisponible"} /></label>
                  </div>
                  <div className="rounded-lg bg-base-100 border border-base-300 p-3">
                    <label className="form-control"><span className="label-text text-xs">Jours déjà réservés</span><input readOnly aria-readonly="true" className="input input-bordered bg-base-200" value={calcul && !calcul.error ? calcul.joursDejaReserves + " j" : "Indisponible"} /></label>
                  </div>
                  <div className="rounded-lg bg-primary/10 border border-primary/20 p-3">
                    <label className="form-control"><span className="label-text text-xs">Reliquat calculé</span><input readOnly aria-readonly="true" className="input input-bordered bg-base-200 font-bold" value={calcul && !calcul.error ? calcul.reliquatDisponible + " j" : "Indisponible"} /></label>
                  </div>
                </div>
                <p className="text-xs text-base-content/60">
                  {calcul?.error || (calcul ? `${calcul.periodeReference} · Reliquat brut : ${calcul.reliquatBrut} j` : "Calcul en cours…")}
                </p>
              </> : <p className="text-sm text-warning">
                Ce dossier historique n'est pas rattaché à une fiche employé disposant de soldes RH. La confirmation automatique est bloquée.
              </p>}
            </div>

            {automaticCalculation ? (
              <section className="rounded-xl border border-primary/20 bg-primary/5 p-4 space-y-2">
                <h3 className="font-semibold text-base">Confirmation automatique du reliquat</h3>
                <div className="flex items-center justify-between gap-3">
                  <span className="text-sm text-base-content/70">Reliquat confirmé depuis les données RH</span>
                  <strong className="text-2xl text-primary">
                    {calcul && !calcul.error ? calcul.reliquatDisponible : "—"} jour(s)
                  </strong>
                </div>
                <div className="grid md:grid-cols-2 gap-3">
                  <label className="form-control"><span className="label-text text-xs">Période de référence</span>
                    <input readOnly aria-readonly="true" className="input input-bordered bg-base-200" value={calcul && !calcul.error ? calcul.periodeReference : "Indisponible"} />
                  </label>
                  <label className="form-control"><span className="label-text text-xs">Référence RH</span>
                    <input readOnly aria-readonly="true" className="input input-bordered bg-base-200" value={"AUTO-RH-" + dossier.id} />
                  </label>
                </div>
                <p className="text-xs text-base-content/60">
                  Le justificatif est consultable dans le scan du dossier. Seule l'observation reste modifiable.
                </p>
                <button type="button" className="btn btn-primary"
                  disabled={busy === dossier.id || !calcul || Boolean(calcul.error)}
                  onClick={() => confirmAutomatically(dossier)}>
                  {busy === dossier.id ? "Confirmation…" : "Confirmer automatiquement"}
                </button>
              </section>
            ) : (
              <p className="text-sm text-warning">
                Confirmation indisponible sans fiche employé et soldes RH enregistrés. Aucun reliquat ne peut être saisi manuellement.
              </p>
            )}

            <div className="grid md:grid-cols-2 gap-3">
              <label className="form-control md:col-span-2">
                <span className="label-text">Observation</span>
                <textarea className="textarea textarea-bordered"
                  value={value.observation ?? ""}
                  onChange={event => update(dossier.id, "observation", event.target.value)} />
              </label>
            </div>
          </article>
        );
      })}

      {!rows.length && <p className="text-slate-500">Aucune demande en attente.</p>}
    </section>
  );
}
