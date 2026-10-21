package nl.juiced.guhs.feature.bouwhulp.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.bouwhulp.BouwhulpFeature;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetPayloads;
import nl.juiced.guhs.feature.bouwhulp.kern.SpookPayloads;

/**
 * The client of the Bouwhulp (F8): the ghosts ({@link ClientSpoken}, drawn by {@link SpookTekenaar}), their control panel
 * ({@link SpookScherm}), the Bouwkeet (drawn by {@link KeetRenderer}) and its menu ({@link BouwkeetScreen} with F8's pages,
 * {@link KeetPaginas}).
 */
public final class BouwhulpClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(BouwhulpFeature.BOUWKEET_BE.get(), KeetRenderer::new));
        KeetPaginas.register();
        SpookPayloads.ontvang = t -> Minecraft.getInstance().execute(() -> ClientSpoken.ontvang(t));
        SpookPayloads.wegHaak = id -> Minecraft.getInstance().execute(() -> ClientSpoken.weg(id));
        SpookPayloads.allesWegHaak = () -> Minecraft.getInstance().execute(ClientSpoken::wegAlles);
        SpookPayloads.voortgangHaak = t -> Minecraft.getInstance().execute(() -> ClientSpoken.voortgang(t));
        SpookPayloads.bedieningHaak = BouwhulpClient::bediening;
        BouwkeetPayloads.opener = BouwhulpClient::keet;
        NeoForge.EVENT_BUS.addListener(SpookTekenaar::teken);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.level.getGameTime() % 10 == 0) {
                ClientSpoken.herbereken();
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            ClientSpoken.wegAlles();
            SpookTekenaar.vergeet();
        });
    }

    private static void bediening(CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.screen instanceof SpookScherm s && s.id().equals(data.read("Id", net.minecraft.core.UUIDUtil.CODEC).orElse(null))) {
                s.update(data);
            } else {
                mc.setScreen(new SpookScherm(data));
            }
        });
    }

    private static void keet(CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.screen instanceof BouwkeetScreen s && s.pos().asLong() == data.getLongOr("Pos", 0L)) {
                s.update(data);
            } else {
                mc.setScreen(new BouwkeetScreen(data));
            }
        });
    }

    private BouwhulpClient() {
    }
}
