package nl.juiced.guhs.feature.vadsrijk.zone6.wezens.client;

import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderTypes;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.rijdier.client.UiterlijkLaag;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Astrolabiumwachter;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Glimmertje;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Sterrenguh;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Wormelien;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Zoutkolos;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.loot.Z6Loot;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.loot.Z6Sets;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.mob.Duin;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.mob.Ruine;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.mob.Z6Wezen;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.mob.Zout;

/**
 * Zone-6 wezens, the client:
 * <ul>
 *   <li>one renderer for every creature: its GeckoLib model; the grey crusted "ontvadst" texture and after the plof the bright happy
 *       {@code <naam>_blij} (translucent while it hops around); a glowmask; the elite look (25 % bigger + gold glow); no name plate
 *       while a Zandvlooi hides in the sand, a Ruïnewachter stands as a statue or the Zoutslang swims through the rock (she is not
 *       drawn then);</li>
 *   <li>the bosses: Wormelien hidden while under the sand, pink once cheered up; the Zoutkolos' glowing heart; the Astrolabiumwachter
 *       shows as many rings as it has (bones {@code ring_1..4}) and glows gold while aligned; the Sterrenguh pink-gold once happy;</li>
 *   <li>the mechanic objects: the salt crust, the singing crystals, the fire bowls (flame bone only when lit), the telescopes
 *       (turned a quarter per {@code stand});</li>
 *   <li>tooltips: the set lines and "Gemaakt voor: &lt;specialisatie&gt;" on the six class weapons;</li>
 *   <li>the Bultguh's tack anchors (t-rijdier's {@link UiterlijkLaag#ANKERS}).</li>
 * </ul>
 */
public final class Z6WezensClient {
    static final DataTicket<Boolean> ELITE = DataTicket.create("guhs_vr_z6_elite", Boolean.class);
    static final DataTicket<Boolean> KLEIN = DataTicket.create("guhs_vr_z6_klein", Boolean.class);
    static final DataTicket<Boolean> GEPLOFD = DataTicket.create("guhs_vr_z6_geplofd", Boolean.class);
    static final DataTicket<Boolean> BLIJ_TEX = DataTicket.create("guhs_vr_z6_blijtex", Boolean.class);
    static final DataTicket<Boolean> WEG = DataTicket.create("guhs_vr_z6_weg", Boolean.class);
    static final DataTicket<Boolean> VLAG = DataTicket.create("guhs_vr_z6_vlag", Boolean.class);
    static final DataTicket<Integer> GETAL = DataTicket.create("guhs_vr_z6_getal", Integer.class);

