package nl.juiced.guhs.feature.vadsrijk.zone4.wezens.client;

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
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.Z4WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.loot.Z4Sets;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.mob.Kust;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.mob.Z4Wezen;

/**
 * Zone-4 wezens, the client: one renderer for every creature (its GeckoLib model; the grey grumpy "ontvadst" texture, and after
 * the plof the bright happy {@code <naam>_blij} one, drawn translucent like zone 1), an always-on glowmask (lanterns, eyes),
 * the zone-1 elite look (25 % bigger + an additive gold glow), a faint Mistmopje while it is in the fog, no nameplate then, and
 * the set lines on zone-4 set pieces.
 */
public final class Z4WezensClient {
    static final DataTicket<Boolean> ELITE = DataTicket.create("guhs_vr_z4_elite", Boolean.class);
    static final DataTicket<Boolean> KLEIN = DataTicket.create("guhs_vr_z4_klein", Boolean.class);
    static final DataTicket<Boolean> GEPLOFD = DataTicket.create("guhs_vr_z4_geplofd", Boolean.class);
    static final DataTicket<Boolean> MIST = DataTicket.create("guhs_vr_z4_mist", Boolean.class);

    private Z4WezensClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            for (var s : Z4WezensRegistry.SOORTEN.entrySet()) {
                wezen(e, s.getValue().get(), s.getKey());
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (RenderNameTagEvent.CanRender e) -> {
            if (e.getEntity() instanceof Kust.Mistmopje m && m.inMist()) {
                e.setCanRender(TriState.FALSE);
            }
        });
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent e) -> setRegels(e));
    }

    static Identifier tex(String naam) {
        return Guhs.id("textures/vadsrijk/zone4/wezens/" + naam + ".png");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void wezen(EntityRenderersEvent.RegisterRenderers e, EntityType<?> type, String naam) {
        e.registerEntityRenderer((EntityType) type, c -> new WezenRenderer(c, naam));
    }

    /** A zone-4 creature. */
    public static class WezenRenderer<T extends Z4Wezen> extends GeoEntityRenderer<T, LivingEntityRenderState> {
        public WezenRenderer(EntityRendererProvider.Context context, String naam) {
            super(context, new DefaultedEntityGeoModel<T>(Guhs.id("vadsrijk/zone4/wezens/" + naam)) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return Boolean.TRUE.equals(state.getGeckolibData(GEPLOFD)) && !naam.equals("vliegerknecht") ? tex(naam + "_blij") : tex(naam);
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
            state.addGeckolibData(MIST, w instanceof Kust.Mistmopje m && m.inMist());
        }

        /** Cheered up: translucent (it hops around for a while); in the fog: a faint shimmer. */
        @Override
        public int getRenderColor(T w, @Nullable Void related, float partialTick) {
            int k = super.getRenderColor(w, related, partialTick);
            if (w.geplofd()) {
                return (k & 0x00FFFFFF) | (VadsrijkMob.PLOF_ALPHA << 24);
            }
            if (w instanceof Kust.Mistmopje m && m.inMist()) {
                return 0x30FFFFFF;
            }
            return k;
        }

        @Override
        public @Nullable net.minecraft.client.renderer.rendertype.RenderType getRenderType(LivingEntityRenderState state, Identifier texture) {
            boolean doorzichtig = Boolean.TRUE.equals(state.getGeckolibData(GEPLOFD)) || Boolean.TRUE.equals(state.getGeckolibData(MIST));
            if (doorzichtig && !state.isInvisible) {
                return net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(texture);
            }
            return super.getRenderType(state, texture);
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            float f = (info.getOrDefaultGeckolibData(ELITE, false) ? 1.25f : 1f) * (info.getOrDefaultGeckolibData(KLEIN, false) ? 0.6f : 1f);
            super.scaleModelForRender(info, widthScale * f, heightScale * f);
        }
    }

    /** The glowmask (always, not in the fog) or the elite gold glow (elites only), additive on top. */
    static class Laag<T extends Z4Wezen> extends GeoRenderLayer<T, Void, LivingEntityRenderState> {
        private final Identifier tex;
        private final boolean alleenElite;

        Laag(GeoRenderer<T, Void, LivingEntityRenderState> r, Identifier tex, boolean alleenElite) {
            super(r);
            this.tex = tex;
            this.alleenElite = alleenElite;
        }

        @Override
        public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
            if (info.getOrDefaultGeckolibData(MIST, false) || info.getOrDefaultGeckolibData(GEPLOFD, false)) {
                return;
            }
            if (info.willRender() && (!alleenElite || info.getOrDefaultGeckolibData(ELITE, false))) {
                getRenderer().submitRenderTasks(info, collector.order(1), GuhRenderTypes.eyes(tex));
            }
        }
    }

    /** "Schelpenset (2/4 gedragen)" and both bonuses (green when you have them). */
    private static void setRegels(ItemTooltipEvent e) {
        Uitrusting u = Rollen.van(e.getItemStack());
        String set = u == null ? null : Z4Sets.setVan(u.type());
        if (set == null) {
            return;
        }
        var p = e.getEntity() != null ? e.getEntity() : Minecraft.getInstance().player;
        int n = p == null ? 0 : Z4Sets.stukken(p, set);
        List<Component> t = e.getToolTip();
        int waar = Math.max(1, t.size() - 1);
        t.add(waar++, Component.translatable("vadsrijk.z4wezens.set.stukken", Component.translatable("vadsrijk.z4wezens.set." + set), n)
                .withStyle(ChatFormatting.GOLD));
        t.add(waar++, nl.juiced.guhs.feature.vadsrijk.client.UitrustingTips.stukken(Z4Sets.stukkenVan(set), p));
        t.add(waar++, Component.translatable("vadsrijk.z4wezens.set." + set + ".2").withStyle(n >= 2 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        t.add(waar, Component.translatable("vadsrijk.z4wezens.set." + set + ".4").withStyle(n >= 4 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
    }
}
