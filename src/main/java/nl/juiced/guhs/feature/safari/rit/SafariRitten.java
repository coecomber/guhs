package nl.juiced.guhs.feature.safari.rit;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.feature.safari.SafariDier;
import nl.juiced.guhs.feature.safari.ParkData;
import nl.juiced.guhs.feature.safari.momenten.Momenten;
import nl.juiced.guhs.feature.safari.momenten.SchemabordBlock;

/**
 * Op Safari! step 2, slice S3 (vervoer &amp; momenten): the registry and wiring. Rides: {@link TruckEntity},
 * {@link KanoEntity}, {@link GondelEntity}, {@link RaceStruisvogelEntity} (kept running by {@link Ritten} along the paths of
 * {@link RitData}); the moments ({@link Momenten}) and the {@link SchemabordBlock}. Client: rit.client.RitClient and
 * momenten.client.SfeerClient. Resources: tools/features/safari_rit.py; park data: tools/features/safari_park/zone_s3_rit.py.
 */
public final class SafariRitten {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<TruckEntity>> TRUCK = ENTITY_TYPES.register("safari_truck",
            () -> EntityType.Builder.of(TruckEntity::new, MobCategory.MISC).sized(2.6f, 1.6f).clientTrackingRange(12).updateInterval(20)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("safari_truck"))));
    public static final DeferredHolder<EntityType<?>, EntityType<KanoEntity>> KANO = ENTITY_TYPES.register("safari_kano",
            () -> EntityType.Builder.of(KanoEntity::new, MobCategory.MISC).sized(1.2f, 0.6f).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("safari_kano"))));
    public static final DeferredHolder<EntityType<?>, EntityType<GondelEntity>> GONDEL = ENTITY_TYPES.register("safari_gondel",
            () -> EntityType.Builder.of(GondelEntity::new, MobCategory.MISC).sized(1.9f, 1.9f).clientTrackingRange(16).updateInterval(20)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("safari_gondel"))));
    public static final DeferredHolder<EntityType<?>, EntityType<RaceStruisvogelEntity>> STRUISVOGEL = ENTITY_TYPES.register("safari_race_struisvogel",
            () -> EntityType.Builder.of(RaceStruisvogelEntity::new, MobCategory.MISC).sized(0.9f, 1.8f).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("safari_race_struisvogel"))));

    public static final DeferredBlock<SchemabordBlock> SCHEMABORD = BLOCKS.registerBlock("safari_schemabord", SchemabordBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_PLANKS).mapColor(MapColor.WOOD).strength(2f, 6f).noOcclusion()
                    .sound(SoundType.WOOD));

    static {
        ITEMS.registerSimpleBlockItem(SCHEMABORD);
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.register(Ritten.class);
        NeoForge.EVENT_BUS.register(Momenten.class);
        NeoForge.EVENT_BUS.addListener(SafariRitten::commando);
        // during feeding time the shy animals of that zone stop hiding (they come out for the food: a 3-star chance)
        SafariDier.KALM = d -> {
            int zone = ParkData.get().zone(d.getBlockX(), d.getBlockZ());
            return zone >= 0 && Momenten.voedertijd(d.level(), ParkData.get().zoneNaam(zone));
        };
    }

    /** {@code /guhs safari schema} (today's plan, everybody may) and {@code /guhs safari ritten} (ops: what runs). */
    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> safari = Commands.literal("safari");
        safari.then(Commands.literal("schema").executes(ctx -> {
            ctx.getSource().sendSuccess(() -> Momenten.schema(ctx.getSource().getLevel()), false);
            return 1;
        }));
        safari.then(Commands.literal("ritten").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx -> {
            RitData d = RitData.get();
            int trucks = d.route == null ? 0 : RitSchema.trucks(d.route.lengte(), d.haltes.size());
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.guhs.safari.ritten",
                    d.route == null ? 0 : (int) d.route.lengte(), d.haltes.size(), trucks,
                    d.kano == null ? 0 : (int) d.kano.lengte(), d.kabelA == null || d.kabelB == null ? 0 : (int) d.kabelA.distanceTo(d.kabelB),
                    d.race == null ? 0 : (int) d.race.lengte(), Momenten.jarigeSoort(),
                    Momenten.nachtsafari(Safari.level(ctx.getSource().getServer()) == null ? ctx.getSource().getLevel()
                            : Safari.level(ctx.getSource().getServer())) ? "ja" : "nee"), false);
            return 1;
        }));
        event.getDispatcher().register(Commands.literal("guhs").then(safari));
    }

    private SafariRitten() {
    }
}