    private Z6WezensClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(Z6WezensClient::renderers);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (RenderNameTagEvent.CanRender e) -> {
            if (e.getEntity() instanceof Duin.Zandvlooi z && z.verstopt() || e.getEntity() instanceof Ruine.Ruinewachter r && r.slaapt()
                    || e.getEntity() instanceof Zout.Zoutslang s && s.onder() || e.getEntity() instanceof Wormelien w && w.onder()) {
                e.setCanRender(TriState.FALSE);
            }
        });
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent e) -> tooltip(e));
        // the Bultguh's tack: head (between the ears), neck (the bells), the saddle between the humps (model pixels from the pivots)
        java.util.Map<UiterlijkLaag.Plek, UiterlijkLaag.Anker> m = new java.util.EnumMap<>(UiterlijkLaag.Plek.class);
        m.put(UiterlijkLaag.Plek.KOP, new UiterlijkLaag.Anker("hoofd", 0, 6.5f, -2.5f, 0.95f));
        m.put(UiterlijkLaag.Plek.NEK, new UiterlijkLaag.Anker("nek", 0, 1.5f, -2.5f, 1.0f));
        m.put(UiterlijkLaag.Plek.RUG, new UiterlijkLaag.Anker("zadel", 0, 1.5f, 1.0f, 1.45f));
        UiterlijkLaag.ANKERS.put(RijdierSoort.BULTGUH, m);
    }

    static Identifier tex(String naam) {
        return Guhs.id("textures/vadsrijk/zone6/wezens/" + naam + ".png");
    }

    static Identifier btex(String naam) {
        return Guhs.id("textures/vadsrijk/zone6/bazen/" + naam + ".png");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        for (var s : Z6WezensRegistry.SOORTEN.entrySet()) {
            e.registerEntityRenderer((EntityType) s.getValue().get(), c -> new WezenRenderer(c, s.getKey()));
        }
        e.registerEntityRenderer(Z6WezensRegistry.WORMELIEN.get(), ctx -> new Baas<Wormelien>(ctx, "wormelien", 1.0f, w -> w.blij(), w -> w.onder()));
        e.registerEntityRenderer(Z6WezensRegistry.ZOUTKOLOS.get(), ctx -> new Baas<Zoutkolos>(ctx, "zoutkolos", 1.0f, k -> k.gloeit(), k -> false));
        e.registerEntityRenderer(Z6WezensRegistry.GLIMMERTJE.get(), ctx -> new Baas<Glimmertje>(ctx, "glimmertje", 1.15f, g -> false, g -> false));
        e.registerEntityRenderer(Z6WezensRegistry.STERRENGUH.get(), ctx -> new Baas<Sterrenguh>(ctx, "sterrenguh", 1.0f, s -> s.blij() || s.hartAan(), s -> false));
        e.registerEntityRenderer(Z6WezensRegistry.ASTROLABIUMWACHTER.get(), ctx -> new Baas<Astrolabiumwachter>(ctx, "astrolabiumwachter", 1.0f,
                Astrolabiumwachter::uitgelijnd, a -> false) {
            @Override
            public void addRenderData(Astrolabiumwachter b, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
                super.addRenderData(b, related, state, partialTick);
                state.addGeckolibData(GETAL, b.ringen());
            }

            @Override
            public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
                super.adjustModelBonesForRender(info, bones);
                int n = info.getOrDefaultGeckolibData(GETAL, 1);
                for (int i = n; i < 4; i++) {
                    bones.ifPresent("ring_" + (i + 1), s -> s.skipRender(true).skipChildrenRender(true));
                }
            }
        });
        e.registerEntityRenderer(Z6WezensRegistry.ZOUTKORST.get(), ctx -> new Doel(ctx, "zoutkorst", false));
        e.registerEntityRenderer(Z6WezensRegistry.ZINGKRISTAL.get(), ctx -> new Doel(ctx, "zingkristal", true));
        e.registerEntityRenderer(Z6WezensRegistry.VUURSCHAAL.get(), ctx -> new Doel(ctx, "vuurschaal", true) {
            @Override
            public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
                if (info.getOrDefaultGeckolibData(GETAL, 1) == 0) {
                    bones.ifPresent("vlam", s -> s.skipRender(true).skipChildrenRender(true));
                }
            }
        });
        e.registerEntityRenderer(Z6WezensRegistry.TELESCOOP.get(), ctx -> new Doel(ctx, "telescoop", false) {
            @Override
            public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
                int stand = info.getOrDefaultGeckolibData(GETAL, 0);
                bones.ifPresent("kijker", s -> s.setRotY((float) (-Math.PI / 2 * stand)));
            }
        });
    }

    /** A zone-6 creature. */
    public static class WezenRenderer<T extends Z6Wezen> extends GeoEntityRenderer<T, LivingEntityRenderState> {
        public WezenRenderer(EntityRendererProvider.Context context, String naam) {
            super(context, new DefaultedEntityGeoModel<T>(Guhs.id("vadsrijk/zone6/wezens/" + naam)) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return Boolean.TRUE.equals(state.getGeckolibData(BLIJ_TEX)) ? tex(naam + "_blij") : tex(naam);
                }
            });
            this.shadowRadius = 0.45f;
            withRenderLayer(new Laag<>(this, tex(naam + "_elite"), true));
            withRenderLayer(new Laag<>(this, tex(naam + "_gloed"), false));
        }

        @Override
        public void addRenderData(T w, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(ELITE, w.eliteZichtbaar());
            state.addGeckolibData(KLEIN, w.klein());
            state.addGeckolibData(GEPLOFD, w.geplofd());
            state.addGeckolibData(BLIJ_TEX, w.geplofd() && w.afloop() == Z6Wezen.Afloop.OPVROLIJKEN);
            state.addGeckolibData(WEG, w instanceof Zout.Zoutslang s && s.onder());
        }

        @Override
        public int getRenderColor(T w, @Nullable Void related, float partialTick) {
            int k = super.getRenderColor(w, related, partialTick);
            if (w.geplofd()) {
                return (k & 0x00FFFFFF) | (VadsrijkMob.PLOF_ALPHA << 24);
            }
            return k;
        }

        @Override
        public @Nullable net.minecraft.client.renderer.rendertype.RenderType getRenderType(LivingEntityRenderState state, Identifier texture) {
            if (Boolean.TRUE.equals(state.getGeckolibData(GEPLOFD)) && !state.isInvisible) {
                return net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(texture);
            }
            return super.getRenderType(state, texture);
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            if (info.getOrDefaultGeckolibData(WEG, false)) {
                bones.ifPresent("root", s -> s.skipRender(true).skipChildrenRender(true));
            }
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            float f = (info.getOrDefaultGeckolibData(ELITE, false) ? 1.25f : 1f) * (info.getOrDefaultGeckolibData(KLEIN, false) ? 0.6f : 1f);
            super.scaleModelForRender(info, widthScale * f, heightScale * f);
        }
    }

    /** The glowmask (always) or the elite gold glow (elites only), additive on top; nothing once it is beaten. */
    static class Laag<T extends Z6Wezen> extends GeoRenderLayer<T, Void, LivingEntityRenderState> {
        private final Identifier tex;
        private final boolean alleenElite;

        Laag(GeoRenderer<T, Void, LivingEntityRenderState> r, Identifier tex, boolean alleenElite) {
            super(r);
            this.tex = tex;
            this.alleenElite = alleenElite;
        }

        @Override
        public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
            if (info.getOrDefaultGeckolibData(GEPLOFD, false) || info.getOrDefaultGeckolibData(WEG, false)) {
                return;
            }
            if (info.willRender() && (!alleenElite || info.getOrDefaultGeckolibData(ELITE, false))) {
                getRenderer().submitRenderTasks(info, collector.order(1), GuhRenderTypes.eyes(tex));
            }
        }
    }

    /** A boss: its model, the normal / "vlag" texture (pink, glowing...), a glowmask, hidden while "weg" (under the sand). */
    static class Baas<T extends LivingEntity & GeoEntity> extends GeoEntityRenderer<T, LivingEntityRenderState> {
        private final Function<T, Boolean> vlag, weg;

        Baas(EntityRendererProvider.Context ctx, String naam, float schaal, Function<T, Boolean> vlag, Function<T, Boolean> weg) {
            super(ctx, new DefaultedEntityGeoModel<T>(Guhs.id("vr_z6_baas_" + naam)) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return Boolean.TRUE.equals(state.getGeckolibData(VLAG)) ? btex(naam + "_blij") : btex(naam);
                }
            });
            this.vlag = vlag;
            this.weg = weg;
            withScale(schaal);
            withRenderLayer(new AutoGlowingGeoLayer<T, Void, LivingEntityRenderState>(this));
            this.shadowRadius = 1.0f;
        }

        @Override
        public void addRenderData(T b, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(VLAG, vlag.apply(b));
            state.addGeckolibData(WEG, weg.apply(b));
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            if (info.getOrDefaultGeckolibData(WEG, false)) {
                bones.ifPresent("root", s -> s.skipRender(true).skipChildrenRender(true));
            }
        }
    }

    /** A mechanic object (Z3Doel): model + texture (+ glowmask); its stand goes to the bones. */
    static class Doel extends GeoEntityRenderer<Z3Doel, LivingEntityRenderState> {
        Doel(EntityRendererProvider.Context ctx, String naam, boolean gloed) {
            super(ctx, new DefaultedEntityGeoModel<Z3Doel>(Guhs.id("vr_z6_baas_" + naam)) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return btex(naam);
                }
            });
            if (gloed) {
                withRenderLayer(new AutoGlowingGeoLayer<Z3Doel, Void, LivingEntityRenderState>(this));
            }
        }

        @Override
        public void addRenderData(Z3Doel d, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(GETAL, d.stand());
        }
    }

    // ---- tooltips ---------------------------------------------------------------------------------------------------------------------

    private static void tooltip(ItemTooltipEvent e) {
        Uitrusting u = Rollen.van(e.getItemStack());
        if (u == null) {
            return;
        }
        List<Component> t = e.getToolTip();
        int waar = Math.max(1, t.size() - 1);
        String spec = Z6Loot.SPEC.get(u.type());
        if (spec != null) {
            t.add(waar, Component.translatable("vadsrijk.z6wezens.spec", Component.translatable("vadsrijk.z6wezens.spec." + spec))
                    .withStyle(ChatFormatting.AQUA));
            return;
        }
        String set = Z6Sets.setVan(u.type());
        if (set == null) {
            return;
        }
        var p = e.getEntity() != null ? e.getEntity() : Minecraft.getInstance().player;
        int n = p == null ? 0 : Z6Sets.stukken(p, set);
        int max = Z6Sets.stukkenVan(set).size();
        t.add(waar++, Component.translatable("vadsrijk.z6wezens.set.stukken", Component.translatable("vadsrijk.z6wezens.set." + set), n, max)
                .withStyle(ChatFormatting.GOLD));
        for (int k : max >= 6 ? new int[] {2, 4, 6} : new int[] {2, 4}) {
            t.add(waar++, Component.translatable("vadsrijk.z6wezens.set." + set + "." + k).withStyle(n >= k ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        }
    }
}
