package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhVariant;

/**
 * One NPC guh of Het Vadsrijk: its id (lang {@code vadsrijk.npc.naam.<id>}, idle lines {@code vadsrijk.npc.praat.<id>.1..3}),
 * where it stands, which way it looks, its look (guh variant + existing guh clothes), how it idles and its role (what a click
 * does when there is no quest business: {@link NpcRollen}; null = just chatter).
 */
public record NpcDef(String id, BlockPos plek, float yaw, GuhVariant variant, List<GuhClothes> kleren, Gedrag gedrag, @Nullable String rol, float maat) {
    public enum Gedrag {
        /** Stands at its spot, looks around and at players. */
        STAAN,
        /** Sits (the cook at the campfire, the angler). */
        ZITTEN,
        /** Wanders a few blocks around its spot (citizens). */
        SLENTER
    }

    public String naamKey() {
        return "vadsrijk.npc.naam." + id;
    }

    /** NPCs with a 4th idle line (WERELDVERHAAL §10; tools/features/vadsrijk_npc.py + vadsrijk_z2_npc.py VIER_REGELS). */
    public static final Set<String> VIER_REGELS = Set.of("oma_truus", "akim_kaasguh", "z2_poollicht", "z2_opa_sneeuwbal");

    /** A random idle line of this NPC: {@code vadsrijk.npc.praat.<id>.1..3} (or ..4). */
    public static String gedagKey(String id, RandomSource r) {
        return "vadsrijk.npc.praat." + id + "." + (1 + r.nextInt(VIER_REGELS.contains(id) ? 4 : 3));
    }
}
