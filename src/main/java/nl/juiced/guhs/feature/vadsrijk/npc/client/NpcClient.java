package nl.juiced.guhs.feature.vadsrijk.npc.client;

import java.util.HashMap;
import java.util.Map;

import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.client.KernClient;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcMarkers;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcPayloads;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRegistry;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;

/** Het Vadsrijk, slice "npc", the client: the NPC guh renderer (the guh model + clothes), the "!"/"?" markers, the questlog (J). */
public final class NpcClient {
    public static final KeyMapping QUESTLOG = new KeyMapping("key.guhs.vr_questlog", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, KernClient.CATEGORIE);
    /** entity id -> marker (NpcMarkers.*), for the NPCs near you. */
    private static final Map<Integer, Byte> MARKERS = new HashMap<>();

    private NpcClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(NpcRegistry.GUH.get(),
                nl.juiced.guhs.feature.vadsrijk.zone5.wolkwal.client.WolkwachtGuhRenderer::new));   // T-WOLKWAL: GuhRenderer + the Wolkwacht knights
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(QUESTLOG));
        VolgHud.init(modBus);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            while (QUESTLOG.consumeClick()) {
                if (mc.player != null && Vadsrijk.in(mc.player) && mc.screen == null) {
                    ClientPacketDistributor.sendToServer(new NpcPayloads.LogVraag());
                }
            }
        });
        NpcPayloads.markerOntvanger = m -> Minecraft.getInstance().execute(() -> {
            MARKERS.clear();
            for (int i = 0; i < m.ids().length && i < m.soort().length; i++) {
                MARKERS.put(m.ids()[i], m.soort()[i]);
            }
        });
        NpcPayloads.reiskaartOntvanger = m -> Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new ReiskaartScherm(m)));
        NpcPayloads.logOntvanger = m -> Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof QuestlogScherm s) {
                s.update(m.data());
            } else if (m.open()) {   // (J only sends with no screen open; the dev command may come from the chat screen)
                mc.setScreen(new QuestlogScherm(m.data()));
            }
        });
        GuhRenderer.hook((guh, partialTick, frame) -> {
            if (!(guh instanceof RijkGuh)) {
                return;
            }
            Byte m = MARKERS.get(guh.getId());
            if (m == null || m == NpcMarkers.GEEN) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            Quaternionf rot = new Quaternionf(mc.gameRenderer.getMainCamera().rotation());
            boolean vraag = m == NpcMarkers.INLEVEREN || m == NpcMarkers.PRATEN;
            int kleur = switch (m) {
                case NpcMarkers.HOOFD -> 0xFFFFB21A;
                case NpcMarkers.INLEVEREN -> 0xFFFFD84A;
                case NpcMarkers.PRATEN -> 0xFFE8E0FF;
                default -> 0xFFFFF27A;
            };
            FormattedCharSequence t = Component.literal(vraag ? "?" : "!").withStyle(ChatFormatting.BOLD).getVisualOrderText();
            float w = mc.font.width(t);
            float hoog = guh.getBbHeight() + 1.05f + (float) Math.sin((guh.tickCount + partialTick) * 0.12f) * 0.07f;
            frame.extra((pose, collector, light) -> {
                pose.translate(0, hoog, 0);
                pose.mulPose(rot);
                pose.scale(0.07f, -0.07f, 0.07f);
                collector.submitText(pose, -w / 2f, 0f, t, false, Font.DisplayMode.SEE_THROUGH, LightCoordsUtil.FULL_BRIGHT, kleur, 0x40000000, 0);
            });
        });
    }
}
