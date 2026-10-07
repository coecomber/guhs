package nl.juiced.guhs.feature.einde2.zwevers;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
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
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.boerderij.BoerderijItems;
import nl.juiced.guhs.feature.landdiertjes.LanddierItems;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModItems;

/**
 * einde2, slice zwevers: what floats, sings and scurries on the outer islands of the Guheinde.
 * <ul>
 *   <li>{@link KruimelkwalEntity Kruimelkwallen}: glowing swarms in the Kruimelwolken ({@link Kruimelkwallen}: the top-up,
 *       and the door for other slices to name more places). Fall onto one and it bounces you up; catch one with a glass
 *       bottle and you have a {@link KruimelkwalPotBlock kruimelkwal_pot}, a lamp.</li>
 *   <li>The {@link LeegtewalvisguhEntity Leegtewalvisguh}: a huge, slow, singing whale guh between the islands, with a
 *       back you can land and walk on (knabbelkoor grows there). Only there for players who have the flag of
 *       {@link Walviszicht}; a knabbelkoorvrucht gets you a {@code leegteparel}, once a Minecraft day.</li>
 *   <li>The four element animals ({@link Elementdier}; wild ones by {@link Elementdieren}): the
 *       {@link VleermuisLemurguhEntity vleermuis-lemurguh} (lucht), the {@link OtterPinguinguhEntity otter-pinguïnguh}
 *       (water; a sled when told so), the {@link DasMolguhEntity das-molguh} (aarde) and the
 *       {@link SchildpadEendguhEntity schildpad-eendguh} (vuur).</li>
 * </ul>
 * Resources: tools/features/einde2_zwevers.py (models: einde2_zwevers_modellen.py).
 */
