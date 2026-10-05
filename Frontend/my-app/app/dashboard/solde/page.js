"use client";

import { useEffect, useState } from "react";
import {
  getSoldesUtilisateur,
  initialiserSoldesAnnuels,
  ajustSolde,
} from "../../api/soldeConge/soldeConge";
import { getAllTypeConge } from "../../api/typeConge/typeConge";
import { getCurrentUser, authFetch } from "@/lib/apiClient";
import {
  Scale,
  CalendarDays,
  Clock,
  RotateCcw,
  Sliders,
  UserCheck,
  CheckCircle2,
  PieChart,
  FileCheck,
  AlertCircle,
} from "lucide-react";

export default function SoldeConge() {
  /*
   * IMPORTANT :
   * getCurrentUser() ne doit pas être appelé directement à chaque rendu
   * avant d'être utilisé comme dépendance d'un useEffect.
   *
   * L'utilisateur est donc initialisé UNE SEULE FOIS.
   */
  const [user] = useState(() => getCurrentUser());

  const [soldes, setSoldes] = useState([]);
  const [typesConge, setTypesConge] = useState([]);
  const [allUsers, setAllUsers] = useState([]);

  const [selectedUserId, setSelectedUserId] = useState(
    () => (user?.id ? Number(user.id) : null)
  );

  const [loading, setLoading] = useState(true);
  const [initLoading, setInitLoading] = useState(false);

  const [msg, setMsg] = useState({
    type: "",
    text: "",
  });

  // Modal d'ajustement - Admin uniquement
  const [modalAjust, setModalAjust] = useState({
    open: false,
    soldeId: null,
    nouveauSolde: 0,
    typeLibelle: "",
  });

  /*
   * ============================================================
   * CHARGEMENT DES SOLDES ET DES TYPES DE CONGE
   * ============================================================
   *
   * Cette fonction est appelée uniquement lorsque selectedUserId
   * change.
   */
  useEffect(() => {
    const loadData = async () => {
      if (!selectedUserId) {
        setLoading(false);
        return;
      }

      try {
        setLoading(true);

        const [soldesData, typesData] = await Promise.all([
          getSoldesUtilisateur(selectedUserId).catch(() => []),
          getAllTypeConge().catch(() => []),
        ]);

        setSoldes(
          Array.isArray(soldesData) ? soldesData : []
        );

        setTypesConge(
          Array.isArray(typesData) ? typesData : []
        );
      } catch (err) {
        console.error("Erreur chargement soldes:", err);

        setSoldes([]);
        setTypesConge([]);
      } finally {
        setLoading(false);
      }
    };

    void loadData();
  }, [selectedUserId]);

  /*
   * ============================================================
   * CHARGEMENT DES UTILISATEURS POUR L'ADMIN
   * ============================================================
   *
   * Cet effet est indépendant du chargement des soldes.
   * Il ne dépend pas d'un objet recréé à chaque rendu.
   */
  useEffect(() => {
    const loadUsers = async () => {
      if (!user) {
        return;
      }

      const role = user.role?.toString().trim().toUpperCase();

      if (role !== "ADMIN") {
        return;
      }

      try {
        const response = await authFetch(
          "http://localhost:8080/utilisateur/all"
        );

        const data = response.ok
          ? await response.json()
          : [];

        setAllUsers(
          Array.isArray(data) ? data : []
        );
      } catch (err) {
        console.error(
          "Erreur lors du chargement des utilisateurs:",
          err
        );

        setAllUsers([]);
      }
    };

    void loadUsers();
  }, [user]);

  /*
   * ============================================================
   * CHANGEMENT D'UTILISATEUR
   * ============================================================
   *
   * On modifie seulement selectedUserId.
   * Le useEffect ci-dessus se charge ensuite de récupérer
   * les soldes du nouvel utilisateur.
   */
  const handleUserChange = (newUserId) => {
    const id = Number(newUserId);

    if (!Number.isFinite(id) || id <= 0) {
      return;
    }

    setSelectedUserId(id);
  };

  /*
   * ============================================================
   * INITIALISATION DES SOLDES
   * ============================================================
   */
  const handleInit = async () => {
    const targetId = selectedUserId || user?.id;

    if (!targetId) {
      return;
    }

    setInitLoading(true);
    setMsg({
      type: "",
      text: "",
    });

    try {
      const annee = new Date().getFullYear();

      await initialiserSoldesAnnuels(
        targetId,
        annee
      );

      setMsg({
        type: "success",
        text: `Soldes pour l'année ${annee} initialisés avec succès !`,
      });

      /*
       * Le changement de selectedUserId n'est pas nécessaire ici.
       * On recharge simplement les données de l'utilisateur actuel.
       */
      const [soldesData, typesData] = await Promise.all([
        getSoldesUtilisateur(targetId).catch(() => []),
        getAllTypeConge().catch(() => []),
      ]);

      setSoldes(
        Array.isArray(soldesData)
          ? soldesData
          : []
      );

      setTypesConge(
        Array.isArray(typesData)
          ? typesData
          : []
      );
    } catch (err) {
      console.error(
        "Erreur initialisation des soldes:",
        err
      );

      setMsg({
        type: "error",
        text: `Erreur initialisation : ${
          err?.message || "Erreur inconnue"
        }`,
      });
    } finally {
      setInitLoading(false);
    }
  };

  /*
   * ============================================================
   * OUVERTURE DU MODAL D'AJUSTEMENT
   * ============================================================
   */
  const handleOpenAjust = (solde, typeLabel) => {
    setModalAjust({
      open: true,
      soldeId: solde.id,
      nouveauSolde: Math.round(
        Number(solde.soldeRestant) || 0
      ),
      typeLibelle: typeLabel,
    });
  };

  /*
   * ============================================================
   * CONFIRMATION DE L'AJUSTEMENT
   * ============================================================
   */
  const handleConfirmAjust = async () => {
    if (!modalAjust.soldeId) {
      return;
    }

    try {
      const nouveauNombre = Math.round(
        Number(modalAjust.nouveauSolde) || 0
      );

      await ajustSolde(
        modalAjust.soldeId,
        nouveauNombre
      );

      setMsg({
        type: "success",
        text: "Solde ajusté avec succès !",
      });

      setModalAjust({
        open: false,
        soldeId: null,
        nouveauSolde: 0,
        typeLibelle: "",
      });

      /*
       * Rechargement des données sans rappeler
       * inutilement le useEffect avec setSelectedUserId().
       */
      const targetId = selectedUserId || user?.id;

      if (targetId) {
        const [soldesData, typesData] = await Promise.all([
          getSoldesUtilisateur(targetId).catch(() => []),
          getAllTypeConge().catch(() => []),
        ]);

        setSoldes(
          Array.isArray(soldesData)
            ? soldesData
            : []
        );

        setTypesConge(
          Array.isArray(typesData)
            ? typesData
            : []
        );
      }
    } catch (err) {
      console.error(
        "Erreur d'ajustement:",
        err
      );

      setMsg({
        type: "error",
        text: `Erreur d'ajustement : ${
          err?.message || "Erreur inconnue"
        }`,
      });
    }
  };

  /*
   * ============================================================
   * INFORMATIONS TYPE DE CONGE
   * ============================================================
   */
  const getTypeInfo = (typeCongeId) => {
    const found = typesConge.find(
      (t) => t.id === typeCongeId
    );

    return {
      libelle:
        found?.libelle ||
        found?.code ||
        `Type #${typeCongeId}`,

      code: found?.code || "",
    };
  };

  /*
   * ============================================================
   * ROLE ADMIN
   * ============================================================
   */
  const isAdmin =
    user?.role?.toString().trim().toUpperCase() ===
    "ADMIN";

  /*
   * ============================================================
   * CALCUL DES TOTAUX
   * ============================================================
   */
  const totalAcquis = Math.round(
    soldes.reduce(
      (acc, s) =>
        acc +
        (Number(
          s.soldeAquis ?? s.soldeAcquis
        ) || 0),
      0
    )
  );

  const totalPris = Math.round(
    soldes.reduce(
      (acc, s) =>
        acc +
        (Number(s.soldePris) || 0),
      0
    )
  );

  const totalRestant = Math.round(
    soldes.reduce(
      (acc, s) =>
        acc +
        (Number(s.soldeRestant) || 0),
      0
    )
  );

  return (
    <div className="space-y-6">
      {/* ======================================================
          BANNER SPAT
          ====================================================== */}
      <div className="bg-slate-900 text-white rounded-2xl p-6 shadow-md border-l-4 border-blue-600 flex justify-between items-center flex-wrap gap-4">
        <div>
          <div className="text-xs font-semibold uppercase tracking-widest text-blue-400 mb-1">
            SPAT — Direction des Ressources Humaines
          </div>

          <h1 className="text-2xl font-bold tracking-tight flex items-center gap-2">
            <Scale className="w-6 h-6 text-blue-400" />

            {isAdmin
              ? "Administration des Soldes de Congés"
              : "Mes Soldes de Congés Réglementaires"}
          </h1>

          <p className="text-slate-400 text-sm mt-1">
            {isAdmin
              ? "Supervision, initialisation et réajustement des droits aux congés pour les agents SPAT."
              : "Suivi en temps réel de votre contingent annuel de droits, consommations et reliquats."}
          </p>
        </div>

        {isAdmin && (
          <button
            onClick={handleInit}
            disabled={
              initLoading ||
              !selectedUserId
            }
            className="btn btn-primary btn-sm font-semibold gap-2 shadow"
          >
            {initLoading ? (
              <span className="loading loading-spinner loading-xs"></span>
            ) : (
              <>
                <RotateCcw className="w-4 h-4" />
                Initialiser les soldes annuels
              </>
            )}
          </button>
        )}
      </div>

      {/* ======================================================
          MESSAGE
          ====================================================== */}
      {msg.text && (
        <div
          className={`alert ${
            msg.type === "success"
              ? "alert-success text-white"
              : "alert-error text-white"
          } shadow-sm rounded-xl flex items-center gap-2`}
        >
          {msg.type === "success" ? (
            <CheckCircle2 className="w-5 h-5 shrink-0" />
          ) : (
            <AlertCircle className="w-5 h-5 shrink-0" />
          )}

          <span>{msg.text}</span>
        </div>
      )}

      {/* ======================================================
          SELECTEUR COLLABORATEUR
          ====================================================== */}
      {isAdmin && allUsers.length > 0 && (
        <div className="bg-white rounded-xl p-4 shadow-sm border border-slate-200 flex items-center gap-3 flex-wrap">
          <div className="flex items-center gap-2 text-slate-700 font-bold text-xs uppercase tracking-wider">
            <UserCheck className="w-4 h-4 text-blue-600" />

            <span>Agent Sélectionné :</span>
          </div>

          <select
            value={selectedUserId || ""}
            onChange={(e) =>
              handleUserChange(e.target.value)
            }
            className="select select-bordered select-sm max-w-md font-medium text-slate-800 bg-white"
          >
            {allUsers.map((u) => (
              <option
                key={u.id}
                value={u.id}
              >
                {u.prenom} {u.nom} ({u.email}) — [
                {u.role}]
              </option>
            ))}
          </select>
        </div>
      )}

      {/* ======================================================
          KPI CARDS
          ====================================================== */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {/* ACQUIS */}
        <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200 flex items-center justify-between">
          <div>
            <p className="text-xs font-bold uppercase text-slate-500 tracking-wider">
              Total Droits Acquis
            </p>

            <p className="text-3xl font-bold text-blue-700 mt-1">
              {totalAcquis}{" "}
              <span className="text-sm text-slate-500 font-normal">
                jours
              </span>
            </p>
          </div>

          <div className="w-12 h-12 rounded-lg bg-blue-50 text-blue-700 flex items-center justify-center border border-blue-100">
            <FileCheck className="w-6 h-6" />
          </div>
        </div>

        {/* PRIS */}
        <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200 flex items-center justify-between">
          <div>
            <p className="text-xs font-bold uppercase text-slate-500 tracking-wider">
              Total Jours Pris
            </p>

            <p className="text-3xl font-bold text-amber-600 mt-1">
              {totalPris}{" "}
              <span className="text-sm text-slate-500 font-normal">
                jours
              </span>
            </p>
          </div>

          <div className="w-12 h-12 rounded-lg bg-amber-50 text-amber-700 flex items-center justify-center border border-amber-100">
            <Clock className="w-6 h-6" />
          </div>
        </div>

        {/* RESTANT */}
        <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200 flex items-center justify-between">
          <div>
            <p className="text-xs font-bold uppercase text-slate-500 tracking-wider">
              Solde Restant Global
            </p>

            <p className="text-3xl font-bold text-emerald-600 mt-1">
              {totalRestant}{" "}
              <span className="text-sm text-slate-500 font-normal">
                jours
              </span>
            </p>
          </div>

          <div className="w-12 h-12 rounded-lg bg-emerald-50 text-emerald-700 flex items-center justify-center border border-emerald-100">
            <CalendarDays className="w-6 h-6" />
          </div>
        </div>
      </div>

      {/* ======================================================
          DETAIL TABLE
          ====================================================== */}
      <div className="bg-white shadow-sm border border-slate-200 rounded-2xl p-6">
        <div className="flex justify-between items-center mb-4 pb-2 border-b border-slate-100">
          <h2 className="text-base font-bold text-slate-800 flex items-center gap-2">
            <PieChart className="w-5 h-5 text-blue-600" />
            Détail des Quotas par Catégorie de Congé
          </h2>
        </div>

        {loading ? (
          <div className="flex justify-center items-center py-12">
            <span className="loading loading-spinner loading-md text-primary"></span>
          </div>
        ) : soldes.length === 0 ? (
          <div className="text-center py-12 text-slate-500">
            <p className="text-base font-semibold">
              Aucun solde répertorié.
            </p>

            <p className="text-sm text-slate-400 mt-1">
              {isAdmin
                ? "Veuillez cliquer sur 'Initialiser les soldes annuels' pour générer le portefeuille de cet agent."
                : "Vos droits annuels n'ont pas encore été crédités par le service Ressources Humaines."}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="table w-full">
              <thead>
                <tr className="bg-slate-50 text-slate-600 text-xs uppercase tracking-wider">
                  <th className="py-3">
                    Motif / Type de congé
                  </th>

                  <th className="py-3">
                    Exercice
                  </th>

                  <th className="py-3">
                    Acquis
                  </th>

                  <th className="py-3">
                    Consommé
                  </th>

                  <th className="py-3">
                    Solde restant
                  </th>

                  <th className="py-3">
                    Taux d&apos;utilisation
                  </th>

                  <th className="py-3">
                    Mise à jour
                  </th>

                  {isAdmin && (
                    <th className="py-3 text-right">
                      Ajustement
                    </th>
                  )}
                </tr>
              </thead>

              <tbody>
                {soldes.map((solde) => {
                  const type = getTypeInfo(
                    solde.typeCongeId
                  );

                  const acquis = Math.round(
                    Number(
                      solde.soldeAquis ??
                        solde.soldeAcquis
                    ) || 0
                  );

                  const pris = Math.round(
                    Number(
                      solde.soldePris
                    ) || 0
                  );

                  const restant = Math.round(
                    Number(
                      solde.soldeRestant
                    ) || 0
                  );

                  const pct =
                    acquis > 0
                      ? Math.min(
                          100,
                          Math.round(
                            (pris / acquis) * 100
                          )
                        )
                      : 0;

                  const rawDate =
                    solde.date_maj ||
                    solde.dateMaj;

                  return (
                    <tr
                      key={solde.id}
                      className="border-b border-slate-100 hover:bg-slate-50/80 transition-colors"
                    >
                      <td className="py-3.5 font-bold text-slate-800">
                        {type.libelle}

                        {type.code && (
                          <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-mono font-medium bg-slate-100 text-slate-700 border border-slate-200">
                            {type.code}
                          </span>
                        )}
                      </td>

                      <td className="py-3.5 text-slate-600 font-medium">
                        {solde.periode ||
                          new Date().getFullYear()}
                      </td>

                      <td className="py-3.5 text-blue-700 font-bold">
                        {acquis} j
                      </td>

                      <td className="py-3.5 text-amber-600 font-bold">
                        {pris} j
                      </td>

                      <td className="py-3.5">
                        <span className="inline-flex items-center px-2.5 py-1 rounded-md text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                          {restant} j
                        </span>
                      </td>

                      <td className="py-3.5 w-36">
                        <div className="flex items-center gap-2">
                          <progress
                            className="progress progress-primary w-20 h-2 bg-slate-100"
                            value={pct}
                            max="100"
                          ></progress>

                          <span className="text-xs font-semibold text-slate-500">
                            {pct}%
                          </span>
                        </div>
                      </td>

                      <td className="py-3.5 text-xs text-slate-500">
                        {rawDate
                          ? new Date(
                              rawDate
                            ).toLocaleDateString(
                              "fr-FR"
                            )
                          : "-"}
                      </td>

                      {isAdmin && (
                        <td className="py-3.5 text-right">
                          <button
                            onClick={() =>
                              handleOpenAjust(
                                solde,
                                type.libelle
                              )
                            }
                            className="btn btn-xs btn-outline btn-primary font-semibold gap-1"
                          >
                            <Sliders className="w-3 h-3" />
                            Ajuster
                          </button>
                        </td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* ======================================================
          MODAL AJUSTEMENT
          ====================================================== */}
      {modalAjust.open && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6 space-y-4 text-slate-800 border border-slate-100">
            <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2 pb-2 border-b border-slate-100">
              <Sliders className="w-5 h-5 text-blue-600" />
              Ajustement Manuel du Solde
            </h3>

            <p className="text-xs text-slate-500">
              Saisissez le nouveau contingent de jours
              disponibles attribué pour :
              <br />

              <strong className="text-slate-800">
                {modalAjust.typeLibelle}
              </strong>
              .
            </p>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1">
                Nouveau solde restant (Jours entiers)
              </label>

              <input
                type="number"
                step="1"
                min="0"
                value={modalAjust.nouveauSolde}
                onChange={(e) =>
                  setModalAjust({
                    ...modalAjust,
                    nouveauSolde:
                      Math.round(
                        Number(
                          e.target.value
                        )
                      ) || 0,
                  })
                }
                className="input input-bordered w-full font-bold text-slate-900 bg-white"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                onClick={() =>
                  setModalAjust({
                    open: false,
                    soldeId: null,
                    nouveauSolde: 0,
                    typeLibelle: "",
                  })
                }
                className="btn btn-ghost btn-sm font-semibold"
              >
                Annuler
              </button>

              <button
                onClick={handleConfirmAjust}
                className="btn btn-primary btn-sm font-semibold"
              >
                Enregistrer les modifications
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}