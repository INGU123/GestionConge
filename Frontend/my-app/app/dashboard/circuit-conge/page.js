"use client";
import { useEffect, useState } from "react";
import { authFetch } from "@/lib/apiClient";
import { useToast } from "../../Components/ToastProvider";

const API = "http://localhost:8080";
export default function CircuitCongePage() {
  const [employe,setEmploye]=useState(null), [matricule,setMatricule]=useState(""), [lookupBusy,setLookupBusy]=useState(false), [types,setTypes]=useState([]), [dossiers,setDossiers]=useState([]);
  const [form,setForm]=useState({employeId:"",utilisateurId:"",typeCongeId:"",dateDebut:"",dateFin:"",commentaire:""});
  const [busy,setBusy]=useState(false); const [scan,setScan]=useState(null); const [pendingScan,setPendingScan]=useState(null); const toast=useToast();
  const load=async()=>{const [t,d]=await Promise.all([
    authFetch(API+"/type-conge/all").then(r=>r.ok?r.json():[]),
    authFetch(API+"/circuit-conge/assistant/mes-dossiers").then(r=>r.ok?r.json():[])
  ]);setTypes(t);setDossiers(d);};
  useEffect(()=>{let actif=true;Promise.resolve().then(()=>load()).catch(()=>{if(actif)toast("Impossible de charger les données.","error");});return()=>{actif=false;};},[toast]);
  const lookupEmploye=async()=>{
    const code=matricule.trim();
    if(!code || !/^[0-9]+$/.test(code)){
      setEmploye(null);
      setForm(v=>({...v,employeId:"",utilisateurId:""}));
      toast("Saisissez un matricule composé uniquement de chiffres.","warning");
      return;
    }
    setLookupBusy(true);
    setEmploye(null);
    setForm(v=>({...v,employeId:"",utilisateurId:""}));
    try{
      const r=await authFetch(API+"/employes/recherche-matricule?matricule="+encodeURIComponent(code));
      const d=await r.json();
      if(!r.ok)throw new Error(d.message||"Matricule introuvable");
      setEmploye(d);
      setForm(v=>({...v,employeId:d.typeCible==="EMPLOYE"?String(d.id):"",utilisateurId:d.typeCible==="COLLABORATEUR"?String(d.id):""}));
    }catch(error){
      toast(error.message,"error");
    }finally{
      setLookupBusy(false);
    }
  };
  const uploadScan=async(id,file)=>{
    const body=new FormData();
    body.append("fichier",file);
    const response=await authFetch(API+`/circuit-conge/${id}/piece-jointe`,{method:"POST",body});
    const data=await response.json().catch(()=>({}));
    if(!response.ok)throw new Error(data.message||"Envoi du scan impossible");
  };

  const retryScan=async()=>{
    if(!pendingScan)return;
    setBusy(true);
    try{
      await uploadScan(pendingScan.id,pendingScan.file);
      setPendingScan(null);
      toast("Scan joint au dossier #"+pendingScan.id+".","success");
      await load();
    }catch(error){toast(error.message,"error");}
    finally{setBusy(false);}
  };

  const create=async e=>{
    e.preventDefault();
    if(!scan){toast("Sélectionnez le scan du formulaire papier.","warning");return;}
    if(scan.size>10*1024*1024){toast("Le scan dépasse 10 Mo.","error");return;}
    setBusy(true);
    try{
      const r=await authFetch(API+"/circuit-conge",{method:"POST",body:JSON.stringify({...form,
        employeId:form.employeId?Number(form.employeId):null,
        utilisateurId:form.utilisateurId?Number(form.utilisateurId):null,typeCongeId:Number(form.typeCongeId)})});
      const data=await r.json();
      if(!r.ok)throw new Error(data.message||"Création impossible");
      if(!data.id)throw new Error("Identifiant du dossier absent de la réponse.");
      setEmploye(null);
      setMatricule("");
      setForm({employeId:"",utilisateurId:"",typeCongeId:"",dateDebut:"",dateFin:"",commentaire:""});
      try{
        await uploadScan(data.id,scan);
        setScan(null);
        setPendingScan(null);
        toast("Demande créée et scan joint au dossier #"+data.id+".","success");
      }catch(error){
        setScan(null);
        setPendingScan({id:data.id,file:scan});
        toast("Dossier créé, mais scan non envoyé : "+error.message,"warning");
      }
      await load();
    }catch(error){toast(error.message,"error");}
    finally{setBusy(false);}
  };
  const submit=async id=>{try{const r=await authFetch(API+`/circuit-conge/${id}/soumettre-chef`,{method:"POST"});const d=await r.json();if(!r.ok)throw new Error(d.message||"Transmission impossible");toast("Dossier transmis au chef de département.","success");await load();}catch(e){toast(e.message,"error");}};
  const download=async id=>{try{const r=await authFetch(API+`/circuit-conge/${id}/pdf`);if(!r.ok)throw new Error("PDF indisponible");const blob=await r.blob();const url=URL.createObjectURL(blob);const a=document.createElement("a");a.href=url;a.download=`decision-conge-${id}.pdf`;a.click();URL.revokeObjectURL(url);}catch(e){toast(e.message,"error");}};
  return <div className="circuit-conge-theme space-y-6">
    <header className="space-y-1">
      <p className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">Espace Assistant de direction</p>
      <h1 className="text-2xl font-bold tracking-tight">Circuit des congés</h1>
      <p className="text-sm text-base-content/60">Création et suivi des dossiers de demande de reliquat.</p>
    </header>

    <form onSubmit={create} className="overflow-hidden rounded-2xl border border-base-300 bg-base-100 shadow-sm">
      <div className="border-b border-base-300 bg-base-200/60 px-5 py-4 sm:px-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-bold text-base-content">Nouvelle demande de reliquat</h2>
            <p className="mt-1 text-sm leading-relaxed text-base-content/70">Les informations administratives du collaborateur sont chargées depuis le référentiel.</p>
          </div>
          <span className="rounded-full border border-primary/20 bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">Nouveau dossier</span>
        </div>
      </div>

      <div className="space-y-6 p-5 sm:p-6">
        <section className="space-y-3">
          <div className="flex items-center gap-2">
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary/10 text-sm font-bold text-primary">1</span>
            <h3 className="font-semibold">Identification du collaborateur</h3>
          </div>
          <div className="grid gap-4 rounded-xl border border-base-300 p-4 md:grid-cols-3">
            <div className="flex items-end gap-2">
              <label className="form-control min-w-0 flex-1">
                <span className="mb-2 text-sm font-medium">Matricule exact <span className="text-error">*</span></span>
                <input className="input input-bordered w-full font-mono tracking-wide" value={matricule} onChange={e=>{setMatricule(e.target.value.replace(/[^0-9]/g,""));setEmploye(null);setForm(v=>({...v,employeId:""}));}} placeholder="Ex. 2026100" inputMode="numeric" pattern="[0-9]+" required autoComplete="off"/>
                <span className="mt-1.5 text-xs text-base-content/55">Saisissez le matricule puis cliquez sur Vérifier.</span>
              </label>
              <button type="button" className="btn btn-primary" onClick={lookupEmploye} disabled={lookupBusy||!matricule.trim()}>
                {lookupBusy?"...":"Vérifier"}
              </button>
            </div>
            <div className="rounded-lg bg-base-200/70 p-3 md:col-span-2">
              {employe ? <>
                <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-success"><span className="h-2 w-2 rounded-full bg-success"></span>{employe.typeCible==="EMPLOYE"?"Employé identifié":"Collaborateur identifié"}</div>
                <p className="mt-1 text-base font-bold">{employe.prenom} {employe.nom}</p>
                <p className="text-xs text-base-content/60">{employe.matricule}{employe.fonction ? " · "+employe.fonction : ""}{employe.role ? " · "+employe.role : ""}</p>
              </> : <div className="flex min-h-[62px] items-center text-sm text-base-content/55">{lookupBusy?"Vérification en cours…":"Le nom et les affectations apparaîtront ici après identification."}</div>}
            </div>
            <div className="rounded-lg border border-base-300 p-3">
              <p className="text-xs font-medium text-base-content/55">Service d’affectation</p>
              <p className="mt-1 font-semibold">{employe?.serviceNom||"—"}</p>
              <p className="mt-1 text-xs text-base-content/45">Récupéré automatiquement</p>
            </div>
            <div className="rounded-lg border border-base-300 p-3 md:col-span-2">
              <p className="text-xs font-medium text-base-content/55">Département associé</p>
              <p className="mt-1 font-semibold">{employe?.departementNom||"—"}</p>
              <p className="mt-1 text-xs text-base-content/45">Aucune sélection manuelle nécessaire</p>
            </div>
          </div>
        </section>

        <section className="space-y-3">
          <div className="flex items-center gap-2">
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary/10 text-sm font-bold text-primary">2</span>
            <h3 className="font-semibold">Informations du congé</h3>
          </div>
          <div className="grid gap-4 md:grid-cols-3">
            <label className="form-control md:col-span-3">
              <span className="mb-2 text-sm font-medium">Type de congé <span className="text-error">*</span></span>
              <select required className="select select-bordered w-full" value={form.typeCongeId} onChange={e=>setForm(v=>({...v,typeCongeId:e.target.value}))}>
                <option value="">Choisir le type de congé</option>
                {types.map(t=><option key={t.id} value={t.id}>{t.libelle||t.code}</option>)}
              </select>
            </label>
            <label className="form-control">
              <span className="mb-2 text-sm font-medium">Date de début <span className="text-error">*</span></span>
              <input required type="date" className="input input-bordered w-full" value={form.dateDebut} onChange={e=>setForm(v=>({...v,dateDebut:e.target.value}))}/>
            </label>
            <label className="form-control">
              <span className="mb-2 text-sm font-medium">Date de fin <span className="text-error">*</span></span>
              <input required min={form.dateDebut||undefined} type="date" className="input input-bordered w-full" value={form.dateFin} onChange={e=>setForm(v=>({...v,dateFin:e.target.value}))}/>
            </label>
            <label className="form-control md:col-span-3">
              <span className="mb-2 text-sm font-medium">Commentaire <span className="font-normal text-base-content/45">(facultatif)</span></span>
              <textarea rows="3" maxLength="1500" className="textarea textarea-bordered w-full" placeholder="Ajouter une précision utile au traitement du dossier…" value={form.commentaire} onChange={e=>setForm(v=>({...v,commentaire:e.target.value}))}/>
            </label>
          </div>
        </section>

        <section className="space-y-3">
          <div className="flex items-center gap-2">
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary/10 text-sm font-bold text-primary">3</span>
            <h3 className="font-semibold">Formulaire papier</h3>
          </div>
          <div className="rounded-xl border border-dashed border-base-300 bg-base-200/30 p-4 sm:p-5">
            <div className="flex flex-col items-start gap-4 sm:flex-row sm:items-center">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" className="h-6 w-6"><path strokeLinecap="round" strokeLinejoin="round" d="M12 16V4m0 0L7.5 8.5M12 4l4.5 4.5M4 16.5v2A1.5 1.5 0 005.5 20h13a1.5 1.5 0 001.5-1.5v-2"/></svg>
              </div>
              <div className="min-w-0 flex-1">
                <p className="font-semibold">Joindre le formulaire papier scanné <span className="text-error">*</span></p>
                <p className="mt-1 text-xs text-base-content/55">Formats acceptés : PDF, JPG ou PNG · Taille maximale : 10 Mo</p>
                <p className="mt-2 truncate text-sm text-base-content/75">{scan?scan.name:"Aucun fichier sélectionné"}</p>
              </div>
              <label className="relative inline-flex shrink-0 cursor-pointer items-center gap-2 rounded-lg border border-base-300 bg-base-100 px-4 py-2.5 text-sm font-semibold shadow-sm transition hover:border-primary hover:bg-primary/5">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className="h-4 w-4"><path strokeLinecap="round" strokeLinejoin="round" d="M12 16V4m0 0L7.5 8.5M12 4l4.5 4.5M4 16.5v2A1.5 1.5 0 005.5 20h13a1.5 1.5 0 001.5-1.5v-2"/></svg>
                {scan?"Remplacer le fichier":"Choisir un fichier"}
                <input type="file" accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png" required className="absolute inset-0 h-full w-full cursor-pointer opacity-0" onChange={e=>{const file=e.target.files?.[0]||null;if(file&&file.size>10*1024*1024){toast("Le scan dépasse 10 Mo.","error");e.target.value="";setScan(null);}else setScan(file);}}/>
              </label>
            </div>
          </div>
        </section>

        <div className="flex flex-col-reverse gap-3 border-t border-base-300 pt-5 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-xs text-base-content/50">Vérifiez les informations avant la création du dossier.</p>
          <button disabled={busy||lookupBusy||(!form.employeId&&!form.utilisateurId)||!form.typeCongeId||!scan} className="btn btn-primary min-w-56">
            {busy?"Enregistrement…":"Créer la demande et joindre le scan"}
          </button>
        </div>
        {pendingScan&&<div className="rounded-lg border border-warning/50 bg-warning/10 p-3 text-sm"><p>Le dossier #{pendingScan.id} existe, mais son scan reste à envoyer.</p><button type="button" className="btn btn-sm btn-warning mt-2" disabled={busy} onClick={retryScan}>Réessayer l’envoi du scan</button></div>}
      </div>
    </form>
    <section className="card bg-base-100 border p-5"><h2 className="font-semibold mb-3">Mes dossiers et décisions reçues</h2><div className="overflow-x-auto"><table className="table table-sm"><thead><tr><th>Agent</th><th>Période</th><th>Jours</th><th>Reliquat</th><th>Observation RH</th><th>Statut</th><th>Action</th></tr></thead><tbody>{dossiers.map(d=><tr key={d.id}><td>{d.utilisateurPrenom} {d.utilisateurNom} ({d.utilisateurMatricule})</td><td>{d.dateDebut} → {d.dateFin}</td><td>{d.nombreJours}</td><td>{d.reliquatJours??"—"}</td><td>{d.reliquatCommentaire||"—"}</td><td>{d.statut}</td><td className="flex gap-2">{d.statut==="RELIQUAT_RETOURNE"&&<button className="btn btn-xs btn-primary" disabled={d.reliquatJours < d.nombreJours} onClick={()=>submit(d.id)}>{d.reliquatJours < d.nombreJours ? "Reliquat insuffisant" : "Soumettre au chef"}</button>}{["VALIDEE","REFUSEE"].includes(d.statut)&&<button className="btn btn-xs btn-outline" onClick={()=>download(d.id)}>PDF décision</button>}</td></tr>)}{!dossiers.length&&<tr><td colSpan="7" className="text-center text-slate-400">Aucun dossier.</td></tr>}</tbody></table></div></section>
  </div>;
}
