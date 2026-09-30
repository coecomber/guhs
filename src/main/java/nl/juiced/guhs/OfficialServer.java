package nl.juiced.guhs;

import java.util.Locale;
import java.util.Set;

/**
 * 1.0.1: the official 24/7 Guhs server. The client adds it once to the multiplayer server list
 * (client.OfficialServerEntry); this class only holds the facts and the address matching, so it has no client classes
 * in it and can be checked in a gametest.
 */
public final class OfficialServer {
    public static final String NAME = "Guhs Server";
    public static final String ADDRESS = "guhs.nl";
    /** Every address the official server is known under (the SRV record points guhs.nl to play.guhs.nl:25565). */
    public static final Set<String> KNOWN_HOSTS = Set.of("guhs.nl", "play.guhs.nl", "2.28.142.15");

    private OfficialServer() {
    }

    /** Is this server-list address (as typed by a player: any case, maybe with a port or a trailing dot) the official server? */
    public static boolean isOfficial(String address) {
        if (address == null) {
            return false;
        }
        String a = address.trim().toLowerCase(Locale.ROOT);
        int colon = a.lastIndexOf(':');
        if (colon >= 0 && a.indexOf(':') == colon) {          // host:port (not an IPv6 address)
            String port = a.substring(colon + 1);
            if (!port.isEmpty() && !port.equals("25565")) {
                return false;                                 // another port = another server
            }
            a = a.substring(0, colon);
        }
        while (a.endsWith(".")) {
            a = a.substring(0, a.length() - 1);
        }
        return KNOWN_HOSTS.contains(a);
    }
}
