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

    function closeDropdown() {
        const detailsElement = document.querySelector("details");
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
                <details>
                    <summary>
                        Certificates
                    </summary>
                    <ul class="p-2 bg-base-100">
                        <li><a href="/certificates/stakekey-registrations" on:click={closeDropdown}>Stake Registration</a></li>
                        <li><a href="/certificates/stakekey-deregistrations" on:click={closeDropdown}>Stake DeRegistration</a></li>
                        <li><a href="/certificates/pool-registrations" on:click={closeDropdown}>Pool Registration</a></li>
                        <li><a href="/certificates/pool-retirements" on:click={closeDropdown}>Pool Retirements</a></li>
                        <li><a href="/certificates/delegations" on:click={closeDropdown}>Delegations</a></li>
                    </ul>
                </details>
            </li>
            <li><a href="/blocks">Blocks</a></li>
            <li><a href="/transactions">Transactions</a></li>
        </ul>
    </div>
</div>


