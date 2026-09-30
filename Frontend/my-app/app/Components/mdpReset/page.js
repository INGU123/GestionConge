'use client';
import { Suspense, useState } from "react";
import { useSearchParams } from "next/navigation";

function ResetPasswordForm() {
  const [newPassword, setNewPassword] = useState("");
  const [message, setMessage] = useState("");
  const searchParams = useSearchParams();
  const token = searchParams.get("token");

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const res = await fetch("http://localhost:8080/utilisateur/reset-password?token=" + encodeURIComponent(token || "") + "&newPassword=" + encodeURIComponent(newPassword), {
        method: "POST"
      });
      if (res.ok) {
        setMessage("Mot de passe réinitialisé avec succès.");
      } else {
        setMessage("Erreur : token invalide ou expiré.");
      }
    } catch (err) {
      setMessage("Erreur serveur.");
    }
  };

  return (
    <div>
      <h2>Réinitialiser le mot de passe</h2>
      <form onSubmit={handleSubmit}>
        <input
          type="password"
          placeholder="Nouveau mot de passe"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          required
        />
        <button type="submit">Réinitialiser</button>
      </form>
      <p>{message}</p>
    </div>
  );
}

export default function ResetPassword() {
  return (
    <Suspense fallback={<div>Chargement...</div>}>
      <ResetPasswordForm />
    </Suspense>
  );
}
