'use client';

import React from 'react';
import { Button } from '@mui/material';
import { Storage as StorageIcon } from '@mui/icons-material';
import { useRouter } from 'next/navigation';

const DBeaverNavigationButton: React.FC = () => {
  const router = useRouter();

  const handleNavigate = () => {
    router.push('/dbeaver');
  };

  return (
    <Button
      variant="outlined"
      startIcon={<StorageIcon />}
      onClick={handleNavigate}
      sx={{
        borderColor: 'white',
        color: 'white',
        textTransform: 'none',
        fontWeight: 500,
        '&:hover': {
          borderColor: 'white',
          backgroundColor: 'rgba(255, 255, 255, 0.1)',
        },
      }}
    >
      DBeaver
    </Button>
  );
};

export default DBeaverNavigationButton;
