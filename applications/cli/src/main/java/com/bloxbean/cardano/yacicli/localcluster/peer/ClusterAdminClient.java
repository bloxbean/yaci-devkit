package com.bloxbean.cardano.yacicli.localcluster.peer;

import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.api.model.TopupRequest;
import com.bloxbean.cardano.yacicli.localcluster.api.model.TopupResult;
import lombok.SneakyThrows;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.info;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.warn;

public class ClusterAdminClient {

    public static boolean isBootstrapNodeInitialized(String adminUrl) {
        String apiUrl = adminUrl + "/local-cluster/api/admin/devnet/status";
        // Create a RestTemplate instance
        RestTemplate restTemplate = new RestTemplate();

        // Send a GET request to the API endpoint and retrieve the response
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.GET,
                null,
                String.class
        );

        // Extract the ClusterInfo object from the response body
        String status = response.getBody();
        if (status != null && status.equals("initialized"))
            return true;
        else
            return false;
    }

    public static ClusterInfo getClusterInfo(String adminUrl) {
        String apiUrl = adminUrl + "/local-cluster/api/admin/devnet";
        // Create a RestTemplate instance
        RestTemplate restTemplate = new RestTemplate();

        // Send a GET request to the API endpoint and retrieve the response
        ResponseEntity<ClusterInfo> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.GET,
                null,
                ClusterInfo.class
        );

        // Extract the ClusterInfo object from the response body
        ClusterInfo clusterInfo = response.getBody();
        return clusterInfo;
    }

    public static int getKesPeriod(String adminUrl) {
        String apiUrl = adminUrl + "/local-cluster/api/admin/devnet/kes-period";
        // Create a RestTemplate instance
        RestTemplate restTemplate = new RestTemplate();

        // Send a GET request to the API endpoint and retrieve the response
        ResponseEntity<Integer> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.GET,
                null,
                Integer.class
        );

        // Extract the ClusterInfo object from the response body
        Integer kesPeriod = response.getBody();
        return kesPeriod;
    }

    public static List<Utxo> getUtxos(String adminUrl, String address) {
        String apiUrl = adminUrl + "/local-cluster/api/addresses/" + address + "/utxos";
        // Create a RestTemplate instance
        RestTemplate restTemplate = new RestTemplate();

        // Send a GET request to the API endpoint and retrieve the response
        ResponseEntity<List<Utxo>> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Utxo>>(){}
        );

        // Extract the ClusterInfo object from the response body
        List<Utxo> utxos = response.getBody();
        return utxos;
    }

    public static TopupResult topup(String adminUrl, String address, double adaAmount) {
        String apiUrl = adminUrl + "/local-cluster/api/addresses/topup";
        // Create a RestTemplate instance
        RestTemplate restTemplate = new RestTemplate();

        //Create a TopupRequest object
        TopupRequest topupRequest = new TopupRequest(address, adaAmount);

        // Send a POST request to the API endpoint with TopupRequest object and retrieve the response
       TopupResult topupResult = restTemplate.postForObject(
                apiUrl,
                topupRequest,
                TopupResult.class
        );

        return topupResult;
    }

    @SneakyThrows
    public static boolean waitForTxToComplete(UtxoSupplier utxoSupplier, String address, String txHash, Consumer<String> writer) {
        int count = 0;
        while (true) {
            List<Utxo> utxos = utxoSupplier.getAll(address);
            writer.accept(info("Waiting for transaction to complete. Please wait..."));
            if (utxos == null || utxos.size() == 0)
                continue;

            Optional<Utxo> utxo = utxos.stream().filter(u -> u.getTxHash().equals(txHash)).findFirst();
            if (utxo.isPresent())
                return true;

            count++;
            Thread.sleep(1000);

            if (count > 60) {
                writer.accept(warn("Transaction not found in 60 seconds. Please check the transaction status on the explorer and try again if required."));
                return false;
            }

        }
    }
}

