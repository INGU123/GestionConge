"use client";
import {useEffect,useState} from "react";
import {authFetch,getCurrentUser} from "@/lib/apiClient";
const API="http://localhost:8080";
export default function ConsultationDemandesPage(){
 const [rows,setRows]=useState([]),[loading,setLoading]=useState(true),[role]=useState(()=>getCurrentUser()?.role);
 useEffect(()=>{authFetch(API+"/conge/all").then(async r=>{if(!r.ok)throw new Error("Consultation non autorisée");setRows(await r.json());}).catch(()=>setRows([])).finally(()=>setLoading(false));},[]);
 return <section className="space-y-5"><header><h1 className="text-2xl font-bold">Consultation des demandes de congé</h1><p className="text-sm text-slate-500">{role==="ADMIN"?"Vue globale en lecture seule : demandes déposées et décisions enregistrées.":"Demandes déposées par les collaborateurs de votre équipe. Consultation uniquement."}</p></header><div className="card bg-base-100 border p-5 overflow-x-auto"><table className="table table-sm"><thead><tr><th>Collaborateur</th><th>Type</th><th>Période</th><th>Jours</th><th>Statut</th><th>Motif de refus</th></tr></thead><tbody>{rows.map(d=><tr key={d.id}><td>#{d.utilisateurId}</td><td>#{d.typeCongeId}</td><td>{d.dateDebut} → {d.dateFin}</td><td>{d.nombreJours}</td><td>{d.statut}</td><td>{d.motifRefus||"—"}</td></tr>)}{!loading&&!rows.length&&<tr><td colSpan="6" className="text-center text-slate-400">Aucune demande à afficher.</td></tr>}</tbody></table>{loading&&<p className="text-center p-6">Chargement…</p>}</div></section>;
}
