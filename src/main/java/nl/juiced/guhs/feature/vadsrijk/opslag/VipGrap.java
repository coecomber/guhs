package nl.juiced.guhs.feature.vadsrijk.opslag;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;

/**
 * U-OPSLAGVIP: the "WORD GUH VIP++" joke. A PARODY of microtransaction pop-ups: nothing is ever paid with real money, the buttons
 * do nothing. The FIRST time an account tries to buy a storage expansion (knapzak or bank), the storage screen shows a
 * fullscreen clickbait deal for ~10 s ({@code client.VipGrapOverlay}), then a normal dialogue offers the real coin price.
 * <p>The flag lives in {@link OpslagData#vipGrapGezien} (account-wide attachment): the client tells the server with menu button
 * {@link OpslagMenu#KNOP_VIP_GEZIEN} the moment the joke starts, so it never comes back (not even after a crash mid-joke).
 * Dev: {@code /guhs vadsrijk opslag vipgrap} (reset + open the knapzak with the joke at once), {@code vipgrap reset}.
 */
public final class VipGrap {
    /** Players whose next storage screen starts the joke at once (the dev command). */
    private static final Set<UUID> NU = ConcurrentHashMap.newKeySet();

    private VipGrap() {
    }

    public static boolean gezien(ServerPlayer p) {
        return Opslag.data(p).vipGrapGezien;
    }

    /** Menu button: mark as seen. */
    static void markeer(ServerPlayer p) {
        Opslag.data(p).vipGrapGezien = true;
    }

    /** Dev: forget that the joke was seen (the next buy attempt shows it again). */
    public static void reset(ServerPlayer p) {
        Opslag.data(p).vipGrapGezien = false;
    }

    /** Dev: the next storage screen of this player starts the joke right away. */
    static void meteen(ServerPlayer p) {
        NU.add(p.getUUID());
    }

    /** Info for the screen: start the joke at once? (asked once per open) */
    static boolean pakMeteen(ServerPlayer p) {
        return NU.remove(p.getUUID());
    }
}
