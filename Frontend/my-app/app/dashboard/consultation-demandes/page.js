"use client";

import { useEffect, useMemo, useState, useSyncExternalStore } from "react";
import { authFetch, getCurrentUser } from "@/lib/apiClient";
import {
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  Clock3,
  FileText,
  Search,
  SlidersHorizontal,
} from "lucide-react";

const API = "http://localhost:8080";
const PAGE_SIZES = [10, 20, 50];

function getRoleSnapshot() {
  return String(getCurrentUser()?.role || "").trim().toUpperCase().replace(/^ROLE_/, "");
}

function subscribeToRole() {
  return () => {};
}

function parseLocalDate(value) {
  if (!value) return null;
  if (Array.isArray(value) && value.length >= 3) {
    return new Date(value[0], value[1] - 1, value[2]);
  }
  const raw = String(value);
  const match = raw.match(/^(\d{4})-(\d{2})-(\d{2})(?:T(\d{2}):(\d{2})(?::(\d{2}))?)?/);
  if (match) {
    return new Date(
      Number(match[1]),
      Number(match[2]) - 1,
      Number(match[3]),
      Number(match[4] || 0),
      Number(match[5] || 0),
      Number(match[6] || 0),
    );
  }
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

function formatDate(value) {
  const date = parseLocalDate(value);
  return date
    ? new Intl.DateTimeFormat("fr-FR", { day: "2-digit", month: "short", year: "numeric" }).format(date)
    : "—";
}

function getPeriodBounds(period) {
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());

  if (period === "week") {
    const start = new Date(today);
    const day = (start.getDay() + 6) % 7;
    start.setDate(start.getDate() - day);
    const end = new Date(start);
    end.setDate(end.getDate() + 7);
    return { start, end };
  }
  if (period === "sixMonths") {
    return { start: new Date(today.getFullYear(), today.getMonth() - 6, today.getDate()), end: null };
  }
  if (period === "year") {
    return { start: new Date(today.getFullYear(), 0, 1), end: null };
  }
  return { start: null, end: null };
}

function isInPeriod(row, period) {
  if (period === "all") return true;
  const date = parseLocalDate(row.dateCreation || row.createdAt);
  if (!date) return false;
  const { start, end } = getPeriodBounds(period);
  return (!start || date >= start) && (!end || date < end);
}

function getStatusLabel(status) {
  const labels = {
    RELIQUAT_EN_ATTENTE: "Reliquat en attente",
    RELIQUAT_RETOURNE: "Reliquat retourné",
    CHEF_EN_ATTENTE: "En attente du chef",
    DIRECTEUR_EN_ATTENTE: "En attente de la Direction",
    VALIDEE: "Validée",
    ACCEPTEE: "Acceptée",
    REFUSEE: "Refusée",
    EN_ATTENTE: "En attente",
    ANNULEE: "Annulée",
    ANNULEE_PAR_UTILISATEUR: "Annulée",
  };
  return labels[status] || String(status || "Statut inconnu").replaceAll("_", " ");
}

function getStatusStyle(status) {
  if (["VALIDEE", "ACCEPTEE"].includes(status)) return "bg-emerald-50 text-emerald-700 ring-emerald-600/20";
  if (status === "REFUSEE") return "bg-rose-50 text-rose-700 ring-rose-600/20";
  if (String(status || "").includes("ATTENTE")) return "bg-amber-50 text-amber-700 ring-amber-600/20";
  if (status === "RELIQUAT_RETOURNE") return "bg-sky-50 text-sky-700 ring-sky-600/20";
  return "bg-slate-100 text-slate-700 ring-slate-500/20";
}

function getCollaborator(row) {
  const prenom = row.utilisateurPrenom || row.utilisateur?.prenom || "";
  const nom = row.utilisateurNom || row.utilisateur?.nom || "";
  return `${prenom} ${nom}`.trim() || "Collaborateur non renseigné";
}

function getMatricule(row) {
  return row.utilisateurMatricule || row.utilisateur?.matricule || "Matricule indisponible";
}

