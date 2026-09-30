'use client';

import { useState } from "react";

import Link from "next/link";

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState({ text: "", type: "" });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMessage({ text: "", type: "" });

    try {
      const res = await fetch(`http://localhost:8080/utilisateur/forgot-password?email=${encodeURIComponent(email)}`, {
        method: "POST"
      });

      if (res.ok) {
        setMessage({
          text: "Si un compte correspond à cette adresse, un lien de réinitialisation sera envoyé.",
          type: "success"
        });
        setEmail("");
      } else {
        setMessage({
          text: "La demande n'a pas pu être traitée. Vérifiez votre adresse e-mail et réessayez.",
          type: "error"
        });
      }
    } catch (err) {
      setMessage({
        text: "Impossible de contacter le serveur. Veuillez réessayer plus tard.",
        type: "error"
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 px-4 py-12">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-xl border border-slate-100 p-8 space-y-6">
        
        {/* En-tête */}
        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-blue-50 text-blue-600 mb-2">
            <svg xmlns="http://www.w3.org/2000/svg" className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
            </svg>
          </div>
          <h2 className="text-2xl font-bold text-slate-900">Mot de passe oublié ?</h2>
          <p className="text-sm text-slate-500">
            Entrez votre adresse e-mail ci-dessous et nous vous enverrons un lien pour réinitialiser votre mot de passe.
          </p>
        </div>

        {/* Message d'état */}
        {message.text && (
          <div
            className={`p-4 rounded-xl text-sm font-medium transition-all ${
              message.type === "success"
                ? "bg-emerald-50 text-emerald-700 border border-emerald-200"
                : "bg-rose-50 text-rose-700 border border-rose-200"
            }`}
          >
            {message.text}
          </div>
        )}

        {/* Formulaire */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="email" className="block text-sm font-semibold text-slate-700 mb-1">
              Adresse e-mail
            </label>
            <input
              id="email"
              type="email"
              placeholder="nom@exemple.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              disabled={loading}
              className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition text-slate-800 placeholder-slate-400 text-sm disabled:opacity-50"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 px-4 bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white font-semibold rounded-xl shadow-md shadow-blue-500/20 transition duration-150 ease-in-out disabled:opacity-50 flex items-center justify-center text-sm"
          >
            {loading ? (
              <span className="flex items-center space-x-2">
                <svg className="animate-spin h-4 w-4 text-white" viewBox="0 0 24 24" fill="none">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                <span>Envoi en cours...</span>
              </span>
            ) : (
              "Envoyer le lien"
            )}
          </button>
        </form>

        {/* Pied de carte / Retour connexion */}
        <div className="text-center pt-2">
          <Link
  href="/"
  className="text-sm font-medium text-blue-600 hover:text-blue-700 transition"
>
  ← Retour à la connexion
</Link>
        </div>

      </div>
    </div>
  );
}