package nl.juiced.guhs;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.client.GuhsClient;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.event.KeepOnDeathHandler;
import nl.juiced.guhs.registry.ModBlockEntities;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModFeatures;
import nl.juiced.guhs.registry.ModVillagers;
import nl.juiced.guhs.registry.ModCreativeTabs;
import nl.juiced.guhs.registry.ModDataComponents;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModFluids;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModMenuTypes;
import nl.juiced.guhs.registry.ModPoiTypes;
import nl.juiced.guhs.registry.ModSounds;
import nl.juiced.guhs.world.GuhmensionSpawner;

/**
 * Guhs - add lieve vadsige guhs to minecraft!
 */
@Mod(Guhs.MODID)
public class Guhs {
    public static final String MODID = "guhs";

    public Guhs(IEventBus modBus) {
        ModFluids.FLUID_TYPES.register(modBus);
        ModFluids.FLUIDS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        // (26.1: armour materials are no registry any more - ModArmorMaterials.ARMOR_MATERIALS is gone)
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITY_TYPES.register(modBus);
        ModFeatures.FEATURES.register(modBus);
        nl.juiced.guhs.registry.ModParticles.PARTICLES.register(modBus);
        nl.juiced.guhs.registry.ModStructureTypes.STRUCTURE_TYPES.register(modBus);
        nl.juiced.guhs.registry.ModStructureTypes.PLACEMENT_MODIFIERS.register(modBus);
        nl.juiced.guhs.registry.ModStructureTypes.POOL_ELEMENT_TYPES.register(modBus);   // 2.10: guhs:grond_single_pool_element
        ModSounds.SOUND_EVENTS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModPoiTypes.POI_TYPES.register(modBus);
        ModVillagers.TYPES.register(modBus);
        ModVillagers.PROFESSIONS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenuTypes.MENUS.register(modBus);
        ModDataComponents.COMPONENTS.register(modBus);
        nl.juiced.guhs.world.BouwCheck.TICKET_TYPES.register(modBus);   // 26.1: ticket types are a registry
        nl.juiced.guhs.feature.Features.register(modBus);   // the 2.4 minigames and rare structures
        nl.juiced.guhs.gametest.GuhsGameTests.register(modBus);   // 26.1: our @GuhTest registrar (only when gametests are enabled)

        modBus.addListener(ModEntities::registerAttributes);
        modBus.addListener(ModPoiTypes::addHiveBlocks);
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(ModBlocks::registerPots));
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> nl.juiced.guhs.compat.FtbQuestsChapter.install());
        modBus.addListener(ModEntities::registerSpawnPlacements);
        modBus.addListener(ModNetworking::register);
        NeoForge.EVENT_BUS.addListener(GuhmensionSpawner::onLevelTick);
        NeoForge.EVENT_BUS.addListener(nl.juiced.guhs.world.BouwCheck::registerCommands);
        NeoForge.EVENT_BUS.addListener(nl.juiced.guhs.world.BouwCheck::onServerTick);
        NeoForge.EVENT_BUS.addListener(nl.juiced.guhs.world.BouwRuimte::onLevelLoad);
        NeoForge.EVENT_BUS.addListener(KeepOnDeathHandler::onDeath);
        NeoForge.EVENT_BUS.addListener(nl.juiced.guhs.quest.PicknickMuziek::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(KeepOnDeathHandler::onRespawnCopy);
        NeoForge.EVENT_BUS.addListener(ModVillagers::onTrades);
        NeoForge.EVENT_BUS.addListener(ModVillagers::onVillagerTick);
        NeoForge.EVENT_BUS.addListener(nl.juiced.guhs.quest.GuhQuests::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(nl.juiced.guhs.quest.GuhQuests::onBigMikaKilled);
        NeoForge.EVENT_BUS.register(nl.juiced.guhs.world.MaagProtection.class);
        NeoForge.EVENT_BUS.register(nl.juiced.guhs.world.VerstopProtection.class);
        NeoForge.EVENT_BUS.addListener(nl.juiced.guhs.quest.VerstopGame::onDamage);
        NeoForge.EVENT_BUS.addListener(nl.juiced.guhs.quest.VerstopGame::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(nl.juiced.guhs.quest.VerstopGame::onLogout);
        NeoForge.EVENT_BUS.addListener(nl.juiced.guhs.quest.VerstopGame::onChangedDimension);
        NeoForge.EVENT_BUS.addListener(nl.juiced.guhs.quest.VerstopGame::onDeath);
        NeoForge.EVENT_BUS.addListener(nl.juiced.guhs.quest.VerstopGame::onServerStopped);
        NeoForge.EVENT_BUS.addListener(nl.juiced.guhs.quest.KasteelPoort::onChat);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            GuhsClient.init(modBus);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
