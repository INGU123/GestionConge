"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import {
  BookOpen,
  Search,
  LogIn,
  LayoutDashboard,
  ClipboardList,
  CalendarDays,
  History,
  Users,
  CheckCircle2,
  Bell,
  KeyRound,
  ShieldCheck,
  CircleHelp,
  ChevronDown,
  ArrowRight,
  Info,
} from "lucide-react";

const sections = [
  {
    id: "connexion",
    title: "1. Connexion et accès",
    description: "Accéder à votre espace GestionConge.",
    icon: LogIn,
    roles: "Tous les rôles",
    steps: [
      "Ouvrez l’adresse de GestionConge communiquée par votre organisation.",
      "Saisissez votre matricule dans le champ « Matricule ».",
      "Entrez votre mot de passe, puis cliquez sur « Se connecter ».",
      "Après connexion, vous arrivez sur le tableau de bord correspondant à votre rôle.",
    ],
    notes: [
      "L’écran de connexion utilise un matricule, et non une adresse e-mail, comme identifiant.",
      "Si vos identifiants sont refusés, vérifiez leur saisie. Si le problème persiste, contactez l’administrateur.",
    ],
  },
  {
    id: "tableau-de-bord",
    title: "2. Tableau de bord et navigation",
    description: "Comprendre le menu et retrouver les fonctionnalités.",
    icon: LayoutDashboard,
    roles: "Tous les rôles",
    steps: [
      "Utilisez le menu latéral pour ouvrir une rubrique.",
      "« Accueil » ramène au tableau de bord.",
      "« Mes Demandes » permet aux profils concernés d’accéder à la gestion de leurs demandes.",
      "« Mes Soldes » ou « Soldes & Quotas » ouvre la consultation des soldes.",
      "« Historique » permet de consulter les mouvements et les demandes accessibles à votre profil.",
    ],
    notes: [
      "Le menu varie selon les droits du compte. Une rubrique absente peut être réservée à un autre rôle.",
      "Le bouton de mode jour/nuit permet de modifier l’apparence du tableau de bord.",
    ],
  },
  {
    id: "demandes",
    title: "3. Créer et suivre une demande de congé",
    description: "Saisir les informations puis suivre la décision.",
    icon: ClipboardList,
    roles: "Collaborateur",
    href: "/dashboard/demande",
    action: "Ouvrir Mes Demandes",
    steps: [
      "Ouvrez « Mes Demandes » dans le menu.",
      "Dans le formulaire, choisissez un type de congé parmi les types proposés.",
      "Renseignez les dates de début et de fin.",
      "Complétez le motif ou le commentaire si nécessaire.",
      "Vérifiez les jours calendaires et les jours ouvrés affichés, puis relisez les informations saisies.",
      "Utilisez le bouton de soumission proposé dans le formulaire et vérifiez que la demande apparaît dans la liste.",
    ],
    notes: [
      "La demande envoyée n’est pas automatiquement approuvée. Consultez son statut pour connaître la décision.",
      "Les types disponibles et les champs obligatoires sont ceux présentés par le formulaire.",
      "Si une erreur apparaît, corrigez les champs signalés avant de réessayer.",
    ],
  },
  {
    id: "soldes",
    title: "4. Consulter les soldes et quotas",
    description: "Lire les informations de congés disponibles.",
    icon: CalendarDays,
    roles: "Tous les rôles selon les droits",
    href: "/dashboard/solde",
    action: "Ouvrir les soldes",
    steps: [
      "Ouvrez « Mes Soldes » ou « Soldes & Quotas ».",
      "Repérez le collaborateur ou le type de congé concerné selon les données affichées.",
      "Lisez les valeurs et les intitulés de colonnes présents à l’écran.",
      "Si le solde semble incohérent, consultez l’historique des mouvements lorsque cette information est disponible.",
    ],
    notes: [
      "Le calcul du solde et son moment de mise à jour dépendent des règles configurées dans l’application.",
      "Ne déduisez pas qu’une demande en attente a déjà été décomptée : vérifiez les valeurs affichées et les règles internes.",
    ],
  },
  {
    id: "historique",
    title: "5. Historique et statuts",
    description: "Retrouver une demande et comprendre son état.",
    icon: History,
    roles: "Selon les droits du compte",
    href: "/dashboard/historique",
    action: "Ouvrir l’historique",
    steps: [
      "Ouvrez « Historique » depuis le menu latéral.",
      "Repérez la demande ou le mouvement recherché dans la liste.",
      "Consultez les informations disponibles, notamment le type de congé, les dates et le statut.",
      "Si les filtres ou la recherche sont proposés à l’écran, utilisez-les pour retrouver plus vite une entrée.",
    ],
    notes: [
      "« En attente » signifie qu’une décision ou une action est attendue.",
      "« Validée » ou « Refusée » indique le résultat de la décision affichée par l’application.",
      "Si une demande est refusée, consultez le motif lorsqu’il est fourni. Contactez le responsable si vous avez besoin d’une précision.",
    ],
  },
  {
    id: "collaborateurs",
    title: "6. Gestion des collaborateurs",
    description: "Administrer les comptes et les informations autorisées.",
    icon: Users,
    roles: "Administrateur (ADMIN)",
    href: "/dashboard/utilisateurs",
    action: "Ouvrir les collaborateurs",
    steps: [
      "Ouvrez « Gestion Collaborateurs ».",
      "Utilisez la recherche pour retrouver un collaborateur par matricule, nom ou rôle.",
      "Pour modifier une fiche, utilisez l’action de modification de la ligne concernée.",
      "Pour supprimer un compte, vérifiez soigneusement la personne concernée avant de confirmer l’action.",
      "Lors de la création, renseignez les champs obligatoires et attribuez uniquement un rôle autorisé.",
    ],
    notes: [
      "La gestion des collaborateurs est réservée à l’administrateur dans l’interface actuelle.",
      "La suppression d’un compte peut affecter l’accès de la personne à l’application. Vérifiez avant de confirmer.",
      "Le rôle ADMIN ne doit être attribué que selon les règles de sécurité de l’organisation.",
    ],
  },
  {
    id: "validation",
    title: "7. Traiter les demandes",
    description: "Examiner les demandes et enregistrer une décision.",
    icon: CheckCircle2,
    roles: "Manager et administrateur selon les autorisations",
    href: "/dashboard/validation",
    action: "Ouvrir la validation",
    steps: [
      "Ouvrez « Validation des congés » si cette rubrique est disponible dans votre menu.",
      "Consultez les informations de la demande : collaborateur, dates, motif et statut.",
      "Pour refuser une demande, utilisez l’action correspondante et renseignez le motif demandé par le formulaire.",
      "Pour approuver une demande, utilisez l’action de validation disponible.",
      "Après l’action, vérifiez le statut mis à jour dans la liste.",
    ],
    notes: [
      "N’effectuez une décision qu’après avoir vérifié les informations de la demande.",
      "Si l’accès est restreint, votre compte ne dispose peut-être pas des droits nécessaires. Contactez l’administrateur.",
      "Les effets sur le solde suivent les règles de gestion configurées dans l’application.",
    ],
  },
  {
    id: "notifications",
    title: "8. Notifications",
    description: "Lire les alertes affichées dans la barre supérieure.",
    icon: Bell,
    roles: "Tous les rôles",
    steps: [
      "Repérez la cloche de notification dans la barre supérieure.",
      "Le compteur indique les notifications non lues disponibles.",
      "Cliquez sur la cloche pour ouvrir la liste.",
      "Utilisez le filtre « Toutes » ou « Non lues » pour changer les notifications affichées.",
      "Utilisez l’action prévue pour marquer une notification comme lue.",
    ],
    notes: [
      "Les notifications affichées dans l’application sont distinctes des e-mails.",
      "La liste déroulante présente un nombre limité de notifications à la fois.",
    ],
  },
  {
    id: "mot-de-passe",
    title: "9. Mot de passe oublié",
    description: "Demander la récupération de votre accès.",
    icon: KeyRound,
    roles: "Tous les rôles",
    href: "/Components/mdpForget",
    action: "Ouvrir la récupération",
    steps: [
      "Sur l’écran de connexion, cliquez sur « Mot de passe oublié ? ».",
      "Suivez les instructions affichées dans le formulaire de récupération.",
      "Consultez la boîte de réception de l’adresse demandée ainsi que les courriers indésirables.",
      "Si vous recevez un lien de réinitialisation, ouvrez-le et choisissez un nouveau mot de passe en respectant les exigences affichées.",
      "Retournez ensuite à la page de connexion et connectez-vous avec votre nouveau mot de passe.",
    ],
    notes: [
      "Si aucun e-mail n’arrive, vérifiez les informations saisies et les courriers indésirables, puis contactez l’administrateur si le problème persiste.",
      "Ne communiquez jamais votre mot de passe à une autre personne.",
    ],
  },
  {
    id: "securite",
    title: "10. Sécurité et bonnes pratiques",
    description: "Protéger votre compte et éviter les erreurs.",
    icon: ShieldCheck,
    roles: "Tous les rôles",
    steps: [
      "Gardez vos identifiants confidentiels et choisissez un mot de passe difficile à deviner.",
      "Vérifiez les dates et les informations avant de soumettre une demande.",
      "Déconnectez-vous après utilisation sur un ordinateur partagé.",
      "N’effectuez que les opérations autorisées par votre rôle.",
      "Signalez à l’administrateur toute anomalie ou action inattendue.",
    ],
    notes: [
      "Avant de répéter une action après une erreur, vérifiez d’abord si l’opération a été enregistrée.",
    ],
  },
];

