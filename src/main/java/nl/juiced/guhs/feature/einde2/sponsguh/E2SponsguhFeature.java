package nl.juiced.guhs.feature.einde2.sponsguh;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
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
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.balto.BaltoBlocks;
import nl.juiced.guhs.feature.huisje.Speelgoed;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.landdiertjes.LanddierItems;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModItems;

/**
 * einde2, slice sponsguh_wezens: the second story of the Guheinde, Sponsguh Kaasbroek (DESIGN 5b), as far as it is pure
 * CONTENT: who and what exists. The story itself (quests, scenes, who says what, who gets what) is phase B.
 * <ul>
 *   <li>the cast as creatures: twenty entity types on the one class {@link SponsguhWezen} (the table: {@link Cast}), each with
 *       its own python-written GeckoLib model and a creative-only spawn egg; no natural spawn;</li>
 *   <li>the buddy and the pet: a player's own {@link Sponsguh} and {@link Gerrit} (one each, never lost: {@link Maatjes});</li>
 *   <li>the ride on {@link Hasselguh David Hasselguh} and the story car, the {@link Knabbelburgermobiel};</li>
 *   <li>the blocks: {@link Karen}, the grill and the {@link Besteltafel order table} of De Krokante Knabbel, the
 *       knabbelkoorvrucht house as building blocks (wall, roof, round window, door), the bubble blower for guhs
 *       ({@link BellenSpel}) and the spooklantaarn; the food: Knabbelburger and kwallenjam;</li>
 *   <li>seven clothes for a player's own guhs (source {@code einde2_sponsguh}; phase B hands them out).</li>
 * </ul>
 * hook fase B: {@link SponsguhWezen#BIJ_KLIK} / {@code zetAnimatie} / {@code zetStil}, the flag setters of the four flag
 * characters, {@link Sponsguh#geef}, {@link Gerrit#geef}, {@link Hasselguh#draag}, {@link Knabbelburgermobiel#zet},
 * {@link Karen#zetGezicht}, {@link Besteltafel#BIJ_BEL}, and the grill asks {@code Einde2Haken.KNABBELBURGER_RECEPT}.
 * <p>
 * Resources: tools/features/einde2_sponsguh_wezens.py (models: einde2_sponsguh_wezens_modellen.py, blocks and items:
 * einde2_sponsguh_wezens_blokken.py, texts: einde2_sponsguh_wezens_tekst.py).
 */
