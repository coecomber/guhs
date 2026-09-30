package nl.juiced.guhs.feature.sjoelen.client;

import javax.annotation.Nullable;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
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
import nl.juiced.guhs.feature.sjoelen.SjoelBak;
import nl.juiced.guhs.feature.sjoelen.SjoelSchijvenItem;
import nl.juiced.guhs.feature.sjoelen.SjoelenBlocks;
import nl.juiced.guhs.feature.sjoelen.SjoelenFeature;
import nl.juiced.guhs.feature.sjoelen.SjoelenPayloads;

/**
 * Client side of guh-sjoelen: the puck renderer, Opoe Njegschuif's model (glasses, a bun with knitting needles, a
 * shawl; her bun wobbles), her screen, and while you hold the pucks a power bar (going up and down, with Opoe's two
 * marks) plus a short dotted line on the bak showing which way the puck will go.
 */
public final class SjoelenClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(SjoelenFeature.SCHIJF.get(), SjoelSchijfRenderer::new));
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) -> event.registerLayerDefinition(SjoelSchijfRenderer.LAYER,
                SjoelSchijfRenderer::createLayer));
        modBus.addListener(SjoelenClient::registerGui);
        NeoForge.EVENT_BUS.addListener(SjoelenClient::onClientTick);
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.SJOELGUH, Guhs.id("entity/guh_npc_sjoelguh"));
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.SJOELGUH, (npc, tick) -> {
            float t = (float) tick * 0.05f;
            return bones -> bones.ifPresent("opoe_knot", b -> b.setRotZ((float) Math.sin(t) * 0.05f));
        });
    }

    public static void openScreen(SjoelenPayloads.Open open) {
        Minecraft.getInstance().setScreen(new SjoelenScreen(open));
    }

    private static void registerGui(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, Guhs.id("sjoelen_power"), SjoelenClient::renderPower);
    }

    private static int holdTicks(Player player) {
        return player.isUsingItem() && player.getUseItem().getItem() instanceof SjoelSchijvenItem ? player.getTicksUsingItem() : -1;
    }

    private static void renderPower(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        int ticks = holdTicks(mc.player);
        if (ticks < 0) {
            return;
        }
        float power = SjoelSchijvenItem.power(ticks);
        int w = 120, h = 8, x = (g.guiWidth() - w) / 2, y = g.guiHeight() - 92;
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFFB98A5A);                     // a wooden frame
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF3A2616);
        int good0 = Math.round(w * SjoelSchijvenItem.GOED_VAN), good1 = Math.round(w * SjoelSchijvenItem.GOED_TOT);
        g.fill(x + good0, y, x + good1, y + h, 0x3050E070);                          // Opoe's good part, faintly
        int filled = Math.round(w * power);
        boolean good = power >= SjoelSchijvenItem.GOED_VAN && power <= SjoelSchijvenItem.GOED_TOT;
        for (int i = 0; i < filled; i++) {
            float t = i / (float) w;
            int r = (int) (230 - t * 40), gr = (int) (190 - t * 90), b = (int) (120 + t * 60);
            g.fill(x + i, y, x + i + 1, y + h, 0xFF000000 | r << 16 | gr << 8 | b);
        }
        g.fill(x + good0, y - 2, x + good0 + 1, y + h + 2, 0xFFFFF0C0);               // Opoe's two marks
        g.fill(x + good1, y - 2, x + good1 + 1, y + h + 2, 0xFFFFF0C0);
        Component label = Component.translatable("gui.guhs.sjoelen.power", Math.round(power * 100));
        g.centeredText(mc.font, label, g.guiWidth() / 2, y - 11, good ? 0xFFB6F5A0 : 0xFFFFE6CC);
    }

    /** The head of a sjoelbak near the player (the kop block with deel 0), or null. */
    @Nullable
    private static BlockPos kop(Player player) {
        BlockPos c = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-7, -2, -7), c.offset(7, 2, 7))) {
            BlockState state = player.level().getBlockState(pos);
            if (state.is(SjoelenFeature.KOP.get()) && state.getValue(SjoelenBlocks.DEEL) == 0) {
                return pos.immutable();
            }
        }
        return null;
    }

    private static BlockPos cachedKop;
    private static long cachedAt = -100;

    /** The aiming line: a few dots on the bak from where the puck starts, the way it will slide. */
    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused()) {
            return;
        }
        int ticks = holdTicks(mc.player);
        if (ticks < 0 || ticks % 2 != 0) {
            return;
        }
        if (mc.level.getGameTime() - cachedAt > 40) {
            cachedKop = kop(mc.player);
            cachedAt = mc.level.getGameTime();
        }
        BlockPos kop = cachedKop;
        if (kop == null || !mc.level.getBlockState(kop).is(SjoelenFeature.KOP.get())) {
            return;
        }
        Direction f = mc.level.getBlockState(kop).getValue(SjoelenBlocks.Deel.FACING), r = f.getClockWise();
        double ox = kop.getX() + 0.5 - f.getStepX() * 0.5 - r.getStepX() * 0.5, oz = kop.getZ() + 0.5 - f.getStepZ() * 0.5 - r.getStepZ() * 0.5;
        double dx = mc.player.getX() - ox, dz = mc.player.getZ() - oz;
        double v = Math.max(SjoelBak.R, Math.min(SjoelBak.WIDTH - SjoelBak.R, dx * r.getStepX() + dz * r.getStepZ()));
        Vec3 look = mc.player.getLookAngle();
        double along = look.x * f.getStepX() + look.z * f.getStepZ(), side = look.x * r.getStepX() + look.z * r.getStepZ();
        if (along < 0.15) {
            return;
        }
        double a = SjoelBak.angle(Math.atan2(side, along));
        float power = SjoelSchijvenItem.power(ticks);
        boolean good = power >= SjoelSchijvenItem.GOED_VAN && power <= SjoelSchijvenItem.GOED_TOT;
        DustParticleOptions dust = new DustParticleOptions(good ? 0x73E680 : 0xFFBF73, 0.6f);   // (0.45, 0.9, 0.5) / (1, 0.75, 0.45)
        for (double d = 0.5; d <= 4.5; d += 0.5) {
            double u = SjoelBak.START_U + Math.cos(a) * d, vv = v + Math.sin(a) * d;
            if (vv < 0 || vv > SjoelBak.WIDTH) {
                break;
            }
            double x = ox + f.getStepX() * u + r.getStepX() * vv, z = oz + f.getStepZ() * u + r.getStepZ() * vv;
            mc.level.addParticle(dust, x, kop.getY() + 1.08, z, 0, 0, 0);
        }
    }

    private SjoelenClient() {
    }
}
