let appConfig = null;

export async function loadConfig() {
  if (!appConfig) {
    const response = await fetch('/config.json');
    appConfig = await response.json();
  }
  return appConfig;
}
