"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { useRouter, usePathname } from "next/navigation";
import { FiMoon, FiSun } from "react-icons/fi";
import { getCurrentUser, clearAuthSession } from "@/lib/apiClient";
import NotificationHeaderMenu from "./notifications/page";
import { ToastProvider } from "../components/ToastProvider";

interface DashboardUser {
  id?: number;
  nom?: string;
  prenom?: string;
  email?: string;
  role?: string;
}

type ThemeMode = "day" | "night";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const [user, setUser] = useState<DashboardUser | null>(null);
  const [loading, setLoading] = useState(true);

  const [theme, setTheme] = useState<ThemeMode>(() => {
    if (typeof window === "undefined") return "day";

    const savedTheme = localStorage.getItem("dashboard-theme");

    return savedTheme === "night" ? "night" : "day";
  });

  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    localStorage.setItem("dashboard-theme", theme);

    document.documentElement.setAttribute("data-theme", theme);

    document.documentElement.style.colorScheme =
      theme === "night" ? "dark" : "light";
  }, [theme]);

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
      <div
        className="flex h-screen items-center justify-center"
        style={{ background: "var(--page-bg)" }}
      >
        <svg
          className="animate-spin h-8 w-8 text-blue-600"
          viewBox="0 0 24 24"
          fill="none"
        >
          <circle
            className="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
          />

          <path
            className="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          />
        </svg>

        <p
          className="ml-3 font-medium"
          style={{ color: "var(--text-muted)" }}
        >
          Chargement en cours...
        </p>
      </div>
    );
  }

  const userRoleClean = user?.role
    ? user.role.toString().trim().toUpperCase()
    : "";

  const isAdmin =
    userRoleClean === "ADMIN" || userRoleClean === "ROLE_ADMIN";

  const isManager =
    userRoleClean === "MANAGER" ||
    userRoleClean === "ROLE_MANAGER" ||
    isAdmin;

  const isNight = theme === "night";

  const navItems = [
    {
      href: "/dashboard",
      label: "Accueil",
      icon: (
        <svg
          className="w-4 h-4"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="2"
            d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0h6"
          />
        </svg>
      ),
      show: true,
    },

    {
      href: "/dashboard/demande",
      label: "Mes Demandes",
      icon: (
        <svg
          className="w-4 h-4"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="2"
            d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
          />
        </svg>
      ),
      show: !isAdmin,
    },

    {
      href: "/dashboard/solde",
      label: isAdmin ? "Soldes & Quotas" : "Mes Soldes",
      icon: (
        <svg
          className="w-4 h-4"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="2"
            d="M11 3.055A9.001 9.001 0 0120.945 13H11V3.055z"
          />

          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="2"
            d="M20.488 9H15V3.512A9.025 9.025 0 0120.488 9z"
          />
        </svg>
      ),
      show: true,
    },

    {
      href: "/dashboard/validation",
      label: "Validation des congés",
      icon: (
        <svg
          className="w-4 h-4"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="2"
            d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
      ),
      show: isManager,
    },

    {
      href: "/dashboard/typeConge",
      label: "Types de congés",
      icon: (
        <svg
          className="w-4 h-4"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="2"
            d="M7 7h.01M7 3h5a1 1 0 01.707.293l7 7a1 1 0 010 1.414l-7 7a1 1 0 01-1.414 0l-7-7A2 2 0 013 12V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414A1 1 1 0 0121 7.586V19a2 2 0 01-2 2H7a2 2 0 01-2-2V5"
          />
        </svg>
      ),
      show: isAdmin,
    },

    {
      href: "/dashboard/utilisateurs",
      label: "Gestion Collaborateurs",
      icon: (
        <svg
          className="w-4 h-4"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="2"
            d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"
          />
        </svg>
      ),
      show: isAdmin,
    },

    {
      href: "/dashboard/historique",
      label: "Historique",
      icon: (
        <svg
          className="w-4 h-4"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="2"
            d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
      ),
      show: true,
    },
  ];

  const visibleNavItems = navItems.filter((item) => item.show);

  return (
    <div className="theme-shell min-h-screen" data-theme={theme}>
      <ToastProvider>
        <div
          className="flex min-h-screen flex-col"
          style={{ background: "var(--page-bg)" }}
        >
        {/* HEADER */}
        <header
          className="navbar px-6 flex justify-between items-center h-16 sticky top-0 z-50 shrink-0 border-b"
          style={{
            background: "var(--header-bg)",
            color: "var(--header-text)",
            borderColor: "var(--primary-border)",
          }}
        >
          {/* LOGO + TITRE */}
          <div className="flex items-center gap-3">
            <Image
              src="/SpatLogo.png"
              alt="Logo SPAT"
              width={42}
              height={42}
              className="object-contain"
            />

            <span
              className="font-bold text-lg tracking-wide"
              style={{ color: "var(--header-text)" }}
            >
              Port de Toamasina - Gestion des Congés
            </span>
          </div>

          {/* ACTIONS HEADER */}
          <div className="flex items-center gap-3">

            {/* DAY / NIGHT */}
            <button
              type="button"
              onClick={() =>
                setTheme(isNight ? "day" : "night")
              }
              className="inline-flex items-center justify-center w-9 h-9 rounded-full border transition-all duration-200 hover:scale-105"
              style={{
                background: isNight
                  ? "rgba(14, 165, 233, 0.12)"
                  : "rgba(148, 163, 184, 0.12)",
                borderColor: "var(--primary-border)",
                color: "var(--header-text)",
              }}
              title={
                isNight
                  ? "Passer au mode jour"
                  : "Passer au mode nuit"
              }
              aria-label={
                isNight
                  ? "Passer au mode jour"
                  : "Passer au mode nuit"
              }
            >
              {isNight ? (
                <FiSun className="text-yellow-300 text-lg" />
              ) : (
                <FiMoon className="text-sky-300 text-lg" />
              )}
            </button>

            {/* NOTIFICATIONS */}
            <NotificationHeaderMenu />

            {/* UTILISATEUR */}
            <div className="text-right">
              <p
                className="text-sm font-semibold"
                style={{ color: "var(--header-text)" }}
              >
                {user
                  ? user.prenom
                    ? `${user.prenom} ${user.nom || ""}`
                    : user.email
                  : "Invité"}
              </p>

              {user?.role && (
                <span
                  className={`badge badge-sm uppercase font-bold text-xs ${
                    isAdmin
                      ? "badge-primary text-white"
                      : isManager
                      ? "badge-secondary text-white"
                      : "badge-ghost"
                  }`}
                >
                  {user.role}
                </span>
              )}
            </div>

            {/* DECONNEXION */}
            <button
              onClick={handleLogout}
              className="btn btn-outline btn-sm flex items-center gap-1"
              title="Se déconnecter"
            >
              <span>Déconnexion</span>
            </button>
          </div>
        </header>

        {/* CONTENU */}
        <div className="flex flex-1">

          {/* SIDEBAR */}
          <aside
            className="w-64 flex flex-col p-4 shadow-lg shrink-0 h-[calc(100vh-4rem)] sticky top-16 overflow-y-auto"
            style={{
              background: "var(--sidebar-bg)",
              color: "var(--sidebar-text)",
            }}
          >
            <div
              className="text-xs uppercase tracking-wider font-bold px-3 mb-3"
              style={{ color: "var(--text-muted)" }}
            >
              Menu ({userRoleClean || "EMPLOYE"})
            </div>

            <nav className="flex flex-col space-y-1">
              {visibleNavItems.map((item) => {
                const isActive = pathname === item.href;

                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                      isActive ? "shadow" : ""
                    }`}
                    style={
                      isActive
                        ? {
                            background:
                              "rgba(59, 130, 246, 0.9)",
                            color: "#ffffff",
                          }
                        : {
                            color: "var(--sidebar-text)",
                          }
                    }
                  >
                    <span className="text-base">
                      {item.icon}
                    </span>

                    <span>{item.label}</span>
                  </Link>
                );
              })}
            </nav>
          </aside>

          {/* MAIN */}
          <main
            className="flex-1 p-6 md:p-8 overflow-y-auto min-h-[calc(100vh-4rem)]"
            style={{ background: "var(--page-bg)" }}
          >
            {children}
          </main>
        </div>
        </div>
      </ToastProvider>
    </div>
  );
}