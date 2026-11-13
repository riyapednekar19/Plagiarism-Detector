// ==========================================
// 🌐 GLOBAL STATE VARIABLES
// ==========================================
let activeCurrentUser = "Guest Explorer";
let activeUserRole = "Student";
let selectedFileObject = null;
let currentTheme = "light";

// Active scan toggles
let activeScanMode = "compare"; // Options: 'compare' | 'internet' | 'web'
let activeInputType = "text";   // Options: 'text' | 'doc'

// Global payload for PDF export
let lastScanResults = null;

// Stopwords set for text comparison highlighting
const stopWords = new Set([
    "a", "an", "the", "is", "and", "or", "of", "to", "in", "on", "at", "by", "for", "with", "this", "that"
]);

// Base API Configuration
const API_BASE_URL = "http://localhost:8080/api";

// ==========================================
// 🛠️ UTILITY & HELPER FUNCTIONS
// ==========================================

function getAuthHeaders() {
    const headers = {
        "Content-Type": "application/json"
    };
    const token = localStorage.getItem("jwtToken") || localStorage.getItem("token") || sessionStorage.getItem("token");
    if (token) {
        headers["Authorization"] = `Bearer ${token}`;
    }
    return headers;
}

function typeWriterEffect(element, text, speed = 10) {
    if (!element) return;
    element.className = "";
    element.textContent = "";
    element.style.whiteSpace = "pre-line";

    let index = 0;
    function type() {
        if (index < text.length) {
            element.textContent += text.charAt(index);
            index++;
            setTimeout(type, speed);
        }
    }
    type();
}

// Ensure function is visible to inline HTML event attributes
window.dismissSplashAndShowLogin = function() {
    const splashScreen = document.getElementById("splash-screen");
    if (splashScreen) {
        splashScreen.classList.add("splash-hidden");
        setTimeout(() => {
            splashScreen.style.display = "none";
        }, 500);
    }

    const loginScreen = document.getElementById("login-screen");
    if (loginScreen) {
        loginScreen.style.display = "block";
        loginScreen.classList.remove("hidden");
    } else if (typeof switchMainTab === "function") {
        switchMainTab("login-screen");
    }
};

function dismissSplashAndShowLogin() {
    window.dismissSplashAndShowLogin();
}

/**
 * 🎯 Core Function: Switches view from Auth/Splash screens to the Main Dashboard
 */
// Function to hide login and show dashboard
function showMainDashboard() {
    const splashScreen = document.getElementById("splash-screen");
    if (splashScreen) splashScreen.style.display = "none";

    const loginScreen = document.getElementById("login-screen") || document.querySelector(".auth-form-container") || document.querySelector(".auth-form");
    if (loginScreen) {
        loginScreen.style.display = "none";
        loginScreen.closest('.splash-container')?.remove(); // removes overlay wrapper if present
    }

    // Unhide Main Dashboard App Workspace
    const mainApp = document.getElementById("main-app-content") || document.querySelector(".app-main") || document.querySelector("main");
    if (mainApp) {
        mainApp.style.display = "block";
        mainApp.style.visibility = "visible";
        mainApp.style.opacity = "1";
    }
}

// Authentication Handler
async function handleLogin(event) {
    if (event) event.preventDefault();

    const emailInput = document.getElementById("login-email") || document.querySelector("input[type='email']");
    const passwordInput = document.getElementById("login-password") || document.querySelector("input[type='password']");

    const email = emailInput ? emailInput.value.trim() : "";
    const password = passwordInput ? passwordInput.value.trim() : "";

    if (!email || !password) {
        alert("Please enter both email and password.");
        return;
    }

    // Save session locally and navigate immediately
    const username = email.split('@')[0];
    localStorage.setItem("activeUser", username);

    alert(`Login successful. Welcome, ${username}!`);
    showMainDashboard();

    document.getElementById('auth-container').style.display = 'none';
    // OR add a hidden class
    // document.getElementById('auth-container').classList.add('hidden');

    // Show main workspace
    document.getElementById('workspace-container').style.display = 'block';
}

// Event Delegation - Targets .auth-primary-btn directly
document.addEventListener("click", function(e) {
    const target = e.target;

    // Match either the button class from your DevTools screenshot OR id
    if (!(target.matches(".auth-primary-btn") || target.matches("#btn-submit-login") || target.closest(".auth-primary-btn"))) {
    } else {
        e.preventDefault();
        handleLogin(e);
    }

    // Match "Get Started for Free" link
    if (target.matches("#btn-bypass-auth") || target.matches(".guest-link") || target.innerText.includes("Get Started for Free")) {
        e.preventDefault();
        showMainDashboard();
    }
});

// Submit event fallback if form is submitted via Enter key
document.addEventListener("submit", function(e) {
    e.preventDefault();
    handleLogin(e);
});

// ==========================================
// 🔑 AUTHENTICATION & NAVIGATION HANDLERS
// ==========================================

