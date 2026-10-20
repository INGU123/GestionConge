"use client";

import { Suspense, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import {
  FiArrowLeft,
  FiArrowRight,
  FiCheckCircle,
  FiEye,
  FiEyeOff,
  FiLock,
  FiShield,
  FiAlertCircle,
  FiKey,
} from "react-icons/fi";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export default function ResetPasswordPage() {
  return (
    <Suspense
      fallback={
        <main className="login-page">
          <div className="login-frame">
            <section className="login-brand-panel">
              <div className="login-brand-top">
                <div className="login-logo-wrap">
                  <Image src="/SpatLogo.png" alt="Logo SPAT" width={58} height={58} priority className="login-logo" />
                </div>
                <div>
                  <p className="login-brand-kicker">SOCIÉTÉ PORTUAIRE</p>
                  <p className="login-brand-name">Port de Toamasina</p>
                </div>
              </div>
              <div className="login-brand-copy">
                <span className="login-eyebrow"><span /> SÉCURITÉ DU COMPTE</span>
                <h1>Un nouvel accès, <em>en toute sécurité.</em></h1>
                <p>Chargement de la page de réinitialisation de votre mot de passe.</p>
              </div>
              <div className="login-brand-footer">
                <span>SPAT · Madagascar</span><span className="login-footer-dot" /><span>Portail interne du personnel</span>
              </div>
            </section>
            <section className="login-form-panel">
              <div className="login-form-content">
                <p className="login-overline">RÉINITIALISATION DU COMPTE</p>
                <h2 style={{ color: "#10233d", fontSize: "28px", fontWeight: 760 }}>Chargement…</h2>
              </div>
            </section>
          </div>
        </main>
      }
    >
      <ResetPasswordForm />
    </Suspense>
  );
}

function ResetPasswordForm() {
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [message, setMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const searchParams = useSearchParams();
  const router = useRouter();
  const token = searchParams.get("token");
  const isSuccess = message.toLowerCase().includes("succès") || message.toLowerCase().includes("réinitialisé");

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!token) {
      setMessage("Erreur : token manquant dans l’URL.");
      return;
    }

    if (!newPassword || newPassword.trim().length < 6) {
      setMessage("Le mot de passe doit contenir au moins 6 caractères.");
      return;
    }

    if (newPassword !== confirmPassword) {
      setMessage("Les mots de passe ne correspondent pas.");
      return;
    }

    setIsLoading(true);
    setMessage("");

    try {
      const res = await fetch(`${API_URL}/auth/reset-password`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          token: token,
          newPassword: newPassword,
        }),
      });

      const data = await res.json().catch(() => ({}));

      if (res.ok) {
        setMessage(data.message || "Mot de passe réinitialisé avec succès.");
        setNewPassword("");
        setConfirmPassword("");

        setTimeout(() => {
          router.push("/");
        }, 1500);
      } else {
        setMessage(data.message || "Erreur : token invalide ou expiré.");
      }
    } catch (error) {
      console.error(error);
      setMessage("Erreur de connexion au serveur.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleBack = () => {
    router.back();
  };

  return (
    <main className="login-page">
      <div className="login-frame">
        <section className="login-brand-panel" aria-label="Présentation du Port de Toamasina">
          <div className="login-brand-top">
            <div className="login-logo-wrap">
              <Image src="/SpatLogo.png" alt="Logo SPAT" width={58} height={58} priority className="login-logo" />
            </div>
            <div>
              <p className="login-brand-kicker">SOCIÉTÉ PORTUAIRE</p>
              <p className="login-brand-name">Port de Toamasina</p>
            </div>
          </div>

          <div className="login-brand-copy">
            <span className="login-eyebrow"><span /> SÉCURITÉ DU COMPTE</span>
            <h1>Un nouvel accès, <em>en toute sécurité.</em></h1>
            <p>
              Choisissez un nouveau mot de passe pour retrouver l’accès à votre
              espace professionnel SPAT.
            </p>
          </div>

          <div className="login-brand-visual" aria-hidden="true">
            <div className="login-orbit login-orbit-one" />
            <div className="login-orbit login-orbit-two" />
            <div className="login-visual-card">
              <div className="login-visual-icon"><FiShield /></div>
              <div>
                <strong>Protégez votre compte</strong>
                <span>Choisissez un mot de passe personnel</span>
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
            <span className="login-secure-label"><FiShield /> RÉINITIALISATION SÉCURISÉE</span>
            <span className="login-language">FR <span>⌄</span></span>
          </div>

          <div className="login-form-content">
            <div className="login-form-heading">
              <span className="login-heading-mark" />
              <p className="login-overline">NOUVEAU MOT DE PASSE</p>
              <h2>Réinitialiser l’accès.</h2>
              <p>Créez votre nouveau mot de passe puis confirmez-le pour terminer la procédure.</p>
            </div>

            {message && (
              <div
                role={isSuccess ? "status" : "alert"}
                style={{
                  display: "flex",
                  alignItems: "flex-start",
                  gap: "10px",
                  marginBottom: "22px",
                  padding: "13px 14px",
                  borderRadius: "11px",
                  border: isSuccess ? "1px solid #a7f3d0" : "1px solid #fecdd3",
                  background: isSuccess ? "#ecfdf5" : "#fff1f2",
                  color: isSuccess ? "#047857" : "#a51d37",
                  fontSize: "12px",
                  lineHeight: 1.6,
                }}
              >
                {isSuccess ? <FiCheckCircle size={18} /> : <FiAlertCircle size={18} />}
                <span>{message}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="login-form">
              <div className="login-field">
                <label htmlFor="new-password">Nouveau mot de passe</label>
                <div className="login-input-wrap">
                  <FiLock className="login-input-icon" aria-hidden="true" />
                  <input
                    id="new-password"
                    type={showPassword ? "text" : "password"}
                    placeholder="6 caractères minimum"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    autoComplete="new-password"
                    minLength={6}
                    required
                    disabled={isLoading}
                  />
                  <button
                    type="button"
                    className="login-password-toggle"
                    onClick={() => setShowPassword(!showPassword)}
                    aria-label={showPassword ? "Masquer le mot de passe" : "Afficher le mot de passe"}
                    title={showPassword ? "Masquer le mot de passe" : "Afficher le mot de passe"}
                  >
                    {showPassword ? <FiEyeOff /> : <FiEye />}
                  </button>
                </div>
              </div>

              <div className="login-field">
                <label htmlFor="confirm-password">Confirmer le mot de passe</label>
                <div className="login-input-wrap">
                  <FiKey className="login-input-icon" aria-hidden="true" />
                  <input
                    id="confirm-password"
                    type="password"
                    placeholder="Saisissez à nouveau le mot de passe"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    autoComplete="new-password"
                    required
                    disabled={isLoading}
                  />
                </div>
              </div>

              <button type="submit" className="login-submit" disabled={isLoading}>
                <span>{isLoading ? "Réinitialisation en cours..." : "Réinitialiser le mot de passe"}</span>
                <span className="login-submit-icon">{isLoading ? <FiLock /> : <FiArrowRight />}</span>
              </button>
            </form>

            <div className="login-security-note">
              <FiLock aria-hidden="true" />
              <span>Votre nouveau mot de passe protège l’accès à votre espace professionnel.</span>
            </div>

            <div className="login-forgot-row" style={{ justifyContent: "flex-start", marginTop: "24px" }}>
              <Link href="/" style={{ display: "inline-flex", alignItems: "center", gap: "8px" }}>
                <FiArrowLeft aria-hidden="true" /> Retour à la connexion
              </Link>
              <button
                type="button"
                onClick={handleBack}
                style={{
                  marginLeft: "auto",
                  color: "#0759a5",
                  background: "transparent",
                  border: 0,
                  fontSize: "12px",
                  fontWeight: 750,
                  cursor: "pointer",
                }}
              >
                Page précédente
              </button>
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
}
