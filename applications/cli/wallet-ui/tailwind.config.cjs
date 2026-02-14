const config = {
	content: [
		'./src/**/*.{html,js,svelte,ts}',
		'./index.html'
	],

	theme: {
		extend: {}
	},

	plugins: [
        require('daisyui')
	],
	darkMode: 'class',
};

module.exports = config;
