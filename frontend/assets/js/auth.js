(function () {
  const signupFieldLabels = {
    email: "아이디",
    loginId: "아이디",
    password: "비밀번호",
    passwordConfirm: "비밀번호 확인",
    currentPassword: "현재 비밀번호",
    newPassword: "새 비밀번호",
    name: "이름",
    phone: "연락처",
    termsAgreed: "약관 동의",
    privacyAgreed: "개인정보 수집 동의"
  };

  function showAuthMessages(messages) {
    const box = document.querySelector("#auth-state");
    if (!box) return;
    box.className = "error";
    box.innerHTML = `
      <strong>입력값을 확인해주세요.</strong>
      <ul class="error-list">
        ${messages.map((message) => `<li>${app.escapeHtml(message)}</li>`).join("")}
      </ul>
    `;
  }

  function messagesFromError(error) {
    if (error.details && typeof error.details === "object") {
      return Object.entries(error.details).map(([field, message]) => {
        const label = signupFieldLabels[field] || field;
        return `${label}: ${message}`;
      });
    }
    return [error.message || "요청 처리 중 오류가 발생했습니다."];
  }

  function validateSignup(form) {
    const messages = [];
    const loginId = form.email.value.trim();
    const password = form.password.value;
    const passwordConfirm = form.passwordConfirm?.value || "";
    const name = form.name.value.trim();
    const phone = form.phone.value.trim();
    if (!loginId) {
      messages.push("아이디를 입력해주세요.");
    } else if (loginId.length < 4 || loginId.length > 40) {
      messages.push("아이디는 4~40자로 입력해주세요.");
    } else if (!/^[A-Za-z0-9._-]+$/.test(loginId)) {
      messages.push("아이디는 영문, 숫자, 마침표, 밑줄, 하이픈만 사용할 수 있습니다.");
    }
    if (!password) {
      messages.push("비밀번호를 입력해주세요.");
    } else if (password.length < 8) {
      messages.push("비밀번호는 8자 이상이어야 합니다.");
    }
    if (!passwordConfirm) {
      messages.push("비밀번호 확인을 입력해주세요.");
    } else if (password && password !== passwordConfirm) {
      messages.push("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
    }
    if (loginId && (form.dataset.loginIdAvailable !== "true" || form.dataset.loginIdChecked !== loginId)) {
      messages.push("아이디 중복 확인을 완료해주세요.");
    }
    if (!name) {
      messages.push("이름을 입력해주세요.");
    }
    if (phone && !/^[0-9+\-\s()]{8,20}$/.test(phone)) {
      messages.push("연락처는 숫자와 하이픈 중심으로 8~20자 내에서 입력해주세요.");
    }
    if (!form.termsAgreed.checked) {
      messages.push("약관에 동의해주세요.");
    }
    if (!form.privacyAgreed.checked) {
      messages.push("개인정보 수집에 동의해주세요.");
    }
    return messages;
  }

  function messageText(error) {
    return messagesFromError(error).join(" ");
  }

  function roleLabel(role) {
    return app.roleLabel(role);
  }

  function roleBadge(roles) {
    const values = Array.isArray(roles) ? roles : [];
    if (!values.length || (values.length === 1 && values[0] === "ROLE_MEMBER")) return "";
    return `<p class="badge">${values.map((role) => app.escapeHtml(roleLabel(role))).join(", ")}</p>`;
  }

  function validatePhone(phone) {
    return !phone || /^[0-9+\-\s()]{8,20}$/.test(phone);
  }

  function resetLoginIdCheck(form) {
    if (!form) return;
    delete form.dataset.loginIdChecked;
    delete form.dataset.loginIdAvailable;
    const state = document.querySelector("#login-id-state");
    if (state) {
      state.className = "form-hint";
      state.textContent = "아이디를 입력한 뒤 중복 확인을 눌러주세요.";
    }
  }

  async function checkLoginIdAvailability(form) {
    const loginId = form.email.value.trim();
    if (!loginId) {
      app.setState("#login-id-state", "아이디를 입력해주세요.", "error");
      return false;
    }
    if (loginId.length < 4 || loginId.length > 40) {
      app.setState("#login-id-state", "아이디는 4~40자로 입력해주세요.", "error");
      return false;
    }
    if (!/^[A-Za-z0-9._-]+$/.test(loginId)) {
      app.setState("#login-id-state", "아이디는 영문, 숫자, 마침표, 밑줄, 하이픈만 사용할 수 있습니다.", "error");
      return false;
    }
    try {
      app.setState("#login-id-state", "아이디를 확인하는 중입니다.", "notice");
      const response = await app.request("/auth/login-id-available?loginId=" + encodeURIComponent(loginId));
      form.dataset.loginIdChecked = loginId;
      form.dataset.loginIdAvailable = response.available ? "true" : "false";
      app.setState(
        "#login-id-state",
        response.available ? "사용할 수 있는 아이디입니다." : "이미 사용 중인 아이디입니다.",
        response.available ? "notice" : "error"
      );
      return Boolean(response.available);
    } catch (error) {
      app.setState("#login-id-state", messageText(error), "error");
      return false;
    }
  }

  function goHomeAfterSignup() {
    const redirectHome = () => {
      location.href = "index.html";
    };
    app.notify("회원가입이 완료되었습니다.");
    redirectHome();
  }

  function loggedInDestination() {
    const user = app.currentUser();
    const adminUser = user && Array.isArray(user.roles) && user.roles.includes("ROLE_ADMIN");
    return adminUser
      ? { href: "admin.html", label: "관리자 콘솔로 이동" }
      : { href: "mypage.html", label: "마이페이지로 이동" };
  }

  async function initLogin() {
    const form = document.querySelector("#login-form");
    if (!form) return;
    if (app.currentUser()) {
      const destination = loggedInDestination();
      form.innerHTML = `
        <div class="notice">이미 로그인되어 있습니다.</div>
        <div class="row">
          <a class="button primary" href="${destination.href}">${destination.label}</a>
          <button class="button" type="button" data-logout>로그아웃</button>
        </div>
      `;
      form.querySelector("[data-logout]").addEventListener("click", app.logout);
      document.querySelector(".auth-switch")?.remove();
      return;
    }
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      try {
        const data = Object.fromEntries(new FormData(form).entries());
        const response = await app.request("/auth/login", { method: "POST", body: data });
        app.setToken(response.accessToken);
        location.href = app.qs("redirect", "index.html");
      } catch (error) {
        showAuthMessages(messagesFromError(error));
      }
    });
  }

  async function initSignup() {
    const form = document.querySelector("#signup-form");
    if (!form) return;
    if (app.currentUser()) {
      const destination = loggedInDestination();
      form.innerHTML = `
        <div class="notice">이미 로그인되어 있습니다.</div>
        <div class="row">
          <a class="button primary" href="${destination.href}">${destination.label}</a>
          <button class="button" type="button" data-logout>로그아웃</button>
        </div>
      `;
      form.querySelector("[data-logout]").addEventListener("click", app.logout);
      document.querySelector(".auth-switch")?.remove();
      return;
    }
    resetLoginIdCheck(form);
    form.email.addEventListener("input", () => resetLoginIdCheck(form));
    document.querySelector("#login-id-check")?.addEventListener("click", () => checkLoginIdAvailability(form));
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const validationMessages = validateSignup(form);
      if (validationMessages.length) {
        showAuthMessages(validationMessages);
        return;
      }
      try {
        const data = Object.fromEntries(new FormData(form).entries());
        data.privacyAgreed = form.privacyAgreed.checked;
        data.termsAgreed = form.termsAgreed.checked;
        data.marketingAgreed = form.marketingAgreed.checked;
        delete data.passwordConfirm;
        const response = await app.request("/auth/signup", { method: "POST", body: data });
        app.setToken(response.accessToken);
        goHomeAfterSignup();
      } catch (error) {
        showAuthMessages(messagesFromError(error));
      }
    });
  }

  async function initMypage() {
    const box = document.querySelector("#mypage");
    if (!box) return;
    try {
      const me = await app.request("/users/me");
      const roles = me.roles || [];
      box.innerHTML = `
        <div class="account-stack">
          <section class="card account-card">
            <div class="account-card-head">
              <div>
                <span class="eyebrow">내 계정</span>
                <h1><span data-profile-name>${app.escapeHtml(me.name || "")}</span> <span class="muted account-login-id">(${app.escapeHtml(me.email || "")})</span></h1>
              </div>
              <button class="button" type="button" id="account-edit-toggle">정보 변경</button>
            </div>
            <div class="account-meta">
              <p><span>연락처</span><strong data-profile-phone>${app.escapeHtml(me.phone || "미등록")}</strong></p>
              ${roleBadge(roles)}
            </div>
          </section>

          <div id="account-edit-panel" class="account-edit-panel" hidden>
            <form id="profile-form" class="card account-form">
              <div class="account-form-head">
                <h2>내 정보 변경</h2>
              </div>
              <label class="field">이름
                <input class="input" name="name" type="text" autocomplete="name" value="${app.escapeHtml(me.name || "")}">
              </label>
              <label class="field">연락처
                <input class="input" name="phone" type="tel" autocomplete="tel" value="${app.escapeHtml(me.phone || "")}" placeholder="010-0000-0000">
              </label>
              <button class="button primary" type="submit">정보 저장</button>
              <div id="profile-state"></div>
            </form>

            <form id="password-form" class="card account-form">
              <div class="account-form-head">
                <h2>비밀번호 변경</h2>
              </div>
              <label class="field">현재 비밀번호
                <input class="input" name="currentPassword" type="password" autocomplete="current-password">
              </label>
              <label class="field">새 비밀번호
                <input class="input" name="newPassword" type="password" autocomplete="new-password">
              </label>
              <label class="field">새 비밀번호 확인
                <input class="input" name="newPasswordConfirm" type="password" autocomplete="new-password">
              </label>
              <button class="button primary" type="submit">비밀번호 변경</button>
              <div id="password-state"></div>
            </form>
          </div>

        </div>
      `;
      bindMypageForms(box);
      await renderMyQuoteHistory();
      await renderMyConsultationHistory();
    } catch (error) {
      app.setState(box, "로그인이 필요합니다.", "error");
    }
  }

  async function renderMyQuoteHistory() {
    const quoteBody = document.querySelector("#my-quotes");
    if (!quoteBody) return;
    try {
      const quotes = await app.request("/quotes/me");
      const rows = (quotes.items || []).map((quote) => quoteHistoryRows(quote)).join("");
      quoteBody.innerHTML = rows || '<tr><td colspan="4" class="empty">아직 견적 요청 기록이 없습니다.</td></tr>';
      bindQuoteHistoryToggle(quoteBody);
    } catch (error) {
      quoteBody.innerHTML = `<tr><td colspan="4" class="empty">${app.escapeHtml(messageText(error))}</td></tr>`;
    }
  }

  function quoteHistoryRows(quote) {
    const detailId = "quote-detail-" + quote.id;
    return `
      <tr class="quote-history-row" data-quote-toggle="${app.escapeHtml(detailId)}" tabindex="0" role="button" aria-expanded="false" aria-controls="${app.escapeHtml(detailId)}">
        <td><strong>${app.escapeHtml(quote.requestNo)}</strong></td>
        <td>${app.escapeHtml(quote.companyName)}</td>
        <td>${app.escapeHtml(app.workflowLabel("quote", quote.status))}</td>
        <td><span class="quote-history-date-cell">
          <span>${app.escapeHtml(quote.createdAt || "")}</span>
          <span class="quote-history-toggle-label">상세 보기</span>
        </span></td>
      </tr>
      <tr class="quote-history-detail-row" id="${app.escapeHtml(detailId)}" hidden>
        <td colspan="4">${quoteItemsTable(quote.items || [])}</td>
      </tr>
    `;
  }

  function quoteItemsTable(items) {
    if (!items.length) {
      return '<div class="empty quote-history-empty">요청 품목이 없습니다.</div>';
    }
    return `
      <div class="quote-history-detail">
        <table class="table quote-history-items">
          <thead><tr><th>상품</th><th>규격</th><th>수량</th><th>단가</th><th>금액</th></tr></thead>
          <tbody>${items.map(quoteItemRow).join("")}</tbody>
        </table>
      </div>
    `;
  }

  function quoteItemRow(item) {
    const product = item.productDeleted
      ? quoteItemProductLabel(item)
      : `<a class="line-item" href="product-detail.html?id=${app.escapeHtml(item.productId)}">${quoteItemProductContent(item)}</a>`;
    return `
      <tr>
        <td>${product}</td>
        <td>${app.escapeHtml(item.variantName || "-")}</td>
        <td>${app.escapeHtml(item.quantity || "-")}</td>
        <td>${quoteAmountLabel(item.unitPrice)}</td>
        <td class="price">${quoteAmountLabel(item.totalAmount || lineTotalAmount(item))}</td>
      </tr>
    `;
  }

  function quoteItemProductLabel(item) {
    return `
      <div class="line-item is-disabled">
        ${quoteItemProductContent(item)}
        <span class="badge danger quote-history-deleted-badge">삭제된 상품</span>
      </div>
    `;
  }

  function quoteItemProductContent(item) {
    return `
      <img class="line-item-thumb" src="${app.escapeHtml(item.productImageUrl || "assets/images/placeholder.svg")}" alt="${app.escapeHtml(item.productName || "상품 이미지")}">
      <span class="line-item-copy">
        <strong>${app.escapeHtml(item.productName || "상품명 없음")}</strong>
        <span>${app.escapeHtml(item.productSummary || "")}</span>
      </span>
    `;
  }

  function lineTotalAmount(item) {
    const unitPrice = Number(item.unitPrice || 0);
    const quantity = Number(item.quantity || 0);
    if (!unitPrice || !quantity) return null;
    return unitPrice * quantity;
  }

  function quoteAmountLabel(value) {
    if (value === null || value === undefined || value === "") return "견적문의";
    return app.escapeHtml(app.money(value));
  }

  function bindQuoteHistoryToggle(quoteBody) {
    if (quoteBody.dataset.quoteToggleBound) return;
    quoteBody.dataset.quoteToggleBound = "true";
    quoteBody.addEventListener("click", (event) => {
      const row = event.target.closest("[data-quote-toggle]");
      if (!row || !quoteBody.contains(row)) return;
      toggleQuoteHistoryRow(row);
    });
    quoteBody.addEventListener("keydown", (event) => {
      if (!["Enter", " "].includes(event.key)) return;
      const row = event.target.closest("[data-quote-toggle]");
      if (!row || !quoteBody.contains(row)) return;
      event.preventDefault();
      toggleQuoteHistoryRow(row);
    });
  }

  function toggleQuoteHistoryRow(row) {
    const detail = document.getElementById(row.dataset.quoteToggle);
    if (!detail) return;
    const open = detail.hidden;
    detail.hidden = !open;
    row.setAttribute("aria-expanded", String(open));
    row.classList.toggle("is-open", open);
    const label = row.querySelector(".quote-history-toggle-label");
    if (label) label.textContent = open ? "닫기" : "상세 보기";
  }

  async function renderMyConsultationHistory() {
    const consultationBody = document.querySelector("#my-consultations");
    if (!consultationBody) return;
    try {
      const consultations = await app.request("/consultations/me");
      const rows = (consultations.items || []).map((consultation) => app.html`
        <tr>
          <td>${consultation.id}</td>
          <td>${app.label("consultationType", consultation.type)}</td>
          <td>${consultationProductName(consultation)}</td>
          <td>${app.workflowLabel("consultation", consultation.status)}</td>
          <td>${consultation.createdAt || ""}</td>
        </tr>
      `).join("");
      consultationBody.innerHTML = rows || '<tr><td colspan="5" class="empty">아직 상담 요청 기록이 없습니다.</td></tr>';
    } catch (error) {
      consultationBody.innerHTML = `<tr><td colspan="5" class="empty">${app.escapeHtml(messageText(error))}</td></tr>`;
    }
  }

  function consultationProductName(consultation) {
    return [consultation.productName, consultation.variantName].filter(Boolean).join(" / ") || "일반 상담";
  }

  function bindMypageForms(box) {
    const editToggle = box.querySelector("#account-edit-toggle");
    const editPanel = box.querySelector("#account-edit-panel");
    editToggle?.addEventListener("click", () => {
      editPanel.hidden = !editPanel.hidden;
      editToggle.textContent = editPanel.hidden ? "정보 변경" : "변경 닫기";
    });

    const profileForm = box.querySelector("#profile-form");
    profileForm?.addEventListener("submit", async (event) => {
      event.preventDefault();
      const phone = profileForm.phone.value.trim();
      if (!profileForm.name.value.trim()) {
        app.setState("#profile-state", "이름을 입력해주세요.", "error");
        return;
      }
      if (!validatePhone(phone)) {
        app.setState("#profile-state", "연락처는 숫자와 하이픈 중심으로 8~20자 내에서 입력해주세요.", "error");
        return;
      }
      try {
        const updated = await app.request("/users/me", {
          method: "PATCH",
          body: { name: profileForm.name.value.trim(), phone }
        });
        box.querySelector("[data-profile-name]").textContent = updated.name || "";
        box.querySelector("[data-profile-phone]").textContent = updated.phone || "미등록";
        app.setState("#profile-state", "내 정보가 저장되었습니다.", "notice");
      } catch (error) {
        app.setState("#profile-state", messageText(error), "error");
      }
    });

    const passwordForm = box.querySelector("#password-form");
    passwordForm?.addEventListener("submit", async (event) => {
      event.preventDefault();
      if (passwordForm.newPassword.value.length < 8) {
        app.setState("#password-state", "새 비밀번호는 8자 이상이어야 합니다.", "error");
        return;
      }
      if (passwordForm.newPassword.value !== passwordForm.newPasswordConfirm.value) {
        app.setState("#password-state", "새 비밀번호 확인이 일치하지 않습니다.", "error");
        return;
      }
      try {
        await app.request("/users/me/password", {
          method: "PATCH",
          body: {
            currentPassword: passwordForm.currentPassword.value,
            newPassword: passwordForm.newPassword.value
          }
        });
        passwordForm.reset();
        app.setState("#password-state", "비밀번호가 변경되었습니다.", "notice");
      } catch (error) {
        app.setState("#password-state", messageText(error), "error");
      }
    });

  }

  document.addEventListener("DOMContentLoaded", () => {
    initLogin();
    initSignup();
    initMypage();
  });
})();
