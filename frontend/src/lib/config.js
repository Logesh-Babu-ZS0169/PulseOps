let appConfig = null;

export async function loadConfig() {
  if (!appConfig) {
    const basePath = process.env.__NEXT_ROUTER_BASEPATH || '';
    const response = await fetch(`${basePath}/config.json`);
    appConfig = await response.json();
  }
  return appConfig;
}
