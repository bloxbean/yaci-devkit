package com.bloxbean.cardano.yacicli.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil {

    public static void unzipFolder(Path source, Path target) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(source.toFile()))) {
            unzipFolderFromInputStream(target, zis);
        }
    }

    public static void unzipFolderFromInputStream(Path target, ZipInputStream zis) throws IOException {
        ZipEntry zipEntry = zis.getNextEntry();

        while (zipEntry != null) {
            boolean isDirectory = false;
            if (zipEntry.getName().endsWith(File.separator)) {
                isDirectory = true;
            }

            Path newPath = zipSlipProtect(zipEntry, target);
            if (isDirectory) {
                Files.createDirectories(newPath);
            } else {
                if (newPath.getParent() != null) {
                    if (Files.notExists(newPath.getParent())) {
                        Files.createDirectories(newPath.getParent());
                    }
                }

                Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
    }

    //Zip slip attack protection
    public static Path zipSlipProtect(ZipEntry zipEntry, Path targetDir)
            throws IOException {
        Path targetDirResolved = targetDir.resolve(zipEntry.getName());
        Path normalizePath = targetDirResolved.normalize();
        if (!normalizePath.startsWith(targetDir)) {
            throw new IOException("Bad zip entry:- " + zipEntry.getName());
        }

        return normalizePath;
    }
}
