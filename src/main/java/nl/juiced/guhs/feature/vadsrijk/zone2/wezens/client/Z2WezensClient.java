package nl.juiced.guhs.feature.vadsrijk.zone2.wezens.client;

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
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.Z2WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.loot.Z2Sets;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.mob.Critter;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.mob.Z2Wezen;

/**
 * Zone-2 wezens, the client: one renderer for every monster (its GeckoLib model + texture, an always-on glowmask for the eyes and
 * crystals, the zone-1 elite look: 25 % bigger + an additive gold glow, small for split copies, no shadow while hidden), the
 * critters, no nameplate while a creature is hidden or defeated, and the set lines under Kristalset / Mika-buitset pieces.
 */
public final class Z2WezensClient {
    static final DataTicket<Boolean> ELITE = DataTicket.create("guhs_vr_z2_elite", Boolean.class);
    static final DataTicket<Boolean> KLEIN = DataTicket.create("guhs_vr_z2_klein", Boolean.class);
    static final DataTicket<Boolean> VERBORGEN = DataTicket.create("guhs_vr_z2_verborgen", Boolean.class);
    static final DataTicket<Boolean> GEPLOFD = DataTicket.create("guhs_vr_z2_geplofd", Boolean.class);

    private Z2WezensClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            wezen(e, Z2WezensRegistry.SPLINTERLING.get(), "splinterling", 1.0f);
            wezen(e, Z2WezensRegistry.GALMVLEERMUIS.get(), "galmvleermuis", 1.0f);
            wezen(e, Z2WezensRegistry.SNEEUWKNABBELAAR.get(), "sneeuwknabbelaar", 1.0f);
            wezen(e, Z2WezensRegistry.KRISTALNAALDKRUIPER.get(), "kristalnaaldkruiper", 1.0f);
            wezen(e, Z2WezensRegistry.IJZELGEEST.get(), "ijzelgeest", 1.0f);
            wezen(e, Z2WezensRegistry.SCHOTSBIJTER.get(), "schotsbijter", 1.0f);
            wezen(e, Z2WezensRegistry.VERSTEENDE_WACHT.get(), "versteende_wacht", 1.0f);
            wezen(e, Z2WezensRegistry.IJSPUFFER.get(), "ijspuffer", 1.0f);
            wezen(e, Z2WezensRegistry.MIKA_MIJNWERKER.get(), "mika_mijnwerker", 1.0f);
            wezen(e, Z2WezensRegistry.MIKA_BOORDER.get(), "mika_boorder", 1.0f);
            wezen(e, Z2WezensRegistry.KLOOFGIER.get(), "kloofgier", 1.0f);
            wezen(e, Z2WezensRegistry.KRISTALSPIN.get(), "kristalspin", 1.0f);
            wezen(e, Z2WezensRegistry.SPIEGELWACHT.get(), "spiegelwacht", 1.0f);
            wezen(e, Z2WezensRegistry.SCHERFGEEST.get(), "scherfgeest", 1.0f);
            critter(e, Z2WezensRegistry.SNEEUWHAAS.get(), "sneeuwhaas", false);
            critter(e, Z2WezensRegistry.IJSVOGEL.get(), "ijsvogel", false);
            critter(e, Z2WezensRegistry.KRISTALMOT.get(), "kristalmot", true);
        });
        // after kern's nameplate listener: hidden (under the ice, burrowed, invisible) or defeated = no nameplate
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (RenderNameTagEvent.CanRender e) -> {
            if (e.getEntity() instanceof Z2Wezen w && (w.verborgen() || w.geplofd() || w.isInvisible())) {
                e.setCanRender(TriState.FALSE);
            } else if (e.getEntity() instanceof Critter) {
                e.setCanRender(TriState.FALSE);
            }
        });
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent e) -> setRegels(e));
    }

    static Identifier tex(String naam) {
        return Guhs.id("textures/vadsrijk/zone2/wezens/" + naam + ".png");
    }

    private static <T extends Z2Wezen> void wezen(EntityRenderersEvent.RegisterRenderers e, EntityType<T> type, String naam, float schaal) {
        e.registerEntityRenderer(type, c -> new WezenRenderer<>(c, naam, schaal));
    }

    private static void critter(EntityRenderersEvent.RegisterRenderers e, EntityType<Critter> type, String naam, boolean gloed) {
        e.registerEntityRenderer(type, c -> new CritterRenderer(c, naam, gloed));
    }

    /** A zone-2 monster. */
    public static class WezenRenderer<T extends Z2Wezen> extends GeoEntityRenderer<T, LivingEntityRenderState> {
        private final float schaal;

        public WezenRenderer(EntityRendererProvider.Context context, String naam, float schaal) {
            super(context, new DefaultedEntityGeoModel<T>(Guhs.id("vadsrijk/zone2/wezens/" + naam)) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return tex(naam);
                }
            });
            this.schaal = schaal;
            this.shadowRadius = 0.5f * schaal;
            withRenderLayer(new Laag<>(this, tex(naam + "_elite"), true));
            withRenderLayer(new Laag<>(this, tex(naam + "_gloed"), false));
        }

        @Override
        public void addRenderData(T w, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(ELITE, w.eliteZichtbaar());
            state.addGeckolibData(KLEIN, w.klein());
            state.addGeckolibData(VERBORGEN, w.verborgen());
            state.addGeckolibData(GEPLOFD, w.geplofd());
        }

        /** (balans 2026-11) After the plof: translucent (and no hitbox, see VadsrijkMob#isPickable). */
        @Override
        public int getRenderColor(T w, @Nullable Void related, float partialTick) {
            int k = super.getRenderColor(w, related, partialTick);
            return w.geplofd() ? (k & 0x00FFFFFF) | (nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob.PLOF_ALPHA << 24) : k;
        }

        @Override
        public @Nullable net.minecraft.client.renderer.rendertype.RenderType getRenderType(LivingEntityRenderState state, Identifier texture) {
            if (Boolean.TRUE.equals(state.getGeckolibData(GEPLOFD)) && !state.isInvisible) {
                return net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(texture);
            }
            return super.getRenderType(state, texture);
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            float f = schaal * (info.getOrDefaultGeckolibData(ELITE, false) ? 1.25f : 1f) * (info.getOrDefaultGeckolibData(KLEIN, false) ? 0.6f : 1f);
            super.scaleModelForRender(info, widthScale * f, heightScale * f);
        }

        @Override
        protected float getShadowRadius(LivingEntityRenderState state) {
            Boolean v = state.getGeckolibData(VERBORGEN);
            return v != null && v ? 0 : this.shadowRadius;
        }
    }

    /** The glowmask (always) or the elite gold glow (elites only), additive on top. */
    static class Laag<T extends Z2Wezen> extends GeoRenderLayer<T, Void, LivingEntityRenderState> {
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

    /** A critter: its model and texture (the crystal moth also glows). */
    public static class CritterRenderer extends GeoEntityRenderer<Critter, LivingEntityRenderState> {
        public CritterRenderer(EntityRendererProvider.Context context, String naam, boolean gloed) {
            super(context, new DefaultedEntityGeoModel<Critter>(Guhs.id("vadsrijk/zone2/wezens/" + naam)) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return tex(naam);
                }
            });
            this.shadowRadius = 0.25f;
            if (gloed) {
                Identifier g = tex(naam + "_gloed");
                withRenderLayer(new GeoRenderLayer<Critter, Void, LivingEntityRenderState>(this) {
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

    /** "Kristalset (2/4)" and both bonuses (green when you have them). */
    private static void setRegels(ItemTooltipEvent e) {
        Uitrusting u = Rollen.van(e.getItemStack());
        String set = u == null ? null : Z2Sets.setVan(u.type());
        if (set == null) {
            return;
        }
        var p = e.getEntity() != null ? e.getEntity() : Minecraft.getInstance().player;
        int n = p == null ? 0 : Z2Sets.stukken(p, set);
        List<Component> t = e.getToolTip();
        int waar = Math.max(1, t.size() - 1);
        t.add(waar++, Component.translatable("vadsrijk.z2wezens.set.stukken", Component.translatable("vadsrijk.z2wezens.set." + set), n)
                .withStyle(ChatFormatting.GOLD));
        t.add(waar++, Component.translatable("vadsrijk.z2wezens.set." + set + ".2").withStyle(n >= 2 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        t.add(waar, Component.translatable("vadsrijk.z2wezens.set." + set + ".4").withStyle(n >= 4 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
    }
}