async function handleLogin(event) {
    if (event) event.preventDefault();

    const emailInput = document.getElementById("login-email") || document.getElementById("email");
    const passwordInput = document.getElementById("login-password") || document.getElementById("password");

    const email = emailInput ? emailInput.value.trim() : "";
    const password = passwordInput ? passwordInput.value.trim() : "";

    if (!email || !password) {
        alert("Please enter both email and password.");
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, password })
        });

        if (response.ok) {
            const data = await response.json();

            // Store token and user state
            if (data.token) {
                localStorage.setItem("jwtToken", data.token);
            }

            activeCurrentUser = data.username || email.split('@')[0] || "Authenticated User";
            localStorage.setItem("activeUser", activeCurrentUser);

            alert(`Login successful. Welcome, ${activeCurrentUser}!`);

            // Navigate directly to dashboard view
            showMainDashboard();
        } else {
            const errorData = await response.json().catch(() => ({}));
            alert(errorData.message || "Invalid credentials. Please try again.");
        }
    } catch (error) {
        console.warn("Backend auth failed or server offline. Defaulting to local login state for demonstration:", error);

        // Fallback for local testing/demo if backend server is unreachable
        activeCurrentUser = email.split('@')[0] || "User";
        localStorage.setItem("activeUser", activeCurrentUser);
        alert(`Welcome, ${activeCurrentUser}!`);
        showMainDashboard();
    }
}

function bypassAuthAndStart(event) {
    if (event) event.preventDefault();
    activeCurrentUser = "Guest Explorer";
    activeUserRole = "Guest";
    showMainDashboard();
}

// ==========================================
// 🚀 1. SYSTEM INITIALIZATION & EVENT BINDINGS
// ==========================================
window.addEventListener("DOMContentLoaded", () => {
    // Auto-check if user is already authenticated
    const token = localStorage.getItem("jwtToken") || localStorage.getItem("token");
    const savedUser = localStorage.getItem("activeUser");

    if (token || savedUser) {
        if (savedUser) activeCurrentUser = savedUser;
        showMainDashboard();
    }

    setupTabSelectors();
    bindGlobalActionButtons();
    bindDropdownDismissal();
    if (typeof syncHistoryLogs === "function") syncHistoryLogs();
});

function bindGlobalActionButtons() {
    // Splash Screen
    const btnGetStarted = document.getElementById("btn-splash-get-started");
    if (btnGetStarted) btnGetStarted.addEventListener("click", window.dismissSplashAndShowLogin);

    // Navigation Links
    document.querySelectorAll('[data-nav-target]').forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const target = item.getAttribute('data-nav-target');
            if (target) {
                if (typeof switchMainTab === "function") {
                    switchMainTab(target);
                }
            }
        });
    });

    // Theme Toggle
    const themeBtn = document.getElementById("theme-toggle-btn");
    if (themeBtn && typeof toggleTheme === "function") {
        themeBtn.addEventListener("click", toggleTheme);
    }

    // Authentication Form Buttons
    const btnLogin = document.getElementById("btn-submit-login");
    if (btnLogin) btnLogin.addEventListener("click", handleLogin);

    const btnSignup = document.getElementById("btn-submit-signup");
    if (btnSignup) {
        btnSignup.addEventListener("click", (e) => {
            if (typeof handleSignUp === "function") handleSignUp(e);
        });
    }

    const btnBypass = document.getElementById("btn-bypass-auth");
    if (btnBypass) btnBypass.addEventListener("click", bypassAuthAndStart);

    // Main Verification Action
    const btnExecuteScan = document.getElementById("btn-execute-scan");
    if (btnExecuteScan) {
        btnExecuteScan.addEventListener("click", (e) => {
            if (typeof executeSelectedVerificationScan === "function") executeSelectedVerificationScan(e);
        });
    }

    // Tools Actions
    const btnGrammar = document.getElementById("btn-run-grammar");
    if (btnGrammar) {
        btnGrammar.addEventListener("click", (e) => {
            if (typeof runGrammarCheck === "function") runGrammarCheck(e);
        });
    }

    const btnAiDetect = document.getElementById("btn-run-ai");
    if (btnAiDetect) {
        btnAiDetect.addEventListener("click", (e) => {
            if (typeof runAIDetection === "function") runAIDetection(e);
        });
    }

    const btnSummarize = document.getElementById("btn-run-summarize");
    if (btnSummarize) {
        btnSummarize.addEventListener("click", (e) => {
            if (typeof runSummarizer === "function") runSummarizer(e);
        });
    }

    const btnExportPdf = document.getElementById("btn-export-pdf");
    if (btnExportPdf) {
        btnExportPdf.addEventListener("click", (e) => {
            if (typeof downloadPdfReport === "function") downloadPdfReport(e);
        });
    }

    // File Input
    const fileInput = document.getElementById("documentFileInput");
    if (fileInput) {
        fileInput.addEventListener("change", (e) => {
            if (typeof handleFileSelection === "function") handleFileSelection(e);
        });
    }
}

function bindDropdownDismissal() {
    window.addEventListener("click", (event) => {
        if (!event.target.matches('.nav-brand-logo') &&
            !event.target.closest('.nav-brand-logo') &&
            !event.target.closest('.user-dropdown-btn')) {
            const dropdowns = document.getElementsByClassName("logo-dropdown-content");
            for (let i = 0; i < dropdowns.length; i++) {
                if (dropdowns[i].classList.contains('show')) {
                    dropdowns[i].classList.remove('show');
                }
            }
        }
    });
}

