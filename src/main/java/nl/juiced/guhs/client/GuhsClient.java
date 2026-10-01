package nl.juiced.guhs.client;

import java.util.List;
import java.util.Set;

import com.geckolib.constant.DefaultAnimations;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.GuhWireBlock;
import nl.juiced.guhs.client.screen.BankGuhScreen;
import nl.juiced.guhs.entity.GuhBeeEntity;
import nl.juiced.guhs.entity.GuhVisEntity;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.registry.ModBlockEntities;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModMenuTypes;

/** Client-only setup. Only called on the physical client (see Guhs constructor). */
public final class GuhsClient {
    public static void init(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        // the client config (language) with a config screen in the mod list. (1.2.0: the official server is no longer added to the
        // multiplayer server list; servers already in a player's list stay there.)
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, GuhsClientConfig.SPEC);
        container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                net.neoforged.neoforge.client.gui.ConfigurationScreen::new);
        // 1.2.0: the NL/EN switch of the Guhs texts (config "language", guh menu button)
        GuhsTaal.init(modBus);
        nl.juiced.guhs.feature.FeaturesClient.init(modBus);
        modBus.addListener(GuhsClient::registerRenderers);
        modBus.addListener(GuhsClient::registerLayerDefinitions);
        modBus.addListener(GuhsClient::addVillagerLayers);
        modBus.addListener(GuhWheelRenderer::registerModels);
        modBus.addListener(GuhsClient::registerScreens);
        modBus.addListener(GuhsClient::registerBlockColors);
        modBus.addListener(KaasSausClient::registerFluidLooks);
        modBus.addListener(KaasSausClient::registerFluidModels);
        modBus.addListener(GuhsClient::clientSetup);
        modBus.addListener(GuhKeys::register);
        modBus.addListener(GuhmensionSky::register);
        modBus.addListener(SkyDraw::registerPipelines);
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
    private static void addVillagerLayers(EntityRenderersEvent.AddLayers event) {
        if (event.getRenderer(EntityType.VILLAGER) instanceof VillagerRenderer villagers) {
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
        event.registerEntityRenderer(ModEntities.GUH_SEAT.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.GUH_BEE.get(), context -> new GeoEntityRenderer<GuhBeeEntity, LivingEntityRenderState>(context,
                new DefaultedEntityGeoModel<>(Guhs.id("guh_bee"))));
        event.registerEntityRenderer(ModEntities.GUH_SLIME.get(), context -> new SlimeRenderer(context) {
            @Override
            public Identifier getTextureLocation(SlimeRenderState state) {
                return Guhs.id("textures/entity/guh_slime.png");
            }
        });
        event.registerEntityRenderer(ModEntities.NETHER_MIKA.get(), context -> new GeoEntityRenderer<MikaEntity, LivingEntityRenderState>(context,
                new DefaultedEntityGeoModel<MikaEntity>(Guhs.id("mika")) {
                    @Override
                    public Identifier getTextureResource(GeoRenderState state) {
                        return Guhs.id("textures/entity/nether_mika.png");
                    }
                }.withAltAnimations(Guhs.id("guh"))) {
            @Override
            public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
                DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
            }
        });
        event.registerEntityRenderer(ModEntities.GUH_VIS.get(), context -> new GeoEntityRenderer<GuhVisEntity, LivingEntityRenderState>(context,
                new DefaultedEntityGeoModel<>(Guhs.id("guh_vis"))));
        event.registerBlockEntityRenderer(ModBlockEntities.SLEE_RAIL.get(), SleeRailRenderer::new);
        event.registerEntityRenderer(ModEntities.MIKA_BAAS.get(), SittingGuhRenderers.MikaBaasRenderer::new);
    }

    /**
     * 1.1.0: the guh compasses' needle ("angle" item property) is data now: their client item definitions
     * (assets/guhs/items/*.json, owner D) use {@code minecraft:range_dispatch} with {@code "property": "minecraft:compass",
     * "target": "lodestone"}, which reads the same LODESTONE_TRACKER component the old property function read.
     */
    private static void clientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> nl.juiced.guhs.entity.GuhEntity.riderJumping =
                () -> net.minecraft.client.Minecraft.getInstance().options.keyJump.isDown());
    }

    /** Guh wire uses the (grey) vanilla redstone dust textures, tinted pink: dark when off, bright when powered. */
    private static void registerBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(new BlockTintSource() {
            @Override
            public int color(BlockState state) {
                return ARGB.opaque(state.getValue(GuhWireBlock.POWERED) ? GuhWireBlock.COLOR_ON : GuhWireBlock.COLOR_OFF);
            }

            @Override
            public Set<Property<?>> relevantProperties() {
                return Set.of(GuhWireBlock.POWERED);
            }
        }), ModBlocks.GUH_WIRE.get());
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.BANK_GUH.get(), BankGuhScreen::new);
        event.register(ModMenuTypes.GUH_WARDROBE.get(), nl.juiced.guhs.client.screen.GuhWardrobeScreen::new);
    }

    private GuhsClient() {
    }
}
