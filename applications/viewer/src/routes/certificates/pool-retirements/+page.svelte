<script lang="ts">
    import {goto} from "$app/navigation";
    import {page} from "$app/stores";
    import {truncate} from "$lib/util";

    export let data;

    interface PoolRetirement {
        pool_id_bech32: string;
        epoch: number;
        retirement_epoch: number;
        block_number: number;
        tx_hash: string;
    }

    interface PageLink {
        href: string;
        active: boolean;
    }

    $: activeUrl = $page.url.searchParams.get('page');
    let pages: PageLink[] = [];

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
        goto(`/certificates/pool-retirements?page=${prevPage}&count=15`)
    };
    const next = () => {
        let currentPage = parseInt(data.page || "1");
        let nextPage = currentPage + 1;

        if (data.retirements.length == 0)
            nextPage = currentPage;

        goto(`/certificates/pool-retirements?page=${nextPage}&count=15`)
    };

    if (!data.retirements)
        data.retirements = []
</script>

<section class="container mx-auto text-sm">
    <h2 class="text-xl font-bold text-center text-gray-500 mb-4">Pool Retirements</h2>
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
                disabled={data.retirements.length === 0}
                on:click={next}
            >
                »
            </button>
        </div>
    </div>
    <div class="overflow-x-auto bg-white shadow-md rounded-lg mb-4">
        <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
            <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Pool</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Epoch</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Retirement Epoch</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Block</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Transaction Hash</th>
            </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
            {#each data.retirements as retirement: PoolRetirement, index}
                <tr class="hover:bg-gray-50">
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{truncate(retirement.pool_id_bech32, 35, "...")}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{retirement.epoch}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{retirement.retirement_epoch}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        <a href="/blocks/{retirement.block_number}" class="text-blue-500 hover:underline">
                            {retirement.block_number}
                        </a>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        <a href="/transactions/{retirement.tx_hash}" class="text-blue-500 hover:underline">
                            <span>{truncate(retirement.tx_hash, 30)}</span>
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
                disabled={data.retirements.length === 0}
                on:click={next}
            >
                »
            </button>
        </div>
    </div>
</section>
