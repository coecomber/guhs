package nl.juiced.guhs.feature.vadsrijk.huisdieren.client;

import javax.annotation.Nullable;

import org.lwjgl.glfw.GLFW;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.client.GuhRenderTypes;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdier;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierEntity;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierPayloads;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierenRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.client.KernClient;

/**
 * G-PETS, client: the pet key {@link #SCHERM} (Vadsrijk category, rebindable, default P; in the dimension it wins over other Guhs
 * keys and vanilla's social-interactions key on the same key), the pet screen ({@link HuisdierScherm}) and the pet renderer (the
 * boss's own GeckoLib model + texture from its {@link Huisdier}, the pet's own animation file, tiny scale, a pop-in; a silhouette in
 * the screen for pets you don't have yet).
 */
public final class HuisdierClient {
    public static final KeyMapping SCHERM = new KeyMapping("key.guhs.vr_huisdieren", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, KernClient.CATEGORIE);
    /** (fix round 3) Its tab in {@link nl.juiced.guhs.feature.vadsrijk.client.CollectieTabs}. */
    public static final String TAB = "huisdieren";

    private HuisdierClient() {
    }

    /** Looked at from up to 8 blocks (the crosshair ray hits its slightly enlarged box), names shown (F1), not invisible. */
    static boolean bekeken(HuisdierEntity h, float pt) {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.world.entity.Entity cam = mc.getCameraEntity();
        if (cam == null || !Minecraft.renderNames() || h.isInvisible() || h.level() != cam.level()) {
            return false;
        }
        net.minecraft.world.phys.Vec3 oog = cam.getEyePosition(pt);
        net.minecraft.world.phys.Vec3 eind = oog.add(cam.getViewVector(pt).scale(8));
        return h.getBoundingBox().inflate(0.3).clip(oog, eind).isPresent();
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(SCHERM));
        nl.juiced.guhs.feature.vadsrijk.client.CollectieTabs.registreer(TAB, 20, () -> Component.translatable("vadsrijk.huisdieren.titel"),
                HuisdierClient::open);
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(HuisdierenRegistry.HUISDIER.get(), HuisdierRenderer::new));
        // N-EXTRA: "<pet> van <player>" while you look at it, like a vanilla named mob (the pet isn't pickable, so we aim ourselves);
        // never the raw type key (fix round 3): without its server-given name a pet shows nothing
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RenderNameTagEvent.CanRender e) -> {
            if (e.getEntity() instanceof HuisdierEntity h) {
                Component naam = h.getCustomName();
                if (naam != null && !h.voorbeeld && bekeken(h, e.getPartialTick())) {
                    e.setContent(naam);
                    e.setCanRender(net.minecraft.util.TriState.TRUE);
                } else {
                    e.setCanRender(net.minecraft.util.TriState.FALSE);
                }
            }
        });
        HuisdierPayloads.schermOntvanger = s -> Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof HuisdierScherm h) {
                h.update(s.data(), java.util.List.of());
            } else {
                mc.setScreen(new HuisdierScherm(s.data()));
            }
        });
        // in the dimension our key wins over other keys on the same key (other Guhs keys, vanilla's social interactions)
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || !Vadsrijk.in(mc.player)) {
                return;
            }
            for (KeyMapping k : mc.options.keyMappings) {
                boolean guhs = k.getCategory() == nl.juiced.guhs.client.GuhKeys.CATEGORY;
                if (k != SCHERM && (guhs || k == mc.options.keySocialInteractions) && k.same(SCHERM)) {
                    while (k.consumeClick()) {
                        // (swallowed)
                    }
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            boolean binnen = mc.player != null && Vadsrijk.in(mc.player) && mc.screen == null;
            while (SCHERM.consumeClick()) {
                if (binnen) {
                    open();
                }
            }
        });
    }

    /** Asks the server for the pet screen (the key, the button in the mount screen). */
    public static void open() {
        ClientPacketDistributor.sendToServer(new HuisdierPayloads.Doe("open", ""));
    }

    private static final DataTicket<String> SOORT = DataTicket.create("guhs_vr_huisdier_soort", String.class);
    private static final DataTicket<Boolean> DONKER = DataTicket.create("guhs_vr_huisdier_donker", Boolean.class);
    private static final DataTicket<Float> GROEI = DataTicket.create("guhs_vr_huisdier_groei", Float.class);
    /** T-RIJDIER: the pet's synced extra size (Vahoegje grows). */
    private static final DataTicket<Float> GROOTTE = DataTicket.create("guhs_vr_huisdier_grootte", Float.class);

    private static Huisdier soort(GeoRenderState state) {
        Huisdier h = Huisdieren.van(state.getOrDefaultGeckolibData(SOORT, ""));
        return h != null ? h : Huisdieren.KLODDERREUS;
    }

    /** The pet: model/texture/animation/scale from its Huisdier. */
    public static class HuisdierRenderer extends GeoEntityRenderer<HuisdierEntity, LivingEntityRenderState> {
        public HuisdierRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<HuisdierEntity>(Huisdieren.KLODDERREUS.modelId()) {
                @Override
                public Identifier getModelResource(GeoRenderState state) {
                    return soort(state).modelId();
                }

                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return soort(state).textuurId();
                }

                @Override
                public Identifier getAnimationResource(HuisdierEntity e) {
                    Huisdier h = e.soort();
                    return (h != null ? h : Huisdieren.KLODDERREUS).animatieId();
                }
            });
            this.shadowRadius = 0.22f;
            withRenderLayer(new AutoGlowingGeoLayer<HuisdierEntity, Void, LivingEntityRenderState>(this) {
                @Nullable
                @Override
                protected RenderType getRenderType(LivingEntityRenderState state) {
                    Huisdier h = Huisdieren.van(state.getOrDefaultGeckolibData(SOORT, ""));
                    boolean donker = Boolean.TRUE.equals(state.getOrDefaultGeckolibData(DONKER, false));
                    return h != null && h.gloed() != null && !donker ? GuhRenderTypes.eyes(nl.juiced.guhs.Guhs.id(h.gloed())) : null;
                }
            });
        }

        @Override
        public void addRenderData(HuisdierEntity e, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(SOORT, e.soortId());
            state.addGeckolibData(DONKER, e.silhouet);   // (a silhouette doesn't glow)
            float t = e.voorbeeld ? 99 : e.tickCount + partialTick;
            // pop-in: 0.2 -> 1.15 -> 1.0
            float g = t >= 9 ? 1f : t < 6 ? 0.2f + 0.95f * (t / 6f) : 1.15f - 0.15f * ((t - 6) / 3f);
            state.addGeckolibData(GROEI, g);
            state.addGeckolibData(GROOTTE, e.grootte());   // T-RIJDIER
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float w, float h) {
            float g = info.getOrDefaultGeckolibData(GROEI, 1f);
            Huisdier s = Huisdieren.van(info.getOrDefaultGeckolibData(SOORT, ""));
            float k = (s != null ? s.schaal() : 0.4f) * g * info.getOrDefaultGeckolibData(GROOTTE, 1f);
            super.scaleModelForRender(info, w * k, h * k);
        }

        @Override
        public int getRenderColor(HuisdierEntity e, @Nullable Void related, float partialTick) {
            return e.silhouet ? 0xFF3A2C48 : super.getRenderColor(e, related, partialTick);
        }
    }
}
