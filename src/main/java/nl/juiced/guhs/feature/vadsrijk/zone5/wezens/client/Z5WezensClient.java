package nl.juiced.guhs.feature.vadsrijk.zone5.wezens.client;

import java.util.List;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.renderer.base.BoneSnapshots;
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
import net.minecraft.world.entity.LivingEntity;
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
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.Z5WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas.Donderbuik;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas.Zilverstaart;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.loot.Z5Loot;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.loot.Z5Sets;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.mob.Z5Wezen;

/**
 * Zone-5 wezens, the client: one GeckoLib renderer for every creature, boss and prop (model + texture + always-on glowmask; the
 * zone-1 elite look: 25 % bigger + the additive gold glow), no nameplate while a creature is hidden/disguised, the costume bone
 * ("vermomming") only while a Vermomde Mika or Kootje Zilverstaart is disguised, Donderbuik pink when cheered up, the valves grey
 * when shut and gassy when open (Z3Doel stand), and the Wolkwachtset lines on its pieces.
 */
public final class Z5WezensClient {
    static final DataTicket<Boolean> ELITE = DataTicket.create("guhs_vr_z5_elite", Boolean.class);
    static final DataTicket<Boolean> VERMOMD = DataTicket.create("guhs_vr_z5_vermomd", Boolean.class);
    static final DataTicket<Integer> STAND = DataTicket.create("guhs_vr_z5_stand", Integer.class);

    private Z5WezensClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            for (var s : Z5WezensRegistry.SOORTEN.entrySet()) {
                reg(e, s.getValue().get(), s.getKey(), 1f);
            }
            reg(e, Z5WezensRegistry.ZILVERSTAART.get(), "zilverstaart", 1.15f);
            reg(e, Z5WezensRegistry.GORGONZOLA.get(), "gorgonzola", 1.4f);
            reg(e, Z5WezensRegistry.DONDERBUIK.get(), "donderbuik", 1.0f);
            reg(e, Z5WezensRegistry.KLEP.get(), "klep", 1.0f);
            reg(e, Z5WezensRegistry.LOKPOP.get(), "lokpop", 1.0f);
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (RenderNameTagEvent.CanRender e) -> {
            if (e.getEntity() instanceof Z5Wezen w && (w.verborgen() || w.geplofd() || w.isInvisible())) {
                e.setCanRender(TriState.FALSE);
            } else if (e.getEntity() instanceof Zilverstaart z && z.vermomd()) {
                e.setCanRender(TriState.FALSE);
            } else if (e.getEntity() instanceof Z3Doel d && d.getType() == Z5WezensRegistry.LOKPOP.get()) {
                e.setCanRender(TriState.FALSE);   // a decoy has no name: that would give it away
            }
        });
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent e) -> setRegels(e));
    }

    static Identifier tex(String naam) {
        return Guhs.id("textures/vadsrijk/zone5/wezens/" + naam + ".png");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void reg(EntityRenderersEvent.RegisterRenderers e, EntityType<?> type, String naam, float schaal) {
        e.registerEntityRenderer((EntityType) type, c -> new Renderer(c, naam, schaal));
    }

    /** Every zone-5 thing. */
    public static class Renderer<T extends LivingEntity & GeoEntity> extends GeoEntityRenderer<T, LivingEntityRenderState> {
        private final String naam;
        private final float schaal;

        public Renderer(EntityRendererProvider.Context context, String naam, float schaal) {
            super(context, new DefaultedEntityGeoModel<T>(Guhs.id("vadsrijk/zone5/wezens/" + naam)) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    int stand = state.getOrDefaultGeckolibData(STAND, 0);
                    if ("donderbuik".equals(naam) && stand == 1) {
                        return tex("donderbuik_roze");
                    }
                    if ("klep".equals(naam) && stand == 1) {
                        return tex("klep_dicht");
                    }
                    if ("klep".equals(naam) && stand == 2) {
                        return tex("klep_gas");
                    }
                    return tex(naam);
                }
            });
            this.naam = naam;
            this.schaal = schaal;
            this.shadowRadius = 0.5f * schaal;
            withRenderLayer(new Laag<>(this, tex(naam + "_elite"), true));
            withRenderLayer(new Laag<>(this, tex(naam + "_gloed"), false));
        }

        @Override
        public void addRenderData(T w, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(ELITE, w instanceof VadsrijkMob m && m.elite());
            boolean vermomd = w instanceof Z5Wezen z ? z.verborgen() : w instanceof Zilverstaart s && s.vermomd();
            state.addGeckolibData(VERMOMD, vermomd);
            int stand = w instanceof Donderbuik d ? (d.rozeWeer() ? 1 : 0) : w instanceof Z3Doel d ? d.stand() : 0;
            state.addGeckolibData(STAND, stand);
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            super.adjustModelBonesForRender(info, bones);
            boolean vermomd = info.getOrDefaultGeckolibData(VERMOMD, false);
            if (!vermomd) {
                bones.ifPresent("vermomming", s -> s.skipRender(true).skipChildrenRender(true));
            } else if ("vermomde_mika".equals(naam) || "zilverstaart".equals(naam)) {
                bones.ifPresent("kop", s -> s.skipRender(true).skipChildrenRender(true));   // the costume head covers it
            }
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            float f = schaal * (info.getOrDefaultGeckolibData(ELITE, false) ? 1.25f : 1f);
            super.scaleModelForRender(info, widthScale * f, heightScale * f);
        }
    }

    /** The glowmask (always) or the elite gold glow (elites only), additive on top. */
    static class Laag<T extends LivingEntity & GeoEntity> extends GeoRenderLayer<T, Void, LivingEntityRenderState> {
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

    /** "Wolkwachtset (n/6 gedragen)" and the three bonuses (green when you have them). */
    private static void setRegels(ItemTooltipEvent e) {
        Uitrusting u = Rollen.van(e.getItemStack());
        if (u == null || !Z5Loot.WOLKWACHT.contains(u.type())) {
            return;
        }
        var p = e.getEntity() != null ? e.getEntity() : Minecraft.getInstance().player;
        int n = p == null ? 0 : Z5Sets.stukken(p);
        List<Component> t = e.getToolTip();
        int waar = Math.max(1, t.size() - 1);
        t.add(waar++, Component.translatable("vadsrijk.z5wezens.set.stukken", n, Z5Loot.WOLKWACHT.size()).withStyle(ChatFormatting.GOLD));
        t.add(waar++, Component.translatable("vadsrijk.z5wezens.set.2").withStyle(n >= 2 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        t.add(waar++, Component.translatable("vadsrijk.z5wezens.set.4").withStyle(n >= 4 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        if (Z5Loot.WOLKWACHT.size() >= 6) {
            t.add(waar, Component.translatable("vadsrijk.z5wezens.set.6").withStyle(n >= 6 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        }
    }
}