// ==========================================
// 🎛️ 2. TAB SELECTORS & VIEW TOGGLES
// ==========================================
function setupTabSelectors() {
    const segmentedBtns = document.querySelectorAll(".segmented-btn");
    segmentedBtns.forEach(btn => {
        btn.addEventListener("click", (e) => {
            e.preventDefault();
            segmentedBtns.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");

            const mode = btn.getAttribute("data-tab");
            if (mode) toggleScanMode(mode);
        });
    });

    const underlineBtns = document.querySelectorAll(".tab-btn");
    underlineBtns.forEach(btn => {
        btn.addEventListener("click", (e) => {
            e.preventDefault();
            underlineBtns.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");

            const inputType = btn.getAttribute("data-target");
            if (inputType) switchInputMode(inputType);
        });
    });
}

function toggleScanMode(mode) {
    activeScanMode = mode;
    const sourceContainer = document.getElementById("source-text-container");
    const compareRadio = document.getElementById("mode-compare");
    const webRadio = document.getElementById("mode-web");

    if (mode === 'internet' || mode === 'web') {
        if (sourceContainer) sourceContainer.style.display = 'none';
        if (webRadio) webRadio.checked = true;
    } else {
        if (sourceContainer) sourceContainer.style.display = 'block';
        if (compareRadio) compareRadio.checked = true;
    }
}

function switchInputMode(mode) {
    if (mode.includes("text")) {
        activeInputType = "text";
    } else if (mode.includes("doc")) {
        activeInputType = "doc";
    }

    const textContainer = document.getElementById("text-input-container");
    const docContainer = document.getElementById("document-input-container");

    if (activeInputType === "text") {
        if (textContainer) textContainer.style.display = "block";
        if (docContainer) docContainer.style.display = "none";
    } else {
        if (textContainer) textContainer.style.display = "none";
        if (docContainer) docContainer.style.display = "block";
    }
}

function switchMainTab(targetId) {
    const layers = document.querySelectorAll(".tab-layer, .app-slide, .app-viewport");
    layers.forEach(layer => {
        layer.classList.remove("active");
        layer.classList.add("hidden");
        layer.style.display = "none";
    });

    const targetLayer = document.getElementById(targetId);
    if (targetLayer) {
        targetLayer.classList.remove("hidden");
        targetLayer.classList.add("active");
        targetLayer.style.display = "block";
        targetLayer.style.animation = "fadeInUp 0.35s ease-out forwards";
    }

    if (targetId === "history-view" || targetId === "history-screen") {
        syncHistoryLogs();
    }
}

// ==========================================
// 🌓 3. THEME & USER DROPDOWN CONTROLS
// ==========================================
function toggleTheme() {
    const body = document.body;
    if (body.getAttribute("data-theme") === "dark") {
        body.removeAttribute("data-theme");
        currentTheme = "light";
    } else {
        body.setAttribute("data-theme", "dark");
        currentTheme = "dark";
    }
}

function toggleLogoMenu(event) {
    if (event) event.stopPropagation();
    const menu = document.getElementById("logoDropdownMenu");
    if (menu) {
        menu.classList.toggle("show");
    }
}

// ==========================================
// 🔐 4. AUTHENTICATION FLOW
// ==========================================

window.togglePasswordVisibility = function(id) {
    const field = document.getElementById(id);
    if (field) {
        field.type = field.type === "password" ? "text" : "password";
    }
};

window.switchAuthForm = function(mode) {
    const loginCard = document.getElementById("login-card");
    const signupCard = document.getElementById("signup-card");

    if (mode === "signup") {
        if (loginCard) loginCard.classList.add("hidden");
        if (signupCard) signupCard.classList.remove("hidden");
    } else {
        if (signupCard) signupCard.classList.add("hidden");
        if (loginCard) loginCard.classList.remove("hidden");
    }
};

window.transitionToWorkspace = function() {
    // Hide the login screen container
    const loginScreen = document.getElementById("login-screen");
    const heroScreen = document.getElementById("hero-screen");

    if (loginScreen) loginScreen.style.display = "none";
    if (heroScreen) heroScreen.style.display = "none";

    // Find workspace container (checks common naming variations)
    const workspaceScreen = document.getElementById("workspace-screen")
        || document.getElementById("main-workspace")
        || document.getElementById("app-container")
        || document.querySelector(".workspace-container");

    if (workspaceScreen) {
        workspaceScreen.classList.remove("hidden");
        workspaceScreen.style.display = "block";
    } else {
        console.error("Could not find workspace container. Ensure your main app section has id='workspace-screen'.");
    }
};

