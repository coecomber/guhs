package nl.juiced.guhs.feature.ringh5;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.ring.Gaven;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.Smikagol;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bezetting;

/**
 * bbq2 (ring-h5): chapter 5 of the Knabbelring, "De Zwarte Roosterpoort" (DESIGN_130 4). Resources: tools/features/ring_h5.py
 * (+ ring_h5_bouw, ring_h5_modellen, ring_h5_tekst).
 * <ul>
 *   <li>The structure {@code guhs:zwarte_roosterpoort} ({@link #STRUCTUUR}): a closed valley in the Asdal with the camp, the
 *       ridge, the fields, the lane, the wall with the gate of grates and the tower of the Eye; behind Guhdalfs sluier until
 *       chapter 4 is done ({@link Ring#sluier}), one copy per world.</li>
 *   <li>{@link OogEntity} (the fixed id {@code oog_van_sausron}): the Eye, with a gaze you can read and avoid ({@link Blik});
 *       {@link RoosterwachterEntity}: the Mika guards only the ring gets you past; two riders of the Nine on patrol in the
 *       lane (ring-kern's {@code Negen.maakPatrouille}).</li>
 *   <li>{@link Hoofdstuk}: the ten steps, per player: Boromika's moment, Smikagol becomes the guide, the sneak sections with
 *       the Elfenmanteltje, the Lichtflesje and the ring, and the return of Guhdalf de Witte at the side door
 *       ({@link Scenes}: one narrator card, three camera scenes).</li>
 *   <li>The fixed id {@code oog_van_sausron_beeldje} ({@link BeeldjeBlock}): the statuette that blinks, an end reward of
 *       the story (ring-kern hands it out).</li>
 * </ul>
 * Being seen (by the Eye, a guard or a rider) only ever puts a player back on their last rest point: nobody is hurt and
 * nothing is lost. Field names of CONTRACT_130 7: {@link #OOG_VAN_SAUSRON}, {@link #OOG_VAN_SAUSRON_BEELDJE}(_ITEM), {@link #LIJN}.
 */
public final class RingH5Feature {
    /** The structure of this chapter. */
    public static final String STRUCTUUR = "zwarte_roosterpoort";
    /**
     * How far around the valley's box Guhdalfs sluier (and the protection) reaches: not at all. The valley may come to stand
     * in solid rock, and then a player has to dig the last blocks to one of its three ways in ({@link Plekken#INGANGEN}).
     */
    public static final int RAND = 0;

