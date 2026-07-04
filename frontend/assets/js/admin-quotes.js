(function () {
  const quoteStatusValues = ["SUBMITTED", "REVIEWING", "QUOTED", "NEGOTIATING", "APPROVED", "REJECTED", "EXPIRED"];

  async function list() {
    const root = document.querySelector("#admin-quotes");
    if (!root) return;
    try {
      const status = document.querySelector("#quote-status-filter")?.value || "";
      const data = await app.request("/admin/quotes?size=50" + (status ? "&status=" + status : ""));
      root.querySelector("tbody").innerHTML = data.items.length ? data.items.map((q) => app.html`
        <tr>
          <td>${q.requestNo}</td>
          <td>${q.companyName}</td>
          <td>${q.contactName}</td>
          <td>${app.label("quoteStatus", q.status)}</td>
          <td><a class="button" href="admin-quote-detail.html?id=${q.id}">상세</a></td>
        </tr>
      `).join("") : `<tr><td colspan="5" class="empty">조건에 맞는 견적이 없습니다.</td></tr>`;
    } catch (error) {
      app.setState("#admin-quote-state", error.message, "error");
    }
  }

  async function detail() {
    const root = document.querySelector("#admin-quote-detail");
    if (!root) return;
    try {
      const quote = await app.request("/admin/quotes/" + app.qs("id"));
      root.innerHTML = `
        <div class="card grid">
          <h2>${app.escapeHtml(quote.requestNo)}</h2>
          <p>${app.escapeHtml(quote.companyName)} · ${app.escapeHtml(quote.contactName)} · ${app.escapeHtml(quote.contactPhone)}</p>
          <p class="muted">${app.escapeHtml(quote.siteAddress)}</p>
          <select id="quote-status" class="input">
            ${quoteStatusValues.map((status) => `<option value="${status}"${status === quote.status ? " selected" : ""}>${app.escapeHtml(app.label("quoteStatus", status))}</option>`).join("")}
          </select>
          <button id="save-status" class="button primary">상태 저장</button>
          <div id="admin-quote-detail-state"></div>
        </div>
        <table class="table"><thead><tr><th>상품</th><th>규격</th><th>수량</th><th>단가</th></tr></thead><tbody>${quote.items.map((item) => `
          <tr>
            <td>${quoteProductCell(item)}</td>
            <td>${app.escapeHtml(item.variantName)}</td>
            <td>${app.escapeHtml(item.quantity)}</td>
            <td>${app.escapeHtml(app.money(item.unitPrice))}</td>
          </tr>
        `).join("")}</tbody></table>
      `;
      document.querySelector("#save-status").addEventListener("click", async () => {
        try {
          await app.request("/admin/quotes/" + quote.id + "/status", { method: "PATCH", body: { status: document.querySelector("#quote-status").value } });
          app.setState("#admin-quote-detail-state", "상태가 저장되었습니다.", "notice");
        } catch (error) {
          app.setState("#admin-quote-detail-state", error.message, "error");
        }
      });
    } catch (error) {
      app.setState(root, error.message, "error");
    }
  }

  function quoteProductCell(item) {
    const image = app.escapeHtml(item.productImageUrl || "assets/images/placeholder.svg");
    const name = app.escapeHtml(item.productName || "상품명 없음");
    const summary = app.escapeHtml(item.productSummary || "");
    const badge = item.productDeleted ? '<span class="badge danger">삭제된 상품</span>' : "";
    const content = `
      <img class="line-item-thumb" src="${image}" alt="${name}">
      <span class="line-item-copy">
        <strong>${name}</strong>
        <span>${summary}</span>
        ${badge}
      </span>
    `;
    if (item.productDeleted) {
      return `<div class="line-item is-disabled">${content}</div>`;
    }
    return `<a class="line-item" href="product-detail.html?id=${app.escapeHtml(item.productId)}">${content}</a>`;
  }

  document.addEventListener("DOMContentLoaded", () => {
    const filter = document.querySelector("#quote-filter-form");
    if (filter) filter.addEventListener("submit", (event) => { event.preventDefault(); list(); });
    document.querySelector("#quote-status-filter")?.addEventListener("change", list);
    list();
    detail();
  });
})();
