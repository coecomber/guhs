package nl.juiced.guhs.feature.guhrio;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * Super Guhrio (bbq2): the side-view platform game of the Kasteel van de Grote Nether-Mika. This is the engine; the six
 * levels, the duel and the forecourt are built on it by the slices guhrio-w1..w3 and guhrio-beloning.
 * <ul>
 *     <li>A level is one or more lanes ({@link Baan}) hanging on a start block ({@link GuhrioLevel}); a player in a level
 *     ({@link GuhrioSpel}) is locked on the lane's line and sees it from the side (client.BaanCamera, client.BaanBesturing:
 *     A/D along the lane, space jumps, S ducks and goes down a pipe, W is for doors, sprint runs, a mouse button throws /
 *     licks, Q twice leaves).</li>
 *     <li>Nobody is hurt in a level: falling or being touched brings you back to your last flag.</li>
 *     <li>The pieces ({@link GuhrioBlocks}, {@link GuhrioStukken}): ground, brick, ?-block, hidden block, coin, big
 *     vadsmunt, flag, flagpole, pipes in every direction, door, switch and switched block, and the spots of the creatures
 *     ({@link GuhmbaEntity}, {@link SchildMikaEntity}, {@link PlofMikaEntity}, {@link HapbloemEntity},
 *     {@link GrillspiesEntity}) and of the moving things ({@link PlatformEntity}, {@link ValblokEntity}). Everything is per
 *     player.</li>
 *     <li>The castle as a whole ({@link GuhrioKasteel}): progress, coins, vadsmunten, records, the questline
 *     {@link #LIJN}.</li>
 * </ul>
 * Resources: tools/features/guhrio.py (+ guhrio_tex, guhrio_modellen, guhrio_baan, guhrio_kasteel). The manual for the
 * level slices: guhs_work130/reports/slice_guhrio-engine.md.
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
    /**
     * The brick as plain masonry: the look of {@link #STEEN}, but an ordinary block. The castle's orange trim is made of it
     * (thousands of blocks). A brick PIECE is no building block: its block entity draws it (one by one, only within 96
     * blocks), it lets light through and the game does not call it solid, so a wall sign falls off it.
     */
    public static final DeferredBlock<Block> SIERSTEEN = BLOCKS.registerSimpleBlock("guhrio_siersteen",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.5f, 6f).sound(SoundType.STONE));

    public static final DeferredBlock<GuhrioBlocks.StartBlok> STARTBLOK = BLOCKS.registerBlock("guhrio_startblok", GuhrioBlocks.StartBlok::new,
            () -> los(MapColor.COLOR_RED).lightLevel(s -> 8));
    public static final DeferredBlock<GuhrioBlocks.PoortBlok> POORT = BLOCKS.registerBlock("guhrio_poort", GuhrioBlocks.PoortBlok::new,
            () -> los(MapColor.COLOR_YELLOW).lightLevel(s -> 12));
    public static final DeferredBlock<GuhrioBlocks.VraagBlok> VRAAGBLOK = BLOCKS.registerBlock("guhrio_vraagblok", GuhrioBlocks.VraagBlok::new,
            () -> vast(MapColor.COLOR_YELLOW).lightLevel(s -> 6));
    public static final DeferredBlock<GuhrioBlocks.SteenBlok> STEEN = BLOCKS.registerBlock("guhrio_steen", GuhrioBlocks.SteenBlok::new,
            () -> vast(MapColor.COLOR_ORANGE));
    public static final DeferredBlock<GuhrioStukken.OnzichtbaarBlok> ONZICHTBAAR = BLOCKS.registerBlock("guhrio_onzichtbaar",
            GuhrioStukken.OnzichtbaarBlok::new, () -> vast(MapColor.NONE).noLootTable());
    public static final DeferredBlock<GuhrioBlocks.MuntBlok> MUNT = BLOCKS.registerBlock("guhrio_munt", GuhrioBlocks.MuntBlok::new,
            () -> los(MapColor.GOLD).lightLevel(s -> 5));
    public static final DeferredBlock<GuhrioStukken.VadsmuntBlok> VADSMUNT = BLOCKS.registerBlock("guhrio_vadsmunt", GuhrioStukken.VadsmuntBlok::new,
            () -> los(MapColor.GOLD).lightLevel(s -> 9));
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
    public static final DeferredBlock<GuhrioStukken.SchakelaarBlok> SCHAKELAAR = BLOCKS.registerBlock("guhrio_schakelaar",
            GuhrioStukken.SchakelaarBlok::new, () -> vast(MapColor.COLOR_LIGHT_BLUE).lightLevel(s -> 6));
    public static final DeferredBlock<GuhrioStukken.SchakelBlok> SCHAKELBLOK = BLOCKS.registerBlock("guhrio_schakelblok",
            GuhrioStukken.SchakelBlok::new, () -> vast(MapColor.COLOR_LIGHT_BLUE));
    public static final DeferredBlock<GuhrioBlocks.GuhmbaPlek> GUHMBA_PLEK = BLOCKS.registerBlock("guhrio_guhmba_plek", GuhrioBlocks.GuhmbaPlek::new,
            GuhrioFeature::plek);
    public static final DeferredBlock<GuhrioStukken.SchildMikaPlek> SCHILD_MIKA_PLEK = BLOCKS.registerBlock("guhrio_schild_mika_plek",
            GuhrioStukken.SchildMikaPlek::new, GuhrioFeature::plek);
    public static final DeferredBlock<GuhrioStukken.PlofMikaPlek> PLOF_MIKA_PLEK = BLOCKS.registerBlock("guhrio_plof_mika_plek",
            GuhrioStukken.PlofMikaPlek::new, GuhrioFeature::plek);
    public static final DeferredBlock<GuhrioStukken.HapbloemPlek> HAPBLOEM_PLEK = BLOCKS.registerBlock("guhrio_hapbloem_plek",
            GuhrioStukken.HapbloemPlek::new, GuhrioFeature::plek);
    public static final DeferredBlock<GuhrioStukken.GrillspiesPlek> GRILLSPIES_PLEK = BLOCKS.registerBlock("guhrio_grillspies_plek",
            GuhrioStukken.GrillspiesPlek::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(1.5f, 6f).sound(SoundType.NETHERITE_BLOCK));
    public static final DeferredBlock<GuhrioStukken.PlatformPlek> PLATFORM_PLEK = BLOCKS.registerBlock("guhrio_platform_plek",
            GuhrioStukken.PlatformPlek::new, GuhrioFeature::plek);
    public static final DeferredBlock<GuhrioStukken.ValblokPlek> VALBLOK_PLEK = BLOCKS.registerBlock("guhrio_valblok_plek",
            GuhrioStukken.ValblokPlek::new, GuhrioFeature::plek);
    public static final DeferredBlock<GuhrioStukken.GuhshiEi> GUHSHI_EI = BLOCKS.registerBlock("guhrio_guhshi_ei", GuhrioStukken.GuhshiEi::new,
            () -> los(MapColor.COLOR_LIGHT_GREEN).lightLevel(s -> 7));
    public static final DeferredBlock<GuhrioStukken.GuhshiPlek> GUHSHI_PLEK = BLOCKS.registerBlock("guhrio_guhshi_plek", GuhrioStukken.GuhshiPlek::new,
            () -> los(MapColor.COLOR_LIGHT_GREEN));

    public static final List<DeferredItem<BlockItem>> BLOK_ITEMS = List.of(
            ITEMS.registerSimpleBlockItem(GROND), ITEMS.registerSimpleBlockItem(BLOK), ITEMS.registerSimpleBlockItem(SIERSTEEN),
            ITEMS.registerSimpleBlockItem(STARTBLOK),
            ITEMS.registerSimpleBlockItem(POORT),
            ITEMS.registerSimpleBlockItem(VRAAGBLOK), ITEMS.registerSimpleBlockItem(STEEN), ITEMS.registerSimpleBlockItem(ONZICHTBAAR),
            ITEMS.registerSimpleBlockItem(MUNT), ITEMS.registerSimpleBlockItem(VADSMUNT),
            ITEMS.registerSimpleBlockItem(VLAG), ITEMS.registerSimpleBlockItem(MAST), ITEMS.registerSimpleBlockItem(PIJP),
            ITEMS.registerSimpleBlockItem(PIJP_LIJF), ITEMS.registerSimpleBlockItem(DEUR), ITEMS.registerSimpleBlockItem(SCHAKELAAR),
            ITEMS.registerSimpleBlockItem(SCHAKELBLOK), ITEMS.registerSimpleBlockItem(GUHMBA_PLEK),
            ITEMS.registerSimpleBlockItem(SCHILD_MIKA_PLEK), ITEMS.registerSimpleBlockItem(PLOF_MIKA_PLEK), ITEMS.registerSimpleBlockItem(HAPBLOEM_PLEK),
            ITEMS.registerSimpleBlockItem(GRILLSPIES_PLEK), ITEMS.registerSimpleBlockItem(PLATFORM_PLEK), ITEMS.registerSimpleBlockItem(VALBLOK_PLEK),
            ITEMS.registerSimpleBlockItem(GUHSHI_EI), ITEMS.registerSimpleBlockItem(GUHSHI_PLEK));

    /** Pictures only (the panel, the Guhdex, a level's signs): the two power-ups and the shadow of a vadsmunt you already have. */
    public static final DeferredItem<Item> SUPERKNABBEL = ITEMS.registerSimpleItem("guhrio_superknabbel");
    public static final DeferredItem<Item> VUURPEPER = ITEMS.registerSimpleItem("guhrio_vuurpeper");
    public static final DeferredItem<Item> VADSMUNT_SCHIM = ITEMS.registerSimpleItem("guhrio_vadsmunt_schim");

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GuhrioBlocks.StartBlockEntity>> START_BE = BLOCK_ENTITIES.register("guhrio_start",
            () -> new BlockEntityType<>(GuhrioBlocks.StartBlockEntity::new, STARTBLOK.get(), POORT.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GuhrioBlocks.StukBlockEntity>> STUK_BE = BLOCK_ENTITIES.register("guhrio_stuk",
            () -> new BlockEntityType<>(GuhrioBlocks.StukBlockEntity::new, VRAAGBLOK.get(), STEEN.get(), MUNT.get(), ONZICHTBAAR.get(), VADSMUNT.get(),
                    SCHAKELAAR.get(), SCHAKELBLOK.get(), GUHSHI_EI.get(), GUHSHI_PLEK.get()));

    // the creatures and moving things of a level (never saved: they belong to a level somebody plays)
    public static final DeferredHolder<EntityType<?>, EntityType<GuhmbaEntity>> GUHMBA = wezen("guhmba", GuhmbaEntity::new, 0.8f, 0.8f, 2);
    public static final DeferredHolder<EntityType<?>, EntityType<SchildMikaEntity>> SCHILD_MIKA = wezen("schild_mika", SchildMikaEntity::new, 0.8f, 0.9f, 1);
    public static final DeferredHolder<EntityType<?>, EntityType<PlofMikaEntity>> PLOF_MIKA = wezen("plof_mika", PlofMikaEntity::new, 1.6f, 1.8f, 1);
    public static final DeferredHolder<EntityType<?>, EntityType<HapbloemEntity>> HAPBLOEM = wezen("hapbloem", HapbloemEntity::new, 0.8f, 1.5f, 1);
    public static final DeferredHolder<EntityType<?>, EntityType<GrillspiesEntity>> GUHRIO_GRILLSPIES = wezen("guhrio_grillspies", GrillspiesEntity::new, 1f, 1f, 600);
    public static final DeferredHolder<EntityType<?>, EntityType<KnabbelEntity>> GUHRIO_KNABBEL = wezen("guhrio_knabbel", KnabbelEntity::new, 0.4f, 0.4f, 1);
    public static final DeferredHolder<EntityType<?>, EntityType<PlatformEntity>> PLATFORM = wezen("guhrio_platform", PlatformEntity::new, 1f, 0.5f, 600);
    public static final DeferredHolder<EntityType<?>, EntityType<ValblokEntity>> VALBLOK = wezen("guhrio_valblok", ValblokEntity::new, 1f, 0.5f, 1);

    /** The questline "guhrio" (its steps: {@link GuhrioKasteel}). */
    public static final Verhaallijn LIJN = GuhrioKasteel.LIJN;

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> wezen(String id, EntityType.EntityFactory<T> maker, float breed,
                                                                                         float hoog, int elke) {
        return ENTITY_TYPES.register(id, () -> EntityType.Builder.of(maker, MobCategory.MISC).sized(breed, hoog).clientTrackingRange(8)
                .updateInterval(elke).noSave().fireImmune().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

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

    /** The spot of a creature: invisible, nothing can break or push it. */
    private static BlockBehaviour.Properties plek() {
        return BlockBehaviour.Properties.of().noCollision().noLootTable().strength(-1f, 3600000f).noOcclusion()
                .isValidSpawn((s, l, p, e) -> false).pushReaction(PushReaction.BLOCK);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        NeoForge.EVENT_BUS.register(GuhrioEvents.class);
        GuhrioKasteel.registreer();
    }

    public static void payloads(PayloadRegistrar registrar) {
        GuhrioPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<BlockItem> item : BLOK_ITEMS) {
            output.accept(new ItemStack(item.get()));
        }
        output.accept(new ItemStack(SUPERKNABBEL.get()));
        output.accept(new ItemStack(VUURPEPER.get()));
    }

    private GuhrioFeature() {
    }
}
