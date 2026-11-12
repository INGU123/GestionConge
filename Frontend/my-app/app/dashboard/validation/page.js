"use client";
import {useEffect} from "react";
import {useRouter} from "next/navigation";
import {getCurrentUser} from "@/lib/apiClient";
export default function ValidationPage(){
 const router=useRouter();
 useEffect(()=>{const role=String(getCurrentUser()?.role||"").toUpperCase();router.replace(role==="ADMIN"||role==="MANAGER"?"/dashboard/consultation-demandes":"/dashboard");},[router]);
 return <div className="p-8 text-slate-500">L'ancien écran de validation est remplacé par le circuit hiérarchique.</div>;
}
