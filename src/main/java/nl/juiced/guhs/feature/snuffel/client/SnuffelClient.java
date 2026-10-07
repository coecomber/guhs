package nl.juiced.guhs.feature.snuffel.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.snuffel.BoompjeEntity;
import nl.juiced.guhs.feature.snuffel.Hondvorm;
import nl.juiced.guhs.feature.snuffel.SnuffelFeature;
import nl.juiced.guhs.feature.snuffel.SnuffelPayloads;

/**
 * Client side of Het Snuffeleiland: the renderers of the dogs, the companion and the tree (the approved models of
 * tools/features/snuffel_modellen.py), the dog form as you see and steer it ({@link HondClient}), the keys
 * ({@link SnuffelKeys}), the scent meter ({@link SnuffelHud}), the screens (the choice, the snuffelboekje, the memory
 * card's menu, the Guhstation's window) and the local player's own state ({@link EigenStand}).
 */
public final class SnuffelClient {
    private static long ticks;
    private static int boomGetoond;

    private SnuffelClient() {
    }

    /** This client's own clock (ticks since the game started; it does not jump when the level changes). */
    static long ticks() {
        return ticks;
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(SnuffelFeature.SNUFFEL_BEWONER.get(), HondRenderer::new);
            event.registerEntityRenderer(SnuffelFeature.SNUFFEL_HOND.get(), HondRenderer::new);
            event.registerEntityRenderer(SnuffelFeature.SNUFFEL_MAATJE.get(), MaatjeRenderer::new);
            event.registerEntityRenderer(SnuffelFeature.SNUFFEL_BOOMPJE.get(), BoompjeRenderer::new);
        });
        modBus.addListener((RegisterKeyMappingsEvent event) -> SnuffelKeys.register(event));
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(Guhs.id("snuffel_hud"), SnuffelHud::extractRenderState));

        Hondvorm.clientRas = HondClient::ras;
        SnuffelPayloads.vormOntvanger = HondClient::vorm;
        SnuffelPayloads.houdingOntvanger = HondClient::houding;
        SnuffelPayloads.gebaarOntvanger = HondClient::gebaar;
        SnuffelPayloads.standOntvanger = EigenStand::ontvang;
        SnuffelPayloads.meterOntvanger = SnuffelHud::meter;
        SnuffelPayloads.openOntvanger = SnuffelClient::open;

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
        NeoForge.EVENT_BUS.addListener((RenderPlayerEvent.Pre<?> event) -> HondClient.teken(event));
        NeoForge.EVENT_BUS.addListener((RenderHandEvent event) -> HondClient.hand(event));
        NeoForge.EVENT_BUS.addListener((InputEvent.InteractionKeyMappingTriggered event) -> HondClient.toets(event));
        NeoForge.EVENT_BUS.addListener((MovementInputUpdateEvent event) -> HondClient.stilStaan(event));
        // a dog has no pockets to look into: the inventory key opens the snuffelboekje (a creative builder keeps the inventory)
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Opening event) -> {
            Minecraft mc = Minecraft.getInstance();
            if (event.getNewScreen() instanceof InventoryScreen && HondClient.eigenHond() && mc.player != null && !mc.player.getAbilities().instabuild) {
                event.setNewScreen(new BoekjeScherm());
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            HondClient.wis();
            EigenStand.wis();
            SnuffelHud.meter(SnuffelPayloads.Meter.NIETS);
            boomGetoond = 0;
        });
    }

    private static void tick() {
        ticks++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) {
            return;
        }
        HondClient.tick();
        SnuffelHud.tick();
        // the tree: when the local player's stage goes up, every tree on this client pops
        boolean[] gegroeid = new boolean[1];
        int stap = EigenStand.boom(gegroeid);
        if (stap != boomGetoond) {
            boomGetoond = stap;
            if (gegroeid[0]) {
                pop(mc.level);
            }
        }
    }

    private static void pop(ClientLevel level) {
        for (Entity e : level.entitiesForRendering()) {
            if (e instanceof BoompjeEntity boom) {
                boom.pop();
            }
        }
    }

    private static void open(SnuffelPayloads.Open p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        switch (p.scherm()) {
            case SnuffelPayloads.Open.KEUZE -> mc.setScreen(new KeuzeScherm(p.data()));
            case SnuffelPayloads.Open.PAUZE -> {
                EigenStand.zet(p.data());
                mc.setScreen(new PauzeScherm(p.data()));
            }
            case SnuffelPayloads.Open.GUHSTATION -> {
                EigenStand.zet(p.data());
                mc.setScreen(new GuhstationScherm(p.data()));
            }
            case SnuffelPayloads.Open.BOEKJE -> {
                EigenStand.zet(p.data());
                mc.setScreen(new BoekjeScherm());
            }
            default -> {
            }
        }
    }
}
