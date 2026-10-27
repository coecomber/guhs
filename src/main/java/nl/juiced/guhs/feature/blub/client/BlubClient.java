package nl.juiced.guhs.feature.blub.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderTypes;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.feature.blub.BlubFeature;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.blub.BlubMenu;
import nl.juiced.guhs.feature.blub.BlubTrekking;

/**
 * 1.8.0, the Blub-guh on screen (model, animations and textures from tools/features/blub_modellen.py). Its Blub-Guh-Meter
 * and soort decide, per Blub, which bones show ({@link #zichtbaar}: worked out once per soort x stage and kept) and which
 * texture it wears ({@code textures/entity/blubguh/<soort>_<tint>_<stap>.png}, stap p / b / the meter rounded down to 5).
 * Its ears (or antennae) show how it feels (up / flat / curled). Two extra passes: the strikje (white in the texture,
 * tinted with its dye colour) and the blue Blub's soft glow. Also opens the Blub's menu ({@link BlubMenuScreen}).
 */
public final class BlubClient {
    /** This frame's look of a Blub (extract time, values only). */
    record Look(Identifier textuur, String[] verborgen, int emotie, int strik, boolean gloeit) {
    }

    static final DataTicket<Look> LOOK = DataTicket.create("guhs_blub_look", Look.class);
    private static final Identifier GLOED = Guhs.id("textures/entity/blubguh/blauw_gloed.png");

    /** The switchable bones (the same list as blub_modellen.SCHAKEL). */
    static final String[] SCHAKEL = {"lijf", "lijf_zacht", "oor_l", "oor_r", "guhoortje_l", "guhoortje_r", "guhoor_l", "guhoor_r",
            "weide_sprieten", "weide_hengel", "schild_vleugel_l", "schild_vleugel_r", "schild_sprieten", "ravijn_spriet_l",
            "ravijn_spriet_r", "ravijn_vin_l", "ravijn_vin_r", "zwam_gewei", "strik"};
    /** The bones that move with its mood (whichever of them show). */
    private static final String[] OREN = {"oor_l", "oor_r", "guhoortje_l", "guhoortje_r", "guhoor_l", "guhoor_r", "ravijn_spriet_l", "ravijn_spriet_r"};
    /** soort x stage (0..6) -> the bones that are hidden (worked out once). */
    private static final String[][] VERBORGEN = new String[BlubTrekking.Soort.values().length * 7][];
    /** soort x tint x stap -> the texture (made once). */
    private static final Identifier[][][] TEXTUREN = new Identifier[BlubTrekking.Soort.values().length][3][19];

