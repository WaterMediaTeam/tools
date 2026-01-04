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
        } catch (final IOException e) {
            return null;
        }
    }

    public static boolean copy(final File inFile, final File outFile) {
        try (final var in = Files.newInputStream(inFile.toPath())) {
            return write(in, outFile);
        } catch (final IOException e) {
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

    public static boolean jarExtractZip(final String resource, final File output, final ClassLoader from) throws Exception {
        return jarExtractZip(jarOpenFile(resource, from), output);
    }

    // I MADE THIS THROW EXCEPTION BECAUSE THIS IS A WAY MORE COMPLEX TASK, AND THE CALLER SHOULD HANDLE IT
    public static boolean jarExtractZip(final InputStream is, final File output) throws Exception {
        try (final var in = new BufferedInputStream(is, BUFFER_SIZE); final var zip = new ZipInputStream(in)) {
            ZipEntry entry;
            final byte[] buffer = new byte[BUFFER_SIZE]; // THIS IS OUTSIDE THE LOOP TO AVOID MULTIPLE ALLOCATIONS PER ENTRY
            while ((entry = zip.getNextEntry()) != null) {
                final File outFile = new File(output, entry.getName());
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
    public static String jarReadZip(final String zipResource, final String fileInZip, final ClassLoader from) {
        return jarReadZip(jarOpenFile(zipResource, from), fileInZip);
    }

    public static String jarReadZip(final InputStream in, final String fileInZip) {
        try (in; final var zip = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals(fileInZip)) {
                    final byte[] data = zip.readAllBytes();
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
            final String version = new Manifest(jarOpenFile("/META-INF/MANIFEST.MF", IOTool.class.getClassLoader())).getMainAttributes().getValue("version");
            return version == null ? "3.0.0-unknown" : version;
        } catch (final IOException e) {
            throw new RuntimeException("Failed to read self manifest", e);
        }
    }

    public static String jarRead(final String path) {
        try (final var in = jarOpenFile(path, IOTool.class.getClassLoader())) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final Exception e) {
            return null;
        }
    }

    public static InputStream jarOpenFile(final String name) {
        return jarOpenFile(name, IOTool.class.getClassLoader());
    }

    public static InputStream jarOpenFile(final String source, final ClassLoader classLoader) {
        var is = classLoader.getResourceAsStream(source);
        if (is == null && source.startsWith("/")) is = classLoader.getResourceAsStream(source.substring(1));
        return is;
    }

}
