"use client";

import { useEffect, useMemo, useState } from "react";
import { getCurrentUser } from "@/lib/apiClient";
import Link from "next/link";
import Image from "next/image";
import {
  FiBookOpen as BookOpen,
  FiSearch as Search,
  FiLogIn as LogIn,
  FiGrid as LayoutDashboard,
  FiClipboard as ClipboardList,
  FiCalendar as CalendarDays,
  FiClock as History,
  FiUsers as Users,
  FiCheckCircle as CheckCircle2,
  FiBell as Bell,
  FiKey as KeyRound,
  FiShield as ShieldCheck,
  FiHelpCircle as CircleHelp,
  FiChevronDown as ChevronDown,
  FiArrowRight as ArrowRight,
  FiInfo as Info,
} from "react-icons/fi";

const sections = [
  {
    id: "demandes",
    title: "Préparer et suivre un dossier de congé",
    forRoles: ["ASSISTANT_DIRECTION"],
    detail: "L'assistant constitue le dossier au nom du collaborateur. Il ne décide pas de l'octroi du congé : il vérifie les pièces et la concordance avant transmission.",
    description: "Créer le dossier maître et suivre son acheminement.",
    icon: ClipboardList,
    roles: "Assistant de direction",
    href: "/dashboard/circuit-conge",
    action: "Ouvrir le circuit des congés",
    steps: [
      "Sélectionnez le collaborateur, le type de congé, les dates et le motif communiqué.",
      "Vérifiez les informations affichées avant de créer le dossier.",
      "Attendez le relevé officiel de l'Administration du personnel.",
      "Comparez le relevé, l'identité, le type, les dates, le motif et le reliquat avec le dossier initial.",
      "En cas de discordance, décrivez précisément l'écart et retournez le dossier à l'Administration du personnel.",
      "Si tout concorde et que le solde est suffisant, transmettez le dossier au chef de département.",
      "Suivez la décision finale et téléchargez le document officiel après décision de la Direction."
    ],
    notes: [
      "Le nombre de jours ouvrés est calculé par le serveur. Ne le corrigez pas manuellement.",
      "L'assistant coordonne le dossier mais ne valide ni ne refuse le congé."
    ]
  },
  {
    id: "soldes",
    title: "Consulter mes soldes",
    forRoles: ["EMPLOYE"],
    detail: "Cette rubrique permet au collaborateur de consulter ses droits. Elle ne permet pas de déposer une demande ni de modifier le solde.",
    description: "Lire les droits acquis, consommés et restants.",
    icon: CalendarDays,
    roles: "Collaborateur",
    href: "/dashboard/solde",
    action: "Consulter mes soldes",
    steps: [
      "Ouvrez « Mes Soldes » depuis le menu.",
      "Vérifiez le type de congé et l'exercice affiché.",
      "Comparez les jours acquis, consommés et restants.",
      "En cas d'écart, contactez l'Administration du personnel."
    ],
    notes: [
      "Le solde n'est débité qu'après la décision favorable de la Direction.",
      "Le collaborateur ne peut pas modifier ses propres soldes."
    ]
  },
  {
    id: "types-conge",
    title: "Gestion des types de congé",
    forRoles: ["ADMIN"],
    detail: "Rubrique d'administration de la configuration des types de congés.",
    description: "Consulter les catégories de congés configurées.",
    icon: CalendarDays,
    roles: "Administrateur",
    images: [
      { src: "/Aides/typeConge.png", alt: "Écran de gestion des types de congés", caption: "Écran des types de congés configurés dans GestionConge.", help: "Vérifiez l'intitulé du type avant toute modification." }
    ],
    steps: [
      "Ouvrez la rubrique des types de congés si elle est disponible dans votre menu.",
      "Consultez les intitulés et les informations affichés pour chaque type.",
      "Les utilisateurs choisissent uniquement un type proposé par le formulaire."
    ],
    notes: ["Les types disponibles dépendent de la configuration de l'application."]
  },
  {
    id: "historique",
    title: "Historique et traçabilité",
    forRoles: ["EMPLOYE", "MANAGER", "ADMIN", "ASSISTANT_DIRECTION", "ADMINISTRATION_PERSONNEL", "CHEF_DEPARTEMENT", "DIRECTEUR"],
    detail: "L'historique permet de contrôler les mouvements et les opérations enregistrées. Chaque acteur doit vérifier le dossier concerné avant toute action.",
    description: "Retrouver les mouvements et contrôler leur traçabilité.",
    icon: History,
    roles: "Selon les droits du compte",
    href: "/dashboard/historique",
    action: "Ouvrir l'historique",
    steps: [
      "Ouvrez « Historique » depuis le menu.",
      "Repérez l'opération par sa date, son collaborateur et son type.",
      "Vérifiez le sens du mouvement, la quantité et le commentaire.",
      "Signalez toute anomalie à l'Administration du personnel."
    ],
    notes: [
      "Un débit de congé ne doit apparaître qu'après une décision favorable.",
      "Une annulation doit être accompagnée de la restitution correspondante des jours selon les règles applicables."
    ]
  },
  {
    id: "collaborateurs",
    title: "Gestion des collaborateurs",
    forRoles: ["ADMIN"],
    detail: "Avant de créer ou modifier un compte, vérifiez le matricule, l'identité, le service, le département et le rôle.",
    description: "Administrer les comptes et les informations autorisées.",
    icon: Users,
    roles: "Administrateur",
    href: "/dashboard/utilisateurs",
    action: "Ouvrir les collaborateurs",
    steps: [
      "Ouvrez « Gestion Collaborateurs ».",
      "Retrouvez la personne à l'aide de son matricule.",
      "Vérifiez le service, le département et le responsable hiérarchique.",
      "Attribuez uniquement le rôle correspondant aux responsabilités réelles."
    ],
    notes: [
      "Le rôle détermine les interfaces et les opérations autorisées.",
      "Ne supprimez pas un compte sans vérifier les conséquences sur l'historique."
    ]
  },
  {
    id: "assistant-rh",
    title: "Vérifier le reliquat",
    forRoles: ["ADMINISTRATION_PERSONNEL"],
    detail: "L'Administration du personnel est responsable de l'exactitude du solde communiqué pour le collaborateur et le type de congé.",
    description: "Contrôler le solde avant de retourner le relevé à l'assistant.",
    icon: CheckCircle2,
    roles: "Administration du personnel",
    href: "/dashboard/reliquats",
    action: "Ouvrir les reliquats",
    steps: [
      "Ouvrez le dossier et confirmez l'identité du collaborateur.",
      "Vérifiez le type de congé et la période demandée.",
      "Contrôlez les soldes de tous les exercices concernés.",
      "Saisissez le solde disponible exact calculé à partir des données RH.",
      "Ajoutez une observation utile puis retournez le relevé à l'assistant.",
      "Si l'assistant signale une discordance, reprenez le contrôle avant un nouvel envoi."
    ],
    notes: [
      "Ne saisissez pas le nombre de jours demandés à la place du solde disponible.",
      "Le serveur refuse un solde saisi qui ne correspond pas au solde calculé."
    ]
  },
  {
    id: "chef",
    title: "Avis du chef de département",
    forRoles: ["CHEF_DEPARTEMENT"],
    detail: "Le chef évalue les nécessités du service et transmet un avis à la Direction. Il ne prend pas la décision finale.",
    description: "Examiner la demande et joindre un avis hiérarchique.",
    icon: Users,
    roles: "Chef de département",
    href: "/dashboard/chef-departement",
    action: "Ouvrir les dossiers à examiner",
    steps: [
      "Vérifiez l'identité, le type, les dates, le nombre de jours et le reliquat.",
      "Évaluez l'impact de l'absence sur l'organisation du département.",
      "Choisissez un avis favorable ou défavorable.",
      "Ajoutez une observation obligatoire si l'avis est défavorable.",
      "Transmettez le dossier à la Direction."
    ],
    notes: ["La Direction reste seule responsable de la décision finale."]
  },
  {
    id: "direction",
    title: "Décision finale de la Direction",
    forRoles: ["DIRECTEUR"],
    detail: "La Direction prend la décision finale après examen du dossier et de l'avis du chef de département.",
    description: "Accorder ou refuser le congé en motivant tout refus.",
    icon: ShieldCheck,
    roles: "Directeur",
    href: "/dashboard/directeur",
    action: "Ouvrir les décisions",
    steps: [
      "Vérifiez le dossier complet, le relevé RH et l'avis hiérarchique.",
      "Vérifiez la cohérence de la période et du nombre de jours.",
      "Accordez le congé ou refusez-le.",
      "En cas de refus, renseignez obligatoirement un motif clair.",
      "La décision est notifiée au collaborateur et à l'assistant."
    ],
    notes: [
      "Le solde est débité une seule fois après validation.",
      "Une décision définitive ne doit pas être modifiée sans procédure de correction traçable."
    ]
  },
  {
    id: "decisions",
    title: "Décisions reçues",
    forRoles: ["EMPLOYE"],
    detail: "Après la décision de la Direction, le collaborateur reçoit une notification et peut consulter le résultat et le document officiel.",
    description: "Consulter l'accord ou le refus prononcé par la Direction.",
    icon: Bell,
    roles: "Collaborateur",
    href: "/dashboard/decisions",
    action: "Voir mes décisions",
    steps: [
      "Ouvrez « Décisions reçues » dans le menu.",
      "Consultez la période, le nombre de jours et le statut final.",
      "En cas de refus, lisez le motif communiqué.",
      "Téléchargez le PDF officiel de la décision."
    ],
    notes: [
      "Les demandes en cours d'instruction ne sont pas affichées comme des décisions définitives.",
      "Le collaborateur ne peut pas modifier la décision."
    ]
  },
  {
    id: "notifications",
    title: "Notifications",
    forRoles: ["EMPLOYE", "MANAGER", "ADMIN"],
    detail: "Ouvrez la cloche pour lire les événements, puis utilisez les filtres proposés. Une notification affichée dans l’application n’est pas nécessairement un e-mail envoyé.",
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
    title: "Mot de passe oublié",
    forRoles: ["EMPLOYE", "MANAGER", "ADMIN"],
    detail: "Démarrez depuis l’écran de connexion et suivez exactement le formulaire actuellement affiché. Si la demande doit être traitée par l’administrateur, surveillez son retour. Ne transmettez jamais votre mot de passe existant.",
    description: "Demander la récupération de votre accès.",
    icon: KeyRound,
    roles: "Tous les rôles",
    href: "/Components/mdpForget",
    action: "Ouvrir la récupération",
    images: [
      { src: "/Aides/mdpForget.png", alt: "Accès à la récupération du mot de passe", caption: "Écran de récupération du mot de passe.", help: "Ouvrez cette fonction depuis la page de connexion et suivez les champs réellement affichés. Relisez le matricule ou les informations demandées avant d’envoyer la requête ; si la procédure ne correspond pas à cet exemple, suivez l’interface actuelle." },
      { src: "/Aides/demandeReinitialisationMdp.png", alt: "Demande de réinitialisation du mot de passe", caption: "Formulaire de demande de réinitialisation.", help: "Remplissez les informations obligatoires indiquées dans le formulaire puis envoyez une seule demande. Notez que l’envoi d’une demande ne signifie pas que le mot de passe a déjà été changé." },
      { src: "/Aides/reinitialisationByAdmin.png", alt: "Réinitialisation du mot de passe par l’administrateur", caption: "Étape de réinitialisation prise en charge par l’administrateur.", help: "Cette capture illustre le traitement côté administrateur. Vérifiez soigneusement le matricule du compte concerné avant d’agir, puis informez l’utilisateur selon la procédure interne. Ne partagez jamais les identifiants dans un canal non sécurisé." },
    ],
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
    title: "Sécurité et bonnes pratiques",
    forRoles: ["EMPLOYE", "MANAGER", "ADMIN"],
    detail: "Protégez votre session et les informations RH. Vérifiez les données avant une opération, ne partagez pas vos identifiants et signalez les anomalies au responsable désigné.",
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
  const [openSections, setOpenSections] = useState([]);
  const [currentUser, setCurrentUser] = useState(null);
  const [userLoading, setUserLoading] = useState(true);

  useEffect(() => {
    let active = true;

    try {
      const user = getCurrentUser();
      if (active) setCurrentUser(user || null);
    } catch (error) {
      console.error("Impossible de récupérer le rôle pour le guide :", error);
      if (active) setCurrentUser(null);
    } finally {
      if (active) setUserLoading(false);
    }

    return () => { active = false; };
  }, []);

  const role = (currentUser?.role || "").toString().trim().toUpperCase().replace(/^ROLE_/, "");
  const isAdmin = role === "ADMIN";
  const isManager = role === "MANAGER";
  const roleLabel = isAdmin ? "Administrateur" : isManager ? "Manager" : role === "EMPLOYE" || role === "EMPLOYÉ" ? "Collaborateur" : "Utilisateur";
  const visibleSections = useMemo(
    () => sections.filter((section) => !section.forRoles || section.forRoles.includes(role)),
    [role],
  );

  const filteredSections = useMemo(() => {
    const term = search.trim().toLocaleLowerCase("fr");
    if (!term) return visibleSections;
    return visibleSections.filter((section) =>
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
  }, [search, visibleSections]);

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
              Retrouvez une aide adaptée à votre profil : {roleLabel}. Les rubriques réservées aux autres rôles sont masquées pour vous montrer les procédures utiles à votre travail.
            </p>
          </div>
          <div className="flex h-20 w-20 shrink-0 items-center justify-center self-start rounded-2xl bg-blue-500/15 md:self-center">
            <BookOpen className="h-10 w-10" />
          </div>
        </div>
      </section>

      <section className="rounded-2xl border border-[var(--primary-border)] bg-[var(--panel)] p-4 shadow-sm sm:p-5">
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

      <div className="mb-1 rounded-xl border border-[var(--primary-border)] bg-[var(--panel)] p-4 text-sm" style={{ color: "var(--text-muted)" }}>
        <span className="font-semibold" style={{ color: "var(--text-main)" }}>Profil actuel : {roleLabel}</span>
        <p className="mt-1">Le guide affiche uniquement les rubriques prévues pour ce rôle. Les opérations restent soumises aux autorisations du compte.</p>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        {[
          { label: "Collaborateur", detail: "Déposer et suivre ses demandes", icon: ClipboardList, visible: !isAdmin },
          { label: "Manager", detail: "Examiner les demandes accessibles", icon: CheckCircle2, visible: isManager },
          { label: "Administrateur", detail: "Gérer les comptes et les paramètres RH", icon: Users, visible: isAdmin },
          { label: "Tous les utilisateurs", detail: "Notifications et sécurité", icon: ShieldCheck, visible: true },
        ].filter((item) => item.visible).map((role) => {
          const Icon = role.icon;
          return (
            <div key={role.label} className="flex items-start gap-3 rounded-xl border border-[var(--primary-border)] bg-[var(--panel)] p-4">
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

      {userLoading ? (
        <section className="rounded-2xl border border-[var(--primary-border)] bg-[var(--panel)] p-6 text-sm" style={{ color: "var(--text-muted)" }}>
          Chargement des rubriques correspondant à votre rôle…
        </section>
      ) : (
      <section className="space-y-3">
        {filteredSections.map((section) => {
          const Icon = section.icon;
          const isOpen = openSections.includes(section.id);
          return (
            <article key={section.id} id={section.id} className="overflow-hidden rounded-2xl border border-[var(--primary-border)] bg-[var(--panel)] shadow-sm">
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
                  {section.detail && (
                    <div className="mb-4 rounded-xl border border-[var(--primary-border)] bg-[var(--page-bg)] p-4 text-sm leading-6" style={{ color: "var(--text-main)" }}>
                      <p className="mb-1 font-semibold">Objectif de cette rubrique</p>
                      {section.detail}
                    </div>
                  )}
                  <h2 className="mb-3 text-sm font-bold" style={{ color: "var(--text-main)" }}>Étapes à suivre</h2>
                  <ol className="space-y-3">
                    {section.steps.map((step, index) => (
                      <li key={step} className="flex items-start gap-3 text-sm leading-6" style={{ color: "var(--text-main)" }}>
                        <span className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-blue-600 text-xs font-bold text-white">{index + 1}</span>
                        <span>{step}</span>
                      </li>
                    ))}
                  </ol>
                  {section.images?.length > 0 && (
                    <div className="mt-5 grid gap-4">
                      {section.images.map((image) => (
                        <figure key={image.src} className="overflow-hidden rounded-xl border border-[var(--primary-border)] bg-[var(--page-bg)] p-3 sm:p-4">
                          <div className="overflow-hidden rounded-lg border border-[var(--primary-border)] bg-[var(--panel)]">
                            <Image
                              src={image.src}
                              alt={image.alt}
                              width={1600}
                              height={1000}
                              sizes="(max-width: 768px) 100vw, 900px"
                              className="h-auto max-h-[620px] w-full object-contain"
                            />
                          </div>
                          <figcaption className="mt-2 text-sm font-semibold leading-5" style={{ color: "var(--text-main)" }}>{image.caption}</figcaption>
                          {image.help && <p className="mt-2 text-sm leading-6" style={{ color: "var(--text-muted)" }}>{image.help}</p>}
                        </figure>
                      ))}
                    </div>
                  )}
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
      )}

      <footer className="rounded-xl bg-[var(--page-bg)] p-4 text-xs leading-5" style={{ color: "var(--text-muted)" }}>
        <p className="font-semibold">Besoin d’aide supplémentaire ?</p>
        <p className="mt-1">Si une rubrique est absente de votre menu ou si une action échoue, contactez l’administrateur de GestionConge. Les fonctionnalités disponibles dépendent de votre rôle et des autorisations attribuées à votre compte.</p>
      </footer>
    </div>
  );
}
