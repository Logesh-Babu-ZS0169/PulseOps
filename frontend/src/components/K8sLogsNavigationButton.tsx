"use client";

import { Button } from "@mui/material";
import { Assessment as AssessmentIcon } from "@mui/icons-material";
import { useRouter, usePathname } from "next/navigation";

export default function K8sLogsNavigationButton() {
  const router = useRouter();
  const pathname = usePathname();

  const isActive = pathname === "/k8s-logs";
  const isHome = pathname === "/" || pathname === "/kronos";
  const isK8s = pathname === "/kubernetes";

  return (
    <>
      {(isHome || isK8s) && (
        <Button
          variant="contained"
          startIcon={<AssessmentIcon />}
          onClick={() => router.push("/k8s-logs")}
          sx={{
            bgcolor: "white",
            color: "#667eea",
            fontWeight: "bold",
            ml: 2,
            "&:hover": {
              bgcolor: "#f5f5f5",
            },
          }}
        >
          Nuvora
        </Button>
      )}
      {isActive && (
        <Button
          variant="contained"
          startIcon={<AssessmentIcon />}
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
