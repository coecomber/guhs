package nl.juiced.guhs.feature.sterrenwacht;

import java.util.function.Supplier;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Knus tab of the Guh-Sterrenwacht (section "sterrenwacht"): the sterrenatlas (the 15 constellations: 12 + 3 that
 * only show during a sterrenregen) and the milestones.
 */
public final class SterrenwachtVoortgang {
    public static final String ONDERDEEL = "sterrenwacht";

    private static Supplier<ItemStack> stack(Supplier<? extends ItemLike> item, int count) {
        return () -> new ItemStack(item.get(), count);
    }

    static void register() {
        String o = ONDERDEEL;
        KnusVoortgang.mijlpaal(o, "sterrenwacht_gevonden", Sterrenkijken.GEVONDEN, 1, stack(ModItems.KAAS_KNABBELS, 8));
        KnusVoortgang.mijlpaal(o, "sterrenwacht_eerste", Sterrenkijken.STERRENBEELDEN, 1, stack(SterrenwachtFeature.WENSSTER, 2));
        KnusVoortgang.mijlpaal(o, "sterrenwacht_zes", Sterrenkijken.ATLAS, 6, stack(SterrenwachtFeature.STERRENLANTAARN_ITEM, 4));
        KnusVoortgang.mijlpaal(o, "sterrenwacht_twaalf", Sterrenkijken.ATLAS, 12, stack(ModItems.GEFRITUURDE_KAASKNABBELS, 8));
        KnusVoortgang.mijlpaal(o, "sterrenwacht_zeldzaam", Sterrenkijken.ZELDZAAM, 1, stack(SterrenwachtFeature.WENSSTER, 5));
        KnusVoortgang.mijlpaal(o, "sterrenwacht_wensen", Sterrenkijken.WENSEN, 10, stack(ModItems.VAHOEGE_VADS_INGOT, 2));
        KnusVoortgang.mijlpaal(o, "sterrenwacht_atlas_vol", Sterrenkijken.ATLAS, Sterrenbeeld.values().length, stack(SterrenwachtFeature.TELESCOOP_ITEM, 1));
        KnusVoortgang.verzameling(o, Sterrenkijken.VERZAMELING, Sterrenbeeld.ids(), id -> {
            Sterrenbeeld b = Sterrenbeeld.byId(id);
            return new ItemStack(b != null && b.zeldzaam ? Items.NETHER_STAR : SterrenwachtFeature.WENSSTER.get());
        });
    }

    private SterrenwachtVoortgang() {
    }
}
