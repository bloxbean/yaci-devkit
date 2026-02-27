package com.bloxbean.cardano.yacimcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.Executors;

public class McpServer {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String PROTOCOL_VERSION = "2025-11-25";

    private static String adminUrl;

    public static void main(String[] args) throws IOException {
        adminUrl = System.getenv().getOrDefault("YACI_ADMIN_URL", "http://localhost:10000");
        int port = Integer.parseInt(System.getenv().getOrDefault("MCP_PORT", "9500"));

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/mcp", McpServer::handleMcp);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.err.println("MCP server listening on port " + port);
        System.err.println("Proxying to " + adminUrl);
    }

    private static void handleMcp(HttpExchange exchange) throws IOException {
        // Handle CORS preflight
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        byte[] body;
        try (InputStream is = exchange.getRequestBody()) {
            body = is.readAllBytes();
        }

        JsonNode request;
        try {
            request = mapper.readTree(body);
        } catch (Exception e) {
            sendJsonRpcError(exchange, null, -32700, "Parse error");
            return;
        }

        String method = request.path("method").asText("");
        JsonNode id = request.get("id");
        JsonNode params = request.get("params");

        System.err.println("-> " + method + " id=" + id);

        try {
            JsonNode result = dispatch(method, params);
            if (result == null) {
                // Notification — no response needed, but HTTP still needs a body
                if (id == null || id.isNull()) {
                    sendJson(exchange, 200, mapper.createObjectNode());
                    return;
                }
                sendJsonRpcResult(exchange, id, mapper.createObjectNode());
            } else {
                sendJsonRpcResult(exchange, id, result);
            }
        } catch (Exception e) {
            System.err.println("Error handling " + method + ": " + e.getMessage());
            e.printStackTrace(System.err);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            sendJsonRpcError(exchange, id, -32603, msg);
        }
    }

    private static JsonNode dispatch(String method, JsonNode params) throws Exception {
        return switch (method) {
            case "initialize" -> handleInitialize();
            case "notifications/initialized" -> null;
            case "ping" -> mapper.createObjectNode();
            case "tools/list" -> handleToolsList();
            case "tools/call" -> handleToolsCall(params);
            default -> throw new Exception("Unknown method: " + method);
        };
    }

    // ---- MCP protocol methods ----

    private static JsonNode handleInitialize() {
        ObjectNode result = mapper.createObjectNode();
        result.put("protocolVersion", PROTOCOL_VERSION);

        ObjectNode capabilities = mapper.createObjectNode();
        capabilities.putObject("tools");
        result.set("capabilities", capabilities);

        ObjectNode serverInfo = mapper.createObjectNode();
        serverInfo.put("name", "yaci-devkit");
        serverInfo.put("version", "0.1.0");
        result.set("serverInfo", serverInfo);

        return result;
    }

    private static JsonNode handleToolsList() {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode tools = result.putArray("tools");

        // devnet_reset
        addTool(tools, "devnet_reset", "Reset devnet to initial state. Wipes all transactions and returns to genesis.",
                mapper.createObjectNode().put("type", "object").set("properties", mapper.createObjectNode()));

        // devnet_status
        addTool(tools, "devnet_status", "Get devnet info and current tip (slot, block, epoch).",
                mapper.createObjectNode().put("type", "object").set("properties", mapper.createObjectNode()));

        // devnet_topup
        ObjectNode topupProps = mapper.createObjectNode();
        topupProps.set("address", mapper.createObjectNode().put("type", "string").put("description", "Bech32 address to fund"));
        topupProps.set("adaAmount", mapper.createObjectNode().put("type", "number").put("description", "Amount of ADA to send"));
        ObjectNode topupSchema = mapper.createObjectNode();
        topupSchema.put("type", "object");
        topupSchema.set("properties", topupProps);
        topupSchema.set("required", mapper.createArrayNode().add("address").add("adaAmount"));
        addTool(tools, "devnet_topup", "Fund an address with ADA from the devnet faucet.", topupSchema);

        // devnet_utxos
        ObjectNode utxoProps = mapper.createObjectNode();
        utxoProps.set("address", mapper.createObjectNode().put("type", "string").put("description", "Bech32 address to query"));
        ObjectNode utxoSchema = mapper.createObjectNode();
        utxoSchema.put("type", "object");
        utxoSchema.set("properties", utxoProps);
        utxoSchema.set("required", mapper.createArrayNode().add("address"));
        addTool(tools, "devnet_utxos", "Query UTxOs at an address.", utxoSchema);

        // devnet_submit_tx
        ObjectNode submitProps = mapper.createObjectNode();
        submitProps.set("cborHex", mapper.createObjectNode().put("type", "string").put("description", "Transaction CBOR as hex string"));
        ObjectNode submitSchema = mapper.createObjectNode();
        submitSchema.put("type", "object");
        submitSchema.set("properties", submitProps);
        submitSchema.set("required", mapper.createArrayNode().add("cborHex"));
        addTool(tools, "devnet_submit_tx", "Submit a signed transaction (CBOR hex) to the devnet.", submitSchema);

        return result;
    }

