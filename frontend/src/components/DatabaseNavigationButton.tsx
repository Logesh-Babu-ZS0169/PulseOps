'use client';

import React from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { Button } from '@mui/material';
import StorageIcon from '@mui/icons-material/Storage';

export default function DatabaseNavigationButton() {
  const router = useRouter();
  const pathname = usePathname();

  const handleNavigate = () => {
    router.push('/database');
  };

  if (pathname === '/database') {
    return null;
  }

  return (
    <Button
      variant="contained"
      startIcon={<StorageIcon />}
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
      Database Manager
    </Button>
  );
}
