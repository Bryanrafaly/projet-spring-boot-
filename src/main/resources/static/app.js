const ROLE_PERMISSIONS = {
    ADMIN: {
        tabs: ["dashboard", "users", "citizens", "requests", "verify"],
        usersManage: true,
        citizensCreateUpdate: true,
        citizensArchive: true,
        requestsCreate: true,
        requestsStatusUpdate: true,
        dashboardAccess: true
    },
    AGENT_REGISTRATION: {
        tabs: ["citizens", "requests", "verify"],
        usersManage: false,
        citizensCreateUpdate: true,
        citizensArchive: false,
        requestsCreate: true,
        requestsStatusUpdate: false,
        dashboardAccess: false
    },
    AGENT_VALIDATION: {
        tabs: ["citizens", "requests", "verify"],
        usersManage: false,
        citizensCreateUpdate: false,
        citizensArchive: false,
        requestsCreate: false,
        requestsStatusUpdate: true,
        dashboardAccess: false
    },
    SUPERVISOR: {
        tabs: ["dashboard", "citizens", "requests", "verify"],
        usersManage: false,
        citizensCreateUpdate: false,
        citizensArchive: false,
        requestsCreate: false,
        requestsStatusUpdate: true,
        dashboardAccess: true
    }
};

const state = {
    token: localStorage.getItem("cni_jwt") || "",
    currentUser: null,
    permissions: null,
    users: [],
    citizens: [],
    requests: [],
    userEditId: null,
    citizenEditId: null
};

const sessionState = document.getElementById("session-state");
const sessionPreview = document.getElementById("session-preview");
const logOutput = document.getElementById("log-output");
const logoutBtn = document.getElementById("logout-btn");

function redirectToLogin() {
    window.location.href = "/login.html";
}

function appendLog(title, payload, isError = false) {
    const timestamp = new Date().toLocaleTimeString("fr-FR");
    const body = typeof payload === "string" ? payload : JSON.stringify(payload, null, 2);
    const line = `[${timestamp}] ${isError ? "ERROR" : "INFO"} - ${title}\n${body}\n\n`;
    logOutput.textContent = line + logOutput.textContent;
}

function clearSession() {
    localStorage.removeItem("cni_jwt");
    state.token = "";
    state.currentUser = null;
    state.permissions = null;
}

function renderSession() {
    if (state.currentUser) {
        sessionState.textContent = `Connecte (${state.currentUser.role})`;
        sessionPreview.textContent = `${state.currentUser.email} - ${state.token.slice(0, 20)}...`;
    } else {
        sessionState.textContent = "Connecte";
        sessionPreview.textContent = `${state.token.slice(0, 28)}...`;
    }
}

