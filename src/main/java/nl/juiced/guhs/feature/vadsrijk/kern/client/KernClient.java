package nl.juiced.guhs.feature.vadsrijk.kern.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.KernPayloads;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import org.lwjgl.glfw.GLFW;

/**
 * Kern's client: the skill keys (category "Vadsrijk", R/V/G/Z, only active in the dimension), the renderers of kern's
 * entities, the HUD ({@link RijkHud}), scenes and title cards ({@link SceneSpeler}).
 */
public final class KernClient {
    public static final KeyMapping.Category CATEGORIE = new KeyMapping.Category(Guhs.id("vadsrijk"));
    public static final KeyMapping[] SKILLS = {
            new KeyMapping("key.guhs.vr_r", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORIE),
            new KeyMapping("key.guhs.vr_v", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORIE),
            new KeyMapping("key.guhs.vr_g", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORIE),
            new KeyMapping("key.guhs.vr_z", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORIE)};

    private KernClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            e.registerCategory(CATEGORIE);
            for (KeyMapping k : SKILLS) {
                e.register(k);
            }
        });
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerEntityRenderer(KernRegistry.PROEFKLODDER.get(), KlodderRenderer::new);
            e.registerEntityRenderer(KernRegistry.PROEFKLODDERTJE.get(), KlodderRenderer::new);
            e.registerEntityRenderer(KernRegistry.KLODDERREUS.get(), KlodderRenderer::new);
        });
        // In the dimension the skill keys win: other Guhs keys on the same key (maag G, snuffel R/V/Z) are swallowed first.
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || !Vadsrijk.in(mc.player)) {
                return;
            }
            for (KeyMapping k : mc.options.keyMappings) {
                if (k.getCategory() != nl.juiced.guhs.client.GuhKeys.CATEGORY) {
                    continue;
                }
                for (KeyMapping s : SKILLS) {
                    if (k != s && k.same(s)) {
                        while (k.consumeClick()) {
                            // (swallowed)
                        }
                    }
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            boolean binnen = mc.player != null && Vadsrijk.in(mc.player);
            for (int i = 0; i < SKILLS.length; i++) {
                while (SKILLS[i].consumeClick()) {
                    if (binnen && mc.screen == null) {
                        ClientPacketDistributor.sendToServer(new KernPayloads.Skill(i));
                    }
                }
            }
        });
        RijkHud.init(modBus);
        SceneSpeler.init();
    }
}
