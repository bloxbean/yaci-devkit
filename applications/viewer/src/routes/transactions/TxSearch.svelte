<script>
import {actions} from './TxSearch'
import {goto} from "$app/navigation";
let txHash = ''
function redirect() {
    goto("/transactions/" + txHash);
}
</script>
<div>
    Transaction
    <input bind:value={txHash}>
    <button on:click={redirect}>Search</button>
</div>