function safeText(value) {
    if (value === null || value === undefined || value === "") {
        return "-";
    }
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

async function requestApi(url, { method = "GET", body = null } = {}) {
    const headers = {
        Authorization: `Bearer ${state.token}`
    };

    if (body !== null) {
        headers["Content-Type"] = "application/json";
    }

    const response = await fetch(url, {
        method,
        headers,
        body: body === null ? undefined : JSON.stringify(body)
    });

    const text = await response.text();
    let payload = text;
    try {
        payload = text ? JSON.parse(text) : {};
    } catch {
        payload = text;
    }

    if (!response.ok) {
        const error = new Error((payload && payload.message) || response.statusText || "API error");
        error.status = response.status;
        error.payload = payload;
        throw error;
    }

    return payload;
}

function getAllowedTabs() {
    return state.permissions ? state.permissions.tabs : [];
}

function setActiveTab(name) {
    const allowedTabs = getAllowedTabs();
    const tabToShow = allowedTabs.includes(name) ? name : allowedTabs[0] || "verify";

    document.querySelectorAll(".nav-link").forEach((button) => {
        const isAllowed = allowedTabs.includes(button.dataset.tab);
        button.classList.toggle("hidden", !isAllowed);
        button.classList.toggle("active", button.dataset.tab === tabToShow && isAllowed);
    });

    document.querySelectorAll(".tab-panel").forEach((panel) => {
        panel.classList.toggle("active", panel.id === `tab-${tabToShow}`);
        panel.classList.toggle("hidden", panel.id !== `tab-${tabToShow}`);
    });
}

function applyRolePermissions() {
    const permissions = state.permissions;
    if (!permissions) {
        return;
    }

    document.getElementById("tab-users").classList.toggle("hidden", !permissions.usersManage);
    document.getElementById("tab-dashboard").classList.toggle("hidden", !permissions.dashboardAccess);

    document.getElementById("user-form").closest("article").classList.toggle("hidden", !permissions.usersManage);
    document.getElementById("citizen-form").closest("article").classList.toggle("hidden", !permissions.citizensCreateUpdate);
    document.getElementById("request-form").closest("article").classList.toggle("hidden", !permissions.requestsCreate);

    setActiveTab("dashboard");
}

function renderDashboard(data) {
    const cards = document.getElementById("dashboard-cards");
    const cardData = [
        ["Total citoyens", data.totalCitizens],
        ["Cartes delivrees", data.totalCardsDelivered],
        ["Demandes en attente", data.pendingRequests],
        ["Demandes validees", data.validatedRequests],
        ["Utilisateurs actifs", data.activeUsers],
        ["Dossiers en retard", data.overdueRequests]
    ];

    cards.innerHTML = cardData.map(([label, value]) => `
        <article class="stat-card">
            <p>${safeText(label)}</p>
            <strong>${safeText(value)}</strong>
        </article>
    `).join("");

    renderBarMap("chart-status", data.requestsByStatus || {});
    renderBarMap("chart-monthly", data.monthlyRequests || {}, true);
    renderBarMap("chart-region", data.citizensByRegion || {});
}

function renderBarMap(containerId, map, sortKeys = false) {
    const container = document.getElementById(containerId);
    const entries = Object.entries(map || {});
    if (!entries.length) {
        container.innerHTML = '<p class="muted">Aucune donnee disponible.</p>';
        return;
    }

    const processed = sortKeys ? [...entries].sort(([a], [b]) => a.localeCompare(b)) : entries;
    const max = Math.max(...processed.map(([, v]) => Number(v) || 0), 1);

    container.innerHTML = processed.map(([key, value]) => {
        const numeric = Number(value) || 0;
        const width = Math.max(6, Math.round((numeric / max) * 100));
        return `
            <div class="bar-row">
                <span class="bar-label">${safeText(key)}</span>
                <div class="bar-track"><div class="bar-fill" style="width:${width}%"></div></div>
                <span class="bar-value">${safeText(numeric)}</span>
            </div>
        `;
    }).join("");
}

function resetUserForm() {
    state.userEditId = null;
    const form = document.getElementById("user-form");
    form.reset();
    form.elements.active.checked = true;
    document.getElementById("user-form-title").textContent = "Creer un utilisateur";
}

function fillUserForm(user) {
    if (!state.permissions?.usersManage) {
        return;
    }
    state.userEditId = user.id;
    const form = document.getElementById("user-form");
    form.elements.id.value = user.id;
    form.elements.firstName.value = user.firstName || "";
    form.elements.lastName.value = user.lastName || "";
    form.elements.email.value = user.email || "";
    form.elements.password.value = "";
    form.elements.role.value = user.role || "AGENT_REGISTRATION";
    form.elements.active.checked = !!user.active;
    document.getElementById("user-form-title").textContent = `Modifier utilisateur #${user.id}`;
}

function renderUsers() {
    const body = document.getElementById("users-body");
    if (!state.permissions?.usersManage) {
        body.innerHTML = "";
        return;
    }

    body.innerHTML = state.users.map((user) => `
        <tr>
            <td>${safeText(user.id)}</td>
            <td>${safeText(`${user.firstName} ${user.lastName}`)}</td>
            <td>${safeText(user.email)}</td>
            <td><span class="badge">${safeText(user.role)}</span></td>
            <td>${user.active ? '<span class="badge">ACTIF</span>' : '<span class="badge archived">INACTIF</span>'}</td>
            <td>
                <div class="row-actions">
                    <button type="button" data-user-edit="${user.id}">Editer</button>
                    <button type="button" class="danger" data-user-delete="${user.id}">Supprimer</button>
                </div>
            </td>
        </tr>
    `).join("");
}

function resetCitizenForm() {
    state.citizenEditId = null;
    const form = document.getElementById("citizen-form");
    form.reset();
    document.getElementById("citizen-form-title").textContent = "Nouveau citoyen";
}

function fillCitizenForm(citizen) {
    if (!state.permissions?.citizensCreateUpdate) {
        return;
    }
    state.citizenEditId = citizen.id;
    const form = document.getElementById("citizen-form");
    form.elements.id.value = citizen.id;
    form.elements.nationalNumber.value = citizen.nationalNumber || "";
    form.elements.firstName.value = citizen.firstName || "";
    form.elements.lastName.value = citizen.lastName || "";
    form.elements.birthDate.value = citizen.birthDate || "";
    form.elements.birthPlace.value = citizen.birthPlace || "";
    form.elements.gender.value = citizen.gender || "MALE";
    form.elements.address.value = citizen.address || "";
    form.elements.region.value = citizen.region || "";
    form.elements.profession.value = citizen.profession || "";
    form.elements.photo.value = citizen.photo || "";
    document.getElementById("citizen-form-title").textContent = `Modifier citoyen #${citizen.id}`;
}

function renderCitizens() {
    const body = document.getElementById("citizens-body");
    const canEdit = !!state.permissions?.citizensCreateUpdate;
    const canArchive = !!state.permissions?.citizensArchive;

    body.innerHTML = state.citizens.map((citizen) => {
        const fullName = `${citizen.firstName || ""} ${citizen.lastName || ""}`.trim();
        const birth = `${citizen.birthDate || "-"} / ${citizen.birthPlace || "-"}`;
        const actions = [];
        if (canEdit) {
            actions.push(`<button type="button" data-citizen-edit="${citizen.id}">Editer</button>`);
        }
        if (canArchive) {
            actions.push(`<button type="button" class="danger" data-citizen-delete="${citizen.id}">Archiver</button>`);
        }

        return `
            <tr>
                <td>${safeText(citizen.id)}</td>
                <td>${safeText(citizen.nationalNumber)}</td>
                <td>${safeText(fullName)}</td>
                <td>${safeText(birth)}</td>
                <td>${safeText(citizen.region)}</td>
                <td>${citizen.archived ? '<span class="badge archived">ARCHIVE</span>' : '<span class="badge">ACTIF</span>'}</td>
                <td>${actions.length ? `<div class="row-actions">${actions.join("")}</div>` : "-"}</td>
            </tr>
        `;
    }).join("");
}

function renderRequests() {
    const body = document.getElementById("requests-body");
    const canUpdateStatus = !!state.permissions?.requestsStatusUpdate;

    body.innerHTML = state.requests.map((request) => {
        const controls = canUpdateStatus
            ? `<div class="row-actions">
                    <select data-status-id="${request.id}">
                        <option value="PENDING">PENDING</option>
                        <option value="IN_PROGRESS">IN_PROGRESS</option>
                        <option value="VALIDATED">VALIDATED</option>
                        <option value="REJECTED">REJECTED</option>
                        <option value="PRINTED">PRINTED</option>
                    </select>
                    <button type="button" data-status-save="${request.id}">Statut</button>
               </div>`
            : "-";

        return `
            <tr>
                <td>${safeText(request.id)}</td>
                <td>${safeText(request.fileNumber)}</td>
                <td>${safeText(request.citizenId)}</td>
                <td>${safeText(request.requestType)}</td>
                <td><span class="badge">${safeText(request.status)}</span></td>
                <td>${safeText(request.submissionDate)}</td>
                <td title="${safeText(request.qrCode)}">${safeText((request.qrCode || "").slice(0, 24))}...</td>
                <td>${controls}</td>
            </tr>
        `;
    }).join("");

    if (canUpdateStatus) {
        state.requests.forEach((request) => {
            const select = body.querySelector(`select[data-status-id="${request.id}"]`);
            if (select) {
                select.value = request.status;
            }
        });
    }
}

function renderRequestFormOptions() {
    const citizenOptions = document.getElementById("citizen-options");
    const agentOptions = document.getElementById("agent-options");

    citizenOptions.innerHTML = state.citizens
        .filter((citizen) => !citizen.archived)
        .map((citizen) => {
            const fullName = `${citizen.firstName || ""} ${citizen.lastName || ""}`.trim();
            return `<option value="${safeText(citizen.id)}">${safeText(fullName)} - ${safeText(citizen.nationalNumber)}</option>`;
        })
        .join("");

    agentOptions.innerHTML = state.users
        .filter((user) => user.active && ["ADMIN", "AGENT_REGISTRATION"].includes(user.role))
        .map((user) => {
            const fullName = `${user.firstName || ""} ${user.lastName || ""}`.trim();
            return `<option value="${safeText(user.id)}">${safeText(fullName)} - ${safeText(user.role)}</option>`;
        })
        .join("");
}

async function loadDashboard() {
    if (!state.permissions?.dashboardAccess) {
        return;
    }
    try {
        renderDashboard(await requestApi("/api/dashboard"));
    } catch (error) {
        appendLog("Dashboard echec", error.payload || error.message, true);
    }
}

async function loadUsers() {
    if (!state.permissions?.usersManage) {
        return;
    }
    try {
        state.users = await requestApi("/api/users");
        renderUsers();
        renderRequestFormOptions();
    } catch (error) {
        appendLog("Utilisateurs echec", error.payload || error.message, true);
    }
}

async function loadCitizens() {
    try {
        const search = document.getElementById("citizen-search").value.trim();
        const query = search ? `?search=${encodeURIComponent(search)}` : "";
        state.citizens = await requestApi(`/api/citizens${query}`);
        renderCitizens();
        renderRequestFormOptions();
    } catch (error) {
        appendLog("Citoyens echec", error.payload || error.message, true);
    }
}

async function loadRequests() {
    try {
        state.requests = await requestApi("/api/requests");
        renderRequests();
    } catch (error) {
        appendLog("Demandes echec", error.payload || error.message, true);
    }
}

async function loadMyProfile() {
    const me = await requestApi("/api/auth/me");
    state.currentUser = me;
    state.permissions = ROLE_PERMISSIONS[me.role] || ROLE_PERMISSIONS.AGENT_REGISTRATION;
    renderSession();
    applyRolePermissions();
}

async function loadAllByRole() {
    await Promise.all([loadDashboard(), loadUsers(), loadCitizens(), loadRequests()]);
}

async function handleUserSubmit(event) {
    event.preventDefault();
    if (!state.permissions?.usersManage) {
        return;
    }

    const form = event.currentTarget;
    const body = {
        firstName: form.elements.firstName.value.trim(),
        lastName: form.elements.lastName.value.trim(),
        email: form.elements.email.value.trim(),
        password: form.elements.password.value,
        role: form.elements.role.value,
        active: form.elements.active.checked
    };

    try {
        if (state.userEditId) {
            await requestApi(`/api/users/${state.userEditId}`, { method: "PUT", body });
        } else {
            await requestApi("/api/users", { method: "POST", body });
        }
        resetUserForm();
        await loadUsers();
    } catch (error) {
        appendLog("Utilisateur echec", error.payload || error.message, true);
    }
}

async function handleCitizenSubmit(event) {
    event.preventDefault();
    if (!state.permissions?.citizensCreateUpdate) {
        return;
    }

    const form = event.currentTarget;
    const body = {
        nationalNumber: form.elements.nationalNumber.value.trim(),
        firstName: form.elements.firstName.value.trim(),
        lastName: form.elements.lastName.value.trim(),
        birthDate: form.elements.birthDate.value,
        birthPlace: form.elements.birthPlace.value.trim(),
        gender: form.elements.gender.value,
        address: form.elements.address.value.trim(),
        region: form.elements.region.value.trim(),
        profession: form.elements.profession.value.trim(),
        photo: form.elements.photo.value.trim()
    };

    try {
        if (state.citizenEditId) {
            await requestApi(`/api/citizens/${state.citizenEditId}`, { method: "PUT", body });
        } else {
            await requestApi("/api/citizens", { method: "POST", body });
        }
        resetCitizenForm();
        await loadCitizens();
    } catch (error) {
        appendLog("Citoyen echec", error.payload || error.message, true);
    }
}

async function handleRequestSubmit(event) {
    event.preventDefault();
    if (!state.permissions?.requestsCreate) {
        return;
    }

    const form = event.currentTarget;
    const agentValue = form.elements.agentResponsibleId.value.trim();
    const citizenId = Number(form.elements.citizenId.value);
    const agentResponsibleId = agentValue ? Number(agentValue) : null;

    if (agentResponsibleId !== null && state.users.length > 0) {
        const agent = state.users.find((entry) => Number(entry.id) === agentResponsibleId);
        if (!agent) {
            appendLog("Creation demande echec", "L'agent responsable est introuvable. Utilise un ID valide ou laisse ce champ vide.", true);
            return;
        }

        if (!agent.active) {
            appendLog("Creation demande echec", "L'agent responsable selectionne est inactif.", true);
            return;
        }
    }

    const body = {
        citizenId,
        agentResponsibleId,
        requestType: form.elements.requestType.value
    };

    try {
        const citizen = await requestApi(`/api/citizens/${citizenId}`);
        if (citizen.archived) {
            appendLog("Creation demande echec", "Le citoyen selectionne est archive et ne peut pas deposer de demande.", true);
            return;
        }

        await requestApi("/api/requests", { method: "POST", body });
        form.reset();
        await Promise.all([loadRequests(), loadDashboard()]);
    } catch (error) {
        appendLog("Creation demande echec", error.payload || error.message, true);
    }
}

async function handleVerify(event) {
    event.preventDefault();
    const qrCode = document.getElementById("qr-input").value.trim();
    try {
        const response = await fetch(`/api/verify/${encodeURIComponent(qrCode)}`);
        const payload = await response.json();
        document.getElementById("verify-output").textContent = JSON.stringify(payload, null, 2);
        appendLog("Verification QR", payload);
    } catch (error) {
        appendLog("Verification QR echec", error.message, true);
    }
}

function wireDelegatedActions() {
    document.getElementById("users-body").addEventListener("click", async (event) => {
        if (!state.permissions?.usersManage) {
            return;
        }

        const editId = event.target.getAttribute("data-user-edit");
        const deleteId = event.target.getAttribute("data-user-delete");

        if (editId) {
            const user = state.users.find((u) => String(u.id) === String(editId));
            if (user) {
                fillUserForm(user);
            }
            return;
        }

        if (deleteId) {
            if (!confirm(`Supprimer utilisateur #${deleteId} ?`)) {
                return;
            }
            try {
                await requestApi(`/api/users/${deleteId}`, { method: "DELETE" });
                await loadUsers();
            } catch (error) {
                appendLog("Suppression utilisateur echec", error.payload || error.message, true);
            }
        }
    });

    document.getElementById("citizens-body").addEventListener("click", async (event) => {
        const editId = event.target.getAttribute("data-citizen-edit");
        const deleteId = event.target.getAttribute("data-citizen-delete");

        if (editId && state.permissions?.citizensCreateUpdate) {
            const citizen = state.citizens.find((c) => String(c.id) === String(editId));
            if (citizen) {
                fillCitizenForm(citizen);
            }
            return;
        }

        if (deleteId && state.permissions?.citizensArchive) {
            if (!confirm(`Archiver citoyen #${deleteId} ?`)) {
                return;
            }
            try {
                await requestApi(`/api/citizens/${deleteId}`, { method: "DELETE" });
                await Promise.all([loadCitizens(), loadDashboard()]);
            } catch (error) {
                appendLog("Archivage citoyen echec", error.payload || error.message, true);
            }
        }
    });

    document.getElementById("requests-body").addEventListener("click", async (event) => {
        if (!state.permissions?.requestsStatusUpdate) {
            return;
        }

        const statusId = event.target.getAttribute("data-status-save");
        if (!statusId) {
            return;
        }

        const select = document.querySelector(`select[data-status-id="${statusId}"]`);
        const nextStatus = select ? select.value : "PENDING";

        try {
            await requestApi(`/api/requests/${statusId}/status`, {
                method: "PUT",
                body: { status: nextStatus }
            });
            await Promise.all([loadRequests(), loadDashboard()]);
        } catch (error) {
            appendLog("Maj statut echec", error.payload || error.message, true);
        }
    });
}

function wireNavigation() {
    document.querySelectorAll(".nav-link").forEach((button) => {
        button.addEventListener("click", () => setActiveTab(button.dataset.tab));
    });
}

function wireFormsAndButtons() {
    document.getElementById("user-form").addEventListener("submit", handleUserSubmit);
    document.getElementById("citizen-form").addEventListener("submit", handleCitizenSubmit);
    document.getElementById("request-form").addEventListener("submit", handleRequestSubmit);
    document.getElementById("verify-form").addEventListener("submit", handleVerify);

    document.getElementById("refresh-dashboard").addEventListener("click", loadDashboard);
    document.getElementById("refresh-users").addEventListener("click", loadUsers);
    document.getElementById("refresh-citizens").addEventListener("click", loadCitizens);
    document.getElementById("refresh-requests").addEventListener("click", loadRequests);

    document.getElementById("cancel-user-edit").addEventListener("click", resetUserForm);
    document.getElementById("cancel-citizen-edit").addEventListener("click", resetCitizenForm);

    document.getElementById("clear-logs").addEventListener("click", () => {
        logOutput.textContent = "Pret.";
    });

    logoutBtn.addEventListener("click", () => {
        clearSession();
        redirectToLogin();
    });
}

async function initialLoad() {
    if (!state.token) {
        redirectToLogin();
        return;
    }

    wireNavigation();
    wireFormsAndButtons();
    wireDelegatedActions();
    resetUserForm();
    resetCitizenForm();

    try {
        await loadMyProfile();
        await loadAllByRole();
    } catch (error) {
        appendLog("Session invalide", error.payload || error.message, true);
        clearSession();
        setTimeout(redirectToLogin, 400);
    }
}

initialLoad();
