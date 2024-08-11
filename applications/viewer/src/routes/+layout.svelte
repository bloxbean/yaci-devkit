<script>
    import '../app.postcss';
    import {navigating} from "$app/stores";
    import Nav from "../components/Nav.svelte";
</script>
<body>
<!--<Nav></Nav>-->
<div class="flex flex-col min-h-screen">
    <!-- Main -->
    <main class="container mx-auto flex-grow px-4">
        <Nav></Nav>
        {#if $navigating}
            <section class="flex justify-center h-screen">
                <span class="loading loading-ring loading-lg mx-auto"></span>
            </section>
        {:else}
            <slot />
        {/if}
    </main>

    <!-- Footer -->
    <footer class="bg-gray-900 py-4 text-center bottom-0 left-0 w-full">
        <div class="container mx-auto">
            <p class="text-gray-300">
                Open-source with ❤️ by <a href="https://github.com/bloxbean/yaci-viewer" target="_blank"
                                          rel="noopener noreferrer">BloxBean</a>
            </p>
        </div>
    </footer>
</div>
</body>
