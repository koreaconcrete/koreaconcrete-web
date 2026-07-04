(function () {
  function requireLogin() {
    if (app.currentUser()) return true;
    location.href = "login.html?redirect=cart.html";
    return false;
  }

  async function render() {
    const body = document.querySelector("#cart-items");
    if (!body) return;
    let items = [];
    try {
      items = await app.cartItems();
    } catch (error) {
      app.setState("#cart-wrap", error.message, "error");
      return;
    }
    if (!items.length) {
      document.querySelector("#cart-wrap").innerHTML = `
        <div class="empty empty-state">
          <strong>장바구니가 비어 있습니다.</strong>
          <p>상품 상세에서 규격과 수량을 선택한 뒤 장바구니에 담아주세요.</p>
          <div class="row"><a class="button primary" href="products.html">상품 보러가기</a></div>
        </div>
      `;
      return;
    }
    const hasQuoteOnly = items.some((item) => !Number(item.salePrice || 0));
    body.innerHTML = items.map((item, index) => app.html`
      <tr>
        <td>
          <a class="line-item" href="product-detail.html?id=${item.productId}">
            <img class="line-item-thumb" src="${item.imageUrl || "assets/images/placeholder.svg"}" alt="${item.productName}">
            <span class="line-item-copy">
              <strong>${item.productName}</strong>
              <span>${item.productSummary || ""}</span>
            </span>
          </a>
        </td>
        <td>${item.variantName}</td>
        <td><input class="input compact-input" data-qty="${index}" type="number" min="1" value="${item.quantity}"></td>
        <td>${app.money(item.salePrice)}</td>
        <td class="price">${lineTotal(item)}</td>
        <td><button class="button danger" data-remove="${index}">삭제</button></td>
      </tr>
    `).join("");
    renderSummary(items, hasQuoteOnly);
    document.querySelectorAll("[data-qty]").forEach((input) => {
      input.addEventListener("input", () => {
        const item = items[Number(input.dataset.qty)];
        if (!item) return;
        item.quantity = Math.max(1, Number(input.value || 1));
        input.closest("tr")?.querySelector(".price")?.replaceChildren(document.createTextNode(lineTotal(item)));
        renderSummary(items, hasQuoteOnly);
      });
      input.addEventListener("change", async () => {
        const item = items[Number(input.dataset.qty)];
        if (!item) return;
        const quantity = Math.max(1, Number(input.value || 1));
        input.value = quantity;
        await app.updateCartItem(item.id || item.variantId, quantity);
        render();
      });
    });
    document.querySelectorAll("[data-remove]").forEach((button) => {
      button.addEventListener("click", async () => {
        const item = items[Number(button.dataset.remove)];
        await app.removeCartItem(item.id || item.variantId);
        render();
      });
    });
  }

  function lineTotal(item) {
    const unitPrice = Number(item.salePrice || 0);
    if (!unitPrice) return "견적문의";
    return app.money(unitPrice * Number(item.quantity || 1));
  }

  function cartTotal(items) {
    return items.reduce((sum, item) => {
      const unitPrice = Number(item.salePrice || 0);
      return unitPrice > 0 ? sum + unitPrice * Number(item.quantity || 1) : sum;
    }, 0);
  }

  function renderSummary(items, hasQuoteOnly) {
    const summary = document.querySelector("#cart-summary");
    if (!summary) return;
    const pricedTotal = cartTotal(items);
    const totalLabel = pricedTotal > 0 ? app.money(pricedTotal) : "견적문의";
    summary.innerHTML = `
      <div>
        <span class="muted">총 ${items.length}개 품목</span>
        ${hasQuoteOnly ? '<p class="muted">가격 미등록 품목은 합계에 포함되지 않습니다.</p>' : ""}
      </div>
      <strong>총합 ${totalLabel}</strong>
    `;
  }

  document.addEventListener("DOMContentLoaded", () => {
    if (!requireLogin()) return;
    render();
  });
})();
