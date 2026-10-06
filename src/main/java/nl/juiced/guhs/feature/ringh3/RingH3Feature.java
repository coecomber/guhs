package nl.juiced.guhs.feature.ringh3;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.StructureType;
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
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;

/**
 * bbq2 (ring-h3): chapter 3 of "In de ban van de Knabbelring": <b>De Mijnen van Knabbelmoria</b> (DESIGN_130 4). Resources:
 * tools/features/ring_h3.py (+ ring_h3_bouw, _modellen, _scene, _tekst, _java, _beeld, _wiki).
 * <p>
 * One structure, {@code guhs:knabbelmoria} (type {@link MijnStructure}): a mine sunk into the rock under a cave floor of the Houtskoolvlakte, exactly once per
 * world, hidden behind Guhdalfs sluier until the player's own story gets there ({@link Ring#sluier}). The questline
 * {@link #LIJN} ({@code ring_h3}, seven steps, per player):
 * <ol start="0">
 *   <li>find the gate (the narrator card);</li>
 *   <li>the riddle of the gate: "zeg njeg en treed binnen" ({@link Raadsels});</li>
 *   <li>the Hal van de Hefbomen: four levers in the order of the rhyme;</li>
 *   <li>the well room: Pippguh and the bucket (scene {@link Scenes#EMMER});</li>
 *   <li>Gimguh's hidden door: knock three times on the right rune;</li>
 *   <li>the great hall: the player's own {@link BarbecuerogEntity Barbecuerog} wakes ({@link Achtervolging}); run, over the
 *       Brokkelpad ({@link RingH3Blocks.Brokkelsteen}) and the bridge, to the scene {@link Scenes#BRUG}: "YOU.. SHALL.. NOT..
 *       VADS!";</li>
 *   <li>up the long stair and out: Araguh ({@link Rollen}).</li>
 * </ol>
 * Everything is per player and nothing in the building changes for good: doors close again ({@link Deuren}), the path grows
 * back, the bridge only breaks in the eyes of whoever watches ({@link Brug}). Nothing here hurts anybody: the Barbecuerog
 * shoves and puts back, a fall ends at the rest fire.
 * <p>
 * The fixed id of CONTRACT_130 7 keeps its field name: {@link #BARBECUEROG}; {@link #LIJN} keeps its name and id.
 */
