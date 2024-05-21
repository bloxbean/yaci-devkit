package com.bloxbean.cardano.yacicli.commands.localcluster;

import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yaci.core.util.OSUtil;
import com.bloxbean.cardano.yacicli.commands.localcluster.events.ClusterCreated;
import com.bloxbean.cardano.yacicli.commands.localcluster.events.ClusterStopped;
import com.bloxbean.cardano.yacicli.commands.tail.BlockStreamerService;
import com.bloxbean.cardano.yacicli.output.OutputFormatter;
import com.bloxbean.cardano.yacicli.util.TemplateEngine;
import com.bloxbean.cardano.yacicli.util.AdvancedTemplateEngine;
import com.bloxbean.cardano.yacicli.util.ZipUtil;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

import static com.bloxbean.cardano.yacicli.commands.localcluster.ClusterConfig.NODE_FOLDER_PREFIX;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;
import static java.nio.file.attribute.PosixFilePermission.*;

@Component
@Slf4j
public class ClusterService {

    public static final String BABBAGE_TEMPLATE_PATH = "localcluster/templates/devnet";
    public static final String CLASSPATH_LOCALCLUSTER_ZIP = "classpath:localcluster.zip";
    private ClusterConfig clusterConfig;
    private ClusterStartService clusterStartService;
    private BlockStreamerService blockStreamerService;

    @Autowired
    ResourceLoader resourceLoader;

    @Autowired
    TemplateEngine templateEngine;

    @Autowired
    AdvancedTemplateEngine templateEngineHelper;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private WebServerApplicationContext server;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Value("${node.networkId:Testnet}")
    private String networkId;

    @Value("${node.maxKESEvolutions:60}")
    private int maxKESEvolutions;

    @Value("${node.securityParam:300}")
    private int securityParam;

    @Value("${node.slotsPerKESPeriod:129600}")
    private long slotsPerKESPeriod;

    @Value("${node.updateQuorum:1}")
    private int updateQuorum;

    @Value("${node.peerSharing:true}")
    private String peerSharing;


    public ClusterService(ClusterConfig config, ClusterStartService clusterStartService, BlockStreamerService blockStreamerService) {
        this.clusterConfig = config;
        this.clusterStartService = clusterStartService;
        this.blockStreamerService = blockStreamerService;
    }

