package nl.juiced.guhs.feature.golf.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.golf.GolfBallEntity;
import nl.juiced.guhs.feature.golf.GolfClubItem;
import nl.juiced.guhs.feature.golf.GolfFeature;
import nl.juiced.guhs.feature.golf.GolfGame;
import nl.juiced.guhs.feature.golf.GolfPayloads;

/**
 * Client side of guh golf: the ball's renderer, the Golfguh's screen, and while you swing the club a power meter above
 * the hotbar plus a dotted line on the course showing where (and roughly how far) the ball will go.
 */
public final class GolfClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(GolfClient::registerRenderers);
        modBus.addListener(GolfClient::registerLayers);
        modBus.addListener(GolfClient::registerGui);
        NeoForge.EVENT_BUS.addListener(GolfClient::onClientTick);
    }

    public static void openScreen(GolfPayloads.Open open) {
        Minecraft.getInstance().setScreen(new GolfScreen(open));
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GolfFeature.BALL.get(), GolfBallRenderer::new);
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GolfBallRenderer.LAYER, GolfBallRenderer::createLayer);
    }

    private static void registerGui(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, Guhs.id("golf_power"), GolfClient::renderPower);
    }

    /** How long the local player has been swinging the club, or -1. */
    private static int swingTicks(Player player) {
        return player.isUsingItem() && player.getUseItem().getItem() instanceof GolfClubItem ? player.getTicksUsingItem() : -1;
    }

    private static void renderPower(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        int ticks = swingTicks(mc.player);
        if (ticks < 0) {
            return;
        }
        float power = GolfClubItem.power(ticks);
        int w = 104, h = 8, x = (g.guiWidth() - w) / 2, y = g.guiHeight() - 92;
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFFF7B6CB);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF301A26);
        int filled = Math.round(w * power);
        for (int i = 0; i < filled; i++) {                       // mint -> pink -> gold
            float t = i / (float) w;
            int r = (int) (t < 0.5f ? 120 + t * 2 * 135 : 255);
            int gr = (int) (t < 0.5f ? 220 - t * 2 * 90 : 130 + (t - 0.5f) * 2 * 80);
            int b = (int) (t < 0.5f ? 170 + t * 2 * 20 : 190 - (t - 0.5f) * 2 * 150);
            g.fill(x + i, y, x + i + 1, y + h, 0xFF000000 | r << 16 | gr << 8 | b);
        }
        g.fill(x + w - 2, y - 1, x + w - 1, y + h + 1, 0xFFFFFFFF);  // the VAHOEG! mark at 100%
        Component label = Component.translatable("gui.guhs.golf.power", Math.round(power * 100));
        g.drawCenteredString(mc.font, label, g.guiWidth() / 2, y - 11, power > 0.97f ? 0xFFFFD27A : 0xFFFFE6EE);
    }

    /** The aiming line: pink dots from your ball in the direction you look, as far as it would roll on flat felt. */
    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused()) {
            return;
        }
        int ticks = swingTicks(mc.player);
        if (ticks < 0 || ticks % 2 != 0) {
            return;
        }
        GolfBallEntity ball = null;
        for (GolfBallEntity b : mc.level.getEntitiesOfClass(GolfBallEntity.class, mc.player.getBoundingBox().inflate(GolfGame.REACH + 1))) {
            if (mc.player.getUUID().equals(b.getOwner()) && !b.isSunk()) {
                ball = b;
            }
        }
        if (ball == null) {
            return;
        }
        Vec3 look = mc.player.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        if (dir.lengthSqr() < 1e-4) {
            return;
        }
        dir = dir.normalize();
        float power = GolfClubItem.power(ticks);
        double speed = GolfGame.MIN_SPEED + (GolfGame.MAX_SPEED - GolfGame.MIN_SPEED) * Math.pow(power, 1.5);
        double reach = Math.min(12, speed * GolfBallEntity.FRICTION / (1 - GolfBallEntity.FRICTION));
        DustParticleOptions dust = new DustParticleOptions(new org.joml.Vector3f(1f, 0.45f + power * 0.35f, 0.75f - power * 0.4f), 0.7f);
        for (double d = 0.6; d <= reach; d += 0.6) {
            mc.level.addParticle(dust, ball.getX() + dir.x * d, ball.getY() + 0.12, ball.getZ() + dir.z * d, 0, 0, 0);
        }
    }

    private GolfClient() {
    }
}
