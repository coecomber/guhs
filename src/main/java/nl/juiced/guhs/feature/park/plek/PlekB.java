package nl.juiced.guhs.feature.park.plek;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
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
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.guhpixel.blok.LoreBlockItem;
import nl.juiced.guhs.feature.park.ParkFeature;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.vogels.Vogeltje;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.ModDimensions;

/**
 * 1.4.2 Het Knabbelpark, park-plekjes b: five more plekjes of the park (templates: tools/features/park_plek_b.py, the
 * plekjes API of park_bouw.py), each at most once per park ({@link Uitkijktoren#schik}):
 * <ul>
 *   <li><b>duivenvoer_oma</b>: Oma-guh on her bench feeds the Duifguhtjes ({@link DuifguhtjeEntity}); she gives you a handful
 *       of broodkruimels a day and you feed along ({@link DuivenOma});</li>
 *   <li><b>uitkijktoren</b>: always on the highest plekje spot of its park, with two {@link Verrekijker}s on its deck;</li>
 *   <li><b>poppenkast</b>: right-click the stage for the show ({@link Poppenkast}), three guhkindjes shout along;</li>
 *   <li><b>wensboom</b>: hang a renamed paper on its trunk and your wish hangs in the tree for everybody ({@link Wensboom});</li>
 *   <li><b>mika_bankje</b>: the sulking Mika on the bench "Verboden voor Mika's"; a kaasknabbel makes him leave happily, the
 *       next day he is back ({@link MikaBankje}).</li>
 * </ul>
 * Every inhabitant is in its template AND registered with {@link Bezetting} (so a park that misses one gets it back; only in
 * the Guhmensie). The spots below are template coordinates of the plekje's piece (python reads them back: park_plek_b.plekken).
 * Registered from {@link ParkFeature} (one line each: register, payloads, creative); client: client/PlekBClient.
 */
public final class PlekB {
    // --- spots in the templates (template coordinates; walk level y = 2) ----------------------------------------------------
    /** duivenvoer_oma: Oma-guh's seat, the four Duifguhtjes. */
    public static final BlockPos OMA = new BlockPos(6, 2, 2);
    public static final float OMA_YAW = 0f;
    public static final BlockPos DUIF_1 = new BlockPos(4, 2, 5);
    public static final BlockPos DUIF_2 = new BlockPos(8, 2, 5);
    public static final BlockPos DUIF_3 = new BlockPos(5, 2, 6);
    public static final BlockPos DUIF_4 = new BlockPos(7, 2, 4);
    /** poppenkast: the stage block, the three guhkindjes' seats. */
    public static final BlockPos PODIUM = new BlockPos(6, 4, 3);
    public static final BlockPos KIND_1 = new BlockPos(4, 2, 6);
    public static final BlockPos KIND_2 = new BlockPos(8, 2, 6);
    public static final BlockPos KIND_3 = new BlockPos(5, 2, 8);
    public static final float KIND_YAW = 180f;
    /** mika_bankje: the sulking Mika's seat. */
    public static final BlockPos MIKA = new BlockPos(5, 2, 3);
    public static final float MIKA_YAW = 0f;
    /** uitkijktoren: the two verrekijkers on the deck. */
    public static final BlockPos KIJKER_OOST = new BlockPos(7, 12, 4);
    public static final BlockPos KIJKER_WEST = new BlockPos(1, 12, 4);
    /** The seat of a park bench: an NPC on a bench sits this high above the bench's block. */
    public static final double ZIT = 0.4375;

    /** The Bezetting ids. */
    public static final String OMA_ID = "parkb_oma", DUIF_ID = "parkb_duif_", KIND_ID = "parkb_kind_", MIKA_ID = "parkb_mika";
    public static final int DUIVEN = 4, KINDJES = 3;

    // --- registers -------------------------------------------------------------------------------------------------------
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ROEKOE = SOUNDS.register("parkb.roekoe",
            () -> SoundEvent.createVariableRangeEvent(Guhs.id("parkb.roekoe")));
    public static final DeferredHolder<SoundEvent, SoundEvent> APPLAUS = SOUNDS.register("parkb.applaus",
            () -> SoundEvent.createVariableRangeEvent(Guhs.id("parkb.applaus")));

