(function () {
  const tokenKey = "civilshop_access_token";
  let cartMigrationPromise = null;
  const brandTitle = "한국콘크리트 산업";
  const brandIcon = "assets/images/brand/hk-favicon.png";

  function rawToken() {
    return localStorage.getItem(tokenKey);
  }

  function decodeToken(value) {
    if (!value) return null;
    try {
      const payload = value.split(".")[1];
      if (!payload) return null;
      const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
      const padded = normalized.padEnd(normalized.length + (4 - normalized.length % 4) % 4, "=");
      return JSON.parse(decodeURIComponent(escape(atob(padded))));
    } catch (error) {
      return null;
    }
  }

  function currentUser() {
    const value = rawToken();
    const payload = decodeToken(value);
    if (!payload) return null;
    if (payload.exp && payload.exp * 1000 < Date.now()) {
      setToken(null);
      return null;
    }
    return {
      id: payload.uid,
      email: payload.sub,
      roles: payload.roles || []
    };
  }

  function token() {
    return currentUser() ? rawToken() : null;
  }

  function setToken(value) {
    if (value) localStorage.setItem(tokenKey, value);
    else localStorage.removeItem(tokenKey);
  }

  function logout() {
    setToken(null);
    location.href = "index.html";
  }

  function hasRole(user, role) {
    return Boolean(user && Array.isArray(user.roles) && user.roles.includes(role));
  }

  function hasAnyRole(user, roles) {
    return Boolean(user && Array.isArray(user.roles) && roles.some((role) => user.roles.includes(role)));
  }

  function normalizedMethod(method) {
    return String(method || "GET").toUpperCase();
  }

  function shouldSkipAuth(path, method) {
    const methodName = normalizedMethod(method);
    return methodName === "POST" && (path.startsWith("/auth/login") || path.startsWith("/auth/signup"));
  }

  function isPublicRetryPath(path, method) {
    const methodName = normalizedMethod(method);
    if (methodName === "GET") {
      return path.startsWith("/products") || path.startsWith("/categories") || path.startsWith("/search");
    }
    return methodName === "POST" && path.startsWith("/consultations");
  }

  function headers(path, method, extra, withoutAuth) {
    const base = {
      "Content-Type": "application/json",
      "X-Session-Id": window.APP_CONFIG.SESSION_ID
    };
    const accessToken = token();
    if (!withoutAuth && !shouldSkipAuth(path, method) && accessToken) {
      base.Authorization = "Bearer " + accessToken;
    }
    const merged = Object.assign(base, extra || {});
    if (withoutAuth || shouldSkipAuth(path, method)) {
      delete merged.Authorization;
    }
    return merged;
  }

  async function request(path, options) {
    return requestWithAuthRetry(path, options || {}, false);
  }

  async function requestWithAuthRetry(path, options, retryingWithoutAuth) {
    const init = Object.assign({ method: "GET" }, options || {});
    init.headers = headers(path, init.method, init.headers, retryingWithoutAuth);
    if (init.body && typeof init.body !== "string") {
      init.body = JSON.stringify(init.body);
    }
    const response = await fetch(window.APP_CONFIG.API_BASE_URL + path, init);
    const text = await response.text();
    let payload = null;
    if (text) {
      try {
        payload = JSON.parse(text);
      } catch (error) {
        payload = { message: text };
      }
    }
    if (!response.ok) {
      const message = payload && payload.message ? payload.message : "요청 처리 중 오류가 발생했습니다.";
      const tokenIssue = response.status === 401 || message.includes("토큰");
      const usedAuthorization = Boolean(init.headers.Authorization);
      if (tokenIssue) {
        setToken(null);
        if (usedAuthorization && !retryingWithoutAuth && isPublicRetryPath(path, init.method)) {
          return requestWithAuthRetry(path, options, true);
        }
      }
      const error = new Error(message);
      error.code = payload && payload.code;
      error.details = payload && payload.details;
      throw error;
    }
    return payload;
  }

  function qs(name, fallback) {
    return new URLSearchParams(location.search).get(name) || fallback || "";
  }

  function money(value) {
    if (value === null || value === undefined) return "견적문의";
    return Number(value).toLocaleString("ko-KR") + "원";
  }

  function unitPrice(value, unit) {
    if (value === null || value === undefined) return "견적문의";
    return (unit ? unit + "당 " : "") + money(value);
  }

  const labelMaps = {
    productStatus: {
      DRAFT: "임시저장",
      ON_SALE: "판매중",
      QUOTE_ONLY: "견적전용",
      SOLD_OUT: "품절",
      HIDDEN: "숨김",
      DISCONTINUED: "판매중단",
      DELETED: "삭제됨"
    },
    quoteStatus: {
      SUBMITTED: "접수완료",
      REVIEWING: "검토중",
      QUOTED: "견적완료",
      NEGOTIATING: "협의중",
      APPROVED: "승인",
      REJECTED: "반려",
      EXPIRED: "만료"
    },
    consultationStatus: {
      NEW: "신규",
      ASSIGNED: "담당자 배정",
      IN_PROGRESS: "진행중",
      DONE: "처리완료",
      CLOSED: "종료"
    },
    consultationType: {
      PHONE: "전화상담",
      SMS: "문자상담",
      KAKAO: "카카오 상담",
      EMAIL: "이메일 상담",
      SITE_QNA: "사이트 문의"
    },
    userStatus: {
      ACTIVE: "정상",
      SUSPENDED: "이용정지",
      WITHDRAWN: "탈퇴",
      DELETED: "삭제됨"
    },
    role: {
      ROLE_ADMIN: "슈퍼 관리자",
      ROLE_OPERATOR: "운영자",
      ROLE_PRODUCT_MANAGER: "상품 관리자",
      ROLE_BUSINESS_MEMBER: "기업 회원",
      ROLE_MEMBER: "일반 회원"
    },
    vatPolicy: {
      VAT_INCLUDED: "부가세 포함",
      VAT_EXCLUDED: "부가세 별도"
    },
    freightPolicy: {
      FREIGHT_INCLUDED: "운임 포함",
      FREIGHT_EXCLUDED: "운임 별도",
      SELECTABLE: "운임 선택"
    },
    vehicleType: {
      ONE_TON: "1톤",
      FIVE_TON: "5톤",
      FIVE_TON_AXIS: "5톤 축차",
      TWENTY_FIVE_TON: "25톤"
    }
  };

  const labelValues = {
    productStatus: ["DRAFT", "ON_SALE", "QUOTE_ONLY", "SOLD_OUT", "HIDDEN"],
    quoteStatus: ["SUBMITTED", "REVIEWING", "QUOTED", "NEGOTIATING", "APPROVED", "REJECTED", "EXPIRED"],
    consultationStatus: ["NEW", "ASSIGNED", "IN_PROGRESS", "DONE", "CLOSED"],
    vehicleType: ["ONE_TON", "FIVE_TON", "FIVE_TON_AXIS", "TWENTY_FIVE_TON"]
  };

  const workflowGroups = {
    quote: {
      NEW: ["SUBMITTED"],
      PROCESSING: ["REVIEWING", "QUOTED", "NEGOTIATING"],
      DONE: ["APPROVED", "REJECTED", "EXPIRED"]
    },
    consultation: {
      NEW: ["NEW"],
      PROCESSING: ["ASSIGNED", "IN_PROGRESS"],
      DONE: ["DONE", "CLOSED"]
    }
  };

  const workflowLabels = {
    NEW: "신규",
    PROCESSING: "처리 중",
    DONE: "처리 완료"
  };

  function label(kind, value) {
    if (value === null || value === undefined || value === "") return "";
    const map = labelMaps[kind] || {};
    return map[value] || "미확인";
  }

  function enumOptions(kind, selected, allLabel) {
    const values = labelValues[kind] || [];
    const options = allLabel ? [`<option value="">${escapeHtml(allLabel)}</option>`] : [];
    values.forEach((value) => {
      options.push(`<option value="${escapeHtml(value)}"${value === selected ? " selected" : ""}>${escapeHtml(label(kind, value))}</option>`);
    });
    return options.join("");
  }

  function roleLabel(role) {
    return label("role", role);
  }

  function rolesLabel(roles) {
    return (roles || []).map(roleLabel).filter(Boolean).join(", ");
  }

  function pricePolicyLabel(price) {
    if (!price) return "";
    return [label("vatPolicy", price.vatPolicy), label("freightPolicy", price.freightPolicy)]
      .filter(Boolean)
      .join(" / ");
  }

  function workflowBucket(kind, value) {
    const groups = workflowGroups[kind] || {};
    return Object.keys(groups).find((key) => groups[key].includes(value)) || "";
  }

  function workflowLabel(kind, value) {
    return workflowLabels[workflowBucket(kind, value)] || label(kind === "quote" ? "quoteStatus" : "consultationStatus", value);
  }

  function escapeHtml(value) {
    return String(value === null || value === undefined ? "" : value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  function html(strings, ...values) {
    return strings.reduce((acc, part, index) => acc + part + (index < values.length ? escapeHtml(values[index]) : ""), "");
  }

  function notify(message, options) {
    const body = String(message || "").trim();
    if (!body) return false;
    window.alert(body);
    return true;
  }

  function initBrandChrome() {
    document.title = brandTitle;
    let icon = document.querySelector('link[rel="icon"]');
    if (!icon) {
      icon = document.createElement("link");
      icon.rel = "icon";
      document.head.appendChild(icon);
    }
    icon.href = brandIcon;
  }

  function initAgreementToggles() {
    if (document.documentElement.dataset.agreementToggleBound) return;
    document.documentElement.dataset.agreementToggleBound = "true";
    document.addEventListener("click", (event) => {
      const button = event.target.closest("[data-agreement-toggle]");
      if (!button) return;
      const detail = document.getElementById(button.dataset.agreementToggle);
      if (!detail) return;
      const open = detail.hidden;
      detail.hidden = !open;
      button.setAttribute("aria-expanded", String(open));
      button.textContent = open ? "접기" : "자세히 보기";
    });
  }

  function setState(target, message, kind) {
    const el = typeof target === "string" ? document.querySelector(target) : target;
    if (!el) return;
    el.className = kind || "notice";
    el.textContent = message;
  }

  function localCartItems() {
    return JSON.parse(localStorage.getItem("civilshop_quote_cart") || "[]");
  }

  function saveCart(items) {
    localStorage.setItem("civilshop_quote_cart", JSON.stringify(items));
  }

  function normalizeCartItem(item) {
    const salePrice = item.unitPriceSnapshot ?? item.salePrice ?? item.unitPrice ?? null;
    return {
      id: item.id || item.cartItemId || null,
      cartItemId: item.id || item.cartItemId || null,
      productId: item.productId,
      productName: item.productName || "",
      productSummary: item.productSummary || "",
      imageUrl: item.productImageUrl || item.imageUrl || "assets/images/placeholder.svg",
      variantId: item.variantId,
      variantName: item.variantName || "",
      quantity: Number(item.quantity || 1),
      salePrice
    };
  }

  function normalizeCartResponse(cart) {
    return ((cart && cart.items) || []).map(normalizeCartItem);
  }

  async function migrateLocalCartToServer() {
    if (!currentUser()) return;
    if (cartMigrationPromise) return cartMigrationPromise;
    cartMigrationPromise = (async () => {
      const localItems = localCartItems().filter((item) => item.productId && item.variantId);
      if (!localItems.length) return;
      const failed = [];
      for (const item of localItems) {
        try {
          await request("/cart/items", {
            method: "POST",
            body: {
              productId: item.productId,
              variantId: item.variantId,
              quantity: Math.max(1, Number(item.quantity || 1))
            }
          });
        } catch (error) {
          failed.push(item);
        }
      }
      saveCart(failed);
    })();
    return cartMigrationPromise;
  }

  async function cartItems() {
    if (currentUser()) {
      await migrateLocalCartToServer();
      return normalizeCartResponse(await request("/cart"));
    }
    return localCartItems();
  }

  async function addCart(item) {
    if (currentUser()) {
      await migrateLocalCartToServer();
      return normalizeCartResponse(await request("/cart/items", {
        method: "POST",
        body: {
          productId: item.productId,
          variantId: item.variantId,
          quantity: Number(item.quantity || 1)
        }
      }));
    }
    const items = localCartItems();
    const existing = items.find((row) => row.variantId === item.variantId);
    if (existing) {
      const quantity = Number(existing.quantity) + Number(item.quantity);
      Object.assign(existing, item, { quantity });
    } else {
      items.push(item);
    }
    saveCart(items);
    return items;
  }

  async function updateCartItem(itemId, quantity) {
    if (currentUser()) {
      return normalizeCartResponse(await request("/cart/items/" + itemId, {
        method: "PATCH",
        body: { quantity: Math.max(1, Number(quantity || 1)) }
      }));
    }
    const items = localCartItems();
    const index = items.findIndex((item) => String(item.id || item.cartItemId || item.variantId) === String(itemId));
    if (index >= 0) {
      items[index].quantity = Math.max(1, Number(quantity || 1));
      saveCart(items);
    }
    return items;
  }

  async function removeCartItem(itemId) {
    if (currentUser()) {
      await request("/cart/items/" + itemId, { method: "DELETE" });
      return cartItems();
    }
    const items = localCartItems().filter((item) => String(item.id || item.cartItemId || item.variantId) !== String(itemId));
    saveCart(items);
    return items;
  }

  async function clearCart() {
    if (currentUser()) {
      const items = await cartItems();
      await Promise.all(items.map((item) => request("/cart/items/" + item.id, { method: "DELETE" })));
      return [];
    }
    saveCart([]);
    return [];
  }

  async function createQuoteFromCart(body) {
    return request("/cart/to-quote", { method: "POST", body });
  }

  function initBrandLogo() {
    document.querySelectorAll(".site-header .brand").forEach((brand) => {
      if (brand.querySelector(".brand-logo")) return;
      brand.innerHTML = `
        <img class="brand-logo" src="assets/images/brand/hk-logo.png" alt="한국콘크리트산업">
      `;
    });
  }

  function initSiteFooter() {
    const path = location.pathname.split("/").pop() || "index.html";
    if (path.startsWith("admin")) return;
    let footer = document.querySelector(".business-footer");
    if (!footer) {
      footer = document.querySelector("body > footer.footer") || document.createElement("footer");
      footer.className = "footer business-footer";
      if (!footer.parentElement) {
        document.body.appendChild(footer);
      }
    }
    footer.innerHTML = `
      <div class="container footer-grid">
        <section>
          <h2>전화번호 안내</h2>
          <p><strong>안성영업소</strong> 031-671-2922</p>
          <p><strong>의정부영업소</strong> 031-855-4425</p>
          <p><strong>화성(매송)영업소</strong> 031-295-2922</p>
          <p><strong>안산영업소</strong> 031-414-7050</p>
          <p><strong>정왕동영업소</strong> 031-434-2922</p>
          <p><strong>팩스</strong> 031-434-2924</p>
        </section>
        <section>
          <h2>사이트 정보</h2>
          <p><strong>회사명</strong> (주)한국콘크리트 산업</p>
          <p><strong>본사사업장</strong> 경기도 안산시 상록구 해양3로 15. 2006호<br>(사동, 그랑시티시그니처타워)</p>
          <p><strong>사업자등록번호</strong> 124-86-94230</p>
          <p><strong>이메일</strong> <a href="mailto:kh4490@naver.com">kh4490@naver.com</a></p>
        </section>
        <section>
          <h2>계좌번호</h2>
          <img class="bank-logo" src="assets/images/brand/hk-bank.png" alt="기업은행">
          <p><strong>기업은행</strong> [예금주 : (주)한국콘크리트산업]</p>
          <p><strong>계좌번호</strong> 345-080236-04-012</p>
        </section>
      </div>
      <div class="container footer-copy">Copyright &copy; 한국콘크리트산업. All Rights Reserved.</div>
    `;
  }

  function initConsultationLauncher() {
    const path = location.pathname.split("/").pop() || "index.html";
    if (path !== "index.html") return;
    if (document.querySelector("#floating-consultation")) return;

    const launcher = document.createElement("div");
    launcher.id = "floating-consultation";
    launcher.className = "floating-consultation";
    launcher.innerHTML = `
      <button class="button primary floating-consultation-button" type="button" id="consultation-open">상담 요청</button>
      <div class="modal-backdrop" id="consultation-modal" hidden>
        <section class="modal-panel consultation-modal-panel" role="dialog" aria-modal="true" aria-labelledby="consultation-modal-title">
          <h2 id="consultation-modal-title">상담 요청</h2>
          <form id="consultation-modal-form" class="grid">
            <label class="field">상담 방식
              <select class="input" name="type">
                <option value="PHONE">전화상담</option>
                <option value="SMS">문자상담</option>
              </select>
            </label>
            <div class="grid cols-2">
              <label class="field">담당자명
                <input class="input" name="contactName" required>
              </label>
              <label class="field">연락처
                <input class="input" name="contactPhone" placeholder="010-0000-0000" required>
              </label>
            </div>
            <label class="field">상담 내용
              <textarea name="message" placeholder="필요한 자재, 수량, 현장 위치를 남겨주세요."></textarea>
            </label>
            <section class="agreement-panel compact" aria-label="개인정보 수집 동의">
              <div class="agreement-list">
                <article class="agreement-item">
                  <div class="agreement-item-head">
                    <label class="agreement-check"><input type="checkbox" name="privacyAgreed" required> <span>개인정보 수집·이용 동의 <small>필수</small></span></label>
                    <button class="agreement-toggle" type="button" aria-expanded="false" aria-controls="consultation-modal-privacy-detail" data-agreement-toggle="consultation-modal-privacy-detail">자세히 보기</button>
                  </div>
                  <div class="agreement-detail" id="consultation-modal-privacy-detail" hidden>
                    <p>상담 접수와 연락을 위해 담당자명, 연락처, 상담 내용을 수집하며 상담 처리 및 이력 확인 목적으로 보관합니다.</p>
                  </div>
                </article>
              </div>
            </section>
            <div class="modal-actions">
              <button class="button" type="button" id="consultation-close">닫기</button>
              <button class="button primary" type="submit">요청 접수</button>
            </div>
            <div id="consultation-modal-state"></div>
          </form>
        </section>
      </div>
    `;
    document.body.appendChild(launcher);

    const modal = launcher.querySelector("#consultation-modal");
    const openButton = launcher.querySelector("#consultation-open");
    const closeButton = launcher.querySelector("#consultation-close");
    const form = launcher.querySelector("#consultation-modal-form");

    function openModal() {
      modal.hidden = false;
      form.contactName.focus();
    }

    function closeModal() {
      modal.hidden = true;
      openButton.focus();
    }

    openButton.addEventListener("click", openModal);
    closeButton.addEventListener("click", closeModal);
    modal.addEventListener("click", (event) => {
      if (event.target === modal) closeModal();
    });
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const data = Object.fromEntries(new FormData(form).entries());
      try {
        await request("/consultations", {
          method: "POST",
          body: {
            type: data.type,
            contactName: data.contactName,
            contactPhone: data.contactPhone,
            message: data.message,
            privacyAgreed: form.privacyAgreed.checked
          }
        });
        notify("상담 요청이 접수되었습니다.");
        setState("#consultation-modal-state", "", "");
        form.reset();
        window.setTimeout(closeModal, 700);
      } catch (error) {
        setState("#consultation-modal-state", error.message, "error");
      }
    });
  }

  const adminNavItems = [
    { href: "admin.html", label: "대시보드", roles: ["ROLE_ADMIN"] },
    { href: "admin-categories.html", label: "카테고리", roles: ["ROLE_ADMIN", "ROLE_PRODUCT_MANAGER"] },
    { href: "admin-products.html", label: "상품", roles: ["ROLE_ADMIN", "ROLE_PRODUCT_MANAGER"] },
    { href: "admin-quotes.html", label: "견적", roles: ["ROLE_ADMIN", "ROLE_OPERATOR"] },
    { href: "admin-consultations.html", label: "상담", roles: ["ROLE_ADMIN", "ROLE_OPERATOR"] },
    { href: "admin-users.html", label: "회원", roles: ["ROLE_ADMIN"] },
    { href: "admin-admins.html", label: "관리자 계정", roles: ["ROLE_ADMIN"] }
  ];

  function adminActiveHref(path) {
    if (path === "admin-product-form.html") return "admin-products.html";
    if (path === "admin-quote-detail.html") return "admin-quotes.html";
    return adminNavItems.some((item) => item.href === path) ? path : "admin.html";
  }

  function allowedAdminItems(user) {
    return adminNavItems.filter((item) => hasAnyRole(user, item.roles));
  }

  function initAdminSidebar() {
    const path = location.pathname.split("/").pop() || "index.html";
    if (!path.startsWith("admin")) return;
    const user = currentUser();
    const activeHref = adminActiveHref(path);
    const items = allowedAdminItems(user);

    document.querySelectorAll(".admin-sidebar nav").forEach((nav) => {
      nav.innerHTML = items.map((item) => {
        const active = item.href === activeHref;
        return `<a href="${item.href}" class="${active ? "is-active" : ""}"${active ? ' aria-current="page"' : ""}>${item.label}</a>`;
      }).join("");
    });
  }

  async function initAdminBadges() {
    const user = currentUser();
    if (!hasAnyRole(user, ["ROLE_ADMIN", "ROLE_OPERATOR"])) return;
    try {
      const [quotes, consultations] = await Promise.all([
        hasAnyRole(user, ["ROLE_ADMIN", "ROLE_OPERATOR"])
          ? request("/admin/quotes?status=SUBMITTED&page=1&size=1")
          : Promise.resolve({ total: 0 }),
        hasAnyRole(user, ["ROLE_ADMIN", "ROLE_OPERATOR"])
          ? request("/admin/consultations?status=NEW&page=1&size=1")
          : Promise.resolve({ total: 0 })
      ]);
      const counts = {
        "admin-quotes.html": Number(quotes?.total || 0),
        "admin-consultations.html": Number(consultations?.total || 0)
      };
      Object.entries(counts).forEach(([href, count]) => {
        document.querySelectorAll(`.admin-sidebar a[href="${href}"]`).forEach((link) => {
          link.querySelector(".admin-nav-count")?.remove();
          if (count > 0) {
            link.insertAdjacentHTML("beforeend", `<span class="admin-nav-count">${count.toLocaleString("ko-KR")}</span>`);
          }
        });
      });
      const totalNew = Object.values(counts).reduce((sum, count) => sum + count, 0);
      document.querySelectorAll('.site-header .brand[href="admin.html"]').forEach((link) => {
        link.classList.remove("has-admin-alert");
      });
      document.querySelectorAll('.site-header .nav a[href="admin.html"]').forEach((link) => {
        link.classList.toggle("has-admin-alert", totalNew > 0);
      });
    } catch (error) {
      // 관리자 배지는 보조 정보라 실패해도 페이지 사용을 막지 않습니다.
    }
  }

  function initAdminAccessGuard() {
    const path = location.pathname.split("/").pop() || "index.html";
    if (!path.startsWith("admin")) return true;
    const user = currentUser();
    if (!user) {
      document.querySelector(".admin-sidebar")?.remove();
      const main = document.querySelector(".admin-main") || document.querySelector("main");
      if (main) {
        main.removeAttribute("id");
        main.innerHTML = `<section class="card"><p class="muted">관리자 로그인이 필요합니다.</p></section>`;
      }
      const redirect = encodeURIComponent(path + location.search);
      location.replace("login.html?redirect=" + redirect);
      return false;
    }
    if (!hasAnyRole(user, ["ROLE_ADMIN", "ROLE_OPERATOR", "ROLE_PRODUCT_MANAGER"])) {
      document.querySelector(".admin-sidebar")?.remove();
      const main = document.querySelector(".admin-main") || document.querySelector("main");
      if (main) {
        main.removeAttribute("id");
        main.innerHTML = `
          <section class="card grid">
            <h1>접근 권한이 없습니다</h1>
            <p class="muted">관리자 계정으로 로그인해야 관리자 콘솔을 사용할 수 있습니다.</p>
            <div class="row">
              <a class="button primary" href="index.html">메인으로 이동</a>
              <button class="button nav-logout" type="button" data-logout>로그아웃</button>
            </div>
          </section>
        `;
      }
      return false;
    }
    const activeHref = adminActiveHref(path);
    const activeItem = adminNavItems.find((item) => item.href === activeHref);
    if (activeItem && !hasAnyRole(user, activeItem.roles)) {
      const fallback = allowedAdminItems(user)[0]?.href || "index.html";
      location.replace(fallback);
      return false;
    }
    return true;
  }

  function initAuthNavigation() {
    const user = currentUser();
    const path = location.pathname.split("/").pop() || "index.html";
    const consoleUser = hasAnyRole(user, ["ROLE_ADMIN", "ROLE_OPERATOR", "ROLE_PRODUCT_MANAGER"]);
    const fixedLinks = [
      { href: "products.html", label: "상품" },
      { href: "cart.html", label: "장바구니" },
      { href: "quote-new.html", label: "견적요청" }
    ];

    function appendLink(nav, item) {
      const link = document.createElement("a");
      link.href = item.href;
      link.textContent = item.label;
      if (path === item.href) {
        link.className = "is-active";
        link.setAttribute("aria-current", "page");
      }
      nav.appendChild(link);
    }

    document.querySelectorAll(".site-header .topbar").forEach((topbar) => {
      let nav = topbar.querySelector(".nav");
      if (!nav) {
        nav = document.createElement("nav");
        nav.className = "nav";
        topbar.appendChild(nav);
      }

      nav.innerHTML = "";
      fixedLinks.forEach((item) => appendLink(nav, item));

      if (!user) {
        appendLink(nav, { href: "login.html", label: "로그인" });
        return;
      }

      appendLink(nav, { href: "mypage.html", label: "마이페이지" });
      if (consoleUser) appendLink(nav, { href: "admin.html", label: "관리자 콘솔" });

      const button = document.createElement("button");
      button.type = "button";
      button.className = "button nav-logout";
      button.dataset.logout = "true";
      button.textContent = "로그아웃";
      nav.appendChild(button);
    });

    document.querySelectorAll("[data-logout]").forEach((button) => {
      button.addEventListener("click", logout);
    });
  }

  function scrollToElement(target, options) {
    const config = options || {};
    const el = typeof target === "string" ? document.querySelector(target) : target;
    if (!el) return;
    requestAnimationFrame(() => {
      const headerHeight = document.querySelector(".site-header")?.getBoundingClientRect().height || 0;
      const offset = Number(config.offset ?? 16);
      const top = el.getBoundingClientRect().top + window.scrollY - headerHeight - offset;
      window.scrollTo({
        top: Math.max(0, top),
        behavior: config.behavior || "smooth"
      });
    });
  }

  function paginationControls(data, options) {
    const config = options || {};
    const totalPages = Math.max(1, Math.ceil(Number(data?.total || 0) / Number(data?.size || config.size || 1)));
    const currentPage = Math.min(totalPages, Math.max(1, Number(data?.page || 1)));
    const visiblePageCount = Number(config.visiblePageCount || 10);
    let start = Math.max(1, currentPage - Math.floor((visiblePageCount - 1) / 2));
    let end = Math.min(totalPages, start + visiblePageCount - 1);
    start = Math.max(1, end - visiblePageCount + 1);
    const attrs = typeof config.pageAttributes === "function"
      ? config.pageAttributes
      : (page) => `data-page="${page}"`;
    const buttons = [];
    for (let page = start; page <= end; page++) {
      buttons.push(`<button class="button page-button ${page === currentPage ? "is-active" : ""}" type="button" ${attrs(page)}${page === currentPage ? ' aria-current="page"' : ""}>${page}</button>`);
    }
    return `
      <div class="pagination-buttons">
        <button class="button" type="button" ${attrs(Math.max(1, currentPage - 1))}${currentPage <= 1 ? " disabled" : ""}>이전</button>
        ${buttons.join("")}
        <button class="button" type="button" ${attrs(Math.min(totalPages, currentPage + 1))}${currentPage >= totalPages ? " disabled" : ""}>다음</button>
      </div>
    `;
  }

  window.app = {
    request,
    token,
    setToken,
    logout,
    currentUser,
    qs,
    money,
    unitPrice,
    label,
    enumOptions,
    roleLabel,
    rolesLabel,
    pricePolicyLabel,
    workflowBucket,
    workflowLabel,
    escapeHtml,
    html,
    notify,
    paginationControls,
    scrollToElement,
    setState,
    cartItems,
    saveCart,
    addCart,
    updateCartItem,
    removeCartItem,
    clearCart,
    createQuoteFromCart
  };

  document.addEventListener("DOMContentLoaded", () => {
    initBrandChrome();
    initAgreementToggles();
    initBrandLogo();
    const canUseCurrentPage = initAdminAccessGuard();
    initAuthNavigation();
    initSiteFooter();
    initConsultationLauncher();
    if (!canUseCurrentPage) return;
    initAdminSidebar();
    initAdminBadges();
  });
})();
