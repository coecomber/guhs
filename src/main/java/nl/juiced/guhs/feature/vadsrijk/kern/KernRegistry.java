package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.List;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.BaasKist;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.klodderreus.Klodderreus;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.RijkItem;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.ProefKlodder;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.kern.wereld.KaartFeature;
import nl.juiced.guhs.feature.vadsrijk.kern.wereld.Ondergrond;
import nl.juiced.guhs.feature.vadsrijk.klassen.ridder.Knuffelridder;

/** Kern's registry class (CONTRACT_VADSRIJK 1): the attachment, component, items, entities, block, feature; payloads; events. */
public final class KernRegistry {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Guhs.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Guhs.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SpelerData>> SPELER = ATTACHMENTS.register("vr_speler",
            () -> AttachmentType.serializable(SpelerData::new).copyOnDeath().build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Uitrusting>> UITRUSTING = COMPONENTS.registerComponentType("vr_uitrusting",
            b -> b.persistent(Uitrusting.CODEC).networkSynchronized(Uitrusting.STREAM_CODEC));

    // ---- the 5 reference base types (the economie slice adds the rest of the ~30) ------------------------------------------
    public static final DeferredItem<Item> KUSSENZWAARD = wapen("vr_kussenzwaard");
    public static final DeferredItem<Item> KNUFFELKNOTS = wapen("vr_knuffelknots");
    public static final DeferredItem<Item> PLUISHAMER = wapen("vr_pluishamer");
    public static final DeferredItem<Item> PLUISHELM = harnas("vr_pluishelm", Slot.HOOFD);
    public static final DeferredItem<Item> KNUFFELHARNAS = harnas("vr_knuffelharnas", Slot.BORST);

    public static final DeferredBlock<BaasKist> BAASKIST = BLOCKS.registerBlock("vr_baaskist", BaasKist::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST).strength(-1, 3600000).noLootTable());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BaasKist.Inhoud>> BAASKIST_BE = BLOCK_ENTITIES.register("vr_baaskist",
            () -> new BlockEntityType<>(BaasKist.Inhoud::new, BAASKIST.get()));

