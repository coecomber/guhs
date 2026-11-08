package nl.juiced.guhs.feature.vadsrijk.zone3.bazen.client;

import javax.annotation.Nullable;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Vahoegnir;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Figurant;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Hulpje;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.hart.Nestwachter;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.piepvel.Piepvel;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.werf.Brokkel;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.werf.Roquefort;

/**
 * Zone-3 bazen, the client: GeckoLib renderers for the bosses, their mechanic objects and adds, and the props
 * ({@link Z3Figurant}: any model/texture/animation, with hidden bones). Graaf Piepvel's cape loses a panel per freed cage
 * (and he is only his glowing eyes in the dark away from the light), Brokkel's plates come off, Roquefort's umbrella only opens
 * when he flees, the Nestwachter turns pink, Vahoegnir goes from grey to pink with every splinter.
 */
public final class Z3BazenClient {
    private static final DataTicket<Integer> GETAL = DataTicket.create("guhs_vr_z3_getal", Integer.class);
    private static final DataTicket<Boolean> VLAG = DataTicket.create("guhs_vr_z3_vlag", Boolean.class);
    private static final DataTicket<String> F_MODEL = DataTicket.create("guhs_vr_z3_f_model", String.class);
    private static final DataTicket<String> F_TEX = DataTicket.create("guhs_vr_z3_f_tex", String.class);
    private static final DataTicket<String> F_VERBORGEN = DataTicket.create("guhs_vr_z3_f_verborgen", String.class);
    private static final DataTicket<Float> F_SCHAAL = DataTicket.create("guhs_vr_z3_f_schaal", Float.class);

