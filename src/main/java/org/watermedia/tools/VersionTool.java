package org.watermedia.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encapsulation of version information and related behaviors.
 */
public class VersionTool implements Comparable<VersionTool> {
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)[\\-_\\s]?(.*)");

    public final String version;
    public final int major;
    public final int minor;
    public final int revision;
    public final String extra;

    /**
     * Create a new version.
     *
     * @param version version string
     */
    public VersionTool(final String version) {
        this.version = version == null ? "" : version;
        final Matcher matcher = VERSION_PATTERN.matcher(this.version);
        if (matcher.matches()) {
            this.major = Integer.parseInt(matcher.group(1));
            this.minor = Integer.parseInt(matcher.group(2));
            this.revision = Integer.parseInt(matcher.group(3));
            if (matcher.groupCount() > 3) {
                this.extra = matcher.group(4);
            } else {
                this.extra = null;
            }
        } else {
            this.major = 0;
            this.minor = 0;
            this.revision = 0;
            this.extra = null;
        }
    }
    /**
     * Test whether this version is at least the required version.
     *
     * @param required required version
     * @return <code>true</code> if this version is at least (equal to or greater than) the required version
     */
    public boolean atLeast(final VersionTool required) {
        return this.compareTo(required) >= 0;
    }

    /**
     * Test whether this version is in range of the required versions.
     *
     * @param min required version
     * @return <code>true</code> if this version is at least (equal to or greater than) the required version
     */
    public boolean inRange(final VersionTool min, final VersionTool max) {
        return this.compareTo(min) >= 0 && this.compareTo(max) < 0;
    }

    /**
     * Check if version is 0.0.0, indicating no version or null argument version.
     * @return true if version is 0.0.0, false otherwise
     */
    public boolean isZero() {
        return this.major == 0 && this.minor == 0 && this.revision == 0 && (this.extra == null || this.extra.isEmpty());
    }

    @Override
    public int compareTo(final VersionTool o) {
        int delta = this.major - o.major;
        if (delta == 0) {
            delta = this.minor - o.minor;
            if (delta == 0) {
                delta = this.revision - o.revision;
            }
        }
        return delta;
    }

    @Override
    public String toString() {
        return this.isZero() ? "<Not Found>" : this.version;
    }
}