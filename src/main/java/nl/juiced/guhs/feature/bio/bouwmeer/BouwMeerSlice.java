package nl.juiced.guhs.feature.bio.bouwmeer;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.guhpixel.blok.DecoBlock;
import nl.juiced.guhs.feature.guhpixel.blok.LoreBlockItem;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.item.SuperkompasItem;

/**
 * biomes3 slice "bouw-meer": what stands at the Bloesemmeertje.
 * <ul>
 *   <li><b>Het botenhuisje</b> (structure {@code botenhuisje}, on a lake shore): a small light boathouse on posts with a
 *       jetty out over the water. {@link MeerpaalBlock De meerpaal} in the jetty is its anchor: a
 *       {@link RoeibootjeEntity roeibootje} always lies ready in the berth, the {@link Visserguh visser-guh} sits on the
 *       jetty's end, and the {@link SteigerlantaarnBlock steigerlantaarn} there lights at dusk.</li>
 *   <li><b>Het picknickeilandje</b> (structure {@code picknickeilandje}, rare, under the big tree of a large lake island): a
 *       picnic rug with the {@link Hanami hanami guhs} and {@link MandBlock de picknickmand} (a treat for every player,
 *       once), paper lanterns in the island's own tree.</li>
 *   <li>Three decorations for a base by the water, the visser-guh's reward (also craftable): the steigerlantaarn, a
 *       fishing rod on a stand and a koi windsock.</li>
 * </ul>
 * Both structures are data of the type {@code guhs:bio_plek} and stand in the Superkompas tab "knus".
 * Resources: tools/features/bio_bouw_meer.py.
 */
public final class BouwMeerSlice {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    /** The hidden proof advancements {@code guhs:quest/<name>} of this slice (for slice systemen). */
    public static final List<String> BEWIJZEN = List.of("botenhuisje_gevonden", "botenhuisje_visser", "botenhuisje_koivoer", "botenhuisje_visser_klaar",
            "roeibootje_gevaren", "picknickeilandje_gevonden", "picknickeilandje_mand", "hanami_gesproken");

    // --- the boat -----------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<RoeibootjeEntity>> ROEIBOOTJE = ENTITY_TYPES.register("roeibootje",
            () -> EntityType.Builder.<RoeibootjeEntity>of(RoeibootjeEntity::new, MobCategory.MISC).noLootTable().sized(1.375f, 0.5625f).eyeHeight(0.5625f)
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("roeibootje"))));

    // --- the anchors (part of their building: no item, unbreakable in survival) ---------------------------------------------
    private static BlockBehaviour.Properties anker(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(-1f, 3600000f).noLootTable().noOcclusion().sound(SoundType.WOOD)
                .pushReaction(PushReaction.BLOCK);
    }

    public static final DeferredBlock<MeerpaalBlock> MEERPAAL = BLOCKS.registerBlock("botenhuisje_meerpaal", MeerpaalBlock::new, () -> anker(MapColor.SAND));
    public static final DeferredBlock<MandBlock> MAND = BLOCKS.registerBlock("picknickeilandje_mand", MandBlock::new, () -> anker(MapColor.COLOR_PINK));

    // --- the decorations ----------------------------------------------------------------------------------------------------
    public static final DeferredBlock<SteigerlantaarnBlock> STEIGERLANTAARN = BLOCKS.registerBlock("botenhuisje_steigerlantaarn", SteigerlantaarnBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.4f).noOcclusion().sound(SoundType.LANTERN)
                    .lightLevel(s -> s.getValue(SteigerlantaarnBlock.LIT) ? SteigerlantaarnBlock.LICHT : 0).pushReaction(PushReaction.DESTROY));
    /** A fishing rod leaning in a little stand, a petal on its line (the shape of tools/features/bio_bouw_meer.py's model, facing north). */
    public static final DeferredBlock<DecoBlock> HENGELSTANDAARD = BLOCKS.registerBlock("botenhuisje_hengelstandaard",
            p -> new DecoBlock(p, Shapes.or(Block.box(4, 0, 5, 12, 3, 11), Block.box(6, 3, 6, 10, 16, 10))), () -> DecoBlock.props().sound(SoundType.BAMBOO_WOOD));
    /** A koi windsock on a pole. */
    public static final DeferredBlock<DecoBlock> KOIWINDZAK = BLOCKS.registerBlock("botenhuisje_koiwindzak",
            p -> new DecoBlock(p, Shapes.or(Block.box(7, 0, 7, 9, 16, 9), Block.box(5, 9, 9, 11, 15, 16))), () -> DecoBlock.props().sound(SoundType.WOOL));

    public static final DeferredItem<BlockItem> STEIGERLANTAARN_ITEM = lore(STEIGERLANTAARN);
    public static final DeferredItem<BlockItem> HENGELSTANDAARD_ITEM = lore(HENGELSTANDAARD);
    public static final DeferredItem<BlockItem> KOIWINDZAK_ITEM = lore(KOIWINDZAK);

    private static DeferredItem<BlockItem> lore(DeferredBlock<? extends Block> blok) {
        return ITEMS.registerItem(blok.getId().getPath(), p -> new LoreBlockItem(blok.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        NpcRollen.zet(GuhNpcEntity.Kind.BOTENHUISJE_VISSERGUH, Visserguh.ROL);
        for (GuhNpcEntity.Kind kind : List.of(GuhNpcEntity.Kind.HANAMI_GUH, GuhNpcEntity.Kind.HANAMI_GUH_SLAAPT, GuhNpcEntity.Kind.HANAMI_BLOESEMGUH,
                GuhNpcEntity.Kind.HANAMI_BLOESEMGUH_SLAAPT)) {
            NpcRollen.zet(kind, Hanami.ROL);
        }
        SuperkompasItem.voegToe("knus", "botenhuisje");
        SuperkompasItem.voegToe("knus", "picknickeilandje");
        NeoForge.EVENT_BUS.register(BouwMeerEvents.class);
        NeoForge.EVENT_BUS.addListener(BouwMeerEvents::commando);
        BouwMeerEvents.zelftest();
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<? extends Item> item : List.of(STEIGERLANTAARN_ITEM, HENGELSTANDAARD_ITEM, KOIWINDZAK_ITEM)) {
            output.accept(new ItemStack(item.get()));
        }
    }

    private BouwMeerSlice() {
    }
}
