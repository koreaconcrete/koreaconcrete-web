(function () {
  let product;
  let selectedVariantId = null;
  let variantPage = 1;
  const variantPageSize = 10;

  function canRequestVariant(variant) {
    return Boolean(variant && variant.status !== "SOLD_OUT");
  }

  function unitLabel(unit) {
    const value = unit || "-";
    return value === "-" || /^\d/.test(value) ? value : "1" + value;
  }

  function formatNumber(value) {
    if (value === null || value === undefined || value === "") return "";
    const number = Number(value);
    if (!Number.isFinite(number)) return String(value);
    return number.toLocaleString("ko-KR", { maximumFractionDigits: 2 });
  }

  function formatWeight(value) {
    const formatted = formatNumber(value);
    return formatted ? `${formatted} kg` : "-";
  }

  function formatTwentyFiveTon(value, unit) {
    const formatted = formatNumber(value);
    return formatted ? `${formatted}${unit || "개"}` : "-";
  }

  function dimensionText(variant) {
    const thickness = formatNumber(variant.thicknessMm);
    const parts = [];
    if (thickness) parts.push(`두께 ${thickness} mm`);
    return parts.join(" / ");
  }

  function variantSearchText(variant) {
    return [
      product.name,
      product.summary,
      variant.variantName,
      dimensionText(variant),
      unitLabel(variant.unit || product.unit),
      formatWeight(variant.weightKg),
      formatTwentyFiveTon(variant.twentyFiveTonQuantity, variant.unit || product.unit),
      variant.price ? app.money(variant.price.salePrice) : "견적문의",
      app.label("productStatus", variant.status)
    ].join(" ").toLowerCase();
  }

  function variantMedia(variant) {
    return (product.media || []).filter((media) => media.type === "IMAGE" && media.variantId === variant.id);
  }

  function variantSearchPlaceholder() {
    const examples = (product.variants || [])
      .map((variant) => variant.variantName || dimensionText(variant))
      .filter(Boolean)
      .slice(0, 3);
    return examples.length ? "예: " + examples.join(", ") : "규격명을 입력해주세요";
  }

  function vatLabel(price) {
    return price ? app.label("vatPolicy", price.vatPolicy) : "";
  }

  async function render() {
    const id = app.qs("id", "1");
    const root = document.querySelector("#product-detail");
    if (!root) return;
    try {
      product = await app.request("/products/" + id);
      product.variants = (product.variants || []).filter((variant) => ["ON_SALE", "QUOTE_ONLY", "SOLD_OUT"].includes(variant.status));
      selectedVariantId = product.variants[0]?.id || null;
      const mediaImages = (product.media || []).filter((media) => media.type === "IMAGE");
      const mainImageUrl = mediaImages.length ? mediaImages[0].url : "assets/images/placeholder.svg";
      const sheetImages = mediaImages.slice(1);
      const constructionImages = detailImagesByLabel("시공 이미지");
      const dimensionImages = detailImagesByLabel("제품 규격 및 치수 이미지");
      const specs = visibleSpecs(product.specs || []).map((spec) =>
        `<tr><th>${app.escapeHtml(spec.specKey)}</th><td>${app.escapeHtml(formatSpecValue(spec.specValue))}</td></tr>`
      ).join("");

      root.innerHTML = `
        <div class="detail-layout">
          <section class="detail-title-block">
            <span class="badge">${app.escapeHtml(product.category.name)}</span>
            <h1>${app.escapeHtml(product.name)}</h1>
            <p class="muted">${app.escapeHtml(product.description || "")}</p>
            ${customMadeNotice()}
            <div id="variant-spec" class="card detail-variant-summary"></div>
          </section>
          <div class="detail-image detail-main-image"><img src="${app.escapeHtml(mainImageUrl)}" alt="${app.escapeHtml(product.name)}"></div>
          <div class="grid detail-action-panel">
            <section class="card variant-browser">
              <div class="variant-browser-head">
                <div>
                  <h2>규격 선택</h2>
                  <p class="muted">규격이 많은 상품은 검색 후 원하는 행을 선택하고 아래에서 수량과 요청 방식을 선택할 수 있습니다.</p>
                </div>
                <div id="variant-count" class="variant-count"></div>
              </div>
              <div class="variant-tools">
                <label class="field">규격 검색
                  <input id="variant-keyword" class="input" placeholder="${app.escapeHtml(variantSearchPlaceholder())}">
                </label>
                <label class="field">판매 상태
                  <select id="variant-status-filter" class="input">
                    <option value="">전체</option>
                    <option value="ON_SALE">판매중</option>
                    <option value="QUOTE_ONLY">견적전용</option>
                    <option value="SOLD_OUT">품절</option>
                  </select>
                </label>
              </div>
              <div class="variant-table-wrap">
                <table class="table variant-select-table">
                  <thead>
                    <tr>
                      <th>선택</th>
                      <th>품명</th>
                      <th>규격</th>
                      <th>단위</th>
                      <th>중량</th>
                      <th>25톤</th>
                      <th>판매가</th>
                    </tr>
                  </thead>
                  <tbody id="variant-table-body"></tbody>
                </table>
              </div>
              <div id="variant-pagination" class="variant-pagination"></div>
              <div class="variant-action-panel">
                <div class="selected-variant-inline">
                  <span>선택한 규격</span>
                  <strong id="selected-variant-name">-</strong>
                </div>
                <div class="variant-action-controls">
                  <label class="field">수량
                    <input id="quantity" class="input variant-quantity" type="number" min="1" value="1" aria-label="수량">
                  </label>
                  <button class="button primary" id="add-cart" type="button">장바구니 담기</button>
                  <button class="button accent" id="request-quote" type="button">견적요청</button>
                </div>
              </div>
            </section>
            <div class="grid detail-service-forms">
              <form id="consultation-inline-form" class="card grid">
                <h2>상담 요청</h2>
                <div class="grid cols-2">
                  <label class="field">담당자명
                    <input class="input" name="contactName" placeholder="담당자명" required>
                  </label>
                  <label class="field">연락처
                    <input class="input" name="contactPhone" placeholder="010-0000-0000" required>
                  </label>
                </div>
                <label class="field">상담 내용
                  <textarea name="message" placeholder="상담 내용을 입력해주세요."></textarea>
                </label>
                <section class="agreement-panel compact" aria-label="개인정보 수집 동의">
                  <div class="agreement-list">
                    <article class="agreement-item">
                      <div class="agreement-item-head">
                        <label class="agreement-check"><input type="checkbox" name="privacyAgreed" required> <span>개인정보 수집·이용 동의 <small>필수</small></span></label>
                        <button class="agreement-toggle" type="button" aria-expanded="false" aria-controls="product-consultation-privacy-detail" data-agreement-toggle="product-consultation-privacy-detail">자세히 보기</button>
                      </div>
                      <div class="agreement-detail" id="product-consultation-privacy-detail" hidden>
                        <p>상품 상담 접수와 연락을 위해 담당자명, 연락처, 상담 내용을 수집하며 상담 처리 및 이력 확인 목적으로 보관합니다.</p>
                      </div>
                    </article>
                  </div>
                </section>
                <div class="row">
                  <button class="button" type="button" data-consult-type="SMS">문자상담 요청</button>
                  <button class="button" type="button" data-consult-type="PHONE">전화상담 요청</button>
                </div>
                <div id="consultation-result"></div>
              </form>
            </div>
          </div>
        </div>
        <div class="modal-backdrop" id="cart-modal" hidden>
          <section class="modal-panel" role="dialog" aria-modal="true" aria-labelledby="cart-modal-title">
            <h2 id="cart-modal-title">장바구니에 담았습니다</h2>
            <p id="cart-modal-message" class="muted">선택한 상품이 장바구니에 담겼습니다.</p>
            <div class="modal-actions">
              <a class="button primary" href="cart.html">장바구니로 이동</a>
              <button class="button" type="button" id="cart-modal-close">닫기</button>
            </div>
          </section>
        </div>
        ${specSection(specs, constructionImages, dimensionImages)}
        ${sheetImages.length ? `
          <section class="section product-media-section">
            <h2>제품 상세</h2>
            <div class="media-sheet">${sheetImages.map((media) => `<img src="${app.escapeHtml(media.url)}" alt="${app.escapeHtml(media.altText || product.name)}">`).join("")}</div>
          </section>
        ` : ""}
      `;
      bind();
      updateVariant();
      renderVariantTable();
    } catch (error) {
      app.setState(root, error.message, "error");
    }
  }

  function selectedVariant() {
    if (!product.variants.length) return null;
    return product.variants.find((variant) => variant.id === selectedVariantId) || product.variants[0];
  }

  function customMadeNotice() {
    if (!product.customMade) return "";
    return `
      <div class="custom-made-notice">
        <strong>주문제작 상품</strong>
        <span>필요한 규격이 목록과 다르면 상담 요청 또는 견적요청 시 원하는 규격으로 변경해 요청할 수 있습니다.</span>
      </div>
    `;
  }

  function detailImagesByLabel(label) {
    return (product.media || []).filter((media) => media.type === "DETAIL_IMAGE" && media.altText === label);
  }

  function specSection(specRows, constructionImages, dimensionImages) {
    const detailImagesHtml = detailImageSections(constructionImages, dimensionImages);
    if (!detailImagesHtml) return "";
    return `
      <section class="section">
        <h2>상품 스펙</h2>
        ${specRows ? `<table class="table"><tbody>${specRows}</tbody></table>` : ""}
        ${detailImagesHtml}
      </section>
    `;
  }

  function detailImageSections(constructionImages, dimensionImages) {
    const sections = [];
    if (constructionImages.length) {
      sections.push(detailImageSection("시공 이미지", constructionImages));
    }
    if (dimensionImages.length) {
      sections.push(detailImageSection("제품 규격 및 치수", dimensionImages));
    }
    return sections.length ? `<div class="product-spec-images">${sections.join("")}</div>` : "";
  }

  function detailImageSection(title, images) {
    return `
      <section class="product-spec-image-group">
        <h3>${app.escapeHtml(title)}</h3>
        <div class="product-spec-image-sheet">
          ${images.map((media) => `<img src="${app.escapeHtml(media.url)}" alt="${app.escapeHtml(media.altText || title)}">`).join("")}
        </div>
      </section>
    `;
  }

  function visibleSpecs(specs) {
    const hiddenKeys = new Set(["자료출처", "원본게시판"]);
    return specs.filter((spec) => !hiddenKeys.has(spec.specKey));
  }

  function formatSpecValue(value) {
    return String(value || "").replace(/^임의\s*단가\s*/u, "");
  }

  function selectedQuantity() {
    const input = document.querySelector("#quantity");
    return Math.max(1, Number(input?.value || 1));
  }

  async function addSelectedToCart(variant, quantity) {
    if (!variant) return;
    const image = (product.media || []).find((media) => media.type === "IMAGE");
    await app.addCart({
      productId: product.id,
      productName: product.name,
      productSummary: product.summary || "",
      imageUrl: image ? image.url : "assets/images/placeholder.svg",
      variantId: variant.id,
      variantName: variant.variantName,
      quantity: quantity || selectedQuantity(),
      salePrice: variant.price ? variant.price.salePrice : null
    });
  }

  function requireLogin() {
    if (app.currentUser()) return true;
    const redirect = encodeURIComponent(location.pathname.split("/").pop() + location.search);
    location.href = "login.html?redirect=" + redirect;
    return false;
  }

  function updateVariant() {
    const variant = selectedVariant();
    if (!variant) {
      document.querySelector("#variant-spec").innerHTML = "<p>선택 가능한 규격이 없습니다.</p>";
      return;
    }
    const price = variant.price;
    const disabled = !canRequestVariant(variant);
    const media = variantMedia(variant);
    const dimensions = dimensionText(variant);
    const mediaHtml = media.length
      ? `<div class="variant-media-strip">${media.slice(0, 3).map((item) => `<img src="${app.escapeHtml(item.url)}" alt="${app.escapeHtml(item.altText || variant.variantName)}">`).join("")}</div>`
      : "";
    document.querySelector("#variant-spec").innerHTML = `
      <p><strong>규격</strong> ${app.escapeHtml(variant.variantName)}</p>
      ${dimensions ? `<p><strong>치수</strong> ${app.escapeHtml(dimensions)}</p>` : ""}
      <p><strong>중량</strong> ${app.escapeHtml(formatWeight(variant.weightKg))}</p>
      <p><strong>단위</strong> ${app.escapeHtml(unitLabel(variant.unit || product.unit))}</p>
      <p><strong>판매단가</strong> ${price ? app.escapeHtml(app.money(price.salePrice)) : "견적문의"}</p>
      <p class="muted">${app.escapeHtml(disabled ? "현재 품절된 규격입니다." : price ? vatLabel(price) : "가격은 관리자 확인 후 안내됩니다.")}</p>
      ${mediaHtml}
    `;
    const selectedName = document.querySelector("#selected-variant-name");
    if (selectedName) selectedName.textContent = `${variant.variantName}${disabled ? " / 품절" : ""}`;
    document.querySelector("#quantity")?.toggleAttribute("disabled", disabled);
    document.querySelector("#add-cart")?.toggleAttribute("disabled", disabled);
    document.querySelector("#request-quote")?.toggleAttribute("disabled", disabled);
    document.querySelectorAll("[data-variant-id]").forEach((row) => {
      const active = Number(row.dataset.variantId) === variant.id;
      row.classList.toggle("is-selected", active);
      const radio = row.querySelector('input[type="radio"]');
      if (radio) radio.checked = active;
    });
  }

  function bind() {
    document.querySelector("#variant-keyword")?.addEventListener("input", () => {
      variantPage = 1;
      renderVariantTable();
    });
    document.querySelector("#variant-status-filter")?.addEventListener("change", () => {
      variantPage = 1;
      renderVariantTable();
    });
    document.querySelector("#variant-table-body")?.addEventListener("click", (event) => {
      const row = event.target.closest("[data-variant-id]");
      if (!row) return;
      const variant = product.variants.find((item) => item.id === Number(row.dataset.variantId));
      if (!variant) return;
      selectedVariantId = variant.id;
      updateVariant();
    });
    document.querySelector("#variant-pagination")?.addEventListener("click", (event) => {
      const button = event.target.closest("[data-variant-page]");
      if (!button) return;
      event.preventDefault();
      const nextPage = Number(button.dataset.variantPage);
      if (!nextPage || nextPage === variantPage) return;
      variantPage = nextPage;
      renderVariantTable();
      button.blur();
      document.querySelector(".variant-table-wrap")?.scrollTo({ top: 0, behavior: "smooth" });
      app.scrollToElement(".variant-browser");
    });
    document.querySelector("#variant-table-body")?.addEventListener("change", (event) => {
      const row = event.target.closest("[data-variant-id]");
      if (!row) return;
      if (event.target.matches('input[type="radio"]')) {
        selectedVariantId = Number(row.dataset.variantId);
        updateVariant();
      }
    });
    document.querySelector("#add-cart").addEventListener("click", async () => {
      const variant = selectedVariant();
      if (!requireLogin()) return;
      if (!variant) {
        app.setState("#variant-spec", "선택 가능한 규격이 없습니다.", "error");
        return;
      }
      if (!canRequestVariant(variant)) {
        app.setState("#variant-spec", "품절된 규격은 장바구니에 담을 수 없습니다.", "error");
        return;
      }
      const quantity = selectedQuantity();
      try {
        await addSelectedToCart(variant, quantity);
        showCartModal(variant, quantity);
      } catch (error) {
        app.setState("#variant-spec", error.message, "error");
      }
    });
    document.querySelector("#request-quote").addEventListener("click", async () => {
      const variant = selectedVariant();
      if (!requireLogin()) return;
      if (!variant) {
        app.setState("#variant-spec", "선택 가능한 규격이 없습니다.", "error");
        return;
      }
      if (!canRequestVariant(variant)) {
        app.setState("#variant-spec", "품절된 규격은 견적요청할 수 없습니다.", "error");
        return;
      }
      try {
        await addSelectedToCart(variant, selectedQuantity());
        location.href = "quote-new.html";
      } catch (error) {
        app.setState("#variant-spec", error.message, "error");
      }
    });
    document.querySelectorAll("[data-consult-type]").forEach((button) => {
      button.addEventListener("click", () => requestConsult(button.dataset.consultType));
    });
    document.querySelector("#cart-modal-close")?.addEventListener("click", closeCartModal);
    document.querySelector("#cart-modal")?.addEventListener("click", (event) => {
      if (event.target.id === "cart-modal") closeCartModal();
    });
  }

  function renderVariantTable() {
    const body = document.querySelector("#variant-table-body");
    if (!body) return;
    const keyword = (document.querySelector("#variant-keyword")?.value || "").trim().toLowerCase();
    const status = document.querySelector("#variant-status-filter")?.value || "";
    const variants = product.variants.filter((variant) => {
      const matchesKeyword = !keyword || variantSearchText(variant).includes(keyword);
      const matchesStatus = !status || variant.status === status;
      return matchesKeyword && matchesStatus;
    });
    if (variants.length && !variants.some((variant) => variant.id === selectedVariantId)) {
      selectedVariantId = variants[0].id;
    }
    const totalPages = Math.max(1, Math.ceil(variants.length / variantPageSize));
    variantPage = Math.min(Math.max(variantPage, 1), totalPages);
    const startIndex = (variantPage - 1) * variantPageSize;
    const pageVariants = variants.slice(startIndex, startIndex + variantPageSize);
    if (pageVariants.length && !pageVariants.some((variant) => variant.id === selectedVariantId)) {
      selectedVariantId = pageVariants[0].id;
    }
    const count = document.querySelector("#variant-count");
    if (count) {
      const visibleStart = variants.length ? startIndex + 1 : 0;
      const visibleEnd = Math.min(startIndex + variantPageSize, variants.length);
      count.textContent = `전체 ${product.variants.length.toLocaleString("ko-KR")}개 중 ${visibleStart.toLocaleString("ko-KR")}-${visibleEnd.toLocaleString("ko-KR")}개 표시`;
    }
    if (!variants.length) {
      body.innerHTML = '<tr><td colspan="7">조건에 맞는 규격이 없습니다.</td></tr>';
      renderVariantPagination(0, 0);
      return;
    }
    body.innerHTML = pageVariants.map(variantRow).join("");
    renderVariantPagination(variants.length, totalPages);
    updateVariant();
  }

  function renderVariantPagination(total, totalPages) {
    const el = document.querySelector("#variant-pagination");
    if (!el) return;
    if (!total) {
      el.innerHTML = "";
      return;
    }
    const visiblePageCount = 10;
    let start = Math.max(1, variantPage - Math.floor((visiblePageCount - 1) / 2));
    let end = Math.min(totalPages, start + visiblePageCount - 1);
    start = Math.max(1, end - visiblePageCount + 1);
    const buttons = [];
    for (let page = start; page <= end; page++) {
      buttons.push(`<button class="button page-button ${page === variantPage ? "is-active" : ""}" type="button" data-variant-page="${page}"${page === variantPage ? ' aria-current="page"' : ""}>${page}</button>`);
    }
    el.innerHTML = `
      <div class="pagination-buttons">
        <button class="button" type="button" data-variant-page="${variantPage - 1}"${variantPage <= 1 ? " disabled" : ""}>이전</button>
        ${buttons.join("")}
        <button class="button" type="button" data-variant-page="${variantPage + 1}"${variantPage >= totalPages ? " disabled" : ""}>다음</button>
      </div>
    `;
  }

  function variantRow(variant) {
    const price = variant.price;
    const dimensions = dimensionText(variant);
    const active = selectedVariant()?.id === variant.id;
    return `
      <tr data-variant-id="${app.escapeHtml(variant.id)}" class="${active ? "is-selected" : ""}">
        <td data-label="선택">
          <input type="radio" name="variant-choice" ${active ? "checked" : ""} aria-label="${app.escapeHtml(variant.variantName)} 선택">
        </td>
        <td data-label="품명">
          <strong>${app.escapeHtml(product.name)}</strong>
        </td>
        <td data-label="규격">
          <strong>${app.escapeHtml(variant.variantName)}</strong>
          ${dimensions ? `<span class="row-subtitle">${app.escapeHtml(dimensions)}</span>` : ""}
          <span class="row-subtitle">${app.escapeHtml(app.label("productStatus", variant.status))}</span>
        </td>
        <td data-label="단위">${app.escapeHtml(unitLabel(variant.unit || product.unit))}</td>
        <td data-label="중량">${app.escapeHtml(formatWeight(variant.weightKg))}</td>
        <td data-label="25톤">${app.escapeHtml(formatTwentyFiveTon(variant.twentyFiveTonQuantity, variant.unit || product.unit))}</td>
        <td data-label="판매가">
          <strong>${price ? app.escapeHtml(app.money(price.salePrice)) : "견적문의"}</strong>
          ${price ? `<span class="row-subtitle">${app.escapeHtml(vatLabel(price))}</span>` : ""}
        </td>
      </tr>
    `;
  }

  function showCartModal(variant, quantity) {
    const modal = document.querySelector("#cart-modal");
    const message = document.querySelector("#cart-modal-message");
    if (message && variant) {
      message.textContent = `${product.name} / ${variant.variantName} ${quantity || selectedQuantity()}개가 장바구니에 담겼습니다.`;
    }
    if (modal) {
      modal.hidden = false;
      document.querySelector("#cart-modal-close")?.focus();
    }
  }

  function closeCartModal() {
    const modal = document.querySelector("#cart-modal");
    if (modal) modal.hidden = true;
    document.querySelector("#add-cart")?.focus();
  }

  async function requestConsult(type) {
    const form = document.querySelector("#consultation-inline-form");
    if (!form.reportValidity()) return;
    const data = Object.fromEntries(new FormData(form).entries());
    const variant = selectedVariant();
    if (!variant) {
      app.setState("#consultation-result", "선택 가능한 규격이 없습니다.", "error");
      return;
    }
    try {
      await app.request(type === "SMS" ? "/consultations/sms" : "/consultations/call-request", {
        method: "POST",
        body: {
          type,
          productId: product.id,
          variantId: variant.id,
          contactName: data.contactName,
          contactPhone: data.contactPhone,
          message: data.message || `${product.name} ${variant.variantName} ${selectedQuantity()}개 상담 요청`,
          privacyAgreed: form.privacyAgreed.checked
        }
      });
      app.notify("상담요청이 접수되었습니다.");
      app.setState("#consultation-result", "", "");
      form.reset();
    } catch (error) {
      app.setState("#consultation-result", error.message, "error");
    }
  }

  document.addEventListener("DOMContentLoaded", render);
})();
