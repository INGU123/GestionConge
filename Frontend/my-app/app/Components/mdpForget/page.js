"use client";

import { API_BASE_URL } from "@/lib/apiClient";

import { useState } from "react";
import Link from "next/link";
import Image from "next/image";
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

const API_URL = API_BASE_URL;

export default function ForgotPassword() {
  const [matricule, setMatricule] = useState("");
  const [message, setMessage] = useState({ text: "", type: "" });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const value = matricule.trim();

    if (!/^[0-9]+$/.test(value)) {
      setMessage({ text: "Le matricule doit contenir uniquement des chiffres (ex. 2026100).", type: "error" });
      return;
    }

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
        setMessage({
          text: data.message || "La demande n'a pas pu être envoyée. Vérifiez votre matricule.",
          type: "error",
        });
      }
    } catch {
      setMessage({
        text: "Impossible de contacter le serveur. Veuillez réessayer plus tard.",
        type: "error",
      });
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
            <span className="login-eyebrow"><span /> ASSISTANCE AU PERSONNEL</span>
            <h1>Retrouvez votre accès, <em>en toute sécurité.</em></h1>
            <p>
              Une difficulté pour vous connecter ? Envoyez une demande à
              l’administrateur à l’aide de votre matricule professionnel.
            </p>
          </div>

          <div className="login-brand-visual" aria-hidden="true">
            <div className="login-orbit login-orbit-one" />
            <div className="login-orbit login-orbit-two" />
            <div className="login-visual-card">
              <div className="login-visual-icon"><FiShield /></div>
              <div>
                <strong>Une procédure sécurisée</strong>
                <span>Votre demande est traitée par l’administration</span>
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
            <span className="login-secure-label"><FiShield /> RÉCUPÉRATION SÉCURISÉE</span>
            <span className="login-language">FR <span>⌄</span></span>
          </div>

          <div className="login-form-content">
            <div className="login-form-heading">
              <span className="login-heading-mark" />
              <p className="login-overline">RÉCUPÉRATION DU COMPTE</p>
              <h2>Mot de passe oublié ?</h2>
              <p>
                Saisissez votre matricule professionnel. Votre demande sera transmise à l&apos;administration pour validation.
              </p>
            </div>

            {message.text && (
              <div
                role={message.type === "error" ? "alert" : "status"}
                style={{
                  display: "flex",
                  alignItems: "flex-start",
                  gap: "10px",
                  marginBottom: "22px",
                  padding: "13px 14px",
                  borderRadius: "11px",
                  border: message.type === "success" ? "1px solid #a7f3d0" : "1px solid #fecdd3",
                  background: message.type === "success" ? "#ecfdf5" : "#fff1f2",
                  color: message.type === "success" ? "#047857" : "#a51d37",
                  fontSize: "12px",
                  lineHeight: 1.6,
                }}
              >
                {message.type === "success" ? <FiCheckCircle size={18} /> : <FiAlertCircle size={18} />}
                <span>{message.text}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="login-form">
              <div className="login-field">
                <label htmlFor="matricule">Matricule professionnel</label>
                <div className="login-input-wrap">
                  <FiUser className="login-input-icon" aria-hidden="true" />
                  <input id="matricule" type="text" inputMode="numeric" pattern="[0-9]+" placeholder="Ex. 2026100"
                    value={matricule} onChange={(e) => setMatricule(e.target.value.replace(/[^0-9]/g, ""))}
                    autoComplete="username" required disabled={loading} />
                </div>
              </div>

              <button type="submit" className="login-submit" disabled={loading}>
                <span>{loading ? "Envoi en cours..." : "Envoyer ma demande"}</span>
                <span className="login-submit-icon">
                  {loading ? <FiKey /> : <FiArrowRight />}
                </span>
              </button>
            </form>

            <div className="login-security-note">
              <FiLock aria-hidden="true" />
              <span>La demande nécessite une validation administrative. Si elle est acceptée, un lien personnel valable 24 heures sera envoyé à l&apos;adresse enregistrée sur votre compte.</span>
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
