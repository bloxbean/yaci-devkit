<script>
    import {lovelaceToAda} from '../../../util/ada_util'

    export let data;
</script>

Tx Hash
<div>{data.tx_hash} </div>

Block
<div>{data.block_number}</div>

Slot
<div>{data.slot}</div>

Fee
<div>{lovelaceToAda(data.fee)} Ada</div>

TTL
<div>{data.ttl}</div>

<h1>Inputs</h1>
{#each data.inputs as input}
    <div>
        Utxo Hash: {input.tx_hash}#{input.output_index}
    </div>
    <div>
        Address: {input.owner_addr}
    </div>
    <div>
        Stake Addr: {input.owner_stake_addr}
    </div>
    <div>
        DataHash: {input.data_hash}
    </div>
    <div>
        Inline Datum: {input.inline_datum}
    </div>
    <div>
        Reference ScriptHash: {input.reference_script_hash}
    </div>
    <div>
        Inline Datum Json:
    </div>
    <div>------------------------------------</div>

    {#each input.amounts as amount}
        {#if amount.assetName === 'lovelace'}
            Ada :
            {lovelaceToAda(amount.quantity)}
        {:else}
            <div>
                {amount.assetName} : {amount.quantity}
                <br/> {amount.policyId}
            </div>
        {/if}

    {/each}
{/each}

<h1> Outputs </h1>

{#each data.outputs as output}
    <div>
        Address: {output.owner_addr}
    </div>
    <div>
        Stake Addr: {output.owner_stake_addr}
    </div>
    <div>
        Utxo: {output.tx_hash}#{output.output_index}
    </div>
    <div>
        DataHash: {output.data_hash}
    </div>
    <div>
        Inline Datum: {output.inline_datum}
    </div>
    <div>
        Reference ScriptHash: {output.reference_script_hash}
    </div>
    <div>
        Inline Datum Json:
    </div>
    {#each output.amounts as amount}
        {#if amount.assetName === 'lovelace'}
            Ada :
            {lovelaceToAda(amount.quantity)}
        {:else}
            <div>
                {amount.assetName} : {amount.quantity}
                <br/> {amount.policyId}
            </div>
        {/if}

    {/each}
    <div>------------------------------------</div>
{/each}
