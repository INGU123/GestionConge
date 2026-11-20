"use client";
import {useEffect,useState} from "react";
import {authFetch} from "@/lib/apiClient";
import {useToast} from "../../Components/ToastProvider";
const API="http://localhost:8080";
export default function ChefDepartementPage(){
 const [rows,setRows]=useState([]),[busy,setBusy]=useState(null);const toast=useToast();
 const load=async()=>{const r=await authFetch(API+"/circuit-conge/chef/en-attente");if(!r.ok)throw new Error("Chargement impossible");setRows(await r.json());};
 useEffect(()=>{load().catch(e=>toast(e.message,"error"));},[]);
 const downloadScan=async id=>{try{const r=await authFetch(API+"/circuit-conge/"+id+"/piece-jointe");if(!r.ok){const detail=await r.text();throw new Error(detail||"Scan indisponible pour ce dossier.");}const blob=await r.blob();const extension=blob.type.includes("pdf")?"pdf":blob.type.includes("png")?"png":blob.type.includes("jpeg")?"jpg":"bin";const url=URL.createObjectURL(blob);const link=document.createElement("a");link.href=url;link.download="scan-demande-conge-"+id+"."+extension;document.body.appendChild(link);link.click();link.remove();setTimeout(()=>URL.revokeObjectURL(url),1000);}catch(e){toast(e.message,"error");}};
 const forward=async id=>{setBusy(id);try{const r=await authFetch(API+`/circuit-conge/${id}/transmettre-directeur`,{method:"POST"});const d=await r.json();if(!r.ok)throw new Error(d.message||"Transmission impossible");toast("Dossier transmis au directeur.","success");await load();}catch(e){toast(e.message,"error");}finally{setBusy(null);}};
 return <section className="space-y-5"><header><h1 className="text-2xl font-bold">Chef de département — Dossiers à examiner</h1><p className="text-sm text-slate-500">Après examen, transmettez le dossier au directeur de votre département.</p></header>{rows.map(d=><article key={d.id} className="card bg-base-100 border p-5 flex flex-wrap justify-between gap-4"><div><b>Dossier #{d.id} · {d.utilisateurPrenom} {d.utilisateurNom} ({d.utilisateurMatricule})</b><p>{d.dateDebut} → {d.dateFin} · {d.nombreJours} jour(s)</p><p className="text-sm text-slate-500">{d.commentaire||"Aucun commentaire"}</p><p className="text-sm">Reliquat confirmé : {d.reliquatJours} jour(s)</p><button type="button" className="btn btn-sm btn-outline mt-2" onClick={()=>downloadScan(d.id)}>Télécharger le scan</button></div><button className="btn btn-primary self-center" disabled={busy===d.id} onClick={()=>forward(d.id)}>{busy===d.id?"Transmission…":"Transmettre au directeur"}</button></article>)}{!rows.length&&<p className="text-slate-500">Aucun dossier à examiner.</p>}</section>;
}
