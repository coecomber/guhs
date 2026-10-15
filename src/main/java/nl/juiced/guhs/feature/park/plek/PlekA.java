package nl.juiced.guhs.feature.park.plek;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.guhpixel.blok.LoreBlockItem;
import nl.juiced.guhs.feature.park.ParkFeature;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.waterdiertjes.GuhEendjeEntity;
import nl.juiced.guhs.feature.waterdiertjes.WaterdiertjesFeature;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.ModDimensions;

/**
 * 1.4.2: five random plekjes of Het Knabbelpark (part A of the park-plekjes; the park itself: {@link ParkFeature}, the
 * plekjes API: {@code feature.park.ParkPlekjes}). Each is a rigid piece of the pool {@code guhs:knabbelpark/plekjes}
 * (templates: tools/features/park_plek_a.py; their spots: {@link PlekAPlekken}):
 * <ul>
 *   <li><b>de eendenvijver</b>: a pond with two guh-eendje mamas and their rijtjes kuikentjes (feed them bread, seeds or a
 *       kaasknabbel: kwak-njeg!) and a little jetty where a rowing boat (the roeibootje of the Bloesemmeertje) always
 *       lies ready ({@link PlekAEvents});</li>
 *   <li><b>de speeltuin</b>: a slide, two swings, a seesaw and a sandbox with a kaasknabbel a day per player
 *       ({@link SpeelBlokken}, {@link ParkZitje});</li>
 *   <li><b>de picknickweide</b>: picnic blankets on the grass and the kraampje of the {@link Kraamguh kraampjesguh}, who sells
 *       picnic treats and playground pieces for vads;</li>
 *   <li><b>de muziektuin</b>: flower beds, a small glass kas and a muziektent where three guhs dance while the jukebox
 *       plays (an empty-handed click puts on a record from the park's own box; it never leaves the jukebox);</li>
 *   <li><b>de japanse tuin</b>: a brook under a red arched bridge into a koi pond with four {@link Knabbelkoi Knabbelkoi's}
 *       (a Guhdex bonus page), toro lanterns and bamboo: a nod to the Klaterdal.</li>
 * </ul>
 * Everyone who lives there is registered with {@link Bezetting} (in the Guhmensie), so every copy gets them and nothing
 * doubles; the eendjes, the dancers and the koi stay at home ({@link PlekAEvents#thuis}). The quest never needs a plekje:
 * they are for fun (hidden advancements {@code parkplek_*} for the FTB book). Texts: tools/features/park_plek_a.py.
 */
public final class PlekA {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    /** The Bezetting ids (numbered ones get 1..n). */
    public static final String EEND_ID = "parka_eend_", KRAAM_ID = "parka_kraamguh", DANSER_ID = "parka_danser_", KOI_ID = "parka_koi_";
    /** The persistent data flag of a park kuikentje (it stays little and safe). */
    public static final String KUIKEN = "guhs_parkplek_kuiken";
    /** How many kuikentjes a park eendje mama has. */
    public static final int KUIKENS = 3;
    /** The dancers' looks (one each). */
    public static final List<GuhVariant> DANSER_VACHT = List.of(GuhVariant.MINT, GuhVariant.CHOCO, GuhVariant.RAINBOW);

