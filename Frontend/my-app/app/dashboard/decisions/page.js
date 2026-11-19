"use client";
import {useEffect,useState} from "react";
import { authFetch, API_BASE_URL } from "@/lib/apiClient";
import {useToast} from "../../Components/ToastProvider";
const API=`${API_BASE_URL}`;
const FINAL=["VALIDEE","REFUSEE"];
export default function DecisionsPage(){
 const [rows,setRows]=useState([]),[loading,setLoading]=useState(true),[busy,setBusy]=useState(null);const toast=useToast();
 useEffect(() => {
  let active = true;
  authFetch(API + "/circuit-conge/employe/mes-decisions")
   .then(r => { if (!r.ok) throw new Error("Impossible de charger les décisions."); return r.json(); })
   .then(data => { if (active) setRows(Array.isArray(data) ? data.filter(d => FINAL.includes(d.statut)) : []); })
   .catch(e => { if (active) toast(e.message, "error"); })
   .finally(() => { if (active) setLoading(false); });
  return () => { active = false; };
 }, [toast]);
 const download=async id=>{setBusy(id);try{const r=await authFetch(API+"/circuit-conge/"+id+"/pdf");if(!r.ok)throw new Error("Document officiel indisponible.");const blob=await r.blob();const url=URL.createObjectURL(blob);const a=document.createElement("a");a.href=url;a.download="decision-conge-"+id+".pdf";document.body.appendChild(a);a.click();a.remove();URL.revokeObjectURL(url);}catch(e){toast(e.message,"error");}finally{setBusy(null);}};
 return <section className="spat-workspace spat-workspace--decisions space-y-5"><header><h1 className="text-2xl font-bold">Mes décisions de congé</h1><p className="text-sm text-slate-500">Consultez les décisions officielles de la Direction concernant vos congés.</p></header>{loading?<p>Chargement des décisions…</p>:rows.length===0?<div className="card bg-base-100 border p-6 text-center text-slate-500">Aucune décision définitive n&apos;est encore disponible.</div>:rows.map(d=><article key={d.id} className="card bg-base-100 border p-5 space-y-3"><div className="flex flex-wrap items-center justify-between gap-2"><b>Décision #{d.id}</b><span className={d.statut==="VALIDEE"?"badge badge-success":"badge badge-error"}>{d.statut==="VALIDEE"?"Congé accordé":"Congé refusé"}</span></div><p>Du <b>{d.dateDebut}</b> au <b>{d.dateFin}</b> · {d.nombreJours} jour(s) ouvrés</p>{d.statut==="REFUSEE"&&<div className="alert alert-error"><b>Motif du refus :</b> {d.motifRefus||"Motif non communiqué"}</div>}{d.avisChef&&<p className="text-sm text-slate-500">Avis hiérarchique : {d.avisChef==="FAVORABLE"?"Favorable":"Défavorable"}{d.commentaireChef?" — "+d.commentaireChef:""}</p>}<p className="text-xs text-slate-500">Décision enregistrée le {d.dateDecision?new Date(d.dateDecision).toLocaleString("fr-FR"):"—"}</p><button className="btn btn-outline btn-primary self-start" disabled={busy===d.id} onClick={()=>download(d.id)}>{busy===d.id?"Préparation…":"Télécharger la décision officielle (PDF)"}</button></article>)}</section>;
}
