package com.bloxbean.cardano.yacicli.commands.localcluster;

import com.bloxbean.cardano.yaci.core.util.OSUtil;
import com.bloxbean.cardano.yacicli.commands.tail.BlockStreamerService;
import com.bloxbean.cardano.yacicli.output.OutputFormatter;
import com.bloxbean.cardano.yacicli.util.TemplateEngine;
import com.bloxbean.cardano.yacicli.util.ZipUtil;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.context.WebServerApplicationContext;
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
import java.util.stream.IntStream;
import java.util.zip.ZipInputStream;

import static com.bloxbean.cardano.yacicli.commands.localcluster.ClusterConfig.NODE_FOLDER_PREFIX;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;
import static java.nio.file.attribute.PosixFilePermission.*;

@Component
@Slf4j
public class ClusterService {

    public static final String BABBAGE_TEMPLATE_PATH = "localcluster/templates/babbage";
    public static final String CLASSPATH_LOCALCLUSTER_ZIP = "classpath:localcluster.zip";
    private ClusterConfig clusterConfig;
    private ClusterStartService clusterStartService;
    private BlockStreamerService blockStreamerService;

    @Autowired
    ResourceLoader resourceLoader;

    @Autowired
    TemplateEngine templateEngine;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private WebServerApplicationContext server;
//    @Autowired
//    private ReactiveWebServerApplicationContext server;

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

    public void startCluster(String clusterName) {
        try {
            clusterStartService.startCluster(getClusterInfo(clusterName), getClusterFolder(clusterName), msg -> writeLn(msg));
            writeLn(info("Swagger Url to interact with the cluster's node : " + "http://localhost:" + server.getWebServer().getPort() +"/swagger-ui.html"));
        } catch (Exception e) {
            System.out.println("Error a creating local cluster");
            throw new RuntimeException(e);
        }
    }

    public void stopCluster(Consumer<String> writer) {
        clusterStartService.stopCluster(writer);
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
            for (int i=0; i<3; i++) {
                String nodeName = NODE_FOLDER_PREFIX + (i+1);
                Path dbFolder = clusterFolder.resolve(nodeName).resolve("db");
                Path logsFolder = clusterFolder.resolve(nodeName).resolve("logs");
                if (dbFolder.toFile().exists())
                    FileUtils.deleteDirectory(dbFolder.toFile());
                if (logsFolder.toFile().exists())
                    FileUtils.deleteDirectory(logsFolder.toFile());
                if (!dbFolder.toFile().exists())
                writer.accept(success("Cluster db and logs folders deleted. %s, %s", clusterName, nodeName));
            }

        } else {
            writer.accept(error("Cluster folder not found : %s", clusterFolder));
        }
    }

    public Path getClusterFolder(String clusterName) {
        return Path.of(clusterConfig.getClusterHome(), clusterName);
    }

    public boolean createClusterFolder(String clusterName, int[] ports, int submitApiPort,
                                       double slotLength,  boolean overwrite, Consumer<String> writer) throws IOException {
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

            //Update configuration
            for (int i=1;i<=3;i++) {
                updatePorts(destPath, ports, i);
            }

            //Update genesis
            updateGenesis(destPath, slotLength, writer);

            updateSubmitApiFiles(destPath, submitApiPort);

            //Update node config
            saveClusterInfo(destPath, ports, submitApiPort, slotLength);

            writer.accept(success("Update ports"));
            writer.accept(success("Create Cluster : %s", clusterName));

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

    private void updateGenesis(Path clusterFolder, double slotLength, Consumer<String> writer) throws IOException {
        //Shelley genesis file
        Path genesisFile = clusterFolder.resolve("genesis").resolve("shelley").resolve("genesis.json");
        Map<String, String> values = new HashMap<>();
        values.put("slotLength", String.valueOf(slotLength));

        //Update Node run script
        try {
            templateEngine.replaceValues(genesisFile, values);
        } catch (Exception e) {
            throw new IOException(e);
        }

        writer.accept(success("Slot length updated in genesis.json"));
    }

    private void updatePorts(Path destPath, int[] ports, int nodeNo) throws IOException {
        String nodeName = NODE_FOLDER_PREFIX + nodeNo;
        String nodeScript =  getOSSpecificScriptName(nodeName);
        Path nodeRunScript = destPath.resolve(nodeName).resolve(nodeScript);
        int nodePort = ports[nodeNo-1]; //Node no = 1..3

        Map<String, String> values = new HashMap<>();
        values.put("BIN_FOLDER", clusterConfig.getCLIBinFolder());
        values.put("port", String.valueOf(nodePort));

        int nodeCount = 1;
        for (int port: ports) { //Set other node port variables
            if (port != nodePort)
                values.put("otherPort" + nodeCount++, String.valueOf(port));
        }

        //Update Node run script
        try {
            templateEngine.replaceValues(nodeRunScript, values);
        } catch (Exception e) {
            throw new IOException(e);
        }

        //Update topology.json
        Path topology = destPath.resolve(nodeName).resolve("topology.json");
        try {
            templateEngine.replaceValues(topology, values);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void updateSubmitApiFiles(Path destPath, int submitApiPort) throws IOException {
        Map<String, String> values = new HashMap<>();
        values.put("BIN_FOLDER", clusterConfig.getCLIBinFolder());
        values.put("SUBMIT_API_PORT", String.valueOf(submitApiPort));
        values.put("PROTOCOL_MAGIC", String.valueOf(42)); //Fixed for now

        //Update submit api script
        Path submitApiSh = destPath.resolve("submit-api.sh");
        try {
            templateEngine.replaceValues(submitApiSh, values);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private ClusterInfo saveClusterInfo(Path clusterFolder, int[] nodePorts, int submitApiPort, double slotLength) throws IOException {
        String[] socketPaths = new String[3];
        IntStream.range(0,3).forEach(i -> {
                socketPaths[i] = clusterFolder.resolve(NODE_FOLDER_PREFIX + (i+1)).resolve("node.sock").toString();
        });

        //Create node_config
        ClusterInfo clusterInfo = ClusterInfo.builder()
                .nodePorts(nodePorts)
                .submitApiPort(submitApiPort)
                .socketPaths(socketPaths)
                .build();

        String clusterInfoPath = clusterFolder.resolve(ClusterConfig.CLUSTER_INFO_FILE).toAbsolutePath().toString();
        objectMapper.writer(new DefaultPrettyPrinter()).writeValue(new File(clusterInfoPath), clusterInfo);

        return clusterInfo;
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


    private String getOSSpecificScriptName(String script) {
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
            blockStreamerService.tail("localhost", clusterInfo.getNodePorts()[0], clusterInfo.getProtocolMagic(), showMint, showInputs, showMetadata, showDatumhash, showInlineDatum, grouping, outputFormatter);
        } catch (IOException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            if (log.isDebugEnabled())
                log.error("Interrupt exception", ex);
        }
    }

}
