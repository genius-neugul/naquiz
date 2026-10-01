import { createContext, useContext, type ReactNode } from "react";
import type { AdminClient } from "./AdminClient";

const AdminClientContext = createContext<AdminClient | null>(null);

export function AdminProvider({ client, children }: { client: AdminClient; children: ReactNode }) {
  return <AdminClientContext.Provider value={client}>{children}</AdminClientContext.Provider>;
}

export function useAdminClient(): AdminClient {
  const client = useContext(AdminClientContext);
  if (!client) throw new Error("AdminProvider 안에서만 쓸 수 있어요");
  return client;
}