window.handleSignUp = function(event) {
    if (event) event.preventDefault();

    const nameVal = document.getElementById("signup-name")?.value.trim();
    const emailVal = document.getElementById("signup-email")?.value.trim();
    const passVal = document.getElementById("signup-password")?.value.trim();

    if (!nameVal || !emailVal || !passVal) {
        alert("Please fill in all fields to sign up.");
        return;
    }

    if (typeof activeCurrentUser !== "undefined") activeCurrentUser = nameVal;
    if (typeof updateUserDisplay === "function") updateUserDisplay();

    window.transitionToWorkspace();
};

window.handleLogin = function(event) {
    if (event) event.preventDefault();

    const emailVal = document.getElementById("login-email")?.value.trim();
    const passVal = document.getElementById("login-password")?.value.trim();

    if (!emailVal || !passVal) {
        alert("Please enter both email and password to log in.");
        return;
    }

    if (typeof activeCurrentUser !== "undefined") activeCurrentUser = emailVal.split('@')[0];
    if (typeof updateUserDisplay === "function") updateUserDisplay();

    window.transitionToWorkspace();
};

window.bypassAuthAndStart = function(event) {
    if (event) event.preventDefault();

    if (typeof activeCurrentUser !== "undefined") activeCurrentUser = "Guest Explorer";
    if (typeof updateUserDisplay === "function") updateUserDisplay();

    window.transitionToWorkspace();
};

// ==========================================
// 📂 5. FILE SELECTION
// ==========================================
function handleFileSelection() {
    const input = document.getElementById("documentFileInput");
    const status = document.getElementById("fileStatusDisplay");

    if (input && input.files.length > 0) {
        selectedFileObject = input.files[0];
        if (status) {
            const sizeInKB = (selectedFileObject.size / 1024).toFixed(1);
            status.textContent = `Selected Document: ${selectedFileObject.name} (${sizeInKB} KB)`;
        }
    }
}

// ==========================================
// ⚡ 6. SCAN EXECUTION & API PIPELINES
// ==========================================
function executeSelectedVerificationScan() {
    if (activeInputType === "text") {
        const submission = document.getElementById("submissionInput")?.value.trim();

        if (!submission) {
            alert("Please provide target text to perform plagiarism check.");
            return;
        }

        if (activeScanMode === "internet" || activeScanMode === "web") {
            dispatchWebScanAPI(submission, "Live Web Scan");
        } else {
            const source = document.getElementById("sourceInput")?.value.trim();
            if (!source) {
                alert("Provide source comparison text for manual mode.");
                return;
            }
            dispatchPlagiarismAPI(submission, source, "Manual Text Scan");
        }
    } else {
        if (!selectedFileObject) {
            alert("Upload a valid file format (.txt, .docx, .pdf) first.");
            return;
        }

        dispatchDocumentAPI(selectedFileObject);
    }
}

function dispatchPlagiarismAPI(submissionText, sourceText, documentTitle) {
    showResultsPanel();

    const payload = {
        submissionText: submissionText,
        sourceText: sourceText,
        username: String(activeCurrentUser),
        documentName: documentTitle,
        excludeBibliography: document.getElementById("excludeBib")?.checked || false,
        excludeQuotes: document.getElementById("excludeQuotes")?.checked || false
    };

    fetch(`${API_BASE_URL}/check`, {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
    })
        .then(res => {
            if (!res.ok) throw new Error("Verification trace failed on network socket.");
            return res.json();
        })
        .then(data => {
            lastScanResults = data;
            lastScanResults.documentName = documentTitle;
            updateVerificationUI(data);
        })
        .catch(err => {
            alert("Failed to reach processing cluster. Is local server online?");
            console.error("API Dispatch Error:", err);
        });
}

function dispatchWebScanAPI(submissionText, documentTitle) {
    showResultsPanel();

    const payload = {
        submissionText: submissionText,
        username: String(activeCurrentUser),
        documentName: documentTitle,
        excludeBibliography: document.getElementById("excludeBib")?.checked || false,
        excludeQuotes: document.getElementById("excludeQuotes")?.checked || false
    };

    fetch(`${API_BASE_URL}/web-scan`, {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
    })
        .then(res => {
            if (!res.ok) throw new Error("Live web scan processing failed.");
            return res.json();
        })
        .then(data => {
            lastScanResults = data;
            lastScanResults.documentName = documentTitle;
            updateVerificationUI(data);
        })
        .catch(err => {
            alert("Failed to reach processing cluster for live web scan.");
            console.error("Web Scan API Error:", err);
        });
}

function dispatchDocumentAPI(file) {
    showResultsPanel();

    const formData = new FormData();
    formData.append("file", file);
    formData.append("username", String(activeCurrentUser));
    formData.append("excludeBibliography", document.getElementById("excludeBib")?.checked || false);
    formData.append("excludeQuotes", document.getElementById("excludeQuotes")?.checked || false);

    const headers = {};
    const token = localStorage.getItem("jwtToken") || localStorage.getItem("token") || sessionStorage.getItem("token");
    if (token) headers["Authorization"] = `Bearer ${token}`;

    fetch(`${API_BASE_URL}/check-document`, {
        method: "POST",
        headers: headers,
        body: formData
    })
        .then(res => {
            if (!res.ok) throw new Error("Document analysis failed on backend server.");
            return res.json();
        })
        .then(data => {
            lastScanResults = data;
            lastScanResults.documentName = file.name;
            updateVerificationUI(data);
        })
        .catch(err => {
            alert("Failed to process document upload. Check server logs.");
            console.error("Document API Error:", err);
        });
}

