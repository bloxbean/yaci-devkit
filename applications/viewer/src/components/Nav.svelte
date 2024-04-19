<script>
    function checkAndCloseDropDown(e){

        let targetEl = e.currentTarget;
        if(targetEl && targetEl.matches(':focus')){
            setTimeout(function(){
                targetEl.blur();
            }, 0);
        }
    }

    function openMenu() {
        open = true;
    }

    function closeMenu() {
        open = false;
    }

    let open = false;
    const closeCertMenu = () => {
        const elem = document.activeElement;
        if(elem){
            elem?.blur();
        }
    };

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
</script>
<div class="navbar bg-base-100">
    <div class="flex-1">
        <a class="btn btn-ghost normal-case text-xl" href="/">Yaci Viewer</a>
    </div>
    <div class="flex-none">
        <ul class="menu menu-horizontal px-1">
            <li><a href="/">Home</a></li>
            <li>
                <details id="certificate-menu">
                    <summary>
                        Certificates
                    </summary>
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
                <details id="governance-menu">
                    <summary>
                        Governance
                    </summary>
                    <ul class="p-2 bg-base-100">
                        <li><a href="/governance/govactions" on:click={closeDropdownGovernanceMenu}>Proposals</a></li>
                        <li><a href="/governance/votes" on:click={closeDropdownGovernanceMenu}>Votes</a></li>
                    </ul>
                </details>
            </li>
            <li><a href="/blocks">Blocks</a></li>
            <li><a href="/transactions">Transactions</a></li>

        </ul>
    </div>
</div>


