"use client";

import { loginUser } from "@/lib/apiLogin.js";
import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";

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

      if (data?.token) {
        localStorage.setItem("token", data.token);
      }
      if (data?.utilisateur) {
        localStorage.setItem("user", JSON.stringify(data.utilisateur));
      } else {
        localStorage.setItem("user", JSON.stringify(data));
      }

      router.push("/dashboard");
    } catch (err) {
      setError(err.message || "Matricule ou mot de passe incorrect.");
    }
  };

  return (
    <div className="min-h-screen bg-slate-100 flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white rounded-xl shadow-2xl border border-slate-200 overflow-hidden">
        {/* En-tête Institutionnel SPAT */}
        <div className="bg-[#023E8A] p-8 text-center text-white relative">
          <div className="w-16 h-16 bg-white/10 rounded-full flex items-center justify-center mx-auto mb-3 border border-white/20">
            <span className="text-2xl font-black tracking-wider text-white">
              SPAT
            </span>
          </div>
          <h1 className="text-2xl font-bold tracking-wide">
            Port de Toamasina
          </h1>
          <p className="text-xs text-blue-200 uppercase tracking-widest mt-1">
            Société du Port à Gestion Autonome de Toamasina
          </p>
        </div>

        {/* Formulaire de connexion */}
        <form onSubmit={handleLogin} className="p-8 space-y-5">
          <div className="text-center mb-2">
            <h2 className="text-lg font-semibold text-slate-800">
              Espace Authentification
            </h2>
            <p className="text-xs text-slate-500">
              Veuillez saisir vos identifiants pour accéder au portail
            </p>
          </div>

          {error && (
            <div className="p-3 bg-red-50 border-l-4 border-red-500 rounded text-red-700 text-xs font-medium">
              {error}
            </div>
          )}

          {/* Champ Matricule */}
          <div className="space-y-1">
            <label className="text-xs font-semibold text-slate-600 block">
              Matricule
            </label>
            <input
              type="text"
              className="w-full px-3 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-[#023E8A] focus:bg-white transition-all"
              placeholder="Ex: MTR-2026-001"
              value={matricule}
              onChange={(e) => setMatricule(e.target.value)}
              required
            />
          </div>

          {/* Champ Mot de passe */}
          <div className="space-y-1">
            <label className="text-xs font-semibold text-slate-600 block">
              Mot de passe
            </label>
            <div className="relative flex items-center">
              <input
                type={mdpSee ? "text" : "password"}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-[#023E8A] focus:bg-white transition-all pr-10"
                placeholder="mot de passe..."
                value={motDePass}
                onChange={(e) => setMotDePass(e.target.value)}
                required
              />
              <button
                type="button"
                onClick={() => setMdpSee(!mdpSee)}
                className="absolute right-3 text-slate-400 hover:text-slate-600 focus:outline-none transition-colors"
                aria-label="Afficher ou masquer le mot de passe"
              >
                {mdpSee ? (
                  /* Icône Œil Barré (Masqué) */
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
                  </svg>
                ) : (
                  /* Icône Œil (Visible) */
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12c1.274 4.057 5.065 7 9.542 7 4.477 0 8.268-2.943 9.542-7-1.274-4.057-5.064-7-9.542-7-4.477 0-8.268 2.943-9.542 7z" />
                    <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                )}
              </button>
            </div>
          </div>

          {/* Lien Mot de passe oublié */}
          <div className="flex justify-end">
            <Link
              href="/Components/mdpForget"
              className="text-xs text-[#023E8A] hover:underline font-medium"
            >
              Mot de passe oublié ?
            </Link>
          </div>

          {/* Bouton de soumission */}
          <button
            type="submit"
            className="w-full py-2.5 bg-[#023E8A] hover:bg-[#002855] text-white font-medium text-sm rounded-lg shadow-md transition-colors duration-200 focus:ring-2 focus:ring-offset-2 focus:ring-[#023E8A]"
          >
            Se connecter
          </button>

          {/* Pied de carte */}
          <div className="pt-4 border-t border-slate-100 text-center">
            <p className="text-[10px] text-slate-400">
              © {new Date().getFullYear()} SPAT Madagascar. Tous droits réservés.
            </p>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Home;