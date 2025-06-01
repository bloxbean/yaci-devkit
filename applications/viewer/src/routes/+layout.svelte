<script>
    import '../app.postcss';
    import {navigating} from "$app/stores";
    import Nav from "../components/Nav.svelte";
    import { env } from '$env/dynamic/public';
</script>

<!--<Nav></Nav>-->
<div class="flex flex-col min-h-screen">
    <!-- Main -->
    <main class="container mx-auto flex-grow px-4">
        <Nav></Nav>
        
        {#if env.PUBLIC_MAINTENANCE_MSG}
            <div class="alert alert-warning mb-4">
                <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-6 w-6" fill="none" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
                <span>{env.PUBLIC_MAINTENANCE_MSG}</span>
            </div>
        {/if}

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
