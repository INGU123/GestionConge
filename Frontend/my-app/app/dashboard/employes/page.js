"use client";

import { useCallback, useEffect, useState } from "react";
import { authFetch } from "@/lib/apiClient";
import { useToast } from "../../Components/ToastProvider";

const API = "http://localhost:8080";
const empty = {
  matricule: "", nom: "", prenom: "", numeroCin: "", dateNaissance: "",
  telephone: "", adresse: "", fonction: "", typeContrat: "", email: "", dateEmbauche: "",
};

export default function EmployesPage() {
  const [items, setItems] = useState([]);
  const [form, setForm] = useState(empty);
  const [editing, setEditing] = useState(null);
  const [showInactive, setShowInactive] = useState(false);
  const [search, setSearch] = useState("");
  const [busy, setBusy] = useState(false);
  const toast = useToast();

  const load = useCallback(async () => {
    const response = await authFetch(API + "/employes");
    if (!response.ok) throw new Error("Impossible de charger le référentiel employés.");
    setItems(await response.json());
  }, []);

  useEffect(() => {
    load().catch((error) => toast(error.message, "error"));
  }, [load, toast]);

  const reset = () => { setForm(empty); setEditing(null); };

  const save = async (event) => {
    event.preventDefault();
    setBusy(true);
    try {
      const payload = Object.fromEntries(Object.entries(form).map(([key, value]) => [key, value || null]));
      const response = await authFetch(API + (editing ? `/employes/${editing.id}` : "/employes"), {
        method: editing ? "PUT" : "POST",
        body: JSON.stringify(payload),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || "Enregistrement impossible.");
      toast(editing ? "Fiche employé modifiée." : "Fiche employé créée.", "success");
      reset();
      await load();
    } catch (error) {
      toast(error.message, "error");
    } finally {
      setBusy(false);
    }
  };

  const edit = (employee) => {
    setEditing(employee);
    setForm({
      matricule: employee.matricule || "", nom: employee.nom || "", prenom: employee.prenom || "",
      numeroCin: employee.numeroCin || "", dateNaissance: employee.dateNaissance || "",
      telephone: employee.telephone || "", adresse: employee.adresse || "",
      fonction: employee.fonction || "", typeContrat: employee.typeContrat || "",
      email: employee.email || "", dateEmbauche: employee.dateEmbauche || "",
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const changeStatus = async (employee) => {
    const action = employee.actif ? "désactiver" : "réactiver";
    if (!window.confirm(`Voulez-vous ${action} la fiche de ${employee.prenom} ${employee.nom} ?`)) return;
    try {
      const response = await authFetch(API + (employee.actif ? `/employes/${employee.id}` : `/employes/${employee.id}/reactiver`), {
        method: employee.actif ? "DELETE" : "POST",
      });
      if (!response.ok) throw new Error("Modification du statut impossible.");
      toast(employee.actif ? "Fiche employé désactivée." : "Fiche employé réactivée.", "success");
      await load();
    } catch (error) {
      toast(error.message, "error");
    }
  };

  const visible = items.filter((item) => (showInactive || item.actif) &&
    [item.matricule, item.nom, item.prenom].some((value) => String(value || "").toLowerCase().includes(search.toLowerCase())));

  const fields = [
    ["matricule", "Matricule", true], ["nom", "Nom", true], ["prenom", "Prénom", true],
    ["numeroCin", "CIN"], ["dateNaissance", "Date de naissance", false, "date"],
    ["telephone", "Téléphone"], ["adresse", "Adresse"], ["fonction", "Fonction"],
    ["typeContrat", "Type de contrat"], ["email", "Email", false, "email"],
    ["dateEmbauche", "Date d'embauche", false, "date"],
  ];

  return <div className="space-y-6 text-slate-800">
    <header>
      <h1 className="text-2xl font-bold">Référentiel des employés</h1>
      <p className="mt-1 text-sm text-slate-500">Fiches RH indépendantes des comptes de connexion. Accès réservé à l'ADMIN.</p>
    </header>

    <form onSubmit={save} className="card border border-slate-200 bg-white p-5 shadow-sm space-y-4">
      <div className="flex items-center justify-between gap-3">
        <h2 className="font-semibold">{editing ? "Modifier une fiche employé" : "Créer une fiche employé"}</h2>
        {editing && <button type="button" className="btn btn-ghost btn-sm" onClick={reset}>Annuler la modification</button>}
      </div>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {fields.map(([key, label, required, type]) => <label key={key} className="form-control">
          <span className="label-text mb-1 font-medium">{label}{required ? " *" : ""}</span>
          <input className="input input-bordered input-sm w-full" type={type || "text"} required={Boolean(required)}
            maxLength={key === "adresse" ? 500 : key === "email" ? 254 : 150}
            value={form[key]} onChange={(event) => setForm({ ...form, [key]: event.target.value })} />
        </label>)}
      </div>
      <div className="flex justify-end">
        <button disabled={busy} className="btn btn-primary btn-sm">{busy ? "Enregistrement…" : editing ? "Enregistrer les modifications" : "Créer la fiche"}</button>
      </div>
    </form>

    <section className="card border border-slate-200 bg-white p-5 shadow-sm space-y-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h2 className="font-semibold">Employés enregistrés ({visible.length})</h2>
        <div className="flex flex-wrap items-center gap-3">
          <input className="input input-bordered input-sm" placeholder="Rechercher dans le référentiel…" value={search} onChange={(e) => setSearch(e.target.value)} />
          <label className="flex items-center gap-2 text-sm"><input type="checkbox" className="checkbox checkbox-sm" checked={showInactive} onChange={(e) => setShowInactive(e.target.checked)} />Afficher les inactifs</label>
        </div>
      </div>
      <div className="overflow-x-auto">
        <table className="table table-sm">
          <thead><tr><th>Matricule</th><th>Nom et prénom</th><th>Fonction</th><th>Contrat</th><th>Statut</th><th>Actions</th></tr></thead>
          <tbody>{visible.map((employee) => <tr key={employee.id}>
            <td className="font-mono">{employee.matricule}</td>
            <td>{employee.prenom} {employee.nom}</td>
            <td>{employee.fonction || "—"}</td>
            <td>{employee.typeContrat || "—"}</td>
            <td><span className={`badge badge-sm ${employee.actif ? "badge-success" : "badge-ghost"}`}>{employee.actif ? "Actif" : "Inactif"}</span></td>
            <td><div className="flex gap-2">
              <button type="button" className="btn btn-xs btn-outline" onClick={() => edit(employee)}>Modifier</button>
              <button type="button" className={`btn btn-xs ${employee.actif ? "btn-error" : "btn-success"}`} onClick={() => changeStatus(employee)}>{employee.actif ? "Désactiver" : "Réactiver"}</button>
            </div></td>
          </tr>)}
          {!visible.length && <tr><td colSpan="6" className="py-8 text-center text-slate-400">Aucune fiche trouvée. Les employés seront ajoutés ultérieurement.</td></tr>}</tbody>
        </table>
      </div>
    </section>
  </div>;
}