public final class E2ZweversFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    /** Random salt of this slice (build contract 3.1). */
    public static final long SALT = 21400450L;

    // --- the creatures (the ids buildings may place, build contract 3.4) --------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<KruimelkwalEntity>> KRUIMELKWAL = ENTITY_TYPES.register("kruimelkwal",
            () -> EntityType.Builder.of(KruimelkwalEntity::new, MobCategory.AMBIENT).sized(0.8f, 0.85f).eyeHeight(0.55f)
                    .clientTrackingRange(8).build(key("kruimelkwal")));
    /**
     * Sized as its model (it lives at scale 8: LeegtewalvisguhEntity.SCHAAL, so its own box is 4 x 4.8 blocks, inside its
     * body). What you land on are its parts (see the class).
     */
    public static final DeferredHolder<EntityType<?>, EntityType<LeegtewalvisguhEntity>> LEEGTEWALVISGUH = ENTITY_TYPES.register("leegtewalvisguh",
            () -> EntityType.Builder.of(LeegtewalvisguhEntity::new, MobCategory.MISC).sized(0.5f, 0.6f).eyeHeight(0.4f)
                    .clientTrackingRange(16).updateInterval(2).fireImmune().build(key("leegtewalvisguh")));
    /**
     * The kind of thing a box of the whale's back is (LeegtewalvisguhEntity.Rug): never summoned, saved or drawn. It has a
     * type of its own so that whatever looks for "a Leegtewalvisguh here" (the Guhdex) never finds the back instead.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<LeegtewalvisguhEntity.Rug>> LEEGTEWALVISGUH_RUG = ENTITY_TYPES.register("leegtewalvisguh_rug",
            () -> EntityType.Builder.<LeegtewalvisguhEntity.Rug>createNothing(MobCategory.MISC).noSave().noSummon()
                    .sized(LeegtewalvisguhEntity.DEEL_BREED, LeegtewalvisguhEntity.DEEL_HOOG).clientTrackingRange(16).build(key("leegtewalvisguh_rug")));
    public static final DeferredHolder<EntityType<?>, EntityType<VleermuisLemurguhEntity>> VLEERMUIS_LEMURGUH = ENTITY_TYPES.register("vleermuis_lemurguh",
            () -> EntityType.Builder.of(VleermuisLemurguhEntity::new, MobCategory.CREATURE).sized(0.5f, 0.6f).eyeHeight(0.5f)
                    .clientTrackingRange(8).build(key("vleermuis_lemurguh")));
    public static final DeferredHolder<EntityType<?>, EntityType<OtterPinguinguhEntity>> OTTER_PINGUINGUH = ENTITY_TYPES.register("otter_pinguinguh",
            () -> EntityType.Builder.of(OtterPinguinguhEntity::new, MobCategory.CREATURE).sized(0.6f, 0.95f).eyeHeight(0.8f)
                    .clientTrackingRange(8).build(key("otter_pinguinguh")));
    public static final DeferredHolder<EntityType<?>, EntityType<DasMolguhEntity>> DAS_MOLGUH = ENTITY_TYPES.register("das_molguh",
            () -> EntityType.Builder.of(DasMolguhEntity::new, MobCategory.CREATURE).sized(0.95f, 0.72f).eyeHeight(0.4f)
                    .clientTrackingRange(8).build(key("das_molguh")));
    public static final DeferredHolder<EntityType<?>, EntityType<SchildpadEendguhEntity>> SCHILDPAD_EENDGUH = ENTITY_TYPES.register("schildpad_eendguh",
            () -> EntityType.Builder.of(SchildpadEendguhEntity::new, MobCategory.CREATURE).sized(0.6f, 0.58f).eyeHeight(0.48f)
                    .clientTrackingRange(8).build(key("schildpad_eendguh")));

    private static ResourceKey<EntityType<?>> key(String id) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id));
    }

    // --- the jar and the pearl ------------------------------------------------------------------------------------------------
    /** A Kruimelkwal in a glass jar: a lamp (light 12), on the floor or hanging. Breaking it lets the kwal go. */
    public static final DeferredBlock<KruimelkwalPotBlock> KRUIMELKWAL_POT = BLOCKS.registerBlock("kruimelkwal_pot", KruimelkwalPotBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.3f).sound(SoundType.GLASS)
                    .lightLevel(state -> KruimelkwalPotBlock.LICHT).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> KRUIMELKWAL_POT_ITEM = ITEMS.registerItem("kruimelkwal_pot",
            p -> new BoerderijItems.LoreBlock(KRUIMELKWAL_POT.get(), p), p -> p.stacksTo(16).useBlockDescriptionPrefix());
    /** What a fed Leegtewalvisguh gives: rare and modest (a keepsake; a later phase may ask for one). */
    public static final DeferredItem<LanddierItems.LoreItem> LEEGTEPAREL = ITEMS.registerItem("leegteparel",
            LanddierItems.LoreItem::new, () -> new Item.Properties().rarity(Rarity.RARE));

    public static final DeferredItem<SpawnEggItem> KRUIMELKWAL_SPAWN_EGG = ModItems.spawnEgg(ITEMS, "kruimelkwal_spawn_egg", KRUIMELKWAL);
    public static final DeferredItem<SpawnEggItem> LEEGTEWALVISGUH_SPAWN_EGG = ModItems.spawnEgg(ITEMS, "leegtewalvisguh_spawn_egg", LEEGTEWALVISGUH);
    public static final DeferredItem<SpawnEggItem> VLEERMUIS_LEMURGUH_SPAWN_EGG = ModItems.spawnEgg(ITEMS, "vleermuis_lemurguh_spawn_egg", VLEERMUIS_LEMURGUH);
    public static final DeferredItem<SpawnEggItem> OTTER_PINGUINGUH_SPAWN_EGG = ModItems.spawnEgg(ITEMS, "otter_pinguinguh_spawn_egg", OTTER_PINGUINGUH);
    public static final DeferredItem<SpawnEggItem> DAS_MOLGUH_SPAWN_EGG = ModItems.spawnEgg(ITEMS, "das_molguh_spawn_egg", DAS_MOLGUH);
    public static final DeferredItem<SpawnEggItem> SCHILDPAD_EENDGUH_SPAWN_EGG = ModItems.spawnEgg(ITEMS, "schildpad_eendguh_spawn_egg", SCHILDPAD_EENDGUH);

    // --- sounds (vanilla sounds, pitched, in sounds.json) -----------------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> KWAL = sound("einde2_zwevers.kwal");
    public static final DeferredHolder<SoundEvent, SoundEvent> KWAL_STUITER = sound("einde2_zwevers.kwal_stuiter");
    public static final DeferredHolder<SoundEvent, SoundEvent> KWAL_POT = sound("einde2_zwevers.kwal_pot");
    /** One long note of the whale's song (the entity pitches it: LeegtewalvisguhEntity.ZANG). */
    public static final DeferredHolder<SoundEvent, SoundEvent> WALVIS_ZANG = sound("einde2_zwevers.walvis_zang");
    public static final DeferredHolder<SoundEvent, SoundEvent> WALVIS_HAP = sound("einde2_zwevers.walvis_hap");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEMUR = sound("einde2_zwevers.lemur");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEMUR_VLEUGEL = sound("einde2_zwevers.lemur_vleugel");
    public static final DeferredHolder<SoundEvent, SoundEvent> OTTER = sound("einde2_zwevers.otter");
    public static final DeferredHolder<SoundEvent, SoundEvent> OTTER_GLIJ = sound("einde2_zwevers.otter_glij");
    public static final DeferredHolder<SoundEvent, SoundEvent> DAS = sound("einde2_zwevers.das");
    public static final DeferredHolder<SoundEvent, SoundEvent> DAS_GRAAF = sound("einde2_zwevers.das_graaf");
    public static final DeferredHolder<SoundEvent, SoundEvent> EENDGUH = sound("einde2_zwevers.eendguh");
    public static final DeferredHolder<SoundEvent, SoundEvent> EENDGUH_SCHELP = sound("einde2_zwevers.eendguh_schelp");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(KRUIMELKWAL.get(), KruimelkwalEntity.createAttributes().build());
            event.put(LEEGTEWALVISGUH.get(), LeegtewalvisguhEntity.createAttributes().build());
            event.put(VLEERMUIS_LEMURGUH.get(), VleermuisLemurguhEntity.createAttributes().build());
            event.put(OTTER_PINGUINGUH.get(), OtterPinguinguhEntity.createAttributes().build());
            event.put(DAS_MOLGUH.get(), DasMolguhEntity.createAttributes().build());
            event.put(SCHILDPAD_EENDGUH.get(), SchildpadEendguhEntity.createAttributes().build());
        });
        GuhDex.creaturePage(GuhVariant.KRUIMELKWAL, KRUIMELKWAL);
        // the whale's page is only filled in by the whale itself, for players who see it (LeegtewalvisguhEntity#kijkers)
        GuhDex.creaturePage(GuhVariant.LEEGTEWALVISGUH, LEEGTEWALVISGUH);
        GuhDex.creaturePage(GuhVariant.VLEERMUIS_LEMURGUH, VLEERMUIS_LEMURGUH);
        GuhDex.creaturePage(GuhVariant.OTTER_PINGUINGUH, OTTER_PINGUINGUH);
        GuhDex.creaturePage(GuhVariant.DAS_MOLGUH, DAS_MOLGUH);
        GuhDex.creaturePage(GuhVariant.SCHILDPAD_EENDGUH, SCHILDPAD_EENDGUH);
        NeoForge.EVENT_BUS.register(E2ZweversEvents.class);
    }

    public static void payloads(PayloadRegistrar registrar) {
        E2ZweversPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        for (var item : List.of(KRUIMELKWAL_SPAWN_EGG, LEEGTEWALVISGUH_SPAWN_EGG, VLEERMUIS_LEMURGUH_SPAWN_EGG, OTTER_PINGUINGUH_SPAWN_EGG,
                DAS_MOLGUH_SPAWN_EGG, SCHILDPAD_EENDGUH_SPAWN_EGG)) {
            output.accept(new ItemStack(item.get()));
        }
        output.accept(new ItemStack(KRUIMELKWAL_POT_ITEM.get()));
        output.accept(new ItemStack(LEEGTEPAREL.get()));
    }

    private E2ZweversFeature() {
    }
}