function StatCard({ label, value, icon: Icon, active, onClick, hint }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={`min-w-0 rounded-2xl border bg-white p-4 text-left shadow-sm transition hover:-translate-y-0.5 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${active ? "border-blue-500 ring-2 ring-blue-100" : "border-slate-200"}`}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p>
          <p className="mt-2 text-2xl font-bold tabular-nums text-slate-900">{value}</p>
          <p className="mt-1 text-xs text-slate-400">{hint}</p>
        </div>
        <span className={`rounded-xl p-2.5 ${active ? "bg-blue-600 text-white" : "bg-slate-100 text-slate-600"}`}>
          <Icon className="h-5 w-5" />
        </span>
      </div>
    </button>
  );
}

export default function ConsultationDemandesPage() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const role = useSyncExternalStore(subscribeToRole, getRoleSnapshot, () => "");
  const [period, setPeriod] = useState("week");
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [pageSize, setPageSize] = useState(10);
  const [page, setPage] = useState(1);
  const [loadError, setLoadError] = useState("");

  useEffect(() => {
    if (!role) return;
    let active = true;

    async function load() {
      setLoading(true);
      setLoadError("");
      try {
        const legacyPromise = authFetch(`${API}/conge/all`).then(async (response) => {
          if (!response.ok) throw new Error("Consultation des demandes impossible.");
          return response.json();
        });

        if (role !== "ADMIN") {
          const legacy = await legacyPromise;
          if (active) setRows(Array.isArray(legacy) ? legacy : []);
          return;
        }

        const [legacyResult, circuitResult] = await Promise.allSettled([
          legacyPromise,
          authFetch(`${API}/circuit-conge/admin/consultation`).then(async (response) => {
            if (!response.ok) throw new Error("Registre du circuit indisponible.");
            return response.json();
          }),
        ]);

        if (legacyResult.status === "rejected") throw legacyResult.reason;
        const legacy = Array.isArray(legacyResult.value) ? legacyResult.value : [];
        const circuits = circuitResult.status === "fulfilled" && Array.isArray(circuitResult.value)
          ? circuitResult.value
          : [];
        const linkedLegacyIds = new Set(
          circuits.map((item) => item.legacyDemandeId).filter((id) => id != null).map(String),
        );

        const circuitRows = circuits.map((item) => ({
          ...item,
          rowKind: "circuit",
          rowKey: `circuit-${item.id}`,
          createdAt: item.dateCreation,
        }));
        const legacyRows = legacy
          .filter((item) => !linkedLegacyIds.has(String(item.id)))
          .map((item) => ({
            ...item,
            rowKind: "legacy",
            rowKey: `legacy-${item.id}`,
            createdAt: item.dateCreation,
          }));

        const combined = [...circuitRows, ...legacyRows].sort((a, b) => {
          const dateA = parseLocalDate(a.dateCreation || a.createdAt)?.getTime() || 0;
          const dateB = parseLocalDate(b.dateCreation || b.createdAt)?.getTime() || 0;
          return dateB - dateA;
        });
        if (active) {
          setRows(combined);
          if (circuitResult.status === "rejected") {
            setLoadError("Les dossiers du circuit hiérarchique n'ont pas pu être chargés.");
          }
        }
      } catch (error) {
        if (active) {
          setRows([]);
          setLoadError(error.message || "Impossible de charger le registre.");
        }
      } finally {
        if (active) setLoading(false);
      }
    }

    void load();
    return () => {
      active = false;
    };
  }, [role]);

  const stats = useMemo(() => ({
    week: rows.filter((row) => isInPeriod(row, "week")).length,
    sixMonths: rows.filter((row) => isInPeriod(row, "sixMonths")).length,
    year: rows.filter((row) => isInPeriod(row, "year")).length,
    all: rows.length,
  }), [rows]);

  const availableStatuses = useMemo(
    () => [...new Set(rows.map((row) => row.statut).filter(Boolean))].sort(),
    [rows],
  );

  const filteredRows = useMemo(() => {
    const query = search.trim().toLocaleLowerCase("fr");
    return rows.filter((row) => {
      if (!isInPeriod(row, period)) return false;
      if (statusFilter !== "ALL" && row.statut !== statusFilter) return false;
      if (!query) return true;
      const searchable = [
        getCollaborator(row),
        getMatricule(row),
        row.serviceNom,
        row.nomService,
        row.typeCongeLibelle,
        row.typeConge?.libelle,
        row.statut,
        row.id,
      ].filter(Boolean).join(" ").toLocaleLowerCase("fr");
      return searchable.includes(query);
    });
  }, [rows, period, search, statusFilter]);


  const pageCount = Math.max(1, Math.ceil(filteredRows.length / pageSize));
  const visibleRows = filteredRows.slice((page - 1) * pageSize, page * pageSize);

  if (role !== "ADMIN") {
    return (
      <section className="space-y-5">
        <header>
          <h1 className="text-2xl font-bold text-slate-900">Consultation des demandes de congé</h1>
          <p className="mt-1 text-sm text-slate-500">Consultation des demandes déposées par les collaborateurs de votre équipe.</p>
        </header>
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="max-h-[68vh] overflow-auto">
            <table className="table table-sm w-full">
              <thead className="sticky top-0 z-10 bg-slate-50 text-xs uppercase text-slate-500">
                <tr><th>Collaborateur</th><th>Type</th><th>Période</th><th>Jours</th><th>Statut</th></tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.id}>
                    <td><div className="font-semibold">{getCollaborator(row)}</div><div className="text-xs text-slate-500">{getMatricule(row)}</div></td>
                    <td>{row.typeCongeLibelle || `Type #${row.typeCongeId}`}</td>
                    <td className="whitespace-nowrap">{formatDate(row.dateDebut)} → {formatDate(row.dateFin)}</td>
                    <td className="font-semibold tabular-nums">{row.nombreJours ?? "—"} j</td>
                    <td><span className={`rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ring-inset ${getStatusStyle(row.statut)}`}>{getStatusLabel(row.statut)}</span></td>
                  </tr>
                ))}
                {!loading && rows.length === 0 && <tr><td colSpan="5" className="py-10 text-center text-slate-400">Aucune demande à afficher.</td></tr>}
              </tbody>
            </table>
          </div>
          {loading && <p className="p-6 text-center text-sm text-slate-500">Chargement…</p>}
        </div>
      </section>
    );
  }

  const periodLabel = {
    week: "Cette semaine",
    sixMonths: "Les 6 derniers mois",
    year: "Cette année",
    all: "Tout le registre",
  }[period];

  return (
    <section className="space-y-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.18em] text-blue-700">SPAT · Administration</p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">Registre des demandes de congé</h1>
          <p className="mt-1 max-w-2xl text-sm text-slate-500">Vue globale en lecture seule · Suivi chronologique de tous les dossiers et de leurs décisions.</p>
        </div>
        <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-600 shadow-sm">
          <FileText className="h-4 w-4 text-blue-600" />
          <span className="font-semibold tabular-nums">{rows.length}</span>
          <span>dossier(s) enregistré(s)</span>
        </div>
      </header>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Cette semaine" value={stats.week} icon={CalendarDays} active={period === "week"} onClick={() => { setPeriod("week"); setPage(1); }} hint="Demandes enregistrées depuis lundi" />
        <StatCard label="6 derniers mois" value={stats.sixMonths} icon={Clock3} active={period === "sixMonths"} onClick={() => { setPeriod("sixMonths"); setPage(1); }} hint="Période glissante de six mois" />
        <StatCard label="Cette année" value={stats.year} icon={CalendarDays} active={period === "year"} onClick={() => { setPeriod("year"); setPage(1); }} hint="Depuis le 1er janvier" />
        <StatCard label="Tout le registre" value={stats.all} icon={FileText} active={period === "all"} onClick={() => { setPeriod("all"); setPage(1); }} hint="Depuis le début de l'application" />
      </div>

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="flex flex-col gap-4 border-b border-slate-200 p-4 sm:p-5 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 className="font-bold text-slate-900">{periodLabel}</h2>
            <p className="mt-1 text-xs text-slate-500">{filteredRows.length} résultat(s) correspondant aux filtres</p>
          </div>
          <div className="flex flex-col gap-2 sm:flex-row">
            <label className="relative block sm:min-w-64">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                type="search"
                value={search}
                onChange={(event) => { setSearch(event.target.value); setPage(1); }}
                placeholder="Nom, matricule, type, dossier…"
                className="input input-bordered h-10 w-full pl-9 text-sm"
              />
            </label>
            <label className="flex h-10 items-center gap-2 rounded-lg border border-slate-300 px-3">
              <SlidersHorizontal className="h-4 w-4 text-slate-400" />
              <select value={statusFilter} onChange={(event) => { setStatusFilter(event.target.value); setPage(1); }} className="max-w-44 bg-transparent text-sm outline-none">
                <option value="ALL">Tous les statuts</option>
                {availableStatuses.map((status) => <option key={status} value={status}>{getStatusLabel(status)}</option>)}
              </select>
            </label>
          </div>
        </div>

        {loadError && <div className="border-b border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">{loadError}</div>}

        <div className="max-h-[min(66vh,720px)] overflow-auto">
          <table className="table table-sm w-full min-w-[900px]">
            <thead className="sticky top-0 z-10 bg-slate-50 text-[11px] uppercase tracking-wide text-slate-500">
              <tr>
                <th className="py-4">Collaborateur</th>
                <th>Type de congé</th>
                <th>Période demandée</th>
                <th className="text-center">Durée</th>
                <th>Enregistrée le</th>
                <th>État du dossier</th>
              </tr>
            </thead>
            <tbody>
              {visibleRows.map((row) => (
                <tr key={row.rowKey || `${row.rowKind || "demande"}-${row.id}`} className="hover:bg-blue-50/40">
                  <td className="py-3">
                    <div className="font-semibold text-slate-800">{getCollaborator(row)}</div>
                    <div className="mt-0.5 text-xs text-slate-500">{getMatricule(row)}</div>
                    {(row.serviceNom || row.nomService) && <div className="mt-0.5 text-xs text-slate-400">{row.serviceNom || row.nomService}</div>}
                    <div className="mt-1 text-[10px] font-medium uppercase tracking-wide text-slate-400">{row.rowKind === "circuit" ? `Dossier #${row.id}` : `Demande #${row.id}`}</div>
                  </td>
                  <td className="text-sm text-slate-700">{row.typeCongeLibelle || row.typeConge?.libelle || `Type #${row.typeCongeId ?? "—"}`}</td>
                  <td className="whitespace-nowrap">
                    <div className="font-medium text-slate-800">{formatDate(row.dateDebut)}</div>
                    <div className="mt-0.5 text-xs text-slate-500">au {formatDate(row.dateFin)}</div>
                  </td>
                  <td className="text-center"><span className="rounded-lg bg-slate-100 px-2.5 py-1 font-bold tabular-nums text-slate-700">{row.nombreJours ?? "—"} j</span></td>
                  <td className="whitespace-nowrap text-sm text-slate-600">{formatDate(row.dateCreation || row.createdAt)}</td>
                  <td><span title={row.motifRefus || ""} className={`inline-flex max-w-48 rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ring-inset ${getStatusStyle(row.statut)}`}>{getStatusLabel(row.statut)}</span></td>
                </tr>
              ))}
              {!loading && visibleRows.length === 0 && (
                <tr><td colSpan="6" className="py-14 text-center">
                  <div className="mx-auto flex max-w-sm flex-col items-center">
                    <Search className="h-8 w-8 text-slate-300" />
                    <p className="mt-3 font-semibold text-slate-600">Aucune demande trouvée</p>
                    <p className="mt-1 text-sm text-slate-400">Modifiez la période, le statut ou votre recherche.</p>
                  </div>
                </td></tr>
              )}
            </tbody>
          </table>
          {loading && <div className="p-10 text-center text-sm text-slate-500">Chargement du registre…</div>}
        </div>

        <footer className="flex flex-col gap-3 border-t border-slate-200 bg-slate-50/70 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-2 text-xs text-slate-500">
            <span>Afficher</span>
            <select value={pageSize} onChange={(event) => { setPageSize(Number(event.target.value)); setPage(1); }} className="select select-bordered select-xs bg-white">
              {PAGE_SIZES.map((size) => <option key={size} value={size}>{size}</option>)}
            </select>
            <span>par page · {filteredRows.length === 0 ? 0 : (page - 1) * pageSize + 1}–{Math.min(page * pageSize, filteredRows.length)} sur {filteredRows.length}</span>
          </div>
          <div className="flex items-center justify-between gap-3 sm:justify-end">
            <span className="text-xs text-slate-500">Page {page} / {pageCount}</span>
            <div className="join">
              <button type="button" className="btn btn-sm join-item" disabled={page <= 1} onClick={() => setPage((current) => Math.max(1, current - 1))} aria-label="Page précédente"><ChevronLeft className="h-4 w-4" /></button>
              <button type="button" className="btn btn-sm join-item" disabled={page >= pageCount} onClick={() => setPage((current) => Math.min(pageCount, current + 1))} aria-label="Page suivante"><ChevronRight className="h-4 w-4" /></button>
            </div>
          </div>
        </footer>
      </div>
    </section>
  );
}
