<script>
    import {goto} from "$app/navigation";
    import {page} from "$app/stores";
    import DRepDetails from "../../../components/drep/DRepDetails.svelte";

    export let data;

    $: activeUrl = $page.url.searchParams.get('page')
    let pages = [];

    $:{
        pages.forEach((page) => {
            let splitUrl = page.href.split('?');
            let queryString = splitUrl.slice(1).join('?');
            const hrefParams = new URLSearchParams(queryString);
            let hrefValue = hrefParams.get('page');
            if (hrefValue === activeUrl) {
                page.active = true
            } else {
                page.active = false
            }
        })
        pages = pages
    }

    const previous = () => {
        let currentPage = parseInt(data.page);
        let prevPage = currentPage - 1;
        if (prevPage <= 0)
            prevPage = 1;
        goto(`/governance/drep-registrations?page=${prevPage}&count=${data.count}`)
    };
    const next = () => {
        let currentPage = parseInt(data.page);
        let nextPage = currentPage + 1;

        if (data.registrations.length == 0)
            nextPage = currentPage;

        goto(`/governance/drep-registrations?page=${nextPage}&count=${data.count}`)
    };

    if (!data.registrations)
        data.registrations = []

    let showDetails = false;
    let selectedDRep = {};

    const toggleDRep = (drep) => {
        selectedDRep = drep;
        showDetails = !showDetails;
        console.log(JSON.stringify(drep))
    };

    function closeDetails() {
        showDetails = false;
    }
</script>


<section class="container mx-auto text-sm">
    <h2 class="text-xl font-bold text-center text-gray-500 mb-4">DRep Registrations</h2>
    <div class="flex flex-wrap justify-between mt-4 mb-2">
        <button
           class="px-4 py-2 text-blue-500 font-medium rounded-md bg-gray-100 hover:bg-gray-200 transition-colors"
           role="button" on:click={previous}>&lt; Previous</button>
        <button
           class="px-4 py-2 text-blue-500 font-medium rounded-md bg-gray-100 hover:bg-gray-200 transition-colors"
           role="button" on:click={next}>Next &gt;</button>
    </div>
    <div class="overflow-x-auto">
        <table class="w-full bg-white border border-gray-300">
            <thead>
            <tr>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">DRep Id</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Type</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Block</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Txn Hash</th>
            </tr>
            </thead>
            <tbody>
            <!-- Iterate over stake registrations data -->
            {#each data.registrations as registration, index}
                <tr class="{index % 2 === 0 ? 'bg-gray-50' : 'bg-white'}">
                    <td class="py-2 px-4">
                        <a href="#" class="text-blue-500">
                            <span class="ml-2" on:click={() => {toggleDRep(registration); }}>{registration.drep_id}</span>
                        </a>
                    </td>
                    <td class="py-2 px-4">{registration.cred_type}</td>
                    <td class="py-2 px-4"><a href="/blocks/{registration.block_number}" class="text-blue-500">{registration.block_number}</a></td>
                    <td class="py-2 px-4"><a href="/transactions/{registration.tx_hash}" class="text-blue-500">{registration.tx_hash}</a></td>
                </tr>
            {/each}
            </tbody>
        </table>
    </div>
    <div class="flex flex-wrap justify-between mt-2 mb-2">
        <button
           class="px-4 py-2 text-blue-500 font-medium rounded-md bg-gray-100 hover:bg-gray-200 transition-colors"
           role="button" on:click={previous}>&lt; Previous</button>
        <button
           class="px-4 py-2 text-blue-500 font-medium rounded-md bg-gray-100 hover:bg-gray-200 transition-colors"
           role="button" on:click={next}>Next &gt;</button>
    </div>
</section>

<DRepDetails drep={selectedDRep} show={showDetails} on:closeDetails={closeDetails}/>
