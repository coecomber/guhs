package nl.juiced.guhs.feature.vadsrijk.npc.client;

import java.util.List;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.client.KernClient;
import nl.juiced.guhs.feature.vadsrijk.kern.client.RijkHud;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcPayloads;

/**
 * The quest tracker (fix round 1): a small panel top right in the Vadsrijk with the main story's current step (title, ▸ what,
 * who and where, distance + an arrow) and the pinned quest's step, plus a soft pink light beam over the target, visible from
 * afar. Both can be switched off in the questlog (J) or with two keys (unbound by default: no key of the player is taken).
 */
public final class VolgHud {
    public static final KeyMapping TRACKER = new KeyMapping("key.guhs.vr_volger", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KernClient.CATEGORIE);
    public static final KeyMapping STRAAL = new KeyMapping("key.guhs.vr_straal", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KernClient.CATEGORIE);
    private static final int BREED = 172;
    private static final float STRAAL_HOOG = 90, STRAAL_VER = 480;
    private static volatile NpcPayloads.Volg volg = new NpcPayloads.Volg(true, true, List.of());

    private VolgHud() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            e.register(TRACKER);
            e.register(STRAAL);
        });
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(Guhs.id("vr_npc_volg"), VolgHud::teken));
        NpcPayloads.volgOntvanger = v -> Minecraft.getInstance().execute(() -> volg = v);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            while (TRACKER.consumeClick()) {
                kies(NpcPayloads.VolgKeuze.TRACKER);
            }
            while (STRAAL.consumeClick()) {
                kies(NpcPayloads.VolgKeuze.STRAAL);
            }
        });
        NeoForge.EVENT_BUS.addListener(VolgHud::straal);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> volg = new NpcPayloads.Volg(true, true, List.of()));
    }

    private static void kies(int soort) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && Vadsrijk.in(mc.player)) {
            ClientPacketDistributor.sendToServer(new NpcPayloads.VolgKeuze(soort, ""));
        }
    }

    private static boolean tonen() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && Vadsrijk.in(mc.player) && !mc.options.hideGui;
    }

    // ---- the panel ---------------------------------------------------------------------------------------------------------

    private static void teken(GuiGraphicsExtractor g, DeltaTracker dt) {
        NpcPayloads.Volg v = volg;
        if (!v.aan() || v.regels().isEmpty() || !tonen()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Font f = mc.font;
        int x = g.guiWidth() - BREED - 6, y = 6 + RijkHud.bovenVrij();
        for (NpcPayloads.VolgRegel r : v.regels()) {
            y = blok(g, f, mc, r, x, y) + 4;
        }
    }

    private static int blok(GuiGraphicsExtractor g, Font f, Minecraft mc, NpcPayloads.VolgRegel r, int x, int y) {
        int tw = BREED - 12;
        List<FormattedCharSequence> wat = f.split(Component.literal("▸ ").append(r.wat()), tw);
        List<FormattedCharSequence> waar = f.split(r.waar(), tw);
        if (waar.size() > 3) {
            waar = waar.subList(0, 3);
        }
        List<FormattedCharSequence> extra = r.extra().map(c -> f.split(c, tw)).orElse(List.of());
        boolean pijl = r.doel().isPresent();
        int hoog = 6 + 10 + wat.size() * 10 + waar.size() * 9 + extra.size() * 9 + (pijl ? 12 : 0) + 3;
        int accent = r.hoofd() ? 0xFFFFB21A : 0xFFFFF27A;
        g.fill(x, y, x + BREED, y + hoog, 0xA0200E1A);
        g.fill(x, y, x + 2, y + hoog, accent);
        int ty = y + 4;
        String titel = (r.hoofd() ? "★ " : "◆ ") + r.titel().getString();
        g.text(f, f.plainSubstrByWidth(titel, tw), x + 7, ty, r.hoofd() ? 0xFFFFC94A : 0xFFFFF27A, true);
        ty += 11;
        for (FormattedCharSequence l : wat) {
            g.text(f, l, x + 7, ty, 0xFFFFFFFF, true);
            ty += 10;
        }
        for (FormattedCharSequence l : waar) {
            g.text(f, l, x + 7, ty, 0xFFFFD9A8, true);
            ty += 9;
        }
        for (FormattedCharSequence l : extra) {
            g.text(f, l, x + 7, ty, 0xFFFF9AD5, true);
            ty += 9;
        }
        if (pijl && mc.player != null) {
            BlockPos d = r.doel().get();
            double dx = d.getX() + 0.5 - mc.player.getX(), dz = d.getZ() + 0.5 - mc.player.getZ(), dy = d.getY() - mc.player.getY();
            int afstand = (int) Math.sqrt(dx * dx + dz * dz);
            float doelYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
            float rel = Mth.wrapDegrees(doelYaw - mc.player.getYRot());
            int ax = x + 12, ay = ty + 5;
            if (afstand > 3) {
                pijl(g, ax, ay, rel, accent);
            } else {
                g.fill(ax - 2, ay - 2, ax + 3, ay + 3, accent);
            }
            Component a = Component.translatable("vadsrijk.npc.volg.afstand", afstand);
            if (dy > 6) {
                a = Component.translatable("vadsrijk.npc.volg.hoger", afstand, (int) dy);
            } else if (dy < -6) {
                a = Component.translatable("vadsrijk.npc.volg.lager", afstand, (int) -dy);
            }
            g.text(f, a, x + 22, ty + 1, 0xFFE8D6DF, true);
        }
        return y + hoog;
    }

    /** A small arrow, pointing up = straight ahead, turned by rel degrees (clockwise = to the right). */
    private static void pijl(GuiGraphicsExtractor g, int x, int y, float rel, int kleur) {
        g.pose().pushMatrix();
        g.pose().translate(x + 0.5f, y + 0.5f);
        g.pose().rotate(rel * Mth.DEG_TO_RAD);
        g.fill(-1, -1, 1, 5, kleur);   // shaft
        for (int i = 0; i < 4; i++) {   // head: rows getting wider downwards
            g.fill(-i, -5 + i, i + 1, -4 + i, kleur);
        }
        g.pose().popMatrix();
    }

    // ---- the light beam --------------------------------------------------------------------------------------------------------

    static void straal(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        NpcPayloads.Volg v = volg;
        Minecraft mc = Minecraft.getInstance();
        if (!v.straal() || v.regels().isEmpty() || mc.level == null || mc.player == null || !Vadsrijk.in(mc.player)) {
            return;
        }
        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        float tijd = mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        boolean iets = false;
        for (NpcPayloads.VolgRegel r : v.regels()) {
            if (r.doel().isEmpty()) {
                continue;
            }
            BlockPos d = r.doel().get();
            Vec3 voet = new Vec3(d.getX() + 0.5, d.getY(), d.getZ() + 0.5);
            double hx = voet.x - cam.x, hz = voet.z - cam.z;
            double afstand = Math.sqrt(hx * hx + hz * hz);
            if (afstand < 5) {
                continue;   // (you are there)
            }
            float schaal = 1f;
            if (afstand > STRAAL_VER) {   // too far for the far plane: draw it closer, in the same direction
                schaal = (float) (STRAAL_VER / afstand);
                voet = new Vec3(cam.x + hx * schaal, cam.y + (voet.y - cam.y) * schaal, cam.z + hz * schaal);
            }
            float zicht = (float) Mth.clamp((afstand - 5) / 12.0, 0, 1);   // fades in as you walk away from it
            float breed = (float) Math.max(0.3, Math.min(afstand, STRAAL_VER) * 0.004);
            int kleur = r.hoofd() ? 0xFF8AD0 : 0xFFF27A;
            pose.pushPose();
            pose.translate(voet.x - cam.x, voet.y - cam.y + 2.2, voet.z - cam.z);
            Matrix4f m = pose.last().pose();
            VertexConsumer vc = buffers.getBuffer(RenderTypes.debugQuads());
            float puls = 0.85f + 0.15f * Mth.sin(tijd * 0.08f);
            zuil(vc, m, breed, STRAAL_HOOG * Math.max(schaal, 0.5f), kleur, (int) (120 * zicht * puls));
            zuil(vc, m, breed * 2.4f, STRAAL_HOOG * 0.7f * Math.max(schaal, 0.5f), kleur, (int) (40 * zicht * puls));
            pose.popPose();
            iets = true;
        }
        if (iets) {
            buffers.endBatch(RenderTypes.debugQuads());
        }
    }

    /** A square column from 0 up to h, half-width r, alpha a at the foot fading to 0 at the top (both windings). */
    private static void zuil(VertexConsumer vc, Matrix4f m, float r, float h, int rgb, int a) {
        if (a <= 2) {
            return;
        }
        int onder = (Mth.clamp(a, 0, 255) << 24) | rgb, boven = rgb;
        float[][] hoeken = {{-r, -r}, {r, -r}, {r, r}, {-r, r}};
        for (int i = 0; i < 4; i++) {
            float[] p = hoeken[i], q = hoeken[(i + 1) % 4];
            vc.addVertex(m, p[0], 0, p[1]).setColor(onder);
            vc.addVertex(m, q[0], 0, q[1]).setColor(onder);
            vc.addVertex(m, q[0], h, q[1]).setColor(boven);
            vc.addVertex(m, p[0], h, p[1]).setColor(boven);
            vc.addVertex(m, p[0], h, p[1]).setColor(boven);
            vc.addVertex(m, q[0], h, q[1]).setColor(boven);
            vc.addVertex(m, q[0], 0, q[1]).setColor(onder);
            vc.addVertex(m, p[0], 0, p[1]).setColor(onder);
        }
    }
}
