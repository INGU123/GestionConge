"use client";

import { useEffect, useState } from "react";
import { authFetch } from "@/lib/apiClient";
import { useToast } from "../../Components/ToastProvider";

const API = "http://localhost:8080";

export default function ReliquatsPage() {
  const [rows, setRows] = useState([]);
  const [balances, setBalances] = useState({});
  const [values, setValues] = useState({});
  const [balanceValues, setBalanceValues] = useState({});
  const [busy, setBusy] = useState(null);
  const toast = useToast();

  const load = async () => {
    const response = await authFetch(API + "/circuit-conge/administration/en-attente");
    if (!response.ok) throw new Error("Chargement impossible");
    const data = await response.json();
    setRows(data);

    const pairs = await Promise.all(data.map(async dossier => {
      const endpoint = dossier.employeId
        ? "/solde/employe/" + dossier.employeId
        : "/solde/utilisateur/" + dossier.utilisateurId;
      const result = await authFetch(API + endpoint);
      return [dossier.id, result.ok ? await result.json() : []];
    }));
    setBalances(Object.fromEntries(pairs));
  };

  useEffect(() => {
    load().catch(error => toast(error.message, "error"));
  }, []);

  const update = (id, key, value) => {
    setValues(previous => ({
      ...previous,
      [id]: { ...previous[id], [key]: value }
    }));
  };

  const saveBalance = async (dossier, year) => {
    const key = dossier.id + "_" + year;
    const days = balanceValues[key];
    if (days == null || days === "" || Number(days) < 0) {
      toast("Saisissez le nombre de jours acquis validé par les RH.", "warning");
      return;
    }
    setBusy(key);
    try {
      const query = new URLSearchParams({
        typeCongeId: String(dossier.typeCongeId),
        periode: String(year),
        joursAcquis: String(Number(days))
      });
      const response = await authFetch(
        API + "/solde/employe/" + dossier.employeId + "/initialiser?" + query.toString(),
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

  const confirm = async id => {
    setBusy(id);
    try {
      const value = values[id] || {};
      const response = await authFetch(API + "/reliquats/dossier/" + id + "/confirmer", {
        method: "POST",
        body: JSON.stringify({
          nombreJoursConfirme: Number(value.days),
          periodeReference: value.period?.trim(),
          referenceConfirmation: value.reference?.trim(),
          justificatif: value.justificatif?.trim() || "",
          observation: value.observation?.trim() || ""
        })
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

        return (
          <article key={dossier.id} className="card bg-base-100 border p-5 space-y-3">
            <div className="flex flex-wrap justify-between gap-2">
              <b>
                Dossier #{dossier.id} — {dossier.utilisateurPrenom} {dossier.utilisateurNom}
                ({dossier.utilisateurMatricule})
              </b>
              <span>
                Type #{dossier.typeCongeId} · {dossier.dateDebut} → {dossier.dateFin}
                · {dossier.nombreJours} jour(s) demandés
              </span>
            </div>

            <div className="rounded-lg bg-slate-50 border p-3 text-sm space-y-2">
              <b>Soldes enregistrés pour ce type de congé :</b>
              {relevantBalances.length ? relevantBalances.map(balance => (
                <p key={balance.id}>
                  Exercice {balance.periode} : acquis {balance.soldeAquis ?? 0} j
                  · pris {balance.soldePris ?? 0} j · restant {balance.soldeRestant ?? 0} j
                </p>
              )) : (
                <p className="text-amber-700">Aucun solde correspondant trouvé pour cet employé.</p>
              )}
              {isEmployee && Array.from(
                { length: Math.max(1, new Date(dossier.dateFin).getFullYear() - new Date(dossier.dateDebut).getFullYear() + 1) },
                (_, index) => new Date(dossier.dateDebut).getFullYear() + index
              ).filter(year => !relevantBalances.some(balance => Number(balance.periode) === year)).map(year => {
                const key = dossier.id + "_" + year;
                return (
                  <div key={year} className="flex flex-wrap items-end gap-2 border-t pt-2">
                    <label className="form-control">
                      <span className="label-text">Jours acquis validés — exercice {year}</span>
                      <input type="number" min="0" className="input input-bordered input-sm"
                        value={balanceValues[key] ?? ""}
                        onChange={event => setBalanceValues(previous => ({ ...previous, [key]: event.target.value }))} />
                    </label>
                    <button type="button" className="btn btn-sm btn-outline"
                      disabled={busy === key || balanceValues[key] == null || balanceValues[key] === ""}
                      onClick={() => saveBalance(dossier, year)}>
                      {busy === key ? "Enregistrement…" : "Enregistrer le solde RH"}
                    </button>
                  </div>
                );
              })}
            </div>

            <div className="grid md:grid-cols-2 gap-3">
              <label className="form-control">
                <span className="label-text">Reliquat confirmé (jours)</span>
                <input type="number" min="0" className="input input-bordered"
                  value={value.days ?? ""}
                  onChange={event => update(dossier.id, "days", event.target.value)} />
              </label>
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
                || !value.period?.trim() || !value.reference?.trim()}
              onClick={() => confirm(dossier.id)}
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