    private BlubClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(BlubClient::renderers);
        BlubMenu.menuOpener = BlubClient::openMenu;
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BlubFeature.BLUBGUH.get(), BlubRenderer::new);
    }

    private static void openMenu(BlubMenu.MenuOpen p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getEntity(p.blub()) instanceof BlubGuhEntity blub) {
            mc.setScreen(new BlubMenuScreen(blub, p.record()));
        }
    }

    /** Does this bone show for this soort at this meter? (the same rules as tools/features/blub.py zichtbaar) */
    public static boolean zichtbaar(String bone, BlubTrekking.Soort soort, int meter) {
        int stap = BlubTrekking.stap(meter);
        boolean blauw = soort == BlubTrekking.Soort.BLAUW, weide = soort == BlubTrekking.Soort.WEIDE,
                schild = soort == BlubTrekking.Soort.SCHILD, ravijn = soort == BlubTrekking.Soort.RAVIJN, zwam = soort == BlubTrekking.Soort.ZWAM;
        return switch (bone) {
            case "lijf" -> stap < 4;
            case "lijf_zacht" -> stap >= 4;
            case "oor_l", "oor_r" -> blauw && stap < 6;
            case "guhoortje_l", "guhoortje_r" -> stap == 5 && (weide || schild || zwam);
            case "guhoor_l", "guhoor_r" -> stap == 6;
            case "weide_sprieten", "weide_hengel" -> weide;
            case "schild_vleugel_l", "schild_vleugel_r", "schild_sprieten" -> schild;
            case "ravijn_spriet_l", "ravijn_spriet_r" -> ravijn && stap < 6;
            case "ravijn_vin_l", "ravijn_vin_r" -> ravijn;
            case "zwam_gewei" -> zwam;
            case "strik" -> false;                          // (its own pass)
            default -> true;
        };
    }

    static String[] verborgen(BlubTrekking.Soort soort, int meter) {
        int key = soort.ordinal() * 7 + BlubTrekking.stap(meter);
        String[] v = VERBORGEN[key];
        if (v == null) {
            List<String> l = new ArrayList<>();
            for (String b : SCHAKEL) {
                if (!zichtbaar(b, soort, meter)) {
                    l.add(b);
                }
            }
            VERBORGEN[key] = v = l.toArray(String[]::new);
        }
        return v;
    }

    /** p (0), b (1-19), or the meter rounded down to 5 (20..100): index 0..18. */
    static int stapIndex(int meter) {
        if (meter <= 0) {
            return 0;
        }
        if (meter < 20) {
            return 1;
        }
        return 2 + (Math.min(100, meter) / 5 - 4);
    }

    static Identifier textuur(BlubTrekking.Soort soort, int tint, int meter) {
        int t = Math.floorMod(tint, 3), i = stapIndex(meter);
        Identifier id = TEXTUREN[soort.ordinal()][t][i];
        if (id == null) {
            String stap = i == 0 ? "p" : i == 1 ? "b" : Integer.toString((i - 2) * 5 + 20);
            TEXTUREN[soort.ordinal()][t][i] = id = Guhs.id("textures/entity/blubguh/" + soort.id() + "_" + t + "_" + stap + ".png");
        }
        return id;
    }

    public static class BlubRenderer extends GeoEntityRenderer<BlubGuhEntity, LivingEntityRenderState> {
        public BlubRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<BlubGuhEntity>(Guhs.id("blubguh")) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    Look look = state.getGeckolibData(LOOK);
                    return look == null ? super.getTextureResource(state) : look.textuur();
                }
            });
            this.shadowRadius = 0.32f;
            withRenderLayer(new StrikLaag(this));
            withRenderLayer(new GloedLaag(this));
        }

        @Override
        public void addRenderData(BlubGuhEntity blub, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            BlubTrekking.Soort soort = blub.blubSoort();
            int meter = blub.meter();
            state.addGeckolibData(LOOK, new Look(textuur(soort, blub.tint(), meter), verborgen(soort, meter), blub.emotie(), blub.strik(),
                    soort == BlubTrekking.Soort.BLAUW));
        }

        /** Babyblubjes are small (the size dice are the entity's scale attribute, drawn by GeckoLib itself). */
        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            float age = info.renderState().ageScale;
            super.scaleModelForRender(info, widthScale * age, heightScale * age);
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            Look look = info.getGeckolibData(LOOK);
            if (look == null) {
                return;
            }
            for (String b : look.verborgen()) {
                bones.ifPresent(b, s -> s.skipRender(true).skipChildrenRender(true));
            }
            if (look.emotie() == BlubGuhEntity.PLAT) {
                for (String b : OREN) {
                    bones.ifPresent(b, s -> s.setRotX(s.getRotX() + 1.0f));
                }
            } else if (look.emotie() == BlubGuhEntity.KRUL) {
                for (String b : OREN) {
                    float kant = b.endsWith("_l") ? -1f : 1f;
                    bones.ifPresent(b, s -> s.setRotZ(s.getRotZ() + 0.4f * kant).setRotX(s.getRotX() - 0.25f));
                }
            }
        }
    }

    /** The strikje: only its bone, in its dye colour (the texture's strikje is white). */
    static class StrikLaag extends GeoRenderLayer<BlubGuhEntity, Void, LivingEntityRenderState> {
        StrikLaag(GeoRenderer<BlubGuhEntity, Void, LivingEntityRenderState> renderer) {
            super(renderer);
        }

        @Override
        public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
            Look look = info.getGeckolibData(LOOK);
            if (look == null || look.strik() < 0 || !info.willRender()) {
                return;
            }
            int kleur = 0xFF000000 | (DyeColor.byId(look.strik()).getTextureDiffuseColor() & 0xFFFFFF);
            GuhRenderer.submitPass(info, collector.order(1), RenderTypes.entityCutout(look.textuur()), info.packedLight(), kleur,
                    name -> name.equals("strik"));
        }
    }

    /** The blue Blub glows a little (its body, full bright, additive). */
    static class GloedLaag extends GeoRenderLayer<BlubGuhEntity, Void, LivingEntityRenderState> {
        GloedLaag(GeoRenderer<BlubGuhEntity, Void, LivingEntityRenderState> renderer) {
            super(renderer);
        }

        @Override
        public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
            Look look = info.getGeckolibData(LOOK);
            if (look == null || !look.gloeit() || !info.willRender()) {
                return;
            }
            getRenderer().submitRenderTasks(info, collector.order(1), GuhRenderTypes.eyes(GLOED));
        }
    }
}
