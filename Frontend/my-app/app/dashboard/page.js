"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { getCurrentUser } from "@/lib/apiClient";

const ROLE_WORKSPACES = {
  ADMIN: "/dashboard/consultation-demandes",
  MANAGER: "/dashboard/consultation-demandes",
  ASSISTANT_DIRECTION: "/dashboard/circuit-conge",
  ADMINISTRATION_PERSONNEL: "/dashboard/reliquats",
  CHEF_DEPARTEMENT: "/dashboard/chef-departement",
  DIRECTEUR: "/dashboard/directeur",
  EMPLOYE: "/dashboard/solde",
};

export default function Dashboard() {
  const router = useRouter();

  useEffect(() => {
    const user = getCurrentUser();
    if (!user) {
      router.replace("/");
      return;
    }

    const role = String(user.role || "")
      .trim()
      .toUpperCase()
      .replace(/^ROLE_/, "");

    router.replace(ROLE_WORKSPACES[role] || "/dashboard/solde");
  }, [router]);

  return (
    <div className="flex min-h-[50vh] items-center justify-center">
      <span className="loading loading-spinner loading-lg text-primary" />
      <span className="ml-3 text-sm text-slate-500">Ouverture de votre espace de travail…</span>
    </div>
  );
}
