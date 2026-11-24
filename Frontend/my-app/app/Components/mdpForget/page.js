'use client';

import { useState } from "react";
import Link from "next/link";
import Image from "next/image";

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
        setMessage({ text: data.message || "Votre demande a été transmise à l'administrateur. Vous recevrez un e-mail après validation.", type: "success" });
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
    <main className="min-h-screen bg-slate-100 flex items-center justify-center p-4 sm:p-6">
      <section className="w-full max-w-5xl min-h-[560px] grid grid-cols-1 lg:grid-cols-2 overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-2xl">
        <div className="relative hidden lg:flex flex-col justify-between overflow-hidden bg-[#023E8A] p-10 text-white">
          <div className="absolute inset-0 bg-cover bg-center opacity-35" style={{ backgroundImage: "url('https://img.magnific.com/vecteurs-libre/concept-connexion-pour-page-destination_23-2148269085.jpg?semt=ais_hybrid&w=740&q=80')" }} />
          <div className="absolute inset-0 bg-gradient-to-br from-[#023E8A]/95 via-[#023E8A]/75 to-[#001b43]/90" />
          <div className="relative z-10 flex items-center gap-4">
            <div className="flex h-16 w-16 items-center justify-center rounded-2xl border border-white/25 bg-white/10 p-2"><Image src="/SpatLogo.png" alt="Logo SPAT" width={52} height={52} className="object-contain" /></div>
            <div><p className="text-xs font-semibold uppercase tracking-[0.24em] text-blue-200">SPAT Madagascar</p><h1 className="mt-1 text-2xl font-bold">Port de Toamasina</h1></div>
          </div>
          <div className="relative z-10 max-w-md py-12"><span className="inline-flex rounded-full border border-white/25 bg-white/10 px-4 py-2 text-xs font-semibold uppercase tracking-widest text-blue-100">Assistance au compte</span><h2 className="mt-5 text-4xl font-bold leading-tight">Retrouvez l’accès à votre espace.</h2><p className="mt-5 text-base leading-7 text-blue-100">Faites une demande de réinitialisation en utilisant votre numéro matricule.</p></div>
          <p className="relative z-10 text-xs text-blue-200">Société du Port à Gestion Autonome de Toamasina</p>
        </div>
        <div className="flex items-center justify-center px-6 py-10 sm:px-10 lg:px-12">
          <div className="w-full max-w-md">
            <Link href="/" className="mb-8 inline-flex items-center gap-2 text-sm font-semibold text-[#023E8A] transition hover:text-[#002855]"><span aria-hidden="true">←</span> Retour à la connexion</Link>
            <div className="mb-8 flex items-start gap-4">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl border border-blue-100 bg-blue-50 text-[#023E8A]">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" /></svg>
              </div>
              <div><p className="text-xs font-bold uppercase tracking-[0.2em] text-[#023E8A]">Port de Toamasina</p><h2 className="mt-1 text-2xl font-bold tracking-tight text-slate-900">Mot de passe oublié ?</h2><p className="mt-2 text-sm leading-6 text-slate-500">Saisissez votre numéro matricule pour demander à l'administrateur la réinitialisation de votre mot de passe.</p></div>
            </div>
            {message.text && <div role="status" className={`mb-5 rounded-xl border border-l-4 p-4 text-sm font-medium ${message.type === "success" ? "border-emerald-200 border-l-emerald-500 bg-emerald-50 text-emerald-700" : "border-rose-200 border-l-rose-500 bg-rose-50 text-rose-700"}`}>{message.text}</div>}
            <form onSubmit={handleSubmit} className="space-y-5">
              <div><label htmlFor="matricule" className="mb-2 block text-sm font-semibold text-slate-700">Numéro matricule</label><input id="matricule" type="text" autoComplete="username" placeholder="Ex. SPAT-001" value={matricule} onChange={(e) => setMatricule(e.target.value)} required disabled={loading} className="w-full rounded-xl border border-slate-300 bg-slate-50 px-4 py-3.5 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-[#023E8A] focus:bg-white focus:ring-4 focus:ring-blue-100 disabled:opacity-50" /></div>
              <button type="submit" disabled={loading} className="flex w-full items-center justify-center rounded-xl bg-[#023E8A] px-5 py-3.5 text-sm font-bold text-white shadow-lg shadow-blue-900/20 transition hover:bg-[#002855] focus:outline-none focus:ring-4 focus:ring-blue-200 disabled:cursor-not-allowed disabled:opacity-50">{loading ? "Envoi en cours..." : "Contacter l'administrateur"}</button>
            </form>
            <p className="mt-8 border-t border-slate-100 pt-5 text-center text-xs text-slate-400">© {new Date().getFullYear()} SPAT Madagascar. Tous droits réservés.</p>
          </div>
        </div>
      </section>
    </main>
  );
}
