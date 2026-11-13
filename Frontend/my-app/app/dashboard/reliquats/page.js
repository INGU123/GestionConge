"use client";
import {useEffect,useState} from "react";
import {authFetch} from "@/lib/apiClient";
import {useToast} from "../../components/ToastProvider";
const API="http://localhost:8080";
const disponiblePourType=(soldes)=>soldes.reduce((total,s)=>{
 const acquis=Number(s.soldeAquis??0), pris=Number(s.soldePris??0);
 // Ne pas utiliser soldeRestant : cette colonne peut être obsolète ou initialisée à 0.
 return total+Math.max(0,acquis-pris);
},0);
export default function ReliquatsPage(){
 const [rows,setRows]=useState([]),[balances,setBalances]=useState({}),[values,setValues]=useState({}),[busy,setBusy]=useState(null);const toast=useToast();
 const load=async()=>{
  const r=await authFetch(API+"/circuit-conge/administration/en-attente");if(!r.ok)throw new Error("Chargement impossible");
  const data=await r.json();setRows(data);
  const pairs=await Promise.all(data.map(async d=>{const res=await authFetch(API+"/solde/utilisateur/"+d.utilisateurId);return [d.id,res.ok?await res.json():[]];}));
  const balanceMap=Object.fromEntries(pairs);setBalances(balanceMap);
  setValues(prev=>{
   const next={...prev};
   data.forEach(d=>{
    const matching=(balanceMap[d.id]||[]).filter(b=>String(b.typeCongeId)===String(d.typeCongeId));
    next[d.id]={...next[d.id],days:String(disponiblePourType(matching))};
   });
   return next;
  });
 };
 useEffect(()=>{load().catch(e=>toast(e.message,"error"));},[]);
 const send=async id=>{setBusy(id);try{const v=values[id]||{};const r=await authFetch(API+"/circuit-conge/"+id+"/reliquat",{method:"POST",body:JSON.stringify({reliquatJours:Number(v.days),commentaire:v.comment||""})});const d=await r.json();if(!r.ok)throw new Error(d.message||"Enregistrement impossible");toast("Reliquat vérifié et retourné à l'assistant.","success");await load();}catch(e){toast(e.message,"error");}finally{setBusy(null);}};
 return <section className="space-y-5"><header><h1 className="text-2xl font-bold">Administration du personnel — Vérification des reliquats</h1><p className="text-sm text-slate-500">Le reliquat disponible est calculé automatiquement : jours acquis moins jours déjà pris. Le circuit compte les jours ouvrés, hors week-ends et jours fériés.</p></header>
 {rows.map(d=>{const userBalances=balances[d.id]||[];const relevant=userBalances.filter(b=>String(b.typeCongeId)===String(d.typeCongeId));const disponible=disponiblePourType(relevant);const demande=Number(d.nombreJours||0);const suffisant=disponible>=demande;return <article key={d.id} className="card bg-base-100 border p-5 space-y-3">
 <div className="flex flex-wrap justify-between gap-2"><b>Dossier #{d.id} — {d.utilisateurPrenom} {d.utilisateurNom} ({d.utilisateurMatricule})</b><span>Type #{d.typeCongeId} · {d.dateDebut} → {d.dateFin}</span></div>
 <div className="rounded-lg bg-slate-50 border p-3 text-sm"><b>Calcul du dossier :</b><p>{demande} jour(s) ouvré(s) demandés</p><b className="mt-2 inline-block">Soldes enregistrés pour ce type de congé :</b>{relevant.length?relevant.map(b=><p key={b.id}>Exercice {b.periode} : acquis {b.soldeAquis??0} j · pris {b.soldePris??0} j · restant {Math.max(0,Number(b.soldeAquis??0)-Number(b.soldePris??0))} j</p>):<p className="text-amber-700">Aucun solde correspondant trouvé. Vérifiez et régularisez le solde du collaborateur avant de poursuivre.</p>}<p className="font-semibold mt-2">Total disponible : {disponible} jour(s)</p></div>
 <label className="form-control max-w-xs"><span className="label-text">Reliquat vérifié (calcul automatique)</span><input type="number" min={demande} max={disponible} readOnly className="input input-bordered" value={values[d.id]?.days??disponible}/></label>
 {!suffisant&&<p className="text-sm text-error">Solde insuffisant : {demande} jour(s) ouvré(s) demandés, {disponible} disponible(s). Le dossier ne peut pas être retourné avec 0 jour ou un reliquat insuffisant. Corrigez le solde dans la gestion des soldes avant de poursuivre.</p>}
 <label className="form-control"><span className="label-text">Observation</span><textarea className="textarea textarea-bordered" value={values[d.id]?.comment??""} onChange={e=>setValues({...values,[d.id]:{...values[d.id],comment:e.target.value}})}/></label>
 <button className="btn btn-primary w-fit" disabled={busy===d.id||!suffisant} onClick={()=>send(d.id)}>{busy===d.id?"Envoi…":"Retourner le reliquat à l'assistant"}</button>
 </article>;})}
 {!rows.length&&<p className="text-slate-500">Aucune demande en attente.</p>}</section>;
}
