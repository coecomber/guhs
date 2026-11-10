package nl.juiced.guhs.feature.vadsrijk.kleding;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort;

/**
 * Y-COSMETICA: the bridge from the Vadsrijk to survival. Vadsrijk outfits are pure LOOKS at home. Most of it was already in
 * T-KLEDING (the account store {@link KledingData}, the synced looks {@link Kledingkast#uiterlijk}, the render layer that draws in
 * every dimension). This class adds the rules that make it a safe one-way bridge:
 * <ul>
 *   <li><b>unlocks only from the Vadsrijk</b>: opening the wardrobe at home never runs the source check or collects armour looks
 *   ({@link Kledingkast#open(ServerPlayer, String, boolean)}), and only Vadsrijk armour ({@code guhs:vr_*}) ever becomes a transmog
 *   look. Survival never feeds the Vadsrijk;</li>
 *   <li><b>no stats</b>: wing tricks, gliding and the soft landing stay Vadsrijk-only (already in {@link Zweven}); nothing here
 *   touches attributes, items or armour;</li>
 *   <li><b>the zone gate</b>: a LOCKED piece of a closed zone is not listed at all (no names, no hints); what you have always shows
 *   ({@link #zichtbaar});</li>
 *   <li>{@code /guhs garderobe} (any player) next to the O key and {@code /guhs vadsrijk kledingkast}.</li>
 * </ul>
 * At home the screen is called "Garderobe" and has no armour (transmog) tab: that only dresses Vadsrijk gear.
 */
public final class Garderobe {
    /** Puzzle rewards: the zone their puzzle stands in (data/guhs/vadsrijk_puzzels/z&lt;n&gt;_*.json). */
    static final Map<String, Integer> PUZZEL_ZONE = Map.of("pz_vleugels_sterrenstof", 1, "pz_cape_sneeuwvlok", 2, "pz_vleugels_poollicht", 2,
            "pz_cape_honingraat", 3, "pz_vleugels_lantaarnmot", 3, "pz_cape_mos", 3);
    /** Secrets that need a later zone: the long glide needs wings (zone 3), the three tailors stand in zones 1-3. */
    static final Map<String, Integer> GEHEIM_ZONE = Map.of("zweefvlucht", 3, "kleermakers", 3);
    private static final Pattern ZONE = Pattern.compile("zone(\\d)");

    private Garderobe() {
    }

    /** The zone a piece belongs to for the gate (0 = general, earnable anywhere). The highest of its own zone and its source's. */
    public static int zone(Cosmetica.Ding d) {
        int z = d.zone();
        String b = d.bron();
        if (b.startsWith("puzzel:")) {
            z = Math.max(z, PUZZEL_ZONE.getOrDefault(b.substring(7), 0));
        } else if (b.startsWith("geheim:")) {
            z = Math.max(z, GEHEIM_ZONE.getOrDefault(b.substring(7), 0));
        }
        Matcher m = ZONE.matcher(b);
        while (m.find()) {
            z = Math.max(z, Integer.parseInt(m.group(1)));
        }
        return z;
    }

    /** Is this row listed? What you have: always. A locked piece: only when its zone is open (gate {@code poort}). */
    public static boolean zichtbaar(Cosmetica.Ding d, boolean heb, int poort) {
        return heb || zone(d) <= poort;
    }

    /** The open zone for the list (the plain gate: ops and creative get no spoilers either). */
    public static int poort() {
        try {
            return ZonePoort.open();
        } catch (RuntimeException | LinkageError e) {
            return 6;
        }
    }

    /** {@code /guhs garderobe}: your wardrobe, anywhere (no permission). */
    static void commando(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("garderobe").executes(c -> {
            Kledingkast.open(c.getSource().getPlayerOrException(), null);
            return 1;
        })));
    }
}
