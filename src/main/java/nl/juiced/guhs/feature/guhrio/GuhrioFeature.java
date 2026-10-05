package nl.juiced.guhs.feature.guhrio;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * Super Guhrio (bbq2): the side-view platform game of the Kasteel van de Grote Nether-Mika. This is the engine and the
 * first pieces; the castle and its levels are built on it.
 * <ul>
 *     <li>A level is one or more lanes ({@link Baan}) hanging on a start block ({@link GuhrioLevel}); a player in a level
 *     ({@link GuhrioSpel}) is locked on the lane's line and sees it from the side (client.BaanCamera, client.BaanBesturing:
 *     A/D along the lane, space jumps, S ducks and goes down a pipe, W is for doors, sprint runs).</li>
 *     <li>Nobody is hurt in a level: falling or being touched brings you back to your last flag.</li>
 *     <li>The pieces ({@link GuhrioBlocks}): ground, brick, ?-block, coin, flag, flagpole, green pipe, and the Guhmba
 *     ({@link GuhmbaEntity}). Everything is per player.</li>
 * </ul>
 * Resources: tools/features/guhrio.py. The engine's manual for the level builders: guhs_work130/reports/guhrio_engine.md.
 */
public final class GuhrioFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    /** The painted ground of a level and its hard block (plain blocks to build with). */
    public static final DeferredBlock<Block> GROND = BLOCKS.registerSimpleBlock("guhrio_grond",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.5f, 6f).sound(SoundType.STONE));
    public static final DeferredBlock<Block> BLOK = BLOCKS.registerSimpleBlock("guhrio_blok",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(1.5f, 6f).sound(SoundType.STONE));

    public static final DeferredBlock<GuhrioBlocks.StartBlok> STARTBLOK = BLOCKS.registerBlock("guhrio_startblok", GuhrioBlocks.StartBlok::new,
            () -> los(MapColor.COLOR_RED).lightLevel(s -> 8));
    public static final DeferredBlock<GuhrioBlocks.VraagBlok> VRAAGBLOK = BLOCKS.registerBlock("guhrio_vraagblok", GuhrioBlocks.VraagBlok::new,
            () -> vast(MapColor.COLOR_YELLOW).lightLevel(s -> 6));
    public static final DeferredBlock<GuhrioBlocks.SteenBlok> STEEN = BLOCKS.registerBlock("guhrio_steen", GuhrioBlocks.SteenBlok::new,
            () -> vast(MapColor.COLOR_ORANGE));
    public static final DeferredBlock<GuhrioBlocks.MuntBlok> MUNT = BLOCKS.registerBlock("guhrio_munt", GuhrioBlocks.MuntBlok::new,
            () -> los(MapColor.GOLD).lightLevel(s -> 5));
    public static final DeferredBlock<GuhrioBlocks.VlagBlok> VLAG = BLOCKS.registerBlock("guhrio_vlag", GuhrioBlocks.VlagBlok::new,
            () -> los(MapColor.COLOR_PINK));
    public static final DeferredBlock<GuhrioBlocks.MastBlok> MAST = BLOCKS.registerBlock("guhrio_mast", GuhrioBlocks.MastBlok::new,
            () -> los(MapColor.COLOR_LIGHT_GREEN));
    public static final DeferredBlock<GuhrioBlocks.PijpBlok> PIJP = BLOCKS.registerBlock("guhrio_pijp", GuhrioBlocks.PijpBlok::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(1.5f, 6f).sound(SoundType.COPPER)
                    .isValidSpawn((s, l, p, e) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false).dynamicShape());
    public static final DeferredBlock<GuhrioBlocks.PijpLijfBlok> PIJP_LIJF = BLOCKS.registerBlock("guhrio_pijp_lijf", GuhrioBlocks.PijpLijfBlok::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(1.5f, 6f).sound(SoundType.COPPER)
                    .isValidSpawn((s, l, p, e) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false).dynamicShape());
    public static final DeferredBlock<GuhrioBlocks.DeurBlok> DEUR = BLOCKS.registerBlock("guhrio_deur", GuhrioBlocks.DeurBlok::new,
            () -> los(MapColor.COLOR_BROWN).sound(SoundType.WOOD));
    public static final DeferredBlock<GuhrioBlocks.GuhmbaPlek> GUHMBA_PLEK = BLOCKS.registerBlock("guhrio_guhmba_plek", GuhrioBlocks.GuhmbaPlek::new,
            () -> BlockBehaviour.Properties.of().noCollision().noLootTable().strength(-1f, 3600000f).noOcclusion()
                    .isValidSpawn((s, l, p, e) -> false).pushReaction(PushReaction.BLOCK));

    public static final List<DeferredItem<BlockItem>> BLOK_ITEMS = List.of(
            ITEMS.registerSimpleBlockItem(GROND), ITEMS.registerSimpleBlockItem(BLOK), ITEMS.registerSimpleBlockItem(STARTBLOK),
            ITEMS.registerSimpleBlockItem(VRAAGBLOK), ITEMS.registerSimpleBlockItem(STEEN), ITEMS.registerSimpleBlockItem(MUNT),
            ITEMS.registerSimpleBlockItem(VLAG), ITEMS.registerSimpleBlockItem(MAST), ITEMS.registerSimpleBlockItem(PIJP),
            ITEMS.registerSimpleBlockItem(PIJP_LIJF), ITEMS.registerSimpleBlockItem(DEUR), ITEMS.registerSimpleBlockItem(GUHMBA_PLEK));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GuhrioBlocks.StartBlockEntity>> START_BE = BLOCK_ENTITIES.register("guhrio_start",
            () -> new BlockEntityType<>(GuhrioBlocks.StartBlockEntity::new, STARTBLOK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GuhrioBlocks.StukBlockEntity>> STUK_BE = BLOCK_ENTITIES.register("guhrio_stuk",
            () -> new BlockEntityType<>(GuhrioBlocks.StukBlockEntity::new, VRAAGBLOK.get(), STEEN.get(), MUNT.get()));

    /** The Guhmba (never saved: it belongs to a level somebody plays). */
    public static final DeferredHolder<EntityType<?>, EntityType<GuhmbaEntity>> GUHMBA = ENTITY_TYPES.register("guhrio_guhmba",
            () -> EntityType.Builder.<GuhmbaEntity>of(GuhmbaEntity::new, MobCategory.MISC).sized(0.8f, 0.8f).clientTrackingRange(8)
                    .updateInterval(2).noSave().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("guhrio_guhmba"))));

    /** A piece that stops you (a ?-block, a brick): drawn by its block entity, so it must not hide its neighbours. */
    private static BlockBehaviour.Properties vast(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(1.5f, 6f).sound(SoundType.STONE).noOcclusion()
                .isValidSpawn((s, l, p, e) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false).dynamicShape();
    }

    /** A piece you walk through (a coin, a flag, the start). */
    private static BlockBehaviour.Properties los(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).noCollision().noOcclusion().strength(0.5f, 6f).sound(SoundType.AMETHYST)
                .isValidSpawn((s, l, p, e) -> false).pushReaction(PushReaction.BLOCK);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        NeoForge.EVENT_BUS.register(GuhrioEvents.class);
    }

    public static void payloads(PayloadRegistrar registrar) {
        GuhrioPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<BlockItem> item : BLOK_ITEMS) {
            output.accept(new ItemStack(item.get()));
        }
    }

    private GuhrioFeature() {
    }
}
