package nl.juiced.guhs.feature.vadsrijk.zone3.wezens.client;

import java.util.List;

import javax.annotation.Nullable;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.Z3WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.loot.Z3Sets;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob.Duister;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob.Z3Critter;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob.Z3Wezen;

/**
 * Zone-3 wezens, the client: one renderer for every monster (its GeckoLib model + texture, an always-on glowmask for eyes and
 * glowing bits, the zone-1 elite look: 25 % bigger + an additive gold glow; contract elites 50 % bigger in their own colours and
 * only drawn for their own players), the soft lantern rule's silhouette (outside the light a creature is drawn almost black,
 * only its glowing eyes stand out; a Wortelschim is then just a shimmer), the critters (the honey bee is the existing guh_bee),
 * no nameplate while hidden / disguised / in the dark (Wortelschim) / not your contract, and the set lines on zone-3 set pieces.
 */
public final class Z3WezensClient {
    static final DataTicket<Boolean> ELITE = DataTicket.create("guhs_vr_z3_elite", Boolean.class);
    static final DataTicket<Boolean> KLEIN = DataTicket.create("guhs_vr_z3_klein", Boolean.class);
    static final DataTicket<Boolean> VERBORGEN = DataTicket.create("guhs_vr_z3_verborgen", Boolean.class);
    static final DataTicket<Boolean> GEPLOFD = DataTicket.create("guhs_vr_z3_geplofd", Boolean.class);
    static final DataTicket<Boolean> DONKER = DataTicket.create("guhs_vr_z3_donker", Boolean.class);
    static final DataTicket<Boolean> SCHIM = DataTicket.create("guhs_vr_z3_schim", Boolean.class);
    static final DataTicket<String> CONTRACT = DataTicket.create("guhs_vr_z3_contract", String.class);

    private Z3WezensClient() {
    }

