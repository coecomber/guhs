package nl.juiced.guhs.feature.park;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.StructureType;
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
import nl.juiced.guhs.block.GuhFurnitureBlock;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.guhpixel.blok.LoreBlockItem;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.Reisguh;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.ModDimensions;

/**
 * 1.4.2: <b>Het Knabbelpark</b>, a park in the Guhmensie that grows like a village ({@link ParkStructure}, structure
 * {@code guhs:knabbelpark}: one guaranteed copy in new terrain and a rare random spread; Superkompas tab "knus").
 * <ul>
 *   <li>Always there: the fountain plaza with the one Reisguh "Knabbelpark" (she stays where she is: {@link Reisguh#VAST}),
 *       the beeldentuin (three statues with plaques), the hertenkooi with three Hertguhtjes and the kooi-verzorger-guh,
 *       and the parkwachter-guh in his hut: the questline {@link Park#LIJN}.</li>
 *   <li>Random: park walks along the hills with hedges, flowers, lanterns, benches and litter bins, and plekjes on their
 *       sides ({@link ParkPlekjes}: the API for more of them).</li>
 *   <li>Blocks: {@link ParkBlokken} (lantern, bin, plaque, statues, the own-guh statue), the bench.</li>
 *   <li>The Hertguhtje ({@code GuhVariant.HERTGUHTJE}): only in the hertenkooi, and one of your own as the reward (a bonus
 *       Guhdex page, it does not count for "compleet").</li>
 * </ul>
 * Everybody (also copies generated before a restart) gets the NPCs and Hertguhtjes through {@link Bezetting}. Resources:
 * tools/features/park.py (+ park_bouw.py templates, park_modellen.py looks, park_wiki.py).
 */
public final class ParkFeature {
    public static final String STRUCTUUR = "knabbelpark";
    /** Blocks of protected rim around every piece (Bescherming). */
    public static final int RAND = 3;
    /** The synced guh flag of the Hertguhtjes in a hertenkooi (fed, never tamed). */
    public static final int KOOIHERT = 1 << 29;
    /** The Bezetting ids (the Hertguhtjes: HERT_ID + 1..3). */
    public static final String REISGUH_ID = "park_reisguh", WACHTER_ID = "park_wachter", VERZORGER_ID = "park_verzorger", HERT_ID = "park_hert_";
    /** The name of the park's Reisguh (a name, the same in every language). */
    public static final String REISGUH_NAAM = "Knabbelpark";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Guhs.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<ParkStructure>> KNABBELPARK_TYPE =
            STRUCTURE_TYPES.register("knabbelpark", () -> () -> ParkStructure.CODEC);

    public static final DeferredHolder<SoundEvent, SoundEvent> NJEG = SOUNDS.register("park.njeg", () -> SoundEvent.createVariableRangeEvent(Guhs.id("park.njeg")));

