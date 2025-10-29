"use client";

import { Button } from "@mui/material";
import { Cloud as CloudIcon } from "@mui/icons-material";
import { useRouter, usePathname } from "next/navigation";

export default function K8sNavigationButton() {
  const router = useRouter();
  const pathname = usePathname();

  const isActive = pathname === "/kubernetes";
  const isHome = pathname === "/" || pathname === "/kronos";

  return (
    <>
      {isHome && (
        <Button
          variant="contained"
          startIcon={<CloudIcon />}
          onClick={() => router.push("/kubernetes")}
          sx={{
            bgcolor: "white",
            color: "#667eea",
            fontWeight: "bold",
            "&:hover": {
              bgcolor: "#f5f5f5",
            },
          }}
        >
          InfraLens
        </Button>
      )}
      {isActive && (
        <Button
          variant="contained"
          startIcon={<CloudIcon />}
          onClick={() => router.push("/")}
          sx={{
            bgcolor: "white",
            color: "#667eea",
            fontWeight: "bold",
            "&:hover": {
              bgcolor: "#f5f5f5",
            },
          }}
        >
          Back to Kronos
        </Button>
      )}
    </>
  );
}