function showResultsPanel() {
    const panel = document.getElementById("results-panel");
    if (panel) {
        panel.classList.remove("hidden");
        panel.style.display = "block";
        panel.style.opacity = "1";
        panel.style.animation = "fadeInUp 0.35s ease-out forwards";
    }
}

function updateVerificationUI(data) {
    const pctElem = document.getElementById("percentageCircle");
    const matchPct = data.percentage || data.matchPercentage || data.finalScore || 0;
    if (pctElem) pctElem.textContent = `${matchPct}%`;

    const sevElem = document.getElementById("severity-label");
    if (sevElem) sevElem.textContent = `Scan Complete: Status set to [${data.status || 'Verified'}]`;

    const submittedText = data.submittedContent || "";
    const matchedText = data.matchedResourceContent || "";

    renderSideBySideDifference(submittedText, matchedText);
    renderCitations(data.citations || []);
    renderReportSection(data);

    syncHistoryLogs();
}

function renderReportSection(data) {
    const reportContent = document.getElementById('reportContent') || document.querySelector('.report-body');

    if (!reportContent) return;

    if (!data) {
        reportContent.innerHTML = `<p style="color: #94a3b8; font-size: 13px; margin: 0;">Awaiting report generation...</p>`;
        return;
    }

    const finalScore = data.finalScore !== undefined ? data.finalScore : (data.percentage || data.matchPercentage || 0);
    const jaccardScore = data.jaccardScore !== undefined ? data.jaccardScore : 0;
    const cosineScore = data.cosineScore !== undefined ? data.cosineScore : 0;
    const status = data.status || "Verified";

    reportContent.innerHTML = `
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 15px; margin-bottom: 15px;">
            <div style="background: #ffffff; padding: 12px; border-radius: 6px; border: 1px solid #cbd5e1; text-align: center;">
                <div style="font-size: 11px; color: #64748b; text-transform: uppercase; font-weight: 600;">Overall Similarity</div>
                <div style="font-size: 18px; font-weight: bold; color: ${finalScore > 20 ? '#dc2626' : '#16a34a'}; margin-top: 4px;">${finalScore}%</div>
            </div>
            <div style="background: #ffffff; padding: 12px; border-radius: 6px; border: 1px solid #cbd5e1; text-align: center;">
                <div style="font-size: 11px; color: #64748b; text-transform: uppercase; font-weight: 600;">Lexical Overlap</div>
                <div style="font-size: 18px; font-weight: bold; color: #0f172a; margin-top: 4px;">${jaccardScore}%</div>
            </div>
            <div style="background: #ffffff; padding: 12px; border-radius: 6px; border: 1px solid #cbd5e1; text-align: center;">
                <div style="font-size: 11px; color: #64748b; text-transform: uppercase; font-weight: 600;">TF-IDF Vector</div>
                <div style="font-size: 18px; font-weight: bold; color: #0f172a; margin-top: 4px;">${cosineScore}%</div>
            </div>
            <div style="background: #ffffff; padding: 12px; border-radius: 6px; border: 1px solid #cbd5e1; text-align: center;">
                <div style="font-size: 11px; color: #64748b; text-transform: uppercase; font-weight: 600;">Status</div>
                <div style="font-size: 14px; font-weight: bold; color: #0f172a; margin-top: 4px;">${status}</div>
            </div>
        </div>
        <div style="font-size: 14px; color: #334155; line-height: 1.6; background: #ffffff; padding: 15px; border-radius: 6px; border: 1px solid #cbd5e1;">
            <strong>Executive Summary:</strong> The submitted text was evaluated against target comparison resources. 
            The scan revealed a <strong>${jaccardScore}%</strong> direct lexical keyword overlap and a <strong>${cosineScore}%</strong> TF-IDF semantic vector similarity, 
            resulting in an overall match index of <strong>${finalScore}%</strong>.
        </div>
    `;
}

function renderSideBySideDifference(submitted, original) {
    const subPane = document.getElementById("submittedPane");
    const srcPane = document.getElementById("sourcePane");

    if (!subPane || !srcPane) return;

    subPane.innerHTML = "";
    srcPane.textContent = original || "No direct textual match resource to render.";

    if (!submitted) return;

    const cleanSubmittedWords = submitted.split(/\s+/);
    const cleanOriginalWords = original ? original.toLowerCase().replace(/[^a-zA-Z0-9 ]/g, "") : "";

    cleanSubmittedWords.forEach(word => {
        const normalized = word.toLowerCase().replace(/[^a-zA-Z0-9]/g, "");

        if (normalized.length > 2 && !stopWords.has(normalized) && cleanOriginalWords.includes(normalized)) {
            subPane.innerHTML += `<span style="background: rgba(239, 68, 68, 0.3); padding: 2px 4px; border-radius: 4px; border: 1px solid var(--accent-red); margin-right: 4px;">${word}</span> `;
        } else {
            subPane.innerHTML += `${word} `;
        }
    });
}