export default function GuideUtilisateurPage() {
  const [search, setSearch] = useState("");
  const [openSections, setOpenSections] = useState(["connexion"]);

  const filteredSections = useMemo(() => {
    const term = search.trim().toLocaleLowerCase("fr");
    if (!term) return sections;
    return sections.filter((section) =>
      [
        section.title,
        section.description,
        section.roles,
        ...section.steps,
        ...section.notes,
      ]
        .join(" ")
        .toLocaleLowerCase("fr")
        .includes(term),
    );
  }, [search]);

  const toggleSection = (id) => {
    setOpenSections((current) =>
      current.includes(id)
        ? current.filter((item) => item !== id)
        : [...current, id],
    );
  };

  return (
    <div className="mx-auto max-w-5xl space-y-6 pb-10">
      <section className="overflow-hidden rounded-2xl border border-[var(--primary-border)] bg-[var(--header-bg)] text-[var(--header-text)] shadow-sm">
        <div className="flex flex-col gap-5 p-6 sm:p-8 md:flex-row md:items-center md:justify-between">
          <div className="max-w-2xl">
            <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-current/20 px-3 py-1 text-xs font-semibold">
              <BookOpen className="h-4 w-4" />
              Centre d’aide
            </div>
            <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">
              Guide d’utilisation GestionConge
            </h1>
            <p className="mt-3 max-w-xl text-sm leading-6 opacity-80">
              Retrouvez les étapes essentielles pour utiliser vos demandes,
              consulter les soldes, suivre les décisions et comprendre les
              outils disponibles selon votre rôle.
            </p>
          </div>
          <div className="flex h-20 w-20 shrink-0 items-center justify-center self-start rounded-2xl bg-blue-500/15 md:self-center">
            <BookOpen className="h-10 w-10" />
          </div>
        </div>
      </section>

      <section className="rounded-2xl border border-[var(--primary-border)] bg-[var(--card-bg)] p-4 shadow-sm sm:p-5">
        <label htmlFor="guide-search" className="mb-2 block text-sm font-semibold" style={{ color: "var(--text-main)" }}>
          Rechercher dans le guide
        </label>
        <div className="relative">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2" style={{ color: "var(--text-muted)" }} />
          <input
            id="guide-search"
            type="search"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Ex. : mot de passe, solde, validation…"
            className="w-full rounded-xl border border-[var(--primary-border)] bg-[var(--page-bg)] py-3 pl-10 pr-4 text-sm outline-none transition focus:ring-2 focus:ring-blue-500"
            style={{ color: "var(--text-main)" }}
          />
        </div>
        <p className="mt-2 text-xs" style={{ color: "var(--text-muted)" }}>
          {filteredSections.length} rubrique{filteredSections.length > 1 ? "s" : ""} affichée{filteredSections.length > 1 ? "s" : ""}.
        </p>
      </section>

      <div className="grid gap-3 sm:grid-cols-2">
        {[
          { label: "Collaborateur", detail: "Demandes et consultation des soldes", icon: ClipboardList },
          { label: "Manager", detail: "Examen des demandes accessibles", icon: CheckCircle2 },
          { label: "Administrateur", detail: "Gestion des collaborateurs et quotas", icon: Users },
          { label: "Tous les utilisateurs", detail: "Connexion, notifications et sécurité", icon: ShieldCheck },
        ].map((role) => {
          const Icon = role.icon;
          return (
            <div key={role.label} className="flex items-start gap-3 rounded-xl border border-[var(--primary-border)] bg-[var(--card-bg)] p-4">
              <div className="rounded-lg bg-blue-500/10 p-2 text-blue-600">
                <Icon className="h-5 w-5" />
              </div>
              <div>
                <p className="text-sm font-semibold" style={{ color: "var(--text-main)" }}>{role.label}</p>
                <p className="mt-1 text-xs leading-5" style={{ color: "var(--text-muted)" }}>{role.detail}</p>
              </div>
            </div>
          );
        })}
      </div>

      <section className="space-y-3">
        {filteredSections.map((section) => {
          const Icon = section.icon;
          const isOpen = openSections.includes(section.id) || Boolean(search.trim());
          return (
            <article key={section.id} id={section.id} className="overflow-hidden rounded-2xl border border-[var(--primary-border)] bg-[var(--card-bg)] shadow-sm">
              <button
                type="button"
                onClick={() => toggleSection(section.id)}
                aria-expanded={isOpen}
                className="flex w-full items-center gap-3 p-4 text-left sm:p-5"
              >
                <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-blue-500/10 text-blue-600">
                  <Icon className="h-5 w-5" />
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block text-sm font-bold sm:text-base" style={{ color: "var(--text-main)" }}>{section.title}</span>
                  <span className="mt-1 block text-xs leading-5" style={{ color: "var(--text-muted)" }}>{section.description}</span>
                </span>
                <ChevronDown className={`h-5 w-5 shrink-0 transition-transform ${isOpen ? "rotate-180" : ""}`} style={{ color: "var(--text-muted)" }} />
              </button>

              {isOpen && (
                <div className="border-t border-[var(--primary-border)] px-4 pb-5 pt-4 sm:px-5">
                  <div className="mb-4 inline-flex items-center gap-2 rounded-full bg-[var(--page-bg)] px-3 py-1.5 text-xs" style={{ color: "var(--text-muted)" }}>
                    <ShieldCheck className="h-3.5 w-3.5" />
                    Accès : {section.roles}
                  </div>
                  <h2 className="mb-3 text-sm font-bold" style={{ color: "var(--text-main)" }}>Étapes à suivre</h2>
                  <ol className="space-y-3">
                    {section.steps.map((step, index) => (
                      <li key={step} className="flex items-start gap-3 text-sm leading-6" style={{ color: "var(--text-main)" }}>
                        <span className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-blue-600 text-xs font-bold text-white">{index + 1}</span>
                        <span>{step}</span>
                      </li>
                    ))}
                  </ol>
                  {section.notes?.length > 0 && (
                    <div className="mt-5 rounded-xl border border-blue-500/20 bg-blue-500/5 p-4">
                      <div className="mb-2 flex items-center gap-2 text-sm font-semibold" style={{ color: "var(--text-main)" }}>
                        <Info className="h-4 w-4 text-blue-600" />
                        À retenir
                      </div>
                      <ul className="list-disc space-y-2 pl-5 text-sm leading-6" style={{ color: "var(--text-muted)" }}>
                        {section.notes.map((note) => <li key={note}>{note}</li>)}
                      </ul>
                    </div>
                  )}
                  {section.href && section.action && (
                    <Link href={section.href} className="mt-4 inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-blue-700">
                      {section.action}
                      <ArrowRight className="h-4 w-4" />
                    </Link>
                  )}
                </div>
              )}
            </article>
          );
        })}
        {filteredSections.length === 0 && (
          <div className="rounded-2xl border border-dashed border-[var(--primary-border)] p-8 text-center">
            <CircleHelp className="mx-auto h-8 w-8" style={{ color: "var(--text-muted)" }} />
            <p className="mt-3 font-semibold" style={{ color: "var(--text-main)" }}>Aucune rubrique trouvée</p>
            <p className="mt-1 text-sm" style={{ color: "var(--text-muted)" }}>Essayez un autre mot-clé, ou effacez votre recherche.</p>
            <button type="button" onClick={() => setSearch("")} className="mt-4 rounded-lg border border-[var(--primary-border)] px-4 py-2 text-sm font-semibold" style={{ color: "var(--text-main)" }}>
              Effacer la recherche
            </button>
          </div>
        )}
      </section>

      <footer className="rounded-xl bg-[var(--page-bg)] p-4 text-xs leading-5" style={{ color: "var(--text-muted)" }}>
        <p className="font-semibold">Besoin d’aide supplémentaire ?</p>
        <p className="mt-1">Si une rubrique est absente de votre menu ou si une action échoue, contactez l’administrateur de GestionConge. Les fonctionnalités disponibles dépendent de votre rôle et des autorisations attribuées à votre compte.</p>
      </footer>
    </div>
  );
}