    // --- entities -------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<ParkZitje>> ZITJE = ENTITY_TYPES.register("park_zitje",
            () -> EntityType.Builder.<ParkZitje>of(ParkZitje::new, MobCategory.MISC).noLootTable().sized(0.4f, 0.3f).clientTrackingRange(8)
                    .updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("park_zitje"))));
    public static final DeferredHolder<EntityType<?>, EntityType<Knabbelkoi>> KNABBELKOI = ENTITY_TYPES.register("knabbelkoi",
            () -> EntityType.Builder.<Knabbelkoi>of(Knabbelkoi::new, MobCategory.WATER_AMBIENT).sized(0.6f, 0.4f).eyeHeight(0.26f)
                    .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("knabbelkoi"))));

    // --- blocks ---------------------------------------------------------------------------------------------------------
    private static BlockBehaviour.Properties speel(MapColor kleur, SoundType geluid) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(1.0f).sound(geluid).noOcclusion().pushReaction(PushReaction.DESTROY);
    }

    public static final DeferredBlock<SpeelBlokken.Glijbaan> GLIJBAAN = BLOCKS.registerBlock("park_glijbaan", SpeelBlokken.Glijbaan::new,
            () -> speel(MapColor.COLOR_PINK, SoundType.METAL).noCollision());
    public static final DeferredBlock<SpeelBlokken.Schommel> SCHOMMEL = BLOCKS.registerBlock("park_schommel", SpeelBlokken.Schommel::new,
            () -> speel(MapColor.WOOD, SoundType.CHERRY_WOOD));
    public static final DeferredBlock<SpeelBlokken.Wip> WIP = BLOCKS.registerBlock("park_wip", SpeelBlokken.Wip::new,
            () -> speel(MapColor.COLOR_PINK, SoundType.CHERRY_WOOD));
    /** The sandbox sand: part of the park (no item, unbreakable in survival). */
    public static final DeferredBlock<SpeelBlokken.Zandbak> ZANDBAK = BLOCKS.registerBlock("park_zandbak", SpeelBlokken.Zandbak::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(-1f, 3600000f).noLootTable().sound(SoundType.SAND)
                    .pushReaction(PushReaction.BLOCK));
    /** A checked picnic blanket (a carpet). */
    public static final DeferredBlock<CarpetBlock> PICKNICKKLEED = BLOCKS.registerBlock("park_picknickkleed", CarpetBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());

    public static final DeferredItem<BlockItem> GLIJBAAN_ITEM = lore(GLIJBAAN);
    public static final DeferredItem<BlockItem> SCHOMMEL_ITEM = lore(SCHOMMEL);
    public static final DeferredItem<BlockItem> WIP_ITEM = lore(WIP);
    public static final DeferredItem<BlockItem> PICKNICKKLEED_ITEM = lore(PICKNICKKLEED);

    private static DeferredItem<BlockItem> lore(DeferredBlock<?> blok) {
        return ITEMS.registerItem(blok.getId().getPath(), p -> new LoreBlockItem(blok.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
    }

    private PlekA() {
    }

    /** (ParkFeature.register) everything of the five plekjes. */
    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(KNABBELKOI.get(), Knabbelkoi.createAttributes().build()));
        GuhDex.creaturePage(GuhVariant.KNABBELKOI, KNABBELKOI, 5);
        NpcRollen.zet(GuhNpcEntity.Kind.KRAAMGUH, Kraamguh.ROL);
        String park = ParkFeature.STRUCTUUR;
        for (int n = 1; n <= PlekAPlekken.EENDEN.size(); n++) {
            Bezetting.wezen(EEND_ID + n, park, PlekAPlekken.EENDENVIJVER, PlekAPlekken.EENDEN.get(n - 1), (level, plek, draai) -> eendje(level, plek), 14);
            Bezetting.alleenIn(EEND_ID + n, ModDimensions.GUHMENSION);
        }
        Bezetting.npc(KRAAM_ID, park, PlekAPlekken.PICKNICKWEIDE, PlekAPlekken.KRAAM, GuhNpcEntity.Kind.KRAAMGUH, null, PlekAPlekken.KRAAM_YAW);
        Bezetting.alleenIn(KRAAM_ID, ModDimensions.GUHMENSION);
        for (int n = 1; n <= PlekAPlekken.DANSERS.size(); n++) {
            GuhVariant vacht = DANSER_VACHT.get(n - 1);
            Bezetting.wezen(DANSER_ID + n, park, PlekAPlekken.MUZIEKTUIN, PlekAPlekken.DANSERS.get(n - 1), (level, plek, draai) -> danser(level, plek, vacht), 10);
            Bezetting.alleenIn(DANSER_ID + n, ModDimensions.GUHMENSION);
        }
        for (int n = 1; n <= PlekAPlekken.KOIEN.size(); n++) {
            Bezetting.wezen(KOI_ID + n, park, PlekAPlekken.JAPANSE_TUIN, PlekAPlekken.KOIEN.get(n - 1), (level, plek, draai) -> koi(level, plek), 12);
            Bezetting.alleenIn(KOI_ID + n, ModDimensions.GUHMENSION);
        }
        NeoForge.EVENT_BUS.register(PlekAEvents.class);
        NeoForge.EVENT_BUS.addListener(PlekACommando::registreer);
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<BlockItem> item : List.of(GLIJBAAN_ITEM, SCHOMMEL_ITEM, WIP_ITEM, PICKNICKKLEED_ITEM)) {
            output.accept(new ItemStack(item.get()));
        }
    }

    // --- the inhabitants ------------------------------------------------------------------------------------------------

    /** A guh-eendje mama on the pond, with her rijtje of {@link #KUIKENS} kuikentjes behind her (they are added at once). */
    static GuhEendjeEntity eendje(ServerLevel level, Vec3 plek) {
        GuhEendjeEntity mama = WaterdiertjesFeature.GUH_EENDJE.get().create(level, EntitySpawnReason.STRUCTURE);
        if (mama == null) {
            return null;
        }
        mama.snapTo(plek.x, plek.y + 0.1, plek.z, level.getRandom().nextFloat() * 360f, 0f);
        mama.setPersistenceRequired();
        kuikens(level, mama, KUIKENS);
        return mama;
    }

    /** {@code n} new kuikentjes behind this mama: little for good, safe, they stay. */
    static void kuikens(ServerLevel level, GuhEendjeEntity mama, int n) {
        for (int i = 0; i < n; i++) {
            Vec3 achter = Vec3.directionFromRotation(0, mama.getYRot()).scale(-(i + 1) * GuhEendjeEntity.AFSTAND);
            GuhEendjeEntity kuiken = mama.kuiken(level, mama.position().add(achter.x, 0, achter.z));
            kuiken.getPersistentData().putBoolean(KUIKEN, true);
            kuiken.setInvulnerable(true);
            kuiken.setPersistenceRequired();
        }
    }

    /** A dancer of the muziektent: a wild guh in its own colour that stays at the music (Bezetting: never tamed). */
    static GuhEntity danser(ServerLevel level, Vec3 plek, GuhVariant vacht) {
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.STRUCTURE);
        if (guh == null) {
            return null;
        }
        guh.setVariant(vacht);
        guh.setBehavior(GuhEntity.Behavior.PASSIVE);
        guh.snapTo(plek.x, plek.y, plek.z, level.getRandom().nextFloat() * 360f, 0f);
        return guh;
    }

    /** A Knabbelkoi for the koi pond. */
    static Knabbelkoi koi(ServerLevel level, Vec3 plek) {
        Knabbelkoi koi = KNABBELKOI.get().create(level, EntitySpawnReason.STRUCTURE);
        if (koi == null) {
            return null;
        }
        koi.snapTo(plek.x, plek.y + 0.3, plek.z, level.getRandom().nextFloat() * 360f, 0f);
        koi.setPersistenceRequired();
        return koi;
    }
}
