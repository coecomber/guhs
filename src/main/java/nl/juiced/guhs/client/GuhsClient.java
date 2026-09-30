package nl.juiced.guhs.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import nl.juiced.guhs.block.GuhWireBlock;
import nl.juiced.guhs.registry.ModBlocks;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import nl.juiced.guhs.client.screen.BankGuhScreen;
import nl.juiced.guhs.registry.ModMenuTypes;
import nl.juiced.guhs.registry.ModBlockEntities;
import nl.juiced.guhs.registry.ModEntities;

/** Client-only setup. Only called on the physical client (see Guhs constructor). */
public final class GuhsClient {
    public static void init(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        // 1.0.1: the client config (addOfficialServer) with a config screen in the mod list, and the official server in the server list
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, GuhsClientConfig.SPEC);
        container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                net.neoforged.neoforge.client.gui.ConfigurationScreen::new);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(OfficialServerEntry::onScreenInit);
        nl.juiced.guhs.feature.FeaturesClient.init(modBus);
        modBus.addListener(GuhsClient::registerRenderers);
        modBus.addListener(GuhsClient::registerLayerDefinitions);
        modBus.addListener(GuhsClient::addVillagerLayers);
        modBus.addListener(GuhsClient::registerExtraModels);
        modBus.addListener(GuhsClient::registerScreens);
        modBus.addListener(GuhsClient::registerBlockColors);
        modBus.addListener(KaasSausClient::registerFluidLooks);
        modBus.addListener(GuhsClient::clientSetup);
        modBus.addListener(GuhKeys::register);
        modBus.addListener(GuhmensionSky::register);
        modBus.addListener(nl.juiced.guhs.client.particle.GuhBlaadjeParticle::register);
        // DEV ONLY: the automatic screenshot round (gradlew runAutocheckClient); the class isn't in the guhs jar
        if (System.getProperty("guhs.autocheck") != null) {
            nl.juiced.guhs.dev.AutoCheck.init(modBus);
        }
    }

    private static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GuhVillagerFeaturesLayer.LAYER, GuhVillagerFeaturesLayer::createLayer);
    }

    /** Guh villagers are vanilla villagers: add their ears and tail to the vanilla villager renderer. */
    @SuppressWarnings("unchecked")
    private static void addVillagerLayers(EntityRenderersEvent.AddLayers event) {
        var renderer = event.getRenderer(net.minecraft.world.entity.EntityType.VILLAGER);
        if (renderer instanceof net.minecraft.client.renderer.entity.VillagerRenderer villagers) {
            villagers.addLayer(new GuhVillagerFeaturesLayer(villagers, event.getEntityModels().bakeLayer(GuhVillagerFeaturesLayer.LAYER)));
        }
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GUH.get(), GuhRenderer::new);
        event.registerEntityRenderer(ModEntities.MIKA.get(), MikaRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GUH_SPAWNER.get(), GuhSpawnerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GUH_WHEEL.get(), GuhWheelRenderer::new);
        event.registerEntityRenderer(ModEntities.QUEST_GUH.get(), SittingGuhRenderers.QuestGuhRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BANK_GUH.get(), SittingGuhRenderers.BankGuhRenderer::new);
        event.registerEntityRenderer(ModEntities.GUH_NPC.get(), SittingGuhRenderers.NpcRenderer::new);
        event.registerEntityRenderer(ModEntities.GUH_SLEE.get(), GuhSleeRenderer::new);
        event.registerEntityRenderer(ModEntities.GUH_SEAT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.GUH_BEE.get(), context -> new software.bernie.geckolib.renderer.GeoEntityRenderer<>(context,
                new software.bernie.geckolib.model.DefaultedEntityGeoModel<nl.juiced.guhs.entity.GuhBeeEntity>(nl.juiced.guhs.Guhs.id("guh_bee"))));
        event.registerEntityRenderer(ModEntities.GUH_SLIME.get(), context -> new net.minecraft.client.renderer.entity.SlimeRenderer(context) {
            @Override
            public net.minecraft.resources.ResourceLocation getTextureLocation(net.minecraft.world.entity.monster.Slime slime) {
                return nl.juiced.guhs.Guhs.id("textures/entity/guh_slime.png");
            }
        });
        event.registerEntityRenderer(ModEntities.NETHER_MIKA.get(), context -> new software.bernie.geckolib.renderer.GeoEntityRenderer<>(context,
                new software.bernie.geckolib.model.DefaultedEntityGeoModel<nl.juiced.guhs.entity.MikaEntity>(nl.juiced.guhs.Guhs.id("mika"), true) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureResource(nl.juiced.guhs.entity.MikaEntity mika) {
                        return nl.juiced.guhs.Guhs.id("textures/entity/nether_mika.png");
                    }
                }.withAltAnimations(nl.juiced.guhs.Guhs.id("guh"))));
        event.registerEntityRenderer(ModEntities.GUH_VIS.get(), context -> new software.bernie.geckolib.renderer.GeoEntityRenderer<>(context,
                new software.bernie.geckolib.model.DefaultedEntityGeoModel<nl.juiced.guhs.entity.GuhVisEntity>(nl.juiced.guhs.Guhs.id("guh_vis"))));
        event.registerBlockEntityRenderer(ModBlockEntities.SLEE_RAIL.get(), SleeRailRenderer::new);
        event.registerEntityRenderer(ModEntities.MIKA_BAAS.get(), SittingGuhRenderers.MikaBaasRenderer::new);
    }

    /** The guh compasses use the vanilla compass needle ("angle") pointing at their lodestone target. */
    private static void clientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            nl.juiced.guhs.entity.GuhEntity.riderJumping = () -> net.minecraft.client.Minecraft.getInstance().options.keyJump.isDown();
            for (var compass : java.util.List.of(nl.juiced.guhs.registry.ModItems.GUH_CAVE_COMPASS.get(), nl.juiced.guhs.registry.ModItems.CHALLENGE_COMPASS.get(),
                    nl.juiced.guhs.registry.ModItems.MIKA_SPOORKOMPAS.get(), nl.juiced.guhs.registry.ModItems.TAARTKRUIMELS.get(),
                    nl.juiced.guhs.registry.ModItems.HEILIGDOM_KOMPAS.get(), nl.juiced.guhs.registry.ModItems.KERMISKOMPAS.get(),
                    nl.juiced.guhs.registry.ModItems.VERSTOPKOMPAS.get(), nl.juiced.guhs.registry.ModItems.KONINGSKOMPAS.get(),
                    nl.juiced.guhs.registry.ModItems.SUPERKOMPAS.get())) {
                net.minecraft.client.renderer.item.ItemProperties.register(compass, net.minecraft.resources.ResourceLocation.withDefaultNamespace("angle"),
                        new net.minecraft.client.renderer.item.CompassItemPropertyFunction((level, stack, entity) -> {
                            var tracker = stack.get(net.minecraft.core.component.DataComponents.LODESTONE_TRACKER);
                            return tracker == null ? null : tracker.target().orElse(null);
                        }));
            }
        });
    }

    /** Guh wire uses the (grey) vanilla redstone dust textures, tinted pink: dark when off, bright when powered. */
    private static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> state.getValue(GuhWireBlock.POWERED) ? GuhWireBlock.COLOR_ON : GuhWireBlock.COLOR_OFF,
                ModBlocks.GUH_WIRE.get());
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.BANK_GUH.get(), BankGuhScreen::new);
        event.register(ModMenuTypes.GUH_WARDROBE.get(), nl.juiced.guhs.client.screen.GuhWardrobeScreen::new);
    }

    /** Models that aren't a block state, but drawn by a renderer (the spinning wheel ring). */
    private static void registerExtraModels(ModelEvent.RegisterAdditional event) {
        event.register(GuhWheelRenderer.RING_MODEL);
        event.register(GuhWheelRenderer.STAND_MODEL);
    }

    private GuhsClient() {
    }
}
