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
    const previews = await Promise.all(data.map(async dossier => {
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

  const confirmManually = async dossier => {
    const id = dossier.id;
    const value = values[id] || {};
    const year = new Date(dossier.dateDebut).getFullYear();
    const type = getType(dossier);
    const isAnnual = String(type?.code || "").toUpperCase().includes("ANNUEL")
      || String(type?.libelle || "").toUpperCase().includes("ANNUEL");
    const period = value.manualPeriod?.trim() || `Exercice ${isAnnual ? year - 1 : year}`;
    const reference = value.manualReference?.trim() || `RH-MANUEL-${id}`;
    if (value.manualDays === undefined || String(value.manualDays).trim() === ""
        || !period || !reference || !value.observation?.trim()) {
      toast("Renseignez les jours confirmés, la période, la référence RH et l'observation justificative.", "warning");
      return;
    }
    setBusy(id);
    try {
      const response = await authFetch(API + "/reliquats/dossier/" + id + "/confirmer-manuellement", {
        method: "POST",
        body: JSON.stringify({
          nombreJoursConfirme: Number(value.manualDays),
          periodeReference: period,
          referenceConfirmation: reference,
          observation: value.observation.trim()
        })
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || "Confirmation manuelle impossible.");
      toast("Rapprochement RH manuel enregistré.", "success");
      await load();
    } catch (error) {
      toast(error.message, "error");
    } finally {
      setBusy(null);
    }
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

  const update = (id, key, value) => {
    setValues(previous => ({
      ...previous,
      [id]: { ...previous[id], [key]: value }
    }));
  };



  return (
    <section className="reliquats-theme space-y-5">
      <header>
        <h1 className="text-2xl font-bold">Administration du personnel — Vérification des reliquats</h1>
        <p className="text-sm text-slate-500">
          Calcul automatique lorsque les soldes existent ; sinon, rapprochement manuel justifié par l'administration pour les dossiers historiques.
        </p>
      </header>

      {rows.map(dossier => {
        const value = values[dossier.id] || {};
        const isEmployee = Boolean(dossier.employeId);
        const isUserAccount = !isEmployee && Boolean(dossier.utilisateurId);
        const calcul = calculs[dossier.id];
        const automaticCalculation = isEmployee || (isUserAccount && Boolean(calcul) && !calcul.error);
        const manualFallback = isUserAccount && Boolean(calcul?.error);
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
                {manualFallback
                  ? `Calcul automatique indisponible : ${calcul.error} Un rapprochement manuel RH avec justificatif est nécessaire.`
                  : isUserAccount
                    ? "Vérification des soldes RH du compte en cours…"
                    : "Ce dossier ne possède ni fiche employé ni compte utilisateur exploitable."}
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
                  L'observation reste modifiable.
                </p>
                <button type="button" className="btn btn-primary"
                  disabled={busy === dossier.id || !calcul || Boolean(calcul.error)}
                  onClick={() => confirmAutomatically(dossier)}>
                  {busy === dossier.id ? "Confirmation…" : "Confirmer automatiquement"}
                </button>
              </section>
            ) : manualFallback ? (
              <section className="rounded-xl border border-warning/40 bg-warning/5 p-4 space-y-3">
                <h3 className="font-semibold">Rapprochement manuel des données RH</h3>
                <p className="text-sm text-base-content/70">
                  Aucun solde exploitable n'est enregistré pour ce compte. Saisissez uniquement le montant vérifié dans les pièces RH. Cette opération ne crée ni ne modifie aucun solde.
                </p>
                <div className="grid gap-3 md:grid-cols-3">
                  <label className="form-control">
                    <span className="label-text text-xs">Reliquat confirmé (jours)</span>
                    <input type="number" min="0" step="1" required className="input input-bordered"
                      value={value.manualDays ?? ""}
                      onChange={event => update(dossier.id, "manualDays", event.target.value)} />
                  </label>
                  <label className="form-control">
                    <span className="label-text text-xs">Période de référence</span>
                    <input required maxLength={100} className="input input-bordered"
                      value={value.manualPeriod ?? `Exercice ${getType(dossier)?.code?.toUpperCase().includes("ANNUEL") || getType(dossier)?.libelle?.toUpperCase().includes("ANNUEL") ? new Date(dossier.dateDebut).getFullYear() - 1 : new Date(dossier.dateDebut).getFullYear()}`}
                      onChange={event => update(dossier.id, "manualPeriod", event.target.value)} />
                  </label>
                  <label className="form-control">
                    <span className="label-text text-xs">Référence RH</span>
                    <input required maxLength={100} className="input input-bordered"
                      value={value.manualReference ?? `RH-MANUEL-${dossier.id}`}
                      onChange={event => update(dossier.id, "manualReference", event.target.value)} />
                  </label>
                </div>
                <p className="text-xs text-base-content/60">L'observation ci-dessous est obligatoire et doit expliquer la source RH de la vérification.</p>
                <button type="button" className="btn btn-warning"
                  disabled={busy === dossier.id || value.manualDays === undefined || String(value.manualDays).trim() === "" || !value.observation?.trim()}
                  onClick={() => confirmManually(dossier)}>
                  {busy === dossier.id ? "Enregistrement…" : "Enregistrer la confirmation RH"}
                </button>
              </section>
            ) : isUserAccount ? (
              <p className="text-sm text-base-content/60">Recherche des soldes RH du compte…</p>
            ) : (
              <p className="text-sm text-warning">
                Dossier sans rattachement utilisateur exploitable : la confirmation RH est bloquée.
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