    private static void addTool(ArrayNode tools, String name, String description, JsonNode inputSchema) {
        ObjectNode tool = tools.addObject();
        tool.put("name", name);
        tool.put("description", description);
        tool.set("inputSchema", inputSchema);
    }

    private static JsonNode handleToolsCall(JsonNode params) throws Exception {
        String name = params.path("name").asText("");
        JsonNode args = params.has("arguments") ? params.get("arguments") : mapper.createObjectNode();

        String text = switch (name) {
            case "devnet_reset" -> callDevnetReset();
            case "devnet_status" -> callDevnetStatus();
            case "devnet_topup" -> callDevnetTopup(args);
            case "devnet_utxos" -> callDevnetUtxos(args);
            case "devnet_submit_tx" -> callDevnetSubmitTx(args);
            default -> throw new Exception("Unknown tool: " + name);
        };

        ObjectNode result = mapper.createObjectNode();
        ArrayNode content = result.putArray("content");
        ObjectNode textContent = content.addObject();
        textContent.put("type", "text");
        textContent.put("text", text);
        return result;
    }

    // ---- Tool implementations ----

    private static String callDevnetReset() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(adminUrl + "/local-cluster/api/admin/devnet/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            return "Devnet reset successfully.";
        }
        return "Reset failed (HTTP " + resp.statusCode() + "): " + resp.body();
    }

    private static String callDevnetStatus() throws Exception {
        // Fetch devnet info
        HttpRequest infoReq = HttpRequest.newBuilder()
                .uri(URI.create(adminUrl + "/local-cluster/api/admin/devnet"))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();
        HttpResponse<String> infoResp = client.send(infoReq, HttpResponse.BodyHandlers.ofString());

        // Fetch tip
        HttpRequest tipReq = HttpRequest.newBuilder()
                .uri(URI.create(adminUrl + "/local-cluster/api/admin/devnet/tip"))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();
        HttpResponse<String> tipResp = client.send(tipReq, HttpResponse.BodyHandlers.ofString());

        ObjectNode result = mapper.createObjectNode();
        try {
            result.set("devnet", mapper.readTree(infoResp.body()));
        } catch (Exception e) {
            result.put("devnet", infoResp.body());
        }
        try {
            result.set("tip", mapper.readTree(tipResp.body()));
        } catch (Exception e) {
            result.put("tip", tipResp.body());
        }
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }

    private static String callDevnetTopup(JsonNode args) throws Exception {
        String address = args.path("address").asText("");
        double adaAmount = args.path("adaAmount").asDouble(0);

        if (address.isEmpty()) throw new Exception("address is required");
        if (adaAmount <= 0) throw new Exception("adaAmount must be positive");

        ObjectNode body = mapper.createObjectNode();
        body.put("address", address);
        body.put("adaAmount", adaAmount);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(adminUrl + "/local-cluster/api/addresses/topup"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            return "Topped up " + adaAmount + " ADA to " + address + "\n" + resp.body();
        }
        return "Topup failed (HTTP " + resp.statusCode() + "): " + resp.body();
    }

    private static String callDevnetUtxos(JsonNode args) throws Exception {
        String address = args.path("address").asText("");
        if (address.isEmpty()) throw new Exception("address is required");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(adminUrl + "/local-cluster/api/addresses/" + address + "/utxos"))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        try {
            JsonNode parsed = mapper.readTree(resp.body());
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
        } catch (Exception e) {
            return resp.body();
        }
    }

    private static String callDevnetSubmitTx(JsonNode args) throws Exception {
        String cborHex = args.path("cborHex").asText("");
        if (cborHex.isEmpty()) throw new Exception("cborHex is required");

        byte[] cborBytes;
        try {
            cborBytes = HexFormat.of().parseHex(cborHex);
        } catch (IllegalArgumentException e) {
            throw new Exception("Invalid hex string: " + e.getMessage());
        }

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(adminUrl + "/local-cluster/api/tx/submit"))
                .header("Content-Type", "application/cbor")
                .POST(HttpRequest.BodyPublishers.ofByteArray(cborBytes))
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            return "Transaction submitted successfully.\n" + resp.body();
        }
        return "Submit failed (HTTP " + resp.statusCode() + "): " + resp.body();
    }

    // ---- HTTP response helpers ----

    private static void sendJsonRpcResult(HttpExchange exchange, JsonNode id, JsonNode result) throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        response.set("result", result);
        sendJson(exchange, 200, response);
    }

    private static void sendJsonRpcError(HttpExchange exchange, JsonNode id, int code, String message) throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        ObjectNode error = response.putObject("error");
        error.put("code", code);
        error.put("message", message);
        sendJson(exchange, 200, response);
    }

    private static void sendJson(HttpExchange exchange, int status, JsonNode body) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

}
