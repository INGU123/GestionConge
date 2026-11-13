"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getCurrentUser } from "@/lib/apiClient";

const ROLE_HOME = {
  ADMIN: { title: "Espace administrateur", description: "Consultez les dossiers de reliquat et gérez les paramètres de l’application.", href: "/dashboard/consultation-demandes", action: "Suivi des reliquats" },
  MANAGER: { title: "Espace professionnel", description: "Bienvenue dans votre espace de travail." },
  ASSISTANT_DIRECTION: { title: "Espace Assistant de direction", description: "Enregistrez les demandes verbales de reliquat et suivez leur traitement." },
  ADMINISTRATION_PERSONNEL: { title: "Espace Administration du personnel", description: "Vérifiez les soldes disponibles et retournez les reliquats aux assistants." },
  CHEF_DEPARTEMENT: { title: "Espace Chef de département", description: "Examinez les dossiers transmis par l’assistant de direction." },
  DIRECTEUR: { title: "Espace Directeur", description: "Consultez les dossiers transmis et rendez votre décision." },
  EMPLOYE: { title: "Espace collaborateur", description: "Consultez vos soldes et votre historique de congés.", href: "/dashboard/solde", action: "Mes soldes" },
};

export default function Dashboard() {
  const [user, setUser] = useState(null);
  useEffect(() => {
    const current = getCurrentUser();
    if (current) setUser(current);
  }, []);

  const role = String(user?.role || "").trim().toUpperCase().replace(/^ROLE_/, "");
  const home = ROLE_HOME[role] || ROLE_HOME.EMPLOYE;

  return (
    <section className="mx-auto max-w-4xl space-y-6">
      <header>
        <p className="text-sm font-semibold uppercase tracking-wide text-primary">Port de Toamasina</p>
        <h1 className="mt-2 text-3xl font-bold">Accueil</h1>
        <p className="mt-2 text-base-content/70">Bienvenue{user?.prenom ? `, ${user.prenom}` : ""} dans l’application de gestion des congés.</p>
      </header>
      <article className="card border border-base-300 bg-base-100 shadow-sm">
        <div className="card-body gap-3">
          <h2 className="card-title">{home.title}</h2>
          <p className="text-base-content/70">{home.description}</p>
          {home.href && <div className="card-actions mt-2"><Link className="btn btn-primary" href={home.href}>{home.action}</Link></div>}
        </div>
      </article>
      <p className="text-sm text-base-content/60">Utilisez le menu latéral pour accéder aux fonctionnalités autorisées selon votre rôle.</p>
    </section>
  );
}
