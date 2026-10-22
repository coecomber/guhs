package nl.juiced.guhs.feature.beverbeek;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * 1.6.2: the guhs of the Beverbeek. Fewer of them are born there (GuhmensionSpawner, the biome's spawners), and a plain
 * wild guh born there is, once:
 * <ul>
 *   <li>a {@link GuhVariant#BEVERGUH Beverguh} ({@link #BEVERGUH_KANS}; the pattern of the biomes3 guhs), or else</li>
 *   <li>dressed for the brook: the knaagdier hood ({@link #MUTS_KANS}), sometimes with the tail too ({@link #STAART_KANS}).</li>
 * </ul>
 * The knaagdier outfit is also craftable (source "crafting" in the Kleding tab).
 */
public final class BeverGuhs {
    public static final float BEVERGUH_KANS = 0.3f, MUTS_KANS = 0.35f, STAART_KANS = 0.5f;
    static final String GEKEKEN = "guhs_beverbeek_gekeken";

    private BeverGuhs() {
    }

    static void registreer() {
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (e.getLevel() instanceof ServerLevel level && e.getEntity() instanceof GuhEntity guh && !e.loadedFromDisk()
                    && level.getBiome(guh.blockPosition()).is(BeverbeekFeature.BIOME)) {
                kies(guh);
            }
        });
        KledingBronnen.bron(GuhClothes.KNAAGDIER_MUTS, "crafting");
        KledingBronnen.bron(GuhClothes.KNAAGDIER_STAART, "crafting");
    }

    /** Decides once for a new wild guh born in the Beverbeek: Beverguh, knaagdier outfit, or neither. Returns what it did ("" for nothing). */
    public static String kies(GuhEntity guh) {
        CompoundTag data = guh.getPersistentData();
        if (guh.getClass() != GuhEntity.class || data.getBooleanOr(GEKEKEN, false)) {
            return "";
        }
        data.putBoolean(GEKEKEN, true);
        if (guh.isTame() || guh.isBaby() || guh.hasCustomName() || guh.getVariant() != GuhVariant.NORMAL || GuhHooks.isBewoner(guh)) {
            return "";
        }
        var rnd = guh.getRandom();
        if (rnd.nextFloat() < BEVERGUH_KANS) {
            guh.setVariant(GuhVariant.BEVERGUH);
            return "beverguh";
        }
        if (rnd.nextFloat() < MUTS_KANS && guh.getClothes(GuhClothes.Slot.HEAD) == null) {
            guh.wear(GuhClothes.KNAAGDIER_MUTS);
            if (rnd.nextFloat() < STAART_KANS && guh.getClothes(GuhClothes.Slot.BACK) == null) {
                guh.wear(GuhClothes.KNAAGDIER_STAART);
            }
            return "knaagdier";
        }
        return "";
    }
}
