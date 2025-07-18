import App from './App.svelte'

console.log('main.js loaded');
console.log('App component:', App);
console.log('Target element:', document.getElementById('app'));

const app = new App({
  target: document.getElementById('app')
})

console.log('App initialized:', app);

export default app