    public void startClusterContext(String clusterName, Consumer<String> writer) {
        try {
            List<String> clusters = listClusters();
            if (!clusters.contains(clusterName)) {
                throw new IllegalArgumentException("Cluster not found : " + clusterName);
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else
                throw new RuntimeException(e);
        }
    }

    public boolean startCluster(String clusterName) {
        try {
            boolean startedSuccessfully = clusterStartService.startCluster(getClusterInfo(clusterName), getClusterFolder(clusterName), msg -> writeLn(msg));
            if (startedSuccessfully)
                writeLn(info("Swagger Url to interact with the cluster's node : " + "http://localhost:" + server.getWebServer().getPort() +"/swagger-ui.html"));

            return startedSuccessfully;
        } catch (Exception e) {
            System.out.println("Error a creating local cluster");
            throw new RuntimeException(e);
        }
    }

    public void stopCluster(Consumer<String> writer) {
        clusterStartService.stopCluster(writer);
        publisher.publishEvent(new ClusterStopped());
    }

    public List<String> listClusters() throws IOException {
        Path path = Path.of(clusterConfig.getClusterHome());
        if (!Files.exists(path))
            return Collections.EMPTY_LIST;

        List<String> folders = Files.walk(path, 1)
                .filter(Files::isDirectory)
                .map(path1 -> path1.getFileName().toString())
                .collect(Collectors.toList());

        if (folders.size() > 0)
            folders.remove(0); //remove the first element as it's local-cluster

        return folders;
    }

    public void deleteCluster(String clusterName, Consumer<String> writer) throws IOException {
        Path clusterFolder = getClusterFolder(clusterName);
        if (Files.exists(clusterFolder)) {
            FileUtils.deleteDirectory(clusterFolder.toFile());
            if (!clusterFolder.toFile().exists())
                writer.accept(success("ClusterFolder %s deleted", clusterFolder.getFileName().toString()));
        } else {
            writer.accept(error("Cluster folder not found : %s", clusterFolder));
        }
    }

    public void deleteClusterDataFolder(String clusterName, Consumer<String> writer) throws IOException {
        Path clusterFolder = getClusterFolder(clusterName);
        if (Files.exists(clusterFolder)) {
            String nodeName = NODE_FOLDER_PREFIX;
            Path dbFolder = clusterFolder.resolve(nodeName).resolve("db");
            Path logsFolder = clusterFolder.resolve(nodeName).resolve("logs");
            if (dbFolder.toFile().exists())
                FileUtils.deleteDirectory(dbFolder.toFile());
            if (logsFolder.toFile().exists())
                FileUtils.deleteDirectory(logsFolder.toFile());
            if (!dbFolder.toFile().exists())
                writer.accept(success("Cluster db and logs folders deleted. %s, %s", clusterName, nodeName));

        } else {
            writer.accept(error("Cluster folder not found : %s", clusterFolder));
        }
    }

    public Path getClusterFolder(String clusterName) {
        return Path.of(clusterConfig.getClusterHome(), clusterName);
    }

    public Path getPoolKeys(String clusterName) {
        return Path.of(clusterConfig.getPoolKeysHome(), clusterName);
    }

    public boolean createNodeClusterFolder(String clusterName, ClusterInfo clusterInfo, boolean overwrite, Consumer<String> writer) throws IOException {
        if(!checkCardanoNodeBin(writer)) return false;

        Path destPath = getClusterFolder(clusterName);
        if (Files.exists(destPath) && !overwrite) {
            writer.accept(error(destPath.toAbsolutePath().toString() + " already exists"));
            return true;
        } else {
            if (!Files.exists(destPath))
                destPath = Files.createDirectories(destPath);

            if (!Files.exists(destPath)) {
                writer.accept(error("Directory could not be created"));
                return false;
            }

            if (overwrite) {
                //cleanup
                FileUtils.deleteDirectory(destPath.toFile());
                writer.accept(success("Delete existing folder"));
            }

            writer.accept(success("Create cluster folder !!!"));

            //Copy localclusterzip to tmp folder
            Resource resource = resourceLoader.getResource(CLASSPATH_LOCALCLUSTER_ZIP);
            if (log.isDebugEnabled())
                log.debug("Extract localcluster.zip file");

            Path tempLocalCluster = Files.createTempDirectory("localcluster");

            try(ZipInputStream zin = new ZipInputStream(resource.getInputStream())) {
                ZipUtil.unzipFolderFromInputStream(tempLocalCluster.toFile().toPath(), zin );
            }

            if (log.isDebugEnabled())
                log.debug("Local cluster folder : " + tempLocalCluster);

            Path sourcePath = tempLocalCluster.resolve(BABBAGE_TEMPLATE_PATH);
            copy(sourcePath, destPath);
            FileUtils.deleteDirectory(tempLocalCluster.toFile());

            double activeCoeff = clusterInfo.getSlotLength() / clusterInfo.getBlockTime();
            //Update configuration
            updatePorts(destPath, clusterInfo.getNodePort());

            //Update genesis
            updateGenesis(destPath, clusterInfo.getEra(), clusterInfo.getSlotLength(), activeCoeff, clusterInfo.getEpochLength(), clusterInfo.getProtocolMagic(), writer);

            //Update P2P configuration
            updateConfiguration(destPath, clusterInfo, writer);

            updateSubmitApiFiles(destPath, clusterInfo.getProtocolMagic(), clusterInfo.getSubmitApiPort());

            //Update node config
            saveClusterInfo(destPath, clusterInfo);

            writer.accept(success("Update ports"));
            writer.accept(success("Create Cluster : %s", clusterName));

            publisher.publishEvent(new ClusterCreated(clusterName));

            return true;
        }

    }

    private boolean checkCardanoNodeBin(Consumer<String> writer) throws IOException {
        Path binFolder = Path.of(clusterConfig.getCLIBinFolder());
        if (!Files.exists(binFolder))
            Files.createDirectories(binFolder);

        String cardanoNodeCLI = OSUtil.getOperatingSystem() == OSUtil.OS.WINDOWS ? "cardano-node.exe" : "cardano-node";
        Path cardanoNodeCli = binFolder.resolve(cardanoNodeCLI); //TODO -- Handle for windows
        if (Files.exists(cardanoNodeCli))
            return true;

        writer.accept(error("cardno-node binary is not found in %s", clusterConfig.getCLIBinFolder()));
        writer.accept(error("Please download and copy cardano-node, cardano-cli, cardano-submit-api executables (1.35.3 or later) to %s and try again (Set execute permission if required)", clusterConfig.getCLIBinFolder()));
        return false;
        //Download

//        URL url = new URL("https://hydra.iohk.io/build/17428186/download/1/cardano-node-1.35.3-macos.tar.gz");
//        FileUtils.copyURLToFile(url, Path.of(clusterConfig.getCLIBinFolder()).toFile());
    }

    private void updateGenesis(Path clusterFolder, Era era, double slotLength, double activeSlotsCoeff, int epochLength, long protocolMagic, Consumer<String> writer) throws IOException {

        Path srcShelleyGenesisFile = null;
        Path srcByronGenesisFile = null;
        Path srcAlonzoGenesisFile = null;
        Path srcConwayGenesisFile = null;
        if (era == Era.Babbage) {
            srcByronGenesisFile = clusterFolder.resolve("genesis-templates").resolve("byron-genesis.json");
            srcShelleyGenesisFile = clusterFolder.resolve("genesis-templates").resolve("shelley-genesis.json");
            srcAlonzoGenesisFile = clusterFolder.resolve("genesis-templates").resolve("alonzo-genesis.json");
            srcConwayGenesisFile = clusterFolder.resolve("genesis-templates").resolve("conway-genesis.json");
        } else if (era == Era.Conway) {
            srcByronGenesisFile = clusterFolder.resolve("genesis-templates").resolve("byron-genesis.json");
            srcShelleyGenesisFile = clusterFolder.resolve("genesis-templates").resolve("shelley-genesis.json");
            srcAlonzoGenesisFile = clusterFolder.resolve("genesis-templates").resolve("alonzo-genesis.json.conway");
            srcConwayGenesisFile = clusterFolder.resolve("genesis-templates").resolve("conway-genesis.json.conway");
        }

        Path destByronGenesisFile = clusterFolder.resolve("node").resolve("genesis").resolve("byron-genesis.json");
        Path destShelleyGenesisFile = clusterFolder.resolve("node").resolve("genesis").resolve("shelley-genesis.json");
        Path destAlonzoGenesisFile = clusterFolder.resolve("node").resolve("genesis").resolve("alonzo-genesis.json");
        Path destConwayGenesisFile = clusterFolder.resolve("node").resolve("genesis").resolve("conway-genesis.json");

        Map<String, String> values = new HashMap<>();
        values.put("networkId", networkId);
        values.put("maxKESEvolutions", String.valueOf(maxKESEvolutions));
        values.put("securityParam", String.valueOf(securityParam));
        values.put("slotLength", String.valueOf(slotLength));
        values.put("slotsPerKESPeriod", String.valueOf(slotsPerKESPeriod));
        values.put("activeSlotsCoeff", String.valueOf(activeSlotsCoeff));
        values.put("protocolMagic", String.valueOf(protocolMagic));
        values.put("epochLength", String.valueOf(epochLength));
        values.put("updateQuorum", String.valueOf(updateQuorum));


        //Update Genesis files
        try {
            templateEngineHelper.replaceValues(srcByronGenesisFile, destByronGenesisFile, values);
            templateEngineHelper.replaceValues(srcShelleyGenesisFile, destShelleyGenesisFile, values);
            templateEngineHelper.replaceValues(srcAlonzoGenesisFile, destAlonzoGenesisFile, new HashMap<>());
            templateEngineHelper.replaceValues(srcConwayGenesisFile, destConwayGenesisFile, new HashMap<>());
        } catch (Exception e) {
            throw new IOException(e);
        }

        writer.accept(success("Slot length updated in genesis.json"));
    }

    private void updatePorts(Path destPath, int port) throws IOException {
        String nodeName = NODE_FOLDER_PREFIX;
        String nodeScript =  getOSSpecificScriptName(nodeName);
        Path nodeRunScript = destPath.resolve(nodeName).resolve(nodeScript);
        int nodePort = port;

        Map<String, String> values = new HashMap<>();
        values.put("BIN_FOLDER", clusterConfig.getCLIBinFolder());
        values.put("port", String.valueOf(nodePort));

        //Update Node run script
        try {
            templateEngine.replaceValues(nodeRunScript, values);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void updateSubmitApiFiles(Path destPath, long protocolMagic, int submitApiPort) throws IOException {
        Map<String, String> values = new HashMap<>();
        values.put("BIN_FOLDER", clusterConfig.getCLIBinFolder());
        values.put("SUBMIT_API_PORT", String.valueOf(submitApiPort));
        values.put("PROTOCOL_MAGIC", String.valueOf(protocolMagic));

        //Update submit api script
        Path submitApiSh = destPath.resolve("submit-api.sh");
        try {
            templateEngine.replaceValues(submitApiSh, values);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void updateConfiguration(Path clusterFolder, ClusterInfo clusterInfo, Consumer<String> writer) throws IOException {
        Path configurationPath = clusterFolder.resolve("templates").resolve("configuration.yaml");
//        if (clusterInfo.getEra() == null || clusterInfo.getEra() == Era.Babbage) {
//            configurationPath = clusterFolder.resolve("configuration.yaml");
//        } else if (clusterInfo.getEra() == Era.Conway) {
//            writer.accept(success("Updating configuration.yaml for conway era"));
//            configurationPath = clusterFolder.resolve("configuration.yaml.conway");
//        }

        Path destConfigPath = clusterFolder.resolve("node").resolve("configuration.yaml");
        boolean enableP2P = clusterInfo.isP2pEnabled();

        Map values = new HashMap<>();
        values.put("enableP2P", String.valueOf(enableP2P));
        values.put("peerSharing", peerSharing);
        values.put("prometheusPort", String.valueOf(clusterInfo.getPrometheusPort()));
        values.put("conway_era", clusterInfo.getEra() == Era.Conway ? Boolean.TRUE : Boolean.FALSE);

        //Update Configuration file
        try {
            templateEngineHelper.replaceValues(configurationPath, destConfigPath, values);
        } catch (Exception e) {
            throw new IOException(e);
        }

        writer.accept(success("Updated configuration.yaml"));
    }

    public ClusterInfo getClusterInfo(String clusterName) throws IOException {
        Path clusterFolder = getClusterFolder(clusterName);
        if (!Files.exists(clusterFolder)) {
            throw new IllegalStateException("Cluster not found : " + clusterName);
        }

        String clusterInfoPath = clusterFolder.resolve(ClusterConfig.CLUSTER_INFO_FILE).toAbsolutePath().toString();
        try {
            return objectMapper.readValue(new File(clusterInfoPath), ClusterInfo.class);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void saveClusterInfo(Path clusterFolder, ClusterInfo clusterInfo) throws IOException {
        if (!Files.exists(clusterFolder)) {
            throw new IllegalStateException("Cluster folder not found - "  + clusterFolder);
        }

        String socketPath = clusterFolder.resolve(NODE_FOLDER_PREFIX).resolve("node.sock").toString();
        clusterInfo.setSocketPath(socketPath);

        String clusterInfoPath = clusterFolder.resolve(ClusterConfig.CLUSTER_INFO_FILE).toAbsolutePath().toString();
        objectMapper.writer(new DefaultPrettyPrinter()).writeValue(new File(clusterInfoPath), clusterInfo);
    }

    private void copy(Path sourceDir, Path destDir) throws IOException {
        Files.walk(sourceDir)
                .forEach(source -> {
                    Path destination = Paths.get(destDir.toAbsolutePath().toString(), sourceDir.relativize(source).toString());
                    try {
                        if (!Files.exists(destination)) {
                            Files.copy(source, destination);
                            if (log.isDebugEnabled())
                                log.debug("Copied : " + source.toAbsolutePath().toString());

                            if (OSUtil.getOperatingSystem() != OSUtil.OS.WINDOWS) {
                                if (destination.getFileName().toString().endsWith("key") || destination.getFileName().toString().endsWith("cert")) {
                                    Files.setPosixFilePermissions(destination, Set.of(OWNER_READ, PosixFilePermission.OWNER_WRITE));
                                } else if (destination.getFileName().toString().endsWith("sh") || destination.getFileName().toString().endsWith("bat")) {
                                    Files.setPosixFilePermissions(destination, Set.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, GROUP_EXECUTE, GROUP_READ));
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }


    public String getOSSpecificScriptName(String script) {
        return OSUtil.getOperatingSystem() == OSUtil.OS.WINDOWS ? script + ".bat" : script + ".sh";
    }

    public void logs(Consumer<String> writer) {
        clusterStartService.showLogs(writer);
    }

    public void submitApiLogs(Consumer<String> writer) {
        clusterStartService.showSubmitApiLogs(writer);
    }

    public void ltail(String clusterName, boolean showMint, boolean showInputs, boolean showMetadata,
                      boolean showDatumhash, boolean showInlineDatum, boolean grouping, OutputFormatter outputFormatter) throws IOException {
        try {
            ClusterInfo clusterInfo = getClusterInfo(clusterName);
            blockStreamerService.tail("localhost", clusterInfo.getNodePort(), clusterInfo.getProtocolMagic(), showMint, showInputs, showMetadata, showDatumhash, showInlineDatum, grouping, outputFormatter);
        } catch (IOException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            if (log.isDebugEnabled())
                log.error("Interrupt exception", ex);
        }
    }

    public boolean isFirstRunt(String clusterName) {
        return clusterStartService.checkIfFirstRun(getClusterFolder(clusterName));
    }

}
