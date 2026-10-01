import "@naquiz/ui/tokens.css";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { AdminProvider } from "./admin/AdminProvider";
import { MockAdminClient } from "./admin/mock/MockAdminClient";
import { StatsPage } from "./pages/StatsPage";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <AdminProvider client={new MockAdminClient()}>
      <StatsPage />
    </AdminProvider>
  </StrictMode>,
);
