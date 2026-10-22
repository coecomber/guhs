package nl.juiced.guhs.feature.beverbeek;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.verhaal.NpcRollen;

/**
 * 1.6.0 deel 2 (F10a, design 2.10): de Beverbeek, a rare brook forest in the Guhmension (willow, alder, birch), with
 * winding brooks and little waterfalls ({@link BeekFeature}), wild beaver pools behind dams with a lodge
 * ({@link BeverpoelFeature}), gnawed stumps, leaf heaps and paw prints ({@link SporenFeature}). 1.6.2: wild beaver
 * families along the water ({@link WildeBevers}), fewer guhs, and those often Beverguhs or in a knaagdier outfit
 * ({@link BeverGuhs}). The occasional ranger post (structure {@code guhs:beverbeek_rangerpost}): the {@link BeverbeekRanger}
 * tells about beavers and his {@link Aaibever} Spaander may be petted.
 * tools/features/safari_beverbeek.py makes the resources (the biome and its region, the trees, the post).
 * New chunks only: the server world is never reset.
 */
public final class BeverbeekFeature {
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Guhs.MODID);
    public static final DeferredHolder<Feature<?>, BeekFeature> BEEK = FEATURES.register("beverbeek_beek", BeekFeature::new);
    public static final DeferredHolder<Feature<?>, BeverpoelFeature> POEL = FEATURES.register("beverbeek_poel", BeverpoelFeature::new);
    public static final DeferredHolder<Feature<?>, SporenFeature> SPOREN = FEATURES.register("beverbeek_sporen", SporenFeature::new);
    public static final ResourceKey<Biome> BIOME = ResourceKey.create(Registries.BIOME, Guhs.id("beverbeek"));

    public static void register(IEventBus modBus) {
        FEATURES.register(modBus);
        NpcRollen.zet(GuhNpcEntity.Kind.BEVERBEEK_RANGER, BeverbeekRanger.ROL);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, Aaibever::opInteractie);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, Aaibever::opInteractieSpecifiek);
        WildeBevers.registreer();   // 1.6.2: wild beaver families along the brooks
        BeverGuhs.registreer();     // 1.6.2: the Beverguh and the knaagdier outfit of the Beverbeek's guhs
        // the Superkompas tab "Biomes" (section Guhmension), with a gnawed stump as its icon
        nl.juiced.guhs.feature.bio.BiomeLijst.biome("guhmension", "beverbeek");
        nl.juiced.guhs.feature.bio.kompas.BiomeIconen.zet("beverbeek", "minecraft:stripped_oak_log");
    }

    private BeverbeekFeature() {
    }
}
