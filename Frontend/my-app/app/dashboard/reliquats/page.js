"use client";

import { useCallback, useEffect, useState } from "react";
import { authFetch } from "@/lib/apiClient";
import { useToast } from "../../Components/ToastProvider";
import { getAllTypeConge } from "../../api/typeConge/typeConge";

const API = "http://localhost:8080";

export default function ReliquatsPage() {
  const [rows, setRows] = useState([]);
  const [typesConge, setTypesConge] = useState([]);
  const [balances, setBalances] = useState({});
  const [values, setValues] = useState({});
  const [balanceValues, setBalanceValues] = useState({});
  const [busy, setBusy] = useState(null);
  const toast = useToast();

  const load = useCallback(async () => {
    const response = await authFetch(API + "/circuit-conge/administration/en-attente");
    if (!response.ok) throw new Error("Chargement impossible");
    const data = await response.json();
    setRows(data);
    setTypesConge(await getAllTypeConge());

    const pairs = await Promise.all(data.map(async dossier => {
      const endpoint = dossier.employeId
        ? "/solde/employe/" + dossier.employeId
        : "/solde/utilisateur/" + dossier.utilisateurId;
      const result = await authFetch(API + endpoint);
      if (!result.ok) {
        const detail = await result.text();
        throw new Error(
          `Chargement du solde du dossier #${dossier.id} : HTTP ${result.status}` +
          (detail ? ` — ${detail}` : "")
        );
      }
      return [dossier.id, await result.json()];
    }));
    setBalances(Object.fromEntries(pairs));
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

  const saveBalance = async (dossier, year) => {
    const key = dossier.id + "_" + year;
    const isAnnual = isAnnualType(dossier);
    const days = balanceValues[key];
    if (!isAnnual && (days == null || days === "" || Number(days) < 0)) {
      toast("Saisissez le nombre de jours acquis validé par les RH.", "warning");
      return;
    }
    setBusy(key);
    try {
      const query = new URLSearchParams({
        typeCongeId: String(dossier.typeCongeId),
        periode: String(year)
      });
      const endpoint = isAnnual ? "calculer-annuel" : "initialiser";
      if (!isAnnual) query.set("joursAcquis", String(Number(days)));
      const response = await authFetch(
        API + "/solde/employe/" + dossier.employeId + "/" + endpoint + "?" + query.toString(),
        { method: "POST" }
      );
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || "Enregistrement du solde impossible");
      toast("Solde RH enregistré pour " + year + ".", "success");
      await load();
    } catch (error) {
      toast(error.message, "error");
    } finally {
      setBusy(null);
    }
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
      const employeeBalances = balances[id] || [];
      const previousYear = new Date(dossier.dateDebut).getFullYear() - 1;
      const previousBalance = employeeBalances.find(balance =>
        String(balance.typeCongeId) === String(dossier.typeCongeId) &&
        Number(balance.periode) === previousYear
      );
      const autoCarryover = Math.floor(Number(previousBalance?.soldeRestantJours ?? 0));
      const body = isEmployee
        ? {
            nombreJoursConfirme: Number(value.days ?? autoCarryover),
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
          Vérifiez les droits, puis enregistrez une confirmation traçable du reliquat.
        </p>
      </header>

      {rows.map(dossier => {
        const employeeBalances = balances[dossier.id] || [];
        const relevantBalances = employeeBalances.filter(
          balance => String(balance.typeCongeId) === String(dossier.typeCongeId)
        );
        const value = values[dossier.id] || {};
        const isEmployee = Boolean(dossier.employeId);
        const requestYears = isEmployee ? Array.from(
          { length: Math.max(1, new Date(dossier.dateFin).getFullYear() - new Date(dossier.dateDebut).getFullYear() + 2) },
          (_, index) => new Date(dossier.dateDebut).getFullYear() - 1 + index
        ) : [];
        const missingBalanceYears = requestYears.filter(
          year => !relevantBalances.some(balance => Number(balance.periode) === year)
        );

        return (
          <article key={dossier.id} className="card bg-base-100 border p-5 space-y-3">
            <div className="flex flex-wrap justify-between gap-2">
              <b>
                Dossier #{dossier.id} — {dossier.utilisateurPrenom} {dossier.utilisateurNom}
                ({dossier.utilisateurMatricule})
              </b>
              <span>
                {getTypeLabel(dossier)} · {dossier.dateDebut} → {dossier.dateFin}
                · {dossier.nombreJours} jour(s) demandés
              </span>
              <button type="button" className="btn btn-sm btn-outline"
                onClick={() => downloadScan(dossier.id)}>
                Télécharger le scan
              </button>
            </div>

            <div className="rounded-lg bg-slate-50 border p-3 text-sm space-y-2">
              <b>Soldes enregistrés pour ce type de congé :</b>
              {relevantBalances.length ? relevantBalances.map(balance => (
                <p key={balance.id}>
                  Exercice {balance.periode} : acquis {balance.soldeAquisJours ?? balance.soldeAquis ?? 0} j
                  · pris {balance.soldePrisJours ?? balance.soldePris ?? 0} j
                  · restant {balance.soldeRestantJours ?? balance.soldeRestant ?? 0} j
                </p>
              )) : (
                <p className="text-amber-700">Aucun solde correspondant trouvé pour cet employé.</p>
              )}
              {isEmployee && missingBalanceYears.map(year => {
                const key = dossier.id + "_" + year;
                return (
                  <div key={year} className="flex flex-wrap items-end gap-2 border-t pt-2">
                    {isAnnualType(dossier) ? (
                      <p className="text-sm text-slate-600">
                        Droit annuel calculé automatiquement depuis la date d'embauche pour l'exercice {year}.
                      </p>
                    ) : (
                      <label className="form-control">
                        <span className="label-text">Jours acquis validés — exercice {year}</span>
                        <input type="number" min="0" className="input input-bordered input-sm"
                          value={balanceValues[key] ?? ""}
                          onChange={event => setBalanceValues(previous => ({ ...previous, [key]: event.target.value }))} />
                      </label>
                    )}
                    <button type="button" className="btn btn-sm btn-outline"
                      disabled={busy === key || (!isAnnualType(dossier)
                        && (balanceValues[key] == null || balanceValues[key] === ""))}
                      onClick={() => saveBalance(dossier, year)}>
                      {busy === key ? "Calcul…" : isAnnualType(dossier)
                        ? "Calculer le solde automatiquement" : "Enregistrer le solde RH"}
                    </button>
                  </div>
                );
              })}
            </div>

            <div className="grid md:grid-cols-2 gap-3">
              <label className="form-control">
                <span className="label-text">Reliquat confirmé (jours)</span>
                <input type="number" min="0" className="input input-bordered"
                  value={value.days ?? (isEmployee ? String(Math.floor(Number(
                    relevantBalances.find(balance => Number(balance.periode) === new Date(dossier.dateDebut).getFullYear() - 1)
                      ?.soldeRestantJours ?? 0
                  ))) : "")}
                  onChange={event => update(dossier.id, "days", event.target.value)} />
              </label>
              {isEmployee && <>
                <label className="form-control">
                  <span className="label-text">Période de référence</span>
                  <input className="input input-bordered" placeholder="Ex. 2025-2026"
                    value={value.period ?? ""}
                    onChange={event => update(dossier.id, "period", event.target.value)} />
                </label>
                <label className="form-control">
                  <span className="label-text">Référence de confirmation RH</span>
                  <input className="input input-bordered" placeholder="Référence du document RH"
                    value={value.reference ?? ""}
                    onChange={event => update(dossier.id, "reference", event.target.value)} />
                </label>
                <label className="form-control">
                  <span className="label-text">Justificatif (référence ou chemin)</span>
                  <input className="input input-bordered"
                    value={value.justificatif ?? ""}
                    onChange={event => update(dossier.id, "justificatif", event.target.value)} />
                </label>
              </>}
              <label className="form-control md:col-span-2">
                <span className="label-text">Observation</span>
                <textarea className="textarea textarea-bordered"
                  value={value.observation ?? ""}
                  onChange={event => update(dossier.id, "observation", event.target.value)} />
              </label>
            </div>

            <button
              className="btn btn-primary w-fit"
              disabled={busy === dossier.id || value.days === "" || value.days == null
                || (isEmployee && (missingBalanceYears.length > 0 || !value.period?.trim() || !value.reference?.trim()))}
              onClick={() => confirm(dossier)}
            >
              {busy === dossier.id ? "Enregistrement…" : "Confirmer le reliquat"}
            </button>
          </article>
        );
      })}

      {!rows.length && <p className="text-slate-500">Aucune demande en attente.</p>}
    </section>
  );
}