    public static void init(IEventBus modBus) {
        Z3Wezen.LOKALE_SPELER = () -> Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getUUID();
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            for (var s : Z3WezensRegistry.SOORTEN.entrySet()) {
                wezen(e, s.getValue().get(), s.getKey());
            }
            for (var s : Z3WezensRegistry.ADDS.entrySet()) {
                wezen(e, s.getValue().get(), s.getKey());
            }
            e.registerEntityRenderer(Z3WezensRegistry.HONINGBIJ.get(), c -> new BijRenderer(c));
            critter(e, Z3WezensRegistry.SPORENMOT.get(), "sporenmot", true);
            critter(e, Z3WezensRegistry.LANTAARNKEVER.get(), "lantaarnkever", true);
            critter(e, Z3WezensRegistry.WORTELMUIS.get(), "wortelmuis", false);
        });
        // after kern's nameplate listener
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (RenderNameTagEvent.CanRender e) -> {
            if (e.getEntity() instanceof Z3Wezen w && (w.zonderNaam() || !w.zichtbaarVoorMij())) {
                e.setCanRender(TriState.FALSE);
            } else if (e.getEntity() instanceof Z3Critter) {
                e.setCanRender(TriState.FALSE);
            }
        });
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent e) -> setRegels(e));
    }

    static Identifier tex(String naam) {
        return Guhs.id("textures/vadsrijk/zone3/wezens/" + naam + ".png");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void wezen(EntityRenderersEvent.RegisterRenderers e, EntityType<?> type, String naam) {
        e.registerEntityRenderer((EntityType) type, c -> new WezenRenderer(c, naam));
    }

    private static void critter(EntityRenderersEvent.RegisterRenderers e, EntityType<Z3Critter> type, String naam, boolean gloed) {
        e.registerEntityRenderer(type, c -> new CritterRenderer(c, naam, gloed));
    }

    /** A zone-3 monster. */
    public static class WezenRenderer<T extends Z3Wezen> extends GeoEntityRenderer<T, LivingEntityRenderState> {
        private final String naam;

        public WezenRenderer(EntityRendererProvider.Context context, String naam) {
            super(context, new DefaultedEntityGeoModel<T>(Guhs.id("vadsrijk/zone3/wezens/" + naam)) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    String c = state.getGeckolibData(CONTRACT);
                    return c != null && !c.isEmpty() && !"hulp".equals(c) ? tex("contract_" + c) : tex(naam);
                }
            });
            this.naam = naam;
            this.shadowRadius = 0.5f;
            withRenderLayer(new Laag<>(this, tex(naam + "_elite"), true));
            withRenderLayer(new Laag<>(this, tex(naam + "_gloed"), false));
        }

        @Override
        public boolean shouldRender(T w, Frustum frustum, double x, double y, double z) {
            return w.zichtbaarVoorMij() && super.shouldRender(w, frustum, x, y, z);
        }

        @Override
        public void addRenderData(T w, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(ELITE, w.eliteZichtbaar());
            state.addGeckolibData(KLEIN, w.klein());
            state.addGeckolibData(VERBORGEN, w.verborgen());
            state.addGeckolibData(GEPLOFD, w.geplofd());
            state.addGeckolibData(DONKER, w.donker());
            state.addGeckolibData(SCHIM, w instanceof Duister.Wortelschim);
            state.addGeckolibData(CONTRACT, w.contract());
        }

        /** Defeated: translucent; outside the light: a near-black silhouette (a Wortelschim: a faint shimmer). */
        @Override
        public int getRenderColor(T w, @Nullable Void related, float partialTick) {
            int k = super.getRenderColor(w, related, partialTick);
            if (w.geplofd()) {
                return (k & 0x00FFFFFF) | (nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob.PLOF_ALPHA << 24);
            }
            if (w.donker()) {
                return w instanceof Duister.Wortelschim ? 0x26101018 : 0xFF1C1A26;
            }
            return k;
        }

        @Override
        public @Nullable net.minecraft.client.renderer.rendertype.RenderType getRenderType(LivingEntityRenderState state, Identifier texture) {
            boolean doorzichtig = Boolean.TRUE.equals(state.getGeckolibData(GEPLOFD))
                    || Boolean.TRUE.equals(state.getGeckolibData(DONKER)) && Boolean.TRUE.equals(state.getGeckolibData(SCHIM));
            if (doorzichtig && !state.isInvisible) {
                return net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(texture);
            }
            return super.getRenderType(state, texture);
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            String c = info.getOrDefaultGeckolibData(CONTRACT, "");
            boolean contract = !c.isEmpty() && !"hulp".equals(c);
            float f = (contract ? 1.5f : info.getOrDefaultGeckolibData(ELITE, false) ? 1.25f : 1f) * (info.getOrDefaultGeckolibData(KLEIN, false) ? 0.6f : 1f);
            super.scaleModelForRender(info, widthScale * f, heightScale * f);
        }

        @Override
        protected float getShadowRadius(LivingEntityRenderState state) {
            Boolean v = state.getGeckolibData(VERBORGEN);
            return v != null && v ? 0 : this.shadowRadius;
        }

        public String naam() {
            return naam;
        }
    }

    /** The glowmask (always) or the elite gold glow (elites only), additive on top. */
    static class Laag<T extends Z3Wezen> extends GeoRenderLayer<T, Void, LivingEntityRenderState> {
        private final Identifier tex;
        private final boolean alleenElite;

        Laag(GeoRenderer<T, Void, LivingEntityRenderState> r, Identifier tex, boolean alleenElite) {
            super(r);
            this.tex = tex;
            this.alleenElite = alleenElite;
        }

        @Override
        public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
            if (info.willRender() && (!alleenElite || info.getOrDefaultGeckolibData(ELITE, false))) {
                getRenderer().submitRenderTasks(info, collector.order(1), GuhRenderTypes.eyes(tex));
            }
        }
    }

    /** A generated critter: its model and texture (moths and fireflies also glow). */
    public static class CritterRenderer extends GeoEntityRenderer<Z3Critter, LivingEntityRenderState> {
        public CritterRenderer(EntityRendererProvider.Context context, String naam, boolean gloed) {
            super(context, new DefaultedEntityGeoModel<Z3Critter>(Guhs.id("vadsrijk/zone3/wezens/" + naam)) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return tex(naam);
                }
            });
            this.shadowRadius = 0.2f;
            if (gloed) {
                Identifier g = tex(naam + "_gloed");
                withRenderLayer(new GeoRenderLayer<Z3Critter, Void, LivingEntityRenderState>(this) {
                    @Override
                    public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
                        if (info.willRender()) {
                            getRenderer().submitRenderTasks(info, collector.order(1), GuhRenderTypes.eyes(g));
                        }
                    }
                });
            }
        }
    }

    /** The honey bee: the existing guh_bee model, texture and fly animation. */
    public static class BijRenderer extends GeoEntityRenderer<Z3Critter, LivingEntityRenderState> {
        public BijRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<Z3Critter>(Guhs.id("guh_bee")));
            this.shadowRadius = 0.2f;
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            super.scaleModelForRender(info, widthScale * 0.7f, heightScale * 0.7f);
        }
    }

    /** "Schorsset (2/4 gedragen)" and both bonuses (green when you have them). */
    private static void setRegels(ItemTooltipEvent e) {
        Uitrusting u = Rollen.van(e.getItemStack());
        String set = u == null ? null : Z3Sets.setVan(u.type());
        if (set == null) {
            return;
        }
        var p = e.getEntity() != null ? e.getEntity() : Minecraft.getInstance().player;
        int n = p == null ? 0 : Z3Sets.stukken(p, set);
        List<Component> t = e.getToolTip();
        int waar = Math.max(1, t.size() - 1);
        t.add(waar++, Component.translatable("vadsrijk.z3wezens.set.stukken", Component.translatable("vadsrijk.z3wezens.set." + set), n)
                .withStyle(ChatFormatting.GOLD));
        t.add(waar++, nl.juiced.guhs.feature.vadsrijk.client.UitrustingTips.stukken(Z3Sets.stukkenVan(set), p));
        t.add(waar++, Component.translatable("vadsrijk.z3wezens.set." + set + ".2").withStyle(n >= 2 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        t.add(waar, Component.translatable("vadsrijk.z3wezens.set." + set + ".4").withStyle(n >= 4 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
    }
}
