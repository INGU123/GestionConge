"use client";

import { Suspense, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { useSearchParams } from "next/navigation";
import {
  FiArrowLeft,
  FiArrowRight,
  FiCheckCircle,
  FiAlertCircle,
  FiLock,
  FiShield,
  FiUser,
  FiKey,
  FiEye,
  FiEyeOff,
} from "react-icons/fi";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<main className="login-page"><div className="login-frame"><section className="login-form-panel"><div className="login-form-content"><p className="login-overline">RÉINITIALISATION DU COMPTE</p><h2>Chargement…</h2></div></section></div></main>}>
      <ResetPasswordForm />
    </Suspense>
  );
}

function ResetPasswordForm() {
  const searchParams = useSearchParams();
  const [matricule, setMatricule] = useState(searchParams.get("matricule") || "");
  const [ancienMotDePasse, setAncienMotDePasse] = useState("");
  const [nouveauMotDePasse, setNouveauMotDePasse] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [message, setMessage] = useState({ text: "", type: "" });
  const [loading, setLoading] = useState(false);
  const [visiblePasswords, setVisiblePasswords] = useState({
    ancienMotDePasse: false,
    nouveauMotDePasse: false,
    confirmation: false,
  });

  const togglePassword = (field) => {
    setVisiblePasswords((previous) => ({
      ...previous,
      [field]: !previous[field],
    }));
  };


  const handleSubmit = async (event) => {
    event.preventDefault();
    const normalizedMatricule = matricule.trim();

    if (!normalizedMatricule || !ancienMotDePasse || !nouveauMotDePasse || !confirmation) {
      setMessage({ text: "Veuillez remplir tous les champs.", type: "error" });
      return;
    }
    if (nouveauMotDePasse.length < 6) {
      setMessage({ text: "Le nouveau mot de passe doit comporter au moins 6 caractères.", type: "error" });
      return;
    }
    if (nouveauMotDePasse !== confirmation) {
      setMessage({ text: "La confirmation ne correspond pas au nouveau mot de passe.", type: "error" });
      return;
    }
    if (nouveauMotDePasse === ancienMotDePasse) {
      setMessage({ text: "Choisissez un mot de passe différent du mot de passe temporaire.", type: "error" });
      return;
    }

    setLoading(true);
    setMessage({ text: "", type: "" });
    try {
      const response = await fetch(`${API_URL}/auth/complete-password-reset`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          matricule: normalizedMatricule,
          ancienMotDePasse,
          nouveauMotDePasse,
          confirmation,
        }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(data.message || "Impossible de modifier le mot de passe. Vérifiez les informations saisies.");
      }
      setMessage({ text: data.message || "Mot de passe modifié avec succès.", type: "success" });
      setAncienMotDePasse("");
      setNouveauMotDePasse("");
      setConfirmation("");
    } catch (error) {
      setMessage({ text: error.message || "Impossible de contacter le serveur. Veuillez réessayer.", type: "error" });
    } finally {
      setLoading(false);
    }
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
            <p>Remplacez le mot de passe temporaire reçu par e-mail par un mot de passe personnel.</p>
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
            <span>SPAT · Madagascar</span><span className="login-footer-dot" /><span>Portail interne du personnel</span>
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
              <p className="login-overline">CHANGEMENT DE MOT DE PASSE</p>
              <h2>Définissez votre mot de passe</h2>
              <p>Saisissez votre matricule et le mot de passe temporaire reçu, puis choisissez et confirmez votre nouveau mot de passe.</p>
            </div>

            {message.text && (
              <div role={message.type === "error" ? "alert" : "status"} style={{
                display: "flex", alignItems: "flex-start", gap: "10px", marginBottom: "22px",
                padding: "13px 14px", borderRadius: "11px",
                border: message.type === "success" ? "1px solid #a7f3d0" : "1px solid #fecdd3",
                background: message.type === "success" ? "#ecfdf5" : "#fff1f2",
                color: message.type === "success" ? "#047857" : "#a51d37",
                fontSize: "12px", lineHeight: 1.6,
              }}>
                {message.type === "success" ? <FiCheckCircle size={18} /> : <FiAlertCircle size={18} />}
                <span>{message.text}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="login-form">
              <div className="login-field">
                <label htmlFor="matricule">Matricule professionnel</label>
                <div className="login-input-wrap">
                  <FiUser className="login-input-icon" aria-hidden="true" />
                  <input id="matricule" type="text" placeholder="Ex. SPAT-001" value={matricule}
                    onChange={(event) => setMatricule(event.target.value)} autoComplete="username" required disabled={loading} />
                </div>
              </div>

              <div className="login-field">
                <label htmlFor="ancienMotDePasse">Mot de passe temporaire (ancien mot de passe)</label>
                <div className="login-input-wrap" style={{ position: "relative" }}>
                  <FiKey className="login-input-icon" aria-hidden="true" />
                  <input id="ancienMotDePasse" type={visiblePasswords.ancienMotDePasse ? "text" : "password"} placeholder="Mot de passe reçu par e-mail"
                    value={ancienMotDePasse} onChange={(event) => setAncienMotDePasse(event.target.value)}
                    autoComplete="current-password" required disabled={loading} style={{ paddingRight: "48px" }} />
                  <button
                    type="button"
                    aria-label={visiblePasswords.ancienMotDePasse ? "Masquer le mot de passe" : "Afficher le mot de passe"}
                    aria-pressed={visiblePasswords.ancienMotDePasse}
                    onClick={() => togglePassword("ancienMotDePasse")}
                    disabled={loading}
                    style={{
                      position: "absolute",
                      right: "12px",
                      top: "50%",
                      transform: "translateY(-50%)",
                      background: "transparent",
                      border: "none",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      width: "24px",
                      height: "24px",
                      padding: 0,
                      color: "#64748b",
                      cursor: loading ? "not-allowed" : "pointer",
                      zIndex: 2,
                    }}
                  >
                    {visiblePasswords.ancienMotDePasse ? <FiEyeOff size={18} /> : <FiEye size={18} />}
                  </button>
                </div>
              </div>

              <div className="login-field">
                <label htmlFor="nouveauMotDePasse">Nouveau mot de passe</label>
                <div className="login-input-wrap" style={{ position: "relative" }}>
                  <FiLock className="login-input-icon" aria-hidden="true" />
                  <input id="nouveauMotDePasse" type={visiblePasswords.nouveauMotDePasse ? "text" : "password"} placeholder="6 caractères minimum"
                    value={nouveauMotDePasse} onChange={(event) => setNouveauMotDePasse(event.target.value)}
                    autoComplete="new-password" minLength={6} required disabled={loading} style={{ paddingRight: "48px" }} />
                  <button
                    type="button"
                    aria-label={visiblePasswords.nouveauMotDePasse ? "Masquer le mot de passe" : "Afficher le mot de passe"}
                    aria-pressed={visiblePasswords.nouveauMotDePasse}
                    onClick={() => togglePassword("nouveauMotDePasse")}
                    disabled={loading}
                    style={{
                      position: "absolute",
                      right: "12px",
                      top: "50%",
                      transform: "translateY(-50%)",
                      background: "transparent",
                      border: "none",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      width: "24px",
                      height: "24px",
                      padding: 0,
                      color: "#64748b",
                      cursor: loading ? "not-allowed" : "pointer",
                      zIndex: 2,
                    }}
                  >
                    {visiblePasswords.nouveauMotDePasse ? <FiEyeOff size={18} /> : <FiEye size={18} />}
                  </button>
                </div>
              </div>

              <div className="login-field">
                <label htmlFor="confirmation">Confirmer le nouveau mot de passe</label>
                <div className="login-input-wrap" style={{ position: "relative" }}>
                  <FiLock className="login-input-icon" aria-hidden="true" />
                  <input id="confirmation" type={visiblePasswords.confirmation ? "text" : "password"} placeholder="Saisissez à nouveau le nouveau mot de passe"
                    value={confirmation} onChange={(event) => setConfirmation(event.target.value)}
                    autoComplete="new-password" minLength={6} required disabled={loading} style={{ paddingRight: "48px" }} />
                  <button
                    type="button"
                    aria-label={visiblePasswords.confirmation ? "Masquer le mot de passe" : "Afficher le mot de passe"}
                    aria-pressed={visiblePasswords.confirmation}
                    onClick={() => togglePassword("confirmation")}
                    disabled={loading}
                    style={{
                      position: "absolute",
                      right: "12px",
                      top: "50%",
                      transform: "translateY(-50%)",
                      background: "transparent",
                      border: "none",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      width: "24px",
                      height: "24px",
                      padding: 0,
                      color: "#64748b",
                      cursor: loading ? "not-allowed" : "pointer",
                      zIndex: 2,
                    }}
                  >
                    {visiblePasswords.confirmation ? <FiEyeOff size={18} /> : <FiEye size={18} />}
                  </button>
                </div>
              </div>

              <button type="submit" className="login-submit" disabled={loading}>
                <span>{loading ? "Modification en cours..." : "Changer mon mot de passe"}</span>
                <span className="login-submit-icon">{loading ? <FiKey /> : <FiArrowRight />}</span>
              </button>
            </form>

            <div className="login-security-note">
              <FiLock aria-hidden="true" />
              <span>Le mot de passe temporaire ne sera plus utilisable après la modification.</span>
            </div>

            <div className="login-forgot-row" style={{ justifyContent: "flex-start", marginTop: "24px" }}>
              <Link href="/" style={{ display: "inline-flex", alignItems: "center", gap: "8px" }}>
                <FiArrowLeft aria-hidden="true" /> Retour à la connexion
              </Link>
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
