'use client';

import React from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { Button } from '@mui/material';
import SpeedIcon from '@mui/icons-material/Speed';

export default function PostgresOptimizerNavigationButton() {
  const router = useRouter();
  const pathname = usePathname();

  const handleNavigate = () => {
    router.push('/postgres-optimizer');
  };

  if (pathname === '/postgres-optimizer') {
    return null;
  }

  return (
    <Button
      variant="contained"
      startIcon={<SpeedIcon />}
      onClick={handleNavigate}
      sx={{
        bgcolor: 'white',
        color: '#667eea',
        fontWeight: 600,
        px: 3,
        py: 1,
        '&:hover': {
          bgcolor: '#f5f5f5',
        },
      }}
    >
      PG Optimizer
    </Button>
  );
}
