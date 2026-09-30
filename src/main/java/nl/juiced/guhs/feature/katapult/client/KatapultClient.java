package nl.juiced.guhs.feature.katapult.client;

import javax.annotation.Nullable;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.katapult.KatapultBlocks;
import nl.juiced.guhs.feature.katapult.KatapultFeature;
import nl.juiced.guhs.feature.katapult.KatapultGame;
import nl.juiced.guhs.feature.katapult.KatapultPayloads;
import nl.juiced.guhs.feature.katapult.PluisbalEntity;
import nl.juiced.guhs.feature.katapult.PluisballenItem;

/**
 * Client side of the Knabbelkatapult: the pluisbal and the tumbling fort pieces, Kapitein Floepguh's model (a captain's
 * hat with a pink feather, an eye patch that's really a heart, a striped scarf; his feather wiggles), his screen, and
 * while you pull the elastic a power bar plus (on makkelijk) the dotted flight of the ball.
 */
public final class KatapultClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(KatapultFeature.PLUISBAL.get(), PluisbalRenderer::new);
            event.registerEntityRenderer(KatapultFeature.BROKJE.get(), BrokjeRenderer::new);
        });
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) -> event.registerLayerDefinition(PluisbalRenderer.LAYER,
                PluisbalRenderer::createLayer));
        modBus.addListener(KatapultClient::registerGui);
        NeoForge.EVENT_BUS.addListener(KatapultClient::onClientTick);
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.KATAPULTGUH, Guhs.id("entity/guh_npc_katapultguh"));
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.KATAPULTGUH, (npc, tick) -> {
            float t = (float) tick * 0.09f;
            return bones -> bones.ifPresent("kapitein_veer", b -> b.setRotX((float) Math.sin(t) * 0.12f));
        });
    }

    public static void openScreen(KatapultPayloads.Open open) {
        Minecraft.getInstance().setScreen(new KatapultScreen(open));
    }

    private static void registerGui(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, Guhs.id("katapult_power"), KatapultClient::renderPower);
    }

    private static int pullTicks(Player player) {
        return player.isUsingItem() && player.getUseItem().getItem() instanceof PluisballenItem ? player.getTicksUsingItem() : -1;
    }

    private static void renderPower(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        int ticks = pullTicks(mc.player);
        if (ticks < 0) {
            return;
        }
        float power = PluisballenItem.power(ticks);
        int w = 110, h = 8, x = (g.guiWidth() - w) / 2, y = g.guiHeight() - 92;
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFFF7B6CB);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF2A1A30);
        int filled = Math.round(w * power);
        for (int i = 0; i < filled; i++) {                         // pink -> lilac -> gold
            float t = i / (float) w;
            int r = (int) (t < 0.6f ? 250 - t * 60 : 214 + (t - 0.6f) * 100);
            int gr = (int) (t < 0.6f ? 170 - t * 60 : 134 + (t - 0.6f) * 180);
            int b = (int) (t < 0.6f ? 210 + t * 40 : 234 - (t - 0.6f) * 400);
            g.fill(x + i, y, x + i + 1, y + h, 0xFF000000 | Math.min(255, r) << 16 | Math.min(255, gr) << 8 | Math.max(0, Math.min(255, b)));
        }
        g.fill(x + w - 2, y - 1, x + w - 1, y + h + 1, 0xFFFFFFFF);
        Component label = Component.translatable("gui.guhs.katapult.power", Math.round(power * 100));
        g.centeredText(mc.font, label, g.guiWidth() / 2, y - 11, power > 0.99f ? 0xFFFFD27A : 0xFFFFE6EE);
    }

    @Nullable
    private static BlockPos cachedWerper;
    private static long cachedAt = -100;

    @Nullable
    private static BlockPos werper(Player player) {
        BlockPos c = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-5, -3, -5), c.offset(5, 3, 5))) {
            if (player.level().getBlockState(pos).is(KatapultFeature.WERPER.get())) {
                return pos.immutable();
            }
        }
        return null;
    }

    /** Makkelijk: the dotted flight of the pluisbal (same physics as the server, without wind: makkelijk has none). */
    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused()) {
            return;
        }
        int ticks = pullTicks(mc.player);
        if (ticks < 0 || ticks % 2 != 0 || !PluisballenItem.richtlijn(mc.player.getUseItem())) {
            return;
        }
        if (mc.level.getGameTime() - cachedAt > 40) {
            cachedWerper = werper(mc.player);
            cachedAt = mc.level.getGameTime();
        }
        BlockPos werper = cachedWerper;
        if (werper == null) {
            return;
        }
        BlockState state = mc.level.getBlockState(werper);
        if (!state.is(KatapultFeature.WERPER.get())) {
            return;
        }
        Direction facing = state.getValue(KatapultBlocks.Gericht.FACING);
        float power = PluisballenItem.power(ticks);
        double speed = KatapultGame.MIN_SPEED + (KatapultGame.MAX_SPEED - KatapultGame.MIN_SPEED) * power;
        Vec3 pos = KatapultGame.launchPoint(werper, facing);
        Vec3 v = mc.player.getLookAngle().normalize().scale(speed);
        DustParticleOptions dust = new DustParticleOptions(net.minecraft.util.ARGB.colorFromFloat(1f, 1f, 0.55f + power * 0.3f, 0.8f - power * 0.3f) & 0xFFFFFF, 0.8f);
        for (int t = 0; t < 90; t++) {
            v = v.add(0, -PluisbalEntity.GRAVITY, 0).scale(PluisbalEntity.DRAG);
            Vec3 next = pos.add(v);
            if (mc.level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)).getType() == HitResult.Type.BLOCK) {
                break;
            }
            pos = next;
            if (t % 2 == 1) {
                mc.level.addParticle(dust, pos.x, pos.y, pos.z, 0, 0, 0);
            }
        }
    }

    private KatapultClient() {
    }
}
