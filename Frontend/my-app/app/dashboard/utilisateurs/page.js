"use client";

import { useEffect, useState } from "react";
import { authFetch, getCurrentUser } from "@/lib/apiClient";
import { useToast } from "../../components/ToastProvider";
import ConfirmDialog from "../../components/ConfirmDialog";
import {
  Users,
  UserPlus,
  Search,
  CheckCircle2,
  ShieldAlert,
  BadgeCheck,
  UserX,
  Calendar,
  Mail,
  ShieldCheck,
  X,
  User,
  MoreHorizontal,
  Eye,
  UserCheck,
  UserMinus
} from "lucide-react";
import { FaEdit, FaTrashAlt } from "react-icons/fa";

export default function UtilisateursPage() {
  const [currentUser] = useState(() => getCurrentUser());
  const [users, setUsers] = useState([]);
  const [services, setServices] = useState([]);
  const [servicesLoading, setServicesLoading] = useState(false);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [showModal, setShowModal] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [deletingUserId, setDeletingUserId] = useState(null);
  const [userToDelete, setUserToDelete] = useState(null);\n  const [detailUser, setDetailUser] = useState(null);\n  const [openActionMenu, setOpenActionMenu] = useState(null);
  const [newUser, setNewUser] = useState({
    matricule: "",
    nom: "",
    prenom: "",
    numero_cin: "",
    date_naissance: "",
    telephone: "",
    adresse: "",
    fonction: "",
    type_contrat: "",
    email: "",
    mot_de_pass: "",
    role: "EMPLOYE",
    service_id: "",
    manager_id: "",
    date_embauche: new Date().toISOString().split("T")[0],
    actif: true,
  });
  const showToast = useToast();

  useEffect(() => {
    let isMounted = true;

    const loadUsers = async () => {
      try {
        setLoading(true);
        const res = await authFetch("http://localhost:8080/utilisateur/all");
        if (res.ok) {
          const data = await res.json();
          if (isMounted) {
            setUsers(Array.isArray(data) ? data : []);
          }
        } else {
          const errorMsg = await res.text();
          console.error("Erreur serveur:", errorMsg);
          if (isMounted) showToast(errorMsg || "Impossible de charger la liste des collaborateurs.", "error");
        }
      } catch (err) {
        console.error("Erreur chargement utilisateurs:", err);
        if (isMounted) showToast("Impossible de charger la liste des collaborateurs.", "error");
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    loadUsers();

    return () => {
      isMounted = false;
    };
  }, [showToast]);

  useEffect(() => {
    let isMounted = true;
    const loadServices = async () => {
      try {
        setServicesLoading(true);
        const res = await authFetch("http://localhost:8080/services");
        if (!res.ok) throw new Error("Impossible de charger les services.");
        const data = await res.json();
        if (isMounted) setServices(Array.isArray(data) ? data : []);
      } catch (err) {
        console.error("Erreur chargement services:", err);
        if (isMounted) showToast("Impossible de charger les services.", "error");
      } finally {
        if (isMounted) setServicesLoading(false);
      }
    };
    loadServices();
    return () => { isMounted = false; };
  }, [showToast]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...(editingUser ? { id: editingUser.id } : {}),
        matricule: newUser.matricule.trim(),
        nom: newUser.nom.trim(),
        prenom: newUser.prenom.trim(),
        email: newUser.email.trim(),
        numero_cin: newUser.numero_cin?.trim() || "",
        date_naissance: newUser.date_naissance || null,
        telephone: newUser.telephone?.trim() || "",
        adresse: newUser.adresse?.trim() || "",
        fonction: newUser.fonction?.trim() || "",
        type_contrat: newUser.type_contrat || "",
        service_id: newUser.service_id ? Number(newUser.service_id) : null,
        manager_id: newUser.manager_id ? Number(newUser.manager_id) : null,
        role: newUser.role,
        actif: editingUser ? newUser.actif : true,
        ...(newUser.date_embauche ? { date_embauche: newUser.date_embauche } : {}),
        ...(newUser.mot_de_pass?.trim() ? { password: newUser.mot_de_pass.trim() } : {}),
      };

      const res = await authFetch(
        `http://localhost:8080/utilisateur/${editingUser ? "update" : "create"}`,
        {
          method: editingUser ? "PUT" : "POST",
          body: JSON.stringify(payload),
        }
      );

      if (res.ok) {
        const responseData = await res.json();
        const createdUser = responseData.utilisateur || responseData;
        setUsers((prev) => editingUser
          ? prev.map((user) => user.id === createdUser.id ? createdUser : user)
          : [...prev, createdUser]
        );
        setShowModal(false);
        setEditingUser(null);
        setNewUser({
          matricule: "", nom: "", prenom: "", numero_cin: "", date_naissance: "",
          telephone: "", adresse: "", fonction: "", type_contrat: "", email: "",
          mot_de_pass: "", role: "EMPLOYE", service_id: "", manager_id: "",
          date_embauche: new Date().toISOString().split("T")[0], actif: true,
        });

        const successText = responseData.message || (editingUser
          ? "Collaborateur modifié avec succès."
          : "Collaborateur créé avec succès. Ses identifiants ont été envoyés par e-mail.");
        showToast(successText, "success");
      } else {
        const txt = await res.text();
        let errMsg = txt;
        try {
          const parsed = JSON.parse(txt);
          if (parsed && parsed.message) errMsg = parsed.message;
        } catch { }
        showToast(errMsg || "Erreur lors de l'enregistrement du collaborateur.", "error");
      }
    } catch (err) {
      showToast(`Erreur : ${err.message}`, "error");
    }
  };

  const handleDelete = async () => {
    if (!userToDelete) return;
    const user = userToDelete;
    setDeletingUserId(user.id);
    try {
      const res = await authFetch(`http://localhost:8080/utilisateur/${user.id}`, {
        method: "DELETE",
      });
      if (!res.ok) {
        const text = await res.text();
        let errorMessage = text;
        try {
          const parsed = JSON.parse(text);
          errorMessage = parsed.message || errorMessage;
        } catch { }
        showToast(errorMessage || "Erreur lors de la suppression du collaborateur.", "error");
        return;
      }

      setUsers((prev) => prev.filter((item) => item.id !== user.id));
      setUserToDelete(null);
      showToast("Collaborateur supprimé avec succès.", "success");
    } catch (err) {
      showToast(`Erreur : ${err.message}`, "error");
    } finally {
      setDeletingUserId(null);
    }
  };

  const startCreate = () => {
    setEditingUser(null);
    setNewUser({
      matricule: "", nom: "", prenom: "", numero_cin: "", date_naissance: "",
      telephone: "", adresse: "", fonction: "", type_contrat: "", email: "", mot_de_pass: "",
      role: "EMPLOYE", service_id: "", manager_id: "",
      date_embauche: new Date().toISOString().split("T")[0], actif: true,
    });
    setShowModal(true);
  };

  const startEdit = (user) => {
    setEditingUser(user);
    setNewUser({
      ...user,
      numero_cin: user.numero_cin || "",
      date_naissance: user.date_naissance || "",
      telephone: user.telephone || "",
      adresse: user.adresse || "",
      fonction: user.fonction || "",
      type_contrat: user.type_contrat || "",
      service_id: user.service_id == null ? "" : String(user.service_id),
      manager_id: user.manager_id == null ? "" : String(user.manager_id),
      mot_de_pass: "",
      date_embauche: user.date_embauche || "",
    });
    setShowModal(true);
  };

  const isAdmin = currentUser?.role?.toUpperCase() === "ADMIN";

  if (!loading && !isAdmin) {
    return (
      <div className="bg-amber-50 border-l-4 border-amber-500 rounded-2xl p-6 shadow-sm flex items-start gap-4">
        <ShieldAlert className="w-6 h-6 text-amber-600 shrink-0 mt-0.5" />
        <div>
          <h3 className="text-base font-bold text-amber-900">Accès Restreint — SPAT</h3>
          <p className="text-sm text-amber-700 mt-1">
            {"Seuls les Administrateurs RH de la SPAT sont habilités à gérer l'annuaire et les comptes collaborateurs."}
          </p>
        </div>
      </div>
    );
  }

  const filteredUsers = users.filter((u) => {
    const term = search.toLowerCase();
    return (
      (u.matricule && u.matricule.toLowerCase().includes(term)) ||
      (u.nom && u.nom.toLowerCase().includes(term)) ||
      (u.prenom && u.prenom.toLowerCase().includes(term)) ||
      (u.email && u.email.toLowerCase().includes(term)) ||
      (u.role && u.role.toLowerCase().includes(term))
    );
  });

  const getRoleBadge = (role) => {
    switch (role?.toUpperCase()) {
      case "ADMIN":
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-xs font-bold bg-blue-50 text-blue-700 border border-blue-200">
            <ShieldCheck className="w-3.5 h-3.5" />
            ADMINISTRATEUR
          </span>
        );
      case "MANAGER":
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-xs font-bold bg-purple-50 text-purple-700 border border-purple-200">
            <BadgeCheck className="w-3.5 h-3.5" />
            MANAGER
          </span>
        );
      case "EMPLOYE":
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-xs font-bold bg-slate-100 text-slate-700 border border-slate-200">
            <User className="w-3.5 h-3.5" />
            EMPLOYÉ
          </span>
        );
    }
  };

  return (
    <div className="space-y-6">
      {/* Banner Institutionnelle SPAT */}
      <div className="bg-slate-900 text-white rounded-2xl p-6 shadow-md border-l-4 border-blue-600 flex justify-between items-center flex-wrap gap-4">
        <div>
          <div className="text-xs font-semibold uppercase tracking-widest text-blue-400 mb-1">
            SPAT — Société du Port à Gestion Autonome de Toamasina
          </div>
          <h1 className="text-2xl font-bold tracking-tight flex items-center gap-2">
            <Users className="w-6 h-6 text-blue-400" />
            Gestion des Collaborateurs & Accès
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            {`Création de comptes, attribution des privilèges et consultation de l'annuaire du personnel.`}
          </p>
        </div>

        <button
          onClick={startCreate}
          className="btn btn-primary text-white font-semibold gap-2 shadow-xs"
        >
          <UserPlus className="w-4 h-4" />
          Nouveau Collaborateur
        </button>
      </div>

      {/* Main Table Container */}
      <div className="bg-white shadow-sm border border-slate-200 rounded-2xl p-6">
        <div className="flex justify-between items-center mb-4 gap-4 flex-wrap">
          <div className="relative w-full max-w-xs">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Rechercher matricule, nom, rôle..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="input input-bordered input-sm w-full pl-9 text-xs text-slate-800 bg-white focus:outline-hidden"
            />
          </div>
          <span className="text-xs text-slate-500 font-medium bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-200">
            Total : <strong className="text-slate-800">{filteredUsers.length}</strong> collaborateur(s)
          </span>
        </div>

        {loading ? (
          <div className="flex justify-center items-center py-16">
            <span className="loading loading-spinner loading-lg text-primary"></span>
          </div>
        ) : filteredUsers.length === 0 ? (
          <div className="text-center py-16 text-slate-400">
            <Users className="w-12 h-12 mx-auto mb-2 text-slate-300" />
            <p className="font-semibold text-slate-600">Aucun utilisateur trouvé.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="table w-full">
              <thead>
                <tr className="bg-slate-50 text-slate-600 text-xs uppercase tracking-wider border-b border-slate-200">
                  <th className="py-3">Matricule</th>
                  <th className="py-3">Collaborateur</th>
                  <th className="py-3">Email</th>
                  <th className="py-3">Rôle & Privilèges</th>
                  <th className="py-3">{`Date d'embauche`}</th>
                  <th className="py-3">Statut</th>
                  <th className="py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map((u) => (
                  <tr key={u.id} className="hover:bg-slate-50/80 transition-colors border-b border-slate-100">
                    <td className="py-3.5">
                      <span className="font-mono text-xs font-bold text-blue-600 bg-blue-50 px-2 py-1 rounded border border-blue-100">
                        {u.matricule || `-`}
                      </span>
                    </td>
                    <td className="py-3.5">
                      <div className="flex items-center gap-2.5">
                        <div className="w-8 h-8 rounded-full bg-slate-100 text-slate-600 flex items-center justify-center font-bold text-xs border border-slate-200">
                          {u.prenom ? u.prenom[0].toUpperCase() : "U"}
                        </div>
                        <span className="font-bold text-slate-800 text-sm">
                          {u.prenom} {u.nom}
                        </span>
                      </div>
                    </td>
                    <td className="py-3.5 text-xs text-slate-600 font-medium">
                      <div className="flex items-center gap-1.5">
                        <Mail className="w-3.5 h-3.5 text-slate-400" />
                        {u.email}
                      </div>
                    </td>
                    <td className="py-3.5">{getRoleBadge(u.role)}</td>
                    <td className="py-3.5 text-xs text-slate-600">
                      <div className="flex items-center gap-1.5 font-medium">
                        <Calendar className="w-3.5 h-3.5 text-slate-400" />
                        {u.date_embauche || "-"}
                      </div>
                    </td>
                    <td className="py-3.5">
                      {u.actif !== false ? (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                          <CheckCircle2 className="w-3 h-3" />
                          Actif
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-bold bg-rose-50 text-rose-700 border border-rose-200">
                          <UserX className="w-3 h-3" />
                          Inactif
                        </span>
                      )}
                    </td>
                    <td className="py-3.5">
                      <div className="relative flex justify-end">
                        <button
                          type="button"
                          onClick={() => setOpenActionMenu(openActionMenu === u.id ? null : u.id)}
                          className="inline-flex h-9 w-9 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-600 shadow-sm transition hover:border-blue-200 hover:bg-blue-50 hover:text-[#023E8A] focus:outline-none focus:ring-2 focus:ring-blue-200"
                          title="Actions du collaborateur"
                          aria-label={`Actions pour ${u.prenom} ${u.nom}`}
                          aria-haspopup="menu"
                          aria-expanded={openActionMenu === u.id}
                        >
                          <MoreHorizontal className="h-5 w-5" />
                        </button>
                        {openActionMenu === u.id && (
                          <div role="menu" className="absolute right-0 top-10 z-30 min-w-48 rounded-xl border border-slate-200 bg-white p-1.5 shadow-xl">
                            <button type="button" role="menuitem" onClick={() => { setDetailUser(u); setOpenActionMenu(null); }} className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-slate-700 hover:bg-slate-50"><Eye className="h-4 w-4" />Voir détail</button>
                            <button type="button" role="menuitem" onClick={() => { startEdit(u); setOpenActionMenu(null); }} className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-slate-700 hover:bg-slate-50"><FaEdit />Modifier</button>
                            <button type="button" role="menuitem" onClick={() => { startEdit(u); setOpenActionMenu(null); }} className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-amber-700 hover:bg-amber-50"><UserMinus className="h-4 w-4" />Désactiver / activer</button>
                            <div className="my-1 border-t border-slate-100" />
                            <button type="button" role="menuitem" onClick={() => { setUserToDelete(u); setOpenActionMenu(null); }} disabled={deletingUserId === u.id || u.id === currentUser?.id} className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-rose-700 hover:bg-rose-50 disabled:cursor-not-allowed disabled:opacity-50"><FaTrashAlt />Supprimer</button>
                          </div>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Formulaire partagé de création et modification */}
      {showModal && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-2xl max-h-[90vh] overflow-y-auto w-full p-6 space-y-4 text-slate-800 border border-slate-100">
            <div className="flex justify-between items-center pb-3 border-b border-slate-100">
              <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                <UserPlus className="w-5 h-5 text-blue-600" />
                {editingUser ? "Modifier le collaborateur" : "Ajouter un Collaborateur SPAT"}
              </h3>
              <button
                onClick={() => setShowModal(false)}
                className="text-slate-400 hover:text-slate-600 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-3">
              <div>
                <label className="block text-xs font-bold uppercase text-slate-600 mb-1">
                  Matricule
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ex: MTR-001"
                  value={newUser.matricule}
                  onChange={(e) => setNewUser({ ...newUser, matricule: e.target.value })}
                  className="input input-bordered input-sm w-full bg-white text-slate-800 focus:outline-hidden"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold uppercase text-slate-600 mb-1">
                    Nom
                  </label>
                  <input
                    type="text"
                    required
                    value={newUser.nom}
                    onChange={(e) => setNewUser({ ...newUser, nom: e.target.value })}
                    className="input input-bordered input-sm w-full bg-white text-slate-800 focus:outline-hidden"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold uppercase text-slate-600 mb-1">
                    Prénom
                  </label>
                  <input
                    type="text"
                    required
                    value={newUser.prenom}
                    onChange={(e) => setNewUser({ ...newUser, prenom: e.target.value })}
                    className="input input-bordered input-sm w-full bg-white text-slate-800 focus:outline-hidden"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold uppercase text-slate-600 mb-1">Numéro CIN</label>
                  <input
                    type="text"
                    maxLength={30}
                    autoComplete="off"
                    placeholder="Numéro de carte d'identité"
                    value={newUser.numero_cin || ""}
                    onChange={(e) => setNewUser({ ...newUser, numero_cin: e.target.value })}
                    className="input input-bordered input-sm w-full bg-white text-slate-800 focus:outline-hidden"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold uppercase text-slate-600 mb-1">Date de naissance</label>
                  <input
                    type="date"
                    value={newUser.date_naissance || ""}
                    max={new Date().toISOString().split("T")[0]}
                    onChange={(e) => setNewUser({ ...newUser, date_naissance: e.target.value })}
                    className="input input-bordered input-sm w-full bg-white text-slate-800 focus:outline-hidden"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase text-slate-600 mb-1">
                  Email Institutionnel
                </label>
                <input
                  type="email"
                  required
                  placeholder="agent@port-toamasina.mg"
                  value={newUser.email}
                  onChange={(e) => setNewUser({ ...newUser, email: e.target.value })}
                  className="input input-bordered input-sm w-full bg-white text-slate-800 focus:outline-hidden"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold uppercase text-slate-600 mb-1">Téléphone</label>
                  <input
                    type="tel"
                    maxLength={30}
                    autoComplete="tel"
                    value={newUser.telephone || ""}
                    onChange={(e) => setNewUser({ ...newUser, telephone: e.target.value })}
                    className="input input-bordered input-sm w-full bg-white text-slate-800 focus:outline-hidden"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold uppercase text-slate-600 mb-1">Adresse</label>
                  <input
                    type="text"
                    maxLength={500}
                    autoComplete="street-address"
                    value={newUser.adresse || ""}
                    onChange={(e) => setNewUser({ ...newUser, adresse: e.target.value })}
                    className="input input-bordered input-sm w-full bg-white text-slate-800 focus:outline-hidden"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold uppercase text-slate-600 mb-1">
                    Mot de passe (optionnel)
                  </label>
                  <input
                    type="password"
                    placeholder="Laisser vide pour auto"
                    value={newUser.mot_de_pass}
                    onChange={(e) => setNewUser({ ...newUser, mot_de_pass: e.target.value })}
                    className="input input-bordered input-sm w-full bg-white text-slate-800 focus:outline-hidden"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold uppercase text-slate-600 mb-1">
                    Rôle
                  </label>
                  <select
                    value={newUser.role}
                    onChange={(e) => setNewUser({ ...newUser, role: e.target.value })}
                    className="select select-bordered select-sm w-full bg-white text-slate-800 focus:outline-hidden"
                  >
                    <option value="EMPLOYE">Employé</option>
                    <option value="MANAGER">Manager</option>
                    {editingUser?.role === "ADMIN" && <option value="ADMIN">Administrateur</option>}
                  </select>
                </div>
              </div>

                            <div>
                <label className="block text-xs font-bold uppercase text-slate-600 mb-1">
                  {`Date d'embauche`}
                </label>
                <input
                  type="date"
                  value={newUser.date_embauche || ""}
                  onChange={(e) => setNewUser({ ...newUser, date_embauche: e.target.value })}
                  className="input input-bordered input-sm w-full bg-white text-slate-800 focus:outline-hidden"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold uppercase text-slate-600 mb-1">Fonction / Poste</label>
                  <input
                    type="text"
                    maxLength={100}
                    placeholder="Ex. Agent administratif"
                    value={newUser.fonction || ""}
                    onChange={(e) => setNewUser({ ...newUser, fonction: e.target.value })}
                    className="input input-bordered input-sm w-full bg-white text-slate-800 focus:outline-hidden"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold uppercase text-slate-600 mb-1">Type de contrat</label>
                  <select
                    value={newUser.type_contrat || ""}
                    onChange={(e) => setNewUser({ ...newUser, type_contrat: e.target.value })}
                    className="select select-bordered select-sm w-full bg-white text-slate-800 focus:outline-hidden"
                  >
                    <option value="">Non renseigné</option>
                    <option value="CDI">CDI</option>
                    <option value="CDD">CDD</option>
                    <option value="Stage">Stage</option>
                    <option value="Intérim">Intérim</option>
                    <option value="Autre">Autre</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold uppercase text-slate-600 mb-1">Service</label>
                  <select
                    value={newUser.service_id == null ? "" : String(newUser.service_id)}
                    onChange={(e) => setNewUser({ ...newUser, service_id: e.target.value })}
                    disabled={servicesLoading}
                    className="select select-bordered select-sm w-full bg-white text-slate-800 focus:outline-hidden"
                  >
                    <option value="">{servicesLoading ? "Chargement des services..." : "Aucun service"}</option>
                    {services.map((service) => (
                      <option key={service.id} value={service.id}>{service.nom}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-bold uppercase text-slate-600 mb-1">Responsable hiérarchique</label>
                  <select
                    value={newUser.manager_id == null ? "" : String(newUser.manager_id)}
                    onChange={(e) => setNewUser({ ...newUser, manager_id: e.target.value })}
                    className="select select-bordered select-sm w-full bg-white text-slate-800 focus:outline-hidden"
                  >
                    <option value="">Aucun responsable</option>
                    {users.filter((user) => user.role?.toUpperCase() === "MANAGER" && user.id !== editingUser?.id).map((manager) => (
                      <option key={manager.id} value={manager.id}>{manager.prenom} {manager.nom} — {manager.matricule}</option>
                    ))}
                  </select>
                </div>
              </div>

              {editingUser && (
                <label className="flex items-center gap-2 text-sm font-medium text-slate-700">
                  <input
                    type="checkbox"
                    checked={newUser.actif !== false}
                    onChange={(e) => setNewUser({ ...newUser, actif: e.target.checked })}
                    className="checkbox checkbox-sm"
                  />
                  Compte actif
                </label>
              )}

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => { setShowModal(false); setEditingUser(null); }}
                  className="btn btn-ghost btn-sm font-semibold"
                >
                  Annuler
                </button>
                <button type="submit" className="btn btn-primary btn-sm text-white font-semibold">
                  {editingUser ? "Enregistrer" : "Créer le compte"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
      {detailUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4" onClick={() => setDetailUser(null)}>
          <section role="dialog" aria-modal="true" aria-labelledby="collaborateur-detail-title" className="w-full max-w-lg rounded-2xl border border-slate-200 bg-white p-6 text-slate-800 shadow-2xl" onClick={(e) => e.stopPropagation()}>
            <div className="mb-5 flex items-start justify-between border-b border-slate-100 pb-4">
              <div><p className="text-xs font-bold uppercase tracking-widest text-[#023E8A]">Fiche collaborateur</p><h2 id="collaborateur-detail-title" className="mt-1 text-xl font-bold">{detailUser.prenom} {detailUser.nom}</h2></div>
              <button type="button" onClick={() => setDetailUser(null)} className="btn btn-ghost btn-sm btn-square" aria-label="Fermer le détail"><X className="h-4 w-4" /></button>
            </div>
            <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div><dt className="text-xs font-semibold uppercase text-slate-500">Matricule</dt><dd className="mt-1 text-sm font-medium">{detailUser.matricule || "—"}</dd></div>
              <div><dt className="text-xs font-semibold uppercase text-slate-500">Rôle</dt><dd className="mt-1 text-sm font-medium">{detailUser.role || "—"}</dd></div>
              <div><dt className="text-xs font-semibold uppercase text-slate-500">E-mail</dt><dd className="mt-1 break-all text-sm font-medium">{detailUser.email || "—"}</dd></div>
              <div><dt className="text-xs font-semibold uppercase text-slate-500">Téléphone</dt><dd className="mt-1 text-sm font-medium">{detailUser.telephone || "—"}</dd></div>
              <div><dt className="text-xs font-semibold uppercase text-slate-500">Fonction</dt><dd className="mt-1 text-sm font-medium">{detailUser.fonction || "—"}</dd></div>
              <div><dt className="text-xs font-semibold uppercase text-slate-500">Date d'embauche</dt><dd className="mt-1 text-sm font-medium">{detailUser.date_embauche || "—"}</dd></div>
              <div><dt className="text-xs font-semibold uppercase text-slate-500">Service</dt><dd className="mt-1 text-sm font-medium">{services.find((s) => String(s.id) === String(detailUser.service_id))?.nom || detailUser.service?.nom || "—"}</dd></div>
              <div><dt className="text-xs font-semibold uppercase text-slate-500">Statut</dt><dd className={`mt-1 text-sm font-semibold ${detailUser.actif === false ? "text-rose-600" : "text-emerald-600"}`}>{detailUser.actif === false ? "Désactivé" : "Actif"}</dd></div>
            </dl>
            <div className="mt-6 flex justify-end gap-2 border-t border-slate-100 pt-4"><button type="button" onClick={() => setDetailUser(null)} className="btn btn-ghost btn-sm">Fermer</button><button type="button" onClick={() => { startEdit(detailUser); setDetailUser(null); }} className="btn btn-primary btn-sm text-white"><FaEdit /> Modifier</button></div>
          </section>
        </div>
      )}
      {userToDelete && (
        <ConfirmDialog
          title="Supprimer ce collaborateur ?"
          message={`Le compte de ${userToDelete.prenom} ${userToDelete.nom} sera supprimé. Cette action ne peut pas être annulée.`}
          pending={deletingUserId === userToDelete.id}
          onCancel={() => {
            if (deletingUserId === null) setUserToDelete(null);
          }}
          onConfirm={handleDelete}
        />
      )}
    </div>
  );
}