    public static final DeferredHolder<EntityType<?>, EntityType<DuifguhtjeEntity>> DUIFGUHTJE = ENTITY_TYPES.register("duifguhtje",
            () -> EntityType.Builder.of(DuifguhtjeEntity::new, MobCategory.CREATURE).sized(0.45f, 0.55f).eyeHeight(0.45f)
                    .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("duifguhtje"))));
    public static final DeferredItem<SpawnEggItem> DUIFGUHTJE_SPAWN_EGG = ModItems.spawnEgg(ITEMS, "duifguhtje_spawn_egg", DUIFGUHTJE);

    /** Broodkruimels: Oma-guh gives a handful a day; right-click to strew some (the Duifguhtjes come). */
    public static final DeferredItem<Item> KRUIMELS = ITEMS.registerItem("park_kruimels", DuivenOma.Kruimels::new, () -> new Item.Properties());

    public static final DeferredBlock<Verrekijker> VERREKIJKER = BLOCKS.registerBlock("park_verrekijker", Verrekijker::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.0f).sound(SoundType.COPPER).noOcclusion()
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<Poppenkast> POPPENKAST = BLOCKS.registerBlock("park_poppenkast", Poppenkast::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.0f).sound(SoundType.WOOL).noOcclusion()
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<Wensboom.Stam> WENSSTAM = BLOCKS.registerBlock("park_wensstam", Wensboom.Stam::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(2.0f).sound(SoundType.CHERRY_WOOD));

    public static final DeferredItem<BlockItem> VERREKIJKER_ITEM = ITEMS.registerItem("park_verrekijker",
            p -> new LoreBlockItem(VERREKIJKER.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
    public static final DeferredItem<BlockItem> POPPENKAST_ITEM = ITEMS.registerItem("park_poppenkast",
            p -> new BlockItem(POPPENKAST.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
    public static final DeferredItem<BlockItem> WENSSTAM_ITEM = ITEMS.registerItem("park_wensstam",
            p -> new LoreBlockItem(WENSSTAM.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());

    private PlekB() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(DUIFGUHTJE.get(), Vogeltje.createAttributes(4.0).build()));
        GuhDex.creaturePage(GuhVariant.DUIFGUHTJE, DUIFGUHTJE, 8);
        String park = ParkFeature.STRUCTUUR;
        // who lives at the plekjes (in the templates too; Bezetting puts back what is missing, never doubles)
        Bezetting.wezen(OMA_ID, park, "duivenvoer_oma", OMA, zitter(GuhNpcEntity.Kind.OMAGUH, OMA_YAW), 4);
        BlockPos[] duiven = {DUIF_1, DUIF_2, DUIF_3, DUIF_4};
        for (int n = 1; n <= DUIVEN; n++) {
            Bezetting.wezen(DUIF_ID + n, park, "duivenvoer_oma", duiven[n - 1], (level, plek, draai) -> duif(level, plek), 20);
        }
        BlockPos[] kindjes = {KIND_1, KIND_2, KIND_3};
        for (int n = 1; n <= KINDJES; n++) {
            Bezetting.wezen(KIND_ID + n, park, "poppenkast", kindjes[n - 1], zitter(GuhNpcEntity.Kind.GUHKINDJE, KIND_YAW), 4);
        }
        Bezetting.wezen(MIKA_ID, park, "mika_bankje", MIKA, MikaBankje::maak, 4);
        for (String id : java.util.List.of(OMA_ID, MIKA_ID)) {
            Bezetting.alleenIn(id, ModDimensions.GUHMENSION);
        }
        for (int n = 1; n <= DUIVEN; n++) {
            Bezetting.alleenIn(DUIF_ID + n, ModDimensions.GUHMENSION);
        }
        for (int n = 1; n <= KINDJES; n++) {
            Bezetting.alleenIn(KIND_ID + n, ModDimensions.GUHMENSION);
        }
        NpcRollen.zet(GuhNpcEntity.Kind.OMAGUH, new DuivenOma());
        NpcRollen.zet(GuhNpcEntity.Kind.GUHKINDJE, new Poppenkast.KindRol());
        NpcRollen.zet(GuhNpcEntity.Kind.MOKMIKA, new MikaBankje());
        Poppenkast.luister();
        NeoForge.EVENT_BUS.addListener(PlekBCommando::registreer);
    }

    public static void payloads(PayloadRegistrar registrar) {
        registrar.playToClient(Verrekijker.Kijk.TYPE, Verrekijker.Kijk.STREAM_CODEC, Verrekijker.Kijk::handle);
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<? extends Item> item : java.util.List.of(KRUIMELS, VERREKIJKER_ITEM, POPPENKAST_ITEM, WENSSTAM_ITEM, DUIFGUHTJE_SPAWN_EGG)) {
            output.accept(new ItemStack(item.get()));
        }
    }

    /** An NPC of this kind on the seat of the bench at its spot, looking the way it looked in the template. */
    static Bezetting.Maker zitter(GuhNpcEntity.Kind kind, float yaw) {
        return (level, plek, draai) -> zitNpc(level, kind, plek, yaw, draai);
    }

    static GuhNpcEntity zitNpc(ServerLevel level, GuhNpcEntity.Kind kind, Vec3 plek, float yaw, net.minecraft.world.level.block.Rotation draai) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.STRUCTURE);
        if (npc == null) {
            return null;
        }
        npc.setKind(kind);
        npc.setYRot(yaw);
        float y = npc.rotate(draai);
        npc.snapTo(plek.x, plek.y + ZIT, plek.z, y, 0f);
        npc.setYBodyRot(y);
        npc.setYHeadRot(y);
        npc.setPersistenceRequired();
        return npc;
    }

    /** A Duifguhtje of Oma-guh's square (its home: where it is made). */
    static DuifguhtjeEntity duif(ServerLevel level, Vec3 plek) {
        DuifguhtjeEntity d = DUIFGUHTJE.get().create(level, EntitySpawnReason.STRUCTURE);
        if (d == null) {
            return null;
        }
        d.snapTo(plek.x, plek.y, plek.z, level.getRandom().nextFloat() * 360f, 0f);
        d.zetThuis(BlockPos.containing(plek));
        d.setPersistenceRequired();
        return d;
    }
}
