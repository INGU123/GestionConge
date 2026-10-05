"use client";

import { useEffect, useMemo, useState } from "react";
import { getCurrentUser } from "@/lib/apiClient";
import { getAllDemandes } from "@/app/api/demandeConge/demandeConge";
import { getJoursFeries } from "@/app/api/joursFeries/joursFeries";

const toDateKey = (date) => {
  if (!date) return "";
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

export default function CalendrierRhPage() {
  const [user] = useState(() => getCurrentUser());
  const [demandes, setDemandes] = useState([]);
  const [joursFeries, setJoursFeries] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let ignore = false;

    const loadData = async () => {
      try {
        const [allDemandes, allJoursFeries] = await Promise.all([
          getAllDemandes(),
          getJoursFeries(),
        ]);

        if (!ignore) {
          setDemandes(Array.isArray(allDemandes) ? allDemandes.filter((d) => d.statut === "VALIDEE") : []);
          setJoursFeries(Array.isArray(allJoursFeries) ? allJoursFeries : []);
        }
      } catch {
        if (!ignore) {
          setDemandes([]);
          setJoursFeries([]);
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    };

    void loadData();
    return () => {
      ignore = true;
    };
  }, []);

  const holidayMap = useMemo(() => {
    const map = new Map();
    joursFeries.forEach((jour) => {
      const raw = jour?.date || jour?.jour;
      if (!raw) return;
      const parsed = new Date(raw);
      if (!Number.isNaN(parsed.getTime())) {
        map.set(toDateKey(parsed), jour.libelle || "Jour férié");
      }
    });
    return map;
  }, [joursFeries]);

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
    return demandes.filter((event) => {
      const start = new Date(event.dateDebut);
      const end = new Date(event.dateFin);
      return target >= start && target <= end;
    });
  };

  const isAdminOrManager = user?.role?.toUpperCase() === "ADMIN" || user?.role?.toUpperCase() === "MANAGER";

  return (
    <div className="space-y-6 p-4" style={{ color: "var(--text-main)" }}>
      <div className="rounded-2xl p-6 shadow-md" style={{ background: "linear-gradient(135deg, #081c30, #123a6d)", color: "#edf6ff" }}>
        <p className="text-xs uppercase tracking-[0.2em]" style={{ color: "#7dd3fc" }}>RH</p>
        <h1 className="mt-2 text-2xl font-bold">Calendrier des congés validés</h1>
      </div>

      {!isAdminOrManager && (
        <div className="alert alert-info text-white">Accès réservé à l’administration et aux managers.</div>
      )}

      {loading ? (
        <div className="rounded-2xl border p-5" style={{ background: "var(--panel)", borderColor: "var(--border)" }}>Chargement du calendrier…</div>
      ) : (
        <div className="rounded-2xl border p-4" style={{ background: "var(--panel)", borderColor: "var(--border)" }}>
          <div className="mb-4 flex items-center justify-between gap-4">
            <h2 className="text-lg font-semibold">
              {now.toLocaleDateString("fr-FR", { month: "long", year: "numeric" })}
            </h2>
            <span className="badge badge-success">{demandes.length} congés validés</span>
          </div>

          <div className="mb-4 flex flex-wrap gap-2 text-xs" style={{ color: "var(--text-muted)" }}>
            {joursFeries.slice(0, 6).map((jour) => (
              <span key={jour.id || jour.date} className="badge badge-ghost">
                {new Date(jour.date).toLocaleDateString("fr-FR", { day: "2-digit", month: "2-digit" })} · {jour.libelle}
              </span>
            ))}
          </div>

          <div className="grid grid-cols-7 gap-2 text-center text-xs font-semibold uppercase" style={{ color: "var(--text-muted)" }}>
            {['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'].map((day) => (
              <div key={day} className="py-2">{day}</div>
            ))}
          </div>

          <div className="grid grid-cols-7 gap-2">
            {monthDays.map((value, index) => {
              if (!value) {
                return <div key={`empty-${index}`} className="min-h-28 rounded-xl border border-dashed p-2" style={{ background: "var(--panel-soft)", borderColor: "var(--border)" }} />;
              }

              const targetDate = new Date(now.getFullYear(), now.getMonth(), value);
              const holidayLabel = holidayMap.get(toDateKey(targetDate));
              const dayEvents = getDayEvents(value);
              const isWeekend = targetDate.getDay() === 0 || targetDate.getDay() === 6;

              return (
                <div
                  key={value}
                  className={`min-h-28 rounded-xl border p-2 ${isWeekend ? "calendar-day-weekend" : ""} ${holidayLabel ? "calendar-day-holiday" : ""} ${dayEvents.length ? "calendar-day-leave" : ""}`}
                  style={{ background: "var(--panel-soft)", borderColor: "var(--border)" }}
                >
                  <div className="text-sm font-semibold" style={{ color: "var(--text-main)" }}>{value}</div>
                  <div className="mt-2 space-y-1">
                    {holidayLabel && (
                      <div className="rounded px-2 py-1 text-[10px] font-medium" style={{ background: "rgba(45,212,191,0.16)", color: "var(--success)" }}>
                        {holidayLabel}
                      </div>
                    )}
                    {dayEvents.map((event) => (
                      <div key={event.id} className="rounded px-2 py-1 text-[10px] font-medium" style={{ background: "rgba(59,130,246,0.12)", color: "var(--primary)" }}>
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
