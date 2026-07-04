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

  let sessionId = storageGet(sessionKey);
  if (!sessionId) {
    sessionId = "sess-" + uuid();
    storageSet(sessionKey, sessionId);
  }

  window.APP_CONFIG = {
    API_BASE_URL: storageGet("civilshop_api_base") || window.location.origin + "/api/v1",
    SESSION_ID: sessionId
  };
})();