    /** The questline of this chapter (ring-kern and the travel map refer to this field and its id). */
    public static final Verhaallijn LIJN = Verhaallijn.maak("ring_h5", "knabbelring").stappen(Hoofdstuk.STAPPEN).na("ring_h4")
            .icoon("guhs:oog_van_sausron_beeldje").nodig(Hoofdstuk::nodig).beloningen(Hoofdstuk::beloningen).doel(Hoofdstuk::doel).registreer();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    /** The Eye (fixed id). */
    public static final DeferredHolder<EntityType<?>, EntityType<OogEntity>> OOG_VAN_SAUSRON = ENTITY_TYPES.register("oog_van_sausron",
            () -> EntityType.Builder.of(OogEntity::new, MobCategory.MISC).sized(3.0f, 3.0f).eyeHeight(1.6f).clientTrackingRange(12).updateInterval(2)
                    .fireImmune().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("oog_van_sausron"))));
    /** A Mika guard of het Wachthek. */
    public static final DeferredHolder<EntityType<?>, EntityType<RoosterwachterEntity>> ROOSTERWACHTER = ENTITY_TYPES.register("ringh5_roosterwachter",
            () -> EntityType.Builder.of(RoosterwachterEntity::new, MobCategory.MISC).sized(0.9f, 0.8f).eyeHeight(0.55f).clientTrackingRange(8).fireImmune()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("ringh5_roosterwachter"))));

    /** The statuette of the Eye (fixed id): it blinks. */
    public static final DeferredBlock<BeeldjeBlock> OOG_VAN_SAUSRON_BEELDJE = BLOCKS.registerBlock("oog_van_sausron_beeldje", BeeldjeBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.5f).sound(SoundType.STONE).noOcclusion().lightLevel(s -> 7)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> OOG_VAN_SAUSRON_BEELDJE_ITEM = ITEMS.registerSimpleBlockItem(OOG_VAN_SAUSRON_BEELDJE);
    /** The grate of the gate: a full block you look through. */
    public static final DeferredBlock<Block> POORTROOSTER = BLOCKS.registerBlock("ringh5_poortrooster", Block::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5f, 6f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()
                    .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false));
    public static final DeferredItem<BlockItem> POORTROOSTER_ITEM = ITEMS.registerSimpleBlockItem(POORTROOSTER);
    /** The light of the Eye's gaze on the ground: only ever shown by a block display, never placed (no item). */
    public static final DeferredBlock<Block> BLIK = BLOCKS.registerBlock("ringh5_blik", Block::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(-1f, 3600000f).noCollision().noOcclusion().noLootTable()
                    .lightLevel(s -> 10).pushReaction(PushReaction.DESTROY));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(OOG_VAN_SAUSRON.get(), OogEntity.createAttributes().build());
            event.put(ROOSTERWACHTER.get(), RoosterwachterEntity.createAttributes().build());
        });
        NeoForge.EVENT_BUS.register(RingH5Events.class);
        NeoForge.EVENT_BUS.addListener(RingH5Commands::register);

        // the valley: hidden, protected, open for whoever finished chapter 4
        Ring.sluier(STRUCTUUR, RAND, 5);
        // the story
        Scenes.registreer();
        LIJN.opStap(Hoofdstuk::opStap);
        NpcRollen.zet(GuhNpcEntity.Kind.BOROMIKA, BOROMIKA_PLEK, (npc, p) -> Hoofdstuk.klikBoromika(npc, p));
        Praat.luister(Hoofdstuk.PRAAT_SMIKAGOL, Hoofdstuk::antwoordSmikagol);
        Smikagol.BIJ_KLIK.add(Hoofdstuk::klikSmikagol);
        Gaven.BIJ_LICHT.add(Hoofdstuk::licht);
        Schijn.registreer(Hoofdstuk::deurSchijn);

        // who lives there (also the template's own Boromika is repaired this way)
        Bezetting.npc(BOROMIKA_ID, STRUCTUUR, null, Plekken.BOROMIKA, GuhNpcEntity.Kind.BOROMIKA, BOROMIKA_PLEK, 120f);
        Bezetting.wezen("ringh5_oog", STRUCTUUR, null, Plekken.OOG, Hoofdstuk::maakOog, 6);
        List<BlockPos> wachters = List.of(Plekken.WACHTER_1, Plekken.WACHTER_2, Plekken.WACHTER_3);
        for (int i = 0; i < wachters.size(); i++) {
            BlockPos post = wachters.get(i);
            Bezetting.wezen("ringh5_wachter_" + (i + 1), STRUCTUUR, null, post, (level, plek, draai) -> Hoofdstuk.maakWachter(level, post, plek, draai), 4);
        }
        Bezetting.wezen("ringh5_ruiter_1", STRUCTUUR, null, Plekken.RUITER_1.get(0), (level, plek, draai) -> Hoofdstuk.maakRuiter(level, Plekken.RUITER_1, plek, draai),
                RUITER_ZOEK);
        Bezetting.wezen("ringh5_ruiter_2", STRUCTUUR, null, Plekken.RUITER_2.get(0), (level, plek, draai) -> Hoofdstuk.maakRuiter(level, Plekken.RUITER_2, plek, draai),
                RUITER_ZOEK);
    }

    /** The Bezetting id and the NpcRollen plek of Boromika at the camp. */
    public static final String BOROMIKA_ID = "ringh5_boromika", BOROMIKA_PLEK = "ringh5_kamp";
    /** A rider on its round (or on a chase) is looked for this far from where its round starts. */
    private static final int RUITER_ZOEK = 44;

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(OOG_VAN_SAUSRON_BEELDJE_ITEM.get()));
        output.accept(new ItemStack(POORTROOSTER_ITEM.get()));
    }

    private RingH5Feature() {
    }
}
