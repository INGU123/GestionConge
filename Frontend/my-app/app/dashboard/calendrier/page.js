"use client";

import { useEffect, useMemo, useState } from "react";
import { authFetch, getCurrentUser } from "@/lib/apiClient";
import { getAllDemandes } from "@/app/api/demandeConge/demandeConge";

export default function CalendrierRhPage() {
  const [user, setUser] = useState(null);
  const [demandes, setDemandes] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const currentUser = getCurrentUser();
    setUser(currentUser);
    loadDemandes();
  }, []);

  const loadDemandes = async () => {
    try {
      const all = await getAllDemandes();
      setDemandes(Array.isArray(all) ? all.filter((d) => d.statut === "VALIDEE") : []);
    } catch {
      setDemandes([]);
    } finally {
      setLoading(false);
    }
  };

  const events = useMemo(() =>
    demandes.map((d) => ({
      id: d.id,
      title: `Congé #${d.id}`,
      start: d.dateDebut,
      end: d.dateFin,
      utilisateurId: d.utilisateurId,
    })),
  [demandes]);

  const now = new Date();
  const monthStart = new Date(now.getFullYear(), now.getMonth(), 1);
  const monthDays = [];
  const firstWeekday = (monthStart.getDay() + 6) % 7;

  for (let i = 0; i < firstWeekday; i++) monthDays.push(null);
  const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
  for (let day = 1; day <= daysInMonth; day++) monthDays.push(day);

  while (monthDays.length % 7 !== 0) monthDays.push(null);

  const getDayEvents = (day) => {
    if (!day) return [];
    const target = new Date(now.getFullYear(), now.getMonth(), day);
    return events.filter((event) => {
      const start = new Date(event.start);
      const end = new Date(event.end);
      return target >= start && target <= end;
    });
  };

  const isAdminOrManager = user?.role?.toUpperCase() === "ADMIN" || user?.role?.toUpperCase() === "MANAGER";

  return (
    <div className="space-y-6 p-4">
      <div className="rounded-2xl bg-slate-900 text-white p-6 shadow-md">
        <p className="text-xs uppercase tracking-[0.2em] text-emerald-400">RH</p>
        <h1 className="mt-2 text-2xl font-bold">Calendrier des congés validés</h1>
      </div>

      {!isAdminOrManager && (
        <div className="alert alert-info text-white">Accès réservé à l’administration et aux managers.</div>
      )}

      {loading ? (
        <div>Chargement du calendrier…</div>
      ) : (
        <div className="rounded-2xl border border-slate-200 bg-white p-4">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold">
              {now.toLocaleDateString("fr-FR", { month: "long", year: "numeric" })}
            </h2>
            <span className="badge badge-success">{events.length} congés validés</span>
          </div>

          <div className="grid grid-cols-7 gap-2 text-center text-xs font-semibold uppercase text-slate-500">
            {['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'].map((day) => (
              <div key={day} className="py-2">{day}</div>
            ))}
          </div>

          <div className="grid grid-cols-7 gap-2">
            {monthDays.map((value, index) => {
              const dayEvents = value ? getDayEvents(value) : [];
              return (
                <div
                  key={value ?? `empty-${index}`}
                  className={`min-h-28 rounded-xl border p-2 ${value ? "bg-slate-50 border-slate-200" : "bg-slate-100 border-dashed border-slate-200"}`}
                >
                  {value && <div className="text-sm font-semibold text-slate-700">{value}</div>}
                  <div className="mt-2 space-y-1">
                    {dayEvents.map((event) => (
                      <div key={event.id} className="rounded bg-emerald-100 px-2 py-1 text-[10px] text-emerald-900">
                        Congé #{event.id}
                      </div>
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
