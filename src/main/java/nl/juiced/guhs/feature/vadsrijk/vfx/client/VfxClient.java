package nl.juiced.guhs.feature.vadsrijk.vfx.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.vfx.VfxPayloads;

/**
 * The client of the VFX kit v1 (T-VFX): takes the {@code guhs:vr_vfx} packets, keeps decals/particles/meshes/poses/the cast bar,
 * ticks them on the client clock and draws them after the translucent blocks. Config: {@link VfxConfig}
 * (config/guhs-vadsrijk-vfx.toml, VOL / ZACHT / UIT).
 */
public final class VfxClient {
    private static long klok;
    private static ClientLevel vorig;

    private VfxClient() {
    }

    /** The client tick counter the effects run on. */
    public static long klok() {
        return klok;
    }

    public static void init(IEventBus modBus) {
        ModList.get().getModContainerById(Guhs.MODID).ifPresent(c -> c.registerConfig(ModConfig.Type.CLIENT, VfxConfig.SPEC, "guhs-vadsrijk-vfx.toml"));
        VfxPayloads.ontvanger = m -> Minecraft.getInstance().execute(() -> ontvang(m));
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAbove(VanillaGuiLayers.HOTBAR, Guhs.id("vr_vfx_cast"), CastBalk::teken));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener(VfxClient::teken);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> wis());
    }

    private static void wis() {
        Decals.ALLE.clear();
        Effecten.wis();
        VfxPose.wis();
        CastBalk.wis();
    }

    private static void ontvang(VfxPayloads.Fx m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        var d = m.d();
        switch (m.soort()) {
            case "decal" -> Decals.nieuw(d, klok);
            case "weg" -> Decals.weg(d.getIntOr("groep", -1));
            case "deeltjes" -> Effecten.burst(d, cam);
            case "golf", "straal", "flits", "spoor", "zuil" -> Effecten.mesh(m.soort(), d, klok, cam);
            case "pose" -> VfxPose.zet(d.getIntOr("id", -1), d.getStringOr("pose", ""), d.getIntOr("ticks", 10), klok);
            case "cast" -> CastBalk.nieuw(d, klok);
            case "cast_breuk" -> CastBalk.breuk(d);
            case "cast_stop" -> CastBalk.stop(d, klok);
            case "niveau" -> {
                try {
                    VfxConfig.zet(VfxConfig.Niveau.valueOf(d.getStringOr("niveau", "VOL")));
                } catch (IllegalArgumentException ignored) {
                    // unknown level: keep
                }
                if (VfxConfig.niveau() == VfxConfig.Niveau.UIT) {
                    Effecten.wis();
                }
            }
            default -> {
            }
        }
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != vorig) {
            vorig = mc.level;
            wis();
        }
        if (mc.level == null || mc.isPaused()) {
            return;
        }
        klok++;
        Decals.tick(klok);
        Effecten.tick(klok, mc.level);
        VfxPose.tick(klok);
        CastBalk.tick(klok);
    }

    private static void teken(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || (Decals.ALLE.isEmpty() && Effecten.DEELTJES_LIJST.isEmpty() && Effecten.MESHES.isEmpty())) {
            return;
        }
        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        Decals.teken(pose, buffers, cam, klok + pt, mc.level);
        Effecten.teken(pose, buffers, cam, mc.gameRenderer.getMainCamera().rotation(), pt, klok);
    }
}
