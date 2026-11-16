"use client";
import {useEffect,useState} from "react";
import { authFetch, API_BASE_URL } from "@/lib/apiClient";
import {useToast} from "../../Components/ToastProvider";
const API=`${API_BASE_URL}`;
export default function ReliquatsPage(){
 const [rows,setRows]=useState([]),[balances,setBalances]=useState({}),[values,setValues]=useState({}),[busy,setBusy]=useState(null);const toast=useToast();
 const load=async()=>{
  const r=await authFetch(API+"/circuit-conge/administration/en-attente");if(!r.ok)throw new Error("Chargement impossible");
  const data=await r.json();setRows(data);
  const pairs=await Promise.all(data.map(async d=>{const res=await authFetch(API+"/solde/utilisateur/"+d.utilisateurId);return [d.id,res.ok?await res.json():[]];}));
  setBalances(Object.fromEntries(pairs));
 };
 useEffect(() => {
  let active = true;
  authFetch(API + "/circuit-conge/administration/en-attente")
   .then(r => { if (!r.ok) throw new Error("Chargement impossible"); return r.json(); })
   .then(async data => {
    if (!active) return;
    setRows(data);
    const pairs = await Promise.all(data.map(async d => {
     const response = await authFetch(API + "/solde/utilisateur/" + d.utilisateurId);
     return [d.id, response.ok ? await response.json() : []];
    }));
    if (active) setBalances(Object.fromEntries(pairs));
   })
   .catch(e => { if (active) toast(e.message, "error"); });
  return () => { active = false; };
 }, [toast]);
 const send=async id=>{setBusy(id);try{const v=values[id]||{};const r=await authFetch(API+"/circuit-conge/"+id+"/reliquat",{method:"POST",body:JSON.stringify({reliquatJours:Number(v.days),commentaire:v.comment||""})});const d=await r.json();if(!r.ok)throw new Error(d.message||"Enregistrement impossible");toast("Reliquat retourné à l'assistant.","success");await load();}catch(e){toast(e.message,"error");}finally{setBusy(null);}};
 return <section className="space-y-5"><header><h1 className="text-2xl font-bold">Administration du personnel — Vérification des reliquats</h1><p className="text-sm text-slate-500">Consultez le solde enregistré, vérifiez les droits disponibles et retournez le reliquat à l&apos;assistant.</p></header>
 {rows.map(d=>{const userBalances=balances[d.id]||[];const anneeDebut=Number(String(d.dateDebut).slice(0,4)),anneeFin=Number(String(d.dateFin).slice(0,4));const relevant=userBalances.filter(b=>String(b.typeCongeId)===String(d.typeCongeId)&&Number(b.periode)>=anneeDebut&&Number(b.periode)<=anneeFin);return <article key={d.id} className="card bg-base-100 border p-5 space-y-3">
 <div className="flex flex-wrap justify-between gap-2"><b>Dossier #{d.id} — {d.utilisateurPrenom} {d.utilisateurNom} ({d.utilisateurMatricule})</b><span>Type #{d.typeCongeId} · {d.dateDebut} → {d.dateFin} · {d.nombreJours} jour(s) demandés</span></div>{d.assistantCorrection&&<div className="alert alert-warning text-sm"><b>Correction demandée par l&apos;assistant :</b> {d.assistantCorrection}</div>}
 <div className="rounded-lg bg-slate-50 border p-3 text-sm"><b>Soldes enregistrés pour ce type de congé :</b>{relevant.length?relevant.map(b=><p key={b.id}>Exercice {b.periode} : acquis {b.soldeAquis??0} j · pris {b.soldePris??0} j · restant {b.soldeRestant??0} j</p>):<p className="text-amber-700">Aucun solde correspondant trouvé. Vérifiez le dossier avant de retourner un reliquat.</p>}</div>
 <label className="form-control max-w-xs"><span className="label-text">Solde exact des exercices concernés</span><input type="number" min="0" className="input input-bordered" value={values[d.id]?.days??""} onChange={e=>setValues({...values,[d.id]:{...values[d.id],days:e.target.value}})}/></label>
 <label className="form-control"><span className="label-text">Observation</span><textarea className="textarea textarea-bordered" value={values[d.id]?.comment??""} onChange={e=>setValues({...values,[d.id]:{...values[d.id],comment:e.target.value}})}/></label>
 <button className="btn btn-primary w-fit" disabled={busy===d.id||values[d.id]?.days===""||values[d.id]?.days==null} onClick={()=>send(d.id)}>{busy===d.id?"Envoi…":"Retourner le reliquat"}</button>
 </article>;})}
 {!rows.length&&<p className="text-slate-500">Aucune demande en attente.</p>}</section>;
}
