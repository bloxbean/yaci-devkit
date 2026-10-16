/* Colours come from CSS variables in app.postcss, so one class works in both themes. */
const token = (name) => `rgb(var(--c-${name}) / <alpha-value>)`;

const config = {
	content: ['./src/**/*.{html,js,svelte,ts}'],
	darkMode: ['selector', '[data-theme="yaci-dark"]'],

	theme: {
		extend: {
			colors: {
				bg: token('bg'),
				'bg-2': token('bg-2'),
				surface: token('surface'),
				'surface-2': token('surface-2'),
				line: token('line'),
				ink: token('ink'),
				muted: token('muted'),
				faint: token('faint'),
				brand: token('brand'),
				'brand-2': token('brand-2'),
				amber: token('amber'),
				blue: token('blue'),
				cyan: token('cyan'),
				mint: token('mint'),
				violet: token('violet'),
				pink: token('pink'),
				danger: token('danger')
			},
			fontFamily: {
				display: ['"Space Grotesk Variable"', 'ui-sans-serif', 'system-ui', 'sans-serif'],
				sans: ['"Inter Variable"', 'ui-sans-serif', 'system-ui', '-apple-system', '"Segoe UI"', 'sans-serif'],
				mono: ['"JetBrains Mono Variable"', 'ui-monospace', 'SFMono-Regular', 'Menlo', 'monospace']
			},
			borderRadius: {
				xl: '14px',
				'2xl': '18px'
			},
			boxShadow: {
				card: '0 1px 0 0 rgb(var(--c-ink) / 0.03), 0 18px 40px -28px rgb(0 0 0 / 0.45)',
				glow: '0 0 0 1px rgb(var(--c-brand) / 0.35), 0 10px 30px -10px rgb(var(--c-brand) / 0.45)'
			},
			backgroundImage: {
				grad: 'var(--grad)'
			},
			keyframes: {
				'slide-in': {
					from: { opacity: '0', transform: 'translateX(-18px) scale(0.96)' },
					to: { opacity: '1', transform: 'none' }
				},
				'fade-up': {
					from: { opacity: '0', transform: 'translateY(8px)' },
					to: { opacity: '1', transform: 'none' }
				},
				flash: {
					from: { backgroundColor: 'rgb(var(--c-brand) / 0.16)' },
					to: { backgroundColor: 'transparent' }
				},
				shimmer: {
					to: { backgroundPosition: '-200% 0' }
				},
				ping: {
					'75%, 100%': { transform: 'scale(2.2)', opacity: '0' }
				}
			},
			animation: {
				'slide-in': 'slide-in 0.55s cubic-bezier(0.2, 0.8, 0.2, 1) both',
				'fade-up': 'fade-up 0.4s cubic-bezier(0.2, 0.8, 0.2, 1) both',
				flash: 'flash 1.8s ease-out',
				shimmer: 'shimmer 1.6s linear infinite',
				ping: 'ping 1.6s cubic-bezier(0, 0, 0.2, 1) infinite'
			}
		}
	},

	plugins: []
};

module.exports = config;
