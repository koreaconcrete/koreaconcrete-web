(function () {
  async function dashboard() {
    const root = document.querySelector("#admin-dashboard");
    if (!root) return;
    try {
      const data = await app.request("/admin/dashboard");
      root.innerHTML = `
        <div class="grid cols-3">
          <div class="card metric"><span class="muted">오늘 견적</span><strong>${data.todayQuoteCount}</strong></div>
          <div class="card metric"><span class="muted">오늘 상담</span><strong>${data.todayConsultationCount}</strong></div>
          <div class="card metric"><span class="muted">전체 상품</span><strong>${data.totalProductCount}</strong></div>
        </div>
        <div class="grid cols-2 section">
          <div class="card"><h2>최근 견적</h2><table class="table"><tbody>${data.recentQuotes.map((q) => app.html`<tr><td>${q.requestNo}</td><td>${q.companyName}</td><td>${app.label("quoteStatus", q.status)}</td></tr>`).join("")}</tbody></table></div>
          <div class="card"><h2>최근 상담</h2><table class="table"><tbody>${data.recentConsultations.map((c) => app.html`<tr><td>${c.contactName}</td><td>${app.label("consultationStatus", c.status)}</td><td>${c.createdAt || ""}</td></tr>`).join("")}</tbody></table></div>
        </div>
      `;
    } catch (error) {
      app.setState(root, error.message, "error");
    }
  }

  async function categories() {
    const root = document.querySelector("#admin-categories");
    if (!root) return;
    const form = root.querySelector("#admin-category-form");
    const parentSelect = form.querySelector('[name="parentId"]');
    const activeSelect = form.querySelector('[name="active"]');
    const categoryNameInput = form.querySelector('[name="name"]');
    const imageUrlInput = form.querySelector('[name="imageUrl"]');
    const imagePreview = root.querySelector("#admin-category-image-preview");
    const imageUploadRow = root.querySelector(".admin-upload-row");
    const submitButton = root.querySelector("#admin-category-submit");
    const cancelButton = root.querySelector("#admin-category-cancel");
    const imageInput = root.querySelector("#admin-category-image-upload");
    const imageRemoveButton = root.querySelector("#admin-category-image-remove-button");
    let rows = [];
    let editingCategoryId = null;

    function flatten(nodes) {
      const nextRows = [];
      function walk(items) {
        items.forEach((node) => {
          nextRows.push(node);
          walk(node.children || []);
        });
      }
      walk(nodes);
      return nextRows;
    }

    function isSelfOrDescendant(category, categoryId) {
      if (!categoryId) return false;
      if (category.id === categoryId) return true;
      let parentId = category.parentId;
      while (parentId) {
        if (parentId === categoryId) return true;
        const parent = rows.find((item) => item.id === parentId);
        parentId = parent?.parentId;
      }
      return false;
    }

    function renderParentSelect() {
      if (!parentSelect) return;
      const currentValue = parentSelect.value;
      parentSelect.innerHTML = `<option value="">최상위 카테고리로 생성</option>` + rows
        .filter((category) => category.depth === 1 && !isSelfOrDescendant(category, editingCategoryId))
        .map((c) => app.html`<option value="${c.id}">${c.name} 아래 세부 카테고리로 생성</option>`)
        .join("");
      parentSelect.value = Array.from(parentSelect.options).some((option) => option.value === currentValue) ? currentValue : "";
    }

    function siblingRows(parentId) {
      return rows.filter((category) => String(category.parentId || "") === String(parentId || ""));
    }

    function nextSortOrder(parentId) {
      const sortOrders = siblingRows(parentId).map((category) => Number(category.sortOrder || 0));
      return sortOrders.length ? Math.max(...sortOrders) + 10 : 10;
    }

    function fillForm(category) {
      editingCategoryId = category.id;
      parentSelect.value = category.parentId || "";
      activeSelect.value = String(category.active !== false);
      categoryNameInput.value = category.name || "";
      imageUrlInput.value = category.parentId ? "" : category.imageUrl || "";
      submitButton.textContent = "수정 저장";
      cancelButton.hidden = false;
      renderParentSelect();
      updateImageControls();
      renderImagePreview();
      form.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    function resetForm() {
      editingCategoryId = null;
      form.reset();
      activeSelect.value = "true";
      imageUrlInput.value = "";
      if (imageInput) imageInput.value = "";
      submitButton.textContent = "생성";
      cancelButton.hidden = true;
      renderParentSelect();
      updateImageControls();
      renderImagePreview();
    }

    function categoryFormBody() {
      const body = Object.fromEntries(new FormData(form).entries());
      body.parentId = body.parentId ? Number(body.parentId) : null;
      const current = editingCategoryId ? rows.find((category) => category.id === editingCategoryId) : null;
      const parentChanged = current && String(current.parentId || "") !== String(body.parentId || "");
      body.sortOrder = current && !parentChanged
        ? Number(current.sortOrder || 0)
        : nextSortOrder(body.parentId);
      body.imageUrl = body.parentId ? null : body.imageUrl?.trim() || null;
      body.active = body.active === "true";
      return body;
    }

    function categoryOrderBody(category, sortOrder) {
      return {
        parentId: category.parentId || null,
        name: category.name,
        slug: category.slug,
        imageUrl: category.imageUrl || null,
        sortOrder,
        active: category.active !== false
      };
    }

    function renderImagePreview() {
      if (!imagePreview) return;
      updateImageControls();
      if (parentSelect.value) {
        imagePreview.innerHTML = "";
        return;
      }
      const imageUrl = imageUrlInput.value.trim();
      if (imageRemoveButton) {
        imageRemoveButton.disabled = !imageUrl;
      }
      imagePreview.innerHTML = imageUrl
        ? `<img src="${app.escapeHtml(imageUrl)}" alt="대표 이미지 미리보기"><span class="muted">대표 이미지가 설정되어 있습니다.</span>`
        : `<span class="muted">대표 이미지가 등록되지 않았습니다.</span>`;
    }

    function updateImageControls() {
      const disabled = Boolean(parentSelect.value);
      if (imagePreview) imagePreview.hidden = disabled;
      if (imageUploadRow) imageUploadRow.hidden = disabled;
      imageUrlInput.disabled = disabled;
      if (imageInput) imageInput.disabled = disabled;
      root.querySelector("#admin-category-image-upload-button")?.toggleAttribute("disabled", disabled);
      if (disabled) {
        imageUrlInput.value = "";
        if (imageInput) imageInput.value = "";
      }
    }

    const render = async () => {
      const data = await app.request("/admin/categories/tree");
      rows = flatten(data);
      renderParentSelect();
      root.querySelector("tbody").innerHTML = rows.length ? rows.map((c) => `
        <tr>
          <td>${app.escapeHtml("· ".repeat(Math.max(c.depth - 1, 0)) + c.name)}</td>
          <td>${categoryImageCell(c)}</td>
          <td>${app.escapeHtml(c.active ? "노출" : "숨김")}</td>
          <td>
            <div class="row admin-order-actions">
              <button class="button" type="button" data-category-move="${app.escapeHtml(c.id)}" data-direction="up">위</button>
              <button class="button" type="button" data-category-move="${app.escapeHtml(c.id)}" data-direction="down">아래</button>
            </div>
          </td>
          <td>
            <div class="row admin-row-actions">
              <button class="button" type="button" data-category-edit="${app.escapeHtml(c.id)}">수정</button>
              <button class="button danger" type="button" data-category-delete="${app.escapeHtml(c.id)}">삭제</button>
            </div>
          </td>
        </tr>
      `).join("") : `<tr><td colspan="5" class="empty">등록된 카테고리가 없습니다.</td></tr>`;
    };

    function categoryImageCell(category) {
      if (category.parentId || Number(category.depth || 0) > 1) {
        return '<span class="muted">-</span>';
      }
      return category.imageUrl
        ? `<img class="admin-category-thumb" src="${app.escapeHtml(category.imageUrl)}" alt="${app.escapeHtml(category.name)}">`
        : '<span class="muted">미등록</span>';
    }
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const body = categoryFormBody();
      try {
        await app.request(editingCategoryId ? "/admin/categories/" + editingCategoryId : "/admin/categories", {
          method: editingCategoryId ? "PATCH" : "POST",
          body
        });
        app.notify(editingCategoryId ? "카테고리가 수정되었습니다." : "카테고리가 생성되었습니다.");
        resetForm();
        await render();
      } catch (error) {
        app.setState("#admin-category-state", error.message, "error");
      }
    });

    cancelButton.addEventListener("click", () => {
      resetForm();
      app.setState("#admin-category-state", "새 카테고리 생성 모드로 전환되었습니다.", "notice");
    });
    root.querySelector("tbody").addEventListener("click", async (event) => {
      const editButton = event.target.closest("button[data-category-edit]");
      if (editButton) {
        const category = rows.find((item) => item.id === Number(editButton.dataset.categoryEdit));
        if (category) fillForm(category);
        return;
      }
      const deleteButton = event.target.closest("button[data-category-delete]");
      if (deleteButton) {
        await deleteCategory(Number(deleteButton.dataset.categoryDelete));
        return;
      }
      const moveButton = event.target.closest("button[data-category-move]");
      if (!moveButton) return;
      try {
        await moveCategory(Number(moveButton.dataset.categoryMove), moveButton.dataset.direction);
      } catch (error) {
        app.setState("#admin-category-state", error.message, "error");
      }
    });
    root.querySelector("#admin-category-image-upload-button")?.addEventListener("click", uploadCategoryImage);
    imageRemoveButton?.addEventListener("click", removeCategoryImage);
    parentSelect?.addEventListener("change", renderImagePreview);

    async function deleteCategory(categoryId) {
      const category = rows.find((item) => item.id === categoryId);
      if (!category) return;
      if (rows.some((item) => item.parentId === categoryId)) {
        app.setState("#admin-category-state", "하위 카테고리가 있는 카테고리는 삭제할 수 없습니다.", "error");
        return;
      }
      if (!window.confirm(`"${category.name}" 카테고리를 삭제할까요?`)) return;
      try {
        await app.request("/admin/categories/" + categoryId, { method: "DELETE" });
        if (editingCategoryId === categoryId) resetForm();
        app.notify("카테고리가 삭제되었습니다.");
        await render();
      } catch (error) {
        app.setState("#admin-category-state", error.message, "error");
      }
    }

    async function uploadCategoryImage() {
      const input = imageInput;
      const file = input?.files?.[0];
      if (!file) {
        app.setState("#admin-category-state", "업로드할 이미지 파일을 선택해주세요.", "error");
        return;
      }
      try {
        app.setState("#admin-category-state", "대표 이미지를 업로드하고 있습니다.", "notice");
        const formData = new FormData();
        formData.append("file", file);
        const response = await fetch(window.APP_CONFIG.API_BASE_URL + "/admin/categories/images", {
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
        imageUrlInput.value = payload.url;
        input.value = "";
        renderImagePreview();
        if (editingCategoryId) {
          await app.request("/admin/categories/" + editingCategoryId, {
            method: "PATCH",
            body: categoryFormBody()
          });
          await render();
          app.setState("#admin-category-state", "대표 이미지가 업로드되어 카테고리에 바로 저장되었습니다.", "notice");
          return;
        }
        app.setState("#admin-category-state", "대표 이미지가 업로드되었습니다. 카테고리 생성을 누르면 함께 저장됩니다.", "notice");
      } catch (error) {
        app.setState("#admin-category-state", error.message, "error");
      }
    }

    async function removeCategoryImage() {
      const previousUrl = imageUrlInput.value.trim();
      if (!previousUrl) {
        app.setState("#admin-category-state", "삭제할 대표 이미지가 없습니다.", "error");
        return;
      }
      if (!window.confirm("이 카테고리의 대표 이미지를 삭제할까요?")) return;
      try {
        imageUrlInput.value = "";
        if (imageInput) imageInput.value = "";
        renderImagePreview();
        if (editingCategoryId) {
          await app.request("/admin/categories/" + editingCategoryId, {
            method: "PATCH",
            body: categoryFormBody()
          });
          await render();
          app.setState("#admin-category-state", "대표 이미지가 삭제되었습니다.", "notice");
          return;
        }
        app.setState("#admin-category-state", "대표 이미지가 삭제되었습니다. 카테고리 생성 시 이미지 없이 저장됩니다.", "notice");
      } catch (error) {
        imageUrlInput.value = previousUrl;
        renderImagePreview();
        app.setState("#admin-category-state", error.message, "error");
      }
    }

    async function moveCategory(categoryId, direction) {
      const category = rows.find((item) => item.id === categoryId);
      if (!category) return;
      const siblings = siblingRows(category.parentId);
      const index = siblings.findIndex((item) => item.id === categoryId);
      const targetIndex = direction === "up" ? index - 1 : index + 1;
      if (index < 0 || targetIndex < 0 || targetIndex >= siblings.length) return;
      const reordered = siblings.slice();
      const [moved] = reordered.splice(index, 1);
      reordered.splice(targetIndex, 0, moved);
      await Promise.all(reordered.map((item, itemIndex) => app.request("/admin/categories/" + item.id, {
        method: "PATCH",
        body: categoryOrderBody(item, (itemIndex + 1) * 10)
      })));
      app.setState("#admin-category-state", "카테고리 순서가 변경되었습니다.", "notice");
      await render();
    }

    render();
  }

  function uploadHeaders() {
    const headers = { "X-Session-Id": window.APP_CONFIG.SESSION_ID };
    if (app.token()) headers.Authorization = "Bearer " + app.token();
    return headers;
  }

  const adminUserPageSize = 20;
  let memberUserPage = 1;
  let adminAccountPage = 1;

  async function users() {
    const root = document.querySelector("#admin-users");
    if (!root) return;
    try {
      const data = await app.request(`/admin/users?accountType=MEMBER&page=${memberUserPage}&size=${adminUserPageSize}`);
      const memberRows = data.items || [];
      root.innerHTML = `<table class="table"><thead><tr><th>번호</th><th>아이디</th><th>이름</th><th>연락처</th><th>상태</th></tr></thead><tbody>${memberRows.length ? memberRows.map((u) => app.html`<tr><td>${u.id}</td><td>${u.email}</td><td>${u.name}</td><td>${u.phone || ""}</td><td>${app.label("userStatus", u.status)}</td></tr>`).join("") : '<tr><td colspan="5" class="empty">등록된 일반 회원이 없습니다.</td></tr>'}</tbody></table><div class="pagination compact" id="member-user-pagination">${paginationButtons(data, "member-user-page")}</div>`;
      root.querySelectorAll("[data-member-user-page]").forEach((button) => {
        button.addEventListener("click", async () => {
          const nextPage = Number(button.dataset.memberUserPage);
          if (!nextPage || nextPage === memberUserPage) return;
          memberUserPage = nextPage;
          button.blur();
          await users();
          app.scrollToElement("#admin-users");
        });
      });
    } catch (error) {
      app.setState(root, error.message, "error");
    }
  }

  async function adminAccounts() {
    const root = document.querySelector("#admin-admins");
    if (!root) return;
    const form = root.querySelector("#admin-account-form");
    const list = root.querySelector("#admin-account-list");

    async function renderList() {
      try {
        const data = await app.request(`/admin/users?accountType=ADMIN&page=${adminAccountPage}&size=${adminUserPageSize}`);
        const currentUser = app.currentUser();
        const rows = data.items || [];
        list.innerHTML = `<table class="table"><thead><tr><th>번호</th><th>아이디</th><th>이름</th><th>연락처</th><th>권한</th><th>상태</th><th></th></tr></thead><tbody>${rows.length ? rows.map((u) => `
          <tr>
            <td>${app.escapeHtml(u.id)}</td>
            <td>${app.escapeHtml(u.email)}</td>
            <td>${app.escapeHtml(u.name)}</td>
            <td>${app.escapeHtml(u.phone || "")}</td>
            <td>${app.escapeHtml(app.rolesLabel(u.roles))}</td>
            <td>${app.escapeHtml(app.label("userStatus", u.status))}</td>
            <td>${currentUser && Number(currentUser.id) === Number(u.id)
              ? '<span class="muted">현재 계정</span>'
              : `<button class="button danger" type="button" data-admin-delete="${app.escapeHtml(u.id)}" data-admin-name="${app.escapeHtml(u.name)}">삭제</button>`}</td>
          </tr>
        `).join("") : '<tr><td colspan="7" class="empty">등록된 관리자 계정이 없습니다.</td></tr>'}</tbody></table><div class="pagination compact" id="admin-account-pagination">${paginationButtons(data, "admin-account-page")}</div>`;
        list.querySelectorAll("[data-admin-account-page]").forEach((button) => {
          button.addEventListener("click", async () => {
            const nextPage = Number(button.dataset.adminAccountPage);
            if (!nextPage || nextPage === adminAccountPage) return;
            adminAccountPage = nextPage;
            button.blur();
            await renderList();
            app.scrollToElement("#admin-account-list");
          });
        });
        list.querySelectorAll("[data-admin-delete]").forEach((button) => {
          button.addEventListener("click", async () => {
            if (!window.confirm(`"${button.dataset.adminName || "선택한 관리자"}" 관리자 계정을 삭제할까요?`)) return;
            try {
              await app.request("/admin/users/admins/" + button.dataset.adminDelete, { method: "DELETE" });
              app.setState("#admin-account-state", "관리자 계정이 삭제되었습니다.", "notice");
              renderList();
            } catch (error) {
              app.setState("#admin-account-state", formatError(error), "error");
            }
          });
        });
      } catch (error) {
        app.setState(list, error.message, "error");
      }
    }

    if (form && !form.dataset.bound) {
      form.dataset.bound = "true";
      form.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (form.password.value.length < 8) {
          app.setState("#admin-account-state", "비밀번호는 8자 이상이어야 합니다.", "error");
          return;
        }
        if (form.password.value !== form.passwordConfirm.value) {
          app.setState("#admin-account-state", "비밀번호 확인이 일치하지 않습니다.", "error");
          return;
        }
        try {
          const created = await app.request("/admin/users/admins", {
            method: "POST",
            body: {
              loginId: form.loginId.value.trim(),
              name: form.name.value.trim(),
              phone: form.phone.value.trim(),
              password: form.password.value,
              role: form.role.value
            }
          });
          form.reset();
          app.setState("#admin-account-state", `${created.name} 계정이 생성되었습니다.`, "notice");
          adminAccountPage = 1;
          renderList();
        } catch (error) {
          app.setState("#admin-account-state", formatError(error), "error");
        }
      });
    }

    renderList();
  }

  function isConsoleAccount(user) {
    const roles = user.roles || [];
    return roles.includes("ROLE_ADMIN") || roles.includes("ROLE_OPERATOR") || roles.includes("ROLE_PRODUCT_MANAGER");
  }

  function paginationButtons(data, datasetName) {
    const datasetAttr = `data-${datasetName}`;
    return app.paginationControls(data, {
      size: adminUserPageSize,
      pageAttributes: (page) => `${datasetAttr}="${page}"`
    });
  }

  function formatError(error) {
    if (error.details && typeof error.details === "object") {
      return Object.values(error.details).join(" ");
    }
    return error.message || "요청 처리 중 오류가 발생했습니다.";
  }

  document.addEventListener("DOMContentLoaded", () => {
    dashboard();
    categories();
    users();
    adminAccounts();
  });
})();
