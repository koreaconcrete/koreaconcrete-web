(function () {
  const sessionKey = "civilshop_session_id";

  function storageGet(key) {
    try {
      return window.localStorage.getItem(key);
    } catch (error) {
      return null;
    }
  }

  function storageSet(key, value) {
    try {
      window.localStorage.setItem(key, value);
    } catch (error) {
      // Safari private mode or embedded browsers can block localStorage.
    }
  }

  function storageRemove(key) {
    try {
      window.localStorage.removeItem(key);
    } catch (error) {
      // Ignore storage access errors.
    }
  }

  function uuid() {
    if (window.crypto && typeof window.crypto.randomUUID === "function") {
      return window.crypto.randomUUID();
    }
    return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (char) => {
      const value = Math.floor(Math.random() * 16);
      const next = char === "x" ? value : (value & 0x3) | 0x8;
      return next.toString(16);
    });
  }

  function isLocalHost() {
    return ["localhost", "127.0.0.1", ""].includes(window.location.hostname);
  }

  function apiBaseUrl() {
    const fallback = window.location.origin + "/api/v1";
    const stored = storageGet("civilshop_api_base");
    if (!stored) return fallback;
    try {
      const url = new URL(stored, window.location.origin);
      if (isLocalHost() || url.origin === window.location.origin) {
        return url.href.replace(/\/$/, "");
      }
    } catch (error) {
      // Invalid override values should fall back to the current origin.
    }
    storageRemove("civilshop_api_base");
    return fallback;
  }

  let sessionId = storageGet(sessionKey);
  if (!sessionId) {
    sessionId = "sess-" + uuid();
    storageSet(sessionKey, sessionId);
  }

  window.APP_CONFIG = {
    API_BASE_URL: apiBaseUrl(),
    SESSION_ID: sessionId
  };
})();
