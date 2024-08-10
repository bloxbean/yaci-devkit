package com.bloxbean.cardano.yacicli.commands.common;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.*;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@Component
@RequiredArgsConstructor
public class JreResolver {
    public static final String JAVA = "java";
    private static String JRE_MAC_OS_BIN = "jdk-21+35-jre/Contents/Home/bin";
    private static String JRE_LINUX_BIN = "jdk-21+35-jre/bin";

    @Value("${store.custom.jre.folder:#{null}}")
    private String jreFolder;

    public String getJavaCommand() {
        if (jreFolder == null)
            return JAVA;
        else {
            String javaCommand = Paths.get(jreFolder, getJreRelativeBinPath(), JAVA).toString();
            if (Files.exists(Paths.get(javaCommand))) {
                return javaCommand;
            } else {
                writeLn(error("Java command not found in the provided JRE folder: " + jreFolder));
                return JAVA;
            }
        }
    }

    public static String getJreRelativeBinPath() {
        String os = detectOS();
        if ("macos".equals(os)) {
            return JRE_MAC_OS_BIN;
        } else if ("linux".equals(os)) {
            return JRE_LINUX_BIN;
        } else {
            return null;
        }
    }

    public static String resolveJreDownloadUrl() {
            String os = detectOS();
            String arch = detectArch();

            String jreUrl = getJreDownloadUrl(os, arch);
            if (jreUrl == null) {
                writeLn(error("Unsupported OS or architecture"));
                return null;
            }

            writeLn(success("JRE download URL: " + jreUrl));

            return jreUrl;
    }

    private static String detectOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            return "macos";
        } else if (os.contains("linux")) {
            return "linux";
        } else {
            return null;
        }
    }

    private static String detectArch() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.contains("x86_64") || arch.contains("amd64")) {
            return "x64";
        } else if (arch.contains("aarch64")) {
            return "aarch64";
        } else {
            return null;
        }
    }

    private static String getJreDownloadUrl(String os, String arch) {
        // URLs for OpenJDK 21 JRE from the Adoptium (Eclipse Temurin) project.
        if ("linux".equals(os)) {
            if ("x64".equals(arch)) {
                return "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21+35/OpenJDK21U-jre_x64_linux_hotspot_21_35.tar.gz";
            } else if ("aarch64".equals(arch)) {
                return "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21+35/OpenJDK21U-jre_aarch64_linux_hotspot_21_35.tar.gz";
            }
        } else if ("macos".equals(os)) {
            if ("x64".equals(arch)) {
                return "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21+35/OpenJDK21U-jre_x64_mac_hotspot_21_35.tar.gz";
            } else if ("aarch64".equals(arch)) {
                return "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21+35/OpenJDK21U-jre_aarch64_mac_hotspot_21_35.tar.gz";
            }
        }
        return null;
    }
}