function renderCitations(citations) {
    const listContainer = document.getElementById("citation-list");
    if (!listContainer) return;

    listContainer.innerHTML = "";

    if (!citations || citations.length === 0) {
        listContainer.innerHTML = `<span style="font-size:0.8rem; color:var(--text-muted);">No web citations detected for this scan.</span>`;
        return;
    }

    citations.forEach(item => {
        const faviconUrl = `https://www.google.com/s2/favicons?domain=${item.domain || 'example.com'}&sz=32`;
        const titleText = item.title ? item.title.substring(0, 30) + '...' : item.url;

        const cardHTML = `
            <div style="border-left: 3px solid var(--accent-blue); padding: 10px; margin-bottom: 8px; background: rgba(255,255,255,0.03); border-radius: 6px;">
                <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 4px;">
<!--                    <img src="${faviconUrl}" style="width: 16px; height: 16px;" alt="favicon" onerror="this.src='🌐'">-->
                    <img src="${faviconUrl}" style="width: 16px; height: 16px;" alt="favicon" onerror="this.onerror=null; this.src='/default-favicon.png';" />
                    <a href="${item.url}" target="_blank" style="font-weight: bold; color: var(--accent-blue); text-decoration: none; font-size: 0.85rem;">
                        ${titleText}
                    </a>
                </div>
                <div style="font-size: 0.75rem; color: var(--text-muted); font-weight: bold;">${item.domain || ''}</div>
                ${item.snippet ? `<p style="font-size: 0.8rem; color: var(--text-light); margin: 4px 0 0 0;">${item.snippet}</p>` : ''}
            </div>
        `;
        listContainer.innerHTML += cardHTML;
    });
}

// ==========================================
// 🛠️ 7. AUXILIARY TOOLS & GRAMMAR CHECK
// ==========================================
async function runGrammarCheck() {
    const textInput = document.getElementById("submissionInput")?.value.trim();
    const grammarResults = document.getElementById("grammarResults");
    const grammarList = document.getElementById("grammarList");

    if (!textInput) {
        alert("Please enter text in the box before checking grammar.");
        return;
    }

    if (grammarResults) {
        grammarResults.style.display = "block";
        grammarResults.style.animation = "fadeInUp 0.35s ease-out forwards";
    }

    if (grammarList) {
        grammarList.className = "loading-state";
        grammarList.innerHTML = "<span>Checking grammar...</span>";
    }

    try {
        const response = await fetch(`${API_BASE_URL}/grammar-check`, {
            method: "POST",
            headers: getAuthHeaders(),
            body: JSON.stringify({ text: textInput })
        });

        const data = await response.json();
        if (grammarList) grammarList.className = "";

        if (data.issues && data.issues.length > 0) {
            grammarList.innerHTML = "";
            data.issues.forEach(issue => {
                const suggestionsText = issue.suggestions && issue.suggestions.length > 0
                    ? issue.suggestions.join(", ")
                    : "None";

                const issueCard = document.createElement("div");
                issueCard.style.padding = "10px";
                issueCard.style.marginBottom = "8px";
                issueCard.style.background = "rgba(255, 255, 255, 0.08)";
                issueCard.style.borderRadius = "6px";
                issueCard.style.borderLeft = "4px solid var(--accent-red)";

                issueCard.innerHTML = `
                    <div style="font-weight: bold; color: var(--accent-red); font-size: 0.9rem;">
                        ⚠️ "${issue.word}"
                    </div>
                    <div style="font-size: 0.85rem; margin: 4px 0;">${issue.message}</div>
                    <div style="font-size: 0.8rem; color: var(--accent-green);">
                        <strong>Suggestions:</strong> ${suggestionsText}
                    </div>
                `;
                grammarList.appendChild(issueCard);
            });
        } else if (grammarList) {
            grammarList.innerHTML = `<span style="color: var(--accent-green); font-size: 0.85rem;">✅ No grammar or punctuation issues detected!</span>`;
        }
    } catch (err) {
        if (grammarList) {
            grammarList.className = "";
            grammarList.innerHTML = `<span style="color: var(--accent-red);">Failed to connect to grammar service.</span>`;
        }
    }
}

