"use client";
import {useEffect,useState} from "react";
import { authFetch, API_BASE_URL } from "@/lib/apiClient";
import {useToast} from "../../Components/ToastProvider";
const API=`${API_BASE_URL}`;
export default function DirecteurPage(){
 const [rows,setRows]=useState([]),[motifs,setMotifs]=useState({}),[busy,setBusy]=useState(null);const toast=useToast();
 const load=async()=>{const r=await authFetch(API+"/circuit-conge/directeur/en-attente");if(!r.ok)throw new Error("Chargement impossible");setRows(await r.json());};
 useEffect(() => {
  let active = true;
  authFetch(API + "/circuit-conge/directeur/en-attente")
   .then(r => { if (!r.ok) throw new Error("Chargement impossible"); return r.json(); })
   .then(data => { if (active) setRows(data); })
   .catch(e => { if (active) toast(e.message, "error"); });
  return () => { active = false; };
 }, [toast]);
 const decide=async(id,decision)=>{if(decision==="REFUSEE"&&!motifs[id]?.trim()){toast("Le motif de refus est obligatoire.","warning");return;}setBusy(id);try{const r=await authFetch(API+`/circuit-conge/${id}/decision`,{method:"POST",body:JSON.stringify({decision,motif:motifs[id]||""})});const d=await r.json();if(!r.ok)throw new Error(d.message||"Décision impossible");toast(decision==="VALIDEE"?"Congé accordé.":"Congé refusé.","success");await load();}catch(e){toast(e.message,"error");}finally{setBusy(null);}};
 return <section className="space-y-5"><header><h1 className="text-2xl font-bold">Directeur — Décisions de congé</h1><p className="text-sm text-slate-500">Accordez ou refusez les demandes transmises par le chef de département. Tout refus doit être motivé.</p></header>{rows.map(d=><article key={d.id} className="card bg-base-100 border p-5 space-y-3"><b>Dossier #{d.id} · {d.utilisateurPrenom} {d.utilisateurNom} ({d.utilisateurMatricule})</b><p>{d.dateDebut} → {d.dateFin} · {d.nombreJours} jour(s) ouvrés</p><p className="text-sm text-slate-500">Reliquat confirmé : {d.reliquatJours} jour(s)</p><div className="rounded-lg bg-slate-50 border p-3 text-sm"><b>Avis du chef : </b><span className={d.avisChef==="DEFAVORABLE"?"text-rose-700":"text-emerald-700"}>{d.avisChef||"Non renseigné"}</span><p>{d.commentaireChef||"Aucune observation"}</p></div><textarea className="textarea textarea-bordered w-full" placeholder="Motif obligatoire en cas de refus" value={motifs[d.id]||""} onChange={e=>setMotifs({...motifs,[d.id]:e.target.value})}/><div className="flex gap-2"><button className="btn btn-success text-white" disabled={busy===d.id} onClick={()=>decide(d.id,"VALIDEE")}>Accorder</button><button className="btn btn-error text-white" disabled={busy===d.id||!motifs[d.id]?.trim()} onClick={()=>decide(d.id,"REFUSEE")}>Refuser</button></div></article>)}{!rows.length&&<p className="text-slate-500">Aucun dossier en attente.</p>}</section>;
}
