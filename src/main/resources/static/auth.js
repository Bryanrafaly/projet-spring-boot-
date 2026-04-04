const logOutput = document.getElementById("log-output");
const pendingLoginChallengeKey = "cni_pending_login_challenge";
const pendingResetChallengeKey = "cni_pending_reset_challenge";

if (localStorage.getItem("cni_jwt")) {
    window.location.href = "/index.html";
}

function appendLog(title, payload, isError = false) {
    if (!logOutput) {
        return;
    }
    const time = new Date().toLocaleTimeString("fr-FR");
    const body = typeof payload === "string" ? payload : JSON.stringify(payload, null, 2);
    const line = `[${time}] ${isError ? "ERROR" : "INFO"} - ${title}\n${body}\n\n`;
    logOutput.textContent = line + logOutput.textContent;
}

async function requestApi(url, { method = "GET", body = null } = {}) {
    const headers = {};
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
        const message = payload && payload.message ? payload.message : response.statusText;
        const error = new Error(message || "API error");
        error.payload = payload;
        throw error;
    }

    return payload;
}

function bindRegisterPage() {
    const form = document.getElementById("register-form");
    if (!form) {
        return;
    }

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        const body = {
            firstName: form.elements.firstName.value.trim(),
            lastName: form.elements.lastName.value.trim(),
            email: form.elements.email.value.trim(),
            password: form.elements.password.value,
            role: form.elements.role.value
        };

        try {
            const payload = await requestApi("/api/auth/register", { method: "POST", body });
            appendLog("Inscription OK", payload);
            form.reset();
            setTimeout(() => {
                window.location.href = "/login.html";
            }, 700);
        } catch (error) {
            appendLog("Inscription echec", error.payload || error.message, true);
        }
    });
}

function bindLoginPage() {
    const loginForm = document.getElementById("login-form");
    const otpForm = document.getElementById("otp-form");
    const challengeInput = document.getElementById("challenge-id");
    const forgotForm = document.getElementById("forgot-form");
    const resetForm = document.getElementById("reset-form");
    const resetChallengeInput = document.getElementById("reset-challenge-id");

    if (challengeInput) {
        challengeInput.value = sessionStorage.getItem(pendingLoginChallengeKey) || "";
    }

    if (resetChallengeInput) {
        resetChallengeInput.value = sessionStorage.getItem(pendingResetChallengeKey) || "";
    }

    if (loginForm) {
        loginForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const body = {
                email: loginForm.elements.email.value.trim(),
                password: loginForm.elements.password.value
            };

            try {
                const payload = await requestApi("/api/auth/login", { method: "POST", body });
                if (payload.challengeId) {
                    sessionStorage.setItem(pendingLoginChallengeKey, payload.challengeId);
                }
                if (payload.emailSent === false) {
                    appendLog("OTP email non envoye", "Le serveur n'a pas pu envoyer l'email OTP.", true);
                }
                if (payload.debugOtpCode) {
                    appendLog("OTP de test (email indisponible)", { debugOtpCode: payload.debugOtpCode }, true);
                }
                appendLog("Connexion etape 1 OK", payload);
                setTimeout(() => {
                    window.location.href = "/otp.html";
                }, 300);
            } catch (error) {
                appendLog("Connexion etape 1 echec", error.payload || error.message, true);
            }
        });
    }

    if (otpForm) {
        otpForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const body = {
                challengeId: otpForm.elements.challengeId.value.trim(),
                otpCode: otpForm.elements.otpCode.value.trim()
            };

            try {
                const payload = await requestApi("/api/auth/verify-otp", { method: "POST", body });
                if (!payload.token) {
                    appendLog("Verification OTP", "Token manquant", true);
                    return;
                }
                sessionStorage.removeItem(pendingLoginChallengeKey);
                localStorage.setItem("cni_jwt", payload.token);
                appendLog("Connexion etape 2 OK", payload);
                window.location.href = "/index.html";
            } catch (error) {
                appendLog("Verification OTP echec", error.payload || error.message, true);
            }
        });
    }

    if (forgotForm) {
        forgotForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const body = {
                email: forgotForm.elements.email.value.trim()
            };

            try {
                const payload = await requestApi("/api/auth/forgot-password", { method: "POST", body });
                if (!payload.challengeId) {
                    appendLog("Reset indisponible", payload.message || "Aucun compte associe a cet email.", true);
                    return;
                }
                sessionStorage.setItem(pendingResetChallengeKey, payload.challengeId);
                if (payload.emailSent === false) {
                    appendLog("Reset email non envoye", "Le serveur n'a pas pu envoyer l'email de reinitialisation.", true);
                }
                if (payload.debugOtpCode) {
                    appendLog("Code reset de test (email indisponible)", { debugOtpCode: payload.debugOtpCode }, true);
                }
                appendLog("Reset etape 1 OK", payload);
                setTimeout(() => {
                    window.location.href = "/reset-password.html";
                }, 300);
            } catch (error) {
                appendLog("Reset etape 1 echec", error.payload || error.message, true);
            }
        });
    }

    if (resetForm) {
        resetForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const body = {
                challengeId: resetForm.elements.challengeId.value.trim(),
                otpCode: resetForm.elements.otpCode.value.trim(),
                newPassword: resetForm.elements.newPassword.value
            };

            try {
                const payload = await requestApi("/api/auth/reset-password", { method: "POST", body });
                appendLog("Reset etape 2 OK", payload);
                resetForm.reset();
                sessionStorage.removeItem(pendingResetChallengeKey);
                setTimeout(() => {
                    window.location.href = "/login.html";
                }, 500);
            } catch (error) {
                appendLog("Reset etape 2 echec", error.payload || error.message, true);
            }
        });
    }
}

function bindCommonActions() {
    const clear = document.getElementById("clear-logs");
    if (clear && logOutput) {
        clear.addEventListener("click", () => {
            logOutput.textContent = "Pret.";
        });
    }
}

bindRegisterPage();
bindLoginPage();
bindCommonActions();
