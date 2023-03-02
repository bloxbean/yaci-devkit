<script>
    import {lovelaceToAda} from '../../../util/ada_util'
    import TxSearch from "../TxSearch.svelte";
    import {List, Li, DescriptionList, Textarea} from 'flowbite-svelte';
    import {Tabs, TabItem, Timeline, TimelineItem, Button} from 'flowbite-svelte';
    import InputOutput from "../../../components/inputoutput/InputOutput.svelte";
    import TxnContext from "../../../components/TxnContext.svelte";

    import {onMount} from "svelte";
    import Inputs from "../../../components/inputoutput/Inputs.svelte";
    import Contract from "../../../components/Contract.svelte";

    export let data;
    let {tx, contracts, metadata} = data;
</script>

<section class="text-gray-600 body-font">
    <div class="container flex flex-wrap px-5 py-24 mx-auto items-center">
        <div class="md:w-1/2 md:pr-12 md:py-8 md:border-r md:border-b-0 mb-10 md:mb-0 pb-10 border-b border-gray-200">
            <List tag="dl" color="text-gray-900 dark:text-white">
                <div class="flex flex-col pb-3">
                    <DescriptionList tag="dt" class="mb-1">Transaction Hash</DescriptionList>
                    <DescriptionList tag="dd" class="text-sm">{tx.hash}</DescriptionList>
                </div>
                <div class="flex flex-col pb-3">
                    <DescriptionList tag="dt" class="mb-1">Block/Slot</DescriptionList>
                    <DescriptionList tag="dd" class="text-sm"><a href="/blocks/{tx.block_height}">{tx.block_height}</a> / {tx.slot}</DescriptionList>
                </div>
                <div class="flex flex-col pb-3">
                    <DescriptionList tag="dt" class="mb-1">TTL</DescriptionList>
                    <DescriptionList tag="dd" class="text-sm">{tx.ttl}</DescriptionList>
                </div>
            </List>
        </div>
        <div class="flex flex-col md:w-1/2 md:pl-12">
            <List tag="dl" color="text-gray-900 dark:text-white">
                <div class="flex flex-col pb-3">
                    <DescriptionList tag="dt" class="mb-1">Fee (Ada)</DescriptionList>
                    <DescriptionList tag="dd" class="text-sm">{lovelaceToAda(tx.fees, 4)}</DescriptionList>
                </div>
                <div class="flex flex-col pb-3">
                    <DescriptionList tag="dt" class="mb-1">Total Outputs (Ada)</DescriptionList>
                    <DescriptionList tag="dd" class="text-sm">{lovelaceToAda(tx.total_output, 2)}</DescriptionList>
                </div>
            </List>
        </div>
    </div>

    <div class="container px-5 py-24 mx-auto items-center">
        <Tabs>
            <TabItem open>
                <span slot="title">Input / Output</span>
                <div>
                    <InputOutput tx={tx}></InputOutput>
                </div>
            </TabItem>
            <TabItem>
                <span slot="title">Contracts ({contracts.length})</span>

                    {#each contracts as contract}
                        <div class="text-sm text-gray-500 dark:text-gray-400 mb-20">
                        <Contract contract={contract}></Contract>
                        </div>
                    {/each}
            </TabItem>
            <TabItem>
                <span slot="title">Collaterals</span>
                <span class="font-semibold title-font text-gray-700 mb-10">Collateral Inputs</span>
                <Inputs inputs={tx.collateral_inputs}></Inputs>
            </TabItem>
<!--            <TabItem>-->
<!--                <span slot="title">Certificates</span>-->
<!--                <p class="text-sm text-gray-500 dark:text-gray-400"><b>Settings:</b> Lorem ipsum dolor sit amet,-->
<!--                    consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</p>-->
<!--            </TabItem>-->
            <TabItem>
                <span slot="title">Metadata</span>
                {metadata? JSON.stringify(metadata, null, 2): []}
            </TabItem>
            <TabItem>
                <span slot="title">Reference Inputs</span>
                <Inputs inputs={tx.reference_inputs}></Inputs>
            </TabItem>
            <TabItem>
                <div slot="title">Json</div>
                <div>
                    <TxnContext tx={tx}></TxnContext>

                </div>

            </TabItem>
        </Tabs>
    </div>

</section>
