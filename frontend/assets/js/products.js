(function () {
  let currentPage = Math.max(1, Number(app.qs("page", "1")) || 1);
  let categoryCache = [];
  const categoryMenuProductCache = new Map();
  const categoryMenuCloseTimers = new WeakMap();
  const pageSize = 12;
  const recentSearchKey = "civilshop_recent_searches";

  function variantChips(variantNames) {
    if (!variantNames.length) return '<p class="muted">등록된 규격이 없습니다.</p>';
    const chips = variantNames
      .map((name) => `<span class="variant-chip" title="${app.escapeHtml(name)}">${app.escapeHtml(name)}</span>`)
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

  function scheduleVariantChipFit() {
    requestAnimationFrame(fitVariantChipLists);
  }

  function recentSearches() {
    try {
      return JSON.parse(localStorage.getItem(recentSearchKey) || "[]").filter(Boolean);
    } catch (error) {
      return [];
    }
  }

  function saveRecentSearch(keyword) {
    const value = String(keyword || "").trim();
    if (!value) return;
    const normalized = value.toLowerCase();
    const next = [value].concat(recentSearches().filter((item) => item.toLowerCase() !== normalized)).slice(0, 8);
    localStorage.setItem(recentSearchKey, JSON.stringify(next));
  }

  function bindRecentSearch(input, onPick) {
    if (!input || input.dataset.recentSearchBound) return;
    input.dataset.recentSearchBound = "true";
    input.parentElement?.classList.add("search-history-field");
    const panel = document.createElement("div");
    panel.className = "recent-search-panel";
    panel.hidden = true;
    input.insertAdjacentElement("afterend", panel);

    function render() {
      const items = recentSearches();
      panel.innerHTML = items.length
        ? items.map((item) => `<button type="button" data-recent-search="${app.escapeHtml(item)}">${app.escapeHtml(item)}</button>`).join("")
        : '<p class="muted">최근 검색어가 없습니다.</p>';
    }

    function show() {
      render();
      panel.hidden = false;
    }

    function hide() {
      panel.hidden = true;
    }

    input.addEventListener("focus", show);
    input.addEventListener("click", show);
    input.addEventListener("keydown", (event) => {
      if (event.key === "Escape") hide();
    });
    panel.addEventListener("mousedown", (event) => event.preventDefault());
    panel.addEventListener("click", (event) => {
      const button = event.target.closest("[data-recent-search]");
      if (!button) return;
      input.value = button.dataset.recentSearch;
      hide();
      if (onPick) onPick(input.value);
    });
    document.addEventListener("mousedown", (event) => {
      if (event.target === input || panel.contains(event.target)) return;
      hide();
    });
  }

  function productCard(product) {
    const imageUrl = product.representativeImageUrl || "assets/images/placeholder.svg";
    const variantNames = product.variantNames && product.variantNames.length
      ? product.variantNames
      : [product.representativeVariantName].filter(Boolean);
    const variants = variantChips(variantNames);
    return `
      <article class="card product-card">
        <h3><a href="product-detail.html?id=${app.escapeHtml(product.id)}">${app.escapeHtml(product.name)}</a></h3>
        <a class="thumb" href="product-detail.html?id=${app.escapeHtml(product.id)}">
          <img src="${app.escapeHtml(imageUrl)}" alt="${app.escapeHtml(product.name)}">
        </a>
        <div class="grid">
          <p class="muted">${app.escapeHtml(product.summary || "")}</p>
          ${variants}
          <p class="price">${app.escapeHtml(app.unitPrice(product.salePrice, product.unit || ""))}</p>
        </div>
      </article>
    `;
  }

  async function renderCategories(target) {
    const el = document.querySelector(target);
    if (!el) return;
    const roots = await app.request("/categories/tree");
    const flat = [];
    function walk(nodes, root) {
      nodes.forEach((node) => {
        const rootNode = root || node;
        flat.push(Object.assign({}, node, { rootId: rootNode.id }));
        walk(node.children || [], rootNode);
      });
    }
    walk(roots);
    categoryCache = flat;
    if (document.querySelector("#featured-products")) {
      el.className = "category-grid";
      el.innerHTML = roots.map((category) => app.html`
        <a class="category-card" href="products.html?categoryId=${category.id}">
          <img src="${category.imageUrl || "assets/images/placeholder.svg"}" alt="${category.name}">
          <span>${category.name}</span>
        </a>
      `).join("");
    } else {
      const selectedId = Number(app.qs("categoryId", "0"));
      const selected = flat.find((category) => category.id === selectedId);
      const activeRootId = selected ? selected.rootId : 0;
      const rootLinks = (await Promise.all(roots.map((category) => categoryMenuItem(category, activeRootId)))).join("");
      el.className = "category-filter-panel";
      el.innerHTML = `
        <div class="category-filter-title">카테고리</div>
        <div class="category-pill-row">
          <a class="category-pill ${activeRootId ? "" : "is-active"}" href="products.html">전체</a>
          ${rootLinks}
        </div>
        <p class="category-filter-help">최상위 카테고리에 마우스를 올리면 세부 카테고리와 상품을 바로 볼 수 있습니다.</p>
      `;
      bindCategoryHoverMenus(el);
      updateCategoryPillState(app.qs("categoryId", ""));
    }
    const select = document.querySelector("#category-filter");
    if (select) {
      const selectedId = Number(app.qs("categoryId", "0"));
      const selected = flat.find((category) => category.id === selectedId);
      const activeRootId = selected ? selected.rootId : selectedId;
      select.innerHTML = '<option value="">전체 카테고리</option>' + roots.map((category) => app.html`
        <option value="${category.id}">${category.name}</option>
      `).join("");
      select.value = activeRootId ? String(activeRootId) : "";
      select.dataset.selectedCategoryId = selectedId ? String(selectedId) : "";
    }
  }

  async function categoryMenuItem(root, activeRootId) {
    const menu = await categoryMegaMenu(root);
    return `
      <div class="category-menu-item">
        <a class="category-pill ${activeRootId === root.id ? "is-active" : ""}" href="products.html?categoryId=${app.escapeHtml(root.id)}">${app.escapeHtml(root.name)}</a>
        ${menu}
      </div>
    `;
  }

  async function categoryMegaMenu(root) {
    const sections = root.children && root.children.length ? root.children : [root];
    const sectionData = await Promise.all(sections.map(async (category) => ({
      category,
      products: await categoryMenuProducts(category.id)
    })));
    return `
      <div class="category-mega-menu" role="menu" aria-label="${app.escapeHtml(root.name)} 세부 카테고리">
        <div class="category-mega-grid">
          ${sectionData.map(({ category, products }) => `
            <section class="category-mega-section">
              <a class="category-mega-title" href="products.html?categoryId=${app.escapeHtml(category.id)}">${app.escapeHtml(category.name)}</a>
              <div class="category-mega-products">
                ${products.length ? products.map(categoryMenuProduct).join("") : '<p class="muted">등록된 상품이 없습니다.</p>'}
              </div>
            </section>
          `).join("")}
        </div>
      </div>
    `;
  }

  async function categoryMenuProducts(categoryId) {
    if (categoryMenuProductCache.has(categoryId)) {
      return categoryMenuProductCache.get(categoryId);
    }
    try {
      const page = await app.request("/products?categoryId=" + encodeURIComponent(categoryId) + "&size=8&sort=name");
      const products = page.items || [];
      categoryMenuProductCache.set(categoryId, products);
      return products;
    } catch (error) {
      categoryMenuProductCache.set(categoryId, []);
      return [];
    }
  }

  function categoryMenuProduct(product) {
    const imageUrl = product.representativeImageUrl || "assets/images/placeholder.svg";
    return `
      <a class="category-mega-product" href="product-detail.html?id=${app.escapeHtml(product.id)}">
        <img src="${app.escapeHtml(imageUrl)}" alt="${app.escapeHtml(product.name)}">
        <span>${app.escapeHtml(product.name)}</span>
      </a>
    `;
  }

  function bindCategoryHoverMenus(root) {
    root.querySelectorAll(".category-menu-item").forEach((item) => {
      if (item.dataset.hoverMenuBound) return;
      item.dataset.hoverMenuBound = "true";
      item.addEventListener("mouseenter", () => openCategoryMenu(item));
      item.addEventListener("mouseleave", () => closeCategoryMenuSoon(item));
      item.addEventListener("focusin", () => openCategoryMenu(item));
      item.addEventListener("focusout", (event) => {
        if (item.contains(event.relatedTarget)) return;
        closeCategoryMenuSoon(item);
      });
    });
  }

  function openCategoryMenu(item) {
    const timer = categoryMenuCloseTimers.get(item);
    if (timer) {
      clearTimeout(timer);
      categoryMenuCloseTimers.delete(item);
    }
    document.querySelectorAll(".category-menu-item.is-open").forEach((openItem) => {
      if (openItem !== item) openItem.classList.remove("is-open");
    });
    item.classList.add("is-open");
  }

  function closeCategoryMenuSoon(item) {
    const timer = categoryMenuCloseTimers.get(item);
    if (timer) clearTimeout(timer);
    categoryMenuCloseTimers.set(item, setTimeout(() => {
      item.classList.remove("is-open");
      categoryMenuCloseTimers.delete(item);
    }, 220));
  }

  async function renderPopular() {
    const el = document.querySelector("#popular-keywords");
    if (!el) return;
    const keywords = await app.request("/search/popular");
    el.innerHTML = (keywords.length ? keywords : [{ keyword: "그레이팅", count: 0 }, { keyword: "수로관", count: 0 }, { keyword: "경계석", count: 0 }])
      .map((item) => app.html`<a class="chip" href="products.html?keyword=${item.keyword}">${item.keyword}</a>`)
      .join("");
  }

  async function renderProducts() {
    const el = document.querySelector("#product-list");
    if (!el) return;
    try {
      app.setState(el, "상품을 불러오는 중입니다.", "notice");
      const keyword = document.querySelector("#keyword-filter")?.value || app.qs("keyword", "");
      saveRecentSearch(keyword);
      const categorySelect = document.querySelector("#category-filter");
      const categoryId = categorySelect?.dataset.selectedCategoryId || categorySelect?.value || app.qs("categoryId", "");
      const sort = document.querySelector("#sort-filter")?.value || "latest";
      const params = new URLSearchParams({ page: String(currentPage), size: String(pageSize), sort });
      if (keyword) params.set("keyword", keyword);
      if (categoryId) params.set("categoryId", categoryId);
      syncProductsUrl({ keyword, categoryId, sort });
      updateCategoryPillState(categoryId);
      const page = await app.request("/products?" + params.toString());
      if (!page.items.length) {
        app.setState(el, "조건에 맞는 상품이 없습니다.", "empty");
        renderPagination(page);
        return;
      }
      el.className = "grid cols-3";
      el.innerHTML = page.items.map(productCard).join("");
      scheduleVariantChipFit();
      renderPagination(page);
    } catch (error) {
      app.setState(el, error.message, "error");
      renderPagination(null);
    }
  }

  function syncProductsUrl(filters) {
    if (!document.querySelector("#product-list")) return;
    const params = new URLSearchParams();
    if (filters.keyword) params.set("keyword", filters.keyword);
    if (filters.categoryId) params.set("categoryId", filters.categoryId);
    if (filters.sort && filters.sort !== "latest") params.set("sort", filters.sort);
    if (currentPage > 1) params.set("page", String(currentPage));
    const nextUrl = "products.html" + (params.toString() ? "?" + params.toString() : "");
    if (location.pathname.endsWith("/products.html") && location.href !== new URL(nextUrl, location.href).href) {
      history.replaceState(null, "", nextUrl);
    }
  }

  function updateCategoryPillState(categoryId) {
    const selectedId = Number(categoryId || 0);
    const selected = categoryCache.find((category) => category.id === selectedId);
    const activeRootId = selected ? selected.rootId : 0;
    document.querySelectorAll(".category-pill").forEach((pill) => {
      const url = new URL(pill.getAttribute("href") || "products.html", location.href);
      const pillCategoryId = Number(url.searchParams.get("categoryId") || 0);
      const active = activeRootId ? pillCategoryId === activeRootId : !pillCategoryId;
      pill.classList.toggle("is-active", active);
      if (active) {
        pill.setAttribute("aria-current", "page");
      } else {
        pill.removeAttribute("aria-current");
      }
    });
  }

  function renderPagination(page) {
    const el = document.querySelector("#product-pagination");
    if (!el) return;
    if (!page || !page.total) {
      el.innerHTML = "";
      return;
    }
    const totalPages = Math.max(1, Math.ceil(page.total / page.size));
    if (totalPages <= 1) {
      el.innerHTML = "";
      return;
    }
    const visiblePageCount = 10;
    let start = Math.max(1, page.page - Math.floor((visiblePageCount - 1) / 2));
    let end = Math.min(totalPages, start + visiblePageCount - 1);
    start = Math.max(1, end - visiblePageCount + 1);
    const buttons = [];
    for (let value = start; value <= end; value++) {
      buttons.push(`<button class="button page-button ${value === page.page ? "is-active" : ""}" type="button" data-page="${value}"${value === page.page ? ' aria-current="page"' : ""}>${value}</button>`);
    }
    el.innerHTML = `
      <div class="pagination-buttons">
        <button class="button" type="button" data-page="${page.page - 1}"${page.page <= 1 ? " disabled" : ""}>이전</button>
        ${buttons.join("")}
        <button class="button" type="button" data-page="${page.page + 1}"${page.hasNext ? "" : " disabled"}>다음</button>
      </div>
    `;
    el.querySelectorAll("[data-page]").forEach((button) => {
      button.addEventListener("click", () => {
        const nextPage = Number(button.dataset.page);
        if (!nextPage || nextPage === currentPage) return;
        currentPage = nextPage;
        renderProducts();
        document.querySelector("#product-list")?.scrollIntoView({ behavior: "smooth", block: "start" });
      });
    });
  }

  document.addEventListener("DOMContentLoaded", async () => {
    await renderCategories("#category-list");
    await renderPopular();
    const keywordInput = document.querySelector("#keyword-filter");
    if (keywordInput) {
      keywordInput.value = app.qs("keyword", "");
      bindRecentSearch(keywordInput, () => {
        currentPage = 1;
        renderProducts();
      });
    }
    const heroKeywordInput = document.querySelector("#hero-keyword");
    if (heroKeywordInput) {
      bindRecentSearch(heroKeywordInput, () => {
        heroKeywordInput.closest("form")?.requestSubmit();
      });
      heroKeywordInput.closest("form")?.addEventListener("submit", () => saveRecentSearch(heroKeywordInput.value));
    }
    const sortSelect = document.querySelector("#sort-filter");
    if (sortSelect) sortSelect.value = app.qs("sort", "latest");
    const searchForm = document.querySelector("#product-search");
    if (searchForm) {
      searchForm.addEventListener("submit", (event) => {
        event.preventDefault();
        saveRecentSearch(keywordInput?.value || "");
        currentPage = 1;
        renderProducts();
      });
    }
    document.querySelector("#category-filter")?.addEventListener("change", (event) => {
      event.currentTarget.dataset.selectedCategoryId = event.currentTarget.value;
      currentPage = 1;
      renderProducts();
    });
    document.querySelector("#sort-filter")?.addEventListener("change", () => {
      currentPage = 1;
      renderProducts();
    });
    if (document.querySelector("#featured-products")) {
      const products = await app.request("/products/popular?size=6");
      document.querySelector("#featured-products").innerHTML = products.map(productCard).join("");
      scheduleVariantChipFit();
    }
    renderProducts();
  });

  window.addEventListener("resize", scheduleVariantChipFit);
})();