public final class E2SponsguhFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, Guhs.MODID);
    /** Random salt of this slice (build contract 3.1). */
    public static final long SALT = 21401550L;

    // --- the blocks the two builds of the story place (build contract 3.4) --------------------------------------------------
    public static final DeferredBlock<SponsguhBlokken.KarenBlock> KAREN = BLOCKS.registerBlock("karen", SponsguhBlokken.KarenBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(2f, 6f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(s -> 7).pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<SponsguhBlokken.GrillBlock> KROKANTE_GRILL = BLOCKS.registerBlock("krokante_grill", SponsguhBlokken.GrillBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3f, 6f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(s -> s.getValue(SponsguhBlokken.GrillBlock.BAKT) ? 9 : 0).pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<SponsguhBlokken.BesteltafelBlock> BESTELTAFEL = BLOCKS.registerBlock("besteltafel", SponsguhBlokken.BesteltafelBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
    /** The knabbelkoorvrucht house as building blocks: the fruit shell, the leafy crown, a round window, a round door. */
    public static final DeferredBlock<Block> VRUCHTHUIS_WAND = BLOCKS.registerBlock("vruchthuis_wand", Block::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.5f, 4f).sound(SoundType.WOOD));
    public static final DeferredBlock<Block> VRUCHTHUIS_DAK = BLOCKS.registerBlock("vruchthuis_dak", Block::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.0f, 3f).sound(SoundType.AZALEA_LEAVES));
    public static final DeferredBlock<TransparentBlock> VRUCHTHUIS_RAAM = BLOCKS.registerBlock("vruchthuis_raam", TransparentBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.0f, 3f).sound(SoundType.GLASS).noOcclusion()
                    .isValidSpawn((s, l, p, t) -> false).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false)
                    .isViewBlocking((s, l, p) -> false));
    /** The round door of the fruit house opens and closes with a soft plop. */
    public static final BlockSetType VRUCHT_SET = BlockSetType.register(new BlockSetType("guhs:vruchthuis", true, true, true,
            BlockSetType.PressurePlateSensitivity.EVERYTHING, SoundType.WOOD, SoundEvents.CHERRY_WOOD_DOOR_CLOSE, SoundEvents.CHERRY_WOOD_DOOR_OPEN,
            SoundEvents.CHERRY_WOOD_TRAPDOOR_CLOSE, SoundEvents.CHERRY_WOOD_TRAPDOOR_OPEN, SoundEvents.CHERRY_WOOD_PRESSURE_PLATE_CLICK_OFF,
            SoundEvents.CHERRY_WOOD_PRESSURE_PLATE_CLICK_ON, SoundEvents.CHERRY_WOOD_BUTTON_CLICK_OFF, SoundEvents.CHERRY_WOOD_BUTTON_CLICK_ON));
    public static final DeferredBlock<DoorBlock> VRUCHTHUIS_DEUR = BLOCKS.registerBlock("vruchthuis_deur", p -> new DoorBlock(VRUCHT_SET, p),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2f).sound(SoundType.WOOD).noOcclusion()
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<SponsguhBlokken.BellenblaasBlock> BELLENBLAAS = BLOCKS.registerBlock("bellenblaas", SponsguhBlokken.BellenblaasBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.0f, 3f).sound(SoundType.WOOL).noOcclusion()
                    .pushReaction(PushReaction.DESTROY));
    /** A ghost-green lantern, hanging or standing. */
    public static final DeferredBlock<LanternBlock> SPOOKLANTAARN = BLOCKS.registerBlock("spooklantaarn", LanternBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SOUL_LANTERN).mapColor(MapColor.COLOR_LIGHT_GREEN).lightLevel(s -> 12));
    /** The nine, in the order of the creative tab. */
    public static final List<DeferredBlock<? extends Block>> BLOKKEN = List.of(KROKANTE_GRILL, BESTELTAFEL, KAREN, VRUCHTHUIS_WAND, VRUCHTHUIS_DAK,
            VRUCHTHUIS_RAAM, VRUCHTHUIS_DEUR, BELLENBLAAS, SPOOKLANTAARN);

    static {
        for (DeferredBlock<? extends Block> block : BLOKKEN) {       // each with a grey line that says what it is for (<block>.lore)
            ITEMS.registerItem(block.getId().getPath(), p -> new BaltoBlocks.LoreBlock(block.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
        }
    }

    /** Every bubble blower: guhs find one through it ({@link BellenSpel}). */
    public static final DeferredHolder<PoiType, PoiType> BELLENBLAAS_POI = POI_TYPES.register("einde2_sponsguh_wezens_bellenblaas",
            () -> new PoiType(ImmutableSet.<BlockState>copyOf(BELLENBLAAS.get().getStateDefinition().getPossibleStates()), 0, 1));

    // --- the food ------------------------------------------------------------------------------------------------------------
    /** The Knabbelburger: baked on the grill (by who knows the recipe) or by a Sponsguh buddy; never crafted. */
    public static final DeferredItem<LanddierItems.LoreItem> KNABBELBURGER = ITEMS.registerItem("knabbelburger", LanddierItems.LoreItem::new,
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.8f).build()));
    /** hook fase B: catching Kruimelkwallen with Patguh gives it; phase A has no recipe for it. */
    public static final DeferredItem<LanddierItems.LoreItem> KWALLENJAM = ITEMS.registerItem("kwallenjam", LanddierItems.LoreItem::new,
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.4f).build()));

    // --- the cast ------------------------------------------------------------------------------------------------------------
    private static final Map<Cast, DeferredHolder<EntityType<?>, ? extends EntityType<? extends SponsguhWezen>>> TYPES = new LinkedHashMap<>();
    private static final List<DeferredItem<SpawnEggItem>> EIEREN = new ArrayList<>();

    public static final DeferredHolder<EntityType<?>, EntityType<SponsguhEntity>> SPONSGUH = wezen(Cast.SPONSGUH, SponsguhEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<SponsguhWezen>> PATGUH = wezen(Cast.PATGUH, SponsguhWezen::new);
    public static final DeferredHolder<EntityType<?>, EntityType<MeneerKrabguhEntity>> MENEER_KRABGUH = wezen(Cast.MENEER_KRABGUH, MeneerKrabguhEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<SponsguhWezen>> PAREL_GUH = wezen(Cast.PAREL_GUH, SponsguhWezen::new);
    public static final DeferredHolder<EntityType<?>, EntityType<SponsguhWezen>> SANDY_GUH = wezen(Cast.SANDY_GUH, SponsguhWezen::new);
    public static final DeferredHolder<EntityType<?>, EntityType<GerritEntity>> GERRIT = wezen(Cast.GERRIT, GerritEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<SponsguhWezen>> OCTO_MIKA = wezen(Cast.OCTO_MIKA, SponsguhWezen::new);
    public static final DeferredHolder<EntityType<?>, EntityType<SponsguhWezen>> OCTOLIAM = wezen(Cast.OCTOLIAM, SponsguhWezen::new);
    public static final DeferredHolder<EntityType<?>, EntityType<SponsguhWezen>> PLANKMIKA = wezen(Cast.PLANKMIKA, SponsguhWezen::new);
    public static final DeferredHolder<EntityType<?>, EntityType<JufPufguhEntity>> JUF_PUFGUH = wezen(Cast.JUF_PUFGUH, JufPufguhEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<NeptuguhEntity>> NEPTUGUH = wezen(Cast.NEPTUGUH, NeptuguhEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<SponsguhWezen>> MINDY_GUH = wezen(Cast.MINDY_GUH, SponsguhWezen::new);
    public static final DeferredHolder<EntityType<?>, EntityType<DennisMikaEntity>> DENNIS_MIKA = wezen(Cast.DENNIS_MIKA, DennisMikaEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<SponsguhWezen>> CYCLOOP_MIKA = wezen(Cast.CYCLOOP_MIKA, SponsguhWezen::new);
    public static final DeferredHolder<EntityType<?>, EntityType<DavidHasselguhEntity>> DAVID_HASSELGUH = wezen(Cast.DAVID_HASSELGUH, DavidHasselguhEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<SponsguhWezen>> LOOPGRAAFMONSTER = wezen(Cast.LOOPGRAAFMONSTER, SponsguhWezen::new);
    public static final DeferredHolder<EntityType<?>, EntityType<SponsguhWezen>> BIKINIBODEM_GUH = wezen(Cast.BIKINIBODEM_GUH, SponsguhWezen::new);
    public static final DeferredHolder<EntityType<?>, EntityType<SponsguhWezen>> SPOOKKAPITEIN = wezen(Cast.SPOOKKAPITEIN, SponsguhWezen::new);
    public static final DeferredHolder<EntityType<?>, EntityType<SponsguhWezen>> ZEEMEERMAN_GUH = wezen(Cast.ZEEMEERMAN_GUH, SponsguhWezen::new);
    public static final DeferredHolder<EntityType<?>, EntityType<SponsguhWezen>> MOSSELGUH = wezen(Cast.MOSSELGUH, SponsguhWezen::new);
    /** The story car: a vehicle, not one of the cast; it has no item at all, not even a spawn egg (only Knabbelburgermobiel.zet makes one). */
    public static final DeferredHolder<EntityType<?>, EntityType<KnabbelburgermobielEntity>> KNABBELBURGERMOBIEL = ENTITY_TYPES.register("knabbelburgermobiel",
            () -> EntityType.Builder.<KnabbelburgermobielEntity>of(KnabbelburgermobielEntity::new, MobCategory.MISC).sized(1.7f, 1.2f).eyeHeight(0.9f)
                    .clientTrackingRange(10).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("knabbelburgermobiel"))));

    /** One character: its entity type (hitbox from the table) and its spawn egg {@code <id>_spawn_egg}. */
    private static <T extends SponsguhWezen> DeferredHolder<EntityType<?>, EntityType<T>> wezen(Cast cast, EntityType.EntityFactory<T> maker) {
        String id = cast.id();
        DeferredHolder<EntityType<?>, EntityType<T>> type = ENTITY_TYPES.register(id,
                () -> EntityType.Builder.of(maker, MobCategory.MISC).sized(cast.breed, cast.hoog).eyeHeight(cast.oog)
                        .clientTrackingRange(cast == Cast.DAVID_HASSELGUH ? 12 : 8).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
        TYPES.put(cast, type);
        EIEREN.add(ModItems.spawnEgg(ITEMS, id + "_spawn_egg", type));
        return type;
    }

    /** The entity type of a character of the cast. */
    public static EntityType<? extends SponsguhWezen> type(Cast cast) {
        return TYPES.get(cast).get();
    }

    // --- sounds (vanilla sounds, pitched, in sounds.json) ----------------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> GUH_BABBEL = sound("einde2_sponsguh_wezens.guh_babbel");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIKA_BROM = sound("einde2_sponsguh_wezens.mika_brom");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPONSGUH_LACH = sound("einde2_sponsguh_wezens.sponsguh_lach");
    public static final DeferredHolder<SoundEvent, SoundEvent> GERRIT_NJEG = sound("einde2_sponsguh_wezens.gerrit_njeg");
    public static final DeferredHolder<SoundEvent, SoundEvent> OCTO_KLARINET = sound("einde2_sponsguh_wezens.octo_klarinet");
    public static final DeferredHolder<SoundEvent, SoundEvent> MONSTER_BRUL = sound("einde2_sponsguh_wezens.monster_brul");
    public static final DeferredHolder<SoundEvent, SoundEvent> MONSTER_ZING = sound("einde2_sponsguh_wezens.monster_zing");
    public static final DeferredHolder<SoundEvent, SoundEvent> HASSELGUH_PLONS = sound("einde2_sponsguh_wezens.hasselguh_plons");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOBIEL_MOTOR = sound("einde2_sponsguh_wezens.mobiel_motor");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOBIEL_TOET = sound("einde2_sponsguh_wezens.mobiel_toet");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRILL_SIS = sound("einde2_sponsguh_wezens.grill_sis");
    public static final DeferredHolder<SoundEvent, SoundEvent> BESTELBEL = sound("einde2_sponsguh_wezens.bestelbel");
    public static final DeferredHolder<SoundEvent, SoundEvent> KAREN_PIEP = sound("einde2_sponsguh_wezens.karen_piep");
    public static final DeferredHolder<SoundEvent, SoundEvent> BELLEN = sound("einde2_sponsguh_wezens.bellen");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    /** The seven clothes of the story (GuhClothes, between the markers einde2_sponsguh), all from the one source. */
    public static final String KLEDINGBRON = "einde2_sponsguh";
    public static final List<GuhClothes> KLEDING = List.of(GuhClothes.SPONSGUH_VIERKANTE_BROEK, GuhClothes.SPONSGUH_KNABBELPETJE,
            GuhClothes.SPONSGUH_BLOEMETJESBROEK, GuhClothes.SPONSGUH_LUCHTHELM, GuhClothes.SPONSGUH_ANTENNES, GuhClothes.SPONSGUH_IJSHOEDJE,
            GuhClothes.SPONSGUH_HELDENCAPE);

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        POI_TYPES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            for (Cast cast : Cast.values()) {
                event.put(type(cast), SponsguhWezen.createAttributes().build());
            }
        });
        // the buddy and the pet have a Guhdex page (bonus pages); the rest of the cast has none (no spoilers)
        GuhDex.creaturePage(GuhVariant.SPONSGUH, SPONSGUH);
        GuhDex.creaturePage(GuhVariant.GERRIT, GERRIT);
        for (GuhClothes stuk : KLEDING) {
            KledingBronnen.bron(stuk, KLEDINGBRON);                          // hook fase B: the story hands them out
        }
        Speelgoed.registreer(BellenSpel.SOORT);
        NeoForge.EVENT_BUS.register(Maatjes.class);
        NeoForge.EVENT_BUS.register(Hasselguh.class);
        NeoForge.EVENT_BUS.addListener(E2SponsguhCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KNABBELBURGER.get()));
        output.accept(new ItemStack(KWALLENJAM.get()));
        for (DeferredBlock<? extends Block> block : BLOKKEN) {
            output.accept(new ItemStack(block.get()));
        }
        for (DeferredItem<SpawnEggItem> ei : EIEREN) {
            output.accept(new ItemStack(ei.get()));
        }
    }

    private E2SponsguhFeature() {
    }
}
