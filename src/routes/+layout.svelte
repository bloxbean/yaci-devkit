<script>
	import '../app.postcss';
	import Nav from "../components/Nav.svelte";
    import { Footer, FooterCopyright, FooterLinkGroup, FooterLink } from 'flowbite-svelte'
</script>

<Nav></Nav>
<!--<nav>-->
<!--	<a href="/">Home</a>-->
<!--	<a href="/transactions">Transactions</a>-->
<!--	<a href="/blocks">Blocks</a>-->
<!--</nav>-->
<div>
<slot />
</div>

<Footer>
<!--    <FooterCopyright href="/" by="BloxBean" target="BloxBean" year={2022} />-->

</Footer>