async function runAIDetection() {
    const text = document.getElementById("submissionInput")?.value.trim();
    if (!text) return alert("Please enter text first.");

    const resultsContainer = document.getElementById("aiSummaryResults");
    const content = document.getElementById("aiSummaryContent");
    const title = document.getElementById("aiSummaryTitle");

    if (title) title.innerText = "🤖 AI Probability Score";

    if (resultsContainer) {
        resultsContainer.style.display = "block";
        resultsContainer.style.animation = "fadeInUp 0.35s ease-out forwards";
    }

    if (content) {
        content.className = "loading-state";
        content.innerText = "Analyzing text patterns...";
    }

    try {
        const response = await fetch(`${API_BASE_URL}/detect-ai`, {
            method: "POST",
            headers: getAuthHeaders(),
            body: JSON.stringify({ text: text })
        });
        const data = await response.json();
        if (content) content.className = "";

        const label = data.label || "Unknown";
        const score = data.aiPercentage !== undefined ? data.aiPercentage : (data.score || 0);

        let badgeBg = "#dcfce7";
        let badgeColor = "#166534";
        if (score > 50) {
            badgeBg = "#fee2e2";
            badgeColor = "#991b1b";
        } else if (score > 20) {
            badgeBg = "#fef3c7";
            badgeColor = "#92400e";
        }

        if (content) {
            content.innerHTML = `
                <div style="padding: 12px; background: rgba(255, 255, 255, 0.05); border-radius: 8px; border: 1px solid var(--border-color); font-family: inherit;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
                        <span style="font-size: 0.9rem; font-weight: 600;">Classification</span>
                        <span style="background: ${badgeBg}; color: ${badgeColor}; padding: 3px 10px; border-radius: 12px; font-size: 0.8rem; font-weight: bold;">
                            ${label}
                        </span>
                    </div>
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <span style="font-size: 0.9rem; font-weight: 600;">AI Score</span>
                        <span style="font-size: 1.1rem; font-weight: bold; color: ${score > 50 ? 'var(--accent-red)' : 'var(--accent-green)'};">
                            ${score}%
                        </span>
                    </div>
                </div>
            `;
        }
    } catch (err) {
        if (content) {
            content.className = "";
            content.innerText = "Failed to connect to AI detection endpoint.";
        }
    }
}

async function runSummarizer() {
    const text = document.getElementById("submissionInput")?.value.trim();
    if (!text) return alert("Please enter text first.");

    const resultsContainer = document.getElementById("aiSummaryResults");
    const content = document.getElementById("aiSummaryContent");
    const title = document.getElementById("aiSummaryTitle");

    if (title) title.innerText = "⚡ Text Summary";

    if (resultsContainer) {
        resultsContainer.style.display = "block";
        resultsContainer.style.animation = "fadeInUp 0.35s ease-out forwards";
    }

    if (content) {
        content.className = "loading-state";
        content.innerText = "Generating summary...";
    }

    try {
        const response = await fetch(`${API_BASE_URL}/summarize`, {
            method: "POST",
            headers: getAuthHeaders(),
            body: JSON.stringify({ text: text })
        });

        const data = await response.json();
        const summaryText = data.summary || data.result || data.text || data.output || data.response;

        if (summaryText && content) {
            typeWriterEffect(content, summaryText);
        } else if (content) {
            content.className = "";
            content.innerText = "Unable to summarize text (Empty API response).";
        }
    } catch (err) {
        if (content) {
            content.className = "";
            content.innerText = "Failed to connect to summarizer service.";
        }
        console.error("Summarize Error:", err);
    }
}

async function downloadPdfReport() {
    const submittedText = document.getElementById("submissionInput")?.value || "";
    if (!submittedText.trim() && (!lastScanResults || !lastScanResults.submittedContent)) {
        alert("Run a scan before downloading the report!");
        return;
    }

    const percentageText = document.getElementById("percentageCircle")?.innerText || "0%";
    const rawPercentage = parseFloat(percentageText.replace("%", "")) || (lastScanResults ? lastScanResults.percentage : 0);

    const payload = {
        username: String(activeCurrentUser),
        documentName: (lastScanResults && lastScanResults.documentName) ? lastScanResults.documentName : "Plagiarism Scan",
        percentage: rawPercentage,
        jaccardScore: lastScanResults ? (lastScanResults.jaccardScore || 0) : 0,
        cosineScore: lastScanResults ? (lastScanResults.cosineScore || 0) : 0,
        status: lastScanResults ? (lastScanResults.status || "Verified") : "Verified",
        submittedContent: submittedText || (lastScanResults ? lastScanResults.submittedContent : ""),
        matchedResourceContent: lastScanResults ? (lastScanResults.matchedResourceContent || "") : "",
        citations: lastScanResults ? (lastScanResults.citations || []) : []
    };

    try {
        const response = await fetch(`${API_BASE_URL}/export-pdf`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            throw new Error("PDF generation failed on server side.");
        }

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'Plagiarism_Audit_Report.pdf';
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);
    } catch (e) {
        alert("Failed to download PDF report. Ensure backend cluster is online.");
        console.error("PDF Download Error:", e);
    }
}

