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

	initTheme();
	initUnreadPolling();
})();
