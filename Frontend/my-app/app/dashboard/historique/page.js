"use client";

import { useEffect, useRef, useState } from "react";
import {
  getAllHistorique,
  getHistoriqueUtilisateur,
} from "../../api/historiqueMouvements/historiqueMouvement";
import { getAllTypeConge } from "../../api/typeConge/typeConge";
import { getCurrentUser, authFetch } from "@/lib/apiClient";
import {
  History,
  User,
  ArrowUpRight,
  ArrowDownLeft,
  Calendar,
  MessageSquare,
  Users,
  Layers,
  ShieldCheck,
  Download,
  FileText,
  FileSpreadsheet,
  ChevronDown,
} from "lucide-react";
import { useToast } from "../../components/ToastProvider";

export default function HistoriquePage() {
  const [currentUser, setCurrentUser] = useState(null);
  const [mouvements, setMouvements] = useState([]);
  const [typesConge, setTypesConge] = useState([]);
  const [utilisateurs, setUtilisateurs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [viewAll, setViewAll] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [exportMenuOpen, setExportMenuOpen] = useState(false);
  const exportMenuRef = useRef(null);
  const showToast = useToast();

  useEffect(() => {
    if (!exportMenuOpen) return;
    const handleOutsideClick = (event) => {
      if (exportMenuRef.current && !exportMenuRef.current.contains(event.target)) {
        setExportMenuOpen(false);
      }
    };
    const handleEscape = (event) => {
      if (event.key === "Escape") setExportMenuOpen(false);
    };
    document.addEventListener("mousedown", handleOutsideClick);
    document.addEventListener("keydown", handleEscape);
    return () => {
      document.removeEventListener("mousedown", handleOutsideClick);
      document.removeEventListener("keydown", handleEscape);
    };
  }, [exportMenuOpen]);

  useEffect(() => {
    let isMounted = true;

    async function fetchData() {
      const user = getCurrentUser();
      if (!user) {
        if (isMounted) setLoading(false);
        return;
      }

      if (isMounted) {
        setCurrentUser(user);
        const isManagerRole = user.role === "ADMIN" || user.role === "MANAGER";
        setViewAll(isManagerRole);
      }

      try {
        if (isMounted) setLoading(true);
        const isManager = user.role === "ADMIN" || user.role === "MANAGER";

        const [mouvementsData, typesData, usersRes] = await Promise.all([
          viewAll && isManager
            ? getAllHistorique()
            : getHistoriqueUtilisateur(user.id),
          getAllTypeConge().catch(() => []),
          authFetch("http://localhost:8080/utilisateur/all")
            .then((r) => (r.ok ? r.json() : []))
            .catch(() => []),
        ]);

        if (isMounted) {
          setMouvements(Array.isArray(mouvementsData) ? mouvementsData : []);
          setTypesConge(Array.isArray(typesData) ? typesData : []);
          setUtilisateurs(Array.isArray(usersRes) ? usersRes : []);
        }
      } catch (err) {
        console.error("Erreur lors du chargement :", err);
        if (isMounted) showToast("Impossible de charger l'historique.", "error");
        if (isMounted) setMouvements([]);
      } finally {
        if (isMounted) setLoading(false);
      }
    }

    fetchData();

    return () => {
      isMounted = false;
    };
  }, [viewAll, showToast]);

  const getUserName = (userId) => {
    const found = utilisateurs.find((u) => u.id === userId);
    if (found) return found.prenom ? `${found.prenom} ${found.nom || ""}` : found.email;
    if (String(currentUser?.id) === String(userId)) {
      const name = `${currentUser.prenom || ""} ${currentUser.nom || ""}`.trim();
      return name || currentUser.email || `Utilisateur #${userId}`;
    }
    return `Utilisateur #${userId}`;
  };

  const getTypeLabel = (typeId) => {
    const found = typesConge.find((t) => t.id === typeId);
    return found?.libelle || found?.code || `Type #${typeId}`;
  };

  const isManager = currentUser?.role === "ADMIN" || currentUser?.role === "MANAGER";

  const handleExportPdf = () => {
    setExportMenuOpen(false);
    try {
      const popup = window.open("", "_blank", "width=1100,height=800");
      if (!popup) {
        showToast("Autorise les fenêtres pop-up pour exporter le PDF.", "error");
        return;
      }

      const escapeHtml = (value) => String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
      const dateCell = (value) =>
        value ? new Date(value).toLocaleDateString("fr-FR") : "-";
      const rows = mouvements.slice().reverse().map((m) => {
        const isDebit = m.typeMouvement === "DEBIT_CONGE";
        const quantity = Number(m.quantite);
        const displayedQuantity = Number.isFinite(quantity)
          ? (isDebit ? -quantity : quantity)
          : (m.quantite ?? "");
        return [
          dateCell(m.date),
          getUserName(m.utilisateurId),
          isDebit ? "DÉBIT" : "CRÉDIT",
          getTypeLabel(m.type_conge_id),
          displayedQuantity,
          m.commentaire || "",
          m.effectue_par ? getUserName(m.effectue_par) : "Système",
        ];
      });

      const headers = [
        "Date", "Collaborateur", "Type de mouvement", "Type de congé",
        "Quantité (jours)", "Commentaire", "Effectué par",
      ];
      const today = new Date().toLocaleDateString("fr-FR");
      popup.document.write(`<!doctype html>
<html lang="fr">
<head>
  <meta charset="utf-8">
  <title>Historique des mouvements de congés</title>
  <style>
    @page { size: A4 landscape; margin: 12mm; }
    body { font-family: Arial, sans-serif; color: #1e293b; margin: 0; }
    h1 { font-size: 20px; margin: 0 0 6px; }
    p { font-size: 11px; color: #475569; margin: 0 0 16px; }
    table { width: 100%; border-collapse: collapse; font-size: 9px; }
    thead { display: table-header-group; }
    tr { break-inside: avoid; }
    th, td { border: 1px solid #cbd5e1; padding: 6px; text-align: left; vertical-align: top; overflow-wrap: anywhere; }
    th { background: #e2e8f0; font-weight: 700; }
    @media screen { body { padding: 24px; } .hint { margin-top: 16px; } }
    @media print { .hint { display: none; } }
  </style>
</head>
<body>
  <h1>SPAT — Historique des mouvements de congés</h1>
  <p>Exporté le ${escapeHtml(today)} · ${rows.length} mouvement(s)</p>
  <table>
    <thead><tr>${headers.map((header) => `<th>${escapeHtml(header)}</th>`).join("")}</tr></thead>
    <tbody>${rows.map((row) => `<tr>${row.map((cell) => `<td>${escapeHtml(cell)}</td>`).join("")}</tr>`).join("")}</tbody>
  </table>
  <p class="hint">Dans la fenêtre d'impression, choisissez « Enregistrer au format PDF ».</p>
  <script>window.onload = () => { window.focus(); window.print(); };</script>
</body>
</html>`);
      popup.document.close();
      showToast("Préparation de l'impression PDF. Choisis « Enregistrer au format PDF ».", "success");
    } catch (error) {
      console.error("Erreur lors de l'export PDF de l'historique :", error);
      showToast("Impossible de préparer l'export PDF.", "error");
    }
  };

  const handleExportExcel = async () => {
    setExportMenuOpen(false);
    setExporting(true);
    try {
      const dateCell = (value) =>
        value ? new Date(value).toLocaleDateString("fr-FR") : "-";
      const headers = [
        "Date",
        "Collaborateur",
        "Type de mouvement",
        "Type de congé",
        "Quantité (jours)",
        "Commentaire",
        "Effectué par",
      ];
      const dataRows = mouvements.slice().reverse().map((m) => {
        const isDebit = m.typeMouvement === "DEBIT_CONGE";
        const quantity = Number(m.quantite);
        return [
          dateCell(m.date),
          getUserName(m.utilisateurId),
          isDebit ? "DÉBIT" : "CRÉDIT",
          getTypeLabel(m.type_conge_id),
          Number.isFinite(quantity) ? (isDebit ? -quantity : quantity) : String(m.quantite ?? ""),
          m.commentaire || "",
          m.effectue_par ? getUserName(m.effectue_par) : "Système",
        ];
      });

      const xmlEscape = (value) => String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&apos;");
      const columnName = (number) => {
        let name = "";
        while (number > 0) {
          const remainder = (number - 1) % 26;
          name = String.fromCharCode(65 + remainder) + name;
          number = Math.floor((number - 1) / 26);
        }
        return name;
      };
      const makeCell = (value, row, column) => {
        const ref = columnName(column) + row;
        if (typeof value === "number" && Number.isFinite(value)) {
          return `<c r="${ref}"><v>${value}</v></c>`;
        }
        const text = xmlEscape(value);
        const preserve = /^\s|\s$/.test(String(value ?? "")) ? ' xml:space="preserve"' : "";
        return `<c r="${ref}" t="inlineStr"><is><t${preserve}>${text}</t></is></c>`;
      };
      const worksheetRows = [headers, ...dataRows].map((row, rowIndex) => {
        const rowNumber = rowIndex + 1;
        return `<row r="${rowNumber}">${row.map((value, columnIndex) => makeCell(value, rowNumber, columnIndex + 1)).join("")}</row>`;
      }).join("");
      const worksheet = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>${worksheetRows}</sheetData><autoFilter ref="A1:G${dataRows.length + 1}"/></worksheet>`;
      const files = [
        {
          name: "[Content_Types].xml",
          content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>',
        },
        {
          name: "_rels/.rels",
          content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>',
        },
        {
          name: "xl/workbook.xml",
          content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Historique" sheetId="1" r:id="rId1"/></sheets></workbook>',
        },
        {
          name: "xl/_rels/workbook.xml.rels",
          content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>',
        },
        { name: "xl/worksheets/sheet1.xml", content: worksheet },
      ];

      const encoder = new TextEncoder();
      const crc32 = (bytes) => {
        let crc = 0xffffffff;
        for (const byte of bytes) {
          crc ^= byte;
          for (let bit = 0; bit < 8; bit++) {
            crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0);
          }
        }
        return (crc ^ 0xffffffff) >>> 0;
      };
      const write16 = (view, offset, value) => view.setUint16(offset, value, true);
      const write32 = (view, offset, value) => view.setUint32(offset, value >>> 0, true);
      const localParts = [];
      const centralParts = [];
      let localOffset = 0;
      for (const file of files) {
        const nameBytes = encoder.encode(file.name);
        const contentBytes = encoder.encode(file.content);
        const crc = crc32(contentBytes);
        const localHeader = new Uint8Array(30 + nameBytes.length);
        const localView = new DataView(localHeader.buffer);
        write32(localView, 0, 0x04034b50);
        write16(localView, 4, 20);
        write16(localView, 6, 0);
        write16(localView, 8, 0);
        write16(localView, 10, 0);
        write16(localView, 12, 0);
        write32(localView, 14, crc);
        write32(localView, 18, contentBytes.length);
        write32(localView, 22, contentBytes.length);
        write16(localView, 26, nameBytes.length);
        write16(localView, 28, 0);
        localHeader.set(nameBytes, 30);
        localParts.push(localHeader, contentBytes);

        const centralHeader = new Uint8Array(46 + nameBytes.length);
        const centralView = new DataView(centralHeader.buffer);
        write32(centralView, 0, 0x02014b50);
        write16(centralView, 4, 20);
        write16(centralView, 6, 20);
        write16(centralView, 8, 0);
        write16(centralView, 10, 0);
        write16(centralView, 12, 0);
        write16(centralView, 14, 0);
        write32(centralView, 16, crc);
        write32(centralView, 20, contentBytes.length);
        write32(centralView, 24, contentBytes.length);
        write16(centralView, 28, nameBytes.length);
        write16(centralView, 30, 0);
        write16(centralView, 32, 0);
        write16(centralView, 34, 0);
        write16(centralView, 36, 0);
        write32(centralView, 38, 0);
        write32(centralView, 42, localOffset);
        centralHeader.set(nameBytes, 46);
        centralParts.push(centralHeader);
        localOffset += localHeader.length + contentBytes.length;
      }
      const centralSize = centralParts.reduce((sum, part) => sum + part.length, 0);
      const end = new Uint8Array(22);
      const endView = new DataView(end.buffer);
      write32(endView, 0, 0x06054b50);
      write16(endView, 4, 0);
      write16(endView, 6, 0);
      write16(endView, 8, files.length);
      write16(endView, 10, files.length);
      write32(endView, 12, centralSize);
      write32(endView, 16, localOffset);
      write16(endView, 20, 0);

      const workbookBlob = new Blob([...localParts, ...centralParts, end], {
        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      });
      const url = URL.createObjectURL(workbookBlob);
      const link = document.createElement("a");
      const today = new Date();
      const date = [
        today.getFullYear(),
        String(today.getMonth() + 1).padStart(2, "0"),
        String(today.getDate()).padStart(2, "0"),
      ].join("-");
      link.href = url;
      link.download = `historique_conges_${date}.xlsx`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
      showToast("L'historique a été exporté au format Excel.", "success");
    } catch (error) {
      console.error("Erreur lors de l'export Excel de l'historique :", error);
      showToast(error.message || "Impossible d'exporter l'historique au format Excel.", "error");
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Banner Institutionnelle SPAT */}
      <div className="bg-slate-900 text-white rounded-2xl p-6 shadow-md border-l-4 border-blue-600 flex justify-between items-center flex-wrap gap-4">
        <div>
          <div className="text-xs font-semibold uppercase tracking-widest text-blue-400 mb-1">
            SPAT — Société du Port à Gestion Autonome de Toamasina
          </div>
          <h1 className="text-2xl font-bold tracking-tight flex items-center gap-2">
            <History className="w-6 h-6 text-blue-400" />
            {`Historique des Mouvements & Journal d'Audit`}
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Traçabilité complète des débits, crédits, ajustements et régularisations de soldes de congés.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          {isManager && (
            <div className="inline-flex p-1 bg-slate-800 rounded-xl border border-slate-700">
              <button
                onClick={() => setViewAll(false)}
                className={`px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all flex items-center gap-1.5 ${
                  !viewAll
                    ? "bg-blue-600 text-white shadow-sm"
                    : "text-slate-300 hover:text-white"
                }`}
              >
                <User className="w-3.5 h-3.5" />
                Mes mouvements
              </button>
              <button
                onClick={() => setViewAll(true)}
                className={`px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all flex items-center gap-1.5 ${
                  viewAll
                    ? "bg-blue-600 text-white shadow-sm"
                    : "text-slate-300 hover:text-white"
                }`}
              >
                <Users className="w-3.5 h-3.5" />
                Tous les collaborateurs
              </button>
            </div>
          )}
          <div className="relative" ref={exportMenuRef}>
            <button
              type="button"
              onClick={() => setExportMenuOpen((open) => !open)}
              disabled={loading || mouvements.length === 0}
              aria-haspopup="menu"
              aria-expanded={exportMenuOpen}
              className="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
            >
              <Download className="h-4 w-4" />
              Exportation fichier
              <ChevronDown className={`h-4 w-4 transition-transform ${exportMenuOpen ? "rotate-180" : ""}`} />
            </button>
            {exportMenuOpen && (
              <div
                role="menu"
                className="absolute right-0 top-full z-50 mt-2 w-56 overflow-hidden rounded-xl border border-slate-200 bg-white p-1.5 shadow-xl"
              >
                <button
                  type="button"
                  role="menuitem"
                  onClick={handleExportPdf}
                  className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-sm font-medium text-slate-700 transition hover:bg-slate-100"
                >
                  <FileText className="h-4 w-4 text-rose-600" />
                  Exporter en PDF
                </button>
                <button
                  type="button"
                  role="menuitem"
                  onClick={handleExportExcel}
                  disabled={exporting}
                  className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {exporting ? (
                    <span className="loading loading-spinner loading-xs" />
                  ) : (
                    <FileSpreadsheet className="h-4 w-4 text-emerald-600" />
                  )}
                  Exporter en Excel
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Tableau des mouvements */}
      <div className="bg-white shadow-sm border border-slate-200 rounded-2xl p-6">
        {loading ? (
          <div className="flex justify-center items-center py-16">
            <span className="loading loading-spinner loading-lg text-primary"></span>
          </div>
        ) : mouvements.length === 0 ? (
          <div className="text-center py-16 text-slate-400">
            <History className="w-12 h-12 mx-auto mb-2 text-slate-300" />
            <p className="font-semibold text-slate-600">Aucun mouvement enregistré.</p>
            <p className="text-sm mt-1 text-slate-400">
              Les mouvements apparaîtront lors de la validation des congés ou ajustements de soldes.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="table w-full">
              <thead>
                <tr className="bg-slate-50 text-slate-600 text-xs uppercase tracking-wider border-b border-slate-200">
                  <th className="py-3">Date</th>
                  <th className="py-3">Collaborateur</th>
                  <th className="py-3">Type de Mouvement</th>
                  <th className="py-3">Type de Congé</th>
                  <th className="py-3">Quantité</th>
                  <th className="py-3">Commentaire</th>
                  <th className="py-3">Effectué par</th>
                </tr>
              </thead>
              <tbody>
                {mouvements.slice().reverse().map((m, idx) => {
                  const isDebit = m.typeMouvement === "DEBIT_CONGE";

                  return (
                    <tr key={m.id || idx} className="hover:bg-slate-50/80 transition-colors border-b border-slate-100">
                      {/* Date */}
                      <td className="py-3.5 text-xs text-slate-600 font-medium">
                        <div className="flex items-center gap-1.5">
                          <Calendar className="w-3.5 h-3.5 text-slate-400" />
                          {m.date ? new Date(m.date).toLocaleDateString("fr-FR") : "-"}
                        </div>
                      </td>

                      {/* Collaborateur */}
                      <td className="py-3.5">
                        <div className="flex items-center gap-2">
                          <div className="w-7 h-7 rounded-full bg-slate-100 text-slate-600 flex items-center justify-center font-bold text-xs border border-slate-200">
                            <User className="w-3.5 h-3.5" />
                          </div>
                          <span className="font-bold text-slate-800 text-sm">
                            {getUserName(m.utilisateurId)}
                          </span>
                        </div>
                      </td>

                      {/* Type Mouvement */}
                      <td className="py-3.5">
                        {isDebit ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded text-xs font-bold bg-rose-50 text-rose-700 border border-rose-200">
                            <ArrowDownLeft className="w-3.5 h-3.5 text-rose-600" />
                            DÉBIT
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            <ArrowUpRight className="w-3.5 h-3.5 text-emerald-600" />
                            CRÉDIT
                          </span>
                        )}
                      </td>

                      {/* Type Congé */}
                      <td className="py-3.5 font-semibold text-slate-700 text-sm">
                        <div className="flex items-center gap-1.5">
                          <Layers className="w-3.5 h-3.5 text-slate-400" />
                          {getTypeLabel(m.type_conge_id)}
                        </div>
                      </td>

                      {/* Quantité */}
                      <td className="py-3.5">
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded text-xs font-bold ${
                          isDebit 
                            ? "bg-rose-50 text-rose-700 border border-rose-200" 
                            : "bg-blue-50 text-blue-700 border border-blue-200"
                        }`}>
                          {isDebit ? `-${m.quantite}` : `+${m.quantite}`} j
                        </span>
                      </td>

                      {/* Commentaire */}
                      <td className="py-3.5 text-xs text-slate-500 max-w-xs">
                        <div className="flex items-center gap-1 truncate" title={m.commentaire}>
                          {m.commentaire ? (
                            <>
                              <MessageSquare className="w-3 h-3 text-slate-400 shrink-0" />
                              <span className="truncate">{m.commentaire}</span>
                            </>
                          ) : (
                            <span className="text-slate-300 italic">-</span>
                          )}
                        </div>
                      </td>

                      {/* Effectué par */}
                      <td className="py-3.5 text-xs text-slate-600 font-medium">
                        <div className="flex items-center gap-1">
                          <ShieldCheck className="w-3.5 h-3.5 text-slate-400" />
                          {m.effectue_par ? getUserName(m.effectue_par) : "Système"}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}