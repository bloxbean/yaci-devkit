/* Shell state: the mobile drawer, the collapsed sidebar and the search palette. */
class ShellUi {
	drawerOpen = $state(false);
	collapsed = $state(false);
	paletteOpen = $state(false);
	paletteQuery = $state('');

	openPalette(query = '') {
		this.paletteQuery = query;
		this.paletteOpen = true;
	}

	toggleCollapsed() {
		this.collapsed = !this.collapsed;
		try {
			localStorage.setItem('viewer:sidebar', this.collapsed ? 'collapsed' : 'open');
		} catch {
			/* storage unavailable */
		}
	}

	restore() {
		try {
			this.collapsed = localStorage.getItem('viewer:sidebar') === 'collapsed';
		} catch {
			/* storage unavailable */
		}
	}
}

export const shell = new ShellUi();
