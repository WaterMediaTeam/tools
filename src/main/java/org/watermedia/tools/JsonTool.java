package org.watermedia.tools;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;

/**
 * Null-safe Gson accessors and lenient URL parsing shared by the platform handlers.
 * One source of truth so a fix (e.g. tolerating a malformed value) lands everywhere at once
 * instead of being copied into each handler under a different name.
 */
public final class JsonTool {
    private JsonTool() {}

    /**
     * Returns the string at {@code key}, or {@code null} when the key is absent or JSON {@code null}.
     */
    public static String str(final JsonObject o, final String key) {
        final JsonElement e = o.get(key);
        return (e == null || e.isJsonNull()) ? null : e.getAsString();
    }

    /**
     * Returns the double at {@code key}, or {@code 0} when the key is absent or JSON {@code null}.
     */
    public static double dbl(final JsonObject o, final String key) {
        final JsonElement e = o.get(key);
        return (e == null || e.isJsonNull()) ? 0d : e.getAsDouble();
    }

    /**
     * Returns the boolean at {@code key}, or {@code false} when the key is absent or JSON {@code null}.
     */
    public static boolean bool(final JsonObject o, final String key) {
        final JsonElement e = o.get(key);
        return e != null && !e.isJsonNull() && e.getAsBoolean();
    }

    /**
     * Returns the int at {@code key}, or {@code null} when the key is absent or JSON {@code null}.
     */
    public static Integer intOrNull(final JsonObject o, final String key) {
        final JsonElement e = o.get(key);
        return (e == null || e.isJsonNull()) ? null : e.getAsInt();
    }

    /**
     * Returns the int at {@code key}, or {@code def} when the key is absent or JSON {@code null}.
     */
    public static int intOr(final JsonObject o, final String key, final int def) {
        final Integer v = intOrNull(o, key);
        return v == null ? def : v;
    }

    /**
     * Parses a URL leniently: {@code null}/empty or unparseable input (e.g. illegal characters such as
     * spaces) yields {@code null}, so one bad URL never aborts a whole result set. Protocol-relative URLs
     * ({@code //host/...}) are promoted to {@code https}.
     * @param url the raw URL string
     * @return the parsed URI, or {@code null} if it is missing or malformed
     */
    public static URI uri(final String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            return URI.create(url.startsWith("//") ? "https:" + url : url);
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Convenience for {@link #uri(String) uri}({@link #str(JsonObject, String) str}(o, key)).
     * @param o the JSON object
     * @param key the key holding the URL string
     * @return the parsed URI, or {@code null} if absent or malformed
     */
    public static URI uri(final JsonObject o, final String key) {
        return uri(str(o, key));
    }
}
