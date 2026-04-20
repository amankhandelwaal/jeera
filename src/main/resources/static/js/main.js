(function () {
	const THEME_KEY = "jeera-theme-preference";

	function resolveTheme(preferredTheme) {
		if (preferredTheme === "dark" || preferredTheme === "light") {
			return preferredTheme;
		}
		return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
	}

	function applyTheme(theme) {
		document.documentElement.setAttribute("data-theme", theme);
		const toggle = document.getElementById("themeToggle");
		if (toggle) {
			const isDark = theme === "dark";
			toggle.setAttribute("aria-pressed", String(isDark));
			toggle.setAttribute("title", isDark ? "Switch to light mode" : "Switch to dark mode");
			toggle.querySelector("[data-theme-label]").textContent = isDark ? "Light" : "Dark";
			toggle.querySelector("[data-theme-icon]").textContent = "◐";
		}
	}

	function initTheme() {
		const storedPreference = localStorage.getItem(THEME_KEY);
		applyTheme(resolveTheme(storedPreference));

		const toggle = document.getElementById("themeToggle");
		if (!toggle) {
			return;
		}

		toggle.addEventListener("click", function () {
			const currentTheme = document.documentElement.getAttribute("data-theme") === "dark" ? "dark" : "light";
			const nextTheme = currentTheme === "dark" ? "light" : "dark";
			localStorage.setItem(THEME_KEY, nextTheme);
			applyTheme(nextTheme);
		});
	}

	function initUnreadPolling() {
		const badge = document.getElementById("unread-badge");
		if (!badge) {
			return;
		}

		async function fetchUnreadCount() {
			try {
				const response = await fetch("/notifications/unread-count", {
					headers: {
						"X-Requested-With": "XMLHttpRequest"
					}
				});

				if (!response.ok) {
					return;
				}

				const count = await response.text();
				const parsed = Number(count);
				const unread = Number.isFinite(parsed) ? parsed : 0;

				badge.textContent = String(unread);
				badge.style.display = unread > 0 ? "inline-block" : "none";
			} catch (error) {
				// Silent failure keeps UI usable if polling endpoint is temporarily unavailable.
			}
		}

		fetchUnreadCount();
		window.setInterval(fetchUnreadCount, 30000);
	}

	function initAdminProjectDeleteModal() {
		const modal = document.getElementById("adminProjectDeleteModal");
		if (!modal) {
			return;
		}

		const nameSlot = document.getElementById("deleteModalProjectName");
		const safeBlock = document.getElementById("deleteModalSafeBlock");
		const forceBlock = document.getElementById("deleteModalForceBlock");
		const impactText = document.getElementById("deleteModalImpactText");
		const confirmInput = document.getElementById("deleteModalConfirmProjectName");
		const reasonInput = document.getElementById("deleteModalReason");
		const forceFlag = document.getElementById("deleteModalForceFlag");
		const form = document.getElementById("adminProjectDeleteModalForm");
		const submitBtn = document.getElementById("deleteModalSubmitBtn");
		const baseAction = form ? (form.getAttribute("data-base-action") || form.getAttribute("action") || "") : "";

		modal.addEventListener("show.bs.modal", function (event) {
			if (!form) {
				return;
			}

			const trigger = event.relatedTarget;
			if (!trigger) {
				return;
			}

			const projectId = String(trigger.getAttribute("data-project-id") || "");
			const projectName = String(trigger.getAttribute("data-project-name") || "project");
			const totalIssues = Number(trigger.getAttribute("data-total-issues") || "0");
			const unresolvedIssues = Number(trigger.getAttribute("data-unresolved-issues") || "0");
			const requiresForceDelete = unresolvedIssues > 0;

			nameSlot.textContent = projectName;

			form.setAttribute("action", baseAction.replace(/\/0\/delete$/, "/" + projectId + "/delete"));

			forceFlag.value = requiresForceDelete ? "true" : "false";

			safeBlock.classList.toggle("d-none", requiresForceDelete);
			forceBlock.classList.toggle("d-none", !requiresForceDelete);

			confirmInput.required = requiresForceDelete;
			reasonInput.required = requiresForceDelete;

			if (requiresForceDelete) {
				impactText.textContent =
					"Force delete will permanently remove " + totalIssues
					+ " issue(s), comments, activity logs, notifications, and memberships.";
				submitBtn.textContent = "Force Delete Project";
				submitBtn.classList.remove("btn-outline-danger");
				submitBtn.classList.add("btn-danger");
			} else {
				confirmInput.value = "";
				reasonInput.value = "";
				submitBtn.textContent = "Delete Project";
				submitBtn.classList.remove("btn-outline-danger");
				submitBtn.classList.add("btn-danger");
			}
		});

		modal.addEventListener("hidden.bs.modal", function () {
			if (!form) {
				return;
			}
			form.setAttribute("action", baseAction);
			confirmInput.value = "";
			reasonInput.value = "";
			forceFlag.value = "false";
		});
	}

	initTheme();
	initUnreadPolling();
	initAdminProjectDeleteModal();
})();
