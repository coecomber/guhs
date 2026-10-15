package nl.juiced.guhs.feature.verhaal;

import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.wereld.RegioJigsawStructure;
import nl.juiced.guhs.feature.verhaal.wereld.RegioPiekPlacement;

/**
 * 3.0 "Guhverhalen": the fundament of the update (the shared parts every story slice builds on). Resources:
 * tools/features/verhaal.py (texts, advancements, FTB) and verhaal_wereld.py (the two new biomes and the region placement).
 * <ul>
 *   <li>{@link VerhaalGuhs}: the story guhs, tameable once per player, and their untameable story copies;</li>
 *   <li>{@link VariantGedrag} / {@link VariantGedragen} (+ client {@code VariantUiterlijk}): per-variant behaviour and looks;</li>
 *   <li>{@link NpcRollen} (roles of the story characters, by plek) and {@link Praat} (the talking screen, scenes);</li>
 *   <li>the worldgen: the biomes {@link #SNEEUWGUHTOENDRA} and {@link #GUHWAII} (terrain in the noise router) and the one-per-region
 *       structures ({@code guhs:regio_piek} placement + {@code guhs:regio_jigsaw} structure, package {@code wereld});</li>
 *   <li>the dead tamed guhs ("In de wolkjes", {@code nl.juiced.guhs.feature.band.Wolkjes}), huisje ownership, the generic maatje
 *       shoulder, the ukelele emote gate: in their own packages.</li>
 * </ul>
 * bbq2: the verhaal engine on top of it (resources: tools/features/verhaal_motor.py):
 * <ul>
 *   <li>{@link Verhaallijn} / {@link Verhaallijnen}: questlines with per-player steps, synced ({@link VerhaalSync});</li>
 *   <li>{@link Cutscene} / {@link Cutscenes} (camera cutscenes) and {@link Verteller} (narrator cards), with the lock {@link Vast};</li>
 *   <li>{@link Reiskaart} / {@link Reiskaarten} (the Guhdex travel map), {@link Doel} / {@link Doelen} (the next-goal pointer);</li>
 *   <li>{@link Sluiers} (Guhdalfs sluier), {@link Duwtje} and {@link Rustpunten} (gentle shoves, rest points);</li>
 *   <li>{@link VerhaalDemo}: the demo story (dev only), {@link VerhaalMotor}: events and {@code /guhs verhaal}.</li>
 * </ul>
 */
