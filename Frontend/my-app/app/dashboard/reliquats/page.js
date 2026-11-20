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
  const [busy, setBusy] = useState(null);
  const toast = useToast();

  const load = useCallback(async () => {
    const response = await authFetch(API + "/circuit-conge/administration/en-attente");
    if (!response.ok) throw new Error("Chargement impossible");
    const data = await response.json();
    setRows(data);
    const loadedTypes = await getAllTypeConge();
    setTypesConge(loadedTypes);

    const fetchBalances = async () => Promise.all(data.map(async dossier => {
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

    let pairs = await fetchBalances();
    const existing = new Set(pairs.flatMap(([id, items]) =>
      items.map(item => `${item.employeId}:${item.typeCongeId}:${item.periode}`)
    ));
    const targets = new Map();
    for (const dossier of data) {
      if (!dossier.employeId) continue;
      const typeId = dossier.typeCongeId ?? dossier.typeConge?.id;
      const type = loadedTypes.find(item => String(item.id) === String(typeId));
      const annual = `${type?.code || ""} ${type?.libelle || dossier.typeConge?.libelle || ""}`
        .toLowerCase().includes("annuel");
      if (!annual) continue;
      const firstYear = new Date(dossier.dateDebut).getFullYear() - 1;
      const lastYear = new Date(dossier.dateFin).getFullYear();
      for (let year = firstYear; year <= lastYear; year++) {
        const key = `${dossier.employeId}:${typeId}:${year}`;
        if (!existing.has(key)) targets.set(key, { employeId: dossier.employeId, typeId, year });
      }
    }
    for (const target of targets.values()) {
      const query = new URLSearchParams({
        typeCongeId: String(target.typeId),
        periode: String(target.year)
      });
      const result = await authFetch(
        API + "/solde/employe/" + target.employeId + "/calculer-annuel?" + query.toString(),
        { method: "POST" }
      );
      if (!result.ok) {
        const detail = await result.text();
        throw new Error(detail || `Calcul automatique impossible pour l’exercice ${target.year}.`);
      }
    }
    if (targets.size) pairs = await fetchBalances();
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

  const confirm = async dossier => {
    const id = dossier.id;
    setBusy(id);
    try {
      const value = values[id] || {};
      const isEmployee = Boolean(dossier.employeId);
      const endpoint = isEmployee
        ? API + "/reliquats/dossier/" + id + "/confirmer-automatiquement"
        : API + "/circuit-conge/" + id + "/reliquat";
      const body = isEmployee
        ? { observation: value.observation?.trim() || "" }
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

            <div className="rounded-lg bg-slate-50 border p-3 text-sm">
              {isEmployee
                ? <span className="text-slate-700">Les droits et le reliquat sont calculés automatiquement à partir de la date d’embauche et de l’historique des congés.</span>
                : <span className="text-slate-700">Vérifiez le reliquat historique associé à ce dossier.</span>}
            </div>

            <div className="grid md:grid-cols-2 gap-3">
              <label className="form-control md:col-span-2">
                <span className="label-text">Observation</span>
                <textarea className="textarea textarea-bordered"
                  value={value.observation ?? ""}
                  onChange={event => update(dossier.id, "observation", event.target.value)} />
              </label>
            </div>

            <button
              className="btn btn-primary w-fit"
              disabled={busy === dossier.id}
              onClick={() => confirm(dossier)}
            >
              {busy === dossier.id ? "Enregistrement…" : "Valider et transmettre"}
            </button>
          </article>
        );
      })}

      {!rows.length && <p className="text-slate-500">Aucune demande en attente.</p>}
    </section>
  );
}
