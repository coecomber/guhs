package nl.juiced.guhs.feature.guhpolder;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * De Guhpolder (2.9): a rare, cold and FLAT biome of the Guhmensie (its own noise guhs:guhmension_polder, flattened
 * towards the middle, see tools/features/guhpolder.py) where the Elf-Guhjestocht is held (the elftocht feature places it
 * on the peak of the noise). The resources come from tools/features/guhpolder.py (+ guhpolder_*.py).
 * <ul>
 *   <li>Blocks: {@code rijpgras} (frosted guh grass with glitter), {@code rijpsprietjes}, {@code guh_ijsbloempje} (a
 *       see-through blue flower with a tiny guh face; light blue dye), {@code polderijs} (never melts, slippery like
 *       packed ice: the canal and the ditches), {@code knotwilg_stam} + {@code knotwilg_bladeren} (snow caps), and
 *       {@code ijspegelguh_kristal} (a glowing ice crystal shaped like a guh ear).</li>
 *   <li>The guh-molentje ({@link MolentjeBlock}, {@link MolentjeBlockEntity}): turning sails and a guh face; grinds
 *       {@code #guhs:knus/knabbelgraan} into {@code knabbelmeel} ({@link #KNABBELMEEL_TAG}), faster in snow and storm.
 *       The knabbeloven of the bakkerij takes knabbelmeel instead of knabbelgraan and then bakes twice as much.</li>
 *   <li>Worldgen ({@link GuhpolderWorldgen}): knotwilgen (pollard willows), frozen sloten (ditches) with rows of
 *       knotwilgen, natural sneeuwguh-heuveltjes and loose molentjes.</li>
 *   <li>The {@link Pinguh}: wild guhs born in the polder often become one; a tamed Pinguh slides along with a skating
 *       player ({@link PinguhMeeglijden}, the public API for the Elf-Guhjestocht).</li>
 * </ul>
 */
public final class GuhpolderFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    /** The biome guhs:guhpolder. */
    public static final ResourceKey<Biome> GUHPOLDER = ResourceKey.create(Registries.BIOME, Guhs.id("guhpolder"));
    /** The polder's own noise (the elftocht structure sits on its peak). */
    public static final ResourceKey<NormalNoise.NoiseParameters> POLDER_NOISE = ResourceKey.create(Registries.NOISE, Guhs.id("guhmension_polder"));
    /** Item tag guhs:knus/knabbelmeel: what the knabbeloven takes instead of knabbelgraan (and then bakes twice as much). */
    public static final TagKey<Item> KNABBELMEEL_TAG = TagKey.create(Registries.ITEM, Guhs.id("knus/knabbelmeel"));
    /** Block tag guhs:guhpolder/glijijs: ice a Pinguh belly-slides on (ice, packed ice, blue ice, polderijs...). */
    public static final TagKey<Block> GLIJIJS = TagKey.create(Registries.BLOCK, Guhs.id("guhpolder/glijijs"));

    // --- the biome's blocks ---------------------------------------------------------------------------------------------
    public static final DeferredBlock<GuhpolderBlocks.Rijpgras> RIJPGRAS = BLOCKS.registerBlock("rijpgras", GuhpolderBlocks.Rijpgras::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK).mapColor(MapColor.SNOW).sound(SoundType.GRASS));
    public static final DeferredBlock<GuhpolderBlocks.Rijpsprietjes> RIJPSPRIETJES = BLOCKS.registerBlock("rijpsprietjes",
            GuhpolderBlocks.Rijpsprietjes::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS).mapColor(MapColor.SNOW));
    public static final DeferredBlock<GuhpolderBlocks.IJsbloempje> GUH_IJSBLOEMPJE = BLOCKS.registerBlock("guh_ijsbloempje",
            p -> new GuhpolderBlocks.IJsbloempje(MobEffects.NIGHT_VISION, 5f, p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CORNFLOWER).mapColor(MapColor.ICE).lightLevel(s -> 3).sound(SoundType.AMETHYST_CLUSTER)
                    .noOcclusion());
    public static final DeferredBlock<GuhpolderBlocks.Polderijs> POLDERIJS = BLOCKS.registerBlock("polderijs", GuhpolderBlocks.Polderijs::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PACKED_ICE).mapColor(MapColor.ICE).strength(0.8f));
    public static final DeferredBlock<RotatedPillarBlock> KNOTWILG_STAM = BLOCKS.registerBlock("knotwilg_stam", RotatedPillarBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LOG).mapColor(MapColor.TERRACOTTA_BROWN));
    public static final DeferredBlock<GuhpolderBlocks.KnotwilgBladeren> KNOTWILG_BLADEREN = BLOCKS.registerBlock("knotwilg_bladeren",
            GuhpolderBlocks.KnotwilgBladeren::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LEAVES).mapColor(MapColor.TERRACOTTA_BROWN)
                    .sound(SoundType.AZALEA_LEAVES));
    public static final DeferredBlock<GuhpolderBlocks.IJspegelKristal> IJSPEGELGUH_KRISTAL = BLOCKS.registerBlock("ijspegelguh_kristal",
            GuhpolderBlocks.IJspegelKristal::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).mapColor(MapColor.ICE)
                    .lightLevel(s -> 11).sound(SoundType.AMETHYST_CLUSTER).noOcclusion().pushReaction(PushReaction.DESTROY));

    // --- the guh-molentje --------------------------------------------------------------------------------------------------
    public static final DeferredBlock<MolentjeBlock> GUH_MOLENTJE = BLOCKS.registerBlock("guh_molentje", MolentjeBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.2f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MolentjeBlockEntity>> GUH_MOLENTJE_BE = BLOCK_ENTITY_TYPES.register(
            "guh_molentje", () -> BlockEntityType.Builder.of(MolentjeBlockEntity::new, GUH_MOLENTJE.get()).build(null));
    public static final DeferredItem<Item> KNABBELMEEL = ITEMS.registerItem("knabbelmeel", GuhpolderItems.Lore::new, () -> new Item.Properties());

    static {
        for (DeferredBlock<?> block : List.of(RIJPGRAS, POLDERIJS, KNOTWILG_STAM, KNOTWILG_BLADEREN)) {
            ITEMS.registerSimpleBlockItem(block);
        }
        for (DeferredBlock<?> block : List.of(RIJPSPRIETJES, GUH_IJSBLOEMPJE, IJSPEGELGUH_KRISTAL)) {
            ITEMS.registerItem(block.getId().getPath(), p -> new GuhpolderItems.LoreBlock(block.get(), p));
        }
        ITEMS.registerItem("guh_molentje", p -> new GuhpolderItems.LoreBlock(GUH_MOLENTJE.get(), p), () -> new Item.Properties().rarity(Rarity.UNCOMMON));
    }

    // --- worldgen ----------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<Feature<?>, GuhpolderWorldgen.Knotwilg> KNOTWILG_FEATURE = FEATURES.register("guhpolder_knotwilg",
            () -> new GuhpolderWorldgen.Knotwilg(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, GuhpolderWorldgen.Sloot> SLOOT_FEATURE = FEATURES.register("guhpolder_sloot",
            () -> new GuhpolderWorldgen.Sloot(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, GuhpolderWorldgen.Sneeuwguhheuvel> SNEEUWGUHHEUVEL_FEATURE = FEATURES.register(
            "guhpolder_sneeuwguhheuvel", () -> new GuhpolderWorldgen.Sneeuwguhheuvel(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, GuhpolderWorldgen.Sneeuwplek> SNEEUWPLEK_FEATURE = FEATURES.register("guhpolder_sneeuwplek",
            () -> new GuhpolderWorldgen.Sneeuwplek(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, GuhpolderWorldgen.LosMolentje> MOLENTJE_FEATURE = FEATURES.register("guhpolder_molentje",
            () -> new GuhpolderWorldgen.LosMolentje(NoneFeatureConfiguration.CODEC));

    // --- particles and sounds ------------------------------------------------------------------------------------------------
    /** A tiny frost glitter (rijpgras, the ice crystal, a sliding Pinguh). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLINSTER = PARTICLES.register("guhpolder_glinster",
            () -> new SimpleParticleType(false));
    /** 2.10.1: guh-sneeuw, the Guhpolder's own snowfall: soft flakes and now and then a tiny pink guh head (client.GuhSneeuw). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GUH_SNEEUW = PARTICLES.register("guh_sneeuw",
            () -> new SimpleParticleType(false));
    public static final DeferredHolder<SoundEvent, SoundEvent> MOLENTJE_MAAL = sound("guhpolder.molentje_maal");
    public static final DeferredHolder<SoundEvent, SoundEvent> PINGUH_GLIJ = sound("guhpolder.pinguh_glij");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        FEATURES.register(modBus);
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener(GuhpolderFeature::setup);
        modBus.addListener(GuhpolderFeature::capabilities);
        NeoForge.EVENT_BUS.register(Pinguh.class);
        NeoForge.EVENT_BUS.register(PinguhMeeglijden.class);
        Pinguh.hooks();
        PinguhMeeglijden.hooks();
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FireBlock fire = (FireBlock) Blocks.FIRE;
            fire.setFlammable(KNOTWILG_STAM.get(), 5, 5);
            fire.setFlammable(KNOTWILG_BLADEREN.get(), 30, 60);
            fire.setFlammable(RIJPSPRIETJES.get(), 60, 100);
        });
    }

    /** Hoppers: knabbelgraan goes in from the top and the sides, knabbelmeel comes out at the bottom. */
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GUH_MOLENTJE_BE.get(), (be, side) -> be.handler(side));
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (var block : List.of(RIJPGRAS, RIJPSPRIETJES, GUH_IJSBLOEMPJE, POLDERIJS, KNOTWILG_STAM, KNOTWILG_BLADEREN, IJSPEGELGUH_KRISTAL,
                GUH_MOLENTJE)) {
            output.accept(new ItemStack(block.get()));
        }
        output.accept(new ItemStack(KNABBELMEEL.get()));
    }

    private GuhpolderFeature() {
    }
}
