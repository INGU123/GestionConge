"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

export default function DemandesPage() {
  const router = useRouter();

  useEffect(() => {
    router.replace("/dashboard");
  }, [router]);

  return (
    <div className="flex min-h-[40vh] items-center justify-center">
      <span className="loading loading-spinner loading-md text-primary" />
    </div>
  );
}
