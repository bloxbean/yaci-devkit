<script>
	import '../app.postcss';
	import Nav from "../components/Nav.svelte";
    import {Footer, FooterCopyright, FooterLinkGroup, FooterLink, Spinner} from 'flowbite-svelte'
    import {navigating} from "$app/stores";
</script>

<Nav></Nav>
<!--<nav>-->
<!--	<a href="/">Home</a>-->
<!--	<a href="/transactions">Transactions</a>-->
<!--	<a href="/blocks">Blocks</a>-->
<!--</nav>-->
<div>
    {#if $navigating}
        <div class="text-center"><Spinner/></div>
    {:else}
        <slot />
    {/if}
</div>

<Footer>
<!--    <FooterCopyright href="/" by="BloxBean" target="BloxBean" year={2022} />-->

</Footer>
