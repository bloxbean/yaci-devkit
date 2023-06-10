const config = {
	content: [
		'./src/**/*.{html,js,svelte,ts}'
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
