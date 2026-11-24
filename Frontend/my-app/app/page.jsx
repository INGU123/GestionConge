"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { loginUser } from "@/lib/apiLogin.js";

function CloudSideDivider() {
  return (
    <div className="spat-auth-cloud-divider" aria-hidden="true">
    {/* Séparateur vertical nuageux (desktop) */}
    <svg
    className="spat-cloud-svg-desktop"
    viewBox="0 0 120 800"
    preserveAspectRatio="none"
    >
    {/* Couche arrière bleue claire pour l'effet de profondeur */}
    <path
    d="M68,0
    C95,45 92,95 62,135
    C40,165 45,210 74,240
    C104,272 102,325 68,365
    C38,400 42,455 75,490
    C106,522 102,575 66,615
    C38,645 45,700 76,735
    C96,758 95,782 82,800
    L120,800 L120,0 Z"
    fill="#9ec5f8"
    opacity="0.55"
    />
    {/* Couche avant blanche principale */}
    <path
    d="M82,0
    C112,42 108,92 78,132
    C52,166 58,212 88,242
    C118,274 114,328 80,368
    C50,404 56,458 88,492
    C118,524 112,578 78,618
    C50,650 58,702 88,736
    C108,758 106,782 94,800
    L120,800 L120,0 Z"
    fill="#ffffff"
    />
    </svg>

    {/* Séparateur horizontal nuageux (mobile) */}
    <svg
    className="spat-cloud-svg-mobile"
    viewBox="0 0 800 90"
    preserveAspectRatio="none"
    >
    <path
    d="M0,45
    C50,15 110,15 160,42
    C210,70 270,68 320,38
    C375,8 435,10 490,40
    C545,70 610,68 665,38
    C715,12 760,15 800,38
    L800,90 L0,90 Z"
    fill="#9ec5f8"
    opacity="0.55"
    />
    <path
    d="M0,58
    C55,28 115,28 165,54
    C215,80 275,78 325,48
    C380,18 440,20 495,50
    C550,80 615,78 670,48
    C720,22 762,25 800,48
    L800,90 L0,90 Z"
    fill="#ffffff"
    />
    </svg>
    </div>
  );
}

