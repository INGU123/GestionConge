"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from "react";
import { AlertCircle, CheckCircle2, Info, TriangleAlert, X } from "lucide-react";

const ToastContext = createContext(null);

const toastStyles = {
  success: {
    icon: CheckCircle2,
    color: "#0f766e",
    label: "Succès",
  },
  error: {
    icon: AlertCircle,
    color: "#b91c1c",
    label: "Erreur",
  },
  warning: {
    icon: TriangleAlert,
    color: "#b45309",
    label: "Attention",
  },
  info: {
    icon: Info,
    color: "#2563eb",
    label: "Information",
  },
};

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const timers = useRef(new Map());

  const dismissToast = useCallback((id) => {
    const timer = timers.current.get(id);
    if (timer) clearTimeout(timer);
    timers.current.delete(id);
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const showToast = useCallback(
    (message, type = "info") => {
      if (!message) return;
      const id = `${Date.now()}-${Math.random().toString(36).slice(2)}`;
      const toast = {
        id,
        message: String(message),
        type: toastStyles[type] ? type : "info",
      };
      setToasts((current) => [...current, toast]);
      timers.current.set(id, setTimeout(() => dismissToast(id), 4500));
    },
    [dismissToast],
  );

  useEffect(
    () => () => {
      timers.current.forEach((timer) => clearTimeout(timer));
      timers.current.clear();
    },
    [],
  );

  return (
    <ToastContext.Provider value={showToast}>
      {children}
      <div
        className="fixed top-4 right-4 z-[110] flex w-[calc(100%-2rem)] max-w-sm flex-col gap-2 sm:w-full"
        aria-live="polite"
        aria-relevant="additions"
      >
        {toasts.map((toast) => {
          const style = toastStyles[toast.type];
          const Icon = style.icon;
          return (
            <div
              key={toast.id}
              role={toast.type === "error" ? "alert" : "status"}
              className="flex items-start gap-3 rounded-xl border p-4 shadow-xl"
              style={{
                background: "var(--panel)",
                color: "var(--text-main)",
                borderColor: "var(--border)",
              }}
            >
              <Icon
                className="mt-0.5 h-5 w-5 shrink-0"
                style={{ color: style.color }}
                aria-hidden="true"
              />
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold">{style.label}</p>
                <p className="mt-0.5 break-words text-sm text-slate-600">{toast.message}</p>
              </div>
              <button
                type="button"
                onClick={() => dismissToast(toast.id)}
                className="shrink-0 rounded-md p-1 text-slate-500 transition hover:bg-slate-100 hover:text-slate-800"
                aria-label="Fermer la notification"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const showToast = useContext(ToastContext);
  if (!showToast) {
    throw new Error("useToast doit être utilisé dans un ToastProvider.");
  }
  return showToast;
}
