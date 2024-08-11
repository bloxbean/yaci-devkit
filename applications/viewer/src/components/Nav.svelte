<script>
    function closeCertMenu() {
        const elem = document.activeElement;
        if (elem) {
            elem.blur();
        }
    }

    function closeDropdownCertificateMenu() {
        const detailsElement = document.querySelector("#certificate-menu");
        if (detailsElement) {
            detailsElement.removeAttribute("open");
        }
    }

    function closeDropdownGovernanceMenu() {
        const detailsElement = document.querySelector("#governance-menu");
        if (detailsElement) {
            detailsElement.removeAttribute("open");
        }
    }

    function closeMenu(menuId) {
        const detailsElement = document.querySelector(`#${menuId}`);
        if (detailsElement) {
            detailsElement.removeAttribute("open");
        }
    }
</script>

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
            <ul tabindex="0" class="menu menu-compact dropdown-content mt-3 p-2 shadow bg-base-100 rounded-box w-52">
                <li><a href="/">Home</a></li>
                <li>
                    <details id="mobile-certificate-menu">
                        <summary>Certificates</summary>
                        <ul class="p-2 bg-base-100">
                            <li><a href="/certificates/stakekey-registrations" on:click={closeDropdownCertificateMenu}>Stake Registration</a></li>
                            <li><a href="/certificates/stakekey-deregistrations" on:click={closeDropdownCertificateMenu}>Stake DeRegistration</a></li>
                            <li><a href="/certificates/pool-registrations" on:click={closeDropdownCertificateMenu}>Pool Registration</a></li>
                            <li><a href="/certificates/pool-retirements" on:click={closeDropdownCertificateMenu}>Pool Retirements</a></li>
                            <li><a href="/certificates/delegations" on:click={closeDropdownCertificateMenu}>Delegations</a></li>
                        </ul>
                    </details>
                </li>
                <li>
                    <details id="mobile-governance-menu">
                        <summary>Governance</summary>
                        <ul class="p-2 bg-base-100">
                            <li><a href="/governance/drep-registrations" on:click={closeDropdownGovernanceMenu}>DRep Registrations</a></li>
                            <li><a href="/governance/drep-updates" on:click={closeDropdownGovernanceMenu}>DRep Updates</a></li>
                            <li><a href="/governance/drep-deregistrations" on:click={closeDropdownGovernanceMenu}>DRep DeRegistrations</a></li>
                            <li><a href="/governance/govactions" on:click={closeDropdownGovernanceMenu}>Proposals</a></li>
                            <li><a href="/governance/votes" on:click={closeDropdownGovernanceMenu}>Votes</a></li>
                        </ul>
                    </details>
                </li>
                <li><a href="/blocks">Blocks</a></li>
                <li><a href="/transactions">Transactions</a></li>
            </ul>
        </div>
        <!-- Desktop Menu -->
        <ul class="menu menu-horizontal px-1 hidden lg:flex">
            <li><a href="/">Home</a></li>
            <li>
                <details id="certificate-menu">
                    <summary>Certificates</summary>
                    <ul class="p-2 bg-base-100">
                        <li><a href="/certificates/stakekey-registrations" on:click={() => closeMenu('certificate-menu')}>Stake Registration</a></li>
                        <li><a href="/certificates/stakekey-deregistrations" on:click={() => closeMenu('certificate-menu')}>Stake DeRegistration</a></li>
                        <li><a href="/certificates/pool-registrations" on:click={() => closeMenu('certificate-menu')}>Pool Registration</a></li>
                        <li><a href="/certificates/pool-retirements" on:click={() => closeMenu('certificate-menu')}>Pool Retirements</a></li>
                        <li><a href="/certificates/delegations" on:click={() => closeMenu('certificate-menu')}>Delegations</a></li>
                    </ul>
                </details>
            </li>
            <li>
                <details id="governance-menu">
                    <summary>Governance</summary>
                    <ul class="p-2 bg-base-100">
                        <li><a href="/governance/drep-registrations" on:click={() => closeMenu('governance-menu')}>DRep Registrations</a></li>
                        <li><a href="/governance/drep-updates" on:click={() => closeMenu('governance-menu')}>DRep Updates</a></li>
                        <li><a href="/governance/drep-deregistrations" on:click={() => closeMenu('governance-menu')}>DRep DeRegistrations</a></li>
                        <li><a href="/governance/govactions" on:click={() => closeMenu('governance-menu')}>Proposals</a></li>
                        <li><a href="/governance/votes" on:click={() => closeMenu('governance-menu')}>Votes</a></li>
                    </ul>
                </details>
            </li>
            <li><a href="/blocks">Blocks</a></li>
            <li><a href="/transactions">Transactions</a></li>
        </ul>
    </div>
</div>
