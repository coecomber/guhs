package nl.juiced.guhs.feature.bio.wereld;

import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * biomes3 slice "wereld": the three biomes Bloesemmeertje, Klaterdal and Wolkenweide in the Guhmensie.
 * Resources: tools/features/bio_wereld.py (+ bio_wereld_dal / _meer / _wolk / _plek / _eiland).
 * <p>
 * The kern of the slice (this commit): where the biomes lie and the shape of their land, all from one terrain model
 * ({@link BioModel}, with {@link DalTerrein}, {@link MeerTerrein}, {@link WolkTerrein}); the two density functions that
 * put it into the world ({@link BioTerreinFunctie}, {@link BioRegioFunctie}); the feature for water, clouds and lifts
 * ({@link BioVulling}); and the structure type {@code guhs:bio_plek} ({@link BioPlekStructure}, {@link BioPlekken},
 * {@link Luchtruim}) that lets other slices put a building at a spot of that land with data only.
 * Dev commands: {@code /guhs bio wereld ...} ({@link BioWereldCommando}); self test "wereld" ({@code /guhs bio zelftest wereld}).
 */
public final class WereldSlice {
    private static final DeferredRegister<MapCodec<? extends DensityFunction>> DICHTHEDEN = DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, Guhs.MODID);
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Guhs.MODID);
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Guhs.MODID);

    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<BioTerreinFunctie>> BIO_TERREIN =
            DICHTHEDEN.register("bio_terrein", () -> BioTerreinFunctie.DATA_CODEC);
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<BioRegioFunctie>> BIO_REGIO =
            DICHTHEDEN.register("bio_regio", () -> BioRegioFunctie.DATA_CODEC);
    public static final DeferredHolder<Feature<?>, BioVulling> VULLING = FEATURES.register("bio_wereld_vulling", BioVulling::new);
    public static final DeferredHolder<StructureType<?>, StructureType<BioPlekStructure>> BIO_PLEK =
            STRUCTURE_TYPES.register("bio_plek", () -> () -> BioPlekStructure.CODEC);

    public static void register(IEventBus modBus) {
        DICHTHEDEN.register(modBus);
        FEATURES.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        MeerLeven.register(modBus); // biomes3 wereld-meer
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent e) -> Luchtruim.laad(e.getServer()));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> BioWereldCommando.registreer(e));
        BioWereldCommando.zelftest();
        DalBlokken.register(modBus); // biomes3 wereld-dal
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        MeerLeven.creative(output); // biomes3 wereld-meer
        DalBlokken.creative(output); // biomes3 wereld-dal
    }

    private WereldSlice() {
    }
}