    // --- the street furniture (also craftable, for a park of your own) -----------------------------------------------------
    public static final DeferredBlock<GuhFurnitureBlock> BANKJE = BLOCKS.registerBlock("park_bankje",
            p -> new GuhFurnitureBlock(p, 0.45, new double[]{1, 5, 3, 15, 7, 12}, new double[]{1, 7, 11, 15, 15, 13},
                    new double[]{2, 0, 4, 4, 5, 11}, new double[]{12, 0, 4, 14, 5, 11}),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.0f).sound(SoundType.CHERRY_WOOD).noOcclusion());
    public static final DeferredBlock<ParkBlokken.Lantaarn> LANTAARN = BLOCKS.registerBlock("park_lantaarn", ParkBlokken.Lantaarn::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.5f).sound(SoundType.LANTERN).lightLevel(s -> 15).noOcclusion()
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<ParkBlokken.Prullenbak> PRULLENBAK = BLOCKS.registerBlock("park_prullenbak", ParkBlokken.Prullenbak::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.0f).sound(SoundType.METAL).noOcclusion());
    // --- the statues and plaques (part of the park: no item, unbreakable in survival) --------------------------------------
    public static final DeferredBlock<ParkBlokken.Plaquette> PLAQUETTE = BLOCKS.registerBlock("park_plaquette", ParkBlokken.Plaquette::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(-1f, 3600000f).noLootTable().noOcclusion().sound(SoundType.COPPER)
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<ParkBlokken.Beeld> BEELD = BLOCKS.registerBlock("park_beeld", ParkBlokken.Beeld::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(-1f, 3600000f).noLootTable().noOcclusion().sound(SoundType.STONE)
                    .pushReaction(PushReaction.BLOCK));
    /** The reward: a little bronze statue of your own guh. */
    public static final DeferredBlock<ParkBlokken.EigenBeeld> EIGEN_BEELD = BLOCKS.registerBlock("park_eigen_beeld", ParkBlokken.EigenBeeld::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.0f).noOcclusion().sound(SoundType.COPPER));

    public static final DeferredItem<BlockItem> BANKJE_ITEM = lore(BANKJE);
    public static final DeferredItem<BlockItem> LANTAARN_ITEM = lore(LANTAARN);
    public static final DeferredItem<BlockItem> PRULLENBAK_ITEM = lore(PRULLENBAK);
    public static final DeferredItem<BlockItem> EIGEN_BEELD_ITEM = ITEMS.registerItem("park_eigen_beeld",
            p -> new EigenBeeldItem(EIGEN_BEELD.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix().stacksTo(16));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BeeldBlockEntity>> BEELD_BE = BLOCK_ENTITIES.register("park_beeld",
            () -> new BlockEntityType<>(BeeldBlockEntity::new, BEELD.get(), EIGEN_BEELD.get()));

    private static DeferredItem<BlockItem> lore(DeferredBlock<?> blok) {
        return ITEMS.registerItem(blok.getId().getPath(), p -> new LoreBlockItem(blok.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        SOUNDS.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        // who lives in every park (also the parks that were generated before: Bezetting puts back what is missing)
        Bezetting.npc(WACHTER_ID, STRUCTUUR, ParkPlekken.PARKWACHTER, ParkPlekken.WACHTER, GuhNpcEntity.Kind.PARKWACHTERGUH, null, ParkPlekken.WACHTER_YAW);
        Bezetting.npc(VERZORGER_ID, STRUCTUUR, ParkPlekken.HERTENKOOI, ParkPlekken.VERZORGER, GuhNpcEntity.Kind.KOOIVERZORGERGUH, null,
                ParkPlekken.VERZORGER_YAW);
        for (int n = 1; n <= Park.HERTEN; n++) {
            Bezetting.wezen(HERT_ID + n, STRUCTUUR, ParkPlekken.HERTENKOOI, ParkPlekken.HERTEN.get(n - 1), (level, plek, draai) -> Park.kooiHert(level, plek), 14);
        }
        Bezetting.wezen(REISGUH_ID, STRUCTUUR, ParkPlekken.PLEIN, ParkPlekken.REISGUH, (level, plek, draai) -> reisguh(level, plek), 6);
        Bezetting.alleenIn(WACHTER_ID, ModDimensions.GUHMENSION);
        Bezetting.alleenIn(VERZORGER_ID, ModDimensions.GUHMENSION);
        Bezetting.alleenIn(REISGUH_ID, ModDimensions.GUHMENSION);
        for (int n = 1; n <= Park.HERTEN; n++) {
            Bezetting.alleenIn(HERT_ID + n, ModDimensions.GUHMENSION);
        }
        NpcRollen.zet(GuhNpcEntity.Kind.PARKWACHTERGUH, new Park.WachterRol());
        NpcRollen.zet(GuhNpcEntity.Kind.KOOIVERZORGERGUH, new Park.VerzorgerRol());
        GuhHooks.klik(Park::klik);
        GuhHooks.item((guh, stack) -> stack.is(EIGEN_BEELD_ITEM.get()));
        ParkPlekjes.bescherm();
        SuperkompasItem.voegToe("knus", STRUCTUUR);
        NeoForge.EVENT_BUS.addListener(ParkCommando::registreer);
        nl.juiced.guhs.feature.park.plek.PlekB.register(modBus);   // park-plekjes b
    }

    /** The park's Reisguh: named "Knabbelpark", never picked up or renamed. */
    private static GuhNpcEntity reisguh(net.minecraft.server.level.ServerLevel level, net.minecraft.world.phys.Vec3 plek) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.STRUCTURE);
        if (npc == null) {
            return null;
        }
        npc.setKind(GuhNpcEntity.Kind.REISGUH);
        npc.setReisName(Component.literal(REISGUH_NAAM));
        npc.getPersistentData().putBoolean(Reisguh.VAST, true);
        npc.setPersistenceRequired();
        npc.snapTo(plek.x, plek.y, plek.z, 0f, 0f);
        return npc;
    }

    public static void payloads(PayloadRegistrar registrar) {
        nl.juiced.guhs.feature.park.plek.PlekB.payloads(registrar);   // park-plekjes b
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<? extends Item> item : List.of(BANKJE_ITEM, LANTAARN_ITEM, PRULLENBAK_ITEM, EIGEN_BEELD_ITEM)) {
            output.accept(new ItemStack(item.get()));
        }
        nl.juiced.guhs.feature.park.plek.PlekB.creative(output);   // park-plekjes b
    }

    private ParkFeature() {
    }
}
