"use client";

import { Suspense, useState } from "react";
import Link from "next/link";
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

function CloudSideDivider() {
  return (
    <div className="spat-auth-cloud-divider" aria-hidden="true">
      <svg className="spat-cloud-svg-desktop" viewBox="0 0 120 800" preserveAspectRatio="none">
        <path d="M68,0 C95,45 92,95 62,135 C40,165 45,210 74,240 C104,272 102,325 68,365 C38,400 42,455 75,490 C106,522 102,575 66,615 C38,645 45,700 76,735 C96,758 95,782 82,800 L120,800 L120,0 Z" fill="#9ec5f8" opacity="0.55" />
        <path d="M82,0 C112,42 108,92 78,132 C52,166 58,212 88,242 C118,274 114,328 80,368 C50,404 56,458 88,492 C118,524 112,578 78,618 C50,650 58,702 88,736 C108,758 106,782 94,800 L120,800 L120,0 Z" fill="#ffffff" />
      </svg>
      <svg className="spat-cloud-svg-mobile" viewBox="0 0 800 90" preserveAspectRatio="none">
        <path d="M0,45 C50,15 110,15 160,42 C210,70 270,68 320,38 C375,8 435,10 490,40 C545,70 610,68 665,38 C715,12 760,15 800,38 L800,90 L0,90 Z" fill="#9ec5f8" opacity="0.55" />
        <path d="M0,58 C55,28 115,28 165,54 C215,80 275,78 325,48 C380,18 440,20 495,50 C550,80 615,78 670,48 C720,22 762,25 800,48 L800,90 L0,90 Z" fill="#ffffff" />
      </svg>
    </div>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={
      <main className="spat-auth-page">
        <section className="spat-auth-card">
          <div className="spat-auth-brand">
            <div className="spat-auth-welcome">Bienvenue sur</div>
            <div className="spat-auth-brand-center">
              <div className="spat-auth-logo"><img src="/SpatLogo.png" alt="Logo SPAT" /></div>
              <h1>SPAT</h1><p className="spat-auth-brand-subtitle">Gestion des congés</p>
            </div>
            <div className="spat-auth-brand-footer"><span>SOCIÉTÉ DU PORT</span><span>|</span><span>TOAMASINA</span></div>
            <CloudSideDivider />
          </div>
          <div className="spat-auth-form-panel"><div className="spat-auth-form-content"><h2>Chargement…</h2></div></div>
        </section>
      </main>
    }>
      <ResetPasswordForm />
    </Suspense>
  );
}

