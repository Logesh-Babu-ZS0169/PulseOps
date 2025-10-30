"use client";

import { Button } from "@mui/material";
import { BarChart as BarChartIcon } from "@mui/icons-material";
import { useRouter, usePathname } from "next/navigation";

export default function MetricsNavigationButton() {
  const router = useRouter();
  const pathname = usePathname();

  const isActive = pathname === "/metrics";
  const isHome = pathname === "/" || pathname === "/kronos";
  const isK8s = pathname === "/kubernetes";

  return (
    <>
      {(isHome || isK8s) && (
        <Button
          variant="contained"
          startIcon={<BarChartIcon />}
          onClick={() => router.push("/metrics")}
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
          Sysora
        </Button>
      )}
      {isActive && (
        <Button
          variant="contained"
          startIcon={<BarChartIcon />}
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
