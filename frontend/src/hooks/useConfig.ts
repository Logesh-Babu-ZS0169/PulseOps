import { useEffect, useState } from 'react';
import { loadConfig } from '@/lib/config';

export function useConfig() {
  const [config, setConfig] = useState<any>(null);

  useEffect(() => {
    let mounted = true;

    async function fetchConfig() {
      const cfg = await loadConfig();
      if (mounted) setConfig(cfg);
    }

    fetchConfig();

    return () => {
      mounted = false;
    };
  }, []);

  return config;
}