function ResetPasswordForm() {
  const searchParams = useSearchParams();
  const initialToken = searchParams.get("token") || "";
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
    setVisiblePasswords((previous) => ({ ...previous, [field]: !previous[field] }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const normalizedMatricule = matricule.trim();

    if (!normalizedMatricule || !/^[0-9]+$/.test(normalizedMatricule) || !ancienMotDePasse || !nouveauMotDePasse || !confirmation) {
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
    if (!initialToken && nouveauMotDePasse === ancienMotDePasse) {
      setMessage({ text: "Choisissez un mot de passe différent du mot de passe temporaire.", type: "error" });
      return;
    }

    setLoading(true);
    setMessage({ text: "", type: "" });
    try {
      const endpoint = initialToken ? `${API_URL}/auth/initial-password` : `${API_URL}/auth/complete-password-reset`;
      const payload = initialToken
        ? { token: initialToken, matricule: normalizedMatricule, ancienMotDePasse, newPassword: nouveauMotDePasse, confirmation }
        : { matricule: normalizedMatricule, ancienMotDePasse, nouveauMotDePasse, confirmation };
      const response = await fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || "Impossible de modifier le mot de passe. Vérifiez les informations saisies.");
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

  const passwordField = (id, label, placeholder, value, setter, visibleKey, Icon) => (
    <div className="spat-auth-field">
      <label htmlFor={id}>{label}</label>
      <div className="spat-auth-input-icon">
        <Icon aria-hidden="true" />
        <input
          id={id}
          type={visiblePasswords[visibleKey] ? "text" : "password"}
          placeholder={placeholder}
          value={value}
          onChange={(event) => setter(event.target.value)}
          autoComplete={visibleKey === "ancienMotDePasse" ? "current-password" : "new-password"}
          minLength={visibleKey === "ancienMotDePasse" ? undefined : 6}
          required
          disabled={loading}
        />
        <button
          type="button"
          className="spat-auth-eye"
          aria-label={visiblePasswords[visibleKey] ? "Masquer le mot de passe" : "Afficher le mot de passe"}
          aria-pressed={visiblePasswords[visibleKey]}
          onClick={() => togglePassword(visibleKey)}
          disabled={loading}
        >
          {visiblePasswords[visibleKey] ? <FiEyeOff /> : <FiEye />}
        </button>
      </div>
    </div>
  );

  return (
    <main className="spat-auth-page">
      <section className="spat-auth-card">
        <div className="spat-auth-brand">
          <div className="spat-auth-welcome">Bienvenue sur</div>
          <div className="spat-auth-brand-center">
            <div className="spat-auth-logo"><img src="/SpatLogo.png" alt="Logo SPAT" /></div>
            <h1>SPAT</h1>
            <p className="spat-auth-brand-subtitle">Gestion des congés</p>
            <p className="spat-auth-description">Société du Port à Gestion Autonome de Toamasina.<br />Une gestion simple et efficace des demandes de congés.</p>
          </div>
          <div className="spat-auth-brand-footer"><span>SOCIÉTÉ DU PORT</span><span className="spat-auth-footer-separator">|</span><span>TOAMASINA</span></div>
          <CloudSideDivider />
        </div>

        <div className="spat-auth-form-panel">
          <div className="spat-auth-form-content">
            <div className="spat-auth-mobile-logo"><img src="/SpatLogo.png" alt="Logo SPAT" /></div>
            <h2>Définissez votre mot de passe</h2>
            <p className="spat-auth-intro">
              {initialToken
                ? "Votre compte a reçu un mot de passe généré automatiquement. Définissez maintenant votre mot de passe personnel."
                : "Saisissez votre matricule et le mot de passe temporaire reçu, puis choisissez et confirmez votre nouveau mot de passe."}
            </p>

            {message.text && (
              <div role={message.type === "error" ? "alert" : "status"} style={{
                display: "flex", alignItems: "flex-start", gap: 10, marginBottom: 18,
                padding: "10px 12px", borderRadius: 7,
                border: message.type === "success" ? "1px solid #a7f3d0" : "1px solid #fecdd3",
                background: message.type === "success" ? "#ecfdf5" : "#fff1f2",
                color: message.type === "success" ? "#047857" : "#a51d37",
                fontSize: 12, lineHeight: 1.5,
              }}>
                {message.type === "success" ? <FiCheckCircle size={18} /> : <FiAlertCircle size={18} />}
                <span>{message.text}</span>
              </div>
            )}

            <form onSubmit={handleSubmit}>
              <div className="spat-auth-field">
                <label htmlFor="matricule">Matricule professionnel</label>
                <div className="spat-auth-input-icon">
                  <FiUser aria-hidden="true" />
                  <input
                    id="matricule" type="text" inputMode="numeric" pattern="[0-9]+"
                    placeholder="Entrez votre matricule" value={matricule}
                    onChange={(event) => setMatricule(event.target.value.replace(/[^0-9]/g, ""))}
                    autoComplete="username" required disabled={loading}
                  />
                </div>
              </div>

              {passwordField("ancienMotDePasse", "Ancien mot de passe (mot de passe temporaire)", "Saisissez le mot de passe automatique reçu", ancienMotDePasse, setAncienMotDePasse, "ancienMotDePasse", FiKey)}
              {passwordField("nouveauMotDePasse", "Nouveau mot de passe", "6 caractères minimum", nouveauMotDePasse, setNouveauMotDePasse, "nouveauMotDePasse", FiLock)}
              {passwordField("confirmation", "Confirmer le nouveau mot de passe", "Saisissez à nouveau le nouveau mot de passe", confirmation, setConfirmation, "confirmation", FiLock)}

              <button type="submit" className="spat-auth-submit" disabled={loading}>
                {loading ? "Modification en cours..." : "Changer mon mot de passe"}
                <span style={{ marginLeft: 8, display: "inline-flex", verticalAlign: "middle" }}>{loading ? <FiKey /> : <FiArrowRight />}</span>
              </button>
            </form>

            <p className="spat-auth-security" style={{ lineHeight: 1.5 }}>
              <FiShield style={{ display: "inline", marginRight: 6, verticalAlign: "middle" }} />
              Le matricule et le mot de passe temporaire vérifient votre identité. Le lien sécurisé est personnel et temporaire.
            </p>

            <div className="spat-auth-forgot" style={{ justifyContent: "center", marginTop: 18, marginBottom: 0 }}>
              <Link href="/" style={{ display: "inline-flex", alignItems: "center", gap: 7 }}>
                <FiArrowLeft aria-hidden="true" /> Retour à la connexion
              </Link>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
