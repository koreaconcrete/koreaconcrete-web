(function () {
  const quoteStatusValues = ["SUBMITTED", "REVIEWING", "APPROVED"];
  const quoteStatusOptionLabels = {
    SUBMITTED: "신규",
    REVIEWING: "처리 중",
    APPROVED: "처리 완료"
  };
  const quotePageSize = 20;
  const quotePages = { active: 1, done: 1 };

  async function list() {
    const root = document.querySelector("#admin-quotes");
    if (!root) return;
    try {
      const bucket = document.querySelector("#quote-status-filter")?.value || "";
      if (bucket === "DONE") {
        renderRows(root.querySelector("#quote-active-body"), [], "조건에 맞는 진행 중 견적이 없습니다.");
        renderPagination("#quote-active-pagination", "active", emptyPage());
        const doneData = await fetchQuotes("DONE", quotePages.done);
        renderRows(root.querySelector("#quote-done-body"), doneData.items || [], "조건에 맞는 처리 완료 견적이 없습니다.");
        renderPagination("#quote-done-pagination", "done", doneData);
        return;
      }
      const activeData = await fetchQuotes(bucket || "ACTIVE", quotePages.active);
      renderRows(root.querySelector("#quote-active-body"), activeData.items || [], "조건에 맞는 진행 중 견적이 없습니다.");
      renderPagination("#quote-active-pagination", "active", activeData);
      if (bucket) {
        renderRows(root.querySelector("#quote-done-body"), [], "조건에 맞는 처리 완료 견적이 없습니다.");
        renderPagination("#quote-done-pagination", "done", emptyPage());
        return;
      }
      const doneData = await fetchQuotes("DONE", quotePages.done);
      renderRows(root.querySelector("#quote-done-body"), doneData.items || [], "조건에 맞는 처리 완료 견적이 없습니다.");
      renderPagination("#quote-done-pagination", "done", doneData);
    } catch (error) {
      app.setState("#admin-quote-state", error.message, "error");
    }
  }

  async function fetchQuotes(bucket, page) {
    const params = new URLSearchParams({
      bucket,
      page: String(page),
      size: String(quotePageSize)
    });
    return app.request("/admin/quotes?" + params.toString());
  }

  function emptyPage() {
    return { page: 1, size: quotePageSize, total: 0 };
  }

  function renderRows(body, items, emptyMessage) {
    if (!body) return;
    body.innerHTML = items.length ? items.map((q) => app.html`
        <tr>
          <td>${q.requestNo}</td>
          <td>${q.companyName}</td>
          <td>${q.contactName}</td>
          <td>${app.workflowLabel("quote", q.status)}</td>
          <td><a class="button" href="admin-quote-detail.html?id=${q.id}">상세</a></td>
        </tr>
      `).join("") : `<tr><td colspan="5" class="empty">${emptyMessage}</td></tr>`;
  }

  function renderPagination(selector, target, data) {
    const container = document.querySelector(selector);
    if (!container) return;
    container.innerHTML = app.paginationControls(data, {
      size: quotePageSize,
      pageAttributes: (page) => `data-quote-page-target="${app.escapeHtml(target)}" data-page="${page}"`
    });
  }

  async function detail() {
    const root = document.querySelector("#admin-quote-detail");
    if (!root) return;
    try {
      const quote = await app.request("/admin/quotes/" + app.qs("id"));
      const quoteItems = quote.items || [];
      root.innerHTML = `
        <section class="card admin-quote-items">
          <h2>견적 상품</h2>
          <table class="table"><thead><tr><th>상품</th><th>규격</th><th>수량</th><th>단가</th><th>금액</th></tr></thead><tbody>${quoteItems.length ? quoteItems.map((item) => `
            <tr>
              <td>${quoteProductCell(item)}</td>
              <td>${app.escapeHtml(item.variantName)}</td>
              <td>${app.escapeHtml(item.quantity)}</td>
              <td>${app.escapeHtml(app.money(item.unitPrice))}</td>
              <td>${app.escapeHtml(quoteItemAmountLabel(item))}</td>
            </tr>
          `).join("") : '<tr><td colspan="5" class="empty">등록된 상품이 없습니다.</td></tr>'}</tbody></table>
          ${quoteTotalSummary(quoteItems)}
        </section>
        <div class="card grid">
          <h2>${app.escapeHtml(quote.requestNo)}</h2>
          <p>${app.escapeHtml(quote.companyName)} · ${app.escapeHtml(quote.contactName)} · ${app.escapeHtml(quote.contactPhone)}</p>
          <p class="muted">${app.escapeHtml(quote.siteAddress)}</p>
          <select id="quote-status" class="input">
            ${quoteStatusValues.map((status) => `<option value="${status}"${statusMatches(status, quote.status) ? " selected" : ""}>${app.escapeHtml(quoteStatusOptionLabels[status])}</option>`).join("")}
          </select>
          <button id="save-status" class="button primary">상태 저장</button>
          <div id="admin-quote-detail-state"></div>
        </div>
      `;
      document.querySelector("#save-status").addEventListener("click", async () => {
        try {
          await app.request("/admin/quotes/" + quote.id + "/status", { method: "PATCH", body: { status: document.querySelector("#quote-status").value } });
          location.href = "admin-quotes.html";
        } catch (error) {
          app.setState("#admin-quote-detail-state", error.message, "error");
        }
      });
    } catch (error) {
      app.setState(root, error.message, "error");
    }
  }

  function quoteTotalSummary(items) {
    const rows = (items || []).map((item) => itemTotalAmount(item));
    const total = rows.reduce((sum, row) => sum + row.amount, 0);
    const hasUnknown = rows.some((row) => row.unknown);
    const totalLabel = total > 0 ? app.money(total) : "견적문의";
    const unknownText = hasUnknown ? '<span class="muted">견적문의 품목 포함</span>' : "";
    return `
      <div class="admin-quote-total quote-total-summary">
        <span>총 예상 금액</span>
        <strong>${app.escapeHtml(totalLabel)}</strong>
        ${unknownText}
        <small>운반비는 별도입니다.</small>
      </div>
    `;
  }

  function quoteItemAmountLabel(item) {
    const total = itemTotalAmount(item);
    return total.unknown ? "견적문의" : app.money(total.amount);
  }

  function itemTotalAmount(item) {
    const storedTotal = Number(item.totalAmount);
    if (Number.isFinite(storedTotal) && storedTotal > 0) {
      return { amount: Math.round(storedTotal), unknown: false };
    }
    const unitPrice = Number(item.unitPrice);
    const quantity = Number(item.quantity);
    if (Number.isFinite(unitPrice) && unitPrice > 0 && Number.isFinite(quantity) && quantity > 0) {
      return { amount: Math.round(unitPrice * quantity), unknown: false };
    }
    return { amount: 0, unknown: true };
  }

  function statusMatches(optionStatus, currentStatus) {
    return app.workflowBucket("quote", optionStatus) === app.workflowBucket("quote", currentStatus);
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
    if (filter) filter.addEventListener("submit", (event) => { event.preventDefault(); resetPages(); list(); });
    document.querySelector("#quote-status-filter")?.addEventListener("change", () => { resetPages(); list(); });
    document.querySelector("#admin-quotes")?.addEventListener("click", async (event) => {
      const button = event.target.closest("button[data-quote-page-target]");
      if (!button) return;
      const target = button.dataset.quotePageTarget;
      const nextPage = Number(button.dataset.page);
      if (!target || !nextPage || quotePages[target] === nextPage) return;
      quotePages[target] = nextPage;
      button.blur();
      await list();
      app.scrollToElement(quoteSection(target));
    });
    list();
    detail();
  });

  function quoteSection(target) {
    return document.querySelector(`#quote-${target}-pagination`)?.closest(".admin-list-section");
  }

  function resetPages() {
    quotePages.active = 1;
    quotePages.done = 1;
  }
})();