public final class RingH3Feature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Guhs.MODID);
    /** The structure type of the mine: a barbecueput-style cave building that stands on the LOWEST cave floor of its column. */
    public static final DeferredHolder<StructureType<?>, StructureType<MijnStructure>> MIJN_TYPE =
            STRUCTURE_TYPES.register("ringh3_mijn", () -> () -> MijnStructure.CODEC);
    /** The narrator card of the chapter (also the page behind the picture-book replay of the two scenes). */
    public static final String KAART = "ring_h3";
    public static final int KAART_REGELS = 4;

    /** The questline of the chapter (texts: tools/features/ring_h3.py). */
    public static final Verhaallijn LIJN = Verhaallijn.maak("ring_h3", "knabbelring").stappen(7).na("ring_h2").icoon("guhs:ringh3_rune")
            .doel((p, stap) -> Ring.doel(3))
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:ringh3_rune", "gui.guhs.verhalen.ring_h3.beloning.rune", RingH3Feature.LIJN.klaar(p))))
            .registreer();

    // --- the creature (fixed id) ---------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<BarbecuerogEntity>> BARBECUEROG = ENTITY_TYPES.register("barbecuerog",
            () -> EntityType.Builder.of(BarbecuerogEntity::new, MobCategory.MISC).sized(4.6f, 9.6f).eyeHeight(8.7f).fireImmune().noSave()
                    .clientTrackingRange(12).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("barbecuerog"))));

    // --- the blocks of the mine ------------------------------------------------------------------------------------------------
    public static final DeferredBlock<RingH3Blocks.Brokkelsteen> BROKKELSTEEN = BLOCKS.registerBlock("ringh3_brokkelsteen", RingH3Blocks.Brokkelsteen::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(-1.0f, 3600000f).sound(SoundType.BASALT).noOcclusion()
                    .dynamicShape().pushReaction(PushReaction.BLOCK).noLootTable());
    public static final DeferredBlock<RingH3Blocks.Hendel> HENDEL = BLOCKS.registerBlock("ringh3_hendel", RingH3Blocks.Hendel::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(-1.0f, 3600000f).sound(SoundType.METAL).noOcclusion().noCollision()
                    .pushReaction(PushReaction.BLOCK).noLootTable());
    /** A rune stone: also the keepsake of the chapter (four of them from Araguh; the "njeg" rune glows). */
    public static final DeferredBlock<RingH3Blocks.Rune> RUNE = BLOCKS.registerBlock("ringh3_rune", RingH3Blocks.Rune::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.5f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS)
                    .lightLevel(s -> s.getValue(RingH3Blocks.Rune.TEKEN) == Plekken.RUNE_NJEG ? 9 : 0));
    public static final DeferredItem<BlockItem> BROKKELSTEEN_ITEM = ITEMS.registerSimpleBlockItem(BROKKELSTEEN);
    public static final DeferredItem<BlockItem> HENDEL_ITEM = ITEMS.registerSimpleBlockItem(HENDEL);
    public static final DeferredItem<BlockItem> RUNE_ITEM = ITEMS.registerSimpleBlockItem(RUNE);

    // --- sounds (vanilla sounds, pitched and layered, in sounds.json) -------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> BRUL = sound("ringh3.barbecuerog.brul");
    public static final DeferredHolder<SoundEvent, SoundEvent> GROM = sound("ringh3.barbecuerog.grom");
    public static final DeferredHolder<SoundEvent, SoundEvent> STAP = sound("ringh3.barbecuerog.stap");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZWEEP = sound("ringh3.barbecuerog.zweep");
    public static final DeferredHolder<SoundEvent, SoundEvent> ONTBRAND = sound("ringh3.barbecuerog.ontbrand");
    public static final DeferredHolder<SoundEvent, SoundEvent> TROMMEL = sound("ringh3.trommel");
    public static final DeferredHolder<SoundEvent, SoundEvent> EMMER = sound("ringh3.emmer");
    public static final DeferredHolder<SoundEvent, SoundEvent> STAF = sound("ringh3.staf");
    public static final DeferredHolder<SoundEvent, SoundEvent> BREUK = sound("ringh3.breuk");
    public static final DeferredHolder<SoundEvent, SoundEvent> VAL = sound("ringh3.val");
    public static final DeferredHolder<SoundEvent, SoundEvent> POORT = sound("ringh3.poort");
    public static final DeferredHolder<SoundEvent, SoundEvent> KLONK = sound("ringh3.klonk");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKKEL = sound("ringh3.brokkel");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String naam) {
        return SOUNDS.register(naam, () -> SoundEvent.createVariableRangeEvent(Guhs.id(naam)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(BARBECUEROG.get(), BarbecuerogEntity.createAttributes().build()));
        NeoForge.EVENT_BUS.register(MijnEvents.class);
        NeoForge.EVENT_BUS.addListener(RingH3Commands::register);

        // the structure: hidden and protected (one box: the template + 6) until the player's story reaches chapter 3
        Ring.sluier(Mijn.STRUCTUUR, 6, 3);
        Verteller.registreer(KAART, KAART_REGELS, LIJN.id());
        Scenes.registreer();
        Raadsels.registreer();
        Rollen.registreer();
    }

    public static void payloads(PayloadRegistrar registrar) {
        RingH3Payloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(RUNE_ITEM.get()));
        output.accept(new ItemStack(BROKKELSTEEN_ITEM.get()));
        output.accept(new ItemStack(HENDEL_ITEM.get()));
    }

    private RingH3Feature() {
    }
}
