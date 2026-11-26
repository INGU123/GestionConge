"use client";
import {useEffect,useState} from "react";
import {authFetch} from "@/lib/apiClient";
import {useToast} from "../../Components/ToastProvider";
import {getAllTypeConge} from "../../api/typeConge/typeConge";
import {Inbox} from "lucide-react";
const API="http://localhost:8080";
export default function ChefDepartementPage(){
 const [rows,setRows]=useState([]),[typesConge,setTypesConge]=useState([]),[busy,setBusy]=useState(null);const toast=useToast();
 const load=async()=>{const [r,typeData]=await Promise.all([authFetch(API+"/circuit-conge/chef/en-attente"),getAllTypeConge()]);if(!r.ok)throw new Error("Chargement impossible");setRows(await r.json());setTypesConge(typeData);};
 useEffect(()=>{
  // eslint-disable-next-line react-hooks/set-state-in-effect
  load().catch(e=>toast(e.message,"error"));
 },[]);
 const getTypeLabel=d=>{const typeId=d.typeCongeId??d.typeConge?.id;const type=typesConge.find(item=>String(item.id)===String(typeId));return type?.libelle||d.typeConge?.libelle||type?.code||"Type de congé";};
 const forward=async id=>{setBusy(id);try{const r=await authFetch(API+`/circuit-conge/${id}/transmettre-directeur`,{method:"POST"});const d=await r.json();if(!r.ok)throw new Error(d.message||"Transmission impossible");toast("Dossier transmis au directeur.","success");await load();}catch(e){toast(e.message,"error");}finally{setBusy(null);}};
 return <section className="chef-departement-theme space-y-5"><header><h1 className="text-2xl font-bold">Chef de département — Dossiers à examiner</h1><p className="text-sm text-base-content/60">Après examen, transmettez le dossier au directeur de votre département.</p></header>{rows.map(d=><article key={d.id} className="card bg-base-100 border p-5 flex flex-wrap justify-between gap-4"><div><b>Dossier #{d.id} · {d.utilisateurPrenom} {d.utilisateurNom} ({d.utilisateurMatricule})</b><p>{getTypeLabel(d)} · {d.dateDebut} → {d.dateFin} · {d.nombreJours} jour(s)</p><p className="text-sm text-base-content/60">{d.commentaire||"Aucun commentaire"}</p><section className="rounded-xl border border-base-300 bg-base-200/50 p-4 space-y-3">
  <div className="flex items-center justify-between gap-2">
   <h3 className="text-sm font-semibold text-base-content">Confirmation de reliquat — Administration du personnel</h3>
   <span className="rounded-full bg-success/15 px-3 py-1 text-xs font-semibold text-success">Confirmé</span>
  </div>
  <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-3">
   <div className="rounded-lg border border-base-300 bg-base-100 p-3"><p className="text-xs font-medium text-base-content/60">Reliquat confirmé</p><p className="mt-1 text-base font-semibold text-base-content">{d.reliquatJours ?? "—"} jour(s)</p></div>
   <div className="rounded-lg border border-base-300 bg-base-100 p-3"><p className="text-xs font-medium text-base-content/60">Période de référence</p><p className="mt-1 text-sm font-semibold text-base-content">{d.reliquatPeriodeReference || "Non renseignée"}</p></div>
   <div className="rounded-lg border border-base-300 bg-base-100 p-3"><p className="text-xs font-medium text-base-content/60">Référence de confirmation RH</p><p className="mt-1 text-sm font-semibold text-base-content break-all">{d.reliquatReferenceConfirmation || "Non renseignée"}</p></div>
   <div className="rounded-lg border border-base-300 bg-base-100 p-3 sm:col-span-2 xl:col-span-3"><p className="text-xs font-medium text-base-content/60">Justificatif</p><p className="mt-1 text-sm text-base-content break-words">{d.reliquatJustificatif || "Aucun justificatif renseigné"}</p></div>
   <div className="rounded-lg border border-base-300 bg-base-100 p-3 sm:col-span-2 xl:col-span-3"><p className="text-xs font-medium text-base-content/60">Observation de l’administration</p><p className="mt-1 text-sm text-base-content whitespace-pre-wrap">{d.reliquatObservation || d.reliquatCommentaire || "Aucune observation"}</p></div>
  </div>
 </section></div><button className="btn btn-primary self-center" disabled={busy===d.id} onClick={()=>forward(d.id)}>{busy===d.id?"Transmission…":"Transmettre au directeur"}</button></article>)}{!rows.length && (
  <div className="flex flex-col items-center justify-center gap-3 rounded-xl border border-dashed border-base-300 bg-base-200/30 px-6 py-12 text-center">
   <Inbox size={48} strokeWidth={1.5} className="text-base-content/40" />
   <p className="text-sm text-base-content/60">Aucun dossier à examiner.</p>
  </div>
 )}</section>;
}
