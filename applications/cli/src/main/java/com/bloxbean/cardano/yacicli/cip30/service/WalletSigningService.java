package com.bloxbean.cardano.yacicli.cip30.service;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.cip.cip30.CIP30DataSigner;
import com.bloxbean.cardano.client.cip.cip30.DataSignature;
import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.client.transaction.spec.TransactionWitnessSet;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yacicli.cip30.dto.SignDataResponse;
import com.bloxbean.cardano.yacicli.cip30.dto.SignTxResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletSigningService {
    private final WalletAccountService walletAccountService;

    /**
     * Sign a transaction with the specified account
     * @param txCbor The transaction CBOR (hex encoded)
     * @param accountId The account ID to sign with
     * @return SignTxResponse with witness set and tx hash
     */
    public Optional<SignTxResponse> signTransaction(String txCbor, String accountId) {
        try {
            // Get the signing account
            Optional<Account> accountOpt = walletAccountService.getSigningAccount(accountId);
            if (accountOpt.isEmpty()) {
                log.error("Account not found: {}", accountId);
                return Optional.empty();
            }

            Account account = accountOpt.get();

            // Deserialize the transaction
            byte[] txBytes = HexUtil.decodeHexString(txCbor);
            Transaction transaction = Transaction.deserialize(txBytes);

            // Sign the transaction using Account.sign(Transaction)
            Transaction signedTx = account.sign(transaction);

            // Get the witness set and serialize it
            TransactionWitnessSet witnessSet = signedTx.getWitnessSet();
            byte[] witnessSetBytes = com.bloxbean.cardano.client.common.cbor.CborSerializationUtil.serialize(witnessSet.serialize());
            String witnessSetHex = HexUtil.encodeHexString(witnessSetBytes);

            // Calculate transaction hash from the body
            byte[] txBodyBytes = com.bloxbean.cardano.client.common.cbor.CborSerializationUtil.serialize(transaction.getBody().serialize());
            String txHash = HexUtil.encodeHexString(Blake2bUtil.blake2bHash256(txBodyBytes));

            log.info("Signed transaction with account {}, txHash: {}", accountId, txHash);

            return Optional.of(SignTxResponse.builder()
                    .witnessSet(witnessSetHex)
                    .txHash(txHash)
                    .build());

        } catch (Exception e) {
            log.error("Error signing transaction", e);
            return Optional.empty();
        }
    }

    /**
     * Sign a transaction with an account matching the given address
     * @param txCbor The transaction CBOR (hex encoded)
     * @param address The address to find the signing account
     * @return SignTxResponse with witness set and tx hash
     */
    public Optional<SignTxResponse> signTransactionByAddress(String txCbor, String address) {
        Optional<Account> accountOpt = walletAccountService.getSigningAccountByAddress(address);
        if (accountOpt.isEmpty()) {
            log.error("No account found for address: {}", address);
            return Optional.empty();
        }

        // Find the account ID
        var accounts = walletAccountService.getAllAccounts();
        String accountId = accounts.stream()
                .filter(a -> a.getAddress().equals(address))
                .map(a -> a.getId())
                .findFirst()
                .orElse("0");

        return signTransaction(txCbor, accountId);
    }

    /**
     * Sign arbitrary data (CIP-8 / CIP-30 signData)
     * Uses CIP30DataSigner from cardano-client-lib for proper COSE_Sign1 signatures.
     * Per CIP-30, the address parameter determines which key is used for signing:
     * base/enterprise/pointer address → payment key, reward address → staking key.
     *
     * @param payload The payload to sign (hex encoded)
     * @param address The address hex from CIP-30 signData call (determines signing key)
     * @param accountId The account ID to sign with
     * @return SignDataResponse with COSE_Sign1 signature and COSE_Key
     */
    public Optional<SignDataResponse> signData(String payload, String address, String accountId) {
        try {
            Optional<Account> accountOpt = walletAccountService.getSigningAccount(accountId);
            if (accountOpt.isEmpty()) {
                log.error("Account not found: {}", accountId);
                return Optional.empty();
            }

            Account account = accountOpt.get();

            // Decode the payload hex
            byte[] payloadBytes = HexUtil.decodeHexString(payload);

            // Use the address from the CIP-30 request (determines signing key per CIP-8)
            // Fall back to account base address if not provided
            byte[] addressBytes;
            if (address != null && !address.isBlank()) {
                addressBytes = HexUtil.decodeHexString(address);
            } else {
                addressBytes = account.getBaseAddress().getBytes();
            }

            // Use CIP30DataSigner for proper COSE_Sign1 signing (CIP-8 compliant)
            DataSignature dataSignature = CIP30DataSigner.INSTANCE.signData(
                    payloadBytes, addressBytes, account);

            log.info("Signed data with account {} (CIP-8 COSE_Sign1)", accountId);

            return Optional.of(SignDataResponse.builder()
                    .signature(dataSignature.signature())
                    .key(dataSignature.key())
                    .build());

        } catch (Exception e) {
            log.error("Error signing data", e);
            return Optional.empty();
        }
    }
}
