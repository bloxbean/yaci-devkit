package com.bloxbean.cardano.yacicli.localcluster.yacistore;

import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.*;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.success;

@Component
@RequiredArgsConstructor
@Getter
public class YaciStoreCustomDbHelper {
    private final ClusterService clusterService;

    //External postgres db configuration
    @Value("${yaci.store.db.url:#{null}}")
    private String storeDbUrl;
    @Value("${yaci.store.db.username:#{null}}")
    private String storeDbUsername;
    @Value("${yaci.store.db.password:#{null}}")
    private String storeDbPassword;

    public void dropDatabase(String clusterName) {
        if (storeDbUrl == null || storeDbUrl.isEmpty() || !storeDbUrl.contains("postgresql")) {
            //Looks like default db.
            Path clusterFolder = clusterService.getClusterFolder(clusterName);
            Path nodeFolder = clusterFolder.resolve("node");
            String dbDir = "yaci_store";
            Path dbPath = nodeFolder.resolve(dbDir);

            writeLn(info("Deleting Yaci Store db folder : " + dbPath.toFile().getAbsolutePath()));
            if (dbPath.toFile().exists()) {
                try {
                    FileUtils.deleteDirectory(dbPath.toFile());
                    writeLn(success("Yaci Store db folder deleted successfully"));
                } catch (IOException e) {
                    writeLn(error("Yaci store db could not be deleted : " + dbPath.toAbsolutePath()));
                }
            }

            return;
        }

        if(storeDbUrl != null && storeDbUrl.contains("postgres")) {
            dropPostgresSqlSchema(storeDbUrl, storeDbUsername, storeDbPassword);
        }
    }

    private void dropPostgresSqlSchema(String url, String user, String password) {
        Connection conn = null;
        try {
            Class.forName("org.postgresql.Driver");

            // Establish the connection
            conn = DriverManager.getConnection(url, user, password);

            String schema = conn.getSchema();

            writeLn("Got schema: " + schema);
            // Try to drop the schema
            String dropSchemaSQL = "DROP SCHEMA " + schema + " CASCADE";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(dropSchemaSQL);
                writeLn("Schema '" + schema + "' dropped successfully.");
            } catch (SQLException e) {
                // Handle specific SQLState codes
                String sqlState = e.getSQLState();
                if ("42501".equals(sqlState)) {
                    writeLn(error("Insufficient privileges to drop the schema '" + schema + "'."));
                } else if ("3F000".equals(sqlState)) {
                    writeLn(error("Schema '" + schema + "' does not exist."));
                } else {
                    writeLn(error("An error occurred while trying to drop the schema:"));
                    e.printStackTrace();
                }
            }

        } catch (ClassNotFoundException e) {
            writeLn(error("PostgreSQL JDBC Driver not found."));
            e.printStackTrace();
        } catch (SQLException e) {
            writeLn(error("Database connection error occurred."));
            e.printStackTrace();
        } finally {
            // Close the connection
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
}
