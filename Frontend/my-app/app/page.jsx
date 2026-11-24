"use client";

import { loginUser } from "@/lib/apiLogin.js";
import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import Image from "next/image";

const Home = () => {
  const [matricule, setMatricule] = useState("");
  const [motDePass, setMotDePass] = useState("");
  const [error, setError] = useState("");
  const [mdpSee, setMdpSee] = useState(false);
  const router = useRouter();

  const handleLogin = async (e) => {
    e.preventDefault();
    setError("");
    try {
      const data = await loginUser(matricule, motDePass);
      if (data?.token) localStorage.setItem("token", data.token);
      if (data?.utilisateur) localStorage.setItem("user", JSON.stringify(data.utilisateur));
      else localStorage.setItem("user", JSON.stringify(data));
      router.push("/dashboard");
    } catch (err) {
      setError(err.message || "Matricule ou mot de passe incorrect.");
    }
  };

  return (
    <main className="min-h-screen bg-slate-100 flex items-center justify-center p-4 sm:p-6">
      <section className="w-full max-w-6xl min-h-[640px] grid grid-cols-1 lg:grid-cols-2 overflow-hidden rounded-3xl bg-white shadow-2xl border border-slate-200">
        <div className="relative hidden lg:flex flex-col justify-between overflow-hidden bg-[#023E8A] p-10 text-white">
          <div className="absolute inset-0 bg-cover bg-center opacity-35" style={{ backgroundImage: "url('https://img.magnific.com/vecteurs-libre/concept-connexion-pour-page-destination_23-2148269085.jpg?semt=ais_hybrid&w=740&q=80')" }} />
          <div className="absolute inset-0 bg-gradient-to-br from-[#023E8A]/95 via-[#023E8A]/75 to-[#001b43]/90" />
          <div className="relative z-10 flex items-center gap-4">
            <div className="flex h-16 w-16 items-center justify-center rounded-2xl border border-white/25 bg-white/10 p-2 backdrop-blur-sm">
              <Image src="/SpatLogo.png" alt="Logo SPAT" width={52} height={52} className="object-contain" priority />
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-blue-200">SPAT Madagascar</p>
              <h1 className="mt-1 text-2xl font-bold tracking-wide">Port de Toamasina</h1>
            </div>
          </div>
          <div className="relative z-10 max-w-md py-12">
            <span className="mb-5 inline-flex rounded-full border border-white/25 bg-white/10 px-4 py-2 text-xs font-semibold uppercase tracking-[0.18em] text-blue-100">Portail du personnel</span>
            <h2 className="text-4xl font-bold leading-tight">Votre espace professionnel, en toute simplicité.</h2>
            <p className="mt-5 text-base leading-7 text-blue-100">Accédez à votre espace sécurisé pour consulter vos congés et suivre vos demandes.</p>
          </div>
          <p className="relative z-10 text-xs text-blue-200">Société du Port à Gestion Autonome de Toamasina</p>
        </div>

        <div className="flex items-center justify-center px-6 py-10 sm:px-10 lg:px-12">
          <div className="w-full max-w-md">
            <div className="mb-8 flex flex-col items-center text-center lg:items-start lg:text-left">
              <div className="mb-5 flex h-16 w-16 items-center justify-center rounded-2xl border border-blue-100 bg-blue-50 p-2 lg:hidden">
                <Image src="/SpatLogo.png" alt="Logo SPAT" width={48} height={48} className="object-contain" />
              </div>
              <p className="text-xs font-bold uppercase tracking-[0.2em] text-[#023E8A]">Port de Toamasina</p>
              <h2 className="mt-2 text-3xl font-bold tracking-tight text-slate-900">Espace Authentification</h2>
              <p className="mt-3 text-sm leading-6 text-slate-500">Veuillez saisir vos identifiants pour accéder au portail.</p>
            </div>

            {error && <div role="alert" className="mb-5 rounded-xl border border-red-200 border-l-4 border-l-red-500 bg-red-50 p-3 text-sm font-medium text-red-700">{error}</div>}

            <form onSubmit={handleLogin} className="space-y-5">
              <div className="space-y-2">
                <label htmlFor="matricule" className="block text-sm font-semibold text-slate-700">Matricule</label>
                <input id="matricule" type="text" autoComplete="username" className="w-full rounded-xl border border-slate-300 bg-slate-50 px-4 py-3.5 text-sm text-slate-900 outline-none transition focus:border-[#023E8A] focus:bg-white focus:ring-4 focus:ring-blue-100" placeholder="Ex: MTR-2026-001" value={matricule} onChange={(e) => setMatricule(e.target.value)} required />
              </div>
              <div className="space-y-2">
                <label htmlFor="motDePass" className="block text-sm font-semibold text-slate-700">Mot de passe</label>
                <div className="relative">
                  <input id="motDePass" type={mdpSee ? "text" : "password"} autoComplete="current-password" className="w-full rounded-xl border border-slate-300 bg-slate-50 px-4 py-3.5 pr-12 text-sm text-slate-900 outline-none transition focus:border-[#023E8A] focus:bg-white focus:ring-4 focus:ring-blue-100" placeholder="mot de passe..." value={motDePass} onChange={(e) => setMotDePass(e.target.value)} required />
                  <button type="button" onClick={() => setMdpSee(!mdpSee)} className="absolute inset-y-0 right-0 flex items-center px-4 text-slate-400 transition hover:text-[#023E8A]" aria-label={mdpSee ? "Masquer le mot de passe" : "Afficher le mot de passe"}>
                    {mdpSee ? <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="h-5 w-5"><path strokeLinecap="round" strokeLinejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" /></svg> : <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="h-5 w-5"><path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12c1.274 4.057 5.065 7 9.542 7 4.477 0 8.268-2.943 9.542-7-1.274-4.057-5.064-7-9.542-7-4.477 0-8.268 2.943-9.542 7z" /><path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg>}
                  </button>
                </div>
              </div>
              <div className="flex justify-end"><Link href="/Components/mdpForget" className="text-sm font-semibold text-[#023E8A] transition hover:text-[#002855] hover:underline">Mot de passe oublié ?</Link></div>
              <button type="submit" className="flex w-full items-center justify-center gap-2 rounded-xl bg-[#023E8A] px-5 py-3.5 text-sm font-bold text-white shadow-lg shadow-blue-900/20 transition hover:bg-[#002855] focus:outline-none focus:ring-4 focus:ring-blue-200">
                Se connecter <span aria-hidden="true">→</span>
              </button>
            </form>
            <p className="mt-8 border-t border-slate-100 pt-5 text-center text-xs text-slate-400">© {new Date().getFullYear()} SPAT Madagascar. Tous droits réservés.</p>
          </div>
        </div>
      </section>
    </main>
  );
};

export default Home;
