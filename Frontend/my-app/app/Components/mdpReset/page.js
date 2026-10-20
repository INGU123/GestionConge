"use client";

import { Suspense } from "react";
import Link from "next/link";
import Image from "next/image";
import {
  FiArrowLeft,
  FiLock,
  FiShield,
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
            <h1>Retrouvez votre accès, <em>en toute sécurité.</em></h1>
            <p>
              La réinitialisation est traitée par l’administration à partir de votre
              matricule professionnel.
            </p>
          </div>

          <div className="login-brand-visual" aria-hidden="true">
            <div className="login-orbit login-orbit-one" />
            <div className="login-orbit login-orbit-two" />
            <div className="login-visual-card">
              <div className="login-visual-icon"><FiShield /></div>
              <div>
                <strong>Une procédure sécurisée</strong>
                <span>Validation par l’administration SPAT</span>
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
              <p className="login-overline">RÉINITIALISATION DU COMPTE</p>
              <h2>Une demande validée par l’administration</h2>
              <p>
                Pour récupérer votre compte, utilisez le formulaire « Mot de passe oublié »
                et saisissez votre matricule. Après validation par l’administrateur, un
                mot de passe temporaire sera envoyé à l’adresse e-mail enregistrée.
              </p>
            </div>

            <div className="login-security-note">
              <FiLock aria-hidden="true" />
              <span>Connectez-vous avec votre matricule et le mot de passe temporaire reçu, puis changez votre mot de passe depuis votre espace personnel.</span>
            </div>

            <div className="login-forgot-row" style={{ justifyContent: "flex-start", marginTop: "24px" }}>
              <Link href="/" style={{ display: "inline-flex", alignItems: "center", gap: "8px" }}>
                <FiArrowLeft aria-hidden="true" /> Retour à la connexion
              </Link>
              <Link href="/Components/mdpForget" style={{ marginLeft: "auto" }}>
                Faire une demande
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
