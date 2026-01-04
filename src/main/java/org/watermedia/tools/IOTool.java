package org.watermedia.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class IOTool {
    public static final int BUFFER_SIZE = 1024 * 64; // 64 KB
    public static final String VERSION_FILE = "version.cfg";

    public static String getPlatformClassifier() {
        final String os = System.getProperty("os.name").toLowerCase();
        final String arch = System.getProperty("os.arch").toLowerCase();

        final String osName = os.contains("win") ? "windows" : os.contains("mac") ? "macos" : "linux";
        final String archSuffix = (arch.contains("aarch64") || arch.contains("arm64")) ? "-arm64" : "";

        return osName + archSuffix;
    }

    public static String read(final File path) {
        try {
            return Files.readString(path.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean copy(final String jarSource, final File outFile) {
        try (final var in = jarOpenFile(jarSource)) {
            return write(in, outFile);
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean copy(final File inFile, final File outFile) {
        try (final var in = Files.newInputStream(inFile.toPath())) {
            return write(in, outFile);
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean write(final InputStream in, final File outFile) {
        final byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;

        try (in; final var out = Files.newOutputStream(outFile.toPath())) {
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            return true;
        } catch (final IOException e) {
            return false;
        }
    }

    public static int count(final File path) {
        int count = 0;
        if (path == null || !path.exists()) {
            return 0;
        }
        if (path.isDirectory()) {
            final File[] files = path.listFiles();
            if (files != null) {
                for (final File file: files) {
                    count += count(file);
                }
            }
        } else {
            count++;
        }
        return count;
    }

    public static int delete(final File path) {
        int deletedCount = 0;
        if (path == null || !path.exists()) {
            return 0;
        }
        if (path.isDirectory()) {
            final File[] files = path.listFiles();
            if (files != null) {
                for (final File file: files) {
                    deletedCount += delete(file);
                }
            }
        }
        if (path.delete()) {
            deletedCount++;
        }
        return deletedCount;
    }

    public static int deleteSchedule(final File path) {
        int deletedCount = 0;
        if (path == null || !path.exists()) {
            return 0;
        }
        if (path.isDirectory()) {
            final File[] files = path.listFiles();
            if (files != null) {
                for (final File file: files) {
                    deletedCount += deleteSchedule(file);
                }
            }
        }
        path.deleteOnExit();
        return ++deletedCount;
    }

    public static boolean closeQuietly(final AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
                return true;
            } catch (final Exception ignored) {}
        }
        return false;
    }

    // I MADE THIS THROW EXCEPTION BECAUSE THIS IS A WAY MORE COMPLEX TASK, AND THE CALLER SHOULD HANDLE IT
    public static boolean jarExtractZip(final String zipResource, final File outDir) throws Exception {
        try (final var zipStream = new BufferedInputStream(jarOpenFile(zipResource), BUFFER_SIZE); final var zip = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            final byte[] buffer = new byte[BUFFER_SIZE]; // THIS IS OUTSIDE THE LOOP TO AVOID MULTIPLE ALLOCATIONS PER ENTRY
            while ((entry = zip.getNextEntry()) != null) {
                final File outFile = new File(outDir, entry.getName());
                if (entry.isDirectory()) {
                    if (!outFile.exists() && !outFile.mkdirs()) {
                        return false;
                    }
                } else {
                    final File parentDir = outFile.getParentFile();
                    if (!parentDir.exists() && !parentDir.mkdirs()) {
                        return false;
                    }
                    try (final var out = Files.newOutputStream(outFile.toPath())) {
                        int len;
                        while ((len = zip.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                    }
                }
                zip.closeEntry();
            }
            return true;
        }
    }

    // READ A FILE INSIDE A ZIP, WHICH IS INSIDE THE JAR RESOURCE AND RETURN AS STRING
    public static String jarReadZip(final String zipResource, final String fileInZip) {
        try (final var zipStream = jarOpenFile(zipResource);
             final var zip = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals(fileInZip)) {
                    final byte[] data = readAllBytes(zip);
                    return new String(data, StandardCharsets.UTF_8);
                }
                zip.closeEntry();
            }
            return null;
        } catch (final Exception e) {
            return null;
        }
    }

    public static String jarVersion() {
        try {
            final String version = new Manifest(jarOpenFile("/META-INF/MANIFEST.MF")).getMainAttributes().getValue("version");
            return version == null ? "3.0.0-unknown" : version;
        } catch (final IOException e) {
            throw new RuntimeException("Failed to read self manifest", e);
        }
    }

    public static String jarRead(final String from) {
        try (final var in = jarOpenFile(from)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final Exception e) {
            return null;
        }
    }

    public static InputStream jarOpenFile(final String resourcePath) {
        return jarOpenFile(resourcePath, IOTool.class.getClassLoader());
    }

    public static InputStream jarOpenFile(final String source, final ClassLoader classLoader) {
        var is = classLoader.getResourceAsStream(source);
        if (is == null && source.startsWith("/")) is = classLoader.getResourceAsStream(source.substring(1));
        return is;
    }

    public static byte[] readAllBytes(final InputStream in) throws Exception {
        if (in == null) throw new NullPointerException("InputStream is null");

        try (in) {
            return in.readAllBytes();
        }
    }
}
