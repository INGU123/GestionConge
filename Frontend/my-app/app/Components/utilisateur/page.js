"use client";
import { postData } from "@/app/api/home";
import { useState } from "react";
import Link from "next/link";

export default function Utilisateur() {
  const [formData, setFormData] = useState({
    nom: "",
    prenom: "",
    email: "",
    password: "", // Utiliser "password" conformément à l'entité Spring Boot
    role: "EMPLOYE",
    service_id: null,
    manager_id: null,
    date_embauche: "",
    actif: true,
    date_creation: new Date().toISOString(),
  });

  const [generatedPassword, setGeneratedPassword] = useState(null);
  const [message, setMessage] = useState("");

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;

    setFormData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage("");
    setGeneratedPassword(null);

    const payload = {
      ...formData,
      service_id: formData.service_id ? Number(formData.service_id) : null,
      manager_id: formData.manager_id ? Number(formData.manager_id) : null,
    };

    try {
      const result = await postData("/utilisateur/create", payload);
      console.log("Utilisateur créé :", result);
      
      if (result?.generatedPassword) {
        setGeneratedPassword(result.generatedPassword);
      }
      setMessage(result?.message || "Utilisateur créé avec succès !");
    } catch (error) {
      console.error("Erreur lors de la création :", error.message);
      setMessage("Erreur lors de la création de l'utilisateur.");
    }
  };

  return (
    <div>
      <div>
        <form
          onSubmit={handleSubmit}
          className="flex flex-col items-center space-y-2"
        >
          <h1 className="text-3xl font-bold underline">Création utilisateur</h1>

          {message && (
            <div className="p-2 my-2 bg-blue-100 text-blue-800 rounded text-sm">
              {message}
            </div>
          )}

          {generatedPassword && (
            <div className="p-3 my-2 bg-green-100 text-green-900 font-mono text-sm rounded border border-green-300">
              Mot de passe généré : <strong>{generatedPassword}</strong>
            </div>
          )}

          <input
            type="text"
            className="input input-sm text-white px-4"
            name="nom"
            onChange={handleChange}
            placeholder="Nom"
            required
          />
          <input
            type="text"
            className="input input-sm text-white px-4"
            name="prenom"
            onChange={handleChange}
            placeholder="Prénom"
            required
          />
          <input
            type="email"
            className="input input-sm text-white px-4"
            name="email"
            onChange={handleChange}
            placeholder="Email"
            required
          />
          
          {/* Saisie masquer du mot de passe (optionnel) */}
          <input
            type="password"
            className="input input-sm text-white px-4"
            name="password"
            onChange={handleChange}
            placeholder="Mot de passe (laisser vide pour auto)"
          />

          {/* Input Rôle avec fond blanc et texte noir */}
          <input
            type="text"
            className="input input-sm bg-white text-black px-4"
            name="role"
            value={formData.role}
            onChange={handleChange}
            placeholder="Rôle (ex: EMPLOYE, MANAGER, ADMIN)"
          />

          <input
            type="number"
            className="input input-sm text-white px-4"
            name="service_id"
            onChange={handleChange}
            placeholder="ID Service"
          />
          <input
            type="number"
            className="input input-sm text-white px-4"
            name="manager_id"
            onChange={handleChange}
            placeholder="ID Manager"
          />
          <input
            type="date"
            className="input input-sm text-white px-4"
            name="date_embauche"
            onChange={handleChange}
          />

          <button type="submit" className="btn btn-primary mt-2">
            Ajouter
          </button>
        </form>

        <div className="mt-4 text-center">
          <span>Déjà membre ? </span>
          <Link href="/">
            <button className="btn btn-error btn-sm">Login</button>
          </Link>
        </div>
      </div>
    </div>
  );
}