"use client";

import { loginUser } from "@/lib/apiLogin.js";
import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import {
  FiArrowRight,
  FiEye,
  FiEyeOff,
  FiLock,
  FiShield,
  FiUser,
} from "react-icons/fi";

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
    <main className="login-page">
      <div className="login-frame">
        <section className="login-brand-panel" aria-label="Présentation du Port de Toamasina">
          <div className="login-brand-top">
            <div className="login-logo-wrap">
              <Image
                src="/SpatLogo.png"
                alt="Logo SPAT"
                width={58}
                height={58}
                priority
                className="login-logo"
              />
            </div>
            <div>
              <p className="login-brand-kicker">SOCIÉTÉ PORTUAIRE</p>
              <p className="login-brand-name">Port de Toamasina</p>
            </div>
          </div>

          <div className="login-brand-copy">
            <span className="login-eyebrow"><span /> PORTAIL DU PERSONNEL</span>
            <h1>La gestion de vos congés, <em>en toute simplicité.</em></h1>
            <p>
              Retrouvez vos demandes, consultez vos soldes et suivez vos démarches
              professionnelles depuis un espace unique et sécurisé.
            </p>
          </div>

          <div className="login-brand-visual" aria-hidden="true">
            <div className="login-orbit login-orbit-one" />
            <div className="login-orbit login-orbit-two" />
            <div className="login-visual-card">
              <div className="login-visual-icon"><FiShield /></div>
              <div>
                <strong>Un espace sécurisé</strong>
                <span>Vos informations professionnelles protégées</span>
              </div>
              <span className="login-visual-check">✓</span>
            </div>
          </div>

          <div className="login-brand-footer">
            <span>SPAT · Madagascar</span>
            <span className="login-footer-dot" />
            <span>Portail interne du personnel</span>
          </div>
        </section>

        <section className="login-form-panel">
          <div className="login-form-topline">
            <span className="login-secure-label"><FiShield /> ACCÈS SÉCURISÉ</span>
            <span className="login-language">FR <span>⌄</span></span>
          </div>

          <div className="login-form-content">
            <div className="login-form-heading">
              <span className="login-heading-mark" />
              <p className="login-overline">VOTRE ESPACE PERSONNEL</p>
              <h2>Bon retour parmi nous.</h2>
              <p>Connectez-vous avec vos identifiants SPAT pour continuer.</p>
            </div>

            {error && (
              <div className="login-error" role="alert">
                <span className="login-error-mark">!</span>
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleLogin} className="login-form">
              <div className="login-field">
                <label htmlFor="matricule">Matricule</label>
                <div className="login-input-wrap">
                  <FiUser className="login-input-icon" aria-hidden="true" />
                  <input
                    id="matricule"
                    type="text"
                    placeholder="Saisissez votre matricule"
                    value={matricule}
                    onChange={(e) => setMatricule(e.target.value)}
                    autoComplete="username"
                    required
                  />
                </div>
              </div>

              <div className="login-field">
                <label htmlFor="mot-de-passe">Mot de passe</label>
                <div className="login-input-wrap">
                  <FiLock className="login-input-icon" aria-hidden="true" />
                  <input
                    id="mot-de-passe"
                    type={mdpSee ? "text" : "password"}
                    placeholder="Saisissez votre mot de passe"
                    value={motDePass}
                    onChange={(e) => setMotDePass(e.target.value)}
                    autoComplete="current-password"
                    required
                  />
                  <button
                    type="button"
                    className="login-password-toggle"
                    onClick={() => setMdpSee(!mdpSee)}
                    aria-label={mdpSee ? "Masquer le mot de passe" : "Afficher le mot de passe"}
                    title={mdpSee ? "Masquer le mot de passe" : "Afficher le mot de passe"}
                  >
                    {mdpSee ? <FiEyeOff /> : <FiEye />}
                  </button>
                </div>
              </div>

              <div className="login-forgot-row">
                <Link href="/Components/mdpForget">Mot de passe oublié ?</Link>
              </div>

              <button type="submit" className="login-submit">
                <span>Se connecter</span>
                <span className="login-submit-icon"><FiArrowRight /></span>
              </button>
            </form>

            <div className="login-security-note">
              <FiLock aria-hidden="true" />
              <span>Connexion réservée au personnel autorisé de la SPAT.</span>
            </div>
          </div>

          <footer className="login-form-footer">
            <span>© {new Date().getFullYear()} SPAT Madagascar</span>
            <span>Tous droits réservés</span>
          </footer>
        </section>
      </div>
    </main>
  );
};

export default Home;
