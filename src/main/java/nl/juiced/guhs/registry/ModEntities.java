package nl.juiced.guhs.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.entity.QuestGuhEntity;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    /**
     * Base hitbox at size 1.0; every guh gets a random SCALE attribute on spawn (see GuhEntity.MIN_SCALE/MAX_SCALE).
     * The passenger sits on the body, a bit behind the head.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<GuhEntity>> GUH = ENTITY_TYPES.register("guh",
            () -> EntityType.Builder.of(GuhEntity::new, MobCategory.CREATURE)
                    .sized(0.9f, 0.8f)
                    .eyeHeight(0.55f)
                    .passengerAttachments(new Vec3(0, 0.6, -0.3))
                    .clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("guh"))));

    /** Mika: the evil guh (hostile, harmless). Fixed base size, randomised a little on spawn. */
    public static final DeferredHolder<EntityType<?>, EntityType<MikaEntity>> MIKA = ENTITY_TYPES.register("mika",
            () -> EntityType.Builder.of(MikaEntity::new, MobCategory.MONSTER)
                    .sized(0.9f, 0.8f)
                    .eyeHeight(0.55f)
                    .clientTrackingRange(8)
                    .notInPeaceful()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("mika"))));

    /** The Hungry Guh (quest NPC at guh picnics). Sits up, so it's taller than a normal guh. */
    public static final DeferredHolder<EntityType<?>, EntityType<QuestGuhEntity>> QUEST_GUH = ENTITY_TYPES.register("quest_guh",
            () -> EntityType.Builder.of(QuestGuhEntity::new, MobCategory.MISC)
                    .sized(0.9f, 1.9f)
                    .eyeHeight(1.5f)
                    .clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("quest_guh"))));

    /** The quest characters (Moeder Vadsig, Tandarts-guh, Maagenzym-guh, Slee-guh); their size follows their kind. */
    public static final DeferredHolder<EntityType<?>, EntityType<nl.juiced.guhs.entity.GuhNpcEntity>> GUH_NPC = ENTITY_TYPES.register("guh_npc",
            () -> EntityType.Builder.of(nl.juiced.guhs.entity.GuhNpcEntity::new, MobCategory.MISC)
                    .sized(0.9f, 1.9f).eyeHeight(1.5f).clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("guh_npc"))));

    /** The Mika-baas of the Mika camp (rock-paper-scissors-VADS). */
    public static final DeferredHolder<EntityType<?>, EntityType<nl.juiced.guhs.entity.MikaBaasEntity>> MIKA_BAAS = ENTITY_TYPES.register("mika_baas",
            () -> EntityType.Builder.of(nl.juiced.guhs.entity.MikaBaasEntity::new, MobCategory.MISC)
                    .sized(0.9f, 0.8f).eyeHeight(0.55f).clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("mika_baas"))));

    /** The guh sled (rides on sled rails). */
    public static final DeferredHolder<EntityType<?>, EntityType<nl.juiced.guhs.entity.GuhSleeEntity>> GUH_SLEE = ENTITY_TYPES.register("guh_slee",
            () -> EntityType.Builder.<nl.juiced.guhs.entity.GuhSleeEntity>of(nl.juiced.guhs.entity.GuhSleeEntity::new, MobCategory.MISC)
                    .sized(1.2f, 0.5f).clientTrackingRange(10).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("guh_slee"))));

    /** Guh fish: schools of them in the pink pools of the guh sea. */
    public static final DeferredHolder<EntityType<?>, EntityType<nl.juiced.guhs.entity.GuhVisEntity>> GUH_VIS = ENTITY_TYPES.register("guh_vis",
            () -> EntityType.Builder.of(nl.juiced.guhs.entity.GuhVisEntity::new, MobCategory.WATER_AMBIENT)
                    .sized(0.5f, 0.35f).eyeHeight(0.2f).clientTrackingRange(4).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("guh_vis"))));

    /** Guh bees: peaceful, pink, fill a knabbelkorf. */
    public static final DeferredHolder<EntityType<?>, EntityType<nl.juiced.guhs.entity.GuhBeeEntity>> GUH_BEE = ENTITY_TYPES.register("guh_bee",
            () -> EntityType.Builder.<nl.juiced.guhs.entity.GuhBeeEntity>of(nl.juiced.guhs.entity.GuhBeeEntity::new, MobCategory.CREATURE)
                    .sized(0.7f, 0.6f).eyeHeight(0.3f).clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("guh_bee"))));
    /** Pink guh slimes: peaceful (a creature, not a monster). */
    public static final DeferredHolder<EntityType<?>, EntityType<nl.juiced.guhs.entity.GuhSlimeEntity>> GUH_SLIME = ENTITY_TYPES.register("guh_slime",
            () -> EntityType.Builder.<nl.juiced.guhs.entity.GuhSlimeEntity>of(nl.juiced.guhs.entity.GuhSlimeEntity::new, MobCategory.CREATURE)
                    .sized(0.52f, 0.52f).eyeHeight(0.325f).spawnDimensionsScale(4f).clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("guh_slime"))));
    /** Nether Mikas: scorched, fire-proof Mikas in the Nether. */
    public static final DeferredHolder<EntityType<?>, EntityType<MikaEntity>> NETHER_MIKA = ENTITY_TYPES.register("nether_mika",
            () -> EntityType.Builder.<MikaEntity>of(MikaEntity::new, MobCategory.MONSTER).fireImmune().notInPeaceful()
                    .sized(0.9f, 0.8f).eyeHeight(0.55f).clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("nether_mika"))));

    /** What you sit on when sitting on guh furniture (invisible, never saved). */
    public static final DeferredHolder<EntityType<?>, EntityType<nl.juiced.guhs.entity.GuhSeatEntity>> GUH_SEAT = ENTITY_TYPES.register("guh_seat",
            () -> EntityType.Builder.<nl.juiced.guhs.entity.GuhSeatEntity>of(nl.juiced.guhs.entity.GuhSeatEntity::new, MobCategory.MISC)
                    .sized(0.01f, 0.01f).noSave().noSummon().clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("guh_seat"))));

    /**
     * 26.1: {@code TemptGoal} reads the tempt range from the new {@code minecraft:tempt_range} attribute (1.21.1: always
     * 10 blocks) and crashes when the mob does not have it. Every guhs living entity gets it with the old 10 blocks,
     * unless its own attribute set already has one (vanilla's {@code Animal.createAnimalAttributes()} does).
     */
    public static void addTemptRange(net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent event) {
        for (var type : event.getTypes()) {
            if (Guhs.MODID.equals(EntityType.getKey(type).getNamespace())
                    && !event.has(type, net.minecraft.world.entity.ai.attributes.Attributes.TEMPT_RANGE)) {
                event.add(type, net.minecraft.world.entity.ai.attributes.Attributes.TEMPT_RANGE, 10.0);
            }
        }
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GUH_BEE.get(), net.minecraft.world.entity.animal.bee.Bee.createAttributes().build());
        event.put(GUH_SLIME.get(), net.minecraft.world.entity.monster.Monster.createMonsterAttributes().build());
        event.put(NETHER_MIKA.get(), MikaEntity.createAttributes().build());
        event.put(GUH_VIS.get(), net.minecraft.world.entity.animal.fish.AbstractFish.createAttributes().build());
        event.put(GUH_NPC.get(), nl.juiced.guhs.entity.GuhNpcEntity.createAttributes().build());
        event.put(MIKA_BAAS.get(), nl.juiced.guhs.entity.MikaBaasEntity.createAttributes().build());
        event.put(QUEST_GUH.get(), QuestGuhEntity.createAttributes().build());
        event.put(GUH.get(), GuhEntity.createAttributes().build());
        event.put(MIKA.get(), MikaEntity.createAttributes().build());
    }

    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(GUH.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                GuhEntity::checkGuhSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GUH_VIS.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                nl.juiced.guhs.entity.GuhVisEntity::checkGuhVisSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GUH_BEE.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                nl.juiced.guhs.entity.GuhBeeEntity::checkGuhBeeSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GUH_SLIME.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                nl.juiced.guhs.entity.GuhSlimeEntity::checkGuhSlimeSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(NETHER_MIKA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(MIKA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MikaEntity::checkMikaSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private ModEntities() {
    }
}
