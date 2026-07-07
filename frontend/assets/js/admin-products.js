(function () {
  let currentProduct = null;
  let mediaItems = [];
  let detailMediaItems = {};
  let variantRows = [];
  let categoryOptions = [];
  let adminProductRows = [];
  let adminProductFilters = { keyword: "", status: "" };
  let priceBookId = null;
  let includeDeletedProducts = false;
  let includeDeletedVariants = false;
  let tempVariantSeq = 1;
  const adminProductFetchSize = 200;
  const variantDecimalFields = new Map([
    ["widthMm", "폭(mm)"],
    ["lengthMm", "길이(mm)"],
    ["heightMm", "높이(mm)"],
    ["thicknessMm", "두께(mm)"],
    ["weightKg", "중량(kg)"],
    ["twentyFiveTonQuantity", "25톤"]
  ]);
  const detailMediaConfigs = [
    { key: "construction", label: "시공 이미지", sortOrder: 1000, multiple: true },
    { key: "dimensions", label: "제품 규격 및 치수 이미지", sortOrder: 2000, multiple: false }
  ];

  async function render() {
    const root = document.querySelector("#admin-products");
    if (!root) return;
    try {
      const keyword = document.querySelector("#admin-product-keyword")?.value || "";
      const status = document.querySelector("#admin-product-status")?.value || "";
      adminProductFilters = { keyword, status };
      adminProductRows = [];
      const tree = await app.request("/categories/tree");
      categoryOptions = flattenCategories(tree || []);
      renderProductDeletedToggle();
      renderProductOrderHint();
      renderProductTree(root.querySelector("#admin-product-tree"), tree || []);
      prepareProductSearchState();
      await loadProductCategoryLists(categoryOptions.filter(isProductCategory).map((category) => category.id));
    } catch (error) {
      app.setState("#admin-product-state", error.message, "error");
    }
  }

  function renderProductTree(container, tree) {
    if (!container) return;
    const sections = (tree || [])
      .map((root) => rootProductSection(root))
      .filter(Boolean);
    container.innerHTML = sections.length
      ? sections.join("") + '<div class="empty" data-product-search-loading hidden>검색 결과를 불러오는 중입니다.</div><div class="empty" data-product-search-empty hidden>조건에 맞는 상품이 없습니다.</div>'
      : `<div class="empty">등록된 세부 카테고리가 없습니다.</div>`;
  }

  function prepareProductSearchState() {
    const searching = isProductSearchFiltered();
    document.querySelectorAll(".admin-product-root, .admin-product-category").forEach((section) => {
      section.hidden = searching;
    });
    const loading = document.querySelector("[data-product-search-loading]");
    if (loading) {
      loading.hidden = !searching;
    }
    const empty = document.querySelector("[data-product-search-empty]");
    if (empty) {
      empty.hidden = true;
    }
  }

  function rootProductSection(root) {
    const children = (root.children || []).filter((category) => category.depth === 2 || category.parentId);
    const childSections = children
      .map((category) => productCategorySection(category))
      .filter(Boolean)
      .join("");
    if (!childSections) return "";
    return `
      <section class="admin-product-root">
        <h2>${app.escapeHtml(root.name)}</h2>
        <div class="admin-product-category-list">${childSections}</div>
      </section>
    `;
  }

  function productCategorySection(category) {
    return `
      <section class="admin-product-category" data-product-category-section="${app.escapeHtml(category.id)}">
        <h3>${app.escapeHtml(category.name)}</h3>
        <table class="table">
          <thead><tr><th>순서</th><th>상품명</th><th>검색어</th><th>상태</th><th></th></tr></thead>
          <tbody data-product-category-body="${app.escapeHtml(category.id)}">
            <tr><td colspan="5" class="empty">상품을 불러오는 중입니다.</td></tr>
          </tbody>
        </table>
      </section>
    `;
  }

  async function loadProductCategoryLists(categoryIds) {
    const categoryProducts = await Promise.all((categoryIds || []).map(loadProductCategoryList));
    adminProductRows = categoryProducts.flat();
    syncProductRootVisibility();
  }

  async function loadProductCategoryList(categoryId) {
    const products = await fetchProductCategoryProducts(categoryId);
    renderProductCategoryList(categoryId, products);
    return products;
  }

  function productCategoryUrl(categoryId, page, size) {
    const params = new URLSearchParams({
      categoryId: String(categoryId),
      page: String(page),
      size: String(size)
    });
    if (adminProductFilters.keyword) params.set("keyword", adminProductFilters.keyword);
    if (adminProductFilters.status) params.set("status", adminProductFilters.status);
    if (includeDeletedProducts) params.set("includeDeleted", "true");
    return "/admin/products?" + params.toString();
  }

  async function fetchProductCategoryProducts(categoryId) {
    const products = [];
    let page = 1;
    let hasNext = true;
    while (hasNext) {
      const data = await app.request(productCategoryUrl(categoryId, page, adminProductFetchSize));
      products.push(...(data.items || []));
      hasNext = Boolean(data.hasNext);
      page += 1;
    }
    return products;
  }

  function renderProductCategoryList(categoryId, products) {
    const section = document.querySelector(`[data-product-category-section="${String(categoryId)}"]`);
    const body = document.querySelector(`[data-product-category-body="${String(categoryId)}"]`);
    const sortedProducts = sortProducts(products || []);
    if (section) {
      section.hidden = isProductSearchFiltered() && !sortedProducts.length;
    }
    if (body) {
      body.innerHTML = sortedProducts.length
        ? sortedProducts.map((product) => productRow(product)).join("")
        : isProductSearchFiltered()
          ? ""
        : `<tr><td colspan="5" class="empty">조건에 맞는 상품이 없습니다.</td></tr>`;
    }
  }

  function isProductSearchFiltered() {
    return Boolean(adminProductFilters.keyword || adminProductFilters.status);
  }

  function syncProductRootVisibility() {
    const searching = isProductSearchFiltered();
    let visibleCategoryCount = 0;
    document.querySelectorAll(".admin-product-root").forEach((root) => {
      const categories = Array.from(root.querySelectorAll(".admin-product-category"));
      const visibleCategories = categories.filter((section) => !section.hidden);
      visibleCategoryCount += visibleCategories.length;
      root.hidden = searching && !visibleCategories.length;
    });
    const empty = document.querySelector("[data-product-search-empty]");
    const loading = document.querySelector("[data-product-search-loading]");
    if (loading) {
      loading.hidden = true;
    }
    if (empty) {
      empty.hidden = !searching || visibleCategoryCount > 0;
    }
  }

  function productRow(product) {
    const deleteButton = product.status === "DELETED"
      ? '<button class="button danger" type="button" disabled>삭제됨</button>'
      : `<button class="button danger" type="button" data-product-delete="${app.escapeHtml(product.id)}" data-product-name="${app.escapeHtml(product.name)}">삭제</button>`;
    const locked = isProductOrderLocked();
    const orderDisabled = locked || product.status === "DELETED";
    const orderTitle = locked
      ? "검색어, 상태, 삭제 포함 보기를 해제한 뒤 순서를 변경할 수 있습니다."
      : "삭제된 상품은 순서를 변경할 수 없습니다.";
    const orderAttrs = orderDisabled ? ` disabled title="${app.escapeHtml(orderTitle)}"` : "";
    return `
      <tr>
        <td>
          <div class="row admin-order-actions">
            <button class="button" type="button" data-product-move="${app.escapeHtml(product.id)}" data-direction="up"${orderAttrs}>위</button>
            <button class="button" type="button" data-product-move="${app.escapeHtml(product.id)}" data-direction="down"${orderAttrs}>아래</button>
          </div>
        </td>
        <td><strong>${app.escapeHtml(product.name)}</strong><span class="muted"> #${app.escapeHtml(product.id)}</span></td>
        <td><span class="admin-keywords">${app.escapeHtml(product.searchKeywords || "-")}</span></td>
        <td>${app.escapeHtml(app.label("productStatus", product.status))}</td>
        <td>
          <div class="row admin-row-actions">
            <a class="button" href="admin-product-form.html?id=${app.escapeHtml(product.id)}">수정</a>
            ${deleteButton}
          </div>
        </td>
      </tr>
    `;
  }

  function sortProducts(products) {
    return products.slice().sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0) || Number(b.id || 0) - Number(a.id || 0));
  }

  function isProductOrderLocked() {
    return Boolean(adminProductFilters.keyword || adminProductFilters.status || includeDeletedProducts);
  }

  function renderProductOrderHint() {
    const hint = document.querySelector("#admin-product-order-hint");
    if (!hint) return;
    if (!isProductOrderLocked()) {
      hint.hidden = true;
      hint.textContent = "";
      return;
    }
    const reasons = [];
    if (adminProductFilters.keyword) reasons.push("검색어");
    if (adminProductFilters.status) reasons.push("상태 필터");
    if (includeDeletedProducts) reasons.push("삭제된 상품 보기");
    hint.hidden = false;
    hint.textContent = `${reasons.join(", ")} 적용 중에는 전체 상품 순서와 화면 목록이 달라질 수 있어 순서 변경을 잠시 막아두었습니다. 필터를 해제한 뒤 조정해주세요.`;
  }

  function renderProductDeletedToggle() {
    const button = document.querySelector("#admin-product-include-deleted");
    if (!button) return;
    button.setAttribute("aria-pressed", String(includeDeletedProducts));
    button.textContent = includeDeletedProducts ? "삭제된 상품 숨기기" : "삭제된 상품까지 보기";
  }

  async function handleProductDelete(event) {
    const button = event.target.closest("button[data-product-delete]");
    if (!button) return;
    const productId = button.dataset.productDelete;
    const productName = button.dataset.productName || "선택한 상품";
    if (!confirm(`"${productName}" 상품을 삭제 상태로 변경할까요?\n장바구니에 담긴 해당 상품은 자동으로 제거되고, 기존 견적/상담 이력에는 삭제된 상품으로 표시됩니다.`)) {
      return;
    }
    button.disabled = true;
    try {
      await app.request("/admin/products/" + productId, { method: "DELETE" });
      app.notify("상품이 삭제됨 상태로 변경되었습니다.");
      await render();
    } catch (error) {
      app.setState("#admin-product-state", error.message, "error");
    } finally {
      button.disabled = false;
    }
  }

  async function form() {
    const form = document.querySelector("#admin-product-form");
    if (!form) return;
    const id = app.qs("id");
    await Promise.all([loadPriceBook(), loadCategories(form)]);
    if (id) {
      try {
        currentProduct = await app.request("/admin/products/" + id);
        mediaItems = mediaFromProduct(currentProduct);
        detailMediaItems = detailMediaFromProduct(currentProduct);
        variantRows = variantsFromProduct(currentProduct);
        fillProductForm(form, currentProduct);
      } catch (error) {
        app.setState("#admin-product-form-state", error.message, "error");
      }
    } else {
      detailMediaItems = {};
      variantRows = [newVariantRow({ variantName: "기본", unit: "개" })];
    }
    renderMediaList();
    renderDetailMediaList();
    renderVariantDeletedToggle();
    renderVariantList();
    renderPreview();
    bindForm(form);
  }

  function mediaFromProduct(product) {
    return (product.media || [])
      .filter((media) => media.type === "IMAGE")
      .map((media) => ({
        variantId: media.variantId || null,
        type: "IMAGE",
        url: media.url,
        altText: media.altText || product.name,
        sortOrder: media.sortOrder || 0
      }));
  }

  function detailMediaFromProduct(product) {
    const detailItems = {};
    detailMediaConfigs.forEach((config) => {
      detailItems[config.key] = config.multiple ? [] : null;
    });
    (product.media || [])
      .filter((media) => media.type === "DETAIL_IMAGE")
      .forEach((media) => {
        const config = detailMediaConfigs.find((item) => item.label === media.altText)
          || detailMediaConfigs.find((item) => item.multiple ? !detailItems[item.key].length : !detailItems[item.key]);
        if (!config) return;
        const item = {
          variantId: media.variantId || null,
          type: "DETAIL_IMAGE",
          url: media.url,
          altText: config.label,
          sortOrder: media.sortOrder || config.sortOrder
        };
        if (config.multiple) {
          detailItems[config.key].push(item);
        } else {
          detailItems[config.key] = item;
        }
      });
    detailMediaConfigs.forEach((config) => {
      if (config.multiple) {
        detailItems[config.key].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
      }
    });
    return detailItems;
  }

  function variantsFromProduct(product) {
    return (product.variants || [])
      .filter((variant) => includeDeletedVariants || variant.status !== "DELETED")
      .map((variant) => ({
      key: String(variant.id),
      id: variant.id,
      variantName: variant.variantName || "",
      widthMm: valueOrEmpty(variant.widthMm),
      lengthMm: valueOrEmpty(variant.lengthMm),
      heightMm: valueOrEmpty(variant.heightMm),
      thicknessMm: valueOrEmpty(variant.thicknessMm),
      weightKg: valueOrEmpty(variant.weightKg),
      twentyFiveTonQuantity: valueOrEmpty(variant.twentyFiveTonQuantity),
      unit: variant.unit || product.unit || "개",
      status: variant.status === "DISCONTINUED" ? "HIDDEN" : variant.status || "ON_SALE",
      priceId: variant.price?.id || null,
      salePrice: valueOrEmpty(variant.price?.salePrice),
      vatPolicy: variant.price?.vatPolicy || "VAT_EXCLUDED",
      freightPolicy: variant.price?.freightPolicy || "FREIGHT_EXCLUDED"
    }));
  }

  function newVariantRow(overrides) {
    const unit = document.querySelector('[name="unit"]')?.value || "개";
    return Object.assign({
      key: "new-" + tempVariantSeq++,
      id: null,
      variantName: "",
      widthMm: "",
      lengthMm: "",
      heightMm: "",
      thicknessMm: "",
      weightKg: "",
      twentyFiveTonQuantity: "",
      unit,
      status: "ON_SALE",
      priceId: null,
      salePrice: "",
      vatPolicy: "VAT_EXCLUDED",
      freightPolicy: "FREIGHT_EXCLUDED"
    }, overrides || {});
  }

  function valueOrEmpty(value) {
    return value === null || value === undefined ? "" : String(value);
  }

  function fillProductForm(form, product) {
    const categoryId = String(product.category.id);
    const rootCategoryId = String(rootCategoryIdFor(categoryId) || "");
    if (form.rootCategoryId) {
      renderRootCategoryOptions(form, rootCategoryId);
      form.rootCategoryId.value = rootCategoryId;
    }
    renderCategoryOptions(form, categoryId);
    form.categoryId.value = categoryId;
    form.name.value = product.name || "";
    form.summary.value = product.summary || "";
    form.searchKeywords.value = product.searchKeywords || "";
    form.description.value = product.description || "";
    form.unit.value = product.unit || "";
    form.originCountry.value = product.originCountry || "";
    form.manufacturer.value = product.manufacturer || "";
    form.customMade.checked = Boolean(product.customMade);
    form.status.value = product.status === "DISCONTINUED" || product.status === "DELETED" ? "HIDDEN" : product.status || "DRAFT";
  }

  async function loadPriceBook() {
    try {
      const books = await app.request("/admin/price-books");
      const book = books.find((item) => item.defaultBook) || books[0];
      priceBookId = book ? book.id : null;
    } catch (error) {
      priceBookId = null;
    }
  }

  async function loadCategories(form) {
    try {
      const tree = await app.request("/categories/tree");
      categoryOptions = flattenCategories(tree);
      renderRootCategoryOptions(form, "");
      renderCategoryOptions(form, "");
    } catch (error) {
      categoryOptions = [];
      renderRootCategoryOptions(form, "");
      renderCategoryOptions(form, "");
      app.setState("#admin-product-form-state", "카테고리 목록을 불러오지 못했습니다. " + error.message, "error");
    }
  }

  function flattenCategories(nodes, depth, parentName) {
    return (nodes || []).flatMap((node) => {
      const currentDepth = node.depth || (depth || 0) + 1;
      const item = {
        id: node.id,
        name: node.name,
        parentId: node.parentId || null,
        parentName: parentName || "",
        pathName: parentName ? `${parentName} > ${node.name}` : node.name,
        active: node.active,
        depth: currentDepth,
        isLeaf: !(node.children || []).length
      };
      return [item].concat(flattenCategories(node.children || [], currentDepth, item.pathName));
    });
  }

  function renderRootCategoryOptions(form, selectedId) {
    const select = form?.rootCategoryId;
    if (!select) return;
    const currentValue = selectedId || select.value || "";
    const roots = categoryOptions.filter((category) => !category.parentId && category.depth === 1);
    select.innerHTML = `<option value="">최상위 카테고리 선택</option>` + roots.map((category) => {
      const activeLabel = category.active === false ? " (비활성)" : "";
      return `<option value="${category.id}">${app.escapeHtml(category.name + activeLabel)}</option>`;
    }).join("");
    select.value = currentValue;
  }

  function renderCategoryOptions(form, selectedId) {
    const select = form?.categoryId;
    if (!select) return;
    const currentValue = selectedId || select.value || "";
    const rootId = Number(form?.rootCategoryId?.value || rootCategoryIdFor(currentValue) || 0);
    const productCategories = categoryOptions
      .filter(isProductCategory)
      .filter((category) => !rootId || Number(category.parentId) === rootId);
    select.disabled = !rootId;
    select.innerHTML = `<option value="">${rootId ? "세부 카테고리 선택" : "최상위 카테고리를 먼저 선택"}</option>` + productCategories.map((category) => {
      const activeLabel = category.active === false ? " (비활성)" : "";
      return `<option value="${category.id}">${app.escapeHtml(category.name + activeLabel)}</option>`;
    }).join("");
    select.value = currentValue;
    if (currentValue && select.value !== currentValue) {
      select.value = "";
    }
  }

  function isProductCategory(category) {
    return category.parentId && category.depth === 2 && category.isLeaf;
  }

  function rootCategoryIdFor(categoryId) {
    const category = categoryOptions.find((item) => Number(item.id) === Number(categoryId));
    return category?.parentId || "";
  }

  function bindForm(form) {
    form.addEventListener("input", (event) => {
      if (event.target.closest("#admin-variant-list")) return;
      renderPreview();
    });
    form.addEventListener("change", (event) => {
      if (event.target.closest("#admin-variant-list")) return;
      if (event.target.name === "rootCategoryId") {
        renderCategoryOptions(form, "");
      }
      renderPreview();
    });
    form.querySelector("#admin-image-upload-button")?.addEventListener("click", uploadSelectedImages);
    form.querySelector("#admin-media-list")?.addEventListener("click", handleMediaClick);
    form.querySelector("#admin-detail-media-list")?.addEventListener("click", handleDetailMediaClick);
    form.querySelector("#admin-variant-add")?.addEventListener("click", () => {
      variantRows.push(newVariantRow());
      renderVariantList();
      renderPreview();
    });
    form.querySelector("#admin-variant-include-deleted")?.addEventListener("click", toggleDeletedVariants);
    form.querySelector("#admin-variant-list")?.addEventListener("input", handleVariantInput);
    form.querySelector("#admin-variant-list")?.addEventListener("change", handleVariantInput);
    form.querySelector("#admin-variant-list")?.addEventListener("click", handleVariantClick);
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      await saveAll(form);
    });
  }

  async function saveAll(form) {
    try {
      validateVariantRows();
      const productId = currentProduct?.id || app.qs("id");
      const path = productId ? "/admin/products/" + productId : "/admin/products";
      const method = productId ? "PATCH" : "POST";
      const saved = await app.request(path, { method, body: productPayload(form) });
      currentProduct = saved;
      if (!app.qs("id")) {
        history.replaceState(null, "", "admin-product-form.html?id=" + saved.id);
      }
      await saveVariantsAndPrices(saved.id);
      currentProduct = await app.request("/admin/products/" + saved.id);
      mediaItems = mediaFromProduct(currentProduct);
      detailMediaItems = detailMediaFromProduct(currentProduct);
      variantRows = variantsFromProduct(currentProduct);
      fillProductForm(form, currentProduct);
      renderMediaList();
      renderDetailMediaList();
      renderVariantList();
      renderPreview();
      app.notify(`상품, 규격, 가격이 저장되었습니다. ID ${currentProduct.id}`);
      location.href = "admin-products.html";
    } catch (error) {
      app.setState("#admin-product-form-state", error.message, "error");
    }
  }

  function productPayload(form) {
    const data = Object.fromEntries(new FormData(form).entries());
    if (!data.categoryId) {
      throw new Error("상품을 연결할 세부 카테고리를 선택해주세요.");
    }
    delete data.rootCategoryId;
    data.categoryId = Number(data.categoryId);
    const selectedCategory = categoryOptions.find((category) => category.id === data.categoryId);
    if (!selectedCategory || !isProductCategory(selectedCategory)) {
      throw new Error("상품은 최상위 카테고리가 아니라 세부 카테고리에만 연결할 수 있습니다.");
    }
    data.customMade = form.customMade.checked;
    data.status = data.status || "DRAFT";
    data.media = mediaItems.map((media, index) => ({
      variantId: media.variantId || null,
      type: "IMAGE",
      url: media.url,
      altText: media.altText || data.name || "상품 이미지",
      sortOrder: (index + 1) * 10
    })).concat(detailMediaConfigs.flatMap((config) => {
      const media = detailMediaItems[config.key];
      const items = config.multiple ? (media || []) : [media].filter(Boolean);
      return items.map((item, index) => ({
        variantId: item.variantId || null,
        type: "DETAIL_IMAGE",
        url: item.url,
        altText: config.label,
        sortOrder: config.sortOrder + index * 10
      }));
    }));
    return data;
  }

  function validateVariantRows() {
    const rows = editableVariantRows();
    if (!rows.length) {
      throw new Error("최소 1개 이상의 규격을 등록해주세요.");
    }
    let hasSalePrice = false;
    rows.forEach((row, index) => {
      if (!row.variantName.trim()) {
        throw new Error(`${index + 1}번째 규격명을 입력해주세요.`);
      }
      variantDecimalFields.forEach((label, field) => {
        parseOptionalDecimal(row[field], `${row.variantName} ${label}`);
      });
      const salePrice = parseOptionalInteger(row.salePrice, `${row.variantName} 판매단가`);
      if (salePrice !== null) hasSalePrice = true;
      if (row.status === "ON_SALE" && salePrice === null) {
        throw new Error(`${row.variantName} 규격의 판매단가를 입력해주세요.`);
      }
      if (salePrice !== null && salePrice <= 0) {
        throw new Error(`${row.variantName} 규격의 판매단가는 0보다 커야 합니다.`);
      }
    });
    if (hasSalePrice && !priceBookId) {
      throw new Error("가격표가 없습니다. 먼저 가격표를 생성해주세요.");
    }
  }

  async function saveVariantsAndPrices(productId) {
    for (const row of editableVariantRows()) {
      const variantBody = {
        variantName: row.variantName.trim(),
        widthMm: parseOptionalDecimal(row.widthMm, `${row.variantName} 폭(mm)`),
        lengthMm: parseOptionalDecimal(row.lengthMm, `${row.variantName} 길이(mm)`),
        heightMm: parseOptionalDecimal(row.heightMm, `${row.variantName} 높이(mm)`),
        thicknessMm: parseOptionalDecimal(row.thicknessMm, `${row.variantName} 두께(mm)`),
        weightKg: parseOptionalDecimal(row.weightKg, `${row.variantName} 중량(kg)`),
        twentyFiveTonQuantity: parseOptionalDecimal(row.twentyFiveTonQuantity, `${row.variantName} 25톤`),
        unit: row.unit.trim() || document.querySelector('[name="unit"]')?.value || "개",
        barcode: null,
        status: row.status || "ON_SALE"
      };
      const variant = await app.request(
        row.id ? "/admin/product-variants/" + row.id : "/admin/products/" + productId + "/variants",
        { method: row.id ? "PATCH" : "POST", body: variantBody }
      );
      row.id = variant.id;
      const salePrice = parseOptionalInteger(row.salePrice, `${row.variantName} 판매단가`);
      if (salePrice !== null) {
        await savePrice(productId, variant.id, row, salePrice);
      } else if (row.priceId) {
        await deletePrice(row);
      }
    }
  }

  function editableVariantRows() {
    return variantRows.filter((row) => row.status !== "DELETED");
  }

  async function savePrice(productId, variantId, row, salePrice) {
    const body = {
      priceBookId,
      productId,
      variantId,
      salePrice,
      vatPolicy: row.vatPolicy || "VAT_EXCLUDED",
      freightPolicy: row.freightPolicy || "FREIGHT_EXCLUDED",
      minOrderQuantity: 1,
      priceNote: "관리자 상품 수정에서 입력"
    };
    const saved = await app.request(row.priceId ? "/admin/product-prices/" + row.priceId : "/admin/product-prices", {
      method: row.priceId ? "PATCH" : "POST",
      body
    });
    row.priceId = saved.id;
  }

  async function deletePrice(row) {
    await app.request("/admin/product-prices/" + row.priceId, { method: "DELETE" });
    row.priceId = null;
  }

  function normalizeNumberText(value) {
    return String(value ?? "").replace(/,/g, "").trim();
  }

  function parseOptionalDecimal(value, label) {
    const normalized = normalizeNumberText(value);
    if (!normalized) return null;
    if (!/^\d+(\.\d+)?$/.test(normalized)) {
      throw new Error(`${label}에는 숫자만 입력해주세요.`);
    }
    return Number(normalized);
  }

  function parseOptionalInteger(value, label) {
    const normalized = normalizeNumberText(value);
    if (!normalized) return null;
    if (!/^\d+$/.test(normalized)) {
      throw new Error(`${label}에는 정수만 입력해주세요.`);
    }
    return Number(normalized);
  }

  function previewNumber(value) {
    const normalized = normalizeNumberText(value);
    if (!normalized || !/^\d+(\.\d+)?$/.test(normalized)) return null;
    return Number(normalized);
  }

  async function uploadSelectedImages() {
    const input = document.querySelector("#admin-image-upload");
    const files = Array.from(input?.files || []);
    if (!files.length) {
      app.setState("#admin-product-form-state", "업로드할 이미지 파일을 선택해주세요.", "error");
      return;
    }
    try {
      app.setState("#admin-product-form-state", "이미지를 업로드하고 있습니다.", "notice");
      for (const file of files) {
        const formData = new FormData();
        formData.append("file", file);
        const response = await fetch(window.APP_CONFIG.API_BASE_URL + "/admin/products/images", {
          method: "POST",
          headers: uploadHeaders(),
          body: formData
        });
        const text = await response.text();
        let payload = {};
        try {
          payload = text ? JSON.parse(text) : {};
        } catch (error) {
          payload = { message: text };
        }
        if (!response.ok) {
          throw new Error(payload.message || "이미지 업로드에 실패했습니다.");
        }
        if (!payload.url) {
          throw new Error("업로드 응답에서 이미지 경로를 확인할 수 없습니다.");
        }
        mediaItems.push({
          variantId: null,
          type: "IMAGE",
          url: payload.url,
          altText: document.querySelector('[name="name"]')?.value || file.name,
          sortOrder: mediaItems.length * 10 + 10
        });
      }
      input.value = "";
      renderMediaList();
      renderPreview();
      app.setState("#admin-product-form-state", "이미지가 목록에 추가되었습니다. 상품 정보 저장을 눌러 반영해주세요.", "notice");
    } catch (error) {
      app.setState("#admin-product-form-state", error.message, "error");
    }
  }

  function uploadHeaders() {
    const headers = { "X-Session-Id": window.APP_CONFIG.SESSION_ID };
    if (app.token()) headers.Authorization = "Bearer " + app.token();
    return headers;
  }

  function handleMediaClick(event) {
    const button = event.target.closest("button[data-media-action]");
    if (!button) return;
    const index = Number(button.dataset.index);
    const action = button.dataset.mediaAction;
    if (action === "delete") {
      mediaItems.splice(index, 1);
    }
    if (action === "up" && index > 0) {
      [mediaItems[index - 1], mediaItems[index]] = [mediaItems[index], mediaItems[index - 1]];
    }
    if (action === "down" && index < mediaItems.length - 1) {
      [mediaItems[index + 1], mediaItems[index]] = [mediaItems[index], mediaItems[index + 1]];
    }
    renderMediaList();
    renderPreview();
    app.setState("#admin-product-form-state", "이미지 변경사항은 상품 정보 저장 시 함께 반영됩니다.", "notice");
  }

  async function handleDetailMediaClick(event) {
    const uploadButton = event.target.closest("button[data-detail-media-upload]");
    const removeButton = event.target.closest("button[data-detail-media-remove]");
    if (uploadButton) {
      await uploadDetailMedia(uploadButton.dataset.detailMediaUpload);
      return;
    }
    if (removeButton) {
      removeDetailMedia(removeButton.dataset.detailMediaRemove, Number(removeButton.dataset.detailMediaIndex || 0));
    }
  }

  async function uploadDetailMedia(key) {
    const config = detailMediaConfigs.find((item) => item.key === key);
    const input = document.querySelector(`[data-detail-media-input="${key}"]`);
    const files = Array.from(input?.files || []);
    if (!config || !files.length) {
      app.setState("#admin-product-form-state", "업로드할 상세 이미지 파일을 선택해주세요.", "error");
      return;
    }
    try {
      app.setState("#admin-product-form-state", `${config.label}를 업로드하고 있습니다.`, "notice");
      for (const file of files) {
        const formData = new FormData();
        formData.append("file", file);
        const response = await fetch(window.APP_CONFIG.API_BASE_URL + "/admin/products/images", {
          method: "POST",
          headers: uploadHeaders(),
          body: formData
        });
        const text = await response.text();
        let payload = {};
        try {
          payload = text ? JSON.parse(text) : {};
        } catch (error) {
          payload = { message: text };
        }
        if (!response.ok) {
          throw new Error(payload.message || "이미지 업로드에 실패했습니다.");
        }
        if (!payload.url) {
          throw new Error("업로드 응답에서 이미지 경로를 확인할 수 없습니다.");
        }
        const item = {
          variantId: null,
          type: "DETAIL_IMAGE",
          url: payload.url,
          altText: config.label,
          sortOrder: nextDetailMediaSortOrder(config)
        };
        if (config.multiple) {
          if (!Array.isArray(detailMediaItems[key])) detailMediaItems[key] = [];
          detailMediaItems[key].push(item);
        } else {
          detailMediaItems[key] = item;
        }
      }
      input.value = "";
      renderDetailMediaList();
      renderPreview();
      app.setState("#admin-product-form-state", `${config.label}가 추가되었습니다. 상품 정보 저장을 눌러 반영해주세요.`, "notice");
    } catch (error) {
      app.setState("#admin-product-form-state", error.message, "error");
    }
  }

  function removeDetailMedia(key, index) {
    const config = detailMediaConfigs.find((item) => item.key === key);
    if (!config || !detailMediaItems[key]) return;
    if (config.multiple) {
      detailMediaItems[key].splice(index, 1);
    } else {
      detailMediaItems[key] = null;
    }
    const input = document.querySelector(`[data-detail-media-input="${key}"]`);
    if (input) input.value = "";
    renderDetailMediaList();
    renderPreview();
    app.setState("#admin-product-form-state", `${config.label} 변경사항은 상품 정보 저장 시 함께 반영됩니다.`, "notice");
  }

  function nextDetailMediaSortOrder(config) {
    const items = config.multiple ? (detailMediaItems[config.key] || []) : [];
    return config.sortOrder + items.length * 10;
  }

  async function toggleDeletedVariants() {
    includeDeletedVariants = !includeDeletedVariants;
    renderVariantDeletedToggle();
    const productId = currentProduct?.id || app.qs("id");
    if (productId) {
      try {
        currentProduct = await app.request("/admin/products/" + productId);
        variantRows = variantsFromProduct(currentProduct);
      } catch (error) {
        app.setState("#admin-product-form-state", error.message, "error");
      }
    }
    renderVariantList();
    renderPreview();
  }

  function renderVariantDeletedToggle() {
    const button = document.querySelector("#admin-variant-include-deleted");
    if (!button) return;
    button.setAttribute("aria-pressed", String(includeDeletedVariants));
    button.textContent = includeDeletedVariants ? "삭제된 규격 숨기기" : "삭제된 규격까지 보기";
  }

  function handleVariantInput(event) {
    const field = event.target.dataset.variantField;
    if (!field) return;
    const row = findVariantRow(event.target.dataset.variantKey);
    if (!row) return;
    row[field] = event.target.value;
    renderPreview();
  }

  async function handleVariantClick(event) {
    const button = event.target.closest("button[data-variant-action]");
    if (!button) return;
    const row = findVariantRow(button.dataset.variantKey);
    if (!row) return;
    if (button.dataset.variantAction === "remove") {
      variantRows = variantRows.filter((item) => item.key !== row.key);
      renderVariantList();
      renderPreview();
      return;
    }
    if (button.dataset.variantAction === "delete") {
      await deleteVariantRow(button, row);
    }
  }

  async function handleProductTreeClick(event) {
    const moveButton = event.target.closest("button[data-product-move]");
    if (moveButton) {
      if (moveButton.disabled) return;
      await moveProduct(Number(moveButton.dataset.productMove), moveButton.dataset.direction);
      return;
    }
    await handleProductDelete(event);
  }

  async function moveProduct(productId, direction) {
    if (isProductOrderLocked()) {
      app.setState("#admin-product-state", "필터를 해제한 뒤 상품 순서를 변경해주세요.", "notice");
      return;
    }
    const product = adminProductRows.find((item) => Number(item.id) === productId);
    if (!product) return;
    try {
      await app.request("/admin/products/" + productId + "/move", {
        method: "PATCH",
        body: { direction }
      });
      app.setState("#admin-product-state", "상품 순서가 변경되었습니다.", "notice");
      const products = await loadProductCategoryList(product.categoryId);
      adminProductRows = adminProductRows
        .filter((item) => Number(item.categoryId) !== Number(product.categoryId))
        .concat(products);
    } catch (error) {
      app.setState("#admin-product-state", error.message, "error");
    }
  }

  async function deleteVariantRow(button, row) {
    if (!row.id) return;
    const name = row.variantName || "선택한 규격";
    if (!confirm(`"${name}" 규격을 삭제할까요?\n삭제하면 상품 상세와 견적 선택 목록에서 바로 사라집니다.`)) {
      return;
    }
    button.disabled = true;
    try {
      await app.request("/admin/product-variants/" + row.id, { method: "DELETE" });
      if (includeDeletedVariants) {
        row.status = "DELETED";
      } else {
        variantRows = variantRows.filter((item) => item.key !== row.key);
      }
      renderVariantList();
      renderPreview();
      app.notify("규격이 삭제되었습니다.");
    } catch (error) {
      app.setState("#admin-product-form-state", error.message, "error");
    } finally {
      button.disabled = false;
    }
  }

  function findVariantRow(key) {
    return variantRows.find((row) => row.key === key);
  }

  function renderMediaList() {
    const list = document.querySelector("#admin-media-list");
    if (!list) return;
    if (!mediaItems.length) {
      list.innerHTML = `<div class="empty">등록된 이미지가 없습니다.</div>`;
      return;
    }
    list.innerHTML = mediaItems.map((media, index) => `
      <article class="admin-media-item">
        <img src="${app.escapeHtml(media.url)}" alt="${app.escapeHtml(media.altText || "상품 이미지")}">
        <div>
          <strong>${index === 0 ? "대표 이미지" : "설명 이미지 " + index}</strong>
          <p class="muted">${index === 0 ? "상품 카드와 상세 상단에 사용됩니다." : "상품 상세 설명 이미지입니다."}</p>
        </div>
        <div class="row">
          <button class="button" type="button" data-media-action="up" data-index="${index}">위로</button>
          <button class="button" type="button" data-media-action="down" data-index="${index}">아래로</button>
          <button class="button danger" type="button" data-media-action="delete" data-index="${index}">삭제</button>
        </div>
      </article>
    `).join("");
  }

  function renderDetailMediaList() {
    const list = document.querySelector("#admin-detail-media-list");
    if (!list) return;
    list.innerHTML = detailMediaConfigs.map((config) => {
      const value = detailMediaItems[config.key];
      const items = config.multiple ? (value || []) : [value].filter(Boolean);
      return `
        <article class="admin-detail-media-item">
          <div>
            <strong>${app.escapeHtml(config.label)}</strong>
            <p class="muted">${items.length ? app.escapeHtml(items.length + "장 등록됨") : "등록된 이미지가 없습니다."}</p>
          </div>
          <div class="admin-detail-media-preview${config.multiple ? " is-multiple" : ""}">
            ${items.length ? items.map((media, index) => `
              <figure class="admin-detail-media-thumb">
                <img src="${app.escapeHtml(media.url)}" alt="${app.escapeHtml(config.label)} ${index + 1}">
                ${config.multiple ? `<button class="button danger" type="button" data-detail-media-remove="${app.escapeHtml(config.key)}" data-detail-media-index="${index}">삭제</button>` : ""}
              </figure>
            `).join("") : '<span class="muted">미등록</span>'}
          </div>
          <div class="row admin-detail-media-actions">
            <label class="admin-field">${app.escapeHtml(config.label)} 파일
              <input class="input" data-detail-media-input="${app.escapeHtml(config.key)}" type="file" accept="image/*"${config.multiple ? " multiple" : ""}>
            </label>
            <button class="button" type="button" data-detail-media-upload="${app.escapeHtml(config.key)}">업로드</button>
            ${config.multiple ? "" : `<button class="button danger" type="button" data-detail-media-remove="${app.escapeHtml(config.key)}"${items.length ? "" : " disabled"}>삭제</button>`}
          </div>
        </article>
      `;
    }).join("");
  }

  function renderVariantList() {
    const list = document.querySelector("#admin-variant-list");
    if (!list) return;
    if (!variantRows.length) {
      list.innerHTML = `<div class="empty">등록된 규격이 없습니다. 규격 추가를 눌러 입력해주세요.</div>`;
      return;
    }
    list.innerHTML = variantRows.map((row, index) => `
      <article class="${variantRowClass(row)}">
        <div class="admin-variant-main">
          <label>규격명
            <input class="input" data-variant-key="${app.escapeHtml(row.key)}" data-variant-field="variantName" value="${app.escapeHtml(row.variantName)}" placeholder="예: 300x1000"${variantDisabledAttr(row)}>
          </label>
          <label>판매단가
            <input class="input" data-variant-key="${app.escapeHtml(row.key)}" data-variant-field="salePrice" type="text" inputmode="numeric" autocomplete="off" value="${app.escapeHtml(row.salePrice)}" placeholder="예: 44100"${variantDisabledAttr(row)}>
          </label>
          <label>상태
            <select class="input" data-variant-key="${app.escapeHtml(row.key)}" data-variant-field="status"${variantDisabledAttr(row)}>
              ${variantStatusOptions(row)}
            </select>
          </label>
        </div>
        <div class="admin-variant-fields">
          ${variantInput(row, "unit", "단위", "개")}
          ${variantInput(row, "widthMm", "폭(mm)", "300", "number")}
          ${variantInput(row, "lengthMm", "길이(mm)", "1000", "number")}
          ${variantInput(row, "heightMm", "높이(mm)", "150", "number")}
          ${variantInput(row, "thicknessMm", "두께(mm)", "50", "number")}
          ${variantInput(row, "weightKg", "중량(kg)", "18", "number")}
          ${variantInput(row, "twentyFiveTonQuantity", "25톤", "예: 300", "number")}
        </div>
        <div class="admin-variant-actions">
          <span class="muted">${row.id ? "규격 번호 " + row.id : "신규 규격 " + (index + 1)}</span>
          ${variantActionHtml(row)}
        </div>
      </article>
    `).join("");
  }

  function variantRowClass(row) {
    return "admin-variant-row" + (["HIDDEN", "DELETED"].includes(row.status) ? " is-muted" : "");
  }

  function variantDisabledAttr(row) {
    return row.status === "DELETED" ? " disabled" : "";
  }

  function variantStatusOptions(row) {
    return [
      statusOption("ON_SALE", row.status, "판매중"),
      statusOption("QUOTE_ONLY", row.status, "견적전용"),
      statusOption("SOLD_OUT", row.status, "품절"),
      statusOption("HIDDEN", row.status, "숨김"),
      row.status === "DELETED" ? statusOption("DELETED", row.status, "삭제됨") : ""
    ].join("");
  }

  function variantActionHtml(row) {
    if (row.status === "DELETED") {
      return '<span class="muted">삭제됨</span>';
    }
    return `<button class="button danger" type="button" data-variant-action="${row.id ? "delete" : "remove"}" data-variant-key="${app.escapeHtml(row.key)}">${row.id ? "삭제" : "행 제거"}</button>`;
  }

  function statusOption(value, selected, label) {
    return `<option value="${value}"${value === selected ? " selected" : ""}>${label}</option>`;
  }

  function variantInput(row, field, label, placeholder, type) {
    const inputType = type === "number" ? "text" : type || "text";
    const numberAttrs = type === "number" ? ' inputmode="decimal" autocomplete="off" data-number-input="decimal"' : "";
    return `
      <label>${app.escapeHtml(label)}
        <input class="input" data-variant-key="${app.escapeHtml(row.key)}" data-variant-field="${field}" type="${inputType}"${numberAttrs} value="${app.escapeHtml(row[field])}" placeholder="${app.escapeHtml(placeholder || "")}"${variantDisabledAttr(row)}>
      </label>
    `;
  }

  function previewVariantChips(rows) {
    if (!rows.length) return '<p class="muted">등록된 규격이 없습니다.</p>';
    const chips = rows
      .map((row) => {
        const name = row.variantName || "이름 없는 규격";
        return `<span class="variant-chip" title="${app.escapeHtml(name)}">${app.escapeHtml(name)}</span>`;
      })
      .join("");
    return `<div class="variant-chip-list" data-chip-list>${chips}<span class="variant-chip variant-chip-more" data-chip-more hidden title="추가 규격 있음">...</span></div>`;
  }

  function fitVariantChipList(list) {
    const chips = Array.from(list.querySelectorAll(".variant-chip:not(.variant-chip-more)"));
    const more = list.querySelector("[data-chip-more]");
    if (!chips.length || !more) return;
    chips.forEach((chip) => {
      chip.hidden = false;
    });
    more.hidden = true;
    more.title = "추가 규격 있음";
    if (!list.clientWidth || list.scrollWidth <= list.clientWidth + 1) return;

    more.hidden = false;
    let hiddenCount = 0;
    for (let index = chips.length - 1; index >= 0 && list.scrollWidth > list.clientWidth + 1; index -= 1) {
      chips[index].hidden = true;
      hiddenCount += 1;
    }
    if (hiddenCount) {
      more.title = `추가 규격 ${hiddenCount}개`;
    }
  }

  function fitVariantChipLists() {
    document.querySelectorAll("[data-chip-list]").forEach(fitVariantChipList);
  }

  function renderPreview() {
    const form = document.querySelector("#admin-product-form");
    if (!form) return;
    const imageUrl = mediaItems[0]?.url || "assets/images/placeholder.svg";
    const name = form.name.value || "상품명";
    const summary = form.summary.value || "상품 요약이 이곳에 표시됩니다.";
    const description = form.description.value || "상품 상세 설명이 이곳에 표시됩니다.";
    const unit = form.unit.value || "개";
    const categoryName = selectedCategoryName(form) || "카테고리";
    const activeVariants = variantRows.filter((row) => row.status !== "HIDDEN" && row.status !== "DELETED");
    const variant = activeVariants[0] || variantRows[0];
    const variantChips = previewVariantChips(activeVariants);
    const priceValue = previewNumber(variant?.salePrice);
    const weightValue = previewNumber(variant?.weightKg);
    const weightText = weightValue !== null ? `${weightValue.toLocaleString("ko-KR", { maximumFractionDigits: 2 })} kg` : "-";
    const unitText = unitLabel(variant?.unit || unit);
    const priceText = priceValue ? app.money(priceValue) : "견적문의";
    const vatText = app.label("vatPolicy", variant?.vatPolicy || "VAT_EXCLUDED") || "부가세 별도";
    const card = document.querySelector("#admin-card-preview");
    const detail = document.querySelector("#admin-detail-preview");
    if (card) {
      card.innerHTML = `
        <article class="card product-card admin-preview-card">
          <h3>${app.escapeHtml(name)}</h3>
          <div class="thumb"><img src="${app.escapeHtml(imageUrl)}" alt="${app.escapeHtml(name)}"></div>
          <div class="grid">
            <p class="muted">${app.escapeHtml(summary)}</p>
            ${variantChips}
            <p class="price">${app.escapeHtml(app.unitPrice(priceValue, variant?.unit || unit))}</p>
          </div>
        </article>
      `;
    }
    if (detail) {
      detail.innerHTML = `
        <div class="detail-layout admin-detail-preview-top">
          <section class="detail-title-block">
            <span class="badge">${app.escapeHtml(categoryName)}</span>
            <h1>${app.escapeHtml(name)}</h1>
            <p class="muted">${app.escapeHtml(description)}</p>
            <div class="card detail-variant-summary">
              <p><strong>규격</strong> ${app.escapeHtml(variant?.variantName || "-")}</p>
              <p><strong>중량</strong> ${app.escapeHtml(weightText)}</p>
              <p><strong>단위</strong> ${app.escapeHtml(unitText)}</p>
              <p><strong>판매단가</strong> ${app.escapeHtml(priceText)}</p>
              <p class="muted">${app.escapeHtml(vatText)}</p>
            </div>
          </section>
          <div class="detail-image detail-main-image"><img src="${app.escapeHtml(imageUrl)}" alt="${app.escapeHtml(name)}"></div>
        </div>
      `;
    }
    requestAnimationFrame(fitVariantChipLists);
  }

  function selectedCategoryName(form) {
    const id = Number(form.categoryId.value || 0);
    return categoryOptions.find((category) => category.id === id)?.name || "";
  }

  function unitLabel(unit) {
    const value = unit || "-";
    return value === "-" || /^\d/.test(value) ? value : "1" + value;
  }

  function formatTwentyFiveTon(value, unit) {
    if (value === null || value === undefined || value === "") return "-";
    const number = Number(value);
    if (!Number.isFinite(number)) return String(value);
    return number.toLocaleString("ko-KR", { maximumFractionDigits: 2 }) + (unit || "개");
  }

  document.addEventListener("DOMContentLoaded", () => {
    const search = document.querySelector("#admin-product-search");
    if (search) search.addEventListener("submit", (event) => { event.preventDefault(); render(); });
    document.querySelector("#admin-product-status")?.addEventListener("change", render);
    document.querySelector("#admin-product-include-deleted")?.addEventListener("click", () => {
      includeDeletedProducts = !includeDeletedProducts;
      renderProductDeletedToggle();
      render();
    });
    document.querySelector("#admin-product-tree")?.addEventListener("click", handleProductTreeClick);
    window.addEventListener("resize", () => requestAnimationFrame(fitVariantChipLists));
    renderProductDeletedToggle();
    render();
    form();
  });
})();
