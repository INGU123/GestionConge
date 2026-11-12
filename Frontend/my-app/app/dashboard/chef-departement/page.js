"use client";
import {useEffect,useState} from "react";
import {authFetch} from "@/lib/apiClient";
import {useToast} from "../../components/ToastProvider";
const API="http://localhost:8080";
export default function ChefDepartementPage(){
 const [rows,setRows]=useState([]),[busy,setBusy]=useState(null);const toast=useToast();
 const load=async()=>{const r=await authFetch(API+"/circuit-conge/chef/en-attente");if(!r.ok)throw new Error("Chargement impossible");setRows(await r.json());};
 useEffect(()=>{load().catch(e=>toast(e.message,"error"));},[]);
 const forward=async id=>{setBusy(id);try{const r=await authFetch(API+`/circuit-conge/${id}/transmettre-directeur`,{method:"POST"});const d=await r.json();if(!r.ok)throw new Error(d.message||"Transmission impossible");toast("Dossier transmis au directeur.","success");await load();}catch(e){toast(e.message,"error");}finally{setBusy(null);}};
 return <section className="space-y-5"><header><h1 className="text-2xl font-bold">Chef de département — Dossiers à examiner</h1><p className="text-sm text-slate-500">Après examen, transmettez le dossier au directeur de votre département.</p></header>{rows.map(d=><article key={d.id} className="card bg-base-100 border p-5 flex flex-wrap justify-between gap-4"><div><b>Dossier #{d.id} · {d.utilisateurPrenom} {d.utilisateurNom} ({d.utilisateurMatricule})</b><p>{d.dateDebut} → {d.dateFin} · {d.nombreJours} jour(s)</p><p className="text-sm text-slate-500">{d.commentaire||"Aucun commentaire"}</p><p className="text-sm">Reliquat confirmé : {d.reliquatJours} jour(s)</p></div><button className="btn btn-primary self-center" disabled={busy===d.id} onClick={()=>forward(d.id)}>{busy===d.id?"Transmission…":"Transmettre au directeur"}</button></article>)}{!rows.length&&<p className="text-slate-500">Aucun dossier à examiner.</p>}</section>;
}
