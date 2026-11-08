package nl.juiced.guhs.feature.vadsrijk.zone6.raid.client;

import javax.annotation.Nullable;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone6.raid.Ringeling;
import nl.juiced.guhs.feature.vadsrijk.zone6.raid.Spiegelvizier;
import nl.juiced.guhs.feature.vadsrijk.zone6.raid.Sterrenbeeld;
import nl.juiced.guhs.feature.vadsrijk.zone6.raid.Z6RaidRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone6.raid.Zandwachter;

/**
 * U-Z6RAID, the client: the renderers (GeckoLib models from tools/features/vadsrijk_u_z6raid.py; textures in
 * textures/vadsrijk/z6raid/) and the raid HUD ({@link Z6RaidHud}).
 * <ul>
 *   <li>Zandwachter: the hourglass glows (glowmask);</li>
 *   <li>Spiegelvizier: the real one and his images look the SAME (that is the puzzle); the clones are the pale "kloon" texture;</li>
 *   <li>Sterrenbeelden: grey while gedoofd, bright ("_blij") once out/cheered up; the glowmask makes the star knots shine;</li>
 *   <li>Ringeling: "_blij" once cheered up; the ten toe rings glow;</li>
 *   <li>objects: sand pillar, turning mirror, star beacon (lit = "_aan" + glow), star shard.</li>
 * </ul>
 */
public final class Z6RaidClient {
    private static final DataTicket<Integer> GETAL = DataTicket.create("guhs_vr_z6_getal", Integer.class);

    private Z6RaidClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(Z6RaidClient::renderers);
        Z6RaidHud.init();
    }

    static Identifier tex(String naam) {
        return Guhs.id("textures/vadsrijk/z6raid/" + naam + ".png");
    }

    static class Gewoon<T extends LivingEntity & com.geckolib.animatable.GeoEntity> extends GeoEntityRenderer<T, LivingEntityRenderState> {
        Gewoon(EntityRendererProvider.Context ctx, com.geckolib.model.GeoModel<T> m, float schaal, boolean gloed) {
            super(ctx, m);
            withScale(schaal);
            if (gloed) {
                withRenderLayer(new AutoGlowingGeoLayer<T, Void, LivingEntityRenderState>(this));
            }
        }
    }

    /** A model whose texture is picked by a number the renderer puts in the state (0 = base, 1 = second texture...). */
    private static <T extends LivingEntity & com.geckolib.animatable.GeoEntity> DefaultedEntityGeoModel<T> model(String id, String... texturen) {
        return new DefaultedEntityGeoModel<T>(Guhs.id(id)) {
            @Override
            public Identifier getTextureResource(GeoRenderState state) {
                int i = state.getOrDefaultGeckolibData(GETAL, 0);
                return tex(texturen[Math.max(0, Math.min(texturen.length - 1, i))]);
            }
        };
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(Z6RaidRegistry.ZANDWACHTER.get(), ctx -> new Gewoon<Zandwachter>(ctx, model("vr_z6raid_baas_zandwachter", "zandwachter", "zandwachter_blij"),
                1.0f, true) {
            @Override
            public void addRenderData(Zandwachter b, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
                state.addGeckolibData(GETAL, b.geplofd() ? 1 : 0);
            }
        });
        e.registerEntityRenderer(Z6RaidRegistry.SPIEGELVIZIER.get(), ctx -> new Gewoon<Spiegelvizier>(ctx,
                model("vr_z6raid_baas_spiegelvizier", "spiegelvizier", "spiegelkloon", "spiegelvizier_blij"), 1.05f, true) {
            @Override
            public void addRenderData(Spiegelvizier b, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
                state.addGeckolibData(GETAL, b.rol() == Spiegelvizier.KLOON ? 1 : b.geplofd() ? 2 : 0);
            }
        });
        for (var t : java.util.List.of(Z6RaidRegistry.STER_KNUFFEL, Z6RaidRegistry.STER_BULTGUH, Z6RaidRegistry.STER_PLUIS)) {
            String naam = t == Z6RaidRegistry.STER_KNUFFEL ? "ster_knuffel" : t == Z6RaidRegistry.STER_BULTGUH ? "ster_bultguh" : "ster_pluis";
            e.registerEntityRenderer(t.get(), ctx -> new Gewoon<Sterrenbeeld>(ctx, model("vr_z6raid_baas_" + naam, naam, naam + "_blij"), 1.0f, true) {
                @Override
                public void addRenderData(Sterrenbeeld b, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
                    state.addGeckolibData(GETAL, b.uit() || b.geplofd() ? 1 : 0);
                }
            });
        }
        e.registerEntityRenderer(Z6RaidRegistry.RINGELING.get(), ctx -> new Gewoon<Ringeling>(ctx, model("vr_z6raid_baas_ringeling", "ringeling", "ringeling_blij"), 1.1f,
                true) {
            @Override
            public void addRenderData(Ringeling b, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
                state.addGeckolibData(GETAL, b.geplofd() ? 1 : 0);
            }
        });
        e.registerEntityRenderer(Z6RaidRegistry.ZANDPILAAR.get(), ctx -> new Gewoon<Z3Doel>(ctx, model("vr_z6raid_zandpilaar", "zandpilaar"), 1.0f, false));
        e.registerEntityRenderer(Z6RaidRegistry.DRAAISPIEGEL.get(), ctx -> new Gewoon<Z3Doel>(ctx, model("vr_z6raid_draaispiegel", "draaispiegel"), 1.0f, true));
        e.registerEntityRenderer(Z6RaidRegistry.STERRENSCHERF.get(), ctx -> new Gewoon<Z3Doel>(ctx, model("vr_z6raid_sterrenscherf", "sterrenscherf"), 1.0f, true));
        e.registerEntityRenderer(Z6RaidRegistry.STERRENBAKEN.get(), ctx -> new Gewoon<Z3Doel>(ctx, model("vr_z6raid_sterrenbaken", "sterrenbaken", "sterrenbaken_aan"), 1.0f,
                true) {
            @Override
            public void addRenderData(Z3Doel d, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
                state.addGeckolibData(GETAL, d.stand());
            }
        });
    }
}
