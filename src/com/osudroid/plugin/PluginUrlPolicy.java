package com.osudroid.plugin;

import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;

/**
 * Network policy shared by the Lua plugin HTTP API and its tests.
 *
 * <p>Plugins may talk to public HTTPS services, but must not be able to poke
 * localhost, the LAN, link-local metadata endpoints or private address ranges.
 * That keeps a downloaded plugin from scanning the user's network or calling
 * router/admin interfaces while still allowing normal web APIs.</p>
 */
public final class PluginUrlPolicy {

    private static final int MAX_URL_LENGTH = 2048;

    private PluginUrlPolicy() {}

    /** Returns null when the URL is allowed, otherwise a short reason. */
    public static String validate(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "URL is empty";
        }
        if (raw.length() > MAX_URL_LENGTH) {
            return "URL is too long";
        }

        final URI uri;
        try {
            uri = new URI(raw);
        } catch (Exception e) {
            return "Malformed URL";
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return "Only HTTPS URLs are allowed";
        }
        if (uri.getUserInfo() != null) {
            return "URL credentials are not allowed";
        }

        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            return "URL has no host";
        }
        host = host.toLowerCase(Locale.ROOT);
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        if (
            host.equals("localhost") ||
            host.endsWith(".localhost") ||
            host.endsWith(".local") ||
            host.endsWith(".internal") ||
            host.endsWith(".home.arpa")
        ) {
            return "Local hosts are not allowed";
        }

        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isBlockedAddress(address)) {
                    return "Private or local addresses are not allowed";
                }
            }
        } catch (Exception e) {
            return "Host could not be resolved";
        }
        return null;
    }

    public static boolean isAllowed(String url) {
        return validate(url) == null;
    }

    /** Visible for tests: loopback, LAN, link-local and other non-public ranges. */
    public static boolean isBlockedAddress(InetAddress address) {
        if (address == null) {
            return true;
        }
        if (
            address.isAnyLocalAddress() ||
            address.isLoopbackAddress() ||
            address.isLinkLocalAddress() ||
            address.isSiteLocalAddress() ||
            address.isMulticastAddress()
        ) {
            return true;
        }

        final byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            return isBlockedIpv4(bytes);
        }

        if (bytes.length == 16) {
            // IPv4-mapped (::ffff:a.b.c.d) and IPv4-compatible (::a.b.c.d)
            // addresses route to the embedded IPv4 endpoint. HttpURLConnection
            // accepts them, so checking only the IPv6 prefix would let a plugin
            // reach localhost/LAN through e.g. https://[::ffff:127.0.0.1]/.
            boolean embeddedIpv4 = true;
            for (int i = 0; i < 10; i++) {
                if (bytes[i] != 0) {
                    embeddedIpv4 = false;
                    break;
                }
            }
            if (embeddedIpv4) {
                final int prefix = ((bytes[10] & 0xff) << 8) | (bytes[11] & 0xff);
                if (prefix == 0xffff || prefix == 0) {
                    final byte[] ipv4 = new byte[4];
                    System.arraycopy(bytes, 12, ipv4, 0, 4);
                    // :: and ::1 are already rejected by isAnyLocalAddress() /
                    // isLoopbackAddress() above; anything else with this shape
                    // is a real embedded IPv4 address.
                    return isBlockedIpv4(ipv4);
                }
            }
            // IPv6 unique-local fc00::/7.
            return (bytes[0] & 0xfe) == 0xfc;
        }
        return true;
    }

    private static boolean isBlockedIpv4(byte[] bytes) {
        final int a = bytes[0] & 0xff;
        final int b = bytes[1] & 0xff;
        // 0.0.0.0/8, carrier-grade NAT, benchmarking, reserved and broadcast.
        if (a == 0 || a >= 240) {
            return true;
        }
        if (a == 100 && b >= 64 && b <= 127) {
            return true;
        }
        if (a == 198 && (b == 18 || b == 19)) {
            return true;
        }
        if (a == 192 && b == 0 && (bytes[2] & 0xff) == 0) {
            return true;
        }
        return a == 255 && b == 255 && (bytes[2] & 0xff) == 255 && (bytes[3] & 0xff) == 255;
    }
}
