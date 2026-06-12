<script lang="ts">
    import {goto} from "$app/navigation";
    import {page} from "$app/stores";
    import {truncate, getDate} from "$lib/util";

    export let data;

    // Define interface for Stake De-registration data
    interface StakeDeRegistration {
        address: string;
        block_number: number;
        block_time: number; 
        tx_hash: string;
    }

    // Define a type for the page objects (if used, otherwise remove)
    interface PageLink {
        href: string;
        active: boolean;
    }

    $: activeUrl = $page.url.searchParams.get('page');
    let pages: PageLink[] = []; // Explicitly type pages

    $:{
        pages.forEach((pageLink) => {
            let splitUrl = pageLink.href.split('?');
            let queryString = splitUrl.slice(1).join('?');
            const hrefParams = new URLSearchParams(queryString);
            let hrefValue = hrefParams.get('page');
            if (hrefValue === activeUrl) {
                pageLink.active = true
            } else {
                pageLink.active = false
            }
        })
        pages = pages
    }

    const previous = () => {
        let currentPage = parseInt(data.page || "1");
        let prevPage = currentPage - 1;
        if (prevPage <= 0)
            prevPage = 1;
        goto(`/certificates/stakekey-deregistrations?page=${prevPage}&count=15`)
    };
    const next = () => {
        let currentPage = parseInt(data.page || "1");
        let nextPage = currentPage + 1;

        if (data.deregistrations.length == 0)
            nextPage = currentPage;

        goto(`/certificates/stakekey-deregistrations?page=${nextPage}&count=15`)
    };

    console.log(data);
    if (!data.deregistrations)
        data.deregistrations = []
</script>


<section class="container mx-auto text-sm">
    <h2 class="text-xl font-bold text-center text-base-content/60 mb-4">Stake Key De-registrations</h2>
    <div class="flex justify-end mt-6">
        <div class="join">
            <button 
                class="join-item btn btn-sm" 
                disabled={parseInt(data.page) === 1}
                on:click={previous}
            >
                «
            </button>
            <button class="join-item btn btn-sm">Page {data.page}</button>
            <button 
                class="join-item btn btn-sm" 
                disabled={data.deregistrations.length === 0}
                on:click={next}
            >
                »
            </button>
        </div>
    </div>
    <div class="overflow-x-auto bg-base-100 shadow-md rounded-lg mb-4">
        <table class="min-w-full divide-y divide-base-300">
            <thead class="bg-base-200">
            <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Address</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Block</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Time</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Transaction Hash</th>
            </tr>
            </thead>
            <tbody class="bg-base-100 divide-y divide-base-300">
            {#each data.deregistrations as deregistration: StakeDeRegistration, index}
                <tr class="hover:bg-base-200">
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-base-content/60">{deregistration.address}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-base-content/60">
                        <a href="/blocks/{deregistration.block_number}" class="text-blue-500 hover:underline">
                            {deregistration.block_number}
                        </a>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-base-content/60">{getDate(deregistration.block_time)}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-base-content">
                        <a href="/transactions/{deregistration.tx_hash}" class="text-blue-500 hover:underline">
                            <span>{truncate(deregistration.tx_hash, 30, "...")}</span>
                        </a>
                    </td>
                </tr>
            {/each}
            </tbody>
        </table>
    </div>
    <div class="flex justify-end mt-6">
        <div class="join">
            <button 
                class="join-item btn btn-sm" 
                disabled={parseInt(data.page) === 1}
                on:click={previous}
            >
                «
            </button>
            <button class="join-item btn btn-sm">Page {data.page}</button>
            <button 
                class="join-item btn btn-sm" 
                disabled={data.deregistrations.length === 0}
                on:click={next}
            >
                »
            </button>
        </div>
    </div>
</section>
