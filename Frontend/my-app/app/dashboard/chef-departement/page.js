"use client";
import {useEffect,useState} from "react";
import { authFetch, API_BASE_URL } from "@/lib/apiClient";
import {useToast} from "../../Components/ToastProvider";
const API=`${API_BASE_URL}`;
export default function ChefDepartementPage(){
 const [rows,setRows]=useState([]),[avis,setAvis]=useState({}),[commentaires,setCommentaires]=useState({}),[busy,setBusy]=useState(null);const toast=useToast();
 const load=async()=>{const r=await authFetch(API+"/circuit-conge/chef/en-attente");if(!r.ok)throw new Error("Chargement impossible");setRows(await r.json());};
 useEffect(() => {
  let active = true;
  authFetch(API + "/circuit-conge/chef/en-attente")
   .then(r => { if (!r.ok) throw new Error("Chargement impossible"); return r.json(); })
   .then(data => { if (active) setRows(data); })
   .catch(e => { if (active) toast(e.message, "error"); });
  return () => { active = false; };
 }, [toast]);
 const forward=async id=>{const choix=avis[id]||"FAVORABLE";const commentaire=(commentaires[id]||"").trim();if(choix==="DEFAVORABLE"&&!commentaire){toast("Une observation est obligatoire pour un avis défavorable.","warning");return;}setBusy(id);try{const r=await authFetch(API+`/circuit-conge/${id}/transmettre-directeur`,{method:"POST",body:JSON.stringify({avis:choix,commentaire})});const d=await r.json();if(!r.ok)throw new Error(d.message||"Transmission impossible");toast("Dossier transmis à la Direction avec votre avis.","success");await load();}catch(e){toast(e.message,"error");}finally{setBusy(null);}};
 return <section className="spat-workspace spat-workspace--chef space-y-5"><header><h1 className="text-2xl font-bold">Chef de département — Dossiers à examiner</h1><p className="text-sm text-slate-500">Vérifiez la période, le nombre de jours, le reliquat et les nécessités du service. Votre avis accompagne le dossier à la Direction.</p></header>{rows.map(d=><article key={d.id} className="card bg-base-100 border p-5 space-y-3"><div><b>Dossier #{d.id} · {d.utilisateurPrenom} {d.utilisateurNom} ({d.utilisateurMatricule})</b><p>{d.dateDebut} → {d.dateFin} · {d.nombreJours} jour(s) ouvrés</p><p className="text-sm text-slate-500">Type #{d.typeCongeId} · Reliquat confirmé : {d.reliquatJours} jour(s)</p><p className="text-sm">{d.commentaire||"Aucun motif communiqué"}</p><p className="text-sm text-slate-500">Observation RH : {d.reliquatCommentaire||"Aucune"}</p></div><label className="form-control max-w-sm"><span className="label-text">Avis hiérarchique</span><select className="select select-bordered" value={avis[d.id]||"FAVORABLE"} onChange={e=>setAvis({...avis,[d.id]:e.target.value})}><option value="FAVORABLE">Favorable</option><option value="DEFAVORABLE">Défavorable</option></select></label><label className="form-control"><span className="label-text">Observation {avis[d.id]==="DEFAVORABLE"?"(obligatoire)":"(facultative)"}</span><textarea className="textarea textarea-bordered" value={commentaires[d.id]||""} onChange={e=>setCommentaires({...commentaires,[d.id]:e.target.value})}/></label><button className="btn btn-primary self-start" disabled={busy===d.id} onClick={()=>forward(d.id)}>{busy===d.id?"Transmission…":"Transmettre à la Direction"}</button></article>)}{!rows.length&&<p className="text-slate-500">Aucun dossier à examiner.</p>}</section>;
}
