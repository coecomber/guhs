package nl.juiced.guhs.feature.vadsrijk.zone2.systemen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Every coordinate and number of the systemen slice that depends on the zone-2 map (POI_Z2.md v0, z2-wereld). The NPCs that
 * open the juwelier / shops (z2-npc) stand at {@link #JUWELIER} / {@link #KANDIJKRAAM}; the 3rd advancement can be done at the
 * kristalkapel altar ({@link #ALTAAR}) or at the ordetempel desk in Zwamzoet.
 */
public final class Z2SystemenPlekken {
    /** The zone 2 box (fix round 2: the compact zone + a 40-block margin, from z2-wereld's map). */
    public static final int MIN_X = nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.ZONE_MIN_X - 40,
            MAX_X = nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.ZONE_MAX_X + 40,
            MIN_Z = nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.ZONE_MIN_Z - 40,
            MAX_Z = nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.ZONE_MAX_Z + 40;
    /** Gear of this level or higher counts as a zone-2 drop (0-2 sockets). */
    public static final int ZONE2_LEVEL = 20;
    /** POI_Z2: npc_juwelier, npc_winkel_kandij, kristalkapel_altaar. */
    public static final BlockPos JUWELIER = w("npc_juwelier", -52, 91, -2440);
    public static final BlockPos KANDIJKRAAM = w("npc_winkel_kandij", 66, 91, -2392);
    public static final BlockPos ALTAAR = w("kristalkapel_altaar", 0, 91, -2476);

    /** (integration) from z2-wereld's generated spots; the number is only a fallback. */
    private static BlockPos w(String id, int x, int y, int z) {
        BlockPos b = nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.plek(id);
        return b != null ? b : nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.z2(x, y, z);   // T-WERELD: local zone-2 fallback
    }

    private Z2SystemenPlekken() {
    }

    public static boolean inZone2(Vec3 v) {
        return v.x >= MIN_X && v.x <= MAX_X && v.z >= MIN_Z && v.z <= MAX_Z;
    }
}
