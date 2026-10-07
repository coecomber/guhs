package nl.juiced.guhs.feature.bio.blokkenwolk;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.GuhFurnitureBlock;
import nl.juiced.guhs.feature.bio.BioZelftest;

/**
 * biomes3 slice "blokken-wolk": what the Wolkenweide (and the lake) are made of.
 * <ul>
 *   <li>Wolkenblok in white and pink, each with slab and stairs ({@link WolkenBlokken}): soft, you sink in a little, landing on
 *       it never hurts, not bouncy. No block entity, no ticks; a solid cube, so cloud banks cull like stone.</li>
 *   <li>Regenboogblok with slab and stairs ({@link RegenboogBlokken}): translucent stripes that join up into one rainbow.</li>
 *   <li>Wolkenmeubels: wolkenbank (a seat), wolkenbed ({@link WolkenbedBlock}, a real bed), wolkenlamp.</li>
 *   <li>Drijvende bloesemblaadjes ({@link BloesemblaadjesBlock}): petals on the water, nothing bumps into them.</li>
 *   <li>Wolkenpluis, the fluff the cloud things are made of.</li>
 *   <li>The particles of the wolkenstroom ({@link WolkenstroomPluis}) and of big waterfalls ({@link Waterval}; the client side is
 *       {@code client/WatervalEffecten}).</li>
 * </ul>
 * Resources: tools/features/bio_blokken_wolk.py.
 */
