"use client";

import { useState } from "react";
import { getCurrentUser, authFetch } from "@/lib/apiClient";

export default function Parametres() {
  const currentUser = getCurrentUser();
  const [userId] = useState(currentUser?.id ?? null);
  const [userRole] = useState(currentUser?.role || "EMPLOYE");
  const [formData, setFormData] = useState({
    nom: currentUser?.nom || "",
    prenom: currentUser?.prenom || "",
    email: currentUser?.email || "",
    mot_de_pass: "",
    role: currentUser?.role || "EMPLOYE",
    notification: true,
  });
  const [status, setStatus] = useState({ type: "", message: "" });
  const [loading, setLoading] = useState(false);

  const isAdmin = userRole?.toUpperCase() === "ADMIN";

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setStatus({ type: "", message: "" });

    try {
      const payload = {
        id: userId,
        nom: formData.nom.trim(),
        prenom: formData.prenom.trim(),
        email: formData.email.trim(),
      };

      if (formData.mot_de_pass && formData.mot_de_pass.trim().length > 0) {
        payload.password = formData.mot_de_pass.trim();
        payload.mot_de_pass = formData.mot_de_pass.trim();
      }

      // Seul l'administrateur a le droit d'envoyer une modification de rôle
      if (isAdmin && formData.role) {
        payload.role = formData.role;
      }

      const res = await authFetch("http://localhost:8080/utilisateur/update", {
        method: "PUT",
        body: JSON.stringify(payload),
      });

      if (res.ok) {
        const updatedUser = await res.json();
        const stored = localStorage.getItem("user");
        const existing = stored ? JSON.parse(stored) : {};
        const merged = { ...existing, ...updatedUser };
        localStorage.setItem("user", JSON.stringify(merged));

        setFormData((prev) => ({ ...prev, mot_de_pass: "" }));

        setStatus({
          type: "success",
          message: "Vos paramètres et informations de profil ont été enregistrés avec succès !",
        });
      } else {
        const errText = await res.text();
        let message = errText;
        try {
          const parsed = JSON.parse(errText);
          if (parsed && parsed.message) message = parsed.message;
        } catch {}
        setStatus({
          type: "error",
          message: message || "Erreur lors de la mise à jour des paramètres.",
        });
      }
    } catch (err) {
      setStatus({
        type: "error",
        message: `Erreur de connexion avec le serveur: ${err.message}`,
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-slate-800 flex items-center gap-2">
          <span>⚙️</span> Paramètres du Compte
        </h1>
        <p className="text-slate-500 text-sm mt-1">
          Gérez vos informations personnelles et vos préférences de compte.
        </p>
      </div>

      {status.message && (
        <div
          className={`alert ${
            status.type === "success" ? "alert-success text-white" : "alert-error text-white"
          } shadow-sm`}
        >
          <span>{status.message}</span>
        </div>
      )}

      <div className="bg-white shadow-sm border border-slate-200 rounded-2xl p-6">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold uppercase text-slate-600 mb-1">
                Nom
              </label>
              <input
                type="text"
                name="nom"
                value={formData.nom}
                onChange={handleChange}
                required
                className="input input-bordered w-full text-slate-800"
                placeholder="Votre nom"
              />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase text-slate-600 mb-1">
                Prénom
              </label>
              <input
                type="text"
                name="prenom"
                value={formData.prenom}
                onChange={handleChange}
                required
                className="input input-bordered w-full text-slate-800"
                placeholder="Votre prénom"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold uppercase text-slate-600 mb-1">
              Adresse Email
            </label>
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              required
              className="input input-bordered w-full text-slate-800"
              placeholder="adresse@exemple.com"
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase text-slate-600 mb-1">
              Mot de passe
            </label>
            <input
              type="password"
              name="mot_de_pass"
              value={formData.mot_de_pass}
              onChange={handleChange}
              className="input input-bordered w-full text-slate-800"
              placeholder="••••••••"
            />
            <span className="text-xs text-slate-400 mt-1 block">
              Laissez vide pour conserver votre mot de passe actuel.
            </span>
          </div>

          <div>
            <label className="block text-xs font-bold uppercase text-slate-600 mb-1">
              Rôle {isAdmin ? "(Modifiable par Administrateur)" : "(Défini par l'administration)"}
            </label>
            {isAdmin ? (
              <select
                name="role"
                value={formData.role}
                onChange={handleChange}
                className="select select-bordered w-full text-slate-800"
              >
                <option value="EMPLOYE">Employé</option>
                <option value="MANAGER">Manager</option>
                <option value="ADMIN">Administrateur</option>
              </select>
            ) : (
              <input
                type="text"
                disabled
                value={userRole}
                className="input input-bordered w-full bg-slate-100 text-slate-600 cursor-not-allowed font-medium"
              />
            )}
          </div>

          <div className="form-control">
            <label className="label cursor-pointer justify-start gap-3">
              <input
                type="checkbox"
                name="notification"
                checked={formData.notification}
                onChange={handleChange}
                className="checkbox checkbox-primary"
              />
              <span className="label-text font-medium text-slate-700">
                Recevoir les notifications d activité par email
              </span>
            </label>
          </div>

          <div className="pt-2 flex justify-end">
            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary px-6"
            >
              {loading ? (
                <>
                  <span className="loading loading-spinner loading-sm"></span>
                  Enregistrement...
                </>
              ) : (
                "Enregistrer les modifications"
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
