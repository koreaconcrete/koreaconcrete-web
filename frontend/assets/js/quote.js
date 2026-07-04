(function () {
  function requireLogin() {
    if (app.currentUser()) return true;
    location.href = "login.html?redirect=quote-new.html";
    return false;
  }

  async function renderItems() {
    const box = document.querySelector("#quote-items");
    if (!box) return;
    let items = [];
    try {
      items = await app.cartItems();
    } catch (error) {
      app.setState(box, error.message, "error");
      return;
    }
    if (!items.length) {
      box.innerHTML = `
        <div class="empty empty-state">
          <strong>요청할 품목이 없습니다.</strong>
          <p>먼저 상품 상세에서 규격과 수량을 선택해 장바구니에 담아주세요.</p>
          <div class="row"><a class="button primary" href="products.html">상품 선택하기</a></div>
        </div>
      `;
      return;
    }
    const pricedTotal = items.reduce((sum, item) => {
      const unitPrice = Number(item.salePrice || 0);
      return unitPrice > 0 ? sum + unitPrice * Number(item.quantity || 1) : sum;
    }, 0);
    const hasQuoteOnly = items.some((item) => !Number(item.salePrice || 0));
    const totalLabel = pricedTotal > 0 ? app.money(pricedTotal) : "견적문의";
    box.innerHTML = `
      <table class="table">
        <thead><tr><th>상품</th><th>규격</th><th>수량</th><th>단가</th><th>상품 금액</th></tr></thead>
        <tbody>${items.map((item) => app.html`
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
            <td>${item.quantity}</td>
            <td>${app.money(item.salePrice)}</td>
            <td class="price">${lineTotal(item)}</td>
          </tr>
        `).join("")}</tbody>
      </table>
      <div class="cart-summary quote-total-summary">
        <div>
          <span class="muted">총 ${items.length}개 품목</span>
          ${hasQuoteOnly ? '<p class="muted">가격 미등록 품목은 예상 금액에 포함되지 않습니다.</p>' : ""}
        </div>
        <strong>총 예상 금액 ${totalLabel}</strong>
      </div>
    `;
  }

  function lineTotal(item) {
    const unitPrice = Number(item.salePrice || 0);
    if (!unitPrice) return "견적문의";
    return app.money(unitPrice * Number(item.quantity || 1));
  }

  function bind() {
    const form = document.querySelector("#quote-form");
    if (!form) return;
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const cart = await app.cartItems();
      if (!cart.length) {
        app.setState("#quote-state", "견적 품목이 없습니다.", "error");
        return;
      }
      const data = Object.fromEntries(new FormData(form).entries());
      const body = {
        companyName: data.companyName,
        contactName: data.contactName,
        contactPhone: data.contactPhone,
        siteAddress: data.siteAddress,
        requestedDeliveryDate: data.requestedDeliveryDate || null,
        memo: data.memo,
        privacyAgreed: form.privacyAgreed.checked
      };
      try {
        const response = await app.createQuoteFromCart(body);
        document.querySelector("#quote-state").className = "notice";
        document.querySelector("#quote-state").textContent = `견적요청이 접수되었습니다. 요청번호 ${response.requestNo}`;
        renderItems();
      } catch (error) {
        app.setState("#quote-state", error.message, "error");
      }
    });
  }

  async function prefillUserInfo() {
    const form = document.querySelector("#quote-form");
    if (!form) return;
    try {
      const me = await app.request("/users/me");
      if (!form.contactName.value && me.name) form.contactName.value = me.name;
      if (!form.contactPhone.value && me.phone) form.contactPhone.value = me.phone;
    } catch (error) {
      // 로그인 가드가 먼저 동작하므로 여기서는 입력 보조 실패만 조용히 무시합니다.
    }
  }

  document.addEventListener("DOMContentLoaded", async () => {
    if (!requireLogin()) return;
    await prefillUserInfo();
    renderItems();
    bind();
  });
})();
