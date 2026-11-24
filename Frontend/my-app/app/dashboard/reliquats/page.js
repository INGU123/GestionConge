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

    const annualDossiers = data.filter(dossier => {
      if (!dossier.employeId) return false;
      const typeId = dossier.typeCongeId ?? dossier.typeConge?.id;
      const type = loadedTypes.find(item => String(item.id) === String(typeId));
      return `${type?.code || ""} ${type?.libelle || dossier.typeConge?.libelle || ""}`
        .toLowerCase().includes("annuel");
    });
    const previews = await Promise.all(annualDossiers.map(async dossier => {
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

  const getTypeLabel = dossier => {
    const typeId = dossier.typeCongeId ?? dossier.typeConge?.id;
    const type = typesConge.find(item => String(item.id) === String(typeId));
    return type?.libelle || dossier.typeConge?.libelle || type?.code || "Type de congé";
  };

  const isAnnualType = dossier => {
    const typeId = dossier.typeCongeId ?? dossier.typeConge?.id;
    const type = typesConge.find(item => String(item.id) === String(typeId));
    return `${type?.code || ""} ${type?.libelle || dossier.typeConge?.libelle || ""}`
      .toLowerCase().includes("annuel");
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

  const confirm = async dossier => {
    const id = dossier.id;
    setBusy(id);
    try {
      const value = values[id] || {};
      const isEmployee = Boolean(dossier.employeId);
      const endpoint = isEmployee
        ? API + "/reliquats/dossier/" + id + "/confirmer"
        : API + "/circuit-conge/" + id + "/reliquat";
      const body = isEmployee
        ? {
            nombreJoursConfirme: Number(value.days),
            periodeReference: value.period?.trim(),
            referenceConfirmation: value.reference?.trim(),
            justificatif: value.justificatif?.trim() || "",
            observation: value.observation?.trim() || ""
          }
        : { reliquatJours: Number(value.days), commentaire: value.observation?.trim() || "" };
      const response = await authFetch(endpoint, {
        method: "POST",
        body: JSON.stringify(body)
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || "Confirmation impossible");
      toast("Reliquat confirmé et enregistré dans l'historique.", "success");
      await load();
    } catch (error) {
      toast(error.message, "error");
    } finally {
      setBusy(null);
    }
  };

  return (
    <section className="space-y-5">
      <header>
        <h1 className="text-2xl font-bold">Administration du personnel — Vérification des reliquats</h1>
        <p className="text-sm text-slate-500">
          Les droits annuels et le reliquat sont calculés automatiquement. Les autres types conservent leur validation spécifique.
        </p>
      </header>

      {rows.map(dossier => {
        const value = values[dossier.id] || {};
        const isEmployee = Boolean(dossier.employeId);
        const annualCalculation = isEmployee && isAnnualType(dossier);
        const calcul = calculs[dossier.id];
        const ecartCalcul = annualCalculation && calcul && !calcul.error
          && Number(value.days) !== Number(calcul.reliquatDisponible);

        return (
          <article key={dossier.id} className="card bg-base-100 border p-5 space-y-3">
            <header className="space-y-2">
              <p className="text-xs font-bold uppercase tracking-wide text-base-content/60">Dossier #{dossier.id}</p>
              <h2 className="text-lg font-bold">{dossier.utilisateurPrenom} {dossier.utilisateurNom}</h2>
              <p className="text-sm text-base-content/70">Matricule : {dossier.utilisateurMatricule}</p>
              <div className="flex flex-wrap items-center justify-between gap-2">
                <p className="text-sm">
                  <span className="font-medium">{getTypeLabel(dossier)}</span>
                  <span> · {dossier.dateDebut} → {dossier.dateFin} · {dossier.nombreJours} jour(s) demandés</span>
                </p>
                <button type="button" className="btn btn-sm btn-outline"
                  onClick={() => downloadScan(dossier.id)}>
                  Télécharger le scan
                </button>
              </div>
            </header>

            <div className="rounded-xl border border-base-300 bg-base-200/50 p-4 space-y-3">
              {annualCalculation ? <>
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <p className="text-sm text-base-content/75">Le système calcule le reliquat à partir de la date réelle d’embauche, des droits acquis et des congés validés. Aucune recherche manuelle du solde n’est nécessaire.</p>
                  <button type="button" className="btn btn-primary btn-sm"
                    disabled={busy === dossier.id || !calcul || Boolean(calcul.error)}
                    onClick={() => confirmAutomatically(dossier)}>
                    {busy === dossier.id ? "Confirmation…" : `Confirmer automatiquement · ${calcul && !calcul.error ? calcul.reliquatDisponible : "—"} j`}
                  </button>
                </div>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                  <div className="rounded-lg bg-base-100 border border-base-300 p-3">
                    <p className="text-xs text-base-content/60">Droits acquis</p>
                    <p className="text-xl font-bold">{calcul && !calcul.error ? calcul.droitsAcquis : "—"} j</p>
                  </div>
                  <div className="rounded-lg bg-base-100 border border-base-300 p-3">
                    <p className="text-xs text-base-content/60">Jours consommés</p>
                    <p className="text-xl font-bold">{calcul && !calcul.error ? calcul.joursConsommes : "—"} j</p>
                  </div>
                  <div className="rounded-lg bg-base-100 border border-base-300 p-3">
                    <p className="text-xs text-base-content/60">Déjà réservés</p>
                    <p className="text-xl font-bold">{calcul && !calcul.error ? calcul.joursDejaReserves : "—"} j</p>
                  </div>
                  <div className="rounded-lg bg-primary/10 border border-primary/20 p-3">
                    <p className="text-xs text-base-content/70">Reliquat disponible</p>
                    <p className="text-xl font-bold text-primary">{calcul && !calcul.error ? calcul.reliquatDisponible : "—"} j</p>
                  </div>
                </div>
                <p className="text-xs text-base-content/60">
                  {calcul?.error || (calcul ? `${calcul.periodeReference} · Reliquat brut : ${calcul.reliquatBrut} j` : "Calcul en cours…")}
                </p>
              </> : <p className="text-sm text-base-content/75">
                {isEmployee ? "Le reliquat de ce type de congé doit être vérifié selon les règles qui lui sont applicables." : "Vérifiez le reliquat historique associé à ce dossier."}
              </p>}
            </div>

            <section className="space-y-3">
              <h3 className="font-semibold text-base">Confirmation administrative</h3>
              <div className="grid md:grid-cols-2 gap-3">
              <label className="form-control">
                <span className="label-text font-semibold">{annualCalculation ? "Ajustement manuel du reliquat (jours)" : "Reliquat confirmé (jours) *"}</span>
                <input type="number" min="0" step="1" className="input input-bordered"
                  value={value.days ?? ""}
                  onChange={event => update(dossier.id, "days", event.target.value)} />
                <span className="text-xs text-base-content/60">{annualCalculation ? "À utiliser uniquement pour corriger exceptionnellement le calcul automatique ; une observation justifiant l’écart sera obligatoire." : "Valeur retenue officiellement après vérification."}</span>
              </label>
              {isEmployee && <>
                <label className="form-control">
                  <span className="label-text font-semibold">Période de référence *</span>
                  <input className={`input input-bordered ${annualCalculation ? "bg-base-200 text-base-content/70" : ""}`}
                    placeholder="Ex. Exercice 2025" value={value.period ?? ""}
                    readOnly={annualCalculation} aria-readonly={annualCalculation}
                    onChange={event => update(dossier.id, "period", event.target.value)} />
                  <span className="text-xs text-base-content/60">
                    {annualCalculation ? "Exercice déterminé automatiquement à partir de l’année précédant le congé." : "Exercice auquel se rapporte le reliquat historique."}
                  </span>
                </label>
                <label className="form-control">
                  <span className="label-text font-semibold">Référence de confirmation RH *</span>
                  <input className="input input-bordered" placeholder="Référence du document ou de la décision RH"
                    value={value.reference ?? ""}
                    onChange={event => update(dossier.id, "reference", event.target.value)} />
                </label>
                <label className="form-control">
                  <span className="label-text">Justificatif (facultatif)</span>
                  <input className="input input-bordered" placeholder="Nom ou référence du justificatif"
                    value={value.justificatif ?? ""}
                    onChange={event => update(dossier.id, "justificatif", event.target.value)} />
                </label>
              </>}
              </div>
            </section>

            <div className="grid md:grid-cols-2 gap-3">
              <label className="form-control md:col-span-2">
                <span className="label-text">Observation</span>
                <textarea className="textarea textarea-bordered"
                  value={value.observation ?? ""}
                  onChange={event => update(dossier.id, "observation", event.target.value)} />
              </label>
            </div>

            {ecartCalcul && (
              <p className="text-sm text-warning">Le nombre confirmé diffère du calcul automatique : une observation est obligatoire pour justifier cet écart.</p>
            )}
            <button
              className="btn btn-primary w-fit"
              disabled={busy === dossier.id
                || (!isEmployee && (value.days == null || value.days === ""))
                || (isEmployee && (value.days == null || value.days === "" || !value.period?.trim() || !value.reference?.trim()))
                || (annualCalculation && (!calcul || calcul.error))
                || (ecartCalcul && !value.observation?.trim())}
              onClick={() => confirm(dossier)}
            >
              {busy === dossier.id ? "Enregistrement…" : annualCalculation ? "Enregistrer l’ajustement manuel" : isEmployee ? "Confirmer le reliquat" : "Valider et transmettre"}
            </button>
          </article>
        );
      })}

      {!rows.length && <p className="text-slate-500">Aucune demande en attente.</p>}
    </section>
  );
}
