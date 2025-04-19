<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import { page } from "$app/stores";

    function closeMenu(menuId: string) {
        if (typeof document !== 'undefined') {
            const detailsElement = document.querySelector(`#${menuId}`);
            if (detailsElement) {
                detailsElement.removeAttribute("open");
            }
        }
    }

    // --- Click Outside Logic ---
    let openMenuId: string | null = null;

    function handleGlobalClick(event: MouseEvent) {
        if (openMenuId && typeof document !== 'undefined') {
            const menuElement = document.getElementById(openMenuId);
            const target = event.target as Node;
            // Check if the click is outside the open menu
            if (menuElement && !menuElement.contains(target)) {
                closeMenu(openMenuId); // Close the currently open menu
                openMenuId = null; // Reset the open menu tracker
            }
        }
    }

    function handleMenuToggle(event: MouseEvent) {
        if (typeof document === 'undefined') return;

        const target = event.target as HTMLElement;
        const linkClicked = target.closest('a');
        const detailsParent = target.closest('details');

        if (linkClicked && detailsParent) {
            const menuId = detailsParent.id;
            closeMenu(menuId);
            if (openMenuId === menuId) {
                openMenuId = null;
            }
        } else if (detailsParent) {
            const menuId = detailsParent.id;
            if (detailsParent.hasAttribute('open')) {
                if (openMenuId === menuId) {
                    openMenuId = null;
                }
            } else {
                if (openMenuId && openMenuId !== menuId) {
                    closeMenu(openMenuId);
                }
                openMenuId = menuId;
            }
        }
    }

    function closeDropdownRewardsMenu() {
        // Close the rewards menu if it exists
        const detailsElement = document.querySelector("#rewards-menu");
        if (detailsElement) {
            detailsElement.removeAttribute("open");
        }
    }

    onMount(() => {
        document.addEventListener('click', handleGlobalClick, true); // Use capture phase
        document.getElementById('desktop-menu-list')?.addEventListener('click', handleMenuToggle);
        document.getElementById('mobile-menu-list')?.addEventListener('click', handleMenuToggle);
    });

    onDestroy(() => {
        if (typeof document !== 'undefined') {
            document.removeEventListener('click', handleGlobalClick, true);
            document.getElementById('desktop-menu-list')?.removeEventListener('click', handleMenuToggle);
            document.getElementById('mobile-menu-list')?.removeEventListener('click', handleMenuToggle);
        }
    });
</script>

<style>
    /* Add styles for dropdown positioning and z-index */
    :global(.dropdown-content) {
        position: absolute;
        z-index: 50;
        background-color: white;
        border: 1px solid #e5e7eb;
        border-radius: 0.375rem;
        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
    }

    /* Ensure the nav container is above the main content but below dropdowns */
    nav {
        position: relative;
        z-index: 40;
        background-color: white;
    }

    /* Style for mobile menu to ensure it's above everything */
    :global(.mobile-menu) {
        z-index: 60;
    }

    /* Ensure details elements in nav show above content */
    details {
        position: relative;
        z-index: 45;
    }
</style>

