import { BrowserWallet, ForgeScript, Transaction, type Protocol } from "@meshsdk/core";
import { deserializeNativeScript } from "@meshsdk/core-cst";
import { stringToHex } from "@meshsdk/common";
import { YaciProvider } from "@meshsdk/provider";
import { BlockfrostProvider } from "@meshsdk/core";

// Yaci Store provider — used to fetch protocol parameters from the devnet
const provider = new YaciProvider("http://localhost:8080/api/v1/");
// const provider = new BlockfrostProvider("");

const walletSelect = document.getElementById("wallet-select") as HTMLSelectElement;
const connectBtn = document.getElementById("connect-btn") as HTMLButtonElement;
const connectedInfo = document.getElementById("connected-info") as HTMLDivElement;
const mintBtn = document.getElementById("mint-btn") as HTMLButtonElement;
const statusEl = document.getElementById("status") as HTMLDivElement;

const assetNameInput = document.getElementById("asset-name") as HTMLInputElement;
const displayNameInput = document.getElementById("display-name") as HTMLInputElement;
const descriptionInput = document.getElementById("description") as HTMLInputElement;
const imageUrlInput = document.getElementById("image-url") as HTMLInputElement;

let wallet: BrowserWallet | null = null;
let walletAddress = "";
let protocolParams: Partial<Protocol> | undefined;

// --- Status helpers ---
function escapeHtml(s: string): string {
  return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function showStatus(msg: string, type: "info" | "success" | "error") {
  statusEl.innerHTML = msg;
  statusEl.className = type;
  statusEl.style.display = "";
}

// --- Fetch protocol params from Yaci Store on startup ---
provider.fetchProtocolParameters().then((params) => {
  protocolParams = params;
}).catch((err) => {
  console.warn("Could not fetch protocol params from Yaci Store, using defaults:", err);
});

// --- Discover wallets ---
async function discoverWallets() {
  const wallets = await BrowserWallet.getAvailableWallets();
  walletSelect.innerHTML = "";

  if (wallets.length === 0) {
    walletSelect.innerHTML = "<option>No wallets found</option>";
    return;
  }

  for (const w of wallets) {
    const opt = document.createElement("option");
    opt.value = w.id;
    opt.textContent = w.name;
    walletSelect.appendChild(opt);
  }

  walletSelect.disabled = false;
  connectBtn.disabled = false;
}

// Discover now, and also when yacidevkit finishes loading
discoverWallets();
window.addEventListener("yacidevkit:ready", () => discoverWallets());

// --- Connect / Disconnect wallet ---
connectBtn.addEventListener("click", async () => {
  // If already connected, disconnect
  if (wallet) {
    wallet = null;
    walletAddress = "";
    connectBtn.textContent = "Connect Wallet";
    walletSelect.disabled = false;
    connectedInfo.style.display = "none";
    connectedInfo.textContent = "";
    mintBtn.disabled = true;
    statusEl.style.display = "none";
    return;
  }

  const walletId = walletSelect.value;
  if (!walletId) return;

  connectBtn.disabled = true;
  connectBtn.textContent = "Connecting...";
  showStatus("Connecting to wallet...", "info");

  try {
    wallet = await BrowserWallet.enable(walletId);
    const usedAddresses = await wallet.getUsedAddresses();
    walletAddress = usedAddresses[0];

    connectedInfo.style.display = "block";
    connectedInfo.textContent = `Connected: ${walletAddress}`;
    connectBtn.textContent = "Disconnect";
    walletSelect.disabled = true;
    mintBtn.disabled = false;
    statusEl.style.display = "none";
  } catch (err: any) {
    showStatus(`Connection failed: ${escapeHtml(String(err.message || err))}`, "error");
    connectBtn.textContent = "Connect Wallet";
  }

  connectBtn.disabled = false;
});

// --- Mint NFT ---
mintBtn.addEventListener("click", async () => {
  if (!wallet || !walletAddress) return;

  const assetName = assetNameInput.value.trim();
  const displayName = displayNameInput.value.trim();
  const description = descriptionInput.value.trim();
  const imageUrl = imageUrlInput.value.trim();

  if (!assetName) {
    showStatus("Asset name is required.", "error");
    return;
  }

  mintBtn.disabled = true;
  mintBtn.textContent = "Building transaction...";
  showStatus("Building minting transaction...", "info");

  try {
    // Fetch latest protocol params if not already cached
    if (!protocolParams) {
      protocolParams = await provider.fetchProtocolParameters();
    }

    // Create native script policy from wallet's payment key
    const forgingScript = ForgeScript.withOneSignature(walletAddress);
    const policyId = deserializeNativeScript(forgingScript).hash().toString();
    const assetNameHex = stringToHex(assetName);
    const assetUnit = policyId + assetNameHex;

    // Build minting transaction with devnet protocol params.
    // We mint without a recipient and send the NFT separately with
    // explicit lovelace so coin selection accounts for the full output.
    const tx = new Transaction({
      initiator: wallet,
      fetcher: provider,
      params: protocolParams,
    });
    tx.mintAsset(forgingScript, {
      assetName,
      assetQuantity: "1",
      metadata: {
        name: displayName || assetName,
        image: imageUrl || "ipfs://QmRzicpReutwCkM6aotuKjErFCUD213DpwPq6ByuzMJaua",
        mediaType: "image/jpg",
        description: description || "NFT minted on Yaci DevKit",
      },
      label: "721",
    });
    tx.sendAssets(walletAddress, [
      { unit: "lovelace", quantity: "2000000" },
      { unit: assetUnit, quantity: "1" },
    ]);

    showStatus("Building transaction...", "info");
    const unsignedTx = await tx.build();

    // Sign via CIP-30 wallet
    showStatus("Waiting for wallet signature...", "info");
    mintBtn.textContent = "Sign in wallet...";
    const signedTx = await wallet.signTx(unsignedTx);

    // Submit via CIP-30 wallet
    showStatus("Submitting transaction...", "info");
    mintBtn.textContent = "Submitting...";
    const txHash = await wallet.submitTx(signedTx);

    showStatus(`NFT minted successfully!<br>Tx Hash: <code>${txHash}</code>`, "success");
  } catch (err: any) {
    showStatus(`Minting failed: ${escapeHtml(String(err.message || err))}`, "error");
  }

  mintBtn.disabled = false;
  mintBtn.textContent = "Mint NFT";
});