public final class VerhaalFeature {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Guhs.MODID);
    public static final DeferredRegister<StructurePlacementType<?>> PLACEMENT_TYPES = DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, Guhs.MODID);
    public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Guhs.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<RegioJigsawStructure>> REGIO_STRUCTURE =
            STRUCTURE_TYPES.register("regio_jigsaw", () -> () -> RegioJigsawStructure.CODEC);
    public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<RegioPiekPlacement>> REGIO_PLACEMENT =
            PLACEMENT_TYPES.register("regio_piek", () -> () -> RegioPiekPlacement.CODEC);
    /** The water of the Guhwai'i region (its own warm lagoon-sea, see wereld.GuhwaiiWaterFeature). */
    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, nl.juiced.guhs.feature.verhaal.wereld.GuhwaiiWaterFeature>
            GUHWAII_WATER = FEATURES.register("guhwaii_water", nl.juiced.guhs.feature.verhaal.wereld.GuhwaiiWaterFeature::new);

    /** bbq2: the thick smoke of Guhdalfs sluier (client: client.SluierRook; particle file by tools/features/verhaal_motor.py). */
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, net.minecraft.core.particles.SimpleParticleType> SLUIERROOK =
            PARTICLES.register("verhaal_sluierrook", () -> new net.minecraft.core.particles.SimpleParticleType(true));

    /** The new biomes (tools/features/verhaal_wereld.py) and their noises. */
    public static final ResourceKey<Biome> SNEEUWGUHTOENDRA = ResourceKey.create(Registries.BIOME, Guhs.id("sneeuwguhtoendra"));
    public static final ResourceKey<Biome> GUHWAII = ResourceKey.create(Registries.BIOME, Guhs.id("guhwaii"));
    public static final ResourceKey<NormalNoise.NoiseParameters> TOENDRA_NOISE = ResourceKey.create(Registries.NOISE, Guhs.id("sneeuwguhtoendra"));
    public static final ResourceKey<NormalNoise.NoiseParameters> GUHWAII_NOISE = ResourceKey.create(Registries.NOISE, Guhs.id("guhwaii"));

    /** The creature pages of the 3.0 critters (seeing one grants the Diertjes tab root). */
    public static final Set<GuhVariant> DIERTJES = java.util.EnumSet.of(GuhVariant.PLUISVINKJE, GuhVariant.KAASMEESJE, GuhVariant.GUH_UILTJE,
            GuhVariant.ZEEMEEUWTJE, GuhVariant.GUHXOLOTL, GuhVariant.GUH_EENDJE, GuhVariant.KNABBELVLINDERTJE, GuhVariant.GLIMGUHTJE,
            GuhVariant.LIEVEHEERSBEESTJE, GuhVariant.PLUISEGELTJE, GuhVariant.GUH_KONIJNTJE, GuhVariant.PLUISEEKHOORNTJE, GuhVariant.SHUCKLE);

    /** The Superkompas tab of the story places. */
    public static final String KOMPAS_TAB = "verhalen";

    public static void register(IEventBus modBus) {
        STRUCTURE_TYPES.register(modBus);
        PLACEMENT_TYPES.register(modBus);
        FEATURES.register(modBus);
        PARTICLES.register(modBus);
        NeoForge.EVENT_BUS.register(VerhaalEvents.class);
        GuhHooks.tick(VerhaalGuhs::tick);
        // bbq2: the verhaal engine (questlines, cutscenes, narrator cards, the next-goal pointer, Guhdalfs sluier)
        NeoForge.EVENT_BUS.register(VerhaalMotor.class);
        NeoForge.EVENT_BUS.register(Vast.class);
        NeoForge.EVENT_BUS.register(Sluiers.class);
        // (PHASE3 R06: the walls of the story's own places are known as soon as a world has its guaranteed spots)
        nl.juiced.guhs.world.GegarandeerdPlacement.NA_VOORUIT.add(Sluiers::kenGegarandeerd);
        NeoForge.EVENT_BUS.register(Doelen.class);
        VerhaalDemo.register();
    }

    public static void payloads(PayloadRegistrar registrar) {
        VerhaalSync.register(registrar);
        VerhaalPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    /** (the Superkompas choice payload) a place of the Verhalen tab was chosen. */
    public static void kompasGekozen(ServerPlayer player, String structure) {
        var cats = nl.juiced.guhs.item.SuperkompasItem.CATEGORIES;
        int tab = nl.juiced.guhs.item.SuperkompasItem.categoryOf(structure);
        boolean inVerhalen = cats.stream().filter(c -> c.id().equals(KOMPAS_TAB)).anyMatch(c -> c.structures().contains(structure));
        if (inVerhalen || tab >= 0 && cats.get(tab).id().equals(KOMPAS_TAB)) {
            nl.juiced.guhs.quest.GuhAdvancements.grant(player, "verhaal_kompas");
            nl.juiced.guhs.feature.gids.GidsFeature.grant(player, "verhalen/verhaal_kompas");
        }
    }

    /** (GuhDex) a creature page was seen: the 3.0 critters open the Diertjes tab. */
    public static void paginaGezien(ServerPlayer player, GuhVariant page) {
        if (DIERTJES.contains(page)) {
            nl.juiced.guhs.feature.gids.GidsFeature.grant(player, "diertjes/root");
        }
    }

    private VerhaalFeature() {
    }
}
