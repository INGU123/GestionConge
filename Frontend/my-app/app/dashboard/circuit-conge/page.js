"use client";
import { useEffect, useState } from "react";
import { authFetch } from "@/lib/apiClient";
import { useToast } from "../../Components/ToastProvider";

const API = "http://localhost:8080";
export default function CircuitCongePage() {
  const [employe,setEmploye]=useState(null), [matricule,setMatricule]=useState(""), [lookupBusy,setLookupBusy]=useState(false), [types,setTypes]=useState([]), [dossiers,setDossiers]=useState([]), [services,setServices]=useState([]), [departements,setDepartements]=useState([]);
  const [form,setForm]=useState({employeId:"",typeCongeId:"",serviceId:"",dateDebut:"",dateFin:"",commentaire:""});
  const selectedService=services.find(x=>String(x.id)===String(form.serviceId));
  const selectedDepartment=departements.find(x=>String(x.id)===String(selectedService?.departementId));
  const [busy,setBusy]=useState(false); const toast=useToast();
  const load=async()=>{const [t,d,s,dep]=await Promise.all([
    authFetch(API+"/type-conge/all").then(r=>r.ok?r.json():[]),
    authFetch(API+"/circuit-conge/assistant/mes-dossiers").then(r=>r.ok?r.json():[]),
    authFetch(API+"/services").then(r=>r.ok?r.json():[]),
    authFetch(API+"/departements").then(r=>r.ok?r.json():[])
  ]);setTypes(t);setDossiers(d);setServices(s);setDepartements(dep);};
  useEffect(()=>{let actif=true;Promise.resolve().then(()=>load()).catch(()=>{if(actif)toast("Impossible de charger les données.","error");});return()=>{actif=false;};},[toast]);
  const rechercherEmploye=async()=>{const code=matricule.trim();if(!code)return;setLookupBusy(true);setEmploye(null);setForm({...form,employeId:""});try{const r=await authFetch(API+"/employes/recherche-matricule?matricule="+encodeURIComponent(code));const d=await r.json();if(!r.ok)throw new Error(d.message||"Matricule introuvable");setEmploye(d);setForm(v=>({...v,employeId:String(d.id)}));}catch(e){toast(e.message,"error");}finally{setLookupBusy(false);}};
  const create=async e=>{e.preventDefault();setBusy(true);try{
    const r=await authFetch(API+"/circuit-conge",{method:"POST",body:JSON.stringify({...form,employeId:Number(form.employeId),typeCongeId:Number(form.typeCongeId),serviceId:Number(form.serviceId)})});
    const data=await r.json();if(!r.ok)throw new Error(data.message||"Création impossible");
    toast("Demande de reliquat créée.","success");setEmploye(null);setMatricule("");setForm({employeId:"",typeCongeId:"",serviceId:"",dateDebut:"",dateFin:"",commentaire:""});await load();
  }catch(e){toast(e.message,"error");}finally{setBusy(false);}};
  const submit=async id=>{try{const r=await authFetch(API+`/circuit-conge/${id}/soumettre-chef`,{method:"POST"});const d=await r.json();if(!r.ok)throw new Error(d.message||"Transmission impossible");toast("Dossier transmis au chef de département.","success");await load();}catch(e){toast(e.message,"error");}};
  const download=async id=>{try{const r=await authFetch(API+`/circuit-conge/${id}/pdf`);if(!r.ok)throw new Error("PDF indisponible");const blob=await r.blob();const url=URL.createObjectURL(blob);const a=document.createElement("a");a.href=url;a.download=`decision-conge-${id}.pdf`;a.click();URL.revokeObjectURL(url);}catch(e){toast(e.message,"error");}};
  return <div className="space-y-6">
    <header><h1 className="text-2xl font-bold">Assistant de direction — Circuit des congés</h1><p className="text-sm text-slate-500">Création du dossier verbal, contrôle du reliquat et réception des décisions.</p></header>
    <form onSubmit={create} className="card bg-base-100 border p-5 grid md:grid-cols-2 gap-3">
      <h2 className="font-semibold md:col-span-2">Nouvelle demande de reliquat</h2>
      <div className="form-control"><span className="label-text">Matricule exact du collaborateur</span><div className="flex gap-2"><input className="input input-bordered min-w-0" value={matricule} onChange={e=>{setMatricule(e.target.value);setEmploye(null);setForm(v=>({...v,employeId:""}));}} placeholder="Saisir le matricule" required/><button type="button" className="btn btn-outline" disabled={lookupBusy||!matricule.trim()} onClick={rechercherEmploye}>{lookupBusy?"Recherche…":"Vérifier"}</button></div>{employe&&<p className="text-sm mt-2 text-success">{employe.matricule} — {employe.prenom} {employe.nom}{employe.fonction?" · "+employe.fonction:""}</p>}</div>
      <label className="form-control"><span className="label-text">Type de congé</span><select required className="select select-bordered" value={form.typeCongeId} onChange={e=>setForm({...form,typeCongeId:e.target.value})}><option value="">Choisir un type</option>{types.map(t=><option key={t.id} value={t.id}>{t.libelle||t.code}</option>)}</select></label>
      <label className="form-control"><span className="label-text">Service</span><select required className="select select-bordered" value={form.serviceId} onChange={e=>setForm({...form,serviceId:e.target.value})}><option value="">Choisir un service</option>{services.map(s=><option key={s.id} value={s.id}>{s.nom}</option>)}</select></label>
      <label className="form-control"><span className="label-text">Département associé</span><input readOnly className="input input-bordered" value={selectedDepartment?.nom||(form.serviceId?"Département non défini":"Sélectionner un service")} /></label>
      <label className="form-control"><span className="label-text">Date de début</span><input required type="date" className="input input-bordered" value={form.dateDebut} onChange={e=>setForm({...form,dateDebut:e.target.value})}/></label>
      <label className="form-control"><span className="label-text">Date de fin</span><input required type="date" className="input input-bordered" value={form.dateFin} onChange={e=>setForm({...form,dateFin:e.target.value})}/></label>
      <label className="form-control md:col-span-2"><span className="label-text">Commentaire (facultatif)</span><textarea className="textarea textarea-bordered" value={form.commentaire} onChange={e=>setForm({...form,commentaire:e.target.value})}/></label>
      <button disabled={busy||!form.employeId} className="btn btn-primary md:col-span-2">{busy?"Enregistrement…":"Créer la demande de reliquat"}</button>
    </form>
    <section className="card bg-base-100 border p-5"><h2 className="font-semibold mb-3">Mes dossiers et décisions reçues</h2><div className="overflow-x-auto"><table className="table table-sm"><thead><tr><th>Agent</th><th>Période</th><th>Jours</th><th>Reliquat</th><th>Observation RH</th><th>Statut</th><th>Action</th></tr></thead><tbody>{dossiers.map(d=><tr key={d.id}><td>{d.utilisateurPrenom} {d.utilisateurNom} ({d.utilisateurMatricule})</td><td>{d.dateDebut} → {d.dateFin}</td><td>{d.nombreJours}</td><td>{d.reliquatJours??"—"}</td><td>{d.reliquatCommentaire||"—"}</td><td>{d.statut}</td><td className="flex gap-2">{d.statut==="RELIQUAT_RETOURNE"&&<button className="btn btn-xs btn-primary" disabled={d.reliquatJours < d.nombreJours} onClick={()=>submit(d.id)}>{d.reliquatJours < d.nombreJours ? "Reliquat insuffisant" : "Soumettre au chef"}</button>}{["VALIDEE","REFUSEE"].includes(d.statut)&&<button className="btn btn-xs btn-outline" onClick={()=>download(d.id)}>PDF décision</button>}</td></tr>)}{!dossiers.length&&<tr><td colSpan="7" className="text-center text-slate-400">Aucun dossier.</td></tr>}</tbody></table></div></section>
  </div>;
}