export default function LoginPage() {
  const router = useRouter();
  const [matricule, setMatricule] = useState("");
  const [motDePass, setMotDePass] = useState("");
  const [erreur, setErreur] = useState("");
  const [chargement, setChargement] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [theme, setTheme] = useState("day");

  useEffect(() => {
    const savedTheme = localStorage.getItem("dashboard-theme");
    if (savedTheme === "night") setTheme("night");
  }, []);

  const toggleTheme = () => {
    const nextTheme = theme === "night" ? "day" : "night";
    setTheme(nextTheme);
    localStorage.setItem("dashboard-theme", nextTheme);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErreur("");
    setChargement(true);

    try {
      const data = await loginUser(matricule, motDePass);

      localStorage.setItem("token", data.token);
      localStorage.setItem("user", JSON.stringify(data?.utilisateur || data));

      router.push("/dashboard");
    } catch (error) {
      setErreur(
        error?.response?.data?.message ||
        error?.message ||
        "Matricule ou mot de passe incorrect."
      );
    } finally {
      setChargement(false);
    }
  };

  return (
    <main className="spat-auth-page" data-auth-theme={theme}>
    <button type="button" className="spat-auth-theme-toggle" onClick={toggleTheme} aria-label={theme === "night" ? "Activer le mode clair" : "Activer le mode sombre"} title={theme === "night" ? "Mode clair" : "Mode sombre"}>
      {theme === "night" ? (
        <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="4" /><path d="M12 2v2m0 16v2M4.93 4.93l1.42 1.42m11.3 11.3 1.42 1.42M2 12h2m16 0h2M4.93 19.07l1.42-1.42m11.3-11.3 1.42-1.42" /></svg>
      ) : (
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20.5 15.5A8.5 8.5 0 0 1 8.5 3.5 8.5 8.5 0 1 0 20.5 15.5Z" /></svg>
      )}
      <span>{theme === "night" ? "Mode clair" : "Mode sombre"}</span>
    </button>
    <section className="spat-auth-card">
    <div className="spat-auth-brand">
    <div className="spat-auth-welcome">Bienvenue sur</div>

    <div className="spat-auth-brand-center">
    <div className="spat-auth-logo">
    <img src="/SpatLogo.png" alt="Logo SPAT" />
    </div>

    <h1>SPAT</h1>
    <p className="spat-auth-brand-subtitle">
    Gestion des congés
    </p>

    <p className="spat-auth-description">
    Société du Port à Gestion Autonome de Toamasina.
    <br />
    Une gestion simple et efficace des demandes de congés.
    </p>
    </div>

    <div className="spat-auth-brand-footer">
    <span>SOCIÉTÉ DU PORT</span>
    <span className="spat-auth-footer-separator">|</span>
    <span>TOAMASINA</span>
    </div>

    <CloudSideDivider />
    </div>

    <div className="spat-auth-form-panel">
    <div className="spat-auth-form-content">
    <div className="spat-auth-mobile-logo">
    <img src="/SpatLogo.png" alt="Logo SPAT" />
    </div>

    <h2>Authentification</h2>
    <p className="spat-auth-intro">
    Connectez-vous à votre espace de gestion des congés.
    </p>

    <form onSubmit={handleSubmit}>
    <div className="spat-auth-field">
    <label htmlFor="matricule">Matricule</label>
    <div className="spat-auth-input-icon">
    <svg viewBox="0 0 24 24" aria-hidden="true">
    <rect x="3" y="5" width="18" height="14" rx="2" />
    <circle cx="9" cy="11" r="2" />
    <path d="M6 16c.8-2 5.2-2 6 0M14 10h4M14 14h4" />
    </svg>
    <input
    id="matricule"
    type="text"
    inputMode="numeric"
    pattern="[0-9]+"
    placeholder="Entrez votre matricule"
    value={matricule}
    onChange={(e) => setMatricule(e.target.value.replace(/\D/g, ""))}
    autoComplete="username"
    required
    />
    </div>
    </div>

    <div className="spat-auth-field">
    <label htmlFor="motDePass">Mot de passe</label>
    <div className="spat-auth-input-icon">
    <svg viewBox="0 0 24 24" aria-hidden="true">
    <rect x="4" y="10" width="16" height="11" rx="2" />
    <path d="M8 10V7a4 4 0 0 1 8 0v3" />
    </svg>
    <input
    id="motDePass"
    type={showPassword ? "text" : "password"}
    placeholder="Entrez votre mot de passe"
    value={motDePass}
    onChange={(e) => setMotDePass(e.target.value)}
    autoComplete="current-password"
    required
    />
    <button
    type="button"
    className="spat-auth-eye"
    onClick={() => setShowPassword(!showPassword)}
    aria-label={showPassword ? "Masquer le mot de passe" : "Afficher le mot de passe"}
    title={showPassword ? "Masquer" : "Afficher"}
    >
    {showPassword ? (
      <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M3 3l18 18M10.6 10.6a2 2 0 0 0 2.8 2.8" />
      <path d="M9.9 5.2A10.8 10.8 0 0 1 12 5c5 0 8.5 4.5 9 7-.2 1.1-1 2.3-2.2 3.4M6.2 6.2C3.8 7.7 2.3 10 2 12c.5 2.5 4 7 10 7 1.1 0 2.1-.2 3-.5" />
      </svg>
    ) : (
      <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z" />
      <circle cx="12" cy="12" r="3" />
      </svg>
    )}
    </button>
    </div>
    </div>

    <div className="spat-auth-forgot">
    <Link href="/Components/mdpForget">
    Mot de passe oublié ?
    </Link>
    </div>

    {erreur && (
      <div className="spat-auth-error" role="alert">
      {erreur}
      </div>
    )}

    <button
    type="submit"
    className="spat-auth-submit"
    disabled={chargement}
    >
    {chargement ? "Connexion en cours..." : "Se connecter"}
    </button>
    </form>

    <p className="spat-auth-security">
    Accès réservé au personnel autorisé de la SPAT.
    </p>
    </div>
    </div>
    </section>
    
<style jsx global>{`

.spat-auth-theme-toggle {
  position: absolute; top: 22px; right: 26px; z-index: 20;
  display: inline-flex; align-items: center; justify-content: center; gap: 9px;
  min-height: 42px; padding: 0 15px; border: 1px solid rgba(7,74,135,.16);
  border-radius: 999px; background: rgba(255,255,255,.94); color: #164675;
  box-shadow: 0 5px 18px rgba(15,40,70,.10); font-size: 12px; font-weight: 650;
  cursor: pointer; transition: background .2s ease,color .2s ease,border-color .2s ease,transform .2s ease;
}
.spat-auth-theme-toggle:hover { transform: translateY(-1px); }
.spat-auth-theme-toggle svg { width: 17px; height: 17px; fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
.spat-auth-page[data-auth-theme="night"] { background: radial-gradient(ellipse at 15% 10%,#183957 0%,#0b1b2d 48%,#071321 100%); }
.spat-auth-page[data-auth-theme="night"] .spat-auth-card { background: #101f31; box-shadow: 0 22px 55px rgba(0,0,0,.42); border: 1px solid #263b52; }
.spat-auth-page[data-auth-theme="night"] .spat-auth-form-panel { background: #101f31; }
.spat-auth-page[data-auth-theme="night"] .spat-auth-form-content h2,
.spat-auth-page[data-auth-theme="night"] .spat-auth-field label { color: #edf4ff; }
.spat-auth-page[data-auth-theme="night"] .spat-auth-intro,
.spat-auth-page[data-auth-theme="night"] .spat-auth-security { color: #a9bdd5; }
.spat-auth-page[data-auth-theme="night"] .spat-auth-input-icon input { color: #edf4ff; border-bottom-color: #385574; }
.spat-auth-page[data-auth-theme="night"] .spat-auth-input-icon input::placeholder { color: #8198b2; }
.spat-auth-page[data-auth-theme="night"] .spat-auth-input-icon input:focus { border-bottom-color: #60a5fa; }
.spat-auth-page[data-auth-theme="night"] .spat-auth-eye,
.spat-auth-page[data-auth-theme="night"] .spat-auth-forgot a { color: #7dbbff; }
.spat-auth-page[data-auth-theme="night"] .spat-auth-error { background: #3b1d2a; color: #fecdd3; border: 1px solid #713047; }
.spat-auth-page[data-auth-theme="night"] .spat-auth-theme-toggle { background: #142941; border-color: #385574; color: #e5f1ff; box-shadow: 0 5px 18px rgba(0,0,0,.22); }
@media (max-width: 760px) { .spat-auth-theme-toggle { top: 12px; right: 12px; min-height: 38px; padding: 0 12px; font-size: 11px; } }

.spat-auth-page {
  min-height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 35px 22px;
  box-sizing: border-box;
  background: #d4d4d4;
  font-family: Arial, Helvetica, sans-serif;
}

.spat-auth-card {
  position: relative;
  display: grid;
  grid-template-columns: 46% 54%;
  width: min(1120px, 100%);
  min-height: 570px;
  overflow: hidden;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 18px 38px rgba(0,0,0,.22);
}

.spat-auth-brand {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  padding: 55px 55px 27px;
  color: white;
  text-align: center;
  background: linear-gradient(145deg,#07519f 0%,#0877d2 65%,#168de5 100%);
}

.spat-auth-welcome {
  width: 100%;
  font-size: 16px;
  font-weight: 500;
}

.spat-auth-brand-center {
  width: 100%;
  max-width: 340px;
  margin: auto 0;
  transform: translateY(-8px);
}

.spat-auth-logo {
  width: 105px;
  height: 105px;
  margin: 0 auto 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 50%;
  background: #fff;
}

.spat-auth-logo img {
  width: 85%;
  height: 85%;
  object-fit: contain;
}

.spat-auth-brand-center h1 {
  margin: 0;
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 1.5px;
}

.spat-auth-brand-subtitle {
  margin: 5px 0 28px;
  color: #fff;
  font-size: 15px;
}

.spat-auth-description {
  max-width: 330px;
  margin: 0 auto;
  color: rgba(255,255,255,.82);
  font-size: 11px;
  line-height: 1.8;
}

.spat-auth-brand-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  width: 100%;
  color: rgba(255,255,255,.8);
  font-size: 9px;
  letter-spacing: 1px;
}

.spat-auth-footer-separator {
  opacity: .7;
}

/* Séparation nuageuse verticale */
.spat-auth-cloud-divider {
  position: absolute;
  z-index: 5;
  top: 0;
  right: -1px;
  width: 82px;
  height: 100%;
  pointer-events: none;
}

.spat-cloud-svg-desktop {
  display: block;
  width: 100%;
  height: 100%;
  overflow: visible;
}

.spat-cloud-svg-mobile {
  display: none;
}

.spat-auth-form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 75px 55px 62px;
  background: #fff;
}

.spat-auth-form-content {
  width: 100%;
  max-width: 390px;
}

.spat-auth-form-content h2 {
  margin: 0 0 12px;
  color: #252525;
  font-size: 27px;
  font-weight: 600;
}

.spat-auth-intro {
  margin: 0 0 38px;
  color: #929292;
  font-size: 12px;
  line-height: 1.6;
}

.spat-auth-field {
  margin-bottom: 25px;
}

.spat-auth-field label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-size: 13px;
  font-weight: 500;
}

.spat-auth-input-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.spat-auth-input-icon > svg {
  position: absolute;
  left: 2px;
  width: 17px;
  height: 17px;
  fill: none;
  stroke: #1683c7;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
  pointer-events: none;
}

.spat-auth-input-icon input {
  display: block;
  width: 100%;
  height: 38px;
  padding: 5px 35px 5px 29px;
  box-sizing: border-box;
  border: 0;
  border-bottom: 1px solid #a8d8ed;
  border-radius: 0;
  outline: none;
  background: transparent;
  color: #222;
  font-size: 12px;
}

.spat-auth-input-icon input::placeholder {
  color: #b8b8b8;
  font-size: 11px;
}

.spat-auth-input-icon input:focus {
  border-bottom: 2px solid #0878cf;
}

.spat-auth-eye {
  position: absolute;
  right: 2px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 5px;
  border: 0;
  background: transparent;
  color: #1683c7;
  cursor: pointer;
}

.spat-auth-eye svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.spat-auth-forgot {
  display: flex;
  justify-content: flex-end;
  margin-top: -8px;
  margin-bottom: 28px;
}

.spat-auth-forgot a {
  color: #0876c9;
  font-size: 11px;
  text-decoration: none;
}

.spat-auth-forgot a:hover {
  text-decoration: underline;
}

.spat-auth-submit {
  display: block;
  width: 100%;
  min-height: 44px;
  border: 0;
  border-radius: 24px;
  background: linear-gradient(90deg,#0875cf,#0754a5);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 5px 12px rgba(0,91,170,.18);
  transition: transform .2s ease, box-shadow .2s ease;
}

.spat-auth-submit:hover {
  transform: translateY(-1px);
  box-shadow: 0 7px 16px rgba(0,91,170,.25);
}

.spat-auth-submit:disabled {
  opacity: .65;
  cursor: wait;
  transform: none;
}

.spat-auth-error {
  margin: -10px 0 18px;
  padding: 10px 12px;
  border-radius: 6px;
  background: #fff0f0;
  color: #bd2929;
  font-size: 12px;
}

.spat-auth-security {
  margin: 27px 0 0;
  color: #aaa;
  font-size: 10px;
  text-align: center;
}

.spat-auth-mobile-logo {
  display: none;
}

@media (max-width: 760px) {
  .spat-auth-page {
    min-height: 100vh;
    padding: 18px 13px;
    background: #f1f3f6;
  }

  .spat-auth-card {
    grid-template-columns: 1fr;
    max-width: 470px;
    min-height: auto;
    border-radius: 14px;
  }

  .spat-auth-brand {
    min-height: 235px;
    padding: 22px 25px 20px;
  }

  .spat-auth-welcome {
    font-size: 13px;
  }

  .spat-auth-brand-center {
    margin: 18px 0;
    transform: none;
  }

  .spat-auth-logo {
    width: 65px;
    height: 65px;
    margin-bottom: 7px;
  }

  .spat-auth-brand-center h1 {
    font-size: 21px;
  }

  .spat-auth-brand-subtitle {
    margin: 2px 0 8px;
    font-size: 12px;
  }

  .spat-auth-description {
    display: none;
  }

  .spat-auth-brand-footer {
    font-size: 8px;
  }

  .spat-auth-cloud-divider {
    top: auto;
    right: 0;
    bottom: -1px;
    left: 0;
    width: 100%;
    height: 42px;
  }

  .spat-cloud-svg-desktop {
    display: none;
  }

  .spat-cloud-svg-mobile {
    display: block;
    width: 100%;
    height: 100%;
  }

  .spat-auth-form-panel {
    padding: 35px 28px 32px;
  }

  .spat-auth-form-content h2 {
    font-size: 24px;
  }

  .spat-auth-intro {
    margin-bottom: 28px;
  }
}

@media (max-width: 380px) {
  .spat-auth-form-panel {
    padding-right: 21px;
    padding-left: 21px;
  }
}
`}</style>
</main>
  );
}