    private Z3BazenClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(Z3BazenClient::renderers);
    }

    static Identifier tex(String naam) {
        return Guhs.id("textures/vadsrijk/z3bazen/" + naam + ".png");
    }

    private static <T extends LivingEntity & com.geckolib.animatable.GeoEntity> DefaultedEntityGeoModel<T> model(String id, String textuur) {
        return new DefaultedEntityGeoModel<T>(Guhs.id(id)) {
            @Override
            public Identifier getTextureResource(GeoRenderState state) {
                return tex(textuur);
            }
        };
    }

    /** A plain boss/prop renderer: model + texture + scale (+ an optional glow layer from texture_glowmask). */
    static class Gewoon<T extends LivingEntity & com.geckolib.animatable.GeoEntity> extends GeoEntityRenderer<T, LivingEntityRenderState> {
        private final boolean hoofd;

        Gewoon(EntityRendererProvider.Context ctx, com.geckolib.model.GeoModel<T> m, float schaal, boolean gloed, boolean hoofd) {
            super(ctx, m);
            withScale(schaal);
            this.hoofd = hoofd;
            if (gloed) {
                withRenderLayer(new AutoGlowingGeoLayer<T, Void, LivingEntityRenderState>(this));
            }
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            if (hoofd) {
                DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
            }
        }
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(Z3BazenRegistry.KRUINROOFVOGEL.get(), ctx -> new Gewoon<>(ctx, model("vr_z3_baas_kruinroofvogel", "kruinroofvogel"), 1.5f, true, false));
        e.registerEntityRenderer(Z3BazenRegistry.TALEGGIO.get(), ctx -> new Gewoon<>(ctx, model("vr_z3_baas_taleggio", "taleggio"), 1.9f, false, true));
        e.registerEntityRenderer(Z3BazenRegistry.BROKKEL.get(), ctx -> new Gewoon<Brokkel>(ctx, model("vr_z3_baas_brokkel", "brokkel"), 1.9f, false, true) {
            @Override
            public void addRenderData(Brokkel b, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
                state.addGeckolibData(GETAL, b.platen());
            }

            @Override
            public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
                super.adjustModelBonesForRender(info, bones);
                int platen = info.getOrDefaultGeckolibData(GETAL, 4);
                for (int i = platen; i < 4; i++) {
                    bones.ifPresent("plaat_" + (i + 1), s -> s.skipRender(true));
                }
            }
        });
        e.registerEntityRenderer(Z3BazenRegistry.ROQUEFORT.get(), ctx -> new Gewoon<Roquefort>(ctx, model("vr_z3_baas_roquefort", "roquefort"), 1.9f, false, true) {
            @Override
            public void addRenderData(Roquefort b, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
                state.addGeckolibData(VLAG, b.paraplu());
            }

            @Override
            public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
                super.adjustModelBonesForRender(info, bones);
                if (!Boolean.TRUE.equals(info.getOrDefaultGeckolibData(VLAG, false))) {
                    bones.ifPresent("paraplu", s -> s.skipRender(true).skipChildrenRender(true));
                }
            }
        });
        e.registerEntityRenderer(Z3BazenRegistry.PIEPVEL.get(), ctx -> new Gewoon<Piepvel>(ctx, model("vr_z3_baas_piepvel", "piepvel"), 2.0f, false, true) {
            @Override
            public void addRenderData(Piepvel b, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
                state.addGeckolibData(GETAL, b.cape());
                state.addGeckolibData(VLAG, b.zichtbaar());
            }

            @Override
            public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
                super.adjustModelBonesForRender(info, bones);
                if (!Boolean.TRUE.equals(info.getOrDefaultGeckolibData(VLAG, true))) {
                    bones.ifPresent("root", s -> s.skipRender(true).skipChildrenRender(true));   // in the dark: only his eyes (server particles)
                    return;
                }
                int cape = info.getOrDefaultGeckolibData(GETAL, 0b1111);
                if (cape == 0) {
                    bones.ifPresent("cape", s -> s.skipRender(true).skipChildrenRender(true));
                    return;
                }
                for (int i = 0; i < 4; i++) {
                    if ((cape & (1 << i)) == 0) {
                        bones.ifPresent("cape_" + (i + 1), s -> s.skipRender(true).skipChildrenRender(true));
                    }
                }
            }
        });
        e.registerEntityRenderer(Z3BazenRegistry.BLEU.get(), ctx -> new Gewoon<>(ctx, model("vr_z3_baas_bleu", "bleu"), 1.9f, false, true));
        e.registerEntityRenderer(Z3BazenRegistry.WORTELWURGER.get(), ctx -> new Gewoon<>(ctx, model("vr_z3_baas_wortelwurger", "wortelwurger"), 1.4f, true, false));
        e.registerEntityRenderer(Z3BazenRegistry.NESTWACHTER.get(), ctx -> {
            var r = new Gewoon<Nestwachter>(ctx, new DefaultedEntityGeoModel<Nestwachter>(Guhs.id("vr_z3_baas_nestwachter")) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return tex(Boolean.TRUE.equals(state.getOrDefaultGeckolibData(VLAG, false)) ? "nestwachter_roze" : "nestwachter");
                }
            }, 1.3f, true, true) {
                @Override
                public void addRenderData(Nestwachter b, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
                    state.addGeckolibData(VLAG, b.rozeWeer());
                }
            };
            return r;
        });
        e.registerEntityRenderer(Z3BazenRegistry.VAHOEGNIR.get(), ctx -> new Gewoon<Vahoegnir>(ctx, new DefaultedEntityGeoModel<Vahoegnir>(Guhs.id("vadsrijk/wezens/draak")) {
            @Override
            public Identifier getTextureResource(GeoRenderState state) {
                return tex("vahoegnir_" + Mth.clamp(state.getOrDefaultGeckolibData(GETAL, 0), 0, 5));
            }

            @Override
            public Identifier getAnimationResource(Vahoegnir v) {
                return Guhs.id("entity/vr_z3_baas_vahoegnir");
            }
        }, 1.6f, true, false) {   // o-draak: glowing purple eyes via vahoegnir_<n>_glowmask (step 5 = pink head, no glow)
            @Override
            public void addRenderData(Vahoegnir b, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
                state.addGeckolibData(GETAL, b.splinters());
            }

            @Override
            protected AABB getBoundingBoxForCulling(Vahoegnir d) {
                return d.getBoundingBox().inflate(10, 6, 10);
            }
        });
        // mechanic objects
        e.registerEntityRenderer(Z3BazenRegistry.KOOI.get(), ctx -> new Gewoon<Z3Doel>(ctx, model("vr_z3_baas_kooi", "kooi"), 1.0f, false, false));
        e.registerEntityRenderer(Z3BazenRegistry.BALLON.get(), ctx -> new Gewoon<Z3Doel>(ctx, model("vr_z3_baas_ballon", "ballon"), 1.5f, false, false));
        e.registerEntityRenderer(Z3BazenRegistry.VOEDINGSWORTEL.get(), ctx -> new Gewoon<Z3Doel>(ctx, model("vr_z3_baas_voedingswortel", "voedingswortel"), 1.2f, true, false));
        e.registerEntityRenderer(Z3BazenRegistry.KANON.get(), ctx -> new Gewoon<Z3Doel>(ctx, model("vr_z3_baas_kanon", "kanon"), 1.5f, false, false));
        e.registerEntityRenderer(Z3BazenRegistry.KETEL.get(), ctx -> new Gewoon<Z3Doel>(ctx, model("vr_z3_baas_ketel", "ketel"), 1.4f, false, false));
        e.registerEntityRenderer(Z3BazenRegistry.KOLOS.get(), ctx -> new Gewoon<Z3Doel>(ctx, model("vr_z3_baas_schimmelkolos", "schimmelkolos"), 1.4f, false, false));
        // adds
        e.registerEntityRenderer(Z3BazenRegistry.MATROOS.get(), ctx -> new Gewoon<Z3Hulpje>(ctx, new DefaultedEntityGeoModel<Z3Hulpje>(Guhs.id("mika")) {
            @Override
            public Identifier getTextureResource(GeoRenderState state) {
                return tex("matroos");
            }
        }.withAltAnimations(Guhs.id("guh")), 1.2f, false, true));
        e.registerEntityRenderer(Z3BazenRegistry.SCHIMMELBOL.get(), ctx -> new Gewoon<Z3Hulpje>(ctx, model("vr_z3_baas_schimmelbol", "schimmelbol"), 1.2f, false, false));
        e.registerEntityRenderer(Z3BazenRegistry.KLODDERTJE.get(), ctx -> new Gewoon<Z3Hulpje>(ctx, model("vr_z3_baas_kloddertje", "kloddertje"), 1.2f, false, false));
        // props
        e.registerEntityRenderer(Z3BazenRegistry.FIGURANT.get(), FigurantRenderer::new);
    }

    /** Any model with any texture/animation (synced from the server), scaled, with hidden bones. */
    static class FigurantRenderer extends GeoEntityRenderer<Z3Figurant, EntityRenderState> {
        /** U-GROOTTE (FPS): per-frame work of up to 160 props was ~5 string/Identifier builds + a split + a variant scan each. */
        private static final java.util.Map<String, Identifier> ID = new java.util.concurrent.ConcurrentHashMap<>();
        private static final java.util.Map<String, String[]> VERBORGEN = new java.util.concurrent.ConcurrentHashMap<>();
        private static final java.util.Map<String, nl.juiced.guhs.entity.GuhVariant> VARIANT = new java.util.concurrent.ConcurrentHashMap<>();
        private static final String[] ZADEL = {"saddle", "armor_iron", "armor_iron_body", "armor_diamond", "armor_diamond_body", "armor_netherite", "armor_netherite_body"};

        FigurantRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new DefaultedEntityGeoModel<Z3Figurant>(Guhs.id("pieppiepmuisje")) {
                @Override
                public Identifier getModelResource(GeoRenderState state) {
                    return ID.computeIfAbsent("entity/" + state.getOrDefaultGeckolibData(F_MODEL, "pieppiepmuisje"), Guhs::id);   // U-GROOTTE: cached
                }

                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return ID.computeIfAbsent(state.getOrDefaultGeckolibData(F_TEX, "textures/entity/pieppiepmuisje.png"), Guhs::id);
                }

                @Override
                public Identifier getAnimationResource(Z3Figurant f) {
                    return ID.computeIfAbsent("entity/" + f.animBestand(), Guhs::id);
                }
            });
            this.shadowRadius = 0.2f;
        }

        @Override
        public void addRenderData(Z3Figurant f, @Nullable Void related, EntityRenderState state, float partialTick) {
            state.addGeckolibData(F_MODEL, f.model());
            state.addGeckolibData(F_TEX, f.textuur());
            state.addGeckolibData(F_VERBORGEN, f.verborgen());
            state.addGeckolibData(F_SCHAAL, f.schaal());
            state.addGeckolibData(DataTickets.ENTITY_BODY_YAW, Mth.rotLerp(partialTick, f.yRotO, f.getYRot()));
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<EntityRenderState> info, float w, float h) {
            float k = info.getOrDefaultGeckolibData(F_SCHAAL, 1f);
            super.scaleModelForRender(info, w * k, h * k);
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> info, BoneSnapshots bones) {
            // U-Z5: a prop on the full guh model showed ALL its saddle/armour/variant/outfit bones at once (the "bugged" townsfolk
            // of Hoogvads, the zone-3 riders): hide them like GuhRenderer does for a plain guh of the texture's variant.
            if ("guh".equals(info.getOrDefaultGeckolibData(F_MODEL, ""))) {
                gewoneGuh(info, bones, info.getOrDefaultGeckolibData(F_TEX, ""));
            }
            String v = info.getOrDefaultGeckolibData(F_VERBORGEN, "");
            if (!v.isEmpty()) {
                for (String b : VERBORGEN.computeIfAbsent(v, k -> java.util.Arrays.stream(k.split(",")).map(String::trim).toArray(String[]::new))) {
                    bones.ifPresent(b, s -> s.skipRender(true).skipChildrenRender(true));
                }
            }
        }

        /** U-Z5: the guh model as a plain guh of the variant whose fur this texture is (no saddle, armour or clothes). */
        private static void gewoneGuh(RenderPassInfo<EntityRenderState> info, BoneSnapshots bones, String tex) {
            nl.juiced.guhs.entity.GuhVariant variant = VARIANT.computeIfAbsent(tex, t -> {
                for (nl.juiced.guhs.entity.GuhVariant g : nl.juiced.guhs.entity.GuhVariant.values()) {
                    if (g != nl.juiced.guhs.entity.GuhVariant.NORMAL && g.texture().getPath().equals(t)) return g;
                }
                return nl.juiced.guhs.entity.GuhVariant.NORMAL;
            });
            for (String b : ZADEL) {
                bones.ifPresent(b, s -> s.skipRender(true).skipChildrenRender(true));
            }
            nl.juiced.guhs.client.GuhBoneMasks.Masks masks = nl.juiced.guhs.client.GuhBoneMasks.of(info.model());
            boolean[] hidden = masks.hidden(variant, java.util.List.of());
            for (int i = 0; i < masks.bones.length; i++) {
                if (masks.variantBone(i) && hidden[i]) {
                    bones.get(masks.bones[i]).skipRender(true);
                }
            }
        }

        @Override
        protected AABB getBoundingBoxForCulling(Z3Figurant f) {
            return f.getBoundingBoxForCulling();
        }
    }
}
