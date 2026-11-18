"use client";

import { useState } from "react";
import Link from "next/link";
import {
  FiArrowLeft,
  FiArrowRight,
  FiCheckCircle,
  FiKey,
  FiLock,
  FiShield,
  FiUser,
  FiAlertCircle,
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

export default function ForgotPassword() {
  const [mode, setMode] = useState("matricule");
  const [matricule, setMatricule] = useState("");
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState({ text: "", type: "" });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const value = mode === "matricule" ? matricule.trim() : email.trim();

    if (mode === "matricule" && !/^[0-9]+$/.test(value)) {
      setMessage({ text: "Le matricule doit contenir uniquement des chiffres (ex. 2026100).", type: "error" });
      return;
    }
    if (!value) {
      setMessage({ text: mode === "matricule" ? "Veuillez saisir votre numéro matricule." : "Veuillez saisir votre adresse e-mail.", type: "error" });
      return;
    }

    setLoading(true);
    setMessage({ text: "", type: "" });
    try {
      const res = await fetch(`${API_URL}/auth/forgot-password${mode === "email" ? "/email" : ""}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(mode === "matricule" ? { matricule: value } : { email: value }),
      });
      const data = await res.json().catch(() => ({}));
      if (res.ok) {
        setMessage({
          text: data.message || "Votre demande a été transmise à l'administrateur. Vous recevrez un e-mail après validation.",
          type: "success",
        });
        setMatricule("");
        setEmail("");
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
          <div className="spat-auth-brand-footer">
            <span>SOCIÉTÉ DU PORT</span><span className="spat-auth-footer-separator">|</span><span>TOAMASINA</span>
          </div>
          <CloudSideDivider />
        </div>

        <div className="spat-auth-form-panel">
          <div className="spat-auth-form-content">
            <div className="spat-auth-mobile-logo"><img src="/SpatLogo.png" alt="Logo SPAT" /></div>
            <h2>Mot de passe oublié ?</h2>
            <p className="spat-auth-intro">Choisissez une récupération par matricule avec validation administrative ou par e-mail.</p>

            {message.text && (
              <div role={message.type === "error" ? "alert" : "status"} style={{
                display: "flex", alignItems: "flex-start", gap: 10, marginBottom: 20,
                padding: "11px 12px", borderRadius: 7,
                border: message.type === "success" ? "1px solid #a7f3d0" : "1px solid #fecdd3",
                background: message.type === "success" ? "#ecfdf5" : "#fff1f2",
                color: message.type === "success" ? "#047857" : "#a51d37",
                fontSize: 12, lineHeight: 1.6,
              }}>
                {message.type === "success" ? <FiCheckCircle size={18} /> : <FiAlertCircle size={18} />}
                <span>{message.text}</span>
              </div>
            )}

            <div style={{ display: "flex", gap: 8, padding: 4, marginBottom: 22, borderRadius: 9, background: "#f1f6fc", border: "1px solid #dce8f5" }}>
              {["matricule", "email"].map((item) => (
                <button key={item} type="button" onClick={() => { setMode(item); setMessage({ text: "", type: "" }); }}
                  aria-pressed={mode === item}
                  style={{
                    flex: 1, minHeight: 36, border: 0, borderRadius: 7, cursor: "pointer",
                    background: mode === item ? "linear-gradient(90deg,#0875cf,#0754a5)" : "transparent",
                    color: mode === item ? "#fff" : "#526b89", fontSize: 12, fontWeight: 700,
                  }}>
                  {item === "matricule" ? "Par matricule" : "Par e-mail"}
                </button>
              ))}
            </div>

            <form onSubmit={handleSubmit}>
              <div className="spat-auth-field">
                <label htmlFor={mode === "matricule" ? "matricule" : "email"}>
                  {mode === "matricule" ? "Matricule professionnel" : "Adresse e-mail enregistrée"}
                </label>
                <div className="spat-auth-input-icon">
                  <FiUser aria-hidden="true" />
                  <input
                    id={mode === "matricule" ? "matricule" : "email"}
                    type={mode === "matricule" ? "text" : "email"}
                    inputMode={mode === "matricule" ? "numeric" : undefined}
                    pattern={mode === "matricule" ? "[0-9]+" : undefined}
                    placeholder={mode === "matricule" ? "Entrez votre matricule" : "nom@exemple.mg"}
                    value={mode === "matricule" ? matricule : email}
                    onChange={(e) => mode === "matricule" ? setMatricule(e.target.value.replace(/[^0-9]/g, "")) : setEmail(e.target.value)}
                    autoComplete={mode === "matricule" ? "username" : "email"}
                    required disabled={loading}
                  />
                </div>
              </div>

              <button type="submit" className="spat-auth-submit" disabled={loading}>
                {loading ? "Envoi en cours..." : mode === "matricule" ? "Envoyer ma demande" : "Recevoir mes informations"}
                <span style={{ marginLeft: 8, display: "inline-flex", verticalAlign: "middle" }}>{loading ? <FiKey /> : <FiArrowRight />}</span>
              </button>
            </form>

            <p className="spat-auth-security" style={{ lineHeight: 1.6 }}>
              <FiLock style={{ display: "inline", marginRight: 6, verticalAlign: "middle" }} />
              {mode === "matricule"
                ? "La demande nécessite une validation administrative. La transmission par SMS sera ajoutée ultérieurement."
                : "Les informations de connexion et le lien de changement seront envoyés à votre adresse enregistrée."}
            </p>

            <div className="spat-auth-forgot" style={{ justifyContent: "center", marginTop: 22, marginBottom: 0 }}>
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
