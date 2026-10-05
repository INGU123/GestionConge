'use client';

import { Suspense, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<div className="max-w-md mx-auto mt-10 p-6 text-center text-slate-600">Chargement...</div>}>
      <ResetPasswordForm />
    </Suspense>
  );
}

function ResetPasswordForm() {
  const [newPassword, setNewPassword] = useState("");
  const [message, setMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const searchParams = useSearchParams();
  const router = useRouter();
  const token = searchParams.get("token");

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!token) {
      setMessage("Erreur : token manquant.");
      return;
    }

    setIsLoading(true);
    setMessage("");

    try {
      const res = await fetch(
        `http://localhost:8080/utilisateur/reset-password?token=${encodeURIComponent(token)}&newPassword=${encodeURIComponent(newPassword)}`,
        { method: "POST" }
      );

      if (res.ok) {
        setMessage("Mot de passe réinitialisé avec succès.");
      } else {
        setMessage("Erreur : token invalide ou expiré.");
      }
    } catch {
      setMessage("Erreur de connexion au serveur.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleBack = () => {
    // Redirige vers la page de connexion ou la page précédente
    router.back(); 
    // Si tu préfères une route fixe, remplace par : router.push('/login');
  };

  return (
    <div className="max-w-md mx-auto mt-10 p-6 border rounded-lg shadow-sm bg-white">
      <h2 className="text-xl font-semibold mb-4 text-gray-800">Réinitialiser le mot de passe</h2>
      
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Champ de mot de passe avec option voir / cacher */}
        <div className="relative">
          <input
            type={showPassword ? "text" : "password"}
            placeholder="Nouveau mot de passe"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            required
            className="w-full px-3 py-2 pr-10 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-500 hover:text-gray-700"
            tabIndex={-1}
          >
            {showPassword ? (
              // Icône œil barré (Cacher)
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                <path strokeLinecap="round" strokeLinejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
              </svg>
            ) : (
              // Icône œil ouvert (Voir)
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                <path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            )}
          </button>
        </div>

        {/* Bouton de validation */}
        <button
          type="submit"
          disabled={isLoading}
          className="w-full bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 disabled:opacity-50 transition-colors font-medium"
        >
          {isLoading ? "Chargement..." : "Réinitialiser"}
        </button>

        {/* Bouton de retour (sans modifier) */}
        <button
          type="button"
          onClick={handleBack}
          className="w-full bg-gray-100 text-gray-700 py-2 rounded-md hover:bg-gray-200 transition-colors font-medium"
        >
          Retour
        </button>
      </form>

      {message && <p className="mt-4 text-sm text-center font-medium">{message}</p>}
    </div>
  );
}