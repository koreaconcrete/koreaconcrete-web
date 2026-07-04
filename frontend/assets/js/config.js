(function () {
  const sessionKey = "civilshop_session_id";
  let sessionId = localStorage.getItem(sessionKey);
  if (!sessionId) {
    sessionId = "sess-" + crypto.randomUUID();
    localStorage.setItem(sessionKey, sessionId);
  }

  window.APP_CONFIG = {
    API_BASE_URL: localStorage.getItem("civilshop_api_base") || window.location.origin + "/api/v1",
    SESSION_ID: sessionId
  };
})();
