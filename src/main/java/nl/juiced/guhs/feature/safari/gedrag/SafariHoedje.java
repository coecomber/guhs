package nl.juiced.guhs.feature.safari.gedrag;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.safari.Safari;

/**
 * Design 1.2 (G1): your own guhs that come along into the park automatically wear a safari hat; their own hat comes back
 * when they leave. The hat they wore is kept in the guh's persistent data ({@link #EIGEN}: the clothes id, "" for none),
 * so it survives a save in the park. Only tame guhs with an owner (the park's own kijk-guhs, visitors and parade guhs
 * wear their own outfits).
 */
public final class SafariHoedje {
    static final String EIGEN = "guhs_safari_eigen_hoed";

    public static void register() {
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (!e.getLevel().isClientSide() && e.getEntity() instanceof GuhEntity g && g.isTame() && g.getOwnerReference() != null) {
                pas(g, Safari.echt(e.getLevel()));
            }
        });
    }

    /** In the park: the safari hat on (remembering its own); outside: its own hat back. */
    public static void pas(GuhEntity g, boolean inPark) {
        CompoundTag data = g.getPersistentData();
        if (inPark) {
            if (data.contains(EIGEN)) {
                return;                                   // (already wearing it: a reload in the park)
            }
            GuhClothes oud = g.getClothes(GuhClothes.SAFARI_HOEDJE.slot);
            data.putString(EIGEN, oud == null ? "" : oud.name());
            g.wear(GuhClothes.SAFARI_HOEDJE);
        } else if (data.contains(EIGEN)) {
            String oud = data.getStringOr(EIGEN, "");
            data.remove(EIGEN);
            if (g.getClothes(GuhClothes.SAFARI_HOEDJE.slot) != GuhClothes.SAFARI_HOEDJE) {
                return;                                   // (changed in the park by the player: keep that)
            }
            GuhClothes eigen = null;
            try {
                eigen = oud.isEmpty() ? null : GuhClothes.valueOf(oud);
            } catch (IllegalArgumentException ex) {
                // (a removed piece of clothing)
            }
            if (eigen == null) {
                g.takeOff(GuhClothes.SAFARI_HOEDJE.slot);
            } else {
                g.wear(eigen);
            }
        }
    }

    private SafariHoedje() {
    }
}
