'use client';

import React from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { Button } from '@mui/material';
import AnalyticsIcon from '@mui/icons-material/Analytics';

export default function DatabaseAnalyticsNavigationButton() {
  const router = useRouter();
  const pathname = usePathname();

  const handleNavigate = () => {
    router.push('/database-analytics');
  };

  if (pathname === '/database-analytics') {
    return null;
  }

  return (
    <Button
      variant="contained"
      startIcon={<AnalyticsIcon />}
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
      DB Analytics
    </Button>
  );
}