public final class BlokkenWolkSlice {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);

    // --- sounds -----------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> WOLK_STAP = sound("wolkenblok.stap");
    public static final DeferredHolder<SoundEvent, SoundEvent> WOLK_PLAATS = sound("wolkenblok.plaats");
    public static final DeferredHolder<SoundEvent, SoundEvent> WOLK_BREEK = sound("wolkenblok.breek");
    /** The soft rush at the foot of a big waterfall (a loop; only the client plays it). */
    public static final DeferredHolder<SoundEvent, SoundEvent> WATERVAL_RUIS = sound("waterval.ruis");
    /** Cloud: soft puffs, quieter than wool. */
    public static final SoundType WOLK_GELUID = new DeferredSoundType(0.7f, 1.0f, WOLK_BREEK, WOLK_STAP, WOLK_PLAATS, WOLK_STAP, WOLK_STAP);

    // --- particles --------------------------------------------------------------------------------------------------------
    /** A fleck of foam hopping away from the foot of a waterfall. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WATERVAL_SCHUIM = PARTICLES.register("waterval_schuim",
            () -> new SimpleParticleType(false));
    /** A big soft puff of mist above the foot of a waterfall. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WATERVAL_NEVEL = PARTICLES.register("waterval_nevel",
            () -> new SimpleParticleType(false));
    /**
     * The fluff of the wolkenstroom: its speed says which way the stream goes. Going up it is white, going down pink; one in
     * three is a little arrow that points the way (the client decides, client/Deeltjes).
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WOLKENSTROOM_PLUIS = PARTICLES.register("wolkenstroom_pluis",
            () -> new SimpleParticleType(false));

    // --- cloud ------------------------------------------------------------------------------------------------------------
    private static BlockBehaviour.Properties wolk(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(0.3f).sound(WOLK_GELUID);
    }

    public static final DeferredBlock<WolkenBlokken.Blok> WOLKENBLOK_WIT = BLOCKS.registerBlock("wolkenblok_wit", WolkenBlokken.Blok::new,
            () -> wolk(MapColor.SNOW));
    public static final DeferredBlock<WolkenBlokken.Plaat> WOLKENBLOK_WIT_PLAAT = BLOCKS.registerBlock("wolkenblok_wit_plaat", WolkenBlokken.Plaat::new,
            () -> wolk(MapColor.SNOW));
    public static final DeferredBlock<WolkenBlokken.Trap> WOLKENBLOK_WIT_TRAP = BLOCKS.registerBlock("wolkenblok_wit_trap",
            p -> new WolkenBlokken.Trap(WOLKENBLOK_WIT.get().defaultBlockState(), p), () -> wolk(MapColor.SNOW));
    public static final DeferredBlock<WolkenBlokken.Blok> WOLKENBLOK_ROZE = BLOCKS.registerBlock("wolkenblok_roze", WolkenBlokken.Blok::new,
            () -> wolk(MapColor.COLOR_PINK));
    public static final DeferredBlock<WolkenBlokken.Plaat> WOLKENBLOK_ROZE_PLAAT = BLOCKS.registerBlock("wolkenblok_roze_plaat", WolkenBlokken.Plaat::new,
            () -> wolk(MapColor.COLOR_PINK));
    public static final DeferredBlock<WolkenBlokken.Trap> WOLKENBLOK_ROZE_TRAP = BLOCKS.registerBlock("wolkenblok_roze_trap",
            p -> new WolkenBlokken.Trap(WOLKENBLOK_ROZE.get().defaultBlockState(), p), () -> wolk(MapColor.COLOR_PINK));

    // --- rainbow ----------------------------------------------------------------------------------------------------------
    private static BlockBehaviour.Properties regenboog() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.3f).sound(WOLK_GELUID).noOcclusion()
                .isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false)
                .isViewBlocking((s, l, p) -> false);
    }

    public static final DeferredBlock<RegenboogBlokken.Blok> REGENBOOGBLOK = BLOCKS.registerBlock("regenboogblok", RegenboogBlokken.Blok::new,
            () -> regenboog());
    public static final DeferredBlock<RegenboogBlokken.Plaat> REGENBOOGBLOK_PLAAT = BLOCKS.registerBlock("regenboogblok_plaat",
            RegenboogBlokken.Plaat::new, () -> regenboog());
    public static final DeferredBlock<RegenboogBlokken.Trap> REGENBOOGBLOK_TRAP = BLOCKS.registerBlock("regenboogblok_trap",
            p -> new RegenboogBlokken.Trap(REGENBOOGBLOK.get().defaultBlockState(), p), () -> regenboog());

    // --- cloud furniture --------------------------------------------------------------------------------------------------
    private static BlockBehaviour.Properties meubel() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.4f).sound(WOLK_GELUID).noOcclusion()
                .pushReaction(PushReaction.DESTROY);
    }

    /** A bench of cloud: sit on it like on the guh-bank. The boxes are tools/features/bio_blokken_wolk.py's BANK (facing north). */
    public static final DeferredBlock<GuhFurnitureBlock> WOLKENBANK = BLOCKS.registerBlock("wolkenbank",
            p -> new GuhFurnitureBlock(p, 0.5, new double[]{0, 0, 1, 16, 8, 15}, new double[]{0, 8, 11, 16, 16, 15},
                    new double[]{0, 8, 1, 2, 12, 11}, new double[]{14, 8, 1, 16, 12, 11}), () -> meubel());
    public static final DeferredBlock<WolkenbedBlock> WOLKENBED = BLOCKS.registerBlock("wolkenbed", WolkenbedBlock::new, () -> meubel());
    public static final DeferredBlock<WolkenlampBlock> WOLKENLAMP = BLOCKS.registerBlock("wolkenlamp", WolkenlampBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.3f).sound(WOLK_GELUID).noOcclusion()
                    .lightLevel(s -> WolkenlampBlock.LICHT).pushReaction(PushReaction.DESTROY));

    // --- petals -----------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<BloesemblaadjesBlock> BLOESEMBLAADJES = BLOCKS.registerBlock("drijvende_bloesemblaadjes",
            BloesemblaadjesBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).instabreak().noCollision()
                    .noOcclusion().sound(SoundType.PINK_PETALS).pushReaction(PushReaction.DESTROY));

    // --- items ------------------------------------------------------------------------------------------------------------
    public static final DeferredItem<Item> WOLKENPLUIS = ITEMS.registerSimpleItem("wolkenpluis", () -> new Item.Properties());
    public static final DeferredItem<BlockItem> WOLKENBLOK_WIT_ITEM = ITEMS.registerSimpleBlockItem(WOLKENBLOK_WIT);
    public static final DeferredItem<BlockItem> WOLKENBLOK_WIT_PLAAT_ITEM = ITEMS.registerSimpleBlockItem(WOLKENBLOK_WIT_PLAAT);
    public static final DeferredItem<BlockItem> WOLKENBLOK_WIT_TRAP_ITEM = ITEMS.registerSimpleBlockItem(WOLKENBLOK_WIT_TRAP);
    public static final DeferredItem<BlockItem> WOLKENBLOK_ROZE_ITEM = ITEMS.registerSimpleBlockItem(WOLKENBLOK_ROZE);
    public static final DeferredItem<BlockItem> WOLKENBLOK_ROZE_PLAAT_ITEM = ITEMS.registerSimpleBlockItem(WOLKENBLOK_ROZE_PLAAT);
    public static final DeferredItem<BlockItem> WOLKENBLOK_ROZE_TRAP_ITEM = ITEMS.registerSimpleBlockItem(WOLKENBLOK_ROZE_TRAP);
    public static final DeferredItem<BlockItem> REGENBOOGBLOK_ITEM = ITEMS.registerSimpleBlockItem(REGENBOOGBLOK);
    public static final DeferredItem<BlockItem> REGENBOOGBLOK_PLAAT_ITEM = ITEMS.registerSimpleBlockItem(REGENBOOGBLOK_PLAAT);
    public static final DeferredItem<BlockItem> REGENBOOGBLOK_TRAP_ITEM = ITEMS.registerSimpleBlockItem(REGENBOOGBLOK_TRAP);
    public static final DeferredItem<BlockItem> WOLKENBANK_ITEM = ITEMS.registerSimpleBlockItem(WOLKENBANK);
    public static final DeferredItem<BlockItem> WOLKENBED_ITEM = ITEMS.registerSimpleBlockItem("wolkenbed", WOLKENBED,
            () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<BlockItem> WOLKENLAMP_ITEM = ITEMS.registerSimpleBlockItem(WOLKENLAMP);
    public static final DeferredItem<BloesemblaadjesBlock.Voorwerp> BLOESEMBLAADJES_ITEM = ITEMS.registerItem("drijvende_bloesemblaadjes",
            p -> new BloesemblaadjesBlock.Voorwerp(BLOESEMBLAADJES.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());

    /** Every block of this slice (the tests walk it). */
    public static List<DeferredBlock<? extends Block>> blokken() {
        return List.of(WOLKENBLOK_WIT, WOLKENBLOK_WIT_PLAAT, WOLKENBLOK_WIT_TRAP, WOLKENBLOK_ROZE, WOLKENBLOK_ROZE_PLAAT, WOLKENBLOK_ROZE_TRAP,
                REGENBOOGBLOK, REGENBOOGBLOK_PLAAT, REGENBOOGBLOK_TRAP, WOLKENBANK, WOLKENBED, WOLKENLAMP, BLOESEMBLAADJES);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        PARTICLES.register(modBus);
        NeoForge.EVENT_BUS.addListener(Proef::commando);
        BioZelftest.registreer("blokken_wolk", BlokkenWolkSlice::zelftest);
    }

    /** On the real server (the game tests have no Guhmensie and a test datapack): the data of this slice is loaded and fits. */
    private static void zelftest(MinecraftServer server, ServerLevel level, BioZelftest.Melder meld) {
        BlockPos hoog = new BlockPos(0, 120, 0);
        BedRule regel = level.environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, hoog);
        meld.check(!regel.explodes() && regel.canSetSpawn(level), "the wolkenbed can be slept in and is a spawn point in the Guhmensie");
        TagKey<Block> wolk = TagKey.create(Registries.BLOCK, Guhs.id("wolkenblok"));
        long inTag = blokken().stream().filter(b -> b.get().defaultBlockState().is(wolk)).count();
        meld.check(inTag == 6, "the tag guhs:wolkenblok holds the six pieces of cloud (" + inTag + ")");
        meld.check(WOLKENBED.get().defaultBlockState().is(BlockTags.BEDS) && WOLKENBLOK_WIT_PLAAT.get().defaultBlockState().is(BlockTags.SLABS)
                && REGENBOOGBLOK_TRAP.get().defaultBlockState().is(BlockTags.STAIRS), "vanilla's tags: beds, slabs, stairs");
        int recepten = 0;
        for (String id : List.of("wolkenblok_wit", "wolkenblok_roze", "regenboogblok", "wolkenbank", "wolkenbed", "wolkenlamp", "drijvende_bloesemblaadjes")) {
            if (server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Guhs.id(id))).isPresent()) {
                recepten++;
            }
        }
        meld.check(recepten == 7, "the recipes are loaded (" + recepten + " of 7)");
        int vallen = 0;
        for (DeferredBlock<? extends Block> b : blokken()) {
            BlockState s = b.get() instanceof WolkenbedBlock ? b.get().defaultBlockState().setValue(WolkenbedBlock.PART, BedPart.HEAD) : b.get().defaultBlockState();
            if (Block.getDrops(s, level, hoog, null).stream().anyMatch(d -> d.is(b.get().asItem()))) {
                vallen++;
            }
        }
        meld.check(vallen == blokken().size(), "every block drops itself (" + vallen + " of " + blokken().size() + ")");
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<? extends Item> item : List.of(WOLKENPLUIS, WOLKENBLOK_WIT_ITEM, WOLKENBLOK_WIT_PLAAT_ITEM, WOLKENBLOK_WIT_TRAP_ITEM,
                WOLKENBLOK_ROZE_ITEM, WOLKENBLOK_ROZE_PLAAT_ITEM, WOLKENBLOK_ROZE_TRAP_ITEM, REGENBOOGBLOK_ITEM, REGENBOOGBLOK_PLAAT_ITEM,
                REGENBOOGBLOK_TRAP_ITEM, WOLKENBANK_ITEM, WOLKENBED_ITEM, WOLKENLAMP_ITEM, BLOESEMBLAADJES_ITEM)) {
            output.accept(new ItemStack(item.get()));
        }
    }

    private BlokkenWolkSlice() {
    }
}
