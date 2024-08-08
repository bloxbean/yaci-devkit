<script>
    import { lovelaceToAda } from '../../../util/ada_util';
    import InputOutput from "../../../components/inputoutput/InputOutput.svelte";
    import TxnContext from "../../../components/TxnContext.svelte";

    import Inputs from "../../../components/inputoutput/Inputs.svelte";
    import Contract from "../../../components/Contract.svelte";
    import JsonContent from "../../../components/JsonContent.svelte";

    export let data;
    let { tx, contracts, metadata } = data;

    const INPUT_TAB = 0;
    const CONTRACT_TAB = 1;
    const COLLATERAL_TAB = 2;
    const METADATA_TAB = 3;
    const REFERENCE_INPUT_TAB = 4;
    const JSON_TAB = 5;

    let activeTabIndex = INPUT_TAB;
</script>

<section class="py-20 px-4 md:px-0">
    <div class="container mx-auto">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <article class="bg-gray-50 p-4 shadow-md w-full">
                <div class="flex flex-col">
                    <div class="bg-gray-200 py-2 px-4 mb-2">
                        <strong>Transaction Hash</strong>
                    </div>
                    <div class="bg-gray-100 py-2 px-4 break-words">
                        <p>
                            <small>{tx.hash}</small>
                        </p>
                    </div>
                </div>
                <div class="flex flex-col">
                    <div class="bg-gray-200 py-2 px-4 mb-2">
                        <strong>Block/Slot</strong>
                    </div>
                    <div class="bg-gray-100 py-2 px-4">
                        <p>
                            <small><a href="/blocks/{tx.block_height}">{tx.block_height}</a> / {tx.slot}</small>
                        </p>
                    </div>
                </div>
                <div class="flex flex-col">
                    <div class="bg-gray-200 py-2 px-4 mb-2">
                        <strong>TTL</strong>
                    </div>
                    <div class="bg-gray-100 py-2 px-4">
                        <p>
                            <small>{tx.ttl}</small>
                        </p>
                    </div>
                </div>
            </article>
            <article class="bg-gray-50 p-4 shadow-md w-full">
                <div class="flex flex-col">
                    <div class="bg-gray-200 py-2 px-4 mb-2">
                        <strong>Fee (Ada)</strong>
                    </div>
                    <div class="bg-gray-100 py-2 px-4">
                        <p>
                            <small>{lovelaceToAda(tx.fees, 4)}</small>
                        </p>
                    </div>
                </div>
                <div class="flex flex-col">
                    <div class="bg-gray-200 py-2 px-4 mb-2">
                        <strong>Total Outputs (Ada)</strong>
                    </div>
                    <div class="bg-gray-100 py-2 px-4">
                        <p>
                            <small>{lovelaceToAda(tx.total_output, 2)}</small>
                        </p>
                    </div>
                </div>
            </article>
        </div>
    </div>
</section>

<section class="py-10 px-4 md:px-0">
    <div class="container mx-auto">
        <div class="tabs tabs-boxed tabs-xs md:tabs-sm lg:tabs">
            <a href="#" role="tab" class="tab  {activeTabIndex == 0 ? 'tab-active' : ''}" on:click={() => activeTabIndex = INPUT_TAB}>
                <span class="block md:hidden">I/O</span>
                <span class="hidden md:block">Input/Output</span>
            </a>
            <a href="#" role="tab" class="tab  {activeTabIndex == 1 ? 'tab-active' : ''}" on:click={() => activeTabIndex = CONTRACT_TAB}>Contracts</a>
            <a href="#" role="tab" class="tab  {activeTabIndex == 2 ? 'tab-active' : ''}" on:click={() => activeTabIndex = COLLATERAL_TAB}>Collaterals</a>
            <a href="#" role="tab" class="tab  {activeTabIndex == 3 ? 'tab-active' : ''}" on:click={() => activeTabIndex = METADATA_TAB}>Metadata</a>
            <a href="#" role="tab" class="tab {activeTabIndex == 4 ? 'tab-active' : ''}" on:click={() => activeTabIndex = REFERENCE_INPUT_TAB}>
                <span class="block md:hidden">Ref In</span>
                <span class="hidden md:block">Reference Input</span>
            </a>
            <a href="#" role="tab" class="tab hidden md:flex md:items-center md:justify-center {activeTabIndex == 5 ? 'tab-active' : ''}" on:click={() => activeTabIndex = JSON_TAB}>Json</a>
        </div>

        {#if activeTabIndex == INPUT_TAB}
            <div class="w-full">
                <InputOutput tx={tx}></InputOutput>
            </div>
        {/if}

        {#if activeTabIndex == CONTRACT_TAB}
            <div class="w-full">
                {#each contracts as contract}
                    <div class="text-sm text-gray-500 dark:text-gray-400 mb-20">
                        <Contract contract={contract}></Contract>
                    </div>
                {/each}
            </div>
        {/if}

        {#if activeTabIndex == COLLATERAL_TAB}
            <div class="w-full">
                <Inputs inputs={tx.collateral_inputs}></Inputs>
            </div>
        {/if}

        {#if activeTabIndex == METADATA_TAB}
            <div class="w-full">
                <JsonContent text={metadata}></JsonContent>
            </div>
        {/if}

        {#if activeTabIndex == REFERENCE_INPUT_TAB}
            <div class="w-full">
                <Inputs inputs={tx.reference_inputs}></Inputs>
            </div>
        {/if}

        {#if activeTabIndex == JSON_TAB}
            <div class="w-full">
                <TxnContext tx={tx}></TxnContext>
            </div>
        {/if}
    </div>
</section>
