package org.watermedia.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Small HTTP(S) helper built on the JDK {@link HttpURLConnection}, with no external dependencies so it can
 * live in the shared tools module. Follows same-protocol (https&rarr;https) redirects, which is all the
 * GitHub/Codeberg endpoints it is used for ever need.
 */
public class NetTool {
    private static final String USER_AGENT = "WaterMedia/3.0.0"; // GITHUB'S API 403s WITHOUT A User-Agent
    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final int READ_TIMEOUT_MS = 60_000; // PER-READ SOCKET TIMEOUT: BOUNDS A STALLED STREAM
    private static final int MAX_RESPONSE_BYTES = 16 * 1024 * 1024; // BOUND IN-MEMORY GET RESPONSES (JSON/TEXT)

    // GETs url AND RETURNS THE RESPONSE BODY AS A UTF-8 STRING (FOR SMALL JSON/TEXT — E.G. A RELEASES API
    // PAYLOAD). FOLLOWS REDIRECTS; THROWS ON A NON-200 STATUS OR A BODY LARGER THAN MAX_RESPONSE_BYTES.
    public static String get(final String url) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setInstanceFollowRedirects(true);
        try {
            final int code = conn.getResponseCode();
            if (code != 200) {
                throw new IOException("GET failed (HTTP " + code + "): " + url);
            }
            try (final InputStream in = conn.getInputStream()) {
                return new String(IOTool.readLimited(in, MAX_RESPONSE_BYTES, conn.getContentLengthLong()), StandardCharsets.UTF_8);
            }
        } finally {
            conn.disconnect();
        }
    }

    // DOWNLOADS url INTO dest, FOLLOWING REDIRECTS. THROWS ON A NON-200 STATUS, A FAILED WRITE, OR A
    // SIZE MISMATCH AGAINST Content-Length (TRUNCATION); DELETES A HALF-WRITTEN FILE SO A RETRY
    // RE-DOWNLOADS INSTEAD OF TRUSTING A TRUNCATED FILE.
    public static void download(final String url, final Path dest) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setInstanceFollowRedirects(true);
        try {
            final int code = conn.getResponseCode();
            if (code != 200) {
                throw new IOException("Download failed (HTTP " + code + "): " + url);
            }
            final long expected = conn.getContentLengthLong();
            // IOTool.write CLOSES THE STREAM AND RETURNS false ON AN IO ERROR
            if (!IOTool.write(conn.getInputStream(), dest.toFile())) {
                Files.deleteIfExists(dest);
                throw new IOException("Failed to write " + dest + " from " + url);
            }
            if (expected >= 0) { // -1 WHEN CHUNKED / UNKNOWN — NOTHING TO CHECK AGAINST
                final long actual = Files.size(dest);
                if (actual != expected) {
                    Files.deleteIfExists(dest);
                    throw new IOException("Truncated download (" + actual + "/" + expected + " bytes): " + url);
                }
            }
        } finally {
            conn.disconnect();
        }
    }
}
