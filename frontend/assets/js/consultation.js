(function () {
  document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("#consultation-form");
    if (!form) return;
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const data = Object.fromEntries(new FormData(form).entries());
      data.privacyAgreed = form.privacyAgreed.checked;
      try {
        await app.request("/consultations", { method: "POST", body: data });
        app.notify("상담요청이 접수되었습니다.");
        app.setState("#consultation-state", "", "");
        form.reset();
      } catch (error) {
        app.setState("#consultation-state", error.message, "error");
      }
    });
  });
})();