// ==========================================
// 📁 8. HISTORY LOG SYNCING
// ==========================================
function syncHistoryLogs() {
    const tableBody = document.getElementById("history-table-body");
    if (!tableBody) return;

    fetch(`${API_BASE_URL}/history?username=${encodeURIComponent(activeCurrentUser)}`, {
        method: "GET",
        headers: getAuthHeaders()
    })
        .then(res => {
            if (!res.ok) throw new Error("Failed to fetch history from server");
            return res.json();
        })
        .then(data => {
            tableBody.innerHTML = "";

            if (!data || data.length === 0) {
                tableBody.innerHTML = `<tr><td colspan="4" style="text-align:center; padding:20px; color:var(--text-muted);">No records in database for this entity.</td></tr>`;
                return;
            }

            data.forEach(item => {
                const dateObj = new Date(item.timestamp);
                const formattedDate = isNaN(dateObj.getTime())
                    ? "N/A"
                    : dateObj.toLocaleDateString() + " " + dateObj.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});

                const percentageDisplay = item.matchPercentageLabel || (item.matchPercentage !== undefined ? item.matchPercentage + "%" : "0%");

                const isVerified = item.status === "Verified";
                const badgeColor = isVerified ? "background:#dcfce7; color:#166534;" : "background:#fee2e2; color:#991b1b;";
                const textClass = isVerified ? "color:var(--accent-green);" : "color:var(--accent-red);";

                tableBody.innerHTML += `
                    <tr style="border-bottom: 1px solid var(--border-color);">
                        <td style="padding: 12px 10px;">${formattedDate}</td>
                        <td>${item.documentName || "Unnamed Scan"}</td>
                        <td><span style="${textClass} font-weight: 600;">${percentageDisplay}</span></td>
                        <td><span style="${badgeColor} padding: 2px 8px; border-radius: 4px; font-size: 12px;">${item.status || "Unknown"}</span></td>
                    </tr>
                `;
            });
        })
        .catch(err => {
            console.error("Historical log synchronizer encountered error: ", err);
        });
}

/* ==========================================
   SCREEN MANAGEMENT
   ========================================== */

function showElement(element) {
    if (element) {
        element.style.display = '';
    }
}

function hideElement(element) {
    if (element) {
        element.style.display = "none";
    }
}

function showElement(element, displayType = "block") {
    if (element) {
        element.style.display = displayType;
        element.style.visibility = "visible";
        element.style.opacity = "1";
    }
}

function dismissSplashAndShowLogin(event) {
    if (event) event.preventDefault();

    const splash = document.getElementById("splash-screen");
    const login = document.getElementById("login-screen");
    const navbar = document.querySelector(".top-navbar");
    const workspace = document.getElementById("workspace-screen");

    hideElement(navbar);
    hideElement(workspace);

    hideElement(splash);
    showElement(login);
}

function showDashboard() {
    const splash = document.getElementById("splash-screen");
    const login = document.getElementById("login-screen");
    const navbar = document.querySelector(".top-navbar");
    const workspace = document.getElementById("workspace-screen");

    hideElement(splash);
    hideElement(login);

    showElement(navbar);
    showElement(workspace);

    if (typeof updateUserDisplay === "function") {
        updateUserDisplay();
    }
}

function bypassAuthAndStart(event) {
    if (event) event.preventDefault();

    activeCurrentUser = "Guest Explorer";
    activeUserRole = "Guest";

    showDashboard();
}

function handleLogin(event) {
    if (event) event.preventDefault();

    const email = document.getElementById("login-email");
    const password = document.getElementById("login-password");

    if (!email || !password) {
        alert("Login fields could not be found. Check their IDs in index.html.");
        return;
    }

    if (!email.value.trim() || !password.value.trim()) {
        alert("Please enter your email and password.");
        return;
    }

    activeCurrentUser = email.value.trim().split("@")[0];
    activeUserRole = "Student";

    localStorage.setItem("activeUser", activeCurrentUser);

    showDashboard();
}

/* ==========================================
   1. SPLASH → LOGIN
   ========================================== */

function dismissSplashAndShowLogin() {

    const splash = document.getElementById('splash-screen');
    const login = document.getElementById('login-screen');

    const navbar = document.querySelector('.top-navbar');
    const workspace = document.getElementById('workspace-screen');

    /* Hide dashboard */
    hideElement(workspace);

    /* Hide navbar */
    hideElement(navbar);

    /* Hide splash */
    if (splash) {
        splash.classList.add('splash-hidden');
    }

    /* Show login */
    if (login) {
        login.classList.add('login-visible');
    }
}


/* ==========================================
   2. LOGIN / GUEST → DASHBOARD
   ========================================== */

function showDashboard() {

    const splash = document.getElementById('splash-screen');
    const login = document.getElementById('login-screen');
    const navbar = document.querySelector('.top-navbar');
    const workspace = document.getElementById('workspace-screen');

    /* Hide splash completely */
    if (splash) {
        splash.classList.add('splash-hidden');
    }

    /* Hide login */
    if (login) {
        login.classList.remove('login-visible');
        login.style.display = 'none';
    }

    /* Show navbar */
    if (navbar) {
        navbar.style.display = 'block';
    }

    /* Show workspace */
    if (workspace) {
        workspace.style.display = 'block';
    }
}


/* ==========================================
   3. GUEST ACCESS
   ========================================== */

function bypassAuthAndStart(event) {

    if (event) {
        event.preventDefault();
    }

    showDashboard();
}


/* ==========================================
   4. LOGIN
   ========================================== */

function handleLogin(event) {

    if (event) {
        event.preventDefault();
    }

    const email = document.getElementById('login-email');
    const password = document.getElementById('login-password');

    /* Basic validation */
    if (!email || !password) {
        return;
    }

    if (!email.value.trim() || !password.value.trim()) {
        alert('Please enter your email and password.');
        return;
    }

    /* Login successful */
    showDashboard();
}