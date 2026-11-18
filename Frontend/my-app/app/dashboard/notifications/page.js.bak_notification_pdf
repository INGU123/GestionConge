"use client";

import { useEffect, useRef, useState } from "react";
import {
  getNotificationNonLues,
  getToutesLesNotifications,
  marquerNotificationLue,
} from "../../api/notificationConge/notificationConge";
import { getCurrentUser } from "@/lib/apiClient";
import {
  Bell,
  Check,
  CheckCheck,
  CheckCircle2,
  XCircle,
  Info,
  X,
  Filter
} from "lucide-react";

export default function NotificationHeaderMenu() {
  const [user] = useState(getCurrentUser);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [filterNonLues, setFilterNonLues] = useState(true);
  const [isOpen, setIsOpen] = useState(false);
  const [showAllNotifications, setShowAllNotifications] = useState(false);
  const [markingAllRead, setMarkingAllRead] = useState(false);

  // State pour la notification Toast éphémère
  const [activeToast, setActiveToast] = useState(null);
  const knownNotificationIds = useRef(new Set());

  // Charger les notifications au montage puis les actualiser périodiquement pour recevoir les nouvelles alertes.
  useEffect(() => {
    if (!user?.id) return;

    let cancelled = false;
    let firstLoad = true;

    const loadNotifications = async () => {
      try {
        const data = filterNonLues
          ? await getNotificationNonLues(user.id)
          : await getToutesLesNotifications(user.id);
        if (cancelled) return;

        const notifsList = Array.isArray(data) ? data : [];
        const newUnread = notifsList.find(
          (notification) => !notification.lue && !knownNotificationIds.current.has(notification.id)
        );

        setNotifications(notifsList);
        if (firstLoad) {
          const derniereNonLue = notifsList.find((notification) => !notification.lue);
          if (derniereNonLue) setActiveToast(derniereNonLue);
        } else if (newUnread) {
          setActiveToast(newUnread);
        }

        knownNotificationIds.current = new Set(notifsList.map((notification) => notification.id));
        firstLoad = false;
      } catch (err) {
        console.error("Erreur chargement notifications:", err);
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    loadNotifications();
    const intervalId = setInterval(loadNotifications, 30000);

    return () => {
      cancelled = true;
      clearInterval(intervalId);
    };
  }, [user?.id, filterNonLues]);

  // Auto-fermeture du Toast après 5 secondes
  useEffect(() => {
    if (activeToast) {
      const timer = setTimeout(() => setActiveToast(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [activeToast]);

  const handleMarquerLue = async (notifId, e) => {
    if (e) e.stopPropagation();
    try {
      const updated = await marquerNotificationLue(notifId);
      if (!updated) return;
      setNotifications((prev) =>
        prev.map((n) => (n.id === notifId ? { ...n, lue: true } : n))
      );
      if (activeToast?.id === notifId) {
        setActiveToast(null);
      }
    } catch (err) {
      console.error("Erreur mise à jour notification:", err);
    }
  };

  const nonLuesCount = notifications.filter((n) => !n.lue).length;
  const badgeCount = nonLuesCount > 999 ? "999+" : String(nonLuesCount);
  const displayedNotifications = showAllNotifications
    ? notifications
    : notifications.slice(0, 5);

  const handleToutMarquerCommeLu = async () => {
    const unread = notifications.filter((notification) => !notification.lue);
    if (unread.length === 0 || markingAllRead) return;

    setMarkingAllRead(true);
    try {
      const results = await Promise.all(
        unread.map(async (notification) => ({
          id: notification.id,
          success: await marquerNotificationLue(notification.id),
        }))
      );
      const successfulIds = new Set(
        results.filter((result) => result.success).map((result) => result.id)
      );
      if (successfulIds.size > 0) {
        setNotifications((prev) =>
          prev.map((notification) =>
            successfulIds.has(notification.id)
              ? { ...notification, lue: true }
              : notification
          )
        );
        if (activeToast && successfulIds.has(activeToast.id)) setActiveToast(null);
      }
    } finally {
      setMarkingAllRead(false);
    }
  };

  const getNotificationIcon = (type) => {
    switch (type) {
      case "VALIDATION":
        return <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0" />;
      case "REFUS":
        return <XCircle className="w-4 h-4 text-rose-500 shrink-0" />;
      default:
        return <Info className="w-4 h-4 text-blue-500 shrink-0" />;
    }
  };

  return (
    <div className="relative">
      {/* BOUTON CLOCHE DANS LE HEADER */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative p-2 rounded-xl text-slate-300 hover:text-white hover:bg-slate-800 transition-all focus:outline-none"
        aria-label={nonLuesCount > 0 ? `Notifications, ${nonLuesCount} non lues` : "Notifications"}
      >
        <Bell className="w-5 h-5" />
        {nonLuesCount > 0 && (
          <span className="absolute top-1 right-1 flex h-4 w-4 items-center justify-center rounded-full bg-rose-600 text-[10px] font-bold text-white ring-2 ring-slate-900">
            {badgeCount}
          </span>
        )}
      </button>

      {/* DROPDOWN MENU */}
      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 md:w-96 bg-white rounded-2xl shadow-xl border border-slate-200 z-50 overflow-hidden text-slate-800">
          <div className="p-3 bg-slate-900 text-white flex items-center justify-between border-b border-slate-800">
            <div className="flex items-center gap-2">
              <Bell className="w-4 h-4 text-blue-400" />
              <span className="font-semibold text-sm">Notifications</span>
            </div>

            {/* Filtres Rapides */}
            <button
              onClick={() => {
                setLoading(true);
                setShowAllNotifications(false);
                setFilterNonLues(!filterNonLues);
              }}
              className="text-xs text-blue-400 hover:text-blue-300 flex items-center gap-1 bg-slate-800 px-2 py-1 rounded-lg"
            >
              <Filter className="w-3 h-3" />
              {filterNonLues ? "Toutes" : "Non lues"}
            </button>
          </div>

          <div className="max-h-80 overflow-y-auto divide-y divide-slate-100">
            {loading ? (
              <div className="p-6 text-center text-xs text-slate-400">
                Chargement...
              </div>
            ) : notifications.length === 0 ? (
              <div className="p-6 text-center text-xs text-slate-400">
                Aucune notification.
              </div>
            ) : (
              displayedNotifications.map((n) => (
                <div
                  key={n.id}
                  className={`p-3 text-xs flex items-start gap-3 transition-colors ${n.lue ? "bg-white" : "bg-blue-50/50"
                    }`}
                >
                  {getNotificationIcon(n.type)}
                  <div className="flex-1">
                    <p className={`leading-snug ${!n.lue ? "font-semibold text-slate-900" : "text-slate-600"}`}>
                      {n.contenu}
                    </p>
                    <span className="text-[10px] text-slate-400 mt-1 block">
                      {n.date_envoi ? new Date(n.date_envoi).toLocaleDateString("fr-FR") : "Aujourd'hui"}
                    </span>
                  </div>
                  {!n.lue && (
                    <button
                      onClick={(e) => handleMarquerLue(n.id, e)}
                      className="p-1 text-blue-600 hover:bg-blue-100 rounded-md shrink-0"
                      title="Marquer comme lue"
                    >
                      <Check className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>
              ))
            )}
          </div>

          {notifications.length > 0 && (
            <div className="p-2 border-t border-slate-200 bg-slate-50 flex items-center justify-between gap-2">
              {notifications.length > 5 ? (
                <button
                  type="button"
                  onClick={() => setShowAllNotifications((value) => !value)}
                  className="text-xs font-medium text-blue-700 hover:text-blue-900 px-2 py-1 rounded-md hover:bg-blue-50"
                >
                  {showAllNotifications ? "Réduire la liste" : `Voir plus (${notifications.length})`}
                </button>
              ) : (
                <span className="text-[11px] text-slate-400">
                  {notifications.length} notification{notifications.length > 1 ? "s" : ""}
                </span>
              )}
              {nonLuesCount > 0 && (
                <button
                  type="button"
                  onClick={handleToutMarquerCommeLu}
                  disabled={markingAllRead}
                  className="text-xs font-medium text-blue-700 hover:text-blue-900 disabled:opacity-50 px-2 py-1 rounded-md hover:bg-blue-50 inline-flex items-center gap-1"
                >
                  <CheckCheck className="w-3.5 h-3.5" />
                  {markingAllRead ? "Mise à jour..." : "Tout lire"}
                </button>
              )}
            </div>
          )}
        </div>
      )}

      {/* TOAST TEMPORAIRE FLOATTANT EN HAUT À DROITE */}
      {activeToast && (
        <div className="fixed top-4 right-4 z-50 max-w-sm w-full bg-slate-900 text-white rounded-xl shadow-2xl p-4 border border-slate-700 flex items-start gap-3 animate-in fade-in slide-in-from-top-2">
          {getNotificationIcon(activeToast.type)}
          <div className="flex-1 text-xs">
            <p className="font-semibold text-slate-200">Nouvelle notification</p>
            <p className="text-slate-300 mt-0.5">{activeToast.contenu}</p>
          </div>
          <button
            onClick={() => setActiveToast(null)}
            className="text-slate-400 hover:text-white p-1 rounded-lg"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}
    </div>
  );
}