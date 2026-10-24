package nl.juiced.guhs.feature.grotspellen.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.grotspellen.GrotSpellenFeature;
import nl.juiced.guhs.feature.grotspellen.GrotSpellenPayloads;
import nl.juiced.guhs.feature.grotspellen.VangGlimwormEntity;

/**
 * Client side of the cave games: the glimworms of Glimwormvangen (the safari glimworm's baby model with its glowing tail;
 * the golden ones in gold), the glimworm jar on a player's back ({@link GlimpotLaag}, brighter with every catch), and the
 * clothes that glow in the dark (the glimworm-antennetjes and the paddenstoelhoedje: their bones again, full bright).
 */
public final class GrotSpellenClient {
    /** Players with a jar: entity id -> {stand, the game time it was last heard of}. */
    private static final Map<Integer, long[]> POTJES = new ConcurrentHashMap<>();
    /** A jar not heard of for this long is gone (the server repeats it every 5 seconds). */
    private static final long VERGEET = 160;
    static final DataTicket<Boolean> GOUD = DataTicket.create("guhs_grotspel_goud", Boolean.class);
    private static final Identifier GOUD_TEXTUUR = Guhs.id("textures/entity/grotspellen/glimworm_goud.png");
    private static final GuhClothes[] GLOEIEND = {GuhClothes.GLIM_ANTENNETJES, GuhClothes.LICHTJES_PADDENSTOELHOEDJE};

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(GrotSpellenFeature.VANGGLIMWORM.get(), context -> {
            GeoEntityRenderer<VangGlimwormEntity, LivingEntityRenderState> r = new GeoEntityRenderer<VangGlimwormEntity, LivingEntityRenderState>(context,
                    new DefaultedEntityGeoModel<VangGlimwormEntity>(Guhs.id("safari/glimworm_baby")) {
                        @Override
                        public Identifier getAnimationResource(VangGlimwormEntity worm) {
                            return buildFormattedAnimationPath(Guhs.id("safari/glimworm"));
                        }

                        @Override
                        public void addAdditionalStateData(VangGlimwormEntity worm, @Nullable Object related, GeoRenderState state) {
                            state.addGeckolibData(GOUD, worm.isGoud());
                        }

                        @Override
                        public Identifier getTextureResource(GeoRenderState state) {
                            Boolean goud = state.getGeckolibData(GOUD);
                            return goud != null && goud ? GOUD_TEXTUUR : super.getTextureResource(state);
                        }
                    }) {
                {
                    this.shadowRadius = 0.0f;
                }
            };
            r.withRenderLayer(new AutoGlowingGeoLayer<>(r));
            return r;
        }));
        modBus.addListener(GrotSpellenClient::renderStates);
        modBus.addListener(GrotSpellenClient::layers);
        GrotSpellenPayloads.potjeOntvanger = p -> {
            Minecraft mc = Minecraft.getInstance();
            if (p.stand() < 0) {
                POTJES.remove(p.speler());
            } else if (mc.level != null) {
                POTJES.put(p.speler(), new long[]{p.stand(), mc.level.getGameTime()});
            }
        };
        // the glowing clothes: their bones once more, full bright
        GuhRenderer.hook((guh, partialTick, frame) -> {
            for (GuhClothes c : GLOEIEND) {
                if (frame.clothes.get(c.slot) == c) {
                    frame.pass(RenderTypes.entityTranslucent(c.texture()), 0xFFFFFFFF, true, c::shows);
                }
            }
        });
    }

    /** How bright this player's jar is (0-4), or -1: no jar. */
    static int stand(Player player) {
        long[] p = POTJES.get(player.getId());
        Minecraft mc = Minecraft.getInstance();
        if (p == null || mc.level == null) {
            return -1;
        }
        if (Math.abs(mc.level.getGameTime() - p[1]) > VERGEET) {
            POTJES.remove(player.getId());
            return -1;
        }
        return (int) p[0];
    }

    private static void layers(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType skin : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(skin);
            if (renderer != null) {
                renderer.addLayer(new GlimpotLaag(renderer));
            }
        }
    }

    private static void renderStates(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                if (avatar instanceof Player player) {
                    GlimpotLaag.extract(player, state);
                }
            }
        });
    }

    private GrotSpellenClient() {
    }
}