    public static final DeferredHolder<EntityType<?>, EntityType<ProefKlodder>> PROEFKLODDER = ENTITIES.register("vr_proefklodder",
            () -> EntityType.Builder.<ProefKlodder>of(ProefKlodder::new, MobCategory.MONSTER).sized(1.0f, 1.0f).eyeHeight(0.6f).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_proefklodder"))));
    public static final DeferredHolder<EntityType<?>, EntityType<ProefKlodder>> PROEFKLODDERTJE = ENTITIES.register("vr_proefkloddertje",
            () -> EntityType.Builder.<ProefKlodder>of(ProefKlodder::new, MobCategory.MONSTER).sized(0.52f, 0.52f).eyeHeight(0.3f).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_proefkloddertje"))));
    public static final DeferredHolder<EntityType<?>, EntityType<Klodderreus>> KLODDERREUS = ENTITIES.register("vr_klodderreus",
            () -> EntityType.Builder.<Klodderreus>of(Klodderreus::new, MobCategory.MONSTER).sized(3.06f, 3.06f).eyeHeight(1.8f).clientTrackingRange(16)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_klodderreus"))));

    public static final DeferredHolder<Feature<?>, KaartFeature> KAART = FEATURES.register("vr_kaart", KaartFeature::new);

    private KernRegistry() {
    }

    private static DeferredItem<Item> wapen(String id) {
        return ITEMS.registerItem(id, RijkItem::new, () -> new Item.Properties().stacksTo(1)
                .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY));
    }

    private static DeferredItem<Item> harnas(String id, Slot slot) {
        return ITEMS.registerItem(id, RijkItem::new, () -> new Item.Properties().stacksTo(1)
                .component(DataComponents.EQUIPPABLE, Equippable.builder(slot.vanilla).build()));
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ENTITIES.register(modBus);
        FEATURES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(PROEFKLODDER.get(), VadsrijkMob.basisAttributen().build());
            e.put(PROEFKLODDERTJE.get(), VadsrijkMob.basisAttributen().build());
            e.put(KLODDERREUS.get(), VadsrijkMob.basisAttributen().add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, 1.0)
                    .add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 40).build());
        });
        inhoud();
        KernEvents.register();
    }

    public static void payloads(PayloadRegistrar r) {
        KernPayloads.register(r);
    }

    /** Kern's content: the class, the 5 base types + loot tables, the proeftuin spawner and the Klodderreus arena. */
    private static void inhoud() {
        Knuffelridder.init();
        BasisTypes.registreer(BasisType.wapen("kussenzwaard", "guhs:vr_kussenzwaard", "zwaard", Klassen.RIDDER, nl.juiced.guhs.feature.vadsrijk.kern.Element.ROZE,
                8, 1.6, Stats.van(Stat.KRACHT, 2), Stats.van(Stat.KRACHT, 0.4), 3));
        BasisTypes.registreer(BasisType.wapen("knuffelknots", "guhs:vr_knuffelknots", "knots", Klassen.RIDDER, nl.juiced.guhs.feature.vadsrijk.kern.Element.NATUUR,
                10, 1.9, Stats.van(Stat.VERDEDIGING, 2), Stats.van(Stat.VERDEDIGING, 0.4), 3));
        BasisTypes.registreer(BasisType.wapen("pluishamer", "guhs:vr_pluishamer", "hamer", Klassen.RIDDER, nl.juiced.guhs.feature.vadsrijk.kern.Element.VUUR,
                13, 2.3, Stats.van(Stat.KRITIEKSCHADE, 10), Stats.van(Stat.KRACHT, 0.3), 3));
        BasisTypes.registreer(BasisType.harnas("pluishelm", "guhs:vr_pluishelm", Slot.HOOFD, 4, 12, 3));
        BasisTypes.registreer(BasisType.harnas("knuffelharnas", "guhs:vr_knuffelharnas", Slot.BORST, 7, 20, 3));
        List<LootTabel.Regel> alle = List.of(new LootTabel.Regel("kussenzwaard", 3), new LootTabel.Regel("knuffelknots", 3), new LootTabel.Regel("pluishamer", 2),
                new LootTabel.Regel("pluishelm", 3), new LootTabel.Regel("knuffelharnas", 3));
        LootTabellen.registreer("kern:proefklodder", new LootTabel(alle, 1, 0.22, 1.0, 3, 12));
        LootTabellen.registreer("kern:klodderreus", new LootTabel(alle, 3, 1.0, 3.0, 150, 400));
        Spawners.registreer(new Spawners.Spawner("kern:proeftuin_oost", new net.minecraft.core.BlockPos(3150, Vadsrijk.GROND + 1, 3120), 22, PROEFKLODDER, 8, 200, 1,
                4, nl.juiced.guhs.feature.vadsrijk.kern.Element.WATER, 0.1));
        Spawners.registreer(new Spawners.Spawner("kern:proeftuin_west", new net.minecraft.core.BlockPos(3040, Vadsrijk.GROND + 1, 3160), 16, PROEFKLODDER, 5, 200, 1,
                2, nl.juiced.guhs.feature.vadsrijk.kern.Element.NATUUR, 0.0));
        Arenas.registreer(new Arena("kern:proefarena", Ondergrond.ARENA, Ondergrond.ARENA_STRAAL, Ondergrond.ARENA_DEUR, 60, Ondergrond.ARENA_BORD,
                "vadsrijk.kern.baas.klodderreus"), sl -> {
                    Klodderreus k = KLODDERREUS.get().create(sl, net.minecraft.world.entity.EntitySpawnReason.SPAWNER);
                    if (k != null) {
                        k.stelIn(3, nl.juiced.guhs.feature.vadsrijk.kern.Element.WATER, false, java.util.Set.of());
                    }
                    return k;
                });
    }
}
