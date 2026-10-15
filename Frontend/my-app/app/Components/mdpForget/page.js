'use client';

import { useState } from "react";
import Link from "next/link";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export default function ForgotPassword() {
  const [matricule, setMatricule] = useState("");
  const [message, setMessage] = useState({ text: "", type: "" });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const value = matricule.trim();
    if (!value) {
      setMessage({ text: "Veuillez saisir votre numéro matricule.", type: "error" });
      return;
    }

    setLoading(true);
    setMessage({ text: "", type: "" });
    try {
      const res = await fetch(`${API_URL}/auth/forgot-password`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ matricule: value }),
      });
      const data = await res.json().catch(() => ({}));
      if (res.ok) {
        setMessage({
          text: data.message || "Votre demande a été transmise à l'administrateur. Vous recevrez un e-mail après validation.",
          type: "success",
        });
        setMatricule("");
      } else {
        setMessage({ text: data.message || "La demande n'a pas pu être envoyée. Vérifiez votre matricule.", type: "error" });
      }
    } catch {
      setMessage({ text: "Impossible de contacter le serveur. Veuillez réessayer plus tard.", type: "error" });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 px-4 py-12">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-xl border border-slate-100 p-8 space-y-6">
        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-blue-50 text-blue-600 mb-2">
            <svg xmlns="http://www.w3.org/2000/svg" className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
            </svg>
          </div>
          <h2 className="text-2xl font-bold text-slate-900">Mot de passe oublié ?</h2>
          <p className="text-sm text-slate-500">
            Saisissez votre numéro matricule pour demander à l'administrateur la réinitialisation de votre mot de passe.
          </p>
        </div>

        {message.text && (
          <div className={`p-4 rounded-xl text-sm font-medium ${message.type === "success" ? "bg-emerald-50 text-emerald-700 border border-emerald-200" : "bg-rose-50 text-rose-700 border border-rose-200"}`}>
            {message.text}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="matricule" className="block text-sm font-semibold text-slate-700 mb-1">Numéro matricule</label>
            <input
              id="matricule"
              type="text"
              autoComplete="username"
              placeholder="Ex. SPAT-001"
              value={matricule}
              onChange={(e) => setMatricule(e.target.value)}
              required
              disabled={loading}
              className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-800 placeholder-slate-400 text-sm disabled:opacity-50"
            />
          </div>
          <button type="submit" disabled={loading} className="w-full py-3 px-4 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-xl shadow-md transition disabled:opacity-50 flex items-center justify-center text-sm">
            {loading ? "Envoi en cours..." : "Contacter l'administrateur"}
          </button>
        </form>

        <div className="text-center pt-2">
          <Link href="/" className="text-sm font-medium text-blue-600 hover:text-blue-700">← Retour à la connexion</Link>
        </div>
      </div>
    </div>
  );
}
