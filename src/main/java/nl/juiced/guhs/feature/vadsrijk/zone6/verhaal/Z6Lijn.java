package nl.juiced.guhs.feature.vadsrijk.zone6.verhaal;

import java.util.function.BiConsumer;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.luchtschip.Lijnen;
import nl.juiced.guhs.feature.vadsrijk.luchtschip.Luchtschepen;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;

/**
 * Airship line E (U-Z6VERHAAL, ZONE6_PLAN 1.2): Hoogvads' Staarthaven (on the whale's tail) &lt;-&gt; the Luchthaven of Rozenkloof,
 * ~70 s. Added to t-luchtschip's {@link Lijnen#ALLE} at runtime (so Lijnen.java stays untouched), unless a line "z5_z6" is there
 * already (an integration that writes it into Lijnen wins). Harbour POIs {@code haven_z5_z6_vertrek} / {@code haven_z6_z5_vertrek}
 * (looked up like every harbour; the placeholders are {@link Z6VerhaalPlekken#STAARTHAVEN} / {@link Z6VerhaalPlekken#LUCHTHAVEN}).
 * <ul>
 *   <li>The gate ({@link Luchtschepen#SLOT} "z5_z6"): out to the Zijdeland only after the great swim (flag {@code z6_lijn_e_open})
 *       and from level 60 ({@link Z6Verhaal#slot}); back to Hoogvads always.</li>
 *   <li>A landing at Rozenkloof counts the zone-6 arrival ({@link Luchtschepen#AANKOMST} is wrapped, the old handler still runs).</li>
 * </ul>
 */
public final class Z6Lijn {
    public static final String ID = "z5_z6";
    public static final Lijnen.Haven STAARTHAVEN = new Lijnen.Haven("haven_z5_z6_vertrek", Z6VerhaalPlekken.STAARTHAVEN, 270f, "z5");
    // W-SCHEPEN: the Luchthaven's pier runs SOUTH over the gorge rim and is open at its south end: the ship docks south (0), not
    // north (180: it lay over the pier and the tower)
    public static final Lijnen.Haven ROZENKLOOF = new Lijnen.Haven("haven_z6_z5_vertrek", Z6VerhaalPlekken.LUCHTHAVEN, 0f, "z6");
    public static final Lijnen.Lijn LE = new Lijnen.Lijn(ID, "E", STAARTHAVEN, ROZENKLOOF, Z6VerhaalPlekken.LIJN_E_VIA, 3300);
    private static boolean klaar;

    private Z6Lijn() {
    }

    static synchronized void registreer() {
        if (klaar) {
            return;
        }
        klaar = true;
        if (Lijnen.vanId(ID) == null) {
            Lijnen.ALLE.add(LE);
        }
        Luchtschepen.SLOT.putIfAbsent(ID, (p, heen) -> !heen ? null : Z6Verhaal.slot(p) == null ? null : "vadsrijk.luchtschip.slot.z6");
        BiConsumer<ServerPlayer, Lijnen.Haven> oud = Luchtschepen.AANKOMST;
        Luchtschepen.AANKOMST = (p, h) -> {
            oud.accept(p, h);
            if (h.id().equals(ROZENKLOOF.id())) {
                aangekomen(p);
            }
        };
    }

    /** A landing at Rozenkloof: the first time the arrival film (the deck chunks are loaded already), else the meld. */
    static void aangekomen(ServerPlayer p) {
        Z6Haken.start(p);
        if (!Z6Verhaal.aankomstGezien(p)) {
            Z6Verhaal.aankomst(p);
        } else {
            NpcApi.meld(p, "z6", "aangekomen");
        }
    }
}
