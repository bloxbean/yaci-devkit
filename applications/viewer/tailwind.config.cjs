const config = {
	content: ['./src/**/*.{html,js,svelte,ts}'],

	theme: {
		extend: {}
	},

	plugins: [require('daisyui')],
	darkMode: 'class',
	daisyui: {
		themes: [
			{
				yaci: {
					...require('daisyui/src/theming/themes')['light']
				},
				'yaci-dark': {
					...require('daisyui/src/theming/themes')['dark']
				}
			}
		]
	}
};

module.exports = config;
