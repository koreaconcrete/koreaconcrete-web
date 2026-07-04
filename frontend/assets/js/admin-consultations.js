(function () {
  async function list() {
    const root = document.querySelector("#admin-consultations");
    if (!root) return;
    try {
      const status = document.querySelector("#consultation-status-filter")?.value || "";
      const data = await app.request("/admin/consultations?size=50" + (status ? "&status=" + status : ""));
      root.querySelector("tbody").innerHTML = data.items.length ? data.items.map((c) => `
        <tr>
          <td>${app.escapeHtml(c.id)}</td>
          <td>${app.escapeHtml(app.label("consultationType", c.type))}</td>
          <td>${consultationProduct(c)}</td>
          <td>${app.escapeHtml(c.contactName)}</td>
          <td>${app.escapeHtml(c.contactPhone)}</td>
          <td>${app.escapeHtml(c.message || "")}</td>
          <td>${app.escapeHtml(app.label("consultationStatus", c.status))}</td>
          <td><button class="button" data-done="${app.escapeHtml(c.id)}">처리완료</button></td>
        </tr>
      `).join("") : `<tr><td colspan="8" class="empty">조건에 맞는 상담이 없습니다.</td></tr>`;
      root.querySelectorAll("[data-done]").forEach((button) => {
        button.addEventListener("click", async () => {
          try {
            await app.request("/admin/consultations/" + button.dataset.done + "/status", { method: "PATCH", body: { status: "DONE" } });
            app.setState("#admin-consultation-state", "상담 상태가 처리완료로 변경되었습니다.", "notice");
            list();
          } catch (error) {
            app.setState("#admin-consultation-state", error.message, "error");
          }
        });
      });
    } catch (error) {
      app.setState("#admin-consultation-state", error.message, "error");
    }
  }

  function consultationProduct(consultation) {
    const text = [
      consultation.productName,
      consultation.variantName
    ].filter(Boolean).join(" / ") || "일반 상담";
    const badge = consultation.productDeleted ? ' <span class="badge danger">삭제된 상품</span>' : "";
    return app.escapeHtml(text) + badge;
  }

  document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("#consultation-filter-form");
    if (form) form.addEventListener("submit", (event) => { event.preventDefault(); list(); });
    document.querySelector("#consultation-status-filter")?.addEventListener("change", list);
    list();
  });
})();
