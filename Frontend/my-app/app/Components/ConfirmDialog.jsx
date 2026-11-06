"use client";

import { useEffect, useState } from "react";
import { AlertTriangle } from "lucide-react";

export default function ConfirmDialog({
  title,
  message,
  confirmLabel = "Confirmer la suppression",
  pending = false,
  countdownSeconds = 0,
  variant = "modal",
  onCancel,
  onConfirm,
}) {
  const [remaining, setRemaining] = useState(countdownSeconds);
  const isToast = variant === "toast";

  useEffect(() => {
    if (countdownSeconds <= 0) return undefined;

    const timer = window.setInterval(() => {
      setRemaining((value) => Math.max(0, value - 1));
    }, 1000);

    return () => window.clearInterval(timer);
  }, [countdownSeconds]);

  const waiting = remaining > 0;

  return (
    <div
      className={isToast
        ? "fixed right-4 top-4 z-[100] w-[calc(100%-2rem)] max-w-md"
        : "fixed inset-0 z-[100] flex items-center justify-center bg-slate-950/55 p-4"}
      onKeyDown={(event) => {
        if (event.key === "Escape" && !pending) onCancel();
      }}
    >
      <section
        role="alertdialog"
        aria-modal={!isToast}
        aria-labelledby="confirm-dialog-title"
        aria-describedby="confirm-dialog-message"
        className={isToast
          ? "overflow-hidden rounded-2xl border bg-white p-5 text-slate-800 shadow-2xl"
          : "w-full max-w-md rounded-2xl border p-6 shadow-2xl"}
        style={isToast
          ? { borderColor: "var(--border)" }
          : { background: "var(--panel)", color: "var(--text-main)", borderColor: "var(--border)" }}
      >
        <div className="flex items-start gap-3">
          <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-rose-50 text-rose-700">
            <AlertTriangle className="h-5 w-5" aria-hidden="true" />
          </span>
          <div className="min-w-0 flex-1">
            <h2 id="confirm-dialog-title" className="text-base font-bold">
              {title}
            </h2>
            <p id="confirm-dialog-message" className="mt-2 text-sm text-slate-600">
              {message}
            </p>
            {waiting && (
              <p className="mt-3 text-sm font-semibold text-rose-700" aria-live="polite">
                Confirmation disponible dans {remaining} seconde{remaining > 1 ? "s" : ""}.
              </p>
            )}
          </div>
        </div>

        {countdownSeconds > 0 && (
          <div className="mt-4 h-1.5 overflow-hidden rounded-full bg-rose-100">
            <div
              className="h-full rounded-full bg-rose-600 transition-[width] duration-1000"
              style={{ width: `${((countdownSeconds - remaining) / countdownSeconds) * 100}%` }}
            />
          </div>
        )}

        <div className="mt-5 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={onCancel}
            disabled={pending}
            className="rounded-lg border px-4 py-2 text-sm font-semibold transition hover:bg-slate-100 disabled:opacity-50"
            style={{ borderColor: "var(--border)" }}
          >
            Annuler
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={pending || waiting}
            className="inline-flex items-center justify-center gap-2 rounded-lg bg-rose-700 px-4 py-2 text-sm font-semibold text-white transition hover:bg-rose-800 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {pending && <span className="loading loading-spinner loading-xs" />}
            {pending ? "Suppression..." : waiting ? `Patientez ${remaining}s` : confirmLabel}
          </button>
        </div>
      </section>
    </div>
  );
}
