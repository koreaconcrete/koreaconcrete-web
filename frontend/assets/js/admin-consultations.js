(function () {
  const consultationPageSize = 20;
  const consultationPages = { active: 1, done: 1 };

  async function list() {
    const root = document.querySelector("#admin-consultations");
    if (!root) return;
    try {
      const bucket = document.querySelector("#consultation-status-filter")?.value || "";
      if (bucket === "DONE") {
        renderRows(root.querySelector("#consultation-active-body"), [], "조건에 맞는 진행 중 상담이 없습니다.");
        renderPagination("#consultation-active-pagination", "active", emptyPage());
        const doneData = await fetchConsultations("DONE", consultationPages.done);
        renderRows(root.querySelector("#consultation-done-body"), doneData.items || [], "조건에 맞는 처리 완료 상담이 없습니다.");
        renderPagination("#consultation-done-pagination", "done", doneData);
        bindStatusSelects(root);
        return;
      }
      const activeData = await fetchConsultations(bucket || "ACTIVE", consultationPages.active);
      renderRows(root.querySelector("#consultation-active-body"), activeData.items || [], "조건에 맞는 진행 중 상담이 없습니다.");
      renderPagination("#consultation-active-pagination", "active", activeData);
      if (bucket) {
        renderRows(root.querySelector("#consultation-done-body"), [], "조건에 맞는 처리 완료 상담이 없습니다.");
        renderPagination("#consultation-done-pagination", "done", emptyPage());
        bindStatusSelects(root);
        return;
      }
      const doneData = await fetchConsultations("DONE", consultationPages.done);
      renderRows(root.querySelector("#consultation-done-body"), doneData.items || [], "조건에 맞는 처리 완료 상담이 없습니다.");
      renderPagination("#consultation-done-pagination", "done", doneData);
      bindStatusSelects(root);
    } catch (error) {
      app.setState("#admin-consultation-state", error.message, "error");
    }
  }

  async function fetchConsultations(bucket, page) {
    const params = new URLSearchParams({
      bucket,
      page: String(page),
      size: String(consultationPageSize)
    });
    return app.request("/admin/consultations?" + params.toString());
  }

  function emptyPage() {
    return { page: 1, size: consultationPageSize, total: 0 };
  }

  function renderRows(body, items, emptyMessage) {
    if (!body) return;
    body.innerHTML = items.length ? items.map((c) => `
        <tr>
          <td>${app.escapeHtml(c.id)}</td>
          <td>${app.escapeHtml(app.label("consultationType", c.type))}</td>
          <td>${consultationProduct(c)}</td>
          <td>${app.escapeHtml(c.contactName)}</td>
          <td>${app.escapeHtml(c.contactPhone)}</td>
          <td>${app.escapeHtml(c.message || "")}</td>
          <td>${consultationStatusSelect(c)}</td>
        </tr>
      `).join("") : `<tr><td colspan="7" class="empty">${emptyMessage}</td></tr>`;
  }

  function renderPagination(selector, target, data) {
    const container = document.querySelector(selector);
    if (!container) return;
    container.innerHTML = app.paginationControls(data, {
      size: consultationPageSize,
      pageAttributes: (page) => `data-consultation-page-target="${app.escapeHtml(target)}" data-page="${page}"`
    });
  }

  function consultationStatusSelect(consultation) {
    const value = statusValueForBucket(app.workflowBucket("consultation", consultation.status));
    return `
      <select class="input admin-status-select" data-status-select="${app.escapeHtml(consultation.id)}" aria-label="상담 상태">
        ${statusOption("NEW", value, "신규")}
        ${statusOption("IN_PROGRESS", value, "처리 중")}
        ${statusOption("DONE", value, "처리 완료")}
      </select>
    `;
  }

  function statusOption(value, selected, label) {
    return `<option value="${value}"${value === selected ? " selected" : ""}>${label}</option>`;
  }

  function statusValueForBucket(bucket) {
    if (bucket === "NEW") return "NEW";
    if (bucket === "DONE") return "DONE";
    return "IN_PROGRESS";
  }

  function bindStatusSelects(root) {
    root.querySelectorAll("[data-status-select]").forEach((select) => {
      select.addEventListener("change", async () => {
        const previousValue = select.dataset.previousValue || select.value;
        select.disabled = true;
        try {
          await app.request("/admin/consultations/" + select.dataset.statusSelect + "/status", { method: "PATCH", body: { status: select.value } });
          app.setState("#admin-consultation-state", "상담 상태가 변경되었습니다.", "notice");
          list();
        } catch (error) {
          select.value = previousValue;
          select.disabled = false;
          app.setState("#admin-consultation-state", error.message, "error");
        }
      });
      select.dataset.previousValue = select.value;
    });
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
    if (form) form.addEventListener("submit", (event) => { event.preventDefault(); resetPages(); list(); });
    document.querySelector("#consultation-status-filter")?.addEventListener("change", () => { resetPages(); list(); });
    document.querySelector("#admin-consultations")?.addEventListener("click", async (event) => {
      const button = event.target.closest("button[data-consultation-page-target]");
      if (!button) return;
      const target = button.dataset.consultationPageTarget;
      const nextPage = Number(button.dataset.page);
      if (!target || !nextPage || consultationPages[target] === nextPage) return;
      consultationPages[target] = nextPage;
      button.blur();
      await list();
      app.scrollToElement(consultationSection(target));
    });
    list();
  });

  function consultationSection(target) {
    return document.querySelector(`#consultation-${target}-pagination`)?.closest(".admin-list-section");
  }

  function resetPages() {
    consultationPages.active = 1;
    consultationPages.done = 1;
  }
})();