<div class="navbar bg-base-100">
    <div class="flex-1">
        <a class="btn btn-ghost normal-case text-xl" href="/">Yaci Viewer</a>
    </div>
    <div class="flex-none">
        <!-- Mobile Menu Button -->
        <div class="dropdown dropdown-end lg:hidden">
            <label tabindex="0" class="btn btn-ghost lg:hidden">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24"
                     stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16m-7 6h7"/>
                </svg>
            </label>
            <ul tabindex="0" id="mobile-menu-list" class="menu menu-compact dropdown-content mt-3 p-2 shadow bg-base-100 rounded-box w-52">
                <li><a href="/">Home</a></li>
                <li>
                    <details id="mobile-certificate-menu">
                        <summary>Certificates</summary>
                        <ul class="p-2 bg-base-100">
                            <li><a href="/certificates/stakekey-registrations">Stake Registration</a></li>
                            <li><a href="/certificates/stakekey-deregistrations">Stake DeRegistration</a></li>
                            <li><a href="/certificates/pool-registrations">Pool Registration</a></li>
                            <li><a href="/certificates/pool-retirements">Pool Retirements</a></li>
                            <li><a href="/certificates/delegations">Delegations</a></li>
                        </ul>
                    </details>
                </li>
                <li>
                    <details id="mobile-governance-menu">
                        <summary>Gov Certificates</summary>
                        <ul class="p-2 bg-base-100">
                            <li><a href="/governance/drep-registrations">DRep Registrations</a></li>
                            <li><a href="/governance/drep-updates">DRep Updates</a></li>
                            <li><a href="/governance/drep-deregistrations">DRep DeRegistrations</a></li>
                            <li><a href="/governance/govactions">Proposals</a></li>
                            <li><a href="/governance/votes">Votes</a></li>
                        </ul>
                    </details>
                </li>
                <li>
                    <details id="mobile-governance-state-menu">
                        <summary>Governance State</summary>
                        <ul class="p-2 bg-base-100">
                            <li><a href="/governance/proposals">Proposals</a></li>
                            <li><a href="/governance/dreps">Dreps</a></li>
                        </ul>
                    </details>
                </li>
                <li>
                    <details id="mobile-adapot-menu">
                        <summary>AdaPot</summary>
                        <ul class="p-2 bg-base-100">
                            <li><a href="/adapot/current">Current Details</a></li>
                            <li><a href="/adapot/list">History</a></li>
                        </ul>
                    </details>
                </li>
                <li>
                    <details id="mobile-rewards-menu">
                        <summary>Pools and Rewards</summary>
                        <ul class="p-2 bg-base-100">
                            <li><a href="/pools/search">Pool Search</a></li>
                            <li><a href="/rewards">Rewards Explorer</a></li>
                        </ul>
                    </details>
                </li>
                <li><a href="/blocks">Blocks</a></li>
                <li><a href="/transactions">Transactions</a></li>
            </ul>
        </div>
        <!-- Desktop Menu -->
        <ul id="desktop-menu-list" class="menu menu-horizontal px-1 hidden lg:flex">
            <li><a href="/">Home</a></li>
            <li>
                <details id="certificate-menu">
                    <summary>Certificates</summary>
                    <ul class="p-2 bg-base-100">
                        <li><a href="/certificates/stakekey-registrations">Stake Registration</a></li>
                        <li><a href="/certificates/stakekey-deregistrations">Stake DeRegistration</a></li>
                        <li><a href="/certificates/pool-registrations">Pool Registration</a></li>
                        <li><a href="/certificates/pool-retirements">Pool Retirements</a></li>
                        <li><a href="/certificates/delegations">Delegations</a></li>
                    </ul>
                </details>
            </li>
            <li>
                <details id="governance-menu">
                    <summary>Gov Certificates</summary>
                    <ul class="p-2 bg-base-100">
                        <li><a href="/governance/drep-registrations">DRep Registrations</a></li>
                        <li><a href="/governance/drep-updates">DRep Updates</a></li>
                        <li><a href="/governance/drep-deregistrations">DRep DeRegistrations</a></li>
                        <li><a href="/governance/govactions">Proposals</a></li>
                        <li><a href="/governance/votes">Votes</a></li>
                    </ul>
                </details>
            </li>
            <li>
                <details id="governance-state-menu">
                    <summary>Governance State</summary>
                    <ul class="p-2 bg-base-100">
                        <li><a href="/governance/proposals">Proposals</a></li>
                        <li><a href="/governance/dreps">Dreps</a></li>
                    </ul>
                </details>
            </li>
            <li>
                <details id="adapot-menu">
                    <summary>AdaPot</summary>
                    <ul class="p-2 bg-base-100">
                        <li><a href="/adapot/current">Current Details</a></li>
                        <li><a href="/adapot/list">History</a></li>
                    </ul>
                </details>
            </li>
            <li>
                <details id="rewards-menu">
                    <summary>Pools and Rewards</summary>
                    <ul class="p-2 bg-base-100">
                        <li><a href="/pools/search" on:click={() => closeMenu('rewards-menu')}>Pool Search</a></li>
                        <li><a href="/rewards" on:click={() => closeMenu('rewards-menu')}>Rewards Explorer</a></li>
                    </ul>
                </details>
            </li>
            <li><a href="/blocks">Blocks</a></li>
            <li><a href="/transactions">Transactions</a></li>
        </ul>
    </div>
</div>
