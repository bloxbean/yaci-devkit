package com.bloxbean.cardano.yacicli.commands.localcluster.peer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.success;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.writeLn;

public class ClusterFileUtil {
    public static File downloadClusterFiles(String downloadUrl) throws IOException {
        try {
            // Create a URL object from the download URL
            URL url = new URL(downloadUrl);

            // Open a connection to the download URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Get the input stream from the connection
            InputStream inputStream = connection.getInputStream();

            // Create a temporary file to store the downloaded zip file
            File tempFile = File.createTempFile("downloaded", ".zip");

            // Copy the downloaded zip file to the temporary file
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Close the input stream and connection
            inputStream.close();
            connection.disconnect();

            File extractDir = Files.createTempDirectory("extracted").toFile();

            // Extract the files from the zip file
            try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(tempFile))) {
                ZipEntry entry = zipInputStream.getNextEntry();
                while (entry != null) {
                    String filePath = extractDir.getAbsolutePath() + File.separator + entry.getName();
                    if (!entry.isDirectory()) {
                        // Create directories if they don't exist
                        new File(filePath).getParentFile().mkdirs();

                        // Extract the file
                        try (OutputStream outputStream = new FileOutputStream(filePath)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        }
                    }

                    zipInputStream.closeEntry();
                    entry = zipInputStream.getNextEntry();
                }
            }

            System.out.println("Downloaded zip file extracted to: " + extractDir.getAbsolutePath());
            return extractDir;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean copyFolder(String sourceDirectory, String destinationDirectory) {
        try {
            // Create the destination directory if it doesn't exist
            Files.createDirectories(Paths.get(destinationDirectory));

            // Copy the source directory to the destination directory
            Files.walkFileTree(Paths.get(sourceDirectory), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path targetDir = Paths.get(destinationDirectory, dir.toString().substring(sourceDirectory.length()));
                    Files.copy(dir, targetDir, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path targetFile = Paths.get(destinationDirectory, file.toString().substring(sourceDirectory.length()));
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });

            writeLn(success("Directory copied successfully!"));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}

