"use client";
import {useEffect,useState} from "react";
import {getMesDemandes} from "@/app/api/demandeConge/demandeConge";
import {getAllTypeConge} from "@/app/api/typeConge/typeConge";
import {getCurrentUser} from "@/lib/apiClient";
import {useToast} from "../../components/ToastProvider";

export default function DemandesPage(){
 const [rows,setRows]=useState([]),[types,setTypes]=useState([]),[loading,setLoading]=useState(true);const toast=useToast();
 useEffect(()=>{const load=async()=>{try{const user=getCurrentUser();if(!user?.id)throw new Error("Session utilisateur introuvable.");const [d,t]=await Promise.all([getMesDemandes(user.id),getAllTypeConge()]);setRows(Array.isArray(d)?d:[]);setTypes(Array.isArray(t)?t:[]);}catch(e){toast(e.message||"Chargement impossible.","error");}finally{setLoading(false);}};load();},[]);
 const typeName=id=>{const t=types.find(x=>String(x.id)===String(id));return t?.libelle||t?.code||`Type #${id}`;};
 return <section className="space-y-5"><header><h1 className="text-2xl font-bold">Mes demandes de congé</h1><p className="text-sm text-slate-500">Consultation uniquement. Toute demande verbale est enregistrée par l'assistant de direction.</p></header><div className="card bg-base-100 border p-5 overflow-x-auto"><table className="table table-sm"><thead><tr><th>Type</th><th>Période</th><th>Jours</th><th>Statut</th><th>Motif de refus</th></tr></thead><tbody>{rows.map(d=><tr key={d.id}><td>{typeName(d.typeCongeId)}</td><td>{d.dateDebut} → {d.dateFin}</td><td>{d.nombreJours}</td><td>{d.statut}</td><td>{d.motifRefus||"—"}</td></tr>)}{!loading&&!rows.length&&<tr><td colSpan="5" className="text-center text-slate-400">Aucune demande enregistrée.</td></tr>}</tbody></table>{loading&&<p className="text-center p-5">Chargement…</p>}</div></section>;
}
