"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  CalendarDays, ClipboardList, GitBranch, Users, ShieldCheck,
  FileCheck2, Building2, History, ArrowRight, Bell,
} from "lucide-react";
import { getCurrentUser } from "@/lib/apiClient";

const roleLinks = {
  ADMIN: [
    { href: "/dashboard/consultation-demandes", label: "Consultation des demandes", description: "Suivre les demandes et les décisions.", icon: ClipboardList },
    { href: "/dashboard/solde", label: "Gestion des soldes", description: "Consulter et administrer les soldes.", icon: CalendarDays },
    { href: "/dashboard/demande", label: "Mes demandes", description: "Consulter les demandes liées à votre compte.", icon: ClipboardList },
    { href: "/dashboard/historique", label: "Mon historique", description: "Retrouver les mouvements liés à vos congés.", icon: History },
    { href: "/dashboard/typeConge", label: "Types de congés", description: "Administrer les types de congés disponibles.", icon: FileCheck2 },
    { href: "/dashboard/services", label: "Services", description: "Gérer les services et leurs rattachements.", icon: Building2 },
    { href: "/dashboard/utilisateurs", label: "Collaborateurs", description: "Gérer les comptes, les rôles et les affectations.", icon: Users },
    { href: "/dashboard/reinitialisations", label: "Réinitialisations", description: "Examiner et traiter les demandes de réinitialisation des comptes.", icon: ShieldCheck },
    { href: "/dashboard/departements", label: "Départements", description: "Administrer l'organisation des départements.", icon: Building2 },
  ],
  MANAGER: [
    { href: "/dashboard/consultation-demandes", label: "Demandes de mon équipe", description: "Consulter les demandes de vos collaborateurs.", icon: ClipboardList },
  ],
  EMPLOYE: [
    { href: "/dashboard/demande", label: "Mes demandes", description: "Consulter l'état et l'historique de vos demandes.", icon: ClipboardList },
    { href: "/dashboard/solde", label: "Mes soldes", description: "Consulter vos droits et jours restants.", icon: CalendarDays },
    { href: "/dashboard/historique", label: "Mon historique", description: "Retrouver les mouvements liés à vos congés.", icon: History },
  ],
  ASSISTANT_DIRECTION: [
    { href: "/dashboard/circuit-conge", label: "Circuit des congés", description: "Enregistrer les demandes verbales et suivre les dossiers.", icon: GitBranch },
  ],
  ADMINISTRATION_PERSONNEL: [
    { href: "/dashboard/reliquats", label: "Vérification des reliquats", description: "Contrôler et retourner les reliquats des dossiers.", icon: ShieldCheck },
  ],
  CHEF_DEPARTEMENT: [
    { href: "/dashboard/chef-departement", label: "Examen du chef", description: "Examiner les dossiers de votre département.", icon: FileCheck2 },
  ],
  DIRECTEUR: [
    { href: "/dashboard/directeur", label: "Décisions du directeur", description: "Consulter les dossiers et rendre une décision.", icon: ShieldCheck },
  ],
};

export default function Dashboard() {
  const [user, setUser] = useState(null);

  useEffect(() => {
    setUser(getCurrentUser());
  }, []);

  const role = String(user?.role || "").trim().toUpperCase().replace(/^ROLE_/, "");
  const links = roleLinks[role] || [];

  return (
    <main className="space-y-7">
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <p className="text-xs font-bold uppercase tracking-[0.18em] text-blue-700">
          SPAT · Gestion des congés
        </p>
        <h1 className="mt-3 text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
          Bienvenue{user?.prenom ? `, ${user.prenom}` : ""}
        </h1>
        <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600">
          Retrouvez ici les espaces de travail correspondant à votre fonction.
          Chaque espace dispose de sa propre page pour consulter et traiter les informations.
        </p>
      </section>

      <section>
        <div className="mb-4 flex items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">Vos espaces de travail</h2>
            <p className="mt-1 text-sm text-slate-500">Accès direct aux fonctionnalités de votre rôle.</p>
          </div>
          <Bell className="h-5 w-5 text-slate-400" aria-hidden="true" />
        </div>

        {links.length ? (
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
            {links.map(({ href, label, description, icon: Icon }) => (
              <Link
                key={href}
                href={href}
                className="group rounded-xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:border-blue-300 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <div className="flex items-start justify-between gap-4">
                  <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-50 text-blue-700">
                    <Icon className="h-5 w-5" />
                  </span>
                  <ArrowRight className="h-4 w-4 text-slate-400 transition group-hover:translate-x-1 group-hover:text-blue-700" />
                </div>
                <h3 className="mt-4 font-semibold text-slate-900">{label}</h3>
                <p className="mt-1 text-sm leading-5 text-slate-500">{description}</p>
              </Link>
            ))}
          </div>
        ) : (
          <p className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
            Aucun espace n'est associé à ce rôle. Contactez l'administrateur.
          </p>
        )}
      </section>
    </main>
  );
}
