"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, usePathname } from "next/navigation";
import { getCurrentUser, clearAuthSession } from "@/lib/apiClient";
import NotificationHeaderMenu from "./notifications/page";

interface DashboardUser {
  id?: number;
  nom?: string;
  prenom?: string;
  email?: string;
  role?: string;
}

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const [user, setUser] = useState<DashboardUser | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    const fetchUser = async () => {
      try {
        const current = await getCurrentUser();
        if (current) {
          setUser(current);
        } else {
          router.push("/");
        }
      } catch (error) {
        console.error("Erreur de récupération utilisateur:", error);
        router.push("/");
      } finally {
        setLoading(false);
      }
    };

    fetchUser();
  }, [router]);

  const handleLogout = () => {
    clearAuthSession();
    router.push("/");
  };

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center bg-slate-100">
        <svg className="animate-spin h-8 w-8 text-blue-600" viewBox="0 0 24 24" fill="none">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        <p className="ml-3 text-slate-600 font-medium">Chargement en cours...</p>
      </div>
    );
  }

  // Normalisation stricte du rôle pour éviter tout bug d'affichage
  const userRoleClean = user?.role ? user.role.toString().trim().toUpperCase() : "";
  const isAdmin = userRoleClean === "ADMIN" || userRoleClean === "ROLE_ADMIN";
  const isManager = userRoleClean === "MANAGER" || userRoleClean === "ROLE_MANAGER" || isAdmin;

  const navItems = [
    {
      href: "/dashboard",
      label: "Accueil",
      icon: (
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
        </svg>
      ),
      show: true,
    },
    {
      href: "/dashboard/demande",
      label: "Mes Demandes",
      icon: (
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
      ),
      show: !isAdmin, // 🛑 Désactivé pour l'Administrateur
    },
    {
      href: "/dashboard/solde",
      label: isAdmin ? "Soldes & Quotas" : "Mes Soldes",
      icon: (
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 3.055A9.001 9.001 0 0120.945 13H11V3.055z" />
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M20.488 9H15V3.512A9.025 9.025 0 0120.488 9z" />
        </svg>
      ),
      show: true,
    },
    {
      href: "/dashboard/validation",
      label: "Validation des congés",
      icon: (
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      ),
      show: isManager,
    },
    {
      href: "/dashboard/typeConge",
      label: "Types de congés",
      icon: (
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 7h.01M7 3h5a1 1 0 01.707.293l7 7a1 1 0 010 1.414l-7 7a1 1 0 01-1.414 0l-7-7A1 1 0 013 12V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
      ),
      show: isAdmin,
    },
    {
      href: "/dashboard/utilisateurs",
      label: "Gestion Collaborateurs",
      icon: (
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
        </svg>
      ),
      show: isAdmin,
    },
    {
      href: "/dashboard/historique",
      label: "Historique",
      icon: (
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      ),
      show: true,
    },
  ];

  const visibleNavItems = navItems.filter((item) => item.show);

  return (
    <div className="flex min-h-screen flex-col bg-slate-100">
      {/* Header Fixe */}
      <header className="navbar bg-slate-900 text-white px-6 shadow-md flex justify-between items-center h-16 sticky top-0 z-50 shrink-0">
        <div className="flex items-center gap-3">
          <span className="text-xl text-blue-400">
            <svg className="w-6 h-6 inline-block" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z" />
            </svg>
          </span>
          <span className="font-bold text-lg tracking-wide text-white">
            Port de Toamasina - Gestion des Congés
          </span>
        </div>

        <div className="flex items-center gap-4">
          <NotificationHeaderMenu />
          <div className="text-right">
            <p className="text-sm font-semibold text-white">
              {user ? (user.prenom ? `${user.prenom} ${user.nom || ""}` : user.email) : "Invité"}
            </p>
            {user?.role && (
              <span
                className={`badge badge-sm uppercase font-bold text-xs ${isAdmin
                    ? "badge-primary text-white"
                    : isManager
                      ? "badge-secondary text-white"
                      : "badge-ghost text-slate-300"
                  }`}
              >
                {user.role}
              </span>
            )}
          </div>

          <button
            onClick={handleLogout}
            className="btn btn-outline btn-error btn-sm flex items-center gap-1"
            title="Se déconnecter"
          >
            <span>Déconnexion</span>
          </button>
        </div>
      </header>

      {/* Main Container */}
      <div className="flex flex-1">
        {/* Sidebar Fixe */}
        <aside className="w-64 bg-slate-800 text-slate-200 flex flex-col p-4 shadow-lg shrink-0 h-[calc(100vh-4rem)] sticky top-16 overflow-y-auto">
          <div className="text-xs uppercase tracking-wider text-slate-400 font-bold px-3 mb-3">
            Menu ({userRoleClean || "EMPLOYE"})
          </div>
          <nav className="flex flex-col space-y-1">
            {visibleNavItems.map((item) => {
              const isActive = pathname === item.href;
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${isActive
                      ? "bg-blue-600 text-white shadow"
                      : "text-slate-300 hover:bg-slate-700 hover:text-white"
                    }`}
                >
                  <span className="text-base">{item.icon}</span>
                  <span>{item.label}</span>
                </Link>
              );
            })}
          </nav>
        </aside>

        {/* Page Content */}
        <main className="flex-1 p-6 md:p-8 overflow-y-auto min-h-[calc(100vh-4rem)] bg-slate-50">
          {children}
        </main>
      </div>
    </div>
  );
}