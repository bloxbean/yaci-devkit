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
                        <li><a href="/certificates/stakekey-registrations">Stake Registration</a></li>
                        <li><a href="/certificates/stakekey-deregistrations">Stake DeRegistration</a></li>
                        <li><a href="/certificates/pool-registrations">Pool Registration</a></li>
                        <li><a href="/certificates/pool-retirements">Pool Retirements</a></li>
                    </ul>
                </details>
            </li>
            <li><a href="/blocks">Blocks</a></li>
            <li><a href="/transactions">Transactions</a></li>
        </ul>
    </div>
</div>


