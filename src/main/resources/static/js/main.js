(function () {
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
})